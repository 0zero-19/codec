package io.codec.media

data class ProgressUpdate(
    val frame: Long = 0,
    val fps: Double = 0.0,
    val bitrateKbps: Double = 0.0,
    val totalSizeBytes: Long = 0,
    val outTimeMs: Long = 0,
    val speedMultiplier: Double = 0.0,
    val isEnd: Boolean = false,
    val percentage: Double = 0.0,
    val etaSeconds: Long = 0
)

class FFmpegProgressParser(
    private val totalDurationMs: Long
) {
    private var currentFrame: Long = 0
    private var currentFps: Double = 0.0
    private var currentBitrateKbps: Double = 0.0
    private var currentTotalSizeBytes: Long = 0
    private var currentOutTimeMs: Long = 0
    private var currentSpeed: Double = 0.0

    fun parseLine(line: String): ProgressUpdate? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val eqIdx = trimmed.indexOf('=')
        if (eqIdx == -1) return null

        val key = trimmed.substring(0, eqIdx).trim()
        val value = trimmed.substring(eqIdx + 1).trim()

        when (key) {
            "frame" -> currentFrame = value.toLongOrNull() ?: currentFrame
            "fps" -> currentFps = value.toDoubleOrNull() ?: currentFps
            "bitrate" -> {
                val clean = value.replace("kbits/s", "").trim()
                currentBitrateKbps = clean.toDoubleOrNull() ?: currentBitrateKbps
            }
            "total_size" -> currentTotalSizeBytes = value.toLongOrNull() ?: currentTotalSizeBytes
            "out_time_us" -> {
                val us = value.toLongOrNull() ?: 0L
                currentOutTimeMs = us / 1000L
            }
            "out_time_ms" -> currentOutTimeMs = (value.toLongOrNull() ?: 0L) / 1000L
            "out_time" -> {
                if (currentOutTimeMs == 0L) {
                    currentOutTimeMs = parseTimeStringToMs(value)
                }
            }
            "speed" -> {
                val clean = value.replace("x", "").trim()
                currentSpeed = clean.toDoubleOrNull() ?: currentSpeed
            }
            "progress" -> {
                val isEnd = value == "end"
                val pct = if (totalDurationMs > 0) {
                    ((currentOutTimeMs.toDouble() / totalDurationMs.toDouble()) * 100.0).coerceIn(0.0, 100.0)
                } else if (isEnd) {
                    100.0
                } else {
                    0.0
                }

                val eta = if (currentSpeed > 0 && totalDurationMs > currentOutTimeMs) {
                    val remainingMs = totalDurationMs - currentOutTimeMs
                    ((remainingMs / 1000.0) / currentSpeed).toLong()
                } else {
                    0L
                }

                return ProgressUpdate(
                    frame = currentFrame,
                    fps = currentFps,
                    bitrateKbps = currentBitrateKbps,
                    totalSizeBytes = currentTotalSizeBytes,
                    outTimeMs = currentOutTimeMs,
                    speedMultiplier = currentSpeed,
                    isEnd = isEnd,
                    percentage = pct,
                    etaSeconds = eta
                )
            }
        }
        return null
    }

    private fun parseTimeStringToMs(timeStr: String): Long {
        return try {
            val parts = timeStr.split(':')
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toDouble()
                ((hours * 3600 + minutes * 60 + seconds) * 1000).toLong()
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }
}
