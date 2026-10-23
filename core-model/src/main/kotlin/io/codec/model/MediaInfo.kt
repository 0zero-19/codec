package io.codec.model

import kotlinx.serialization.Serializable

@Serializable
enum class StreamType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    ATTACHMENT,
    DATA
}

@Serializable
data class VideoStreamInfo(
    val index: Int,
    val codecName: String,
    val codecLongName: String = "",
    val profile: String? = null,
    val level: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sampleAspectRatio: String? = null,
    val displayAspectRatio: String? = null,
    val pixelFormat: String? = null,
    val frameRate: Double = 0.0,
    val avgFrameRate: String? = null,
    val bitRate: Long? = null,
    val durationSeconds: Double? = null,
    val colorSpace: String? = null,
    val colorTransfer: String? = null,
    val colorPrimaries: String? = null,
    val isHdr: Boolean = false,
    val bitsPerRawSample: Int? = null,
    val tags: Map<String, String> = emptyMap()
)

@Serializable
data class AudioStreamInfo(
    val index: Int,
    val codecName: String,
    val codecLongName: String = "",
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val channelLayout: String? = null,
    val bitRate: Long? = null,
    val durationSeconds: Double? = null,
    val language: String? = null,
    val title: String? = null,
    val tags: Map<String, String> = emptyMap()
)

@Serializable
data class SubtitleStreamInfo(
    val index: Int,
    val codecName: String,
    val language: String? = null,
    val title: String? = null,
    val isForced: Boolean = false,
    val isDefault: Boolean = false,
    val tags: Map<String, String> = emptyMap()
)

@Serializable
data class MediaInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val formatName: String,
    val formatLongName: String = "",
    val durationSeconds: Double = 0.0,
    val overallBitRate: Long = 0,
    val videoStreams: List<VideoStreamInfo> = emptyList(),
    val audioStreams: List<AudioStreamInfo> = emptyList(),
    val subtitleStreams: List<SubtitleStreamInfo> = emptyList(),
    val tags: Map<String, String> = emptyMap()
) {
    val primaryVideoStream: VideoStreamInfo? get() = videoStreams.firstOrNull()
    val primaryAudioStream: AudioStreamInfo? get() = audioStreams.firstOrNull()

    val formattedDuration: String
        get() {
            val totalSec = durationSeconds.toLong()
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val resolutionLabel: String
        get() {
            val v = primaryVideoStream ?: return "Audio Only"
            return "${v.width}x${v.height}"
        }
}
