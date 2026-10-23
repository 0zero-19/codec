package io.codec.queue

import io.codec.media.ExecutionResult
import io.codec.media.FFmpegProcessExecutor
import io.codec.media.FFprobeAnalyzer
import io.codec.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class TaskQueueManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val analyzer: FFprobeAnalyzer = FFprobeAnalyzer(),
    private val storageDir: File = defaultStorageDir(),
    maxConcurrent: Int = 2
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val queueFile = File(storageDir, "queue.json")
    private val _jobs = MutableStateFlow<List<TaskJob>>(emptyList())
    val jobs: StateFlow<List<TaskJob>> = _jobs.asStateFlow()

    private var semaphore = Semaphore(maxConcurrent)
    private val activeExecutors = mutableMapOf<String, FFmpegProcessExecutor>()
    private val activeCoroutines = mutableMapOf<String, Job>()

    @Volatile
    private var isQueueProcessingActive = true

    init {
        loadPersistedQueue()
    }

    fun setMaxConcurrentTasks(limit: Int) {
        val clamped = limit.coerceIn(1, 8)
        semaphore = Semaphore(clamped)
    }

    suspend fun addJob(
        inputFile: File,
        outputDir: File,
        options: ConversionOptions = ConversionOptions(),
        priority: TaskPriority = TaskPriority.NORMAL
    ): TaskJob = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val baseName = inputFile.nameWithoutExtension
        val ext = options.video.container.extension
        val outputPath = resolveUniqueOutputPath(outputDir, baseName, ext)

        // Initial task
        val job = TaskJob(
            id = id,
            inputPath = inputFile.absolutePath,
            outputPath = outputPath,
            options = options,
            status = TaskStatus.PENDING,
            priority = priority
        )

        _jobs.value = _jobs.value + job
        persistQueue()

        // Asynchronously probe media info
        scope.launch {
            probeJobMedia(id, inputFile)
        }

        job
    }

    suspend fun addBatchJobs(
        inputFiles: List<File>,
        outputDir: File,
        options: ConversionOptions = ConversionOptions()
    ): List<TaskJob> = withContext(Dispatchers.IO) {
        val newJobs = mutableListOf<TaskJob>()
        for (file in inputFiles) {
            val job = addJob(file, outputDir, options)
            newJobs.add(job)
        }
        newJobs
    }

    private suspend fun probeJobMedia(jobId: String, file: File) {
        updateJob(jobId) { it.copy(status = TaskStatus.PROBING) }
        val result = analyzer.analyze(file)
        if (result.isSuccess) {
            val info = result.getOrThrow()
            updateJob(jobId) {
                it.copy(
                    mediaInfo = info,
                    status = TaskStatus.PENDING,
                    totalDurationMs = (info.durationSeconds * 1000).toLong()
                )
            }
        } else {
            updateJob(jobId) {
                it.copy(status = TaskStatus.PENDING)
            }
        }
        triggerNextJobs()
    }

    fun startQueue() {
        isQueueProcessingActive = true
        triggerNextJobs()
    }

    fun pauseQueue() {
        isQueueProcessingActive = false
        for ((_, executor) in activeExecutors) {
            executor.pause()
        }
        _jobs.value = _jobs.value.map {
            if (it.status == TaskStatus.RUNNING) it.copy(status = TaskStatus.PAUSED) else it
        }
    }

    fun resumeQueue() {
        isQueueProcessingActive = true
        for ((_, executor) in activeExecutors) {
            executor.resume()
        }
        _jobs.value = _jobs.value.map {
            if (it.status == TaskStatus.PAUSED) it.copy(status = TaskStatus.RUNNING) else it
        }
        triggerNextJobs()
    }

    fun pauseJob(id: String) {
        activeExecutors[id]?.pause()
        updateJob(id) { it.copy(status = TaskStatus.PAUSED) }
    }

    fun resumeJob(id: String) {
        activeExecutors[id]?.resume()
        updateJob(id) { it.copy(status = TaskStatus.RUNNING) }
        triggerNextJobs()
    }

    fun cancelJob(id: String) {
        activeExecutors[id]?.cancel()
        activeCoroutines[id]?.cancel()
        activeExecutors.remove(id)
        activeCoroutines.remove(id)
        updateJob(id) { it.copy(status = TaskStatus.CANCELLED) }
        persistQueue()
        triggerNextJobs()
    }

    fun retryJob(id: String) {
        updateJob(id) {
            it.copy(
                status = TaskStatus.PENDING,
                progress = 0.0,
                currentFps = 0.0,
                speedMultiplier = 0.0,
                etaSeconds = 0,
                errorMessage = null
            )
        }
        triggerNextJobs()
    }

    fun removeJob(id: String) {
        cancelJob(id)
        _jobs.value = _jobs.value.filter { it.id != id }
        persistQueue()
    }

    fun clearCompletedJobs() {
        _jobs.value = _jobs.value.filter {
            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED
        }
        persistQueue()
    }

    fun updateJobOptions(id: String, options: ConversionOptions) {
        updateJob(id) { job ->
            if (job.status == TaskStatus.PENDING) {
                val base = File(job.outputPath).nameWithoutExtension
                val dir = File(job.outputPath).parentFile ?: File(".")
                val ext = options.video.container.extension
                val newOutput = resolveUniqueOutputPath(dir, base, ext)
                job.copy(options = options, outputPath = newOutput)
            } else {
                job
            }
        }
    }

    private fun triggerNextJobs() {
        if (!isQueueProcessingActive) return

        val pendingJobs = _jobs.value
            .filter { it.status == TaskStatus.PENDING }
            .sortedByDescending { it.priority.ordinal }

        for (job in pendingJobs) {
            if (activeExecutors.containsKey(job.id)) continue

            val coroutineJob = scope.launch {
                val acquired = semaphore.tryAcquire()
                if (!acquired) return@launch

                try {
                    runJob(job.id)
                } finally {
                    semaphore.release()
                    activeExecutors.remove(job.id)
                    activeCoroutines.remove(job.id)
                    triggerNextJobs()
                }
            }
            activeCoroutines[job.id] = coroutineJob
        }
    }

    private suspend fun runJob(jobId: String) {
        val currentJob = _jobs.value.firstOrNull { it.id == jobId } ?: return
        val executor = FFmpegProcessExecutor()
        activeExecutors[jobId] = executor

        updateJob(jobId) {
            it.copy(
                status = TaskStatus.RUNNING,
                startedAtMs = System.currentTimeMillis()
            )
        }

        val jobLogs = mutableListOf<String>()

        val result = executor.execute(
            inputPath = currentJob.inputPath,
            outputPath = currentJob.outputPath,
            options = currentJob.options,
            mediaInfo = currentJob.mediaInfo,
            onProgress = { update, pass, totalPasses ->
                updateJob(jobId) { j ->
                    j.copy(
                        progress = update.percentage,
                        currentFps = update.fps,
                        speedMultiplier = update.speedMultiplier,
                        etaSeconds = update.etaSeconds,
                        processedTimeMs = update.outTimeMs,
                        outputSizeBytes = update.totalSizeBytes,
                        currentPass = pass,
                        totalPasses = totalPasses
                    )
                }
            },
            onLogLine = { line ->
                synchronized(jobLogs) {
                    if (jobLogs.size >= 100) jobLogs.removeAt(0)
                    jobLogs.add(line)
                }
            }
        )

        when (result) {
            is ExecutionResult.Success -> {
                val finalLength = result.finalOutputFile.length()
                updateJob(jobId) {
                    it.copy(
                        status = TaskStatus.COMPLETED,
                        progress = 100.0,
                        outputSizeBytes = finalLength,
                        finishedAtMs = System.currentTimeMillis(),
                        ffmpegLog = synchronized(jobLogs) { jobLogs.toList() }
                    )
                }
            }
            is ExecutionResult.Failure -> {
                updateJob(jobId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        errorMessage = "${result.errorMessage}\n${result.logSnippet}",
                        finishedAtMs = System.currentTimeMillis(),
                        ffmpegLog = synchronized(jobLogs) { jobLogs.toList() }
                    )
                }
            }
            is ExecutionResult.Cancelled -> {
                updateJob(jobId) {
                    it.copy(
                        status = TaskStatus.CANCELLED,
                        finishedAtMs = System.currentTimeMillis()
                    )
                }
            }
        }
        persistQueue()
    }

    private fun updateJob(jobId: String, transform: (TaskJob) -> TaskJob) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) transform(it) else it
        }
    }

    private fun resolveUniqueOutputPath(dir: File, baseName: String, extension: String): String {
        dir.mkdirs()
        var target = File(dir, "$baseName.$extension")
        var counter = 1
        while (target.exists()) {
            target = File(dir, "$baseName ($counter).$extension")
            counter++
        }
        return target.absolutePath
    }

    private fun loadPersistedQueue() {
        if (!queueFile.exists()) return
        try {
            val content = queueFile.readText()
            val list = json.decodeFromString<List<TaskJob>>(content)
            // Reset running/probing to pending upon cold restart
            _jobs.value = list.map {
                if (it.status == TaskStatus.RUNNING || it.status == TaskStatus.PROBING) {
                    it.copy(status = TaskStatus.PENDING)
                } else it
            }
        } catch (_: Exception) {}
    }

    private fun persistQueue() {
        try {
            storageDir.mkdirs()
            queueFile.writeText(json.encodeToString(_jobs.value))
        } catch (_: Exception) {}
    }

    companion object {
        fun defaultStorageDir(): File {
            val userHome = System.getProperty("user.home")
            return File(userHome, ".codec")
        }
    }
}
