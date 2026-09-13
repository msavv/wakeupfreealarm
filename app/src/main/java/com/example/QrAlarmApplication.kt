package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QrAlarmApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        repository = AlarmRepository(database.alarmDao(), database.savedBarcodeDao(), database.appSettingsDao())

        createNotificationChannels()
        seedDefaultDataIfEmpty()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "QR Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Foreground alarm ringing alerts"
                setSound(null, null) // Handled in-service for custom volume fade-in
                enableVibration(false) // Handled in-service for custom patterns
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "QR Alarm Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing alarm service status"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun seedDefaultDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedDefaultsIfEmpty()
        }
    }

    companion object {
        const val CHANNEL_ALARM_ID = "qr_alarm_channel_active"
        const val CHANNEL_SERVICE_ID = "qr_alarm_channel_service"
        lateinit var instance: QrAlarmApplication
            private set
    }
}
