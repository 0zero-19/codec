package io.codec.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.media.FFprobeAnalyzer
import io.codec.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.imageio.ImageIO

@Composable
fun MediaInspectorView(state: DesktopState, modifier: Modifier = Modifier) {
    val lang = state.config.language
    var mediaInfo by remember { mutableStateOf<MediaInfo?>(state.selectedJob?.mediaInfo) }
    var currentFile by remember { mutableStateOf<File?>(state.selectedJob?.let { File(it.inputPath) }) }
    var previewTimestamp by remember { mutableStateOf(1.0) }
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }

    fun loadPreview(time: Double) {
        val f = currentFile ?: return
        isLoadingPreview = true
        state.scope.launch {
            val tmp = File(System.getProperty("java.io.tmpdir"), "codec_preview_${System.currentTimeMillis()}.png")
            val res = state.tools.extractFrame(f, tmp, time)
            if (res.isSuccess) {
                withContext(Dispatchers.IO) {
                    try {
                        val img = ImageIO.read(tmp)
                        if (img != null) {
                            val baos = java.io.ByteArrayOutputStream()
                            ImageIO.write(img, "png", baos)
                            val skiaImg = Image.makeFromEncoded(baos.toByteArray())
                            previewBitmap = skiaImg.toComposeImageBitmap()
                        }
                    } catch (_: Exception) {}
                    tmp.delete()
                }
            }
            isLoadingPreview = false
        }
    }

    LaunchedEffect(currentFile) {
        if (currentFile != null) {
            val info = state.capabilitiesDetector.let { FFprobeAnalyzer().analyze(currentFile!!) }
            if (info.isSuccess) {
                mediaInfo = info.getOrThrow()
                loadPreview(1.0)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.get("nav_inspector", lang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentFile?.name ?: "No file loaded for inspection",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Inspect File", FileDialog.LOAD)
                    dialog.isVisible = true
                    dialog.file?.let {
                        currentFile = File(dialog.directory, it)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select File", fontSize = 12.sp)
            }
        }

        if (mediaInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a file or task to inspect stream properties and metadata", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val info = mediaInfo!!
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Left: Visual Frame Preview and Seeking
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = previewBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Video Frame",
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            } else if (isLoadingPreview) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            } else {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = CodecColors.PrimaryCyan, modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seek Slider
                    Text("Frame Preview Time: ${String.format("%.1f", previewTimestamp)}s / ${info.formattedDuration}", fontSize = 11.sp)
                    Slider(
                        value = previewTimestamp.toFloat(),
                        onValueChange = { previewTimestamp = it.toDouble() },
                        onValueChangeFinished = { loadPreview(previewTimestamp) },
                        valueRange = 0f..info.durationSeconds.toFloat().coerceAtLeast(1f)
                    )
                }

                // Right: Detailed Stream Information
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Container Card
                    InfoCard(title = "Container Format") {
                        InfoRow("Format", "${info.formatName} (${info.formatLongName})")
                        InfoRow("File Size", info.formattedSize)
                        InfoRow("Duration", info.formattedDuration)
                        InfoRow("Overall Bitrate", "${info.overallBitRate / 1000} kbps")
                    }

                    // Video Stream Card
                    info.primaryVideoStream?.let { v ->
                        InfoCard(title = "Primary Video Stream") {
                            InfoRow("Codec", "${v.codecName} (${v.codecLongName})")
                            InfoRow("Resolution", "${v.width} x ${v.height}")
                            InfoRow("Pixel Format", v.pixelFormat ?: "N/A")
                            InfoRow("Framerate", "${String.format("%.2f", v.frameRate)} fps")
                            InfoRow("Aspect Ratio", v.displayAspectRatio ?: "N/A")
                            InfoRow("Color Space", v.colorSpace ?: "Default")
                            InfoRow("HDR Capable", if (v.isHdr) "Yes (HDR10/HLG)" else "No (SDR)")
                        }
                    }

                    // Audio Stream Card
                    info.primaryAudioStream?.let { a ->
                        InfoCard(title = "Primary Audio Stream") {
                            InfoRow("Codec", "${a.codecName} (${a.codecLongName})")
                            InfoRow("Sample Rate", "${a.sampleRate} Hz")
                            InfoRow("Channels", "${a.channels} (${a.channelLayout ?: "Stereo"})")
                            InfoRow("Bitrate", "${(a.bitRate ?: 0) / 1000} kbps")
                            InfoRow("Language", a.language ?: "Undetermined")
                        }
                    }

                    // Metadata Card
                    if (info.tags.isNotEmpty()) {
                        InfoCard(title = "Tags & Metadata") {
                            info.tags.forEach { (k, v) ->
                                InfoRow(k, v)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CodecColors.PrimaryCyan)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
