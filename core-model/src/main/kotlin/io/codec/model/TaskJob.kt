package io.codec.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskJob(
    val id: String,
    val inputPath: String,
    val outputPath: String,
    val mediaInfo: MediaInfo? = null,
    val options: ConversionOptions = ConversionOptions(),
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val progress: Double = 0.0,
    val currentFps: Double = 0.0,
    val speedMultiplier: Double = 0.0,
    val etaSeconds: Long = 0,
    val processedTimeMs: Long = 0,
    val totalDurationMs: Long = 0,
    val outputSizeBytes: Long = 0,
    val currentPass: Int = 1,
    val totalPasses: Int = 1,
    val errorMessage: String? = null,
    val ffmpegLog: List<String> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis(),
    val startedAtMs: Long? = null,
    val finishedAtMs: Long? = null
) {
    val inputFileName: String
        get() = inputPath.substringAfterLast('/').substringAfterLast('\\')

    val outputFileName: String
        get() = outputPath.substringAfterLast('/').substringAfterLast('\\')

    val formattedEta: String
        get() {
            if (etaSeconds <= 0) return "--:--"
            val minutes = etaSeconds / 60
            val seconds = etaSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedOutputSize: String
        get() {
            val mb = outputSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
