package io.codec.desktop.ui

import androidx.compose.runtime.*
import io.codec.media.FFmpegCapabilities
import io.codec.media.FFprobeAnalyzer
import io.codec.media.MediaTools
import io.codec.media.SystemCapabilities
import io.codec.model.*
import io.codec.profiles.ProfileRepository
import io.codec.queue.TaskQueueManager
import io.codec.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

enum class Screen {
    QUEUE,
    PRESETS,
    TOOLS,
    INSPECTOR,
    SETTINGS
}

class StagedMediaFile(
    val file: File,
    initialOptions: ConversionOptions = ConversionOptions()
) {
    var mediaInfo by mutableStateOf<MediaInfo?>(null)
    var options by mutableStateOf(initialOptions)
    var isProbing by mutableStateOf(true)
}

class DesktopState(
    val scope: CoroutineScope,
    val queueManager: TaskQueueManager = TaskQueueManager(),
    val profileRepo: ProfileRepository = ProfileRepository(),
    val settingsRepo: SettingsRepository = SettingsRepository(),
    val capabilitiesDetector: FFmpegCapabilities = FFmpegCapabilities(),
    val analyzer: FFprobeAnalyzer = FFprobeAnalyzer(),
    val tools: MediaTools = MediaTools()
) {
    var activeScreen by mutableStateOf(Screen.QUEUE)
    var selectedJobId by mutableStateOf<String?>(null)
    var activeJobForLogs by mutableStateOf<TaskJob?>(null)
    var inspectedFile by mutableStateOf<File?>(null)
    var inspectedMediaInfo by mutableStateOf<MediaInfo?>(null)

    // Staged configuration dialog state
    val stagedFiles = mutableStateListOf<StagedMediaFile>()
    var isConfigDialogOpen by mutableStateOf(false)
    var activeStagedIndex by mutableStateOf(0)
    var applyUniformlyToAll by mutableStateOf(true)
    var editingJobId by mutableStateOf<String?>(null)

    // Save as preset sub-dialog
    var isSavePresetDialogOpen by mutableStateOf(false)

    val activeStagedFile: StagedMediaFile?
        get() = stagedFiles.getOrNull(activeStagedIndex)

    val jobs = mutableStateListOf<TaskJob>()
    val profiles = mutableStateListOf<PresetProfile>()
    var config by mutableStateOf(AppConfig())
    var systemCaps by mutableStateOf<SystemCapabilities?>(null)

    val selectedJob: TaskJob?
        get() = jobs.firstOrNull { it.id == selectedJobId } ?: jobs.firstOrNull()

    init {
        scope.launch {
            queueManager.jobs.collectLatest { list ->
                jobs.clear()
                jobs.addAll(list)
                if (selectedJobId == null && list.isNotEmpty()) {
                    selectedJobId = list.first().id
                }
            }
        }

        scope.launch {
            profileRepo.profiles.collectLatest { list ->
                profiles.clear()
                profiles.addAll(list)
            }
        }

        scope.launch {
            settingsRepo.config.collectLatest { cfg ->
                config = cfg
                queueManager.setMaxConcurrentTasks(cfg.maxConcurrentTasks)
            }
        }

        scope.launch {
            systemCaps = capabilitiesDetector.detectCapabilities()
        }
    }

    fun addFilesDialog() {
        val dialog = FileDialog(null as Frame?, Strings.get("add_files", config.language), FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.isVisible = true

        val files = dialog.files
        if (files != null && files.isNotEmpty()) {
            addInputFiles(files.toList())
        }
    }

    fun addFolderDialog() {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val dialog = FileDialog(null as Frame?, Strings.get("add_folder", config.language), FileDialog.LOAD)
        dialog.isVisible = true

        val dirPath = dialog.directory
        val fileName = dialog.file
        if (dirPath != null) {
            val selectedDir = if (fileName != null) File(dirPath, fileName) else File(dirPath)
            if (selectedDir.isDirectory) {
                val mediaExtensions = setOf(
                    "mp4", "mkv", "mov", "avi", "webm", "wmv", "flv", "ts", "m4v", "mpeg", "mpg",
                    "mp3", "aac", "wav", "flac", "ogg", "opus", "m4a", "wma", "gif", "apng"
                )
                val foundFiles = selectedDir.walkTopDown()
                    .filter { it.isFile && mediaExtensions.contains(it.extension.lowercase()) }
                    .take(300)
                    .toList()

                if (foundFiles.isNotEmpty()) {
                    addInputFiles(foundFiles)
                }
            }
        }
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
    }

    fun addInputFiles(files: List<File>) {
        if (files.isEmpty()) return
        val defaultOptions = config.lastUsedOptions
            ?: profileRepo.getDefaultProfile().options

        stagedFiles.clear()
        editingJobId = null
        activeStagedIndex = 0
        applyUniformlyToAll = true

        for (file in files) {
            stagedFiles.add(StagedMediaFile(file, defaultOptions))
        }

        isConfigDialogOpen = true

        // Launch probing on all staged files concurrently
        for (staged in stagedFiles) {
            scope.launch(Dispatchers.IO) {
                val result = analyzer.analyze(staged.file)
                if (result.isSuccess) {
                    staged.mediaInfo = result.getOrNull()
                }
                staged.isProbing = false
            }
        }
    }

    fun openConfigForExistingJob(job: TaskJob) {
        stagedFiles.clear()
        editingJobId = job.id
        activeStagedIndex = 0
        applyUniformlyToAll = false

        val staged = StagedMediaFile(File(job.inputPath), job.options)
        staged.mediaInfo = job.mediaInfo
        staged.isProbing = false
        stagedFiles.add(staged)

        isConfigDialogOpen = true
    }

    fun confirmConvertNow() {
        val outDir = resolveOutputDirectory()
        val baseOptions = activeStagedFile?.options ?: ConversionOptions()

        scope.launch(Dispatchers.IO) {
            // Save last used options
            settingsRepo.updateConfig { it.copy(lastUsedOptions = baseOptions) }

            if (editingJobId != null) {
                // Editing an existing job
                queueManager.updateJobOptions(editingJobId!!, baseOptions)
                queueManager.startQueue()
            } else {
                // Staged files
                for (staged in stagedFiles) {
                    val opts = if (applyUniformlyToAll) baseOptions else staged.options
                    queueManager.addJob(
                        inputFile = staged.file,
                        outputDir = outDir,
                        options = opts,
                        autoStart = true
                    )
                }
            }
        }

        stagedFiles.clear()
        editingJobId = null
        isConfigDialogOpen = false
        activeScreen = Screen.QUEUE
    }

    fun confirmAddToQueue() {
        val outDir = resolveOutputDirectory()
        val baseOptions = activeStagedFile?.options ?: ConversionOptions()

        scope.launch(Dispatchers.IO) {
            // Save last used options
            settingsRepo.updateConfig { it.copy(lastUsedOptions = baseOptions) }

            if (editingJobId != null) {
                queueManager.updateJobOptions(editingJobId!!, baseOptions)
            } else {
                for (staged in stagedFiles) {
                    val opts = if (applyUniformlyToAll) baseOptions else staged.options
                    queueManager.addJob(
                        inputFile = staged.file,
                        outputDir = outDir,
                        options = opts,
                        autoStart = false
                    )
                }
            }
        }

        stagedFiles.clear()
        editingJobId = null
        isConfigDialogOpen = false
        activeScreen = Screen.QUEUE
    }

    fun cancelConfigDialog() {
        stagedFiles.clear()
        editingJobId = null
        isConfigDialogOpen = false
    }

    fun resetActiveStagedOptions() {
        val defaultOptions = profileRepo.getDefaultProfile().options
        activeStagedFile?.let { it.options = defaultOptions }
    }

    fun applyProfileToStaged(profile: PresetProfile) {
        if (applyUniformlyToAll) {
            for (staged in stagedFiles) {
                staged.options = profile.options
            }
        } else {
            activeStagedFile?.let { it.options = profile.options }
        }
    }

    fun saveCurrentOptionsAsPreset(name: String, description: String, category: ProfileCategory = ProfileCategory.CUSTOM) {
        val options = activeStagedFile?.options ?: selectedJob?.options ?: ConversionOptions()
        val newProfile = PresetProfile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            category = category,
            options = options,
            isBuiltIn = false
        )
        scope.launch {
            profileRepo.saveCustomProfile(newProfile)
        }
    }

    fun duplicatePreset(id: String) {
        scope.launch {
            profileRepo.duplicateCustomProfile(id)
        }
    }

    fun deletePreset(id: String) {
        scope.launch {
            profileRepo.deleteCustomProfile(id)
        }
    }

    fun renamePreset(id: String, newName: String, newDesc: String?) {
        scope.launch {
            profileRepo.renameCustomProfile(id, newName, newDesc)
        }
    }

    fun setDefaultPreset(id: String) {
        scope.launch {
            profileRepo.setDefaultProfile(id)
        }
    }

    fun exportPresets(targetFile: File) {
        scope.launch {
            profileRepo.exportProfilesToJson(targetFile)
        }
    }

    fun importPresets(sourceFile: File) {
        scope.launch {
            profileRepo.importProfilesFromJson(sourceFile)
        }
    }

    fun updateSelectedJobOptions(options: ConversionOptions) {
        val id = selectedJobId ?: return
        queueManager.updateJobOptions(id, options)
    }

    fun applyProfileToSelectedJob(profile: PresetProfile) {
        val id = selectedJobId ?: return
        queueManager.updateJobOptions(id, profile.options)
    }

    fun applyProfileToAllJobs(profile: PresetProfile) {
        for (job in jobs) {
            if (job.status == TaskStatus.PENDING) {
                queueManager.updateJobOptions(job.id, profile.options)
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(language = lang) }
        }
    }

    fun setTheme(theme: ThemeMode) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(themeMode = theme) }
        }
    }

    fun setMaxConcurrency(max: Int) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(maxConcurrentTasks = max) }
        }
    }

    fun setOutputDirectory(path: String) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(defaultOutputDirectory = path) }
        }
    }

    fun setHardwareEncoder(mode: HardwareEncoder) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(defaultHardwareEncoder = mode) }
        }
    }

    fun setDefaultCollisionBehavior(behavior: FileCollisionBehavior) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(defaultCollisionBehavior = behavior) }
        }
    }

    fun setDefaultNamingTemplate(template: String) {
        scope.launch {
            settingsRepo.updateConfig { it.copy(fileNamingTemplate = template) }
        }
    }

    private fun resolveOutputDirectory(): File {
        val path = if (config.defaultOutputDirectory.isNotBlank()) {
            config.defaultOutputDirectory
        } else {
            SettingsRepository.defaultOutputDirectory()
        }
        val dir = File(path)
        dir.mkdirs()
        return dir
    }
}
