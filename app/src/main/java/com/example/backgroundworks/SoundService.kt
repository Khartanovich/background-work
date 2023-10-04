package com.example.backgroundworks

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SoundService : Service() {

    private lateinit var ringtone: Ringtone
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ringtone.play()
        val text = "Time work, sun get up hour ago"
        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, SoundService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        ringtone.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        private const val CHANNEL_ID = "My channel id"
    }
}