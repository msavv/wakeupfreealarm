package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.QrAlarmApplication
import com.example.R
import com.example.data.model.AlarmEntity
import com.example.ui.alarm.AlarmRingingActivity
import com.example.util.AlarmScheduler
import com.example.util.AudioPlayerHelper
import com.example.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var muteGraceJob: Job? = null
    private var watchdogJob: Job? = null
    private var currentAlarm: AlarmEntity? = null
    private var muteCount: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        when (action) {
            ACTION_START_ALARM -> {
                if (alarmId != -1L) {
                    startAlarmRinging(alarmId)
                }
            }
            ACTION_START_PREVIEW -> {
                startAlarmPreview(alarmId)
            }
            ACTION_MUTE_GRACE_30S -> {
                activateMuteGracePeriod()
            }
            ACTION_SNOOZE -> {
                handleSnooze()
            }
            ACTION_DISMISS -> {
                handleDismiss()
            }
            ACTION_STOP_SERVICE -> {
                stopAlarmRinging()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startAlarmRinging(alarmId: Long) {
        serviceScope.launch {
            val repo = QrAlarmApplication.instance.repository
            val alarm = repo.getAlarmById(alarmId) ?: return@launch
            currentAlarm = alarm
            muteCount = 0
            val maxMute = alarm.maxMuteCount
            _activeAlarmState.value = ActiveRingingState(
                alarm = alarm,
                isMutedGrace = false,
                remainingGraceSeconds = 0,
                isPreview = false,
                currentMuteCount = 0,
                maxMuteCount = maxMute
            )

            val notification = buildForegroundNotification(alarm)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Start Audio playback with fade-in and volume control
            AudioPlayerHelper.startAlarmAudio(
                context = applicationContext,
                audioUriString = alarm.audioUri,
                audioTitle = alarm.audioTitle,
                fadeInSeconds = alarm.fadeInDurationSeconds,
                volumePercent = alarm.volumePercent,
                scope = serviceScope
            )

            // Start Vibration
            if (alarm.vibrateEnabled) {
                VibrationHelper.startVibration(
                    context = applicationContext,
                    patternName = alarm.vibrationPattern,
                    intensityName = alarm.vibrationIntensity,
                    repeat = true
                )
            }

            // Watchdog: continuously enforce locked volume
            watchdogJob?.cancel()
            watchdogJob = serviceScope.launch {
                while (isActive) {
                    delay(1500L)
                    val activeAlarm = currentAlarm ?: break
                    if (activeAlarm.lockVolumeButtons) {
                        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        am?.let { audioManager ->
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                            val target = ((activeAlarm.volumePercent / 100f) * maxVol).toInt().coerceIn(1, maxVol)
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    private fun activateMuteGracePeriod() {
        val alarm = currentAlarm ?: return
        val maxMute = alarm.maxMuteCount
        if (maxMute == 0) return // Mute disabled
        if (maxMute > 0 && muteCount >= maxMute) return // Limit reached

        muteGraceJob?.cancel()
        muteCount++
        AudioPlayerHelper.setMuteGrace(true)
        VibrationHelper.stopVibration(applicationContext)

        serviceScope.launch {
            val repo = QrAlarmApplication.instance.repository
            val settings = repo.getSettingsSync()
            val totalGrace = settings?.defaultMuteGraceSeconds ?: 30

            _activeAlarmState.value = _activeAlarmState.value?.copy(
                isMutedGrace = true,
                remainingGraceSeconds = totalGrace,
                currentMuteCount = muteCount,
                maxMuteCount = maxMute
            )
            updateForegroundNotification()

            muteGraceJob = serviceScope.launch {
                for (sec in totalGrace downTo 1) {
                    _activeAlarmState.value = _activeAlarmState.value?.copy(
                        isMutedGrace = true,
                        remainingGraceSeconds = sec,
                        currentMuteCount = muteCount,
                        maxMuteCount = maxMute
                    )
                    delay(1000L)
                }
                // Grace expired, resume sound and vibration!
                AudioPlayerHelper.setMuteGrace(false)
                val current = currentAlarm
                if (current != null && current.vibrateEnabled) {
                    VibrationHelper.startVibration(
                        context = applicationContext,
                        patternName = current.vibrationPattern,
                        intensityName = current.vibrationIntensity,
                        repeat = true
                    )
                }
                _activeAlarmState.value = _activeAlarmState.value?.copy(
                    isMutedGrace = false,
                    remainingGraceSeconds = 0,
                    currentMuteCount = muteCount,
                    maxMuteCount = maxMute
                )
                updateForegroundNotification()
            }
        }
    }

    private fun handleSnooze() {
        val alarm = currentAlarm ?: return
        serviceScope.launch {
            val repo = QrAlarmApplication.instance.repository
            val nextSnoozeCount = alarm.currentSnoozeCount + 1
            repo.updateSnoozeCount(alarm.id, nextSnoozeCount)
            AlarmScheduler.scheduleSnooze(applicationContext, alarm)

            stopAlarmRinging()
            stopSelf()
        }
    }

    private fun handleDismiss() {
        val alarm = currentAlarm ?: return
        if (_activeAlarmState.value?.isPreview == true) {
            stopAlarmRinging()
            stopSelf()
            return
        }

        serviceScope.launch {
            val repo = QrAlarmApplication.instance.repository
            repo.updateSnoozeCount(alarm.id, 0)

            if (alarm.daysOfWeek == AlarmEntity.DAYS_ONCE) {
                // Disable one-off alarm
                repo.setAlarmEnabled(alarm.id, false)
            } else {
                // Reschedule recurring alarm for next cycle
                AlarmScheduler.scheduleAlarm(applicationContext, alarm)
            }

            stopAlarmRinging()
            stopSelf()
        }
    }

    private fun startAlarmPreview(alarmId: Long) {
        serviceScope.launch {
            val repo = QrAlarmApplication.instance.repository
            val alarm = (if (alarmId != -1L) repo.getAlarmById(alarmId) else null) ?: previewAlarmHolder ?: return@launch
            currentAlarm = alarm
            muteCount = 0
            val maxMute = alarm.maxMuteCount
            _activeAlarmState.value = ActiveRingingState(
                alarm = alarm,
                isMutedGrace = false,
                remainingGraceSeconds = 0,
                isPreview = true,
                currentMuteCount = 0,
                maxMuteCount = maxMute
            )

            val notification = buildForegroundNotification(alarm)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            AudioPlayerHelper.startAlarmAudio(
                context = applicationContext,
                audioUriString = alarm.audioUri,
                audioTitle = alarm.audioTitle,
                fadeInSeconds = alarm.fadeInDurationSeconds,
                volumePercent = alarm.volumePercent,
                scope = serviceScope
            )

            if (alarm.vibrateEnabled) {
                VibrationHelper.startVibration(
                    context = applicationContext,
                    patternName = alarm.vibrationPattern,
                    intensityName = alarm.vibrationIntensity,
                    repeat = true
                )
            }
        }
    }

    private fun stopAlarmRinging() {
        watchdogJob?.cancel()
        watchdogJob = null
        muteGraceJob?.cancel()
        muteGraceJob = null
        muteCount = 0
        AudioPlayerHelper.stopAudio()
        VibrationHelper.stopVibration(applicationContext)
        _activeAlarmState.value = null
        currentAlarm = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val alarm = currentAlarm
        if (alarm != null) {
            // Alarm is ringing - cannot be swiped from task view! Relaunch immediately.
            val reopenIntent = Intent(applicationContext, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            }
            startActivity(reopenIntent)
        }
    }

    private fun updateForegroundNotification() {
        val alarm = currentAlarm ?: return
        try {
            val notification = buildForegroundNotification(alarm)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun buildForegroundNotification(alarm: AlarmEntity): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute Grace intent
        val muteIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_MUTE_GRACE_30S
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val maxMute = alarm.maxMuteCount
        val canMute = (maxMute < 0 || muteCount < maxMute) && maxMute != 0

        val builder = NotificationCompat.Builder(this, QrAlarmApplication.CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("QR Alarm: ${alarm.title}")
            .setContentText("Scan your QR code to turn off the alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)

        if (canMute) {
            val muteLabel = if (maxMute > 0) "Mute for a while ($muteCount/$maxMute)" else "Mute for a while"
            builder.addAction(0, muteLabel, mutePendingIntent)
        }
        builder.addAction(0, "Open Scanner", fullScreenPendingIntent)

        return builder.build()
    }

    override fun onDestroy() {
        stopAlarmRinging()
        serviceScope.cancel()
        super.onDestroy()
    }

    data class ActiveRingingState(
        val alarm: AlarmEntity,
        val isMutedGrace: Boolean = false,
        val remainingGraceSeconds: Int = 0,
        val isPreview: Boolean = false,
        val currentMuteCount: Int = 0,
        val maxMuteCount: Int = 3
    )

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_ALARM = "com.aistudio.qralarm.ACTION_START_ALARM"
        const val ACTION_START_PREVIEW = "com.aistudio.qralarm.ACTION_START_PREVIEW"
        const val ACTION_MUTE_GRACE_30S = "com.aistudio.qralarm.ACTION_MUTE_GRACE_30S"
        const val ACTION_SNOOZE = "com.aistudio.qralarm.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.aistudio.qralarm.ACTION_DISMISS"
        const val ACTION_STOP_SERVICE = "com.aistudio.qralarm.ACTION_STOP_SERVICE"

        var previewAlarmHolder: AlarmEntity? = null

        private val _activeAlarmState = MutableStateFlow<ActiveRingingState?>(null)
        val activeAlarmState = _activeAlarmState.asStateFlow()

        fun startPreview(context: Context, alarm: AlarmEntity) {
            previewAlarmHolder = alarm
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_PREVIEW
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                putExtra(AlarmRingingActivity.EXTRA_IS_PREVIEW, true)
            }
            context.startActivity(activityIntent)
        }

        fun triggerMuteGrace(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_MUTE_GRACE_30S
            }
            context.startService(intent)
        }

        fun triggerSnooze(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_SNOOZE
            }
            context.startService(intent)
        }

        fun triggerDismiss(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }
    }
}
