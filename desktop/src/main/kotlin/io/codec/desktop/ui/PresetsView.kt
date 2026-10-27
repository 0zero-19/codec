package io.codec.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.codec.model.*
import io.codec.profiles.ProfileRepository
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun PresetsView(state: DesktopState, modifier: Modifier = Modifier) {
    val lang = state.config.language
    var selectedCategory by remember { mutableStateOf<ProfileCategory?>(null) }
    var presetToEdit by remember { mutableStateOf<PresetProfile?>(null) }
    var isCreatePresetOpen by remember { mutableStateOf(false) }

    val builtInPreset = state.profiles.firstOrNull { it.isBuiltIn }
        ?: state.profileRepo.getDefaultProfile()
    val customPresets = state.profiles.filter { !it.isBuiltIn }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.get("presets_manager_title", lang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Strings.get("presets_manager_sub", lang),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isCreatePresetOpen = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CodecColors.PrimaryCyan)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("create_preset", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, Strings.get("import_presets", lang), FileDialog.LOAD)
                        dialog.file = "*.json"
                        dialog.isVisible = true
                        dialog.file?.let {
                            state.importPresets(File(dialog.directory, it))
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("import_presets", lang), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, Strings.get("export_presets", lang), FileDialog.SAVE)
                        dialog.file = "codec_presets.json"
                        dialog.isVisible = true
                        dialog.file?.let {
                            state.exportPresets(File(dialog.directory, it))
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("export_presets", lang), fontSize = 12.sp)
                }
            }
        }

        // Category Filter Chips
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All", fontSize = 11.sp) }
            )
            ProfileCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.displayName, fontSize = 11.sp) }
                )
            }
        }

        // List of presets
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Primary Built-in Batch Preset
            item(key = builtInPreset.id) {
                BuiltInBatchPresetCard(
                    profile = builtInPreset,
                    state = state,
                    lang = lang
                )
            }

            // 2. Custom Presets Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Strings.get("custom_presets_section", lang)} (${customPresets.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (customPresets.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = Strings.get("no_custom_presets", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                val filteredCustom = if (selectedCategory != null) {
                    customPresets.filter { it.category == selectedCategory }
                } else {
                    customPresets
                }

                items(filteredCustom, key = { it.id }) { customProfile ->
                    CustomPresetCard(
                        profile = customProfile,
                        state = state,
                        lang = lang,
                        onEdit = { presetToEdit = customProfile }
                    )
                }
            }
        }
    }

    // Edit Preset Dialog
    presetToEdit?.let { profile ->
        EditPresetDialog(
            profile = profile,
            state = state,
            onDismiss = { presetToEdit = null }
        )
    }

    // Create New Preset Dialog
    if (isCreatePresetOpen) {
        CreatePresetDialog(
            state = state,
            onDismiss = { isCreatePresetOpen = false }
        )
    }
}

@Composable
private fun BuiltInBatchPresetCard(
    profile: PresetProfile,
    state: DesktopState,
    lang: AppLanguage
) {
    val options = profile.options
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, CodecColors.PrimaryCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CodecColors.PrimaryCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Strings.get("builtin_batch_preset_title", lang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CodecColors.AccentGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = Strings.get("protected_preset_badge", lang),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CodecColors.AccentGreen
                                )
                            }
                            if (profile.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CodecColors.PrimaryCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = Strings.get("is_default_badge", lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CodecColors.PrimaryCyan
                                    )
                                }
                            }
                        }
                        Text(
                            text = Strings.get("builtin_batch_preset_desc", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            state.applyProfileToSelectedJob(profile)
                            state.activeScreen = Screen.QUEUE
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(Strings.get("btn_apply_to_selected", lang), fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            state.applyProfileToAllJobs(profile)
                            state.activeScreen = Screen.QUEUE
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(Strings.get("btn_apply_to_all_jobs", lang), fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = { state.duplicatePreset(profile.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Parameter summary pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ParameterPill("Format: ${options.video.container.extension.uppercase()}")
                ParameterPill("Video: ${options.video.codec.displayName.substringBefore(" ")}")
                ParameterPill("Audio: ${options.audio.codec.displayName.substringBefore(" ")} (${options.audio.bitrateKbps}k)")
                ParameterPill("Quality: CRF ${options.video.crf}")
                ParameterPill("Speed: ${options.video.preset.value}")
            }
        }
    }
}

@Composable
private fun CustomPresetCard(
    profile: PresetProfile,
    state: DesktopState,
    lang: AppLanguage,
    onEdit: () -> Unit
) {
    val options = profile.options
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CodecColors.PrimaryCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = profile.category.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CodecColors.PrimaryCyan
                            )
                        }
                        if (profile.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CodecColors.AccentGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = Strings.get("is_default_badge", lang),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CodecColors.AccentGreen
                                )
                            }
                        }
                    }
                    if (profile.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            state.applyProfileToSelectedJob(profile)
                            state.activeScreen = Screen.QUEUE
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(Strings.get("apply", lang), fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            state.applyProfileToAllJobs(profile)
                            state.activeScreen = Screen.QUEUE
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(Strings.get("btn_apply_to_all_jobs", lang), fontSize = 11.sp)
                    }

                    if (!profile.isDefault) {
                        IconButton(
                            onClick = { state.setDefaultPreset(profile.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.StarOutline, contentDescription = "Set Default", modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { state.duplicatePreset(profile.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { state.deletePreset(profile.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CodecColors.AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parameter summary pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ParameterPill("Format: ${options.video.container.extension.uppercase()}")
                ParameterPill("Video: ${options.video.codec.displayName.substringBefore(" ")}")
                ParameterPill("Audio: ${options.audio.codec.displayName.substringBefore(" ")} (${options.audio.bitrateKbps}k)")
                ParameterPill("CRF: ${options.video.crf}")
                ParameterPill("Speed: ${options.video.preset.value}")
            }
        }
    }
}

@Composable
private fun ParameterPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreatePresetDialog(state: DesktopState, onDismiss: () -> Unit) {
    val lang = state.config.language
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProfileCategory.CUSTOM) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(440.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = Strings.get("create_preset", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("preset_name_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("preset_desc_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(Strings.get("preset_category_label", lang), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ProfileCategory.GENERAL, ProfileCategory.WEB, ProfileCategory.SOCIAL, ProfileCategory.CUSTOM).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName, fontSize = 10.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                state.saveCurrentOptionsAsPreset(name, description, category)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Strings.get("save", lang))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPresetDialog(profile: PresetProfile, state: DesktopState, onDismiss: () -> Unit) {
    val lang = state.config.language
    var name by remember { mutableStateOf(profile.name) }
    var description by remember { mutableStateOf(profile.description) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(440.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "${Strings.get("edit_preset", lang)}: ${profile.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("preset_name_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.get("preset_desc_label", lang), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                state.renamePreset(profile.id, name, description)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Strings.get("save", lang))
                    }
                }
            }
        }
    }
}
