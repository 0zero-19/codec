package io.codec.desktop.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.model.TaskJob
import io.codec.model.TaskStatus
import java.awt.Desktop
import java.io.File

@Composable
fun QueueView(
    state: DesktopState,
    modifier: Modifier = Modifier
) {
    val lang = state.config.language

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { state.queueManager.startQueue() },
                    colors = ButtonDefaults.buttonColors(containerColor = CodecColors.AccentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("start_queue", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { state.queueManager.pauseQueue() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("pause_queue", lang), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = { state.queueManager.clearCompletedJobs() }
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("clear_completed", lang), fontSize = 12.sp)
                }
            }

            // Task count & Concurrency summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.jobs.size} ${Strings.get("size", lang).lowercase()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                AssistChip(
                    onClick = {
                        val next = when (state.config.maxConcurrentTasks) {
                            1 -> 2
                            2 -> 4
                            4 -> 8
                            else -> 1
                        }
                        state.setMaxConcurrency(next)
                    },
                    label = {
                        Text("${state.config.maxConcurrentTasks} parallel", fontSize = 11.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }
        }

        // Job list or empty drop zone
        if (state.jobs.isEmpty()) {
            EmptyDropZone(state)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.jobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        isSelected = state.selectedJobId == job.id,
                        lang = lang,
                        onSelect = { state.selectedJobId = job.id },
                        onPause = { state.queueManager.pauseJob(job.id) },
                        onResume = { state.queueManager.resumeJob(job.id) },
                        onCancel = { state.queueManager.cancelJob(job.id) },
                        onRetry = { state.queueManager.retryJob(job.id) },
                        onDelete = { state.queueManager.removeJob(job.id) },
                        onViewLogs = { state.activeJobForLogs = job }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDropZone(state: DesktopState) {
    val lang = state.config.language

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { state.addFilesDialog() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = CodecColors.PrimaryCyan,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.get("drag_drop_hint", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Strings.get("drag_drop_sub", lang),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Button(
                    onClick = { state.addFilesDialog() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("add_files", lang), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { state.addFolderDialog() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("add_folder", lang), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun JobCard(
    job: TaskJob,
    isSelected: Boolean,
    lang: io.codec.model.AppLanguage,
    onSelect: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onViewLogs: () -> Unit
) {
    val borderColor = if (isSelected) CodecColors.PrimaryCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val cardBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name, Codec badges, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (job.options.video.container.isAudioOnly) Icons.Default.Audiotrack else Icons.Default.Movie,
                        contentDescription = null,
                        tint = CodecColors.PrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = job.inputFileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dur = job.mediaInfo?.formattedDuration ?: "--:--"
                            val res = job.mediaInfo?.resolutionLabel ?: ""
                            Text(
                                text = "$dur ${if (res.isNotBlank()) "• $res" else ""} → ${job.options.video.container.extension.uppercase()} (${job.options.video.codec.displayName})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                StatusBadge(job.status, lang)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar & Live Info (if running/paused/failed)
            if (job.status == TaskStatus.RUNNING || job.status == TaskStatus.PAUSED || job.status == TaskStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { (job.progress / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (job.status == TaskStatus.COMPLETED) CodecColors.AccentGreen else CodecColors.PrimaryCyan,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "${String.format("%.1f", job.progress)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (job.status == TaskStatus.RUNNING) {
                            Text(
                                text = "${Strings.get("speed", lang)}: ${String.format("%.1f", job.speedMultiplier)}x",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Strings.get("fps", lang)}: ${job.currentFps.toInt()}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Strings.get("eta", lang)}: ${job.formattedEta}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (job.outputSizeBytes > 0) {
                        Text(
                            text = "${Strings.get("size", lang)}: ${job.formattedOutputSize}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Error display if failed
            val errMsg = job.errorMessage
            if (job.status == TaskStatus.FAILED && errMsg != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CodecColors.AccentRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errMsg.take(150),
                        color = CodecColors.AccentRed,
                        fontSize = 11.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left tools: View logs
                TextButton(
                    onClick = onViewLogs,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.get("view_logs", lang), fontSize = 11.sp)
                }

                // Right buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (job.status) {
                        TaskStatus.RUNNING -> {
                            IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CodecColors.AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        TaskStatus.PAUSED -> {
                            IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CodecColors.AccentGreen, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CodecColors.AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                        TaskStatus.COMPLETED -> {
                            IconButton(
                                onClick = {
                                    val f = File(job.outputPath)
                                    if (f.exists()) {
                                        Desktop.getDesktop().open(f.parentFile)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        TaskStatus.FAILED, TaskStatus.CANCELLED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CodecColors.PrimaryCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        TaskStatus.PENDING, TaskStatus.PROBING -> {
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: TaskStatus, lang: io.codec.model.AppLanguage) {
    val (color, text) = when (status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant to Strings.get("status_pending", lang)
        TaskStatus.PROBING -> CodecColors.PrimaryCyan to Strings.get("status_probing", lang)
        TaskStatus.RUNNING -> CodecColors.AccentGreen to Strings.get("status_running", lang)
        TaskStatus.PAUSED -> CodecColors.AccentOrange to Strings.get("status_paused", lang)
        TaskStatus.COMPLETED -> CodecColors.AccentGreen to Strings.get("status_completed", lang)
        TaskStatus.FAILED -> CodecColors.AccentRed to Strings.get("status_failed", lang)
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant to Strings.get("status_cancelled", lang)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
