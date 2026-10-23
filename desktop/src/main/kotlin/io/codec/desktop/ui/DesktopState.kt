package io.codec.desktop.ui

import androidx.compose.runtime.*
import io.codec.media.FFmpegCapabilities
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

class DesktopState(
    val scope: CoroutineScope,
    val queueManager: TaskQueueManager = TaskQueueManager(),
    val profileRepo: ProfileRepository = ProfileRepository(),
    val settingsRepo: SettingsRepository = SettingsRepository(),
    val capabilitiesDetector: FFmpegCapabilities = FFmpegCapabilities(),
    val tools: MediaTools = MediaTools()
) {
    var activeScreen by mutableStateOf(Screen.QUEUE)
    var selectedJobId by mutableStateOf<String?>(null)
    var activeJobForLogs by mutableStateOf<TaskJob?>(null)
    var inspectedFile by mutableStateOf<File?>(null)
    var inspectedMediaInfo by mutableStateOf<MediaInfo?>(null)

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
        // Folder selection
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
                    .take(200)
                    .toList()

                if (foundFiles.isNotEmpty()) {
                    addInputFiles(foundFiles)
                }
            }
        }
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
    }

    fun addInputFiles(files: List<File>) {
        val outDir = if (config.defaultOutputDirectory.isNotBlank()) {
            File(config.defaultOutputDirectory)
        } else {
            File(SettingsRepository.defaultOutputDirectory())
        }
        outDir.mkdirs()

        scope.launch(Dispatchers.IO) {
            // Apply default profile or standard options
            val defaultProfile = profiles.firstOrNull { it.id == "builtin-fast-1080p" }
            val options = defaultProfile?.options ?: ConversionOptions()
            queueManager.addBatchJobs(files, outDir, options)
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
}
