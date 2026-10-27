package io.codec.queue

import io.codec.media.FFmpegCapabilities
import io.codec.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class TaskQueueTest {

    companion object {
        private val tempDir = File(System.getProperty("java.io.tmpdir"), "codec_queue_test")
        private val sampleVideo = File(tempDir, "sample.mp4")

        @JvmStatic
        @BeforeAll
        fun setup() {
            tempDir.mkdirs()
            if (!sampleVideo.exists() || sampleVideo.length() < 1000L) {
                val ffmpeg = FFmpegCapabilities.findFFmpegExecutable()
                val cmd = listOf(
                    ffmpeg, "-y",
                    "-f", "lavfi", "-i", "testsrc=duration=2:size=320x240:rate=25",
                    "-f", "lavfi", "-i", "sine=frequency=800:duration=2",
                    "-c:v", "libx264", "-preset", "ultrafast",
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
    fun testQueueAddAndProcess() = runBlocking {
        val outDir = File(tempDir, "out")
        outDir.mkdirs()
        val queue = TaskQueueManager(
            storageDir = tempDir,
            maxConcurrent = 1
        )

        val job = queue.addJob(
            inputFile = sampleVideo,
            outputDir = outDir,
            options = ConversionOptions(
                video = VideoSettings(
                    codec = VideoCodec.H264,
                    container = ContainerFormat.MP4,
                    preset = PresetSpeed.ULTRAFAST
                )
            )
        )

        assertNotNull(job)
        assertEquals(TaskStatus.PENDING, job.status)

        // Give coroutine time to probe
        delay(1500)

        val jobsList = queue.jobs.value
        val probedJob = jobsList.firstOrNull { it.id == job.id }
        assertNotNull(probedJob)

        // Start processing queue
        queue.startQueue()

        // Wait until completion
        var attempts = 0
        while (attempts < 50) {
            val current = queue.jobs.value.firstOrNull { it.id == job.id }
            if (current?.status == TaskStatus.COMPLETED || current?.status == TaskStatus.FAILED) {
                break
            }
            delay(300)
            attempts++
        }

        val completed = queue.jobs.value.firstOrNull { it.id == job.id }
        assertNotNull(completed)
        if (completed?.status == TaskStatus.FAILED) {
            println("FAILED REASON: ${completed.errorMessage}")
            println("LOGS:\n${completed.ffmpegLog.joinToString("\n")}")
        }
        assertEquals(TaskStatus.COMPLETED, completed?.status, "Job should complete successfully")
        assertTrue(File(completed!!.outputPath).exists(), "Output file must exist")
    }

    @Test
    fun testFilenameResolutionAndCollision() {
        val outDir = File(tempDir, "collision_test")
        outDir.mkdirs()
        val queue = TaskQueueManager(storageDir = tempDir)

        val out1 = queue.resolveOutputPath(
            dir = outDir,
            baseName = "video_one",
            ext = "mp4",
            behavior = FileCollisionBehavior.UNIQUE_NAME,
            template = "{name}_converted.{ext}"
        )
        val file1 = File(out1)
        file1.writeText("placeholder")

        // Next call with UNIQUE_NAME should generate a unique name with (1)
        val out2 = queue.resolveOutputPath(
            dir = outDir,
            baseName = "video_one",
            ext = "mp4",
            behavior = FileCollisionBehavior.UNIQUE_NAME,
            template = "{name}_converted.{ext}"
        )
        assertTrue(out2.contains("(1)"), "Unique name should add (1)")

        // OVERWRITE should return exact path
        val outOverwrite = queue.resolveOutputPath(
            dir = outDir,
            baseName = "video_one",
            ext = "mp4",
            behavior = FileCollisionBehavior.OVERWRITE,
            template = "{name}_converted.{ext}"
        )
        assertEquals(file1.absolutePath, outOverwrite)

        file1.delete()
        File(out2).delete()
    }
}
