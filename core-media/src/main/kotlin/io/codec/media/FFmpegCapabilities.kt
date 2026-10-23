package io.codec.media

import io.codec.model.HardwareEncoder
import io.codec.model.VideoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SystemCapabilities(
    val ffmpegVersion: String,
    val availableEncoders: Set<String>,
    val availableHwaccels: Set<String>,
    val supportedHardwareModes: List<HardwareEncoder>
)

class FFmpegCapabilities(
    private val ffmpegExecutable: String = findFFmpegExecutable()
) {
    private var cachedCapabilities: SystemCapabilities? = null

    suspend fun detectCapabilities(): SystemCapabilities = withContext(Dispatchers.IO) {
        cachedCapabilities?.let { return@withContext it }

        val encoders = mutableSetOf<String>()
        val hwaccels = mutableSetOf<String>()
        var version = "Unknown"

        try {
            // Check version
            val versionProcess = ProcessBuilder(listOf(ffmpegExecutable, "-version")).start()
            val firstLine = versionProcess.inputStream.bufferedReader().readLine() ?: ""
            versionProcess.waitFor()
            version = firstLine.substringBefore("Copyright").trim()

            // Check encoders
            val encProcess = ProcessBuilder(listOf(ffmpegExecutable, "-encoders", "-v", "quiet")).start()
            encProcess.inputStream.bufferedReader().forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("V") || trimmed.startsWith("A") || trimmed.startsWith("S")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        encoders.add(parts[1])
                    }
                }
            }
            encProcess.waitFor()

            // Check hwaccels
            val hwProcess = ProcessBuilder(listOf(ffmpegExecutable, "-hwaccels", "-v", "quiet")).start()
            hwProcess.inputStream.bufferedReader().forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("Hardware")) {
                    hwaccels.add(trimmed.lowercase())
                }
            }
            hwProcess.waitFor()
        } catch (_: Exception) {
        }

        val supportedHw = mutableListOf<HardwareEncoder>()
        supportedHw.add(HardwareEncoder.SOFTWARE)

        val hasNvenc = encoders.contains("h264_nvenc") || hwaccels.contains("cuda") || hwaccels.contains("nvdec")
        val hasQsv = encoders.contains("h264_qsv") || hwaccels.contains("qsv")
        val hasAmf = encoders.contains("h264_amf") || hwaccels.contains("amf")
        val hasVaapi = encoders.contains("h264_vaapi") || hwaccels.contains("vaapi")
        val hasMediaCodec = encoders.contains("h264_mediacodec") || hwaccels.contains("mediacodec")

        if (hasNvenc) supportedHw.add(0, HardwareEncoder.NVENC)
        if (hasQsv) supportedHw.add(0, HardwareEncoder.QSV)
        if (hasAmf) supportedHw.add(0, HardwareEncoder.AMF)
        if (hasVaapi) supportedHw.add(0, HardwareEncoder.VAAPI)
        if (hasMediaCodec) supportedHw.add(0, HardwareEncoder.MEDIACODEC)

        if (supportedHw.size > 1) {
            supportedHw.add(0, HardwareEncoder.AUTO)
        }

        val caps = SystemCapabilities(
            ffmpegVersion = version,
            availableEncoders = encoders,
            availableHwaccels = hwaccels,
            supportedHardwareModes = supportedHw
        )
        cachedCapabilities = caps
        caps
    }

    companion object {
        fun findFFmpegExecutable(): String {
            val os = System.getProperty("os.name").lowercase()
            val binaryName = if (os.contains("win")) "ffmpeg.exe" else "ffmpeg"

            val pathDirs = System.getenv("PATH")?.split(File.pathSeparator) ?: emptyList()
            for (dir in pathDirs) {
                val candidate = File(dir, binaryName)
                if (candidate.canExecute()) {
                    return candidate.absolutePath
                }
            }

            return binaryName
        }
    }
}
