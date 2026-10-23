package io.codec.settings

import io.codec.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepository(
    private val storageDir: File = defaultStorageDir()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val configFile = File(storageDir, "settings.json")
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private fun loadConfig(): AppConfig {
        if (!configFile.exists()) {
            return AppConfig(
                defaultOutputDirectory = defaultOutputDirectory()
            )
        }
        return try {
            val content = configFile.readText()
            json.decodeFromString<AppConfig>(content)
        } catch (_: Exception) {
            AppConfig(defaultOutputDirectory = defaultOutputDirectory())
        }
    }

    suspend fun updateConfig(transform: (AppConfig) -> AppConfig) = withContext(Dispatchers.IO) {
        val newConfig = transform(_config.value)
        _config.value = newConfig
        persistConfig(newConfig)
    }

    private fun persistConfig(config: AppConfig) {
        try {
            storageDir.mkdirs()
            configFile.writeText(json.encodeToString(config))
        } catch (_: Exception) {}
    }

    companion object {
        fun defaultStorageDir(): File {
            val userHome = System.getProperty("user.home")
            return File(userHome, ".codec")
        }

        fun defaultOutputDirectory(): String {
            val userHome = System.getProperty("user.home")
            val videos = File(userHome, "Videos")
            if (videos.exists() && videos.isDirectory) {
                return File(videos, "Codec_Exports").absolutePath
            }
            return File(userHome, "Codec_Exports").absolutePath
        }
    }
}
