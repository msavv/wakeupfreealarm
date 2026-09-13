package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Alarm",
    val hour: Int = 7, // 0-23
    val minute: Int = 0, // 0-59
    val isEnabled: Boolean = true,
    // Bitmask for recurring days:
    // Mon = 1, Tue = 2, Wed = 4, Thu = 8, Fri = 16, Sat = 32, Sun = 64
    // 0 indicates one-time alarm
    val daysOfWeek: Int = DAYS_WEEKDAYS,
    val targetBarcodeId: Long? = null,
    val audioUri: String? = null, // null for built-in preset
    val audioTitle: String = "Digital Alarm",
    val fadeInDurationSeconds: Int = 30, // 0 for instant, 15, 30, 60, 120, 300
    val vibrateEnabled: Boolean = true,
    val vibrationPattern: String = "PULSE", // PULSE, STEADY, HEARTBEAT, ESCALATING, STACCATO
    val vibrationIntensity: String = "MEDIUM", // SOFT, MEDIUM, STRONG
    val snoozeDurationMinutes: Int = 5, // 0 = disabled, 3, 5, 10, 15, 20
    val maxSnoozeCount: Int = 3, // 0 = no snooze, 1, 2, 3, 5, -1 = unlimited
    val currentSnoozeCount: Int = 0,
    val lastTriggeredTimestamp: Long = 0L,
    val volumePercent: Int = 100, // 10 to 100
    val lockVolumeButtons: Boolean = true, // Prevent lowering volume with hardware keys
    val qrOnlyDismiss: Boolean = true, // Alarm can only be dismissed by scanning QR code
    val maxMuteCount: Int = 3 // 0 = disabled, 1, 2, 3, 5, -1 = unlimited
) {
    companion object {
        const val DAY_MON = 1
        const val DAY_TUE = 2
        const val DAY_WED = 4
        const val DAY_THU = 8
        const val DAY_FRI = 16
        const val DAY_SAT = 32
        const val DAY_SUN = 64

        const val DAYS_ONCE = 0
        const val DAYS_WEEKDAYS = DAY_MON or DAY_TUE or DAY_WED or DAY_THU or DAY_FRI
        const val DAYS_WEEKEND = DAY_SAT or DAY_SUN
        const val DAYS_EVERYDAY = DAYS_WEEKDAYS or DAYS_WEEKEND

        fun isDaySelected(daysOfWeek: Int, dayFlag: Int): Boolean {
            return (daysOfWeek and dayFlag) != 0
        }

        fun toggleDay(currentMask: Int, dayFlag: Int): Int {
            return currentMask xor dayFlag
        }
    }
}
