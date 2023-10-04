package com.example.backgroundworks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.shredzone.commons.suncalc.SunTimes

class MyWork(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        val userHour = inputData.getInt(Constants.USER_HOUR, 7)
        val userMinute = inputData.getInt(Constants.USER_MINUTE, 35)
        val latitude = inputData.getDouble(Constants.LATITUDE, 0.0)
        val longitude = inputData.getDouble(Constants.LONGITUDE, 0.0)
        val timeZone = inputData.getString(Constants.TIME_ZONE)
        val timeSunRise = getSunRise(timeZone, longitude, latitude)

        val hourAlarm = timeSunRise.split(":").first().toInt() + userHour
        val minuteAlarm = timeSunRise.split(":")[1].toInt() + userMinute
        val okMinuteAlarm = if (minuteAlarm < 60) minuteAlarm else (minuteAlarm - 60)
        val okHourAlarm = if (hourAlarm < 24 && minuteAlarm < 60) {
            hourAlarm
        } else if (hourAlarm < 24 && minuteAlarm > 60) {
            if (hourAlarm + 1 < 24) {
                hourAlarm + 1
            } else {
                0
            }
        } else if (hourAlarm == 24) {
            0
        } else {
            hourAlarm - 24
        }

        val timeAlarm = "$okHourAlarm : $okMinuteAlarm "
        val outputData = Data.Builder()
            .putInt(Constants.HOUR_ALARM, okHourAlarm)
            .putInt(Constants.MINUTE_ALARM, okMinuteAlarm)
            .build()

        val text = "будильник установлен на $timeAlarm"
        context.createNotificationChannel(CHANNEL_ID)
        context.getNotificationManager()
            .notify(text.hashCode(), createNotification(context, text))
        return Result.success(outputData)
    }

    private fun getSunRise(timeZone: String?, longitude: Double, latitude: Double): String {
        val sunRise = SunTimes.compute().tomorrow()
            .timezone(timeZone).at(latitude, longitude).execute()
        return sunRise.rise.toString().substring(11, 19)
    }

    private fun createNotification(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timing")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun Context.createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = descriptionText
            val notificationManager = getNotificationManager()
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun Context.getNotificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CHANNEL_ID = "MyWork channel id"
    }
}