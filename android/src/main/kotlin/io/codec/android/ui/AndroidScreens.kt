package io.codec.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.model.*
import io.codec.queue.TaskQueueManager

@Composable
fun AndroidQueueScreen(
    jobs: List<TaskJob>,
    queueManager: TaskQueueManager,
    onPickFiles: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Task Queue (${jobs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
                IconButton(onClick = { queueManager.startQueue() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start All", tint = AndroidColors.AccentGreen)
                }
                IconButton(onClick = { queueManager.pauseQueue() }) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause All")
                }
                IconButton(onClick = { queueManager.clearCompletedJobs() }) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear")
                }
            }
        }

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickFiles),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = AndroidColors.PrimaryCyan, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No media in queue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap to select audio or video files via SAF", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onPickFiles) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Media Files")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    AndroidJobCard(
                        job = job,
                        onPause = { queueManager.pauseJob(job.id) },
                        onResume = { queueManager.resumeJob(job.id) },
                        onCancel = { queueManager.cancelJob(job.id) },
                        onRetry = { queueManager.retryJob(job.id) },
                        onDelete = { queueManager.removeJob(job.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AndroidJobCard(
    job: TaskJob,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.inputFileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${job.options.video.container.extension.uppercase()} (${job.options.video.codec.displayName})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AndroidStatusBadge(job.status)
            }

            if (job.status == TaskStatus.RUNNING || job.status == TaskStatus.PAUSED || job.status == TaskStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (job.progress / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (job.status == TaskStatus.COMPLETED) AndroidColors.AccentGreen else AndroidColors.PrimaryCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format("%.1f", job.progress)}% • ${String.format("%.1f", job.speedMultiplier)}x",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ETA ${job.formattedEta}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (job.status) {
                    TaskStatus.RUNNING -> {
                        IconButton(onClick = onPause) { Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null, tint = AndroidColors.AccentRed, modifier = Modifier.size(18.dp)) }
                    }
                    TaskStatus.PAUSED -> {
                        IconButton(onClick = onResume) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AndroidColors.AccentGreen, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null, tint = AndroidColors.AccentRed, modifier = Modifier.size(18.dp)) }
                    }
                    TaskStatus.FAILED, TaskStatus.CANCELLED -> {
                        IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, contentDescription = null, tint = AndroidColors.PrimaryCyan, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    }
                    TaskStatus.COMPLETED, TaskStatus.PENDING, TaskStatus.PROBING -> {
                        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun AndroidStatusBadge(status: TaskStatus) {
    val (color, label) = when (status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant to "Queued"
        TaskStatus.PROBING -> AndroidColors.PrimaryCyan to "Probing"
        TaskStatus.RUNNING -> AndroidColors.AccentGreen to "Encoding"
        TaskStatus.PAUSED -> AndroidColors.AccentOrange to "Paused"
        TaskStatus.COMPLETED -> AndroidColors.AccentGreen to "Done"
        TaskStatus.FAILED -> AndroidColors.AccentRed to "Failed"
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant to "Cancelled"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AndroidPresetsScreen(
    presets: List<PresetProfile>,
    onApplyToAll: (PresetProfile) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ProfileCategory?>(null) }
    val filtered = remember(presets, selectedCategory) {
        if (selectedCategory == null) presets else presets.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Encoding Presets (${filtered.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "HandBrake-grade profiles tuned for quality, speed & compatibility",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All", fontSize = 11.sp) }
            )
            ProfileCategory.entries.take(4).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.displayName, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { preset ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                preset.options.video.container.extension.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AndroidColors.PrimaryCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${preset.options.video.codec.displayName} • ${preset.options.audio.codec.displayName} • CRF ${preset.options.video.crf}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = { onApplyToAll(preset) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AndroidToolsScreen(
    onPickVideoForTool: (toolType: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Media Utilities",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Quick operations for audio extraction, GIF creation, trim, and remuxing",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        AndroidToolCard(
            title = "Fast Audio Extractor",
            description = "Extract pristine MP3, AAC, FLAC or WAV without re-encoding video streams",
            icon = Icons.Default.Audiotrack,
            onClick = { onPickVideoForTool("audio_extract") }
        )

        AndroidToolCard(
            title = "Animated GIF Maker",
            description = "Generate 2-pass palette-optimized animated GIFs from video fragments",
            icon = Icons.Default.Animation,
            onClick = { onPickVideoForTool("gif") }
        )

        AndroidToolCard(
            title = "Video Trimmer",
            description = "Cut start and end timestamps rapidly with direct stream copy",
            icon = Icons.Default.ContentCut,
            onClick = { onPickVideoForTool("trim") }
        )

        AndroidToolCard(
            title = "Lossless Container Remux",
            description = "Switch container format (MP4, MKV, MOV, WebM) instantaneously without quality loss",
            icon = Icons.Default.Transform,
            onClick = { onPickVideoForTool("remux") }
        )
    }
}

@Composable
private fun AndroidToolCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AndroidColors.PrimaryCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AndroidColors.PrimaryCyan, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AndroidSettingsScreen(
    currentThreads: Int,
    onThreadsChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Application Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance & Background Processing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Concurrent encoding tasks: $currentThreads",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = currentThreads.toFloat(),
                    onValueChange = { onThreadsChange(it.toInt().coerceIn(1, 4)) },
                    valueRange = 1f..4f,
                    steps = 2
                )
                Text(
                    "Running on foreground service with persistent progress notification.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Engine Capabilities", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• FFmpeg engine: Embedded local binary / MediaCodec", fontSize = 12.sp)
                Text("• Cloud upload: NONE (100% offline & private)", fontSize = 12.sp, color = AndroidColors.AccentGreen)
                Text("• Hardware Acceleration: Device MediaCodec API", fontSize = 12.sp)
                Text("• Supported containers: MP4, MKV, WebM, MOV, MP3, AAC, FLAC, WAV, OGG, Opus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About Codec", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version: 1.0.0 (HandBrake / FFmpeg architecture)", fontSize = 12.sp)
                Text("Open-source media processing for Windows, Linux & Android.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
