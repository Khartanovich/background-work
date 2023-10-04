package com.example.backgroundworks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class MyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        if (intent.action != ALARM_ACTION) {
            // not out case, ignore
            return
        }

        val soundIntent = Intent(context, SoundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(soundIntent)
        }
    }

    companion object {
        const val ALARM_ACTION = "W_ACTION"
        private const val ALARM_INPUT_KEY = "W_INPUT"
        fun createIntent(context: Context, message: String): Intent {
            return Intent(context, MyAlarmReceiver::class.java).apply {
                action = ALARM_ACTION
                putExtra(ALARM_INPUT_KEY, message)
            }
        }
    }
}