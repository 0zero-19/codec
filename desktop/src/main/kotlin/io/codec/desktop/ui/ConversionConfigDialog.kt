package io.codec.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.window.Dialog
import io.codec.media.FFmpegCommandBuilder
import io.codec.model.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionConfigDialog(state: DesktopState) {
    if (!state.isConfigDialogOpen) return

    val lang = state.config.language
    val stagedFiles = state.stagedFiles
    val currentStaged = state.activeStagedFile ?: return
    val currentOptions = currentStaged.options
    val mediaInfo = currentStaged.mediaInfo

    var activeTab by remember { mutableStateOf(0) } // 0: Video & Size, 1: Audio, 2: Filters & Trimming, 3: Output & File, 4: Advanced

    Dialog(onDismissRequest = { state.cancelConfigDialog() }) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(1080.dp)
                .height(780.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("config_dialog_title", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Strings.get("config_dialog_subtitle", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { state.cancelConfigDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multiple Files Bar (if more than 1 file)
                if (stagedFiles.size > 1) {
                    MultipleFilesSelectorBar(state = state)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Source File Information vs Target Preview Panel
                SourceAndTargetSummaryCard(
                    staged = currentStaged,
                    options = currentOptions,
                    mediaInfo = mediaInfo,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Settings Tabs Row
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                        Text(Strings.get("tab_video", lang), fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                        Text(Strings.get("tab_audio", lang), fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                        Text(Strings.get("tab_filters", lang), fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                        Text(Strings.get("tab_output", lang), fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                        Text(Strings.get("tab_advanced", lang), fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Configuration Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeTab) {
                            0 -> VideoAndQualityTab(currentStaged, currentOptions, mediaInfo, lang, state)
                            1 -> AudioAndTracksTab(currentStaged, currentOptions, mediaInfo, lang)
                            2 -> FiltersAndTrimmingTab(currentStaged, currentOptions, mediaInfo, lang)
                            3 -> OutputAndNamingTab(currentStaged, currentOptions, lang)
                            4 -> AdvancedAndExpertTab(currentStaged, currentOptions, lang)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { state.resetActiveStagedOptions() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("btn_reset_settings", lang), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { state.isSavePresetDialogOpen = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("btn_save_as_preset", lang), fontSize = 12.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { state.cancelConfigDialog() }) {
                            Text(Strings.get("cancel", lang), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { state.confirmAddToQueue() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("btn_add_to_queue", lang), fontSize = 12.sp)
                        }

                        Button(
                            onClick = { state.confirmConvertNow() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CodecColors.PrimaryCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("btn_convert_now", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (state.isSavePresetDialogOpen) {
        SavePresetDialog(state = state, onDismiss = { state.isSavePresetDialogOpen = false })
    }
}

@Composable
private fun MultipleFilesSelectorBar(state: DesktopState) {
    val lang = state.config.language
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = "${Strings.get("file_selected_count", lang)} (${state.stagedFiles.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))

                val outlineColor = MaterialTheme.colorScheme.outline
                val surfaceColor = MaterialTheme.colorScheme.surface
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.stagedFiles.forEachIndexed { index, item ->
                        val isSelected = index == state.activeStagedIndex
                        val chipBorder = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CodecColors.PrimaryCyan else outlineColor.copy(alpha = 0.4f)
                        )
                        AssistChip(
                            onClick = { state.activeStagedIndex = index },
                            label = {
                                Text(
                                    text = item.file.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) CodecColors.PrimaryCyan.copy(alpha = 0.2f) else surfaceColor
                            ),
                            border = chipBorder
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.applyUniformlyToAll,
                    onCheckedChange = { state.applyUniformlyToAll = it }
                )
                Text(
                    text = Strings.get("btn_apply_to_all", lang),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SourceAndTargetSummaryCard(
    staged: StagedMediaFile,
    options: ConversionOptions,
    mediaInfo: MediaInfo?,
    lang: AppLanguage
) {
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
                // Source info block
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Source, contentDescription = null, tint = CodecColors.PrimaryCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.get("source_info_title", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (staged.isProbing) {
                        Text("Analyzing file properties...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (mediaInfo != null) {
                        val video = mediaInfo.primaryVideoStream
                        val audio = mediaInfo.primaryAudioStream
                        Text(
                            text = "${mediaInfo.fileName} (${mediaInfo.formattedSize}, ${mediaInfo.formattedDuration})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Video: ${video?.codecName?.uppercase() ?: "None"} ${mediaInfo.resolutionLabel} @ ${String.format("%.1f", video?.frameRate ?: 0.0)} FPS | Audio: ${audio?.codecName?.uppercase() ?: "None"} (${mediaInfo.audioStreams.size} tracks)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(staged.file.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Arrow
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))

                // Target Preview block
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Output, contentDescription = null, tint = CodecColors.AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.get("target_info_title", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CodecColors.AccentGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val targetRes = if (options.video.resolutionPreset == ResolutionPreset.ORIGINAL) {
                        mediaInfo?.resolutionLabel ?: "Original"
                    } else if (options.video.resolutionPreset == ResolutionPreset.CUSTOM) {
                        "${options.video.customWidth ?: 0}x${options.video.customHeight ?: 0}"
                    } else {
                        options.video.resolutionPreset.displayName.substringBefore(" ")
                    }

                    val targetFps = if (options.video.framerateMode == FramerateMode.SAME_AS_SOURCE) {
                        "Source FPS"
                    } else {
                        "${options.video.targetFps ?: 30} FPS"
                    }

                    val estBitrate = FFmpegCommandBuilder().calculateTargetBitrateKbps(options, mediaInfo)
                    val estSizeMb = if (options.video.qualityMode == QualityMode.TARGET_SIZE && options.video.targetSizeMb != null) {
                        "~${options.video.targetSizeMb} MB"
                    } else {
                        val dur = mediaInfo?.durationSeconds?.takeIf { it > 0 } ?: 60.0
                        val estTotalKbps = estBitrate + options.audio.bitrateKbps
                        val estMb = (estTotalKbps * dur) / (8 * 1024)
                        "~${String.format("%.1f", estMb)} MB"
                    }

                    Text(
                        text = "${options.video.container.extension.uppercase()} | $estSizeMb (approx)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Video: ${options.video.codec.displayName.substringBefore(" ")} ($targetRes, $targetFps) | Audio: ${options.audio.codec.displayName.substringBefore(" ")} (${options.audio.bitrateKbps}k)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Compatibility Warning if any
            val warning = options.video.container.getCompatibilityWarning(options.video.codec, options.audio.codec)
            if (warning != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CodecColors.AccentOrange.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CodecColors.AccentOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = warning, fontSize = 11.sp, color = CodecColors.AccentOrange)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoAndQualityTab(
    staged: StagedMediaFile,
    options: ConversionOptions,
    mediaInfo: MediaInfo?,
    lang: AppLanguage,
    state: DesktopState
) {
    val video = options.video
    val isAudioOnly = video.container.isAudioOnly

    // 1. Output Format & Container
    Text(Strings.get("container", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ContainerFormat.MP4, ContainerFormat.MKV, ContainerFormat.WEBM, ContainerFormat.MOV,
            ContainerFormat.AVI, ContainerFormat.MP3, ContainerFormat.AAC, ContainerFormat.FLAC,
            ContainerFormat.WAV, ContainerFormat.GIF
        ).forEach { format ->
            FilterChip(
                selected = video.container == format,
                onClick = {
                    val supportedV = format.supportedVideoCodecs()
                    val supportedA = format.supportedAudioCodecs()
                    val newVCodec = if (supportedV.contains(video.codec)) video.codec else supportedV.first()
                    val newACodec = if (supportedA.contains(options.audio.codec)) options.audio.codec else supportedA.first()
                    staged.options = options.copy(
                        video = video.copy(container = format, codec = newVCodec),
                        audio = options.audio.copy(codec = newACodec)
                    )
                },
                label = { Text(format.extension.uppercase(), fontSize = 11.sp) }
            )
        }
    }

    if (isAudioOnly) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(
                text = "Audio-only format selected (${video.container.displayName}). Video tracks will be stripped.",
                fontSize = 11.sp,
                color = CodecColors.PrimaryCyan,
                modifier = Modifier.padding(10.dp)
            )
        }
        return
    }

    // 2. Video Codec
    Text(Strings.get("video_codec", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val availableVideoCodecs = video.container.supportedVideoCodecs()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        availableVideoCodecs.forEach { codec ->
            FilterChip(
                selected = video.codec == codec,
                onClick = {
                    staged.options = options.copy(video = video.copy(codec = codec))
                },
                label = { Text(codec.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // 3. Compression Mode
    Text(Strings.get("compression_mode", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompressionPresetMode.entries.forEach { mode ->
            FilterChip(
                selected = video.compressionMode == mode,
                onClick = {
                    val newCrf = when (mode) {
                        CompressionPresetMode.MAX_QUALITY -> 18
                        CompressionPresetMode.BALANCED -> 22
                        CompressionPresetMode.MIN_SIZE -> 28
                        CompressionPresetMode.CUSTOM -> video.crf
                    }
                    staged.options = options.copy(
                        video = video.copy(
                            compressionMode = mode,
                            crf = newCrf,
                            qualityMode = if (mode == CompressionPresetMode.CUSTOM) video.qualityMode else QualityMode.CRF
                        )
                    )
                },
                label = { Text(mode.displayName, fontSize = 11.sp) }
            )
        }
    }

    // 4. Rate Control & CRF / Bitrate / Target Size
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Strings.get("quality_mode", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = video.qualityMode == QualityMode.CRF,
                        onClick = { staged.options = options.copy(video = video.copy(qualityMode = QualityMode.CRF)) }
                    )
                    Text(Strings.get("quality_crf", lang), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = video.qualityMode == QualityMode.BITRATE,
                        onClick = { staged.options = options.copy(video = video.copy(qualityMode = QualityMode.BITRATE)) }
                    )
                    Text(Strings.get("quality_bitrate", lang), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = video.qualityMode == QualityMode.TARGET_SIZE,
                        onClick = {
                            val defaultMb = video.targetSizeMb ?: 25.0
                            staged.options = options.copy(video = video.copy(qualityMode = QualityMode.TARGET_SIZE, targetSizeMb = defaultMb))
                        }
                    )
                    Text(Strings.get("quality_target_size", lang), fontSize = 12.sp)
                }
            }

            when (video.qualityMode) {
                QualityMode.CRF -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = video.crf.toFloat(),
                            onValueChange = { staged.options = options.copy(video = video.copy(crf = it.toInt())) },
                            valueRange = 0f..51f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("CRF ${video.crf}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (video.crf > 28) {
                        Text(Strings.get("warning_quality_loss", lang), fontSize = 10.sp, color = CodecColors.AccentOrange)
                    }
                }

                QualityMode.BITRATE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = video.videoBitrateKbps.toFloat(),
                            onValueChange = { staged.options = options.copy(video = video.copy(videoBitrateKbps = it.toInt())) },
                            valueRange = 300f..25000f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${video.videoBitrateKbps} kbps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                QualityMode.TARGET_SIZE, QualityMode.TWO_PASS -> {
                    val targetMb = video.targetSizeMb ?: 25.0
                    val estBitrate = FFmpegCommandBuilder().calculateTargetBitrateKbps(options, mediaInfo)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = targetMb.toFloat(),
                            onValueChange = { staged.options = options.copy(video = video.copy(targetSizeMb = it.toDouble())) },
                            valueRange = 5f..1000f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${targetMb.toInt()} MB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "${Strings.get("calculated_bitrate", lang)}: ~${estBitrate} kbps",
                        fontSize = 11.sp,
                        color = CodecColors.PrimaryCyan
                    )
                }
            }
        }
    }

    // 5. Resolution & Dimensions
    Text(Strings.get("resolution_preset", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ResolutionPreset.ORIGINAL, ResolutionPreset.P1080, ResolutionPreset.P720,
            ResolutionPreset.P480, ResolutionPreset.CUSTOM
        ).forEach { res ->
            FilterChip(
                selected = video.resolutionPreset == res,
                onClick = {
                    staged.options = options.copy(video = video.copy(resolutionPreset = res))
                },
                label = { Text(res.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    if (video.resolutionPreset == ResolutionPreset.CUSTOM) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = video.customWidth?.toString() ?: "",
                onValueChange = {
                    val w = it.toIntOrNull()
                    staged.options = options.copy(video = video.copy(customWidth = w))
                },
                label = { Text(Strings.get("custom_width", lang), fontSize = 11.sp) },
                modifier = Modifier.width(140.dp)
            )
            OutlinedTextField(
                value = video.customHeight?.toString() ?: "",
                onValueChange = {
                    val h = it.toIntOrNull()
                    staged.options = options.copy(video = video.copy(customHeight = h))
                },
                label = { Text(Strings.get("custom_height", lang), fontSize = 11.sp) },
                modifier = Modifier.width(140.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = video.keepAspectRatio,
                    onCheckedChange = { staged.options = options.copy(video = video.copy(keepAspectRatio = it)) }
                )
                Text(Strings.get("keep_aspect_ratio", lang), fontSize = 11.sp)
            }
        }
    }

    // 6. Framerate & Preset Speed
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Strings.get("framerate_mode", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.0 to "Source", 24.0 to "24", 30.0 to "30", 60.0 to "60").forEach { (fpsVal, label) ->
                    val isSel = if (fpsVal == 0.0) video.framerateMode == FramerateMode.SAME_AS_SOURCE else video.targetFps == fpsVal
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            if (fpsVal == 0.0) {
                                staged.options = options.copy(video = video.copy(framerateMode = FramerateMode.SAME_AS_SOURCE, targetFps = null))
                            } else {
                                staged.options = options.copy(video = video.copy(framerateMode = FramerateMode.CFR, targetFps = fpsVal))
                            }
                        },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(Strings.get("preset_speed", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(PresetSpeed.VERYFAST, PresetSpeed.FAST, PresetSpeed.MEDIUM, PresetSpeed.SLOW).forEach { speed ->
                    FilterChip(
                        selected = video.preset == speed,
                        onClick = { staged.options = options.copy(video = video.copy(preset = speed)) },
                        label = { Text(speed.value, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioAndTracksTab(
    staged: StagedMediaFile,
    options: ConversionOptions,
    mediaInfo: MediaInfo?,
    lang: AppLanguage
) {
    val audio = options.audio

    // 1. Audio Codec
    Text(Strings.get("audio_codec", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val availableCodecs = options.video.container.supportedAudioCodecs()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        availableCodecs.forEach { codec ->
            FilterChip(
                selected = audio.codec == codec,
                onClick = { staged.options = options.copy(audio = audio.copy(codec = codec)) },
                label = { Text(codec.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // 2. Audio Bitrate
    if (audio.codec != AudioCodec.COPY && audio.codec != AudioCodec.FLAC && audio.codec != AudioCodec.WAV_PCM) {
        Text(Strings.get("audio_bitrate", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(64, 96, 128, 192, 256, 320).forEach { br ->
                FilterChip(
                    selected = audio.bitrateKbps == br,
                    onClick = { staged.options = options.copy(audio = audio.copy(bitrateKbps = br)) },
                    label = { Text("$br k", fontSize = 11.sp) }
                )
            }
        }
    }

    // 3. Audio Stream Selection (if source has audio streams)
    val streams = mediaInfo?.audioStreams ?: emptyList()
    if (streams.size > 1) {
        Text(Strings.get("audio_track_selection", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = audio.selectedAudioStreamIndex == null,
                    onClick = { staged.options = options.copy(audio = audio.copy(selectedAudioStreamIndex = null)) }
                )
                Text(Strings.get("audio_track_original", lang), fontSize = 12.sp)
            }

            streams.forEach { s ->
                val trackName = buildString {
                    append("Track #${s.index + 1}: ${s.codecName.uppercase()}")
                    if (!s.language.isNullOrBlank()) append(" [${s.language}]")
                    if (!s.title.isNullOrBlank()) append(" - ${s.title}")
                    append(" (${s.channels} ch)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = audio.selectedAudioStreamIndex == s.index,
                        onClick = { staged.options = options.copy(audio = audio.copy(selectedAudioStreamIndex = s.index)) }
                    )
                    Text(trackName, fontSize = 11.sp)
                }
            }
        }
    }

    // 4. Channels & Volume
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Strings.get("channels", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ChannelMode.STEREO, ChannelMode.MONO, ChannelMode.KEEP_SOURCE).forEach { ch ->
                    FilterChip(
                        selected = audio.channels == ch,
                        onClick = { staged.options = options.copy(audio = audio.copy(channels = ch)) },
                        label = { Text(ch.displayName.substringBefore(" "), fontSize = 11.sp) }
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("${Strings.get("volume_multiplier", lang)}: ${String.format("%.1f", audio.volumeMultiplier)}x", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Slider(
                value = audio.volumeMultiplier,
                onValueChange = { staged.options = options.copy(audio = audio.copy(volumeMultiplier = it)) },
                valueRange = 0.2f..2.5f
            )
        }
    }

    // 5. Loudnorm & Mute
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = audio.normalizeLoudness,
                onCheckedChange = { staged.options = options.copy(audio = audio.copy(normalizeLoudness = it)) }
            )
            Text(Strings.get("loudnorm", lang), fontSize = 12.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = audio.stripAudio,
                onCheckedChange = { staged.options = options.copy(audio = audio.copy(stripAudio = it)) }
            )
            Text(Strings.get("strip_audio", lang), fontSize = 12.sp)
        }
    }
}

@Composable
private fun FiltersAndTrimmingTab(
    staged: StagedMediaFile,
    options: ConversionOptions,
    mediaInfo: MediaInfo?,
    lang: AppLanguage
) {
    val filter = options.filter

    // Trimming
    Text(Strings.get("trimming_title", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = options.trimStartTimeSeconds?.toString() ?: "",
            onValueChange = {
                val sec = it.toDoubleOrNull()
                staged.options = options.copy(trimStartTimeSeconds = sec)
            },
            label = { Text(Strings.get("trim_start", lang), fontSize = 11.sp) },
            modifier = Modifier.width(160.dp)
        )
        OutlinedTextField(
            value = options.trimEndTimeSeconds?.toString() ?: "",
            onValueChange = {
                val sec = it.toDoubleOrNull()
                staged.options = options.copy(trimEndTimeSeconds = sec)
            },
            label = { Text(Strings.get("trim_end", lang), fontSize = 11.sp) },
            modifier = Modifier.width(160.dp)
        )
    }

    // Rotation & Flip
    Text(Strings.get("rotation_title", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to Strings.get("rotate_0", lang), 90 to Strings.get("rotate_90", lang), 180 to Strings.get("rotate_180", lang), 270 to Strings.get("rotate_270", lang)).forEach { (deg, label) ->
            FilterChip(
                selected = filter.rotate == deg,
                onClick = { staged.options = options.copy(filter = filter.copy(rotate = deg)) },
                label = { Text(label, fontSize = 11.sp) }
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.hFlip,
                onCheckedChange = { staged.options = options.copy(filter = filter.copy(hFlip = it)) }
            )
            Text(Strings.get("flip_h", lang), fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.vFlip,
                onCheckedChange = { staged.options = options.copy(filter = filter.copy(vFlip = it)) }
            )
            Text(Strings.get("flip_v", lang), fontSize = 11.sp)
        }
    }

    // Filters
    Text("Video Processing Filters", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.deinterlace,
                onCheckedChange = { staged.options = options.copy(filter = filter.copy(deinterlace = it)) }
            )
            Text(Strings.get("deinterlace", lang), fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.denoise,
                onCheckedChange = { staged.options = options.copy(filter = filter.copy(denoise = it)) }
            )
            Text(Strings.get("denoise", lang), fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.sharpen,
                onCheckedChange = { staged.options = options.copy(filter = filter.copy(sharpen = it)) }
            )
            Text(Strings.get("sharpen", lang), fontSize = 11.sp)
        }
    }
}

@Composable
private fun OutputAndNamingTab(
    staged: StagedMediaFile,
    options: ConversionOptions,
    lang: AppLanguage
) {
    // 1. Output Folder
    Text(Strings.get("output_directory", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = options.outputDirectory ?: "",
            onValueChange = { staged.options = options.copy(outputDirectory = it) },
            placeholder = { Text("Default Export Folder") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                val dialog = FileDialog(null as Frame?, "Choose Destination Folder", FileDialog.LOAD)
                dialog.isVisible = true
                dialog.directory?.let {
                    staged.options = options.copy(outputDirectory = it)
                }
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(Strings.get("settings_browse", lang), fontSize = 12.sp)
        }
    }

    // 2. Naming Template
    Text(Strings.get("naming_template", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = options.outputFileNameTemplate,
        onValueChange = { staged.options = options.copy(outputFileNameTemplate = it) },
        modifier = Modifier.fillMaxWidth()
    )
    Text(Strings.get("naming_hint", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    // 3. Collision Behavior
    Text(Strings.get("collision_behavior", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = options.collisionBehavior == FileCollisionBehavior.UNIQUE_NAME,
                onClick = { staged.options = options.copy(collisionBehavior = FileCollisionBehavior.UNIQUE_NAME) }
            )
            Text(Strings.get("collision_unique", lang), fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = options.collisionBehavior == FileCollisionBehavior.OVERWRITE,
                onClick = { staged.options = options.copy(collisionBehavior = FileCollisionBehavior.OVERWRITE) }
            )
            Text(Strings.get("collision_overwrite", lang), fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = options.collisionBehavior == FileCollisionBehavior.SKIP,
                onClick = { staged.options = options.copy(collisionBehavior = FileCollisionBehavior.SKIP) }
            )
            Text(Strings.get("collision_skip", lang), fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdvancedAndExpertTab(
    staged: StagedMediaFile,
    options: ConversionOptions,
    lang: AppLanguage
) {
    Text(Strings.get("tab_advanced", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = options.stripMetadata,
            onCheckedChange = { staged.options = options.copy(stripMetadata = it) }
        )
        Text(Strings.get("strip_metadata", lang), fontSize = 12.sp)
    }

    OutlinedTextField(
        value = options.customFfmpegArgs ?: "",
        onValueChange = { staged.options = options.copy(customFfmpegArgs = it) },
        label = { Text(Strings.get("custom_ffmpeg_args", lang), fontSize = 11.sp) },
        placeholder = { Text("-movflags +faststart -pix_fmt yuv420p") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SavePresetDialog(state: DesktopState, onDismiss: () -> Unit) {
    val lang = state.config.language
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProfileCategory.CUSTOM) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(420.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.get("create_preset", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("preset_name_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("preset_desc_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                state.saveCurrentOptionsAsPreset(name, description, category)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Strings.get("save", lang))
                    }
                }
            }
        }
    }
}
