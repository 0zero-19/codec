package io.codec.profiles

import io.codec.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class ProfileRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repo: ProfileRepository

    @BeforeEach
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "codec_profile_test_${UUID.randomUUID()}")
        tempDir.mkdirs()
        repo = ProfileRepository(storageDir = tempDir)
    }

    @Test
    fun testBuiltInBatchPresetExistsAndIsProtected() = runBlocking {
        val profiles = repo.profiles.value
        val batchPreset = profiles.firstOrNull { it.id == ProfileRepository.BATCH_PRESET_ID }

        assertNotNull(batchPreset, "Built-in batch preset must exist")
        assertTrue(batchPreset!!.isBuiltIn, "Batch preset must be marked builtIn")
        assertEquals("Batch Video Conversion / Массовая конвертация видео", batchPreset.name)
        assertEquals(VideoCodec.H264, batchPreset.options.video.codec)
        assertEquals(ContainerFormat.MP4, batchPreset.options.video.container)

        // Attempting to delete built-in preset must fail
        val deleted = repo.deleteCustomProfile(ProfileRepository.BATCH_PRESET_ID)
        assertFalse(deleted, "Built-in batch preset must not be deletable")
        assertNotNull(repo.getProfileById(ProfileRepository.BATCH_PRESET_ID))
    }

    @Test
    fun testCustomPresetCrudAndExportImport() = runBlocking {
        val custom = PresetProfile(
            id = "my-custom-hevc",
            name = "My HEVC Preset",
            description = "Custom H.265 compression",
            category = ProfileCategory.CUSTOM,
            options = ConversionOptions(
                video = VideoSettings(
                    codec = VideoCodec.H265,
                    container = ContainerFormat.MKV,
                    crf = 24
                )
            )
        )

        // 1. Save
        repo.saveCustomProfile(custom)
        var found = repo.getProfileById("my-custom-hevc")
        assertNotNull(found)
        assertEquals("My HEVC Preset", found!!.name)
        assertEquals(VideoCodec.H265, found.options.video.codec)

        // 2. Duplicate
        val copy = repo.duplicateCustomProfile("my-custom-hevc")
        assertNotNull(copy)
        assertTrue(copy!!.name.contains("Copy"))
        assertNotEquals("my-custom-hevc", copy.id)

        // 3. Rename
        val renamed = repo.renameCustomProfile("my-custom-hevc", "Renamed HEVC", "New desc")
        assertTrue(renamed)
        val afterRename = repo.getProfileById("my-custom-hevc")
        assertEquals("Renamed HEVC", afterRename?.name)

        // 4. Export & Import
        val exportFile = File(tempDir, "exported.json")
        val exportRes = repo.exportProfilesToJson(exportFile)
        assertTrue(exportRes.isSuccess)
        assertTrue(exportFile.exists() && exportFile.length() > 0)

        // Clean custom and import
        repo.deleteCustomProfile("my-custom-hevc")
        repo.deleteCustomProfile(copy.id)
        val importRes = repo.importProfilesFromJson(exportFile)
        assertTrue(importRes.isSuccess)
        assertTrue(importRes.getOrThrow() > 0)

        // 5. Delete
        val toDelete = repo.profiles.value.first { !it.isBuiltIn }
        val delRes = repo.deleteCustomProfile(toDelete.id)
        assertTrue(delRes)
    }
}
