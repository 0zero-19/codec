package io.codec.media

import io.codec.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class MediaEngineTest {

    companion object {
        private val tempDir = File(System.getProperty("java.io.tmpdir"), "codec_test")
        val sampleVideo = File(tempDir, "sample_test.mp4")

        @JvmStatic
        @BeforeAll
        fun setup() {
            tempDir.mkdirs()
            if (!sampleVideo.exists() || sampleVideo.length() == 0L) {
                val ffmpeg = FFmpegCapabilities.findFFmpegExecutable()
                val cmd = listOf(
                    ffmpeg,
                    "-y",
                    "-f", "lavfi", "-i", "testsrc=duration=2:size=640x360:rate=30",
                    "-f", "lavfi", "-i", "sine=frequency=1000:duration=2",
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-c:a", "aac",
                    "-pix_fmt", "yuv420p",
                    sampleVideo.absolutePath
                )
                val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                proc.inputStream.bufferedReader().readText()
                proc.waitFor()
            }
        }
    }

    @Test
    fun testDetectCapabilities() = runBlocking {
        val detector = FFmpegCapabilities()
        val caps = detector.detectCapabilities()
        assertNotNull(caps)
        assertTrue(caps.ffmpegVersion.isNotBlank(), "FFmpeg version should not be blank")
        assertTrue(caps.availableEncoders.contains("libx264"), "FFmpeg should support libx264")
        assertTrue(caps.availableEncoders.contains("aac"), "FFmpeg should support aac")
    }

    @Test
    fun testProbeRealMedia() = runBlocking {
        val analyzer = FFprobeAnalyzer()
        val result = analyzer.analyze(sampleVideo)
        assertTrue(result.isSuccess, "Probe should succeed on sample video")
        val info = result.getOrThrow()

        assertEquals("sample_test.mp4", info.fileName)
        assertTrue(info.durationSeconds in 1.8..2.2, "Duration should be ~2s")
        assertEquals(1, info.videoStreams.size)
        assertEquals(1, info.audioStreams.size)

        val video = info.primaryVideoStream!!
        assertEquals(640, video.width)
        assertEquals(360, video.height)
        assertEquals("h264", video.codecName.lowercase())
        assertEquals(30.0, video.frameRate, 0.5)

        val audio = info.primaryAudioStream!!
        assertEquals("aac", audio.codecName.lowercase())
    }

    @Test
    fun testProgressParser() {
        val parser = FFmpegProgressParser(totalDurationMs = 10000L) // 10s video

        val update1 = parser.parseLine("frame=150")
        assertNull(update1)

        parser.parseLine("fps=60.0")
        parser.parseLine("bitrate= 2500.0kbits/s")
        parser.parseLine("total_size=1048576")
        parser.parseLine("out_time_us=5000000") // 5s = 50%
        parser.parseLine("speed= 2.0x")
        val updateFinal = parser.parseLine("progress=continue")

        assertNotNull(updateFinal)
        assertEquals(50.0, updateFinal!!.percentage, 0.1)
        assertEquals(150L, updateFinal.frame)
        assertEquals(60.0, updateFinal.fps, 0.1)
        assertEquals(2.0, updateFinal.speedMultiplier, 0.1)
        assertEquals(2L, updateFinal.etaSeconds) // (10 - 5) / 2 = 2.5s -> 2s
    }

    @Test
    fun testCommandBuilderGeneration() {
        val builder = FFmpegCommandBuilder("ffmpeg")
        val options = ConversionOptions(
            video = VideoSettings(
                codec = VideoCodec.H265,
                container = ContainerFormat.MP4,
                qualityMode = QualityMode.CRF,
                crf = 26,
                preset = PresetSpeed.FAST
            ),
            audio = AudioSettings(
                codec = AudioCodec.OPUS,
                bitrateKbps = 128
            )
        )

        val cmd = builder.buildCommand("input.mkv", "output.mp4", options, null)

        assertTrue(cmd.contains("-i"))
        assertTrue(cmd.contains("input.mkv"))
        assertTrue(cmd.contains("-c:v"))
        assertTrue(cmd.contains("libx265"))
        assertTrue(cmd.contains("-crf"))
        assertTrue(cmd.contains("26"))
        assertTrue(cmd.contains("-preset"))
        assertTrue(cmd.contains("fast"))
        assertTrue(cmd.contains("-c:a"))
        assertTrue(cmd.contains("libopus"))
        assertTrue(cmd.contains("-b:a"))
        assertTrue(cmd.contains("128k"))
        assertTrue(cmd.contains("-progress"))
        assertTrue(cmd.contains("output.mp4"))
        assertTrue(cmd.contains("-tag:v"))
        assertTrue(cmd.contains("hvc1"))
        assertTrue(cmd.contains("-movflags"))
        assertTrue(cmd.contains("+faststart"))
    }

    @Test
    fun testContainerCompatibility() {
        // WebM cannot accept H.264
        assertFalse(ContainerFormat.WEBM.isCompatibleWith(VideoCodec.H264, AudioCodec.OPUS))
        assertTrue(ContainerFormat.WEBM.isCompatibleWith(VideoCodec.VP9, AudioCodec.OPUS))
        assertNotNull(ContainerFormat.WEBM.getCompatibilityWarning(VideoCodec.H264, AudioCodec.OPUS))

        // MP4 supports H.264, H.265, AV1 with AAC, MP3, Opus
        assertTrue(ContainerFormat.MP4.isCompatibleWith(VideoCodec.H265, AudioCodec.AAC))
        assertTrue(ContainerFormat.MP4.isCompatibleWith(VideoCodec.H264, AudioCodec.AAC))

        // MP3 is audio-only
        assertTrue(ContainerFormat.MP3.isAudioOnly)
        assertTrue(ContainerFormat.MP3.isCompatibleWith(VideoCodec.NONE, AudioCodec.MP3))
        assertFalse(ContainerFormat.MP3.isCompatibleWith(VideoCodec.H264, AudioCodec.MP3))
    }

    @Test
    fun testRealConversionExecution() = runBlocking {
        val executor = FFmpegProcessExecutor()
        val outputFile = File(tempDir, "converted_test.webm")
        if (outputFile.exists()) outputFile.delete()

        val options = ConversionOptions(
            video = VideoSettings(
                codec = VideoCodec.VP9,
                container = ContainerFormat.WEBM,
                qualityMode = QualityMode.CRF,
                crf = 32,
                preset = PresetSpeed.VERYFAST
            ),
            audio = AudioSettings(
                codec = AudioCodec.OPUS,
                bitrateKbps = 96
            )
        )

        var lastProgressPct = 0.0
        val logLines = mutableListOf<String>()

        val result = executor.execute(
            inputPath = sampleVideo.absolutePath,
            outputPath = outputFile.absolutePath,
            options = options,
            mediaInfo = null,
            onProgress = { update, _, _ ->
                lastProgressPct = update.percentage
            },
            onLogLine = { line ->
                logLines.add(line)
            }
        )

        assertTrue(result is ExecutionResult.Success, "Conversion to WebM should succeed")
        assertTrue(outputFile.exists() && outputFile.length() > 0, "Output file must exist and be non-empty")
        assertTrue(logLines.isNotEmpty(), "Logs should be captured")

        // Verify with ffprobe
        val analyzer = FFprobeAnalyzer()
        val probed = analyzer.analyze(outputFile).getOrThrow()
        assertEquals("vp9", probed.primaryVideoStream?.codecName?.lowercase())
        assertEquals("opus", probed.primaryAudioStream?.codecName?.lowercase())

        outputFile.delete()
    }

    @Test
    fun testFrameExtractionAndAudioExtraction() = runBlocking {
        val tools = MediaTools()
        val frameFile = File(tempDir, "extracted_frame.png")
        val audioFile = File(tempDir, "extracted_audio.aac")

        if (frameFile.exists()) frameFile.delete()
        if (audioFile.exists()) audioFile.delete()

        val frameResult = tools.extractFrame(sampleVideo, frameFile, 0.5)
        assertTrue(frameResult.isSuccess, "Frame extraction should succeed")
        assertTrue(frameFile.exists() && frameFile.length() > 0, "PNG frame must exist")

        val audioResult = tools.extractAudio(sampleVideo, audioFile, copyLossless = true)
        assertTrue(audioResult.isSuccess, "Audio extraction should succeed")
        assertTrue(audioFile.exists() && audioFile.length() > 0, "Extracted audio must exist")

        frameFile.delete()
        audioFile.delete()
    }
}
