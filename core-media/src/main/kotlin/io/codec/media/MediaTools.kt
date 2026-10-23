package io.codec.media

import io.codec.model.AudioCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaTools(
    private val ffmpegExecutable: String = FFmpegCapabilities.findFFmpegExecutable()
) {
    suspend fun extractFrame(
        inputFile: File,
        outputImageFile: File,
        timestampSeconds: Double
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputImageFile.parentFile?.mkdirs()
            val cmd = listOf(
                ffmpegExecutable,
                "-hide_banner",
                "-y",
                "-ss", String.format(java.util.Locale.US, "%.3f", timestampSeconds),
                "-i", inputFile.absolutePath,
                "-vframes", "1",
                "-q:v", "2",
                outputImageFile.absolutePath
            )

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val errorOutput = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputImageFile.exists() && outputImageFile.length() > 0) {
                Result.success(outputImageFile)
            } else {
                Result.failure(RuntimeException("Frame extraction failed ($exitCode): $errorOutput"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractAudio(
        inputFile: File,
        outputAudioFile: File,
        copyLossless: Boolean = true,
        codec: AudioCodec = AudioCodec.AAC,
        bitrateKbps: Int = 320
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputAudioFile.parentFile?.mkdirs()
            val cmd = mutableListOf(
                ffmpegExecutable,
                "-hide_banner",
                "-y",
                "-i", inputFile.absolutePath,
                "-vn"
            )

            if (copyLossless) {
                cmd.add("-c:a")
                cmd.add("copy")
            } else {
                cmd.add("-c:a")
                cmd.add(codec.encoderName)
                if (codec != AudioCodec.FLAC && codec != AudioCodec.WAV_PCM) {
                    cmd.add("-b:a")
                    cmd.add("${bitrateKbps}k")
                }
            }

            cmd.add(outputAudioFile.absolutePath)

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val errorOutput = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputAudioFile.exists()) {
                Result.success(outputAudioFile)
            } else {
                Result.failure(RuntimeException("Audio extraction failed ($exitCode): $errorOutput"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trim(
        inputFile: File,
        outputFile: File,
        startSeconds: Double,
        endSeconds: Double,
        streamCopy: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            val duration = endSeconds - startSeconds
            val cmd = mutableListOf(
                ffmpegExecutable,
                "-hide_banner",
                "-y",
                "-ss", String.format(java.util.Locale.US, "%.3f", startSeconds),
                "-i", inputFile.absolutePath,
                "-t", String.format(java.util.Locale.US, "%.3f", duration)
            )

            if (streamCopy) {
                cmd.add("-c")
                cmd.add("copy")
            }

            cmd.add(outputFile.absolutePath)

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val errorOutput = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                Result.success(outputFile)
            } else {
                Result.failure(RuntimeException("Trim failed ($exitCode): $errorOutput"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun remux(
        inputFile: File,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            val cmd = listOf(
                ffmpegExecutable,
                "-hide_banner",
                "-y",
                "-i", inputFile.absolutePath,
                "-c", "copy",
                outputFile.absolutePath
            )

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val errorOutput = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                Result.success(outputFile)
            } else {
                Result.failure(RuntimeException("Remux failed ($exitCode): $errorOutput"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
