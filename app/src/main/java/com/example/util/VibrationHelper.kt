package com.example.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun startVibration(
        context: Context,
        patternName: String,
        intensityName: String,
        repeat: Boolean = true
    ) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        val timings: LongArray
        val amplitudes: IntArray

        val amplitudeValue = when (intensityName.uppercase()) {
            "SOFT" -> 80
            "STRONG" -> 255
            else -> 170 // MEDIUM
        }

        when (patternName.uppercase()) {
            "STEADY" -> {
                timings = longArrayOf(0, 1000, 400)
                amplitudes = intArrayOf(0, amplitudeValue, 0)
            }
            "HEARTBEAT" -> {
                timings = longArrayOf(0, 160, 120, 200, 600)
                amplitudes = intArrayOf(0, amplitudeValue, 0, (amplitudeValue * 0.8).toInt().coerceAtLeast(1), 0)
            }
            "ESCALATING" -> {
                timings = longArrayOf(0, 200, 200, 400, 200, 600, 300)
                val softAmp = (amplitudeValue * 0.4).toInt().coerceAtLeast(1)
                val medAmp = (amplitudeValue * 0.7).toInt().coerceAtLeast(1)
                amplitudes = intArrayOf(0, softAmp, 0, medAmp, 0, amplitudeValue, 0)
            }
            "STACCATO" -> {
                timings = longArrayOf(0, 100, 80, 100, 80, 100, 500)
                amplitudes = intArrayOf(0, amplitudeValue, 0, amplitudeValue, 0, amplitudeValue, 0)
            }
            else -> { // "PULSE"
                timings = longArrayOf(0, 400, 300, 400, 500)
                amplitudes = intArrayOf(0, amplitudeValue, 0, amplitudeValue, 0)
            }
        }

        val repeatIndex = if (repeat) 0 else -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)
                vibrator.vibrate(effect)
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, repeatIndex)
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, repeatIndex)
        }
    }

    fun stopVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)
            vibrator.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playPreview(context: Context, patternName: String, intensityName: String) {
        startVibration(context, patternName, intensityName, repeat = false)
    }

    fun vibrateOneShot(context: Context, durationMs: Long = 100L) {
        try {
            val vibrator = getVibrator(context)
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
