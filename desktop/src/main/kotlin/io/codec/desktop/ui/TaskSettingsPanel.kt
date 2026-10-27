package io.codec.desktop.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.media.FFmpegCommandBuilder
import io.codec.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSettingsPanel(
    state: DesktopState,
    modifier: Modifier = Modifier
) {
    val job = state.selectedJob
    val lang = state.config.language

    if (job == null) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(380.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.get("no_jobs", lang),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    val options = job.options
    val video = options.video
    val audio = options.audio
    val filter = options.filter
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(0) } // 0: Video & Size, 1: Audio, 2: Filters, 3: More

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(380.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Title & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = Strings.get("settings_panel_title", lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = job.inputFileName,
                    fontSize = 11.sp,
                    color = CodecColors.PrimaryCyan,
                    maxLines = 1
                )
            }

            Row {
                IconButton(
                    onClick = { state.openConfigForExistingJob(job) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                }
                TextButton(
                    onClick = {
                        val p = state.profileRepo.getDefaultProfile()
                        state.applyProfileToSelectedJob(p)
                    }
                ) {
                    Text(Strings.get("btn_reset_settings", lang), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Quick Carousel (from state.profiles)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.profiles.take(3).forEach { p ->
                AssistChip(
                    onClick = { state.applyProfileToSelectedJob(p) },
                    label = { Text(p.name.substringBefore("/").substringBefore("(").trim(), fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Selector
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Video", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Audio", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Filters", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("More", fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Settings Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                0 -> VideoTabContent(state, job, options, video, lang)
                1 -> AudioTabContent(state, job, options, audio, lang)
                2 -> FiltersTabContent(state, job, options, filter, lang)
                3 -> AdvancedTabContent(state, job, options, lang)
            }
        }
    }
}

@Composable
private fun VideoTabContent(
    state: DesktopState,
    job: TaskJob,
    options: ConversionOptions,
    video: VideoSettings,
    lang: AppLanguage
) {
    // Container Format
    Text(Strings.get("container", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(ContainerFormat.MP4, ContainerFormat.MKV, ContainerFormat.WEBM, ContainerFormat.MOV, ContainerFormat.MP3).forEach { format ->
            FilterChip(
                selected = video.container == format,
                onClick = {
                    val supportedV = format.supportedVideoCodecs()
                    val newVCodec = if (supportedV.contains(video.codec)) video.codec else supportedV.first()
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(container = format, codec = newVCodec))
                    )
                },
                label = { Text(format.extension.uppercase(), fontSize = 11.sp) }
            )
        }
    }

    if (video.container.isAudioOnly) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(
                text = "Audio-only container. Video disabled.",
                fontSize = 11.sp,
                color = CodecColors.PrimaryCyan,
                modifier = Modifier.padding(8.dp)
            )
        }
        return
    }

    // Video Codec
    Text(Strings.get("video_codec", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val availableCodecs = video.container.supportedVideoCodecs()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        availableCodecs.forEach { codec ->
            FilterChip(
                selected = video.codec == codec,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(codec = codec))
                    )
                },
                label = { Text(codec.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // Compression Mode
    Text(Strings.get("compression_mode", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompressionPresetMode.entries.forEach { mode ->
            FilterChip(
                selected = video.compressionMode == mode,
                onClick = {
                    val crf = when (mode) {
                        CompressionPresetMode.MAX_QUALITY -> 18
                        CompressionPresetMode.BALANCED -> 22
                        CompressionPresetMode.MIN_SIZE -> 28
                        CompressionPresetMode.CUSTOM -> video.crf
                    }
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(compressionMode = mode, crf = crf))
                    )
                },
                label = { Text(mode.displayName.substringBefore(" "), fontSize = 10.sp) }
            )
        }
    }

    // Quality Mode
    Text(Strings.get("quality_mode", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = video.qualityMode == QualityMode.CRF,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(qualityMode = QualityMode.CRF))
                    )
                }
            )
            Text(Strings.get("quality_crf", lang), fontSize = 12.sp)
        }
        if (video.qualityMode == QualityMode.CRF) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = video.crf.toFloat(),
                    onValueChange = {
                        state.updateSelectedJobOptions(
                            options.copy(video = video.copy(crf = it.toInt()))
                        )
                    },
                    valueRange = 0f..51f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("CRF ${video.crf}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = video.qualityMode == QualityMode.TARGET_SIZE,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(qualityMode = QualityMode.TARGET_SIZE, targetSizeMb = 25.0))
                    )
                }
            )
            Text(Strings.get("quality_target_size", lang), fontSize = 12.sp)
        }
        if (video.qualityMode == QualityMode.TARGET_SIZE) {
            val targetMb = video.targetSizeMb ?: 25.0
            val estBitrate = FFmpegCommandBuilder().calculateTargetBitrateKbps(options, job.mediaInfo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = targetMb.toFloat(),
                    onValueChange = {
                        state.updateSelectedJobOptions(
                            options.copy(video = video.copy(targetSizeMb = it.toDouble()))
                        )
                    },
                    valueRange = 5f..500f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${targetMb.toInt()} MB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                text = "${Strings.get("calculated_bitrate", lang)}: ~${estBitrate} kbps",
                fontSize = 11.sp,
                color = CodecColors.PrimaryCyan
            )
        }
    }

    // Resolution Presets
    Text(Strings.get("resolution", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(ResolutionPreset.ORIGINAL, ResolutionPreset.P1080, ResolutionPreset.P720, ResolutionPreset.P480).forEach { res ->
            FilterChip(
                selected = video.resolutionPreset == res,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(resolutionPreset = res))
                    )
                },
                label = { Text(res.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // Encoding Speed
    Text(Strings.get("preset_speed", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(PresetSpeed.VERYFAST, PresetSpeed.FAST, PresetSpeed.MEDIUM, PresetSpeed.SLOW).forEach { speed ->
            FilterChip(
                selected = video.preset == speed,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(preset = speed))
                    )
                },
                label = { Text(speed.value, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun AudioTabContent(
    state: DesktopState,
    job: TaskJob,
    options: ConversionOptions,
    audio: AudioSettings,
    lang: AppLanguage
) {
    // Audio Codec
    Text(Strings.get("audio_codec", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val availableCodecs = options.video.container.supportedAudioCodecs()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        availableCodecs.forEach { codec ->
            FilterChip(
                selected = audio.codec == codec,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(audio = audio.copy(codec = codec))
                    )
                },
                label = { Text(codec.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // Audio Bitrate
    Text(Strings.get("audio_bitrate", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(96, 128, 192, 256, 320).forEach { br ->
            FilterChip(
                selected = audio.bitrateKbps == br,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(audio = audio.copy(bitrateKbps = br))
                    )
                },
                label = { Text("$br k", fontSize = 11.sp) }
            )
        }
    }

    // Channels
    Text(Strings.get("channels", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(ChannelMode.STEREO, ChannelMode.MONO, ChannelMode.KEEP_SOURCE).forEach { ch ->
            FilterChip(
                selected = audio.channels == ch,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(audio = audio.copy(channels = ch))
                    )
                },
                label = { Text(ch.displayName.substringBefore(" "), fontSize = 11.sp) }
            )
        }
    }

    // Audio Filters
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = audio.normalizeLoudness,
            onCheckedChange = {
                state.updateSelectedJobOptions(
                    options.copy(audio = audio.copy(normalizeLoudness = it))
                )
            }
        )
        Text(Strings.get("loudnorm", lang), fontSize = 12.sp)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = audio.stripAudio,
            onCheckedChange = {
                state.updateSelectedJobOptions(
                    options.copy(audio = audio.copy(stripAudio = it))
                )
            }
        )
        Text(Strings.get("strip_audio", lang), fontSize = 12.sp)
    }
}

@Composable
private fun FiltersTabContent(
    state: DesktopState,
    job: TaskJob,
    options: ConversionOptions,
    filter: FilterSettings,
    lang: AppLanguage
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = filter.deinterlace,
            onCheckedChange = {
                state.updateSelectedJobOptions(
                    options.copy(filter = filter.copy(deinterlace = it))
                )
            }
        )
        Text(Strings.get("deinterlace", lang), fontSize = 12.sp)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = filter.denoise,
            onCheckedChange = {
                state.updateSelectedJobOptions(
                    options.copy(filter = filter.copy(denoise = it))
                )
            }
        )
        Text(Strings.get("denoise", lang), fontSize = 12.sp)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = filter.sharpen,
            onCheckedChange = {
                state.updateSelectedJobOptions(
                    options.copy(filter = filter.copy(sharpen = it))
                )
            }
        )
        Text(Strings.get("sharpen", lang), fontSize = 12.sp)
    }

    Text(Strings.get("rotation_title", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0, 90, 180, 270).forEach { deg ->
            FilterChip(
                selected = filter.rotate == deg,
                onClick = {
                    state.updateSelectedJobOptions(
                        options.copy(filter = filter.copy(rotate = deg))
                    )
                },
                label = { Text("$deg°", fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun AdvancedTabContent(
    state: DesktopState,
    job: TaskJob,
    options: ConversionOptions,
    lang: AppLanguage
) {
    Text(Strings.get("tab_advanced", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = options.stripMetadata,
            onCheckedChange = {
                state.updateSelectedJobOptions(options.copy(stripMetadata = it))
            }
        )
        Text(Strings.get("strip_metadata", lang), fontSize = 12.sp)
    }

    OutlinedTextField(
        value = options.customFfmpegArgs ?: "",
        onValueChange = {
            state.updateSelectedJobOptions(options.copy(customFfmpegArgs = it))
        },
        label = { Text(Strings.get("custom_ffmpeg_args", lang), fontSize = 11.sp) },
        placeholder = { Text("-movflags +faststart", fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
    )
}
