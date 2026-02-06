package org.maplibre.navigation.sample.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.maplibre.navigation.core.navigation.AndroidMapLibreNavigation
import org.maplibre.navigation.sample.android.MainActivity
import org.maplibre.navigation.sample.android.R
import org.maplibre.navigation.sample.android.auto.MyCarAppService
import org.maplibre.navigation.sample.android.NavigationHolder

class NavigationService : Service() {

    private val channelId = "NavigationServiceChannel"
    private lateinit var notificationManager: NotificationManager
    private var mapLibreNavigation: AndroidMapLibreNavigation? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mapLibreNavigation = NavigationHolder.navigation
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val instruction = intent?.getStringExtra("instruction") ?: "Starting navigation..."
        val iconRes = intent?.getIntExtra("icon_res", R.drawable.ic_navigation) ?: R.drawable.ic_navigation
        val notification = createNotification(instruction, iconRes)

        startForeground(1, notification)

        return START_STICKY
    }

    private fun createNotification(contentText: String, iconRes: Int): Notification {
        val stopIntent = Intent(this, MyCarAppService::class.java).apply {
            action = "STOP_NAVIGATION"
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Turn-by-turn Navigation")
            .setContentText(contentText)
            .setSmallIcon(iconRes)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(activityPendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText, R.drawable.ic_navigation)
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Navigation Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        NavigationHolder.navigation = null // Clear the reference when the service is destroyed
    }
}