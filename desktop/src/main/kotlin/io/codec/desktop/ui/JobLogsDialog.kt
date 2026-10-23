package io.codec.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.codec.model.TaskJob

@Composable
fun JobLogsDialog(
    job: TaskJob,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F1117),
            modifier = Modifier
                .width(700.dp)
                .height(500.dp)
                .border(1.dp, CodecColors.DarkBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FFmpeg Console Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CodecColors.PrimaryCyan
                        )
                        Text(
                            text = job.inputFileName,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Close", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF050608))
                        .border(1.dp, Color(0xFF1E2332), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    if (job.ffmpegLog.isEmpty()) {
                        Text("No logs captured yet...", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(job.ffmpegLog) { line ->
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.contains("error", ignoreCase = true)) CodecColors.AccentRed else Color(0xFFD2DAE2)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
