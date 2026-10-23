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
import io.codec.model.PresetProfile
import io.codec.model.ProfileCategory
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun PresetsView(state: DesktopState, modifier: Modifier = Modifier) {
    val lang = state.config.language
    var selectedCategory by remember { mutableStateOf<ProfileCategory?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Strings.get("nav_presets", lang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "HandBrake & modern streaming profiles ready for 1-click batch application",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                OutlinedButton(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, "Import Presets", FileDialog.LOAD)
                        dialog.file = "*.json"
                        dialog.isVisible = true
                        dialog.file?.let {
                            state.scope.launch {
                                state.profileRepo.importProfilesFromJson(File(dialog.directory, it))
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import JSON", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, "Export Presets", FileDialog.SAVE)
                        dialog.file = "codec_presets.json"
                        dialog.isVisible = true
                        dialog.file?.let {
                            state.scope.launch {
                                state.profileRepo.exportProfilesToJson(File(dialog.directory, it))
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export All", fontSize = 12.sp)
                }
            }
        }

        // Category Filter Chips
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All Presets", fontSize = 11.sp) }
            )
            ProfileCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.displayName, fontSize = 11.sp) }
                )
            }
        }

        // Profiles List
        val filtered = if (selectedCategory != null) {
            state.profiles.filter { it.category == selectedCategory }
        } else {
            state.profiles
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { profile ->
                PresetCard(profile, state)
            }
        }
    }
}

@Composable
private fun PresetCard(profile: PresetProfile, state: DesktopState) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                            .clip(RoundedCornerShape(6.dp))
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
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        state.applyProfileToSelectedJob(profile)
                        state.activeScreen = Screen.QUEUE
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Apply to Selected", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        state.applyProfileToAllJobs(profile)
                        state.activeScreen = Screen.QUEUE
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Apply to All", fontSize = 11.sp)
                }
            }
        }
    }
}
