package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.QrAlarmApplication
import com.example.service.AlarmRingingService
import com.example.ui.alarm.AlarmRingingActivity
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "AlarmReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms on reboot
            val repository = QrAlarmApplication.instance.repository
            CoroutineScope(Dispatchers.IO).launch {
                val enabledAlarms = repository.getEnabledAlarmsSync()
                for (alarm in enabledAlarms) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            }
            return
        }

        if (action == AlarmScheduler.ACTION_TRIGGER_ALARM) {
            val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
            if (alarmId == -1L) return

            // Acquire temporary WakeLock
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "QrAlarm:AlarmWakeLock"
            )
            wakeLock.acquire(30000L) // 30 seconds safety timeout

            // Start foreground ringing service
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                this.action = AlarmRingingService.ACTION_START_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
            }
            ContextCompat.startForegroundService(context, serviceIntent)

            // Launch full-screen ringing activity
            val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
            }
            context.startActivity(activityIntent)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
