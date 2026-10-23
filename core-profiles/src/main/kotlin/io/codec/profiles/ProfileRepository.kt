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
                json.decodeFromString<List<PresetProfile>>(content)
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

        val toSave = profile.copy(isBuiltIn = false)
        if (index >= 0) {
            updated[index] = toSave
        } else {
            updated.add(toSave)
        }

        _profiles.value = updated
        persistCustomProfiles()
    }

    suspend fun deleteCustomProfile(id: String): Boolean = withContext(Dispatchers.IO) {
        val profile = _profiles.value.firstOrNull { it.id == id } ?: return@withContext false
        if (profile.isBuiltIn) return@withContext false // cannot delete built-in

        _profiles.value = _profiles.value.filter { it.id != id }
        persistCustomProfiles()
        true
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
                val newId = if (existingIds.contains(p.id)) UUID.randomUUID().toString() else p.id
                toAdd.add(p.copy(id = newId, isBuiltIn = false))
                count++
            }

            _profiles.value = _profiles.value + toAdd
            persistCustomProfiles()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        fun defaultStorageDir(): File {
            val userHome = System.getProperty("user.home")
            return File(userHome, ".codec")
        }

        fun createBuiltInProfiles(): List<PresetProfile> {
            return listOf(
                // General & HandBrake Style
                PresetProfile(
                    id = "builtin-fast-1080p",
                    name = "Fast 1080p30",
                    description = "Standard high-speed H.264 Full HD encoding with AAC audio",
                    category = ProfileCategory.GENERAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 22,
                            preset = PresetSpeed.FAST,
                            resolutionPreset = ResolutionPreset.P1080,
                            targetFps = 30.0
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 192,
                            channels = ChannelMode.STEREO
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-hq-1080p",
                    name = "HQ 1080p30 (High Quality)",
                    description = "High quality slower H.264 encode for sharp crisp visuals",
                    category = ProfileCategory.GENERAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 19,
                            preset = PresetSpeed.SLOW,
                            resolutionPreset = ResolutionPreset.P1080,
                            targetFps = 30.0
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 256,
                            channels = ChannelMode.STEREO
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-veryfast-720p",
                    name = "Very Fast 720p30",
                    description = "Ultra rapid encoding for quick sharing",
                    category = ProfileCategory.GENERAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 24,
                            preset = PresetSpeed.VERYFAST,
                            resolutionPreset = ResolutionPreset.P720,
                            targetFps = 30.0
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 128
                        )
                    ),
                    isBuiltIn = true
                ),

                // Web & AV1
                PresetProfile(
                    id = "builtin-webm-av1-1080p",
                    name = "WebM AV1 1080p",
                    description = "Next-gen AV1 video compression with Opus audio",
                    category = ProfileCategory.WEB,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.AV1,
                            container = ContainerFormat.WEBM,
                            qualityMode = QualityMode.CRF,
                            crf = 28,
                            preset = PresetSpeed.MEDIUM,
                            resolutionPreset = ResolutionPreset.P1080
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.OPUS,
                            bitrateKbps = 128
                        )
                    ),
                    isBuiltIn = true
                ),

                // Social Media Compression
                PresetProfile(
                    id = "builtin-discord-25mb",
                    name = "Discord File Limit (25 MB)",
                    description = "Compress video to fit Discord upload limit (2-Pass exact size)",
                    category = ProfileCategory.SOCIAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.TWO_PASS,
                            targetSizeMb = 24.5,
                            preset = PresetSpeed.MEDIUM
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 128
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-discord-10mb",
                    name = "Discord Free Limit (10 MB)",
                    description = "Compress video to under 10 MB for free Discord accounts",
                    category = ProfileCategory.SOCIAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.TWO_PASS,
                            targetSizeMb = 9.8,
                            preset = PresetSpeed.FAST,
                            resolutionPreset = ResolutionPreset.P720
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 96
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-telegram-video",
                    name = "Telegram Video (Fast Streaming)",
                    description = "Optimized for instant mobile playback in Telegram channels",
                    category = ProfileCategory.SOCIAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H264,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 23,
                            preset = PresetSpeed.FAST,
                            resolutionPreset = ResolutionPreset.P720
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 128
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-youtube-4k",
                    name = "YouTube 4K UHD",
                    description = "High bitrate 4K video ready for YouTube creator uploads",
                    category = ProfileCategory.WEB,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H265,
                            container = ContainerFormat.MP4,
                            qualityMode = QualityMode.CRF,
                            crf = 18,
                            preset = PresetSpeed.SLOW,
                            resolutionPreset = ResolutionPreset.P4K,
                            targetFps = 60.0
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.AAC,
                            bitrateKbps = 320
                        )
                    ),
                    isBuiltIn = true
                ),

                // Archival & Production
                PresetProfile(
                    id = "builtin-archive-hevc",
                    name = "Archive H.265 (Max Compression)",
                    description = "High efficiency H.265 archival preset to save 50%+ disk space",
                    category = ProfileCategory.PRODUCTION,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.H265,
                            container = ContainerFormat.MKV,
                            qualityMode = QualityMode.CRF,
                            crf = 23,
                            preset = PresetSpeed.SLOW
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.OPUS,
                            bitrateKbps = 128
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-prores-master",
                    name = "Apple ProRes Intermediate",
                    description = "Lossless visual quality for video editing in Premiere / DaVinci",
                    category = ProfileCategory.PRODUCTION,
                    options = ConversionOptions(
                        video = VideoSettings(
                            codec = VideoCodec.PRORES,
                            container = ContainerFormat.MOV,
                            qualityMode = QualityMode.CRF,
                            profile = "standard"
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.WAV_PCM
                        )
                    ),
                    isBuiltIn = true
                ),

                // Audio Only
                PresetProfile(
                    id = "builtin-audio-mp3-320",
                    name = "MP3 High Quality (320 kbps)",
                    description = "Maximum MP3 bitrate for universal playback",
                    category = ProfileCategory.AUDIO,
                    options = ConversionOptions(
                        video = VideoSettings(
                            container = ContainerFormat.MP3
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.MP3,
                            bitrateKbps = 320,
                            normalizeLoudness = true
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-audio-flac-lossless",
                    name = "FLAC Studio Lossless",
                    description = "Bit-perfect lossless audio preservation",
                    category = ProfileCategory.AUDIO,
                    options = ConversionOptions(
                        video = VideoSettings(
                            container = ContainerFormat.FLAC
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.FLAC
                        )
                    ),
                    isBuiltIn = true
                ),
                PresetProfile(
                    id = "builtin-audio-opus-voice",
                    name = "Opus Voice & Podcast (64 kbps)",
                    description = "Crystal clear speech encoding at minuscule file size",
                    category = ProfileCategory.AUDIO,
                    options = ConversionOptions(
                        video = VideoSettings(
                            container = ContainerFormat.OPUS
                        ),
                        audio = AudioSettings(
                            codec = AudioCodec.OPUS,
                            bitrateKbps = 64,
                            normalizeLoudness = true
                        )
                    ),
                    isBuiltIn = true
                ),

                // Animated GIF
                PresetProfile(
                    id = "builtin-gif-high-quality",
                    name = "High Quality Animated GIF",
                    description = "2-pass palette optimized GIF animation from video clip",
                    category = ProfileCategory.GENERAL,
                    options = ConversionOptions(
                        video = VideoSettings(
                            container = ContainerFormat.GIF,
                            resolutionPreset = ResolutionPreset.P480,
                            targetFps = 15.0
                        )
                    ),
                    isBuiltIn = true
                )
            )
        }
    }
}
