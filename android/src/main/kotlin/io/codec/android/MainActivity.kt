package io.codec.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.codec.android.service.ConversionForegroundService
import io.codec.android.ui.AndroidQueueScreen
import io.codec.android.ui.CodecAndroidTheme
import io.codec.model.ConversionOptions
import io.codec.queue.TaskQueueManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var queueManager: TaskQueueManager
    private val jobsState = mutableStateListOf<io.codec.model.TaskJob>()

    private val pickFilesLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            handleSelectedUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storageDir = File(filesDir, ".codec")
        queueManager = TaskQueueManager(storageDir = storageDir)
        val profileRepository = io.codec.profiles.ProfileRepository(storageDir = storageDir)

        lifecycleScope.launch {
            queueManager.jobs.collectLatest { list ->
                jobsState.clear()
                jobsState.addAll(list)
                if (list.any { it.status == io.codec.model.TaskStatus.RUNNING }) {
                    ConversionForegroundService.start(this@MainActivity)
                }
            }
        }

        // Handle incoming shared media from other apps
        handleIncomingIntent(intent)

        setContent {
            var selectedTab by remember { mutableStateOf(0) }
            val presets by profileRepository.profiles.collectAsState(initial = emptyList())
            var concurrentLimit by remember { mutableStateOf(2) }

            CodecAndroidTheme {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Queue, contentDescription = null) },
                                label = { Text("Queue") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                label = { Text("Presets") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Handyman, contentDescription = null) },
                                label = { Text("Tools") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> AndroidQueueScreen(
                                jobs = jobsState,
                                queueManager = queueManager,
                                onPickFiles = { pickFilesLauncher.launch(arrayOf("video/*", "audio/*")) }
                            )
                            1 -> io.codec.android.ui.AndroidPresetsScreen(
                                presets = presets,
                                onApplyToAll = { preset ->
                                    jobsState.filter { it.status == io.codec.model.TaskStatus.PENDING }.forEach { job ->
                                        queueManager.updateJobOptions(job.id, preset.options)
                                    }
                                    selectedTab = 0
                                }
                            )
                            2 -> io.codec.android.ui.AndroidToolsScreen(
                                onPickVideoForTool = {
                                    pickFilesLauncher.launch(arrayOf("video/*", "audio/*"))
                                }
                            )
                            3 -> io.codec.android.ui.AndroidSettingsScreen(
                                currentThreads = concurrentLimit,
                                onThreadsChange = {
                                    concurrentLimit = it
                                    queueManager.setMaxConcurrentTasks(it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                handleSelectedUris(listOf(uri))
            }
        }
    }

    private fun handleSelectedUris(uris: List<Uri>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val outputDir = File(getExternalFilesDir(null) ?: filesDir, "Codec_Exports")
            outputDir.mkdirs()

            for (uri in uris) {
                val tempFile = copyUriToInternalCache(uri) ?: continue
                queueManager.addJob(
                    inputFile = tempFile,
                    outputDir = outputDir,
                    options = ConversionOptions()
                )
            }
        }
    }

    private suspend fun copyUriToInternalCache(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val name = resolveFileName(uri) ?: "imported_media_${System.currentTimeMillis()}.mp4"
            val cacheFile = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val colIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (colIdx >= 0) {
                    name = it.getString(colIdx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
