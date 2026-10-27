package io.codec.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoSettings(
    val codec: VideoCodec = VideoCodec.H264,
    val container: ContainerFormat = ContainerFormat.MP4,
    val hardwareEncoder: HardwareEncoder = HardwareEncoder.AUTO,
    val qualityMode: QualityMode = QualityMode.CRF,
    val compressionMode: CompressionPresetMode = CompressionPresetMode.BALANCED,
    val crf: Int = 23,
    val videoBitrateKbps: Int = 2500,
    val targetSizeMb: Double? = null,
    val twoPass: Boolean = false,
    val preset: PresetSpeed = PresetSpeed.MEDIUM,
    val tune: Tune = Tune.NONE,
    val profile: String? = null,
    val level: String? = null,
    val resolutionPreset: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val customWidth: Int? = null,
    val customHeight: Int? = null,
    val keepAspectRatio: Boolean = true,
    val framerateMode: FramerateMode = FramerateMode.SAME_AS_SOURCE,
    val targetFps: Double? = null,
    val gopSize: Int? = null,
    val bFrames: Int? = null,
    val pixelFormat: String? = "yuv420p",
    val hdrToSdrTonemap: Boolean = false,
    val selectedVideoStreamIndex: Int? = null
)

@Serializable
data class AudioSettings(
    val codec: AudioCodec = AudioCodec.AAC,
    val bitrateKbps: Int = 192,
    val sampleRateHz: Int = 48000,
    val channels: ChannelMode = ChannelMode.STEREO,
    val volumeMultiplier: Float = 1.0f,
    val normalizeLoudness: Boolean = false,
    val dynamicAudioNormalizer: Boolean = false,
    val stripAudio: Boolean = false,
    val selectedAudioStreamIndex: Int? = null,
    val audioTrackTitle: String? = null
)

@Serializable
data class FilterSettings(
    val cropEnabled: Boolean = false,
    val cropX: Int = 0,
    val cropY: Int = 0,
    val cropWidth: Int = 0,
    val cropHeight: Int = 0,
    val rotate: Int = 0, // 0, 90, 180, 270
    val hFlip: Boolean = false,
    val vFlip: Boolean = false,
    val deinterlace: Boolean = false,
    val denoise: Boolean = false,
    val sharpen: Boolean = false,
    val brightness: Float = 0.0f, // -1.0 to 1.0
    val contrast: Float = 1.0f,   // 0.0 to 2.0
    val saturation: Float = 1.0f, // 0.0 to 3.0
    val gamma: Float = 1.0f       // 0.1 to 10.0
)

@Serializable
data class SubtitleSettings(
    val mode: SubtitleMode = SubtitleMode.NONE,
    val externalSubtitlePath: String? = null,
    val selectedSubtitleStreamIndex: Int? = null
)

@Serializable
data class ConversionOptions(
    val video: VideoSettings = VideoSettings(),
    val audio: AudioSettings = AudioSettings(),
    val filter: FilterSettings = FilterSettings(),
    val subtitle: SubtitleSettings = SubtitleSettings(),
    val trimStartTimeSeconds: Double? = null,
    val trimEndTimeSeconds: Double? = null,
    val customFfmpegArgs: String? = null,
    val stripMetadata: Boolean = false,
    val collisionBehavior: FileCollisionBehavior = FileCollisionBehavior.UNIQUE_NAME,
    val outputFileNameTemplate: String = "{name}_converted.{ext}",
    val outputDirectory: String? = null
)
