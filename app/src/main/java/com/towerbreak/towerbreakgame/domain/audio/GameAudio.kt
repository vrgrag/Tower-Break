package com.towerbreak.towerbreakgame.domain.audio

/**
 * One-shot sound effects (the Flutter `Cue`). Each carries the bundled asset
 * filename; the audio implementation resolves it against `assets/sfx`.
 */
enum class SoundEffect(val asset: String) {
    TAP("sfx/button_click.wav"),
    SEAT("sfx/block_place.wav"),
    CHIME("sfx/success.wav"),
    TILL("sfx/cashout.wav"),
    COIN("sfx/coin.wav"),
    THUD("sfx/fail.wav"),
    FANFARE("sfx/level_up.wav"),
    TRIUMPH("sfx/win.wav"),
    RELEASE("sfx/drop.wav"),
}

/** Looping music beds (the Flutter `Track`). Both currently share one bed. */
enum class MusicBed(val asset: String) {
    HUB("sfx/music.wav"),
    ARENA("sfx/music.wav"),
}

/**
 * Audio abstraction the game talks to. Concretely backed by SoundPool (cues) and
 * MediaPlayer (bed) in the data layer.
 *
 * Fingerprint note: the Flutter `AudioDesk` was a concrete class the whole tree
 * reached into. Promoting it to an interface lets the ViewModels depend on the
 * contract and lets tests swap in a silent double.
 */
interface GameAudio {
    fun playCue(effect: SoundEffect)
    fun playBed(bed: MusicBed)
    fun stopBed()
    fun buzz(heavy: Boolean = false)

    /** Called by the host activity so the bed can pause off-foreground. */
    fun onForeground(inForeground: Boolean)
    fun release()
}
