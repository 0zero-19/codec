package io.codec.profiles

import io.codec.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ProfileRepository(
    private val storageDir: File = defaultStorageDir()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val profilesFile = File(storageDir, "profiles.json")
    private val _profiles = MutableStateFlow<List<PresetProfile>>(emptyList())
    val profiles: StateFlow<List<PresetProfile>> = _profiles.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val builtIns = createBuiltInProfiles()
        val customProfiles = if (profilesFile.exists()) {
            try {
                val content = profilesFile.readText()
                val list = json.decodeFromString<List<PresetProfile>>(content)
                // Filter out any legacy built-ins that may have been saved to custom profiles.json
                list.filter { !it.isBuiltIn && it.id != "builtin-batch-convert" }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        _profiles.value = builtIns + customProfiles
    }

    suspend fun saveCustomProfile(profile: PresetProfile) = withContext(Dispatchers.IO) {
        val updated = _profiles.value.toMutableList()
        val index = updated.indexOfFirst { it.id == profile.id }

        val validated = validateProfile(profile.copy(isBuiltIn = false))
        if (index >= 0) {
            updated[index] = validated
        } else {
            updated.add(validated)
        }

        _profiles.value = updated
        persistCustomProfiles()
    }

    suspend fun duplicateCustomProfile(id: String): PresetProfile? = withContext(Dispatchers.IO) {
        val original = _profiles.value.firstOrNull { it.id == id } ?: return@withContext null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            isBuiltIn = false,
            isDefault = false
        )
        saveCustomProfile(copy)
        copy
    }

    suspend fun renameCustomProfile(id: String, newName: String, newDescription: String? = null): Boolean = withContext(Dispatchers.IO) {
        val original = _profiles.value.firstOrNull { it.id == id } ?: return@withContext false
        if (original.isBuiltIn) return@withContext false
        val updated = original.copy(
            name = newName.trim(),
            description = newDescription?.trim() ?: original.description
        )
        saveCustomProfile(updated)
        true
    }

    suspend fun deleteCustomProfile(id: String): Boolean = withContext(Dispatchers.IO) {
        val profile = _profiles.value.firstOrNull { it.id == id } ?: return@withContext false
        if (profile.isBuiltIn) return@withContext false // Protected: cannot delete built-in batch preset

        _profiles.value = _profiles.value.filter { it.id != id }
        persistCustomProfiles()
        true
    }

    suspend fun setDefaultProfile(id: String): Boolean = withContext(Dispatchers.IO) {
        if (_profiles.value.none { it.id == id }) return@withContext false
        _profiles.value = _profiles.value.map {
            it.copy(isDefault = it.id == id)
        }
        persistCustomProfiles()
        true
    }

    fun getDefaultProfile(): PresetProfile {
        return _profiles.value.firstOrNull { it.isDefault }
            ?: _profiles.value.firstOrNull { it.id == BATCH_PRESET_ID }
            ?: _profiles.value.first()
    }

    suspend fun exportProfilesToJson(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            val text = json.encodeToString(_profiles.value)
            file.writeText(text)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProfilesFromJson(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val imported = json.decodeFromString<List<PresetProfile>>(content)
            var count = 0
            val existingIds = _profiles.value.map { it.id }.toSet()
            val toAdd = mutableListOf<PresetProfile>()

            for (p in imported) {
                // Ensure valid encoding parameters
                val safe = validateProfile(p)
                val newId = if (existingIds.contains(safe.id) || safe.id == BATCH_PRESET_ID) {
                    UUID.randomUUID().toString()
                } else {
                    safe.id
                }
                toAdd.add(safe.copy(id = newId, isBuiltIn = false, isDefault = false))
                count++
            }

            _profiles.value = _profiles.value + toAdd
            persistCustomProfiles()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateProfile(profile: PresetProfile): PresetProfile {
        val opt = profile.options
        val crf = opt.video.crf.coerceIn(0, 51)
        val vBitrate = maxOf(64, opt.video.videoBitrateKbps)
        val aBitrate = maxOf(32, opt.audio.bitrateKbps)
        val safeName = if (profile.name.isBlank()) "Custom Preset" else profile.name

        val validatedVideo = opt.video.copy(crf = crf, videoBitrateKbps = vBitrate)
        val validatedAudio = opt.audio.copy(bitrateKbps = aBitrate)
        val validatedOptions = opt.copy(video = validatedVideo, audio = validatedAudio)

        return profile.copy(name = safeName, options = validatedOptions)
    }

    private fun persistCustomProfiles() {
        try {
            storageDir.mkdirs()
            val custom = _profiles.value.filter { !it.isBuiltIn }
            profilesFile.writeText(json.encodeToString(custom))
        } catch (_: Exception) {}
    }

    fun getProfileById(id: String): PresetProfile? {
        return _profiles.value.firstOrNull { it.id == id }
    }

    companion object {
        const val BATCH_PRESET_ID = "builtin-batch-convert"

        fun defaultStorageDir(): File {
            val userHome = System.getProperty("user.home")
            return File(userHome, ".codec")
        }

        fun createBuiltInProfiles(): List<PresetProfile> {
            return listOf(
                PresetProfile(
                    id = BATCH_PRESET_ID,
                    name = "Batch Video Conversion / Массовая конвертация видео",
                    description = "Universal batch conversion for multiple videos (MP4, H.264, AAC, 1080p, CRF 22)",
                    category = ProfileCategory.GENERAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 22,
                            preset = PresetSpeed.FAST,
                            resolutionPreset = ResolutionPreset.ORIGINAL,
                            framerateMode = FramerateMode.SAME_AS_SOURCE
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 192,
                            channels = ChannelMode.STEREO,
                            sampleRateHz = 48000
                        ),
                        collisionBehavior = FileCollisionBehavior.UNIQUE_NAME,
                        outputFileNameTemplate = "{name}_converted.{ext}"
                    ),
                    isBuiltIn = true,
                    isDefault = true
                )
            )
        }
    }
}
