package io.codec.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codec.model.TaskStatus

@Composable
fun Sidebar(state: DesktopState) {
    val lang = state.config.language
    val activeJobsCount = state.jobs.count { it.status == TaskStatus.RUNNING }
    val totalPending = state.jobs.count { it.status == TaskStatus.PENDING }

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Branding Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp, top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CodecColors.PrimaryCyan, CodecColors.PrimaryIndigo)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CODEC",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pro Media Engine",
                        fontSize = 11.sp,
                        color = CodecColors.PrimaryCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Add Buttons
            Button(
                onClick = { state.addFilesDialog() },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("add_files", lang), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { state.addFolderDialog() },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("add_folder", lang), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Links
            NavItem(
                title = Strings.get("nav_queue", lang),
                icon = Icons.Default.Queue,
                isSelected = state.activeScreen == Screen.QUEUE,
                badge = if (activeJobsCount > 0) "$activeJobsCount" else if (totalPending > 0) "$totalPending" else null,
                badgeColor = if (activeJobsCount > 0) CodecColors.AccentGreen else CodecColors.PrimaryCyan
            ) { state.activeScreen = Screen.QUEUE }

            NavItem(
                title = Strings.get("nav_presets", lang),
                icon = Icons.Default.Bookmarks,
                isSelected = state.activeScreen == Screen.PRESETS
            ) { state.activeScreen = Screen.PRESETS }

            NavItem(
                title = Strings.get("nav_tools", lang),
                icon = Icons.Default.Handyman,
                isSelected = state.activeScreen == Screen.TOOLS
            ) { state.activeScreen = Screen.TOOLS }

            NavItem(
                title = Strings.get("nav_inspector", lang),
                icon = Icons.Default.Analytics,
                isSelected = state.activeScreen == Screen.INSPECTOR
            ) { state.activeScreen = Screen.INSPECTOR }

            NavItem(
                title = Strings.get("nav_settings", lang),
                icon = Icons.Default.Settings,
                isSelected = state.activeScreen == Screen.SETTINGS
            ) { state.activeScreen = Screen.SETTINGS }
        }

        // Bottom Engine Status Indicator
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasHw = (state.systemCaps?.supportedHardwareModes?.size ?: 0) > 1
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (hasHw) CodecColors.AccentGreen else CodecColors.AccentOrange)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    val engineLabel = state.systemCaps?.ffmpegVersion?.substringBefore(" ") ?: "FFmpeg Core"
                    Text(
                        text = engineLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == io.codec.model.AppLanguage.RU) {
                            if (hasHw) "Аппаратное ускорение" else "Программный режим"
                        } else {
                            if (hasHw) "HW Accel Active" else "Software Mode"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String? = null,
    badgeColor: Color = CodecColors.PrimaryCyan,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
