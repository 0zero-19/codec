package io.codec.desktop.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.model.AudioCodec
import io.codec.model.ContainerFormat
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun ToolsView(state: DesktopState, modifier: Modifier = Modifier) {
    val lang = state.config.language
    var selectedTool by remember { mutableStateOf(0) } // 0: Trim, 1: GIF, 2: Audio Extract, 3: Remux

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = Strings.get("nav_tools", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Specialized media utilities for rapid stream manipulation",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tool Selector Tabs
        TabRow(selectedTabIndex = selectedTool) {
            Tab(selected = selectedTool == 0, onClick = { selectedTool = 0 }) {
                Text(Strings.get("tools_trim_title", lang), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            Tab(selected = selectedTool == 1, onClick = { selectedTool = 1 }) {
                Text(Strings.get("tools_gif_title", lang), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            Tab(selected = selectedTool == 2, onClick = { selectedTool = 2 }) {
                Text(Strings.get("tools_audio_title", lang), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            Tab(selected = selectedTool == 3, onClick = { selectedTool = 3 }) {
                Text(Strings.get("tools_remux_title", lang), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedTool) {
            0 -> TrimmerTool(state)
            1 -> GifMakerTool(state)
            2 -> AudioExtractorTool(state)
            3 -> RemuxerTool(state)
        }
    }
}

@Composable
private fun TrimmerTool(state: DesktopState) {
    var inputFile by remember { mutableStateOf<File?>(null) }
    var startSec by remember { mutableStateOf("0.0") }
    var endSec by remember { mutableStateOf("10.0") }
    var streamCopy by remember { mutableStateOf(true) }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SelectFileButton(inputFile?.name ?: "Select Video File to Trim") { file ->
            inputFile = file
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = startSec,
                onValueChange = { startSec = it },
                label = { Text("Start Time (seconds)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endSec,
                onValueChange = { endSec = it },
                label = { Text("End Time (seconds)") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = streamCopy, onCheckedChange = { streamCopy = it })
            Text("Lossless stream copy (Zero quality loss, instant export)", fontSize = 12.sp)
        }

        Button(
            onClick = {
                val f = inputFile ?: return@Button
                val s = startSec.toDoubleOrNull() ?: 0.0
                val e = endSec.toDoubleOrNull() ?: 10.0
                val out = File(f.parentFile, "${f.nameWithoutExtension}_trimmed.${f.extension}")
                state.scope.launch {
                    statusMsg = "Trimming in progress..."
                    val res = state.tools.trim(f, out, s, e, streamCopy)
                    statusMsg = if (res.isSuccess) "Saved: ${out.name}" else "Error: ${res.exceptionOrNull()?.message}"
                }
            },
            enabled = inputFile != null
        ) {
            Text("Trim & Export")
        }

        if (statusMsg.isNotBlank()) {
            Text(statusMsg, color = CodecColors.PrimaryCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GifMakerTool(state: DesktopState) {
    var inputFile by remember { mutableStateOf<File?>(null) }
    var startSec by remember { mutableStateOf("0.0") }
    var durationSec by remember { mutableStateOf("5.0") }
    var fps by remember { mutableStateOf(15f) }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SelectFileButton(inputFile?.name ?: "Select Video File for GIF") { file ->
            inputFile = file
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = startSec,
                onValueChange = { startSec = it },
                label = { Text("Start Time (seconds)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = durationSec,
                onValueChange = { durationSec = it },
                label = { Text("Duration (seconds)") },
                modifier = Modifier.weight(1f)
            )
        }

        Text("GIF Framerate: ${fps.toInt()} FPS", fontSize = 12.sp)
        Slider(value = fps, onValueChange = { fps = it }, valueRange = 10f..30f)

        Button(
            onClick = {
                val f = inputFile ?: return@Button
                val s = startSec.toDoubleOrNull() ?: 0.0
                val d = durationSec.toDoubleOrNull() ?: 5.0
                val out = File(f.parentFile, "${f.nameWithoutExtension}.gif")
                state.scope.launch {
                    statusMsg = "Generating 2-pass palette GIF..."
                    val res = state.tools.createGif(f, out, s, d, fps.toInt())
                    statusMsg = if (res.isSuccess) "Saved: ${out.name}" else "Finished: ${res.exceptionOrNull()?.message}"
                }
            },
            enabled = inputFile != null
        ) {
            Text("Create Animated GIF")
        }

        if (statusMsg.isNotBlank()) {
            Text(statusMsg, color = CodecColors.PrimaryCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AudioExtractorTool(state: DesktopState) {
    var inputFile by remember { mutableStateOf<File?>(null) }
    var targetFormat by remember { mutableStateOf(ContainerFormat.MP3) }
    var bitrateKbps by remember { mutableStateOf(320) }
    var losslessCopy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SelectFileButton(inputFile?.name ?: "Select Video to Extract Audio From") { file ->
            inputFile = file
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = losslessCopy, onCheckedChange = { losslessCopy = it })
            Text("Stream Copy (Direct lossless stream dump without re-encoding)", fontSize = 12.sp)
        }

        if (!losslessCopy) {
            Text("Target Audio Format", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ContainerFormat.MP3, ContainerFormat.AAC, ContainerFormat.FLAC, ContainerFormat.OPUS).forEach { fmt ->
                    FilterChip(
                        selected = targetFormat == fmt,
                        onClick = { targetFormat = fmt },
                        label = { Text(fmt.extension.uppercase()) }
                    )
                }
            }
        }

        Button(
            onClick = {
                val f = inputFile ?: return@Button
                val ext = if (losslessCopy) "m4a" else targetFormat.extension
                val out = File(f.parentFile, "${f.nameWithoutExtension}_audio.$ext")
                state.scope.launch {
                    statusMsg = "Extracting audio..."
                    val codec = when (targetFormat) {
                        ContainerFormat.MP3 -> AudioCodec.MP3
                        ContainerFormat.FLAC -> AudioCodec.FLAC
                        ContainerFormat.OPUS -> AudioCodec.OPUS
                        else -> AudioCodec.AAC
                    }
                    val res = state.tools.extractAudio(f, out, losslessCopy, codec, bitrateKbps)
                    statusMsg = if (res.isSuccess) "Extracted: ${out.name}" else "Error: ${res.exceptionOrNull()?.message}"
                }
            },
            enabled = inputFile != null
        ) {
            Text("Extract Audio Track")
        }

        if (statusMsg.isNotBlank()) {
            Text(statusMsg, color = CodecColors.PrimaryCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RemuxerTool(state: DesktopState) {
    var inputFile by remember { mutableStateOf<File?>(null) }
    var targetContainer by remember { mutableStateOf(ContainerFormat.MKV) }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SelectFileButton(inputFile?.name ?: "Select Media File to Remux") { file ->
            inputFile = file
        }

        Text("Target Container Format (Lossless Remux)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ContainerFormat.MP4, ContainerFormat.MKV, ContainerFormat.MOV, ContainerFormat.WEBM).forEach { fmt ->
                FilterChip(
                    selected = targetContainer == fmt,
                    onClick = { targetContainer = fmt },
                    label = { Text(fmt.extension.uppercase()) }
                )
            }
        }

        Button(
            onClick = {
                val f = inputFile ?: return@Button
                val out = File(f.parentFile, "${f.nameWithoutExtension}_remux.${targetContainer.extension}")
                state.scope.launch {
                    statusMsg = "Remuxing container..."
                    val res = state.tools.remux(f, out)
                    statusMsg = if (res.isSuccess) "Remuxed successfully: ${out.name}" else "Error: ${res.exceptionOrNull()?.message}"
                }
            },
            enabled = inputFile != null
        ) {
            Text("Remux Container")
        }

        if (statusMsg.isNotBlank()) {
            Text(statusMsg, color = CodecColors.PrimaryCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SelectFileButton(title: String, onSelected: (File) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Button(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Select File", FileDialog.LOAD)
                    dialog.isVisible = true
                    dialog.file?.let {
                        onSelected(File(dialog.directory, it))
                    }
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Browse", fontSize = 11.sp)
            }
        }
    }
}
