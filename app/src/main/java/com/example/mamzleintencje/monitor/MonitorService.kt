package com.example.mamzleintencje.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private lateinit var monitor: IntentMonitor

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        monitor = IntentMonitor(this, serviceScope) { state ->
            Log.d("MonitorService", "Background State: $state")
        }
        monitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedPrefs = getSharedPreferences("monitor_settings", Context.MODE_PRIVATE)
        val periodSeconds = intent?.getIntExtra("PERIOD_SECONDS", -1)?.takeIf { it != -1 }
            ?: sharedPrefs.getInt("fetch_period_seconds", 30)

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimerLoop(periodSeconds)

        return START_STICKY // Tells the OS to recreate the service if killed [1]
    }    private fun startTimerLoop(periodSeconds: Int) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                Log.d("Service", "scan")
                monitor.triggerScan()
                delay(periodSeconds * 1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        monitor.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Intent Monitor Background Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Intent Monitor Running")
            .setContentText("Scanning broadcasts in the background...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "monitor_service_channel"
    }
}