package io.codec.desktop.ui

import androidx.compose.foundation.background
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
                .width(360.dp)
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

    var activeTab by remember { mutableStateOf(0) } // 0: Video & Size, 1: Audio, 2: Filters & Subs, 3: Expert

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(380.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Strings.get("settings_panel_title", lang),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Apply preset button
            TextButton(
                onClick = {
                    val p = state.profiles.firstOrNull { it.id == "builtin-fast-1080p" }
                    if (p != null) state.applyProfileToSelectedJob(p)
                }
            ) {
                Text("Reset", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset Chips Carousel / Selection
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = {
                    state.profiles.firstOrNull { it.id == "builtin-fast-1080p" }?.let {
                        state.applyProfileToSelectedJob(it)
                    }
                },
                label = { Text("Fast 1080p", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = {
                    state.profiles.firstOrNull { it.id == "builtin-discord-25mb" }?.let {
                        state.applyProfileToSelectedJob(it)
                    }
                },
                label = { Text("Discord 25M", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = {
                    state.profiles.firstOrNull { it.id == "builtin-telegram-video" }?.let {
                        state.applyProfileToSelectedJob(it)
                    }
                },
                label = { Text("Telegram", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    state.updateSelectedJobOptions(
                        options.copy(video = video.copy(container = format))
                    )
                },
                label = { Text(format.extension.uppercase(), fontSize = 11.sp) }
            )
        }
    }

    // Video Codec
    Text(Strings.get("video_codec", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(VideoCodec.H264, VideoCodec.H265, VideoCodec.AV1, VideoCodec.VP9, VideoCodec.COPY).forEach { codec ->
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
                text = "Calc bitrate: ~${estBitrate} kbps",
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
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(AudioCodec.AAC, AudioCodec.MP3, AudioCodec.OPUS, AudioCodec.FLAC, AudioCodec.COPY).forEach { codec ->
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

    Text(Strings.get("rotate", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    Text(Strings.get("section_advanced", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

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
