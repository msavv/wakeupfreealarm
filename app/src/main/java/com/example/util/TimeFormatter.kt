package com.example.util

import com.example.data.model.AlarmEntity
import java.util.Calendar
import java.util.Locale

object TimeFormatter {

    fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String {
        return if (use24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
        }
    }

    fun formatRepeatDays(daysOfWeek: Int): String {
        return when (daysOfWeek) {
            AlarmEntity.DAYS_ONCE -> "Once"
            AlarmEntity.DAYS_EVERYDAY -> "Every day"
            AlarmEntity.DAYS_WEEKDAYS -> "Weekdays"
            AlarmEntity.DAYS_WEEKEND -> "Weekends"
            else -> {
                val days = mutableListOf<String>()
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_MON)) days.add("Mon")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_TUE)) days.add("Tue")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_WED)) days.add("Wed")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_THU)) days.add("Thu")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_FRI)) days.add("Fri")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_SAT)) days.add("Sat")
                if (AlarmEntity.isDaySelected(daysOfWeek, AlarmEntity.DAY_SUN)) days.add("Sun")
                days.joinToString(", ")
            }
        }
    }

    fun getTimeRemainingDescription(nextTriggerMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = nextTriggerMillis - now
        if (diff <= 0) return "Alarm is due now"

        val totalMinutes = diff / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 24 -> {
                val days = hours / 24
                val remainingHours = hours % 24
                "Alarm in $days d $remainingHours h"
            }
            hours > 0 -> "Alarm in $hours h $minutes min"
            else -> "Alarm in $minutes min"
        }
    }

    fun calculateNextTriggerMillis(hour: Int, minute: Int, daysOfWeek: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek == AlarmEntity.DAYS_ONCE) {
            // One-time alarm
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Recurring alarm
        for (dayOffset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (candidate.timeInMillis <= now.timeInMillis) {
                continue
            }
            val calDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            val flag = when (calDayOfWeek) {
                Calendar.MONDAY -> AlarmEntity.DAY_MON
                Calendar.TUESDAY -> AlarmEntity.DAY_TUE
                Calendar.WEDNESDAY -> AlarmEntity.DAY_WED
                Calendar.THURSDAY -> AlarmEntity.DAY_THU
                Calendar.FRIDAY -> AlarmEntity.DAY_FRI
                Calendar.SATURDAY -> AlarmEntity.DAY_SAT
                Calendar.SUNDAY -> AlarmEntity.DAY_SUN
                else -> 0
            }
            if (AlarmEntity.isDaySelected(daysOfWeek, flag)) {
                return candidate.timeInMillis
            }
        }

        // Fallback next week same day
        target.add(Calendar.DAY_OF_YEAR, 7)
        return target.timeInMillis
    }
}
