package io.codec.media

import io.codec.model.AudioStreamInfo
import io.codec.model.MediaInfo
import io.codec.model.SubtitleStreamInfo
import io.codec.model.VideoStreamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File

class FFprobeAnalyzer(
    private val ffprobeExecutable: String = findFFprobeExecutable()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun analyze(file: File): Result<MediaInfo> = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
        }

        try {
            val cmd = listOf(
                ffprobeExecutable,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                "-show_chapters",
                file.absolutePath
            )

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0 || output.isBlank()) {
                return@withContext Result.failure(
                    RuntimeException("ffprobe failed with exit code $exitCode: $output")
                )
            }

            val rootJson = json.parseToJsonElement(output).jsonObject
            val formatObj = rootJson["format"]?.jsonObject ?: JsonObject(emptyMap())
            val streamsArr = rootJson["streams"]?.jsonArray ?: JsonArray(emptyList())

            val durationSeconds = formatObj["duration"]?.jsonPrimitive?.doubleOrNull
                ?: parseDurationFallback(streamsArr)
            val overallBitRate = formatObj["bit_rate"]?.jsonPrimitive?.longOrNull ?: 0L
            val formatName = formatObj["format_name"]?.jsonPrimitive?.contentOrNull ?: ""
            val formatLongName = formatObj["format_long_name"]?.jsonPrimitive?.contentOrNull ?: ""

            val formatTags = formatObj["tags"]?.jsonObject?.mapValues {
                it.value.jsonPrimitive.contentOrNull ?: ""
            } ?: emptyMap()

            val videoStreams = mutableListOf<VideoStreamInfo>()
            val audioStreams = mutableListOf<AudioStreamInfo>()
            val subtitleStreams = mutableListOf<SubtitleStreamInfo>()

            for (element in streamsArr) {
                val sObj = element.jsonObject
                val codecType = sObj["codec_type"]?.jsonPrimitive?.contentOrNull?.lowercase()
                val index = sObj["index"]?.jsonPrimitive?.intOrNull ?: 0
                val codecName = sObj["codec_name"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                val codecLongName = sObj["codec_long_name"]?.jsonPrimitive?.contentOrNull ?: ""
                val streamTags = sObj["tags"]?.jsonObject?.mapValues {
                    it.value.jsonPrimitive.contentOrNull ?: ""
                } ?: emptyMap()

                when (codecType) {
                    "video" -> {
                        val width = sObj["width"]?.jsonPrimitive?.intOrNull ?: 0
                        val height = sObj["height"]?.jsonPrimitive?.intOrNull ?: 0
                        val profile = sObj["profile"]?.jsonPrimitive?.contentOrNull
                        val level = sObj["level"]?.jsonPrimitive?.contentOrNull
                        val pixFmt = sObj["pix_fmt"]?.jsonPrimitive?.contentOrNull
                        val sar = sObj["sample_aspect_ratio"]?.jsonPrimitive?.contentOrNull
                        val dar = sObj["display_aspect_ratio"]?.jsonPrimitive?.contentOrNull
                        val rFpsStr = sObj["r_frame_rate"]?.jsonPrimitive?.contentOrNull ?: ""
                        val avgFpsStr = sObj["avg_frame_rate"]?.jsonPrimitive?.contentOrNull ?: ""
                        val fps = parseFps(avgFpsStr).takeIf { it > 0 } ?: parseFps(rFpsStr)
                        val bitRate = sObj["bit_rate"]?.jsonPrimitive?.longOrNull
                        val duration = sObj["duration"]?.jsonPrimitive?.doubleOrNull
                        val colorSpace = sObj["color_space"]?.jsonPrimitive?.contentOrNull
                        val colorTransfer = sObj["color_transfer"]?.jsonPrimitive?.contentOrNull
                        val colorPrimaries = sObj["color_primaries"]?.jsonPrimitive?.contentOrNull
                        val bitsPerRawSample = sObj["bits_per_raw_sample"]?.jsonPrimitive?.intOrNull

                        val isHdr = (colorTransfer?.contains("smpte2084", ignoreCase = true) == true) ||
                                (colorTransfer?.contains("arib-std-b67", ignoreCase = true) == true) ||
                                (colorPrimaries?.contains("bt2020", ignoreCase = true) == true)

                        videoStreams.add(
                            VideoStreamInfo(
                                index = index,
                                codecName = codecName,
                                codecLongName = codecLongName,
                                profile = profile,
                                level = level,
                                width = width,
                                height = height,
                                sampleAspectRatio = sar,
                                displayAspectRatio = dar,
                                pixelFormat = pixFmt,
                                frameRate = fps,
                                avgFrameRate = avgFpsStr,
                                bitRate = bitRate,
                                durationSeconds = duration,
                                colorSpace = colorSpace,
                                colorTransfer = colorTransfer,
                                colorPrimaries = colorPrimaries,
                                isHdr = isHdr,
                                bitsPerRawSample = bitsPerRawSample,
                                tags = streamTags
                            )
                        )
                    }

                    "audio" -> {
                        val sampleRate = sObj["sample_rate"]?.jsonPrimitive?.intOrNull ?: 44100
                        val channels = sObj["channels"]?.jsonPrimitive?.intOrNull ?: 2
                        val channelLayout = sObj["channel_layout"]?.jsonPrimitive?.contentOrNull
                        val bitRate = sObj["bit_rate"]?.jsonPrimitive?.longOrNull
                        val duration = sObj["duration"]?.jsonPrimitive?.doubleOrNull
                        val language = streamTags["language"]
                        val title = streamTags["title"]

                        audioStreams.add(
                            AudioStreamInfo(
                                index = index,
                                codecName = codecName,
                                codecLongName = codecLongName,
                                sampleRate = sampleRate,
                                channels = channels,
                                channelLayout = channelLayout,
                                bitRate = bitRate,
                                durationSeconds = duration,
                                language = language,
                                title = title,
                                tags = streamTags
                            )
                        )
                    }

                    "subtitle" -> {
                        val language = streamTags["language"]
                        val title = streamTags["title"]
                        val disposition = sObj["disposition"]?.jsonObject
                        val isForced = disposition?.get("forced")?.jsonPrimitive?.intOrNull == 1
                        val isDefault = disposition?.get("default")?.jsonPrimitive?.intOrNull == 1

                        subtitleStreams.add(
                            SubtitleStreamInfo(
                                index = index,
                                codecName = codecName,
                                language = language,
                                title = title,
                                isForced = isForced,
                                isDefault = isDefault,
                                tags = streamTags
                            )
                        )
                    }
                }
            }

            val mediaInfo = MediaInfo(
                filePath = file.absolutePath,
                fileName = file.name,
                fileSize = file.length(),
                formatName = formatName,
                formatLongName = formatLongName,
                durationSeconds = durationSeconds,
                overallBitRate = overallBitRate,
                videoStreams = videoStreams,
                audioStreams = audioStreams,
                subtitleStreams = subtitleStreams,
                tags = formatTags
            )

            Result.success(mediaInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDurationFallback(streams: JsonArray): Double {
        for (element in streams) {
            val dur = element.jsonObject["duration"]?.jsonPrimitive?.doubleOrNull
            if (dur != null && dur > 0) return dur
        }
        return 0.0
    }

    private fun parseFps(fpsFraction: String): Double {
        if (fpsFraction.isBlank() || fpsFraction == "0/0") return 0.0
        val parts = fpsFraction.split('/')
        return if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull() ?: 0.0
            val den = parts[1].toDoubleOrNull() ?: 1.0
            if (den != 0.0) num / den else 0.0
        } else {
            fpsFraction.toDoubleOrNull() ?: 0.0
        }
    }

    companion object {
        fun findFFprobeExecutable(): String {
            val os = System.getProperty("os.name").lowercase()
            val binaryName = if (os.contains("win")) "ffprobe.exe" else "ffprobe"

            // Check PATH first
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
