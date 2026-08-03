package com.towerbreak.towerbreakgame.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.towerbreak.towerbreakgame.core.di.ApplicationScope
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.MusicBed
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns all sound: a pooled player for one-shot cues plus a single looping
 * [MediaPlayer] for the music bed (the Flutter `AudioDesk`).
 *
 * Fingerprint note: `audioplayers` (one throwaway player per cue) is replaced by
 * the platform-native SoundPool, which is purpose-built for low-latency
 * overlapping game SFX. Volumes and mutes are driven reactively off the
 * [SettingsRepository] flow rather than a `ChangeNotifier` listener.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : GameAudio {

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(attributes)
        .build()

    private val cueIds = mutableMapOf<SoundEffect, Int>()
    private val loaded = mutableSetOf<Int>()

    private var bed: MediaPlayer? = null
    private var bedTrack: MusicBed? = null
    private var inForeground = true

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
        SoundEffect.entries.forEach { effect ->
            runCatching {
                context.assets.openFd(effect.asset).use { afd ->
                    cueIds[effect] = pool.load(afd, 1)
                }
            }
        }
        // React to volume / mute changes exactly like the old knob listener.
        settings.state
            .onEach { s ->
                val player = bed
                if (s.trackOn && inForeground) {
                    player?.setVolume(s.trackGain, s.trackGain)
                    if (player != null && !player.isPlaying) runCatching { player.start() }
                } else {
                    runCatching { player?.pause() }
                }
            }
            .launchIn(scope)
    }

    override fun playCue(effect: SoundEffect) {
        val s = settings.state.value
        if (!s.cuesOn) return
        val id = cueIds[effect] ?: return
        if (id !in loaded) return
        pool.play(id, s.cueGain, s.cueGain, 1, 0, 1f)
    }

    override fun playBed(bed: MusicBed) {
        if (bedTrack == bed && this.bed?.isPlaying == true) return
        bedTrack = bed
        val s = settings.state.value
        if (!s.trackOn || !inForeground) return
        restartBed(bed, s.trackGain)
    }

    private fun restartBed(track: MusicBed, gain: Float) {
        releaseBed()
        runCatching {
            context.assets.openFd(track.asset).use { afd ->
                bed = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = true
                    setVolume(gain, gain)
                    prepare()
                    start()
                }
            }
        }
    }

    override fun stopBed() {
        bedTrack = null
        releaseBed()
    }

    override fun buzz(heavy: Boolean) {
        if (!settings.state.value.buzzOn) return
        val vibrator = vibrator() ?: return
        val ms = if (heavy) 40L else 15L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(ms)
        }
    }

    override fun onForeground(inForeground: Boolean) {
        if (this.inForeground == inForeground) return
        this.inForeground = inForeground
        val s = settings.state.value
        if (!inForeground) {
            runCatching { bed?.pause() }
        } else if (s.trackOn) {
            val track = bedTrack
            if (track != null) {
                if (bed == null) restartBed(track, s.trackGain) else runCatching { bed?.start() }
            }
        }
    }

    override fun release() {
        pool.release()
        releaseBed()
    }

    private fun releaseBed() {
        runCatching { bed?.release() }
        bed = null
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
