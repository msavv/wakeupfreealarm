package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.AlarmEntity
import com.example.receiver.AlarmReceiver

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val ACTION_TRIGGER_ALARM = "com.aistudio.qralarm.ACTION_TRIGGER_ALARM"

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerMillis = TimeFormatter.calculateNextTriggerMillis(
            alarm.hour,
            alarm.minute,
            alarm.daysOfWeek
        )

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using setAlarmClock gives highest priority and shows alarm clock icon in system bar
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled alarm ${alarm.id} for $triggerMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm: ${e.message}")
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback scheduling failed", ex)
            }
        }
    }

    fun scheduleSnooze(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeMillis = System.currentTimeMillis() + (alarm.snoozeDurationMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt() + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt() + 100000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(snoozeMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled snooze for alarm ${alarm.id} at $snoozeMillis")
        } catch (e: Exception) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Also cancel snooze pending intent if any
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 100000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }
}
