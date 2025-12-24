package org.maplibre.navigation.sample.android.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.maplibre.navigation.sample.android.R

class NavigationNotificationService : Service() {
    companion object {
        const val CHANNEL_ID = "navigation_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_INSTRUCTION = "extra_instruction"
        const val EXTRA_ICON_RES = "extra_icon_res"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val instruction = intent?.getStringExtra(EXTRA_INSTRUCTION) ?: "Navegando..."
        val iconRes = intent?.getIntExtra(EXTRA_ICON_RES, R.drawable.ic_navigation) ?: R.drawable.ic_navigation
        val notification = buildNotification(instruction, iconRes)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(instruction: String, iconRes: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS")
            .setContentText(instruction)
            .setSmallIcon(iconRes)
            .setLargeIcon(BitmapFactory.decodeResource(resources, iconRes))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Navegación GPS",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones de instrucciones de navegación"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(instruction: String, iconRes: Int) {
        val notification = buildNotification(instruction, iconRes)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}

