package io.codec.media

import io.codec.model.ConversionOptions
import io.codec.model.MediaInfo
import io.codec.model.QualityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ExecutionResult {
    data class Success(val finalOutputFile: File) : ExecutionResult()
    data class Failure(val errorMessage: String, val exitCode: Int, val logSnippet: String) : ExecutionResult()
    data object Cancelled : ExecutionResult()
}

class FFmpegProcessExecutor(
    private val commandBuilder: FFmpegCommandBuilder = FFmpegCommandBuilder()
) {
    @Volatile
    private var currentProcess: Process? = null

    @Volatile
    private var isCancelled: Boolean = false

    @Volatile
    private var isPaused: Boolean = false

    suspend fun execute(
        inputPath: String,
        outputPath: String,
        options: ConversionOptions,
        mediaInfo: MediaInfo?,
        onProgress: (ProgressUpdate, currentPass: Int, totalPasses: Int) -> Unit,
        onLogLine: (String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        isCancelled = false
        isPaused = false

        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val tempOutputFile = File(outputFile.parentFile, ".codec_tmp_${outputFile.name}")
        if (tempOutputFile.exists()) {
            tempOutputFile.delete()
        }

        val totalDurationMs = ((mediaInfo?.durationSeconds ?: 0.0) * 1000).toLong()
        val isTwoPass = options.video.qualityMode == QualityMode.TWO_PASS
        val totalPasses = if (isTwoPass) 2 else 1
        val passLogPrefix = if (isTwoPass) {
            File(System.getProperty("java.io.tmpdir"), "codec_pass_${UUID.randomUUID()}").absolutePath
        } else null

        val recentLogs = mutableListOf<String>()

        try {
            for (pass in 1..totalPasses) {
                if (isCancelled) {
                    cleanupTempFiles(tempOutputFile, passLogPrefix)
                    return@withContext ExecutionResult.Cancelled
                }

                val targetPath = if (pass == totalPasses) tempOutputFile.absolutePath else outputPath
                val cmd = commandBuilder.buildCommand(
                    inputPath = inputPath,
                    outputPath = targetPath,
                    options = options,
                    mediaInfo = mediaInfo,
                    pass = pass,
                    totalPasses = totalPasses,
                    passLogFilePrefix = passLogPrefix
                )

                onLogLine("[EXEC] Pass $pass/$totalPasses: ${cmd.joinToString(" ")}")

                val processBuilder = ProcessBuilder(cmd)
                val process = processBuilder.start()
                currentProcess = process

                val parser = FFmpegProgressParser(totalDurationMs)

                // Read stdout (where -progress pipe:1 writes)
                val stdoutJob = Thread {
                    try {
                        process.inputStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                if (isCancelled) break
                                val update = parser.parseLine(line)
                                if (update != null) {
                                    onProgress(update, pass, totalPasses)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                stdoutJob.start()

                // Read stderr (where FFmpeg writes informational and error logs)
                val stderrJob = Thread {
                    try {
                        process.errorStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                onLogLine(line)
                                synchronized(recentLogs) {
                                    if (recentLogs.size >= 50) recentLogs.removeAt(0)
                                    recentLogs.add(line)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                stderrJob.start()

                val exitCode = process.waitFor()
                stdoutJob.join(1000)
                stderrJob.join(1000)

                currentProcess = null

                if (isCancelled) {
                    cleanupTempFiles(tempOutputFile, passLogPrefix)
                    return@withContext ExecutionResult.Cancelled
                }

                if (exitCode != 0) {
                    cleanupTempFiles(tempOutputFile, passLogPrefix)
                    val errorLog = synchronized(recentLogs) { recentLogs.takeLast(15).joinToString("\n") }
                    return@withContext ExecutionResult.Failure(
                        errorMessage = "FFmpeg process failed with exit code $exitCode",
                        exitCode = exitCode,
                        logSnippet = errorLog
                    )
                }
            }

            // Atomic rename from .part to final outputPath
            if (tempOutputFile.exists()) {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                val renamed = tempOutputFile.renameTo(outputFile)
                if (!renamed) {
                    // Fallback copy & delete
                    tempOutputFile.copyTo(outputFile, overwrite = true)
                    tempOutputFile.delete()
                }
            }

            cleanupPassLogFiles(passLogPrefix)
            ExecutionResult.Success(outputFile)
        } catch (e: Exception) {
            cleanupTempFiles(tempOutputFile, passLogPrefix)
            if (isCancelled) {
                ExecutionResult.Cancelled
            } else {
                ExecutionResult.Failure(
                    errorMessage = e.message ?: "Unknown execution error",
                    exitCode = -1,
                    logSnippet = e.stackTraceToString().take(500)
                )
            }
        } finally {
            currentProcess = null
        }
    }

    fun pause() {
        val proc = currentProcess ?: return
        val pid = getProcessPid(proc) ?: return
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (isWindows) {
                ProcessBuilder("powershell", "-Command", "Suspend-Process -Id $pid").start().waitFor()
            } else {
                ProcessBuilder("kill", "-STOP", pid.toString()).start().waitFor()
            }
            isPaused = true
        } catch (_: Exception) {}
    }

    fun resume() {
        val proc = currentProcess ?: return
        val pid = getProcessPid(proc) ?: return
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (isWindows) {
                ProcessBuilder("powershell", "-Command", "Resume-Process -Id $pid").start().waitFor()
            } else {
                ProcessBuilder("kill", "-CONT", pid.toString()).start().waitFor()
            }
            isPaused = false
        } catch (_: Exception) {}
    }

    fun cancel() {
        isCancelled = true
        val proc = currentProcess ?: return

        // If paused, resume first so it can receive kill/exit
        if (isPaused) {
            resume()
        }

        try {
            // Send 'q' to gracefully request ffmpeg termination
            proc.outputStream.bufferedWriter().use {
                it.write("q\n")
                it.flush()
            }
        } catch (_: Exception) {}

        Thread {
            try {
                Thread.sleep(1500)
                if (proc.isAlive) {
                    proc.destroyForcibly()
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun getProcessPid(process: Process): Long? {
        return try {
            process.pid()
        } catch (_: Throwable) {
            null
        }
    }

    private fun cleanupTempFiles(tempOutputFile: File, passLogPrefix: String?) {
        try {
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            cleanupPassLogFiles(passLogPrefix)
        } catch (_: Exception) {}
    }

    private fun cleanupPassLogFiles(passLogPrefix: String?) {
        if (passLogPrefix == null) return
        try {
            val p1 = File("${passLogPrefix}-0.log")
            val p2 = File("${passLogPrefix}-0.log.mbtree")
            if (p1.exists()) p1.delete()
            if (p2.exists()) p2.delete()
        } catch (_: Exception) {}
    }
}
