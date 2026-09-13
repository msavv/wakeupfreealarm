package com.example.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.QrAlarmApplication
import com.example.data.model.AlarmEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SavedBarcodeEntity
import com.example.service.AlarmRingingService
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AlarmScheduler

class AlarmRingingActivity : ComponentActivity() {

    private var currentAlarm: AlarmEntity? = null
    private var isLegitimatelyDismissed: Boolean = false
    private var isTestPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTestPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        enableEdgeToEdge()
        setupLockScreenFlags()
        setupImmersiveMode()

        // Handle Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTestPreview || AlarmRingingService.activeAlarmState.value?.isPreview == true) {
                    isLegitimatelyDismissed = true
                    AlarmRingingService.triggerDismiss(this@AlarmRingingActivity)
                    finish()
                } else {
                    Toast.makeText(
                        this@AlarmRingingActivity,
                        "Scan your QR code to turn off the alarm",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            MyApplicationTheme {
                val repository = QrAlarmApplication.instance.repository
                val activeState by AlarmRingingService.activeAlarmState.collectAsStateWithLifecycle()

                var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
                var targetBarcode by remember { mutableStateOf<SavedBarcodeEntity?>(null) }
                var allBarcodes by remember { mutableStateOf<List<SavedBarcodeEntity>>(emptyList()) }
                var settings by remember { mutableStateOf(AppSettingsEntity()) }

                val isCurrentlyPreview = isTestPreview || (activeState?.isPreview == true)

                LaunchedEffect(alarmId) {
                    val loadedAlarm = if (alarmId != -1L) repository.getAlarmById(alarmId) else null
                    val finalAlarm = loadedAlarm ?: activeState?.alarm ?: AlarmRingingService.previewAlarmHolder
                    alarm = finalAlarm
                    currentAlarm = finalAlarm
                    val barcodeId = finalAlarm?.targetBarcodeId
                    if (barcodeId != null) {
                        targetBarcode = repository.getBarcodeById(barcodeId)
                    }
                    settings = repository.getSettingsSync()
                }

                LaunchedEffect(Unit) {
                    repository.allSavedBarcodes.collect { barcodes ->
                        allBarcodes = barcodes
                    }
                }

                val activeAlarm = alarm ?: activeState?.alarm ?: AlarmRingingService.previewAlarmHolder

                if (activeAlarm != null) {
                    AlarmRingingScreen(
                        alarm = activeAlarm,
                        targetBarcode = targetBarcode,
                        allSavedBarcodes = allBarcodes,
                        settings = settings,
                        activeRingingState = activeState,
                        isPreviewMode = isCurrentlyPreview,
                        onMute30sClicked = {
                            AlarmRingingService.triggerMuteGrace(this@AlarmRingingActivity)
                        },
                        onSnoozeClicked = {
                            isLegitimatelyDismissed = true
                            AlarmRingingService.triggerSnooze(this@AlarmRingingActivity)
                            finishAndRemoveTask()
                        },
                        onDismissSuccess = {
                            isLegitimatelyDismissed = true
                            AlarmRingingService.triggerDismiss(this@AlarmRingingActivity)
                            finishAndRemoveTask()
                        },
                        onExitPreview = {
                            isLegitimatelyDismissed = true
                            AlarmRingingService.triggerDismiss(this@AlarmRingingActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun setupLockScreenFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // Intercept Volume Buttons: Disable changing volume with buttons while ringing
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val activeAlarm = currentAlarm ?: AlarmRingingService.activeAlarmState.value?.alarm
        val lockVolume = activeAlarm?.lockVolumeButtons ?: true
        if (lockVolume) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    // Enforce stream volume at user configured alarm level
                    enforceAlarmVolume(activeAlarm)
                    return true // Consume event - prevent user from lowering or muting volume
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val activeAlarm = currentAlarm ?: AlarmRingingService.activeAlarmState.value?.alarm
        val lockVolume = activeAlarm?.lockVolumeButtons ?: true
        if (lockVolume) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    enforceAlarmVolume(activeAlarm)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun enforceAlarmVolume(alarm: AlarmEntity?) {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val percent = alarm?.volumePercent ?: 100
        val target = ((percent / 100f) * maxVol).toInt().coerceIn(1, maxVol)
        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        } catch (_: Exception) {}
    }

    // If trying to press Home button or switch apps, force alarm back on screen immediately
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isTestPreview || AlarmRingingService.activeAlarmState.value?.isPreview == true) return
        if (!isLegitimatelyDismissed && AlarmRingingService.activeAlarmState.value != null) {
            bringAlarmBackToFront()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTestPreview || AlarmRingingService.activeAlarmState.value?.isPreview == true) return
        if (!isLegitimatelyDismissed && !isFinishing && AlarmRingingService.activeAlarmState.value != null) {
            bringAlarmBackToFront()
        }
    }

    // Prevent power-off menu and system dialogs from obscuring the alarm
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (isTestPreview || AlarmRingingService.activeAlarmState.value?.isPreview == true) return
        if (!hasFocus && !isLegitimatelyDismissed && AlarmRingingService.activeAlarmState.value != null) {
            // Dismiss system dialogs (power off menu, notification shade, recent apps)
            @Suppress("DEPRECATION")
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            try {
                sendBroadcast(closeIntent)
            } catch (_: Exception) {}

            // Re-force alarm screen to front
            window.decorView.postDelayed({
                if (!isLegitimatelyDismissed && !isFinishing && AlarmRingingService.activeAlarmState.value != null) {
                    bringAlarmBackToFront()
                    setupImmersiveMode()
                }
            }, 300L)
        }
    }

    private fun bringAlarmBackToFront() {
        val bringBackIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarm?.id ?: -1L)
        }
        try {
            startActivity(bringBackIntent)
        } catch (_: Exception) {}
    }

    companion object {
        const val EXTRA_IS_PREVIEW = "extra_is_preview"
    }
}
