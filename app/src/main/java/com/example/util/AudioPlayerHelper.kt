package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"

    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var fadeInJob: Job? = null
    private var isMutedGrace: Boolean = false
    private var targetVolume: Float = 1.0f
    private var currentVolume: Float = 1.0f

    val PRESETS = listOf(
        "Digital Alarm",
        "Gentle Chimes",
        "Morning Bell",
        "Energetic Siren",
        "System Default"
    )

    fun startAlarmAudio(
        context: Context,
        audioUriString: String?,
        audioTitle: String,
        fadeInSeconds: Int,
        volumePercent: Int = 100,
        scope: CoroutineScope
    ) {
        stopAudio()
        isMutedGrace = false

        val maxVolume = (volumePercent.coerceIn(10, 100) / 100f)
        targetVolume = maxVolume
        val initialVol = if (fadeInSeconds > 0) (0.005f * maxVolume).coerceAtLeast(0.001f) else maxVolume
        currentVolume = initialVol

        // Enforce system alarm stream volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxStreamVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetStreamVol = ((volumePercent / 100f) * maxStreamVol).toInt().coerceIn(1, maxStreamVol)
                am.setStreamVolume(AudioManager.STREAM_ALARM, targetStreamVol, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting stream volume", e)
        }

        var playedCustom = false
        if (!audioUriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(audioUriString)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    setVolume(initialVol, initialVol)
                    prepare()
                    start()
                }
                playedCustom = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing custom URI: $audioUriString", e)
            }
        }

        if (!playedCustom) {
            if (audioTitle == "System Default") {
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(context, alertUri)
                        isLooping = true
                        setVolume(initialVol, initialVol)
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    startSynthesizedSound(audioTitle, initialVol, scope)
                }
            } else {
                startSynthesizedSound(audioTitle, initialVol, scope)
            }
        }

        // Handle volume fade-in with smooth quadratic easing
        if (fadeInSeconds > 0) {
            fadeInJob = scope.launch(Dispatchers.Default) {
                val totalMs = fadeInSeconds * 1000L
                val intervalMs = 250L
                val totalSteps = (totalMs / intervalMs).toInt().coerceAtLeast(1)

                for (step in 1..totalSteps) {
                    delay(intervalMs)
                    if (!isActive) break
                    val progress = step.toFloat() / totalSteps
                    // Quadratic easing (progress^2) ensures a very quiet initial start that ramps up naturally
                    val vol = (initialVol + (targetVolume - initialVol) * (progress * progress)).coerceIn(0.001f, targetVolume)
                    currentVolume = vol
                    if (!isMutedGrace) {
                        applyVolume(vol)
                    }
                }
                currentVolume = targetVolume
                if (!isMutedGrace) {
                    applyVolume(targetVolume)
                }
            }
        }
    }

    private fun startSynthesizedSound(title: String, initialVolume: Float, scope: CoroutineScope) {
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(initialVolume)
            track.play()
            synthTrack = track

            synthJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(1024)
                var phase = 0.0
                var noteTimer = 0

                val melodyFrequencies = when (title) {
                    "Gentle Chimes" -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 783.99) // C5, E5, G5, C6
                    "Morning Bell" -> doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A4, C#5, E5, A5
                    "Energetic Siren" -> doubleArrayOf(600.0, 950.0, 600.0, 1100.0)
                    else -> doubleArrayOf(880.0, 880.0, 0.0, 880.0, 880.0, 0.0) // Digital Alarm beep-beep
                }
                val noteDurationSamples = (sampleRate * 0.28).toInt()

                var noteIndex = 0
                while (isActive) {
                    val freq = melodyFrequencies[noteIndex % melodyFrequencies.size]
                    val phaseIncrement = (2.0 * Math.PI * freq) / sampleRate

                    for (i in buffer.indices) {
                        if (freq <= 0.0) {
                            buffer[i] = 0
                        } else {
                            val sample = sin(phase)
                            // Add slight harmonics for warmth
                            val harmonics = 0.75 * sample + 0.25 * sin(phase * 2.0)
                            val volFactor = if (isMutedGrace) 0.0f else currentVolume.coerceIn(0.0f, 1.0f)
                            buffer[i] = (harmonics * 24000 * volFactor).toInt().toShort()
                            phase += phaseIncrement
                        }
                        noteTimer++
                        if (noteTimer >= noteDurationSamples) {
                            noteTimer = 0
                            noteIndex++
                            break
                        }
                    }

                    if (synthTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        synthTrack?.write(buffer, 0, buffer.size)
                    } else {
                        delay(20)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack synthesis error", e)
        }
    }

    fun setMuteGrace(muted: Boolean) {
        isMutedGrace = muted
        if (muted) {
            applyVolume(0.0f)
        } else {
            applyVolume(currentVolume)
        }
    }

    private fun applyVolume(vol: Float) {
        try {
            val safeVol = vol.coerceIn(0.0f, 1.0f)
            mediaPlayer?.setVolume(safeVol, safeVol)
            synthTrack?.setVolume(safeVol)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying volume", e)
        }
    }

    fun stopAudio() {
        fadeInJob?.cancel()
        fadeInJob = null
        synthJob?.cancel()
        synthJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null

        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        synthTrack = null
        isMutedGrace = false
    }

    fun playPreview(context: Context, audioUriString: String?, audioTitle: String, volumePercent: Int = 100, scope: CoroutineScope) {
        startAlarmAudio(context, audioUriString, audioTitle, fadeInSeconds = 0, volumePercent = volumePercent, scope = scope)
        scope.launch {
            delay(4000)
            stopAudio()
        }
    }

    fun getDisplayName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = cursor.getString(index)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isNullOrBlank()) {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                if (cut != -1) {
                    name = path.substring(cut + 1)
                }
            }
        }
        return name?.takeIf { it.isNotBlank() } ?: "Custom Audio File"
    }
}
