package com.example.fitnesstrackerapp.service

import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fitnesstrackerapp.ui.MainActivity
import kotlinx.coroutines.*

class StepForegroundService : Service() {

    private val CHANNEL_ID = "step_notification_channel"
    private val MILESTONE_CHANNEL_ID = "milestone_notification_channel"
    private val FOREGROUND_NOTIFICATION_ID = 1
    private val MILESTONE_NOTIFICATION_ID = 2

    private var job: Job? = null
    private var boundService: StepBoundService? = null
    private var isBound = false

    private val milestoneSteps = setOf(50, 100, 150)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val stepBinder = binder as? StepBoundService.StepBinder
            boundService = stepBinder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isBound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        val initialNotification = buildNotification(0, "Starting step tracking...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, initialNotification)
        }

        // Start & Bind BoundService
        val intent = Intent(this, StepBoundService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        val notificationManager = getSystemService(NotificationManager::class.java)
        var lastMilestoneShown = -1

        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            while (isActive) {
                delay(1000)
                val steps = boundService?.getStepCount() ?: 0
                val quote = getRandomMotivationalQuote()

                // Update foreground notification
                val updatedNotification = buildNotification(steps, quote)
                notificationManager?.notify(FOREGROUND_NOTIFICATION_ID, updatedNotification)

                // Show milestone notification khi bước >= mốc milestone chưa hiển thị
                val nextMilestone = milestoneSteps.filter { it > lastMilestoneShown }.minOrNull()
                if (nextMilestone != null && steps >= nextMilestone) {
                    lastMilestoneShown = nextMilestone
                    showMilestoneNotification(nextMilestone)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        super.onDestroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.enableLights(true)
            channel.enableVibration(false)

            val milestoneChannel = NotificationChannel(
                MILESTONE_CHANNEL_ID,
                "Milestone Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            milestoneChannel.enableLights(true)
            milestoneChannel.enableVibration(true)
            milestoneChannel.vibrationPattern = longArrayOf(0, 250, 250, 250)

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(milestoneChannel)
        }
    }

    private fun buildNotification(steps: Int, quote: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Steps: $steps")
            .setContentText(quote)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showMilestoneNotification(steps: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val milestoneNotification = NotificationCompat.Builder(this, MILESTONE_CHANNEL_ID)
            .setContentTitle("Milestone reached!")
            .setContentText("You have reached $steps steps!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(MILESTONE_NOTIFICATION_ID, milestoneNotification)
    }

    private fun getRandomMotivationalQuote(): String {
        val quotes = listOf(
            "Keep going, you're doing great!",
            "Every step counts!",
            "Stay strong and keep moving!",
            "You got this!",
            "Almost there!"
        )
        return quotes.random()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
