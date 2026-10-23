package io.codec.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContainerFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val isAudioOnly: Boolean = false,
    val isAnimatedImage: Boolean = false
) {
    MP4("mp4", "video/mp4", "MP4 (MPEG-4 Part 14)"),
    MKV("mkv", "video/x-matroska", "MKV (Matroska)"),
    WEBM("webm", "video/webm", "WebM"),
    MOV("mov", "video/quicktime", "MOV (QuickTime)"),
    AVI("avi", "video/x-msvideo", "AVI"),
    WMV("wmv", "video/x-ms-wmv", "WMV"),
    FLV("flv", "video/x-flv", "FLV (Flash Video)"),
    TS("ts", "video/mp2t", "MPEG-TS"),
    M4V("m4v", "video/x-m4v", "M4V (Apple Video)"),
    OGV("ogv", "video/ogg", "OGV (Ogg Video)"),
    MP3("mp3", "audio/mpeg", "MP3 Audio", isAudioOnly = true),
    AAC("aac", "audio/aac", "AAC Audio", isAudioOnly = true),
    M4A("m4a", "audio/mp4", "M4A (Apple Audio)", isAudioOnly = true),
    FLAC("flac", "audio/flac", "FLAC Lossless", isAudioOnly = true),
    WAV("wav", "audio/wav", "WAV (Uncompressed)", isAudioOnly = true),
    OGG("ogg", "audio/ogg", "OGG Vorbis", isAudioOnly = true),
    OPUS("opus", "audio/opus", "Opus Audio", isAudioOnly = true),
    WMA("wma", "audio/x-ms-wma", "WMA Audio", isAudioOnly = true),
    AIFF("aiff", "audio/x-aiff", "AIFF Audio", isAudioOnly = true),
    GIF("gif", "image/gif", "GIF Animation", isAnimatedImage = true),
    APNG("apng", "image/apng", "APNG Animation", isAnimatedImage = true);

    companion object {
        fun fromExtension(ext: String): ContainerFormat {
            val clean = ext.trimStart('.').lowercase()
            return entries.firstOrNull { it.extension == clean } ?: MP4
        }
    }
}

@Serializable
enum class VideoCodec(
    val encoderName: String,
    val displayName: String,
    val description: String
) {
    H264("libx264", "H.264 / AVC", "Widely compatible, excellent balance"),
    H265("libx265", "H.265 / HEVC", "Higher compression efficiency than H.264"),
    AV1("libsvtav1", "AV1 (SVT-AV1)", "Modern open standard, superior compression"),
    VP8("libvpx", "VP8", "Google WebM open video codec"),
    VP9("libvpx-vp9", "VP9", "Efficient open video for YouTube & WebM"),
    MPEG4("mpeg4", "MPEG-4 Part 2", "Legacy compatibility for older devices"),
    PRORES("prores_ks", "Apple ProRes", "High-quality intermediate editing codec"),
    COPY("copy", "Stream Copy (No Re-encode)", "Lossless remuxing at blazing speed"),
    NONE("none", "No Video", "Strip video track")
}

@Serializable
enum class AudioCodec(
    val encoderName: String,
    val displayName: String,
    val description: String
) {
    AAC("aac", "AAC", "Standard high-efficiency audio"),
    MP3("libmp3lame", "MP3 (LAME)", "Universal compatibility"),
    OPUS("libopus", "Opus", "State of the art speech and music codec"),
    FLAC("flac", "FLAC", "Lossless audio compression"),
    WAV_PCM("pcm_s16le", "PCM 16-bit (WAV)", "Uncompressed studio quality"),
    VORBIS("libvorbis", "Ogg Vorbis", "Open source audio codec"),
    ALAC("alac", "ALAC", "Apple Lossless Audio"),
    AC3("ac3", "AC3 (Dolby Digital)", "Multi-channel surround audio"),
    COPY("copy", "Stream Copy (No Re-encode)", "Lossless stream copy"),
    NONE("none", "No Audio", "Strip audio track")
}

@Serializable
enum class HardwareEncoder(
    val id: String,
    val displayName: String,
    val h264Encoder: String,
    val hevcEncoder: String,
    val av1Encoder: String? = null
) {
    AUTO("auto", "Auto Detect", "h264_nvenc", "hevc_nvenc", "av1_nvenc"),
    NVENC("nvenc", "NVIDIA NVENC", "h264_nvenc", "hevc_nvenc", "av1_nvenc"),
    QSV("qsv", "Intel Quick Sync Video (QSV)", "h264_qsv", "hevc_qsv", "av1_qsv"),
    AMF("amf", "AMD AMF", "h264_amf", "hevc_amf", "av1_amf"),
    VAAPI("vaapi", "Linux VA-API", "h264_vaapi", "hevc_vaapi", null),
    MEDIACODEC("mediacodec", "Android MediaCodec", "h264_mediacodec", "hevc_mediacodec", null),
    SOFTWARE("software", "Software (CPU)", "libx264", "libx265", "libsvtav1")
}

@Serializable
enum class QualityMode {
    CRF,
    BITRATE,
    TWO_PASS,
    TARGET_SIZE
}

@Serializable
enum class PresetSpeed(val value: String) {
    ULTRAFAST("ultrafast"),
    SUPERFAST("superfast"),
    VERYFAST("veryfast"),
    FASTER("faster"),
    FAST("fast"),
    MEDIUM("medium"),
    SLOW("slow"),
    SLOWER("slower"),
    VERYSLOW("veryslow")
}

@Serializable
enum class Tune(val value: String) {
    NONE("none"),
    FILM("film"),
    ANIMATION("animation"),
    GRAIN("grain"),
    STILLIMAGE("stillimage"),
    FASTDECODE("fastdecode"),
    ZEROLATENCY("zerolatency")
}

@Serializable
enum class FramerateMode {
    SAME_AS_SOURCE,
    CFR,
    VFR
}

@Serializable
enum class ChannelMode(val channels: Int, val displayName: String) {
    KEEP_SOURCE(-1, "Original Channels"),
    MONO(1, "Mono (1.0)"),
    STEREO(2, "Stereo (2.0)"),
    SURROUND_5_1(6, "5.1 Surround"),
    SURROUND_7_1(8, "7.1 Surround")
}

@Serializable
enum class ResolutionPreset(val width: Int, val height: Int, val displayName: String) {
    ORIGINAL(0, 0, "Original"),
    P360(640, 360, "360p (640x360)"),
    P480(854, 480, "480p (854x480)"),
    P720(1280, 720, "720p HD (1280x720)"),
    P1080(1920, 1080, "1080p Full HD (1920x1080)"),
    P1440(2560, 1440, "1440p 2K QHD (2560x1440)"),
    P4K(3840, 2160, "4K UHD (3840x2160)"),
    P8K(7680, 4320, "8K UHD (7680x4320)"),
    CUSTOM(-1, -1, "Custom Resolution")
}

@Serializable
enum class SubtitleMode {
    NONE,
    EMBED_TRACK,
    BURN_IN
}

@Serializable
enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

@Serializable
enum class TaskStatus {
    PENDING,
    PROBING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
