package io.codec.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.codec.desktop.ui.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File

fun main() = application {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { DesktopState(coroutineScope) }

    Window(
        onCloseRequest = ::exitApplication,
        title = Strings.get("app_title", state.config.language),
        state = WindowState(width = 1320.dp, height = 840.dp)
    ) {
        // Setup Drag and Drop from OS file manager
        LaunchedEffect(window) {
            window.dropTarget = DropTarget(window, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
                override fun drop(dtde: DropTargetDropEvent) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY)
                        val transferable = dtde.transferable
                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            @Suppress("UNCHECKED_CAST")
                            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                            if (files.isNotEmpty()) {
                                state.addInputFiles(files)
                            }
                        }
                        dtde.dropComplete(true)
                    } catch (_: Exception) {
                        dtde.dropComplete(false)
                    }
                }
            })
        }

        CodecTheme(themeMode = state.config.themeMode) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left Navigation Sidebar
                Sidebar(state = state)

                // Center Dynamic Workspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (state.activeScreen) {
                        Screen.QUEUE -> QueueView(state = state)
                        Screen.PRESETS -> PresetsView(state = state)
                        Screen.TOOLS -> ToolsView(state = state)
                        Screen.INSPECTOR -> MediaInspectorView(state = state)
                        Screen.SETTINGS -> SettingsView(state = state)
                    }
                }

                // Right Task Settings Inspector (Shown when on Queue Screen)
                if (state.activeScreen == Screen.QUEUE) {
                    TaskSettingsPanel(state = state)
                }
            }

            // Full conversion settings modal dialog
            ConversionConfigDialog(state = state)

            // Logs Dialog
            state.activeJobForLogs?.let { job ->
                JobLogsDialog(
                    job = job,
                    onDismiss = { state.activeJobForLogs = null }
                )
            }
        }
    }
}
