package io.codec.desktop.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.model.AppLanguage
import io.codec.model.HardwareEncoder
import io.codec.model.ThemeMode
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun SettingsView(state: DesktopState, modifier: Modifier = Modifier) {
    val lang = state.config.language
    val caps = state.systemCaps

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Strings.get("nav_settings", lang),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // General Preferences Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("General Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CodecColors.PrimaryCyan)

                // Language
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.get("settings_lang", lang), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.config.language == AppLanguage.RU,
                            onClick = { state.setLanguage(AppLanguage.RU) },
                            label = { Text("Русский") }
                        )
                        FilterChip(
                            selected = state.config.language == AppLanguage.EN,
                            onClick = { state.setLanguage(AppLanguage.EN) },
                            label = { Text("English") }
                        )
                    }
                }

                // Theme
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.get("settings_theme", lang), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.config.themeMode == ThemeMode.DARK,
                            onClick = { state.setTheme(ThemeMode.DARK) },
                            label = { Text("Dark") }
                        )
                        FilterChip(
                            selected = state.config.themeMode == ThemeMode.LIGHT,
                            onClick = { state.setTheme(ThemeMode.LIGHT) },
                            label = { Text("Light") }
                        )
                    }
                }

                // Output Directory
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Strings.get("settings_output_dir", lang), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.config.defaultOutputDirectory,
                            onValueChange = { state.setOutputDirectory(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                System.setProperty("apple.awt.fileDialogForDirectories", "true")
                                val dialog = FileDialog(null as Frame?, "Select Output Directory", FileDialog.LOAD)
                                dialog.isVisible = true
                                dialog.directory?.let {
                                    state.setOutputDirectory(it)
                                }
                                System.setProperty("apple.awt.fileDialogForDirectories", "false")
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(Strings.get("settings_browse", lang), fontSize = 12.sp)
                        }
                    }
                }

                // Max Concurrent Tasks
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${Strings.get("settings_max_concurrent", lang)}: ${state.config.maxConcurrentTasks}", fontSize = 13.sp)
                    Slider(
                        value = state.config.maxConcurrentTasks.toFloat(),
                        onValueChange = { state.setMaxConcurrency(it.toInt()) },
                        valueRange = 1f..8f,
                        steps = 6
                    )
                }
            }
        }

        // Hardware Acceleration Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hardware Acceleration Engines", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CodecColors.PrimaryCyan)

                val availableModes = caps?.supportedHardwareModes ?: listOf(HardwareEncoder.SOFTWARE)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableModes.forEach { mode ->
                        FilterChip(
                            selected = state.config.defaultHardwareEncoder == mode,
                            onClick = { state.setHardwareEncoder(mode) },
                            label = { Text(mode.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // FFmpeg Core Diagnostics Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FFmpeg Engine Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CodecColors.PrimaryCyan)
                Text("Installed Core: ${caps?.ffmpegVersion ?: "Detecting..."}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Detected Hardware Accelerators: ${caps?.availableHwaccels?.joinToString(", ") ?: "None"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total Registered Encoders: ${caps?.availableEncoders?.size ?: 0}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
