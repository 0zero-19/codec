package io.codec.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProfileCategory(val displayName: String) {
    GENERAL("General"),
    WEB("Web & Streaming"),
    SOCIAL("Social Media"),
    DEVICES("Devices"),
    PRODUCTION("Production / Master"),
    AUDIO("Audio Only"),
    CUSTOM("Custom")
}

@Serializable
data class PresetProfile(
    val id: String,
    val name: String,
    val description: String,
    val category: ProfileCategory,
    val options: ConversionOptions,
    val isBuiltIn: Boolean = false,
    val isDefault: Boolean = false
)

@Serializable
enum class AppLanguage(val code: String, val displayName: String) {
    RU("ru", "Русский"),
    EN("en", "English")
}

@Serializable
enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    DARK("Dark"),
    LIGHT("Light")
}

@Serializable
data class AppConfig(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.RU,
    val defaultOutputDirectory: String = "",
    val maxConcurrentTasks: Int = 2,
    val defaultHardwareEncoder: HardwareEncoder = HardwareEncoder.AUTO,
    val keepPartialFilesOnFailure: Boolean = false,
    val overwriteExistingFiles: Boolean = false,
    val fileNamingTemplate: String = "{name}_converted.{ext}",
    val preferredVideoCodec: VideoCodec = VideoCodec.H264,
    val preferredAudioCodec: AudioCodec = AudioCodec.AAC,
    val defaultPresetId: String = "builtin-batch-convert",
    val defaultCollisionBehavior: FileCollisionBehavior = FileCollisionBehavior.UNIQUE_NAME,
    val lastUsedOptions: ConversionOptions? = null
)
