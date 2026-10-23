package io.codec.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.codec.model.TaskJob
import io.codec.model.TaskStatus
import io.codec.queue.TaskQueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class ConversionForegroundService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = buildNotification("Codec Engine Active", "Ready for background conversions", 0, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // Monitor active conversions and update notification
        val queueDir = File(filesDir, ".codec")
        val queueManager = TaskQueueManager(storageDir = queueDir)

        serviceScope.launch {
            queueManager.jobs.collectLatest { jobs ->
                val running = jobs.firstOrNull { it.status == TaskStatus.RUNNING }
                if (running != null) {
                    val notif = buildNotification(
                        title = "Converting: ${running.inputFileName}",
                        content = "${String.format("%.1f", running.progress)}% • ${String.format("%.1f", running.speedMultiplier)}x • ETA ${running.formattedEta}",
                        progress = running.progress.toInt(),
                        indeterminate = false
                    )
                    notificationManager.notify(NOTIFICATION_ID, notif)
                } else {
                    val anyActive = jobs.any { it.status == TaskStatus.PENDING || it.status == TaskStatus.PROBING }
                    if (!anyActive) {
                        stopSelf()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Conversion Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time media encoding progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String, progress: Int, indeterminate: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "codec_conversion_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ConversionForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ConversionForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
