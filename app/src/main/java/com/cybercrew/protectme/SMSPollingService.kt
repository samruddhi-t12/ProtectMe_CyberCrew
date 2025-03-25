package com.cybercrew.protectme



import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class SMSPollingService : Service() {
    private val handler = Handler()
    private val pollingInterval = 5000L  // ✅ Poll every 5 seconds

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "SMSPollingChannel"
            val channelName = "SMS Polling Service"
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(channelId, channelName, importance)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("🔄 Monitoring SMS in Background...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(1, notification)
        }

        startPolling()
    }

    private fun startPolling() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d("SMS_DEBUG", "🔄 Polling SMS...")
                SMSUtils.fetchLatestSMS(this@SMSPollingService)  // ✅ Fetch new SMS only
                handler.postDelayed(this, pollingInterval)  // ✅ Re-run every 5 sec
            }
        }, pollingInterval)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w("SMS_DEBUG", "⚠ App removed from recents, restarting service...")
        val restartIntent = Intent(applicationContext, SMSPollingService::class.java)
        restartIntent.setPackage(packageName)
        startService(restartIntent)
    }
}
