package com.towerbreak.towerbreakgame.init

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.data.repository.Hydratable
import com.towerbreak.towerbreakgame.presentation.game.render.PlayfieldTextures
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings the app's long-lived state up before the first real screen (the Flutter
 * `ServiceHub.assemble()` + `SpriteVault.warmUp()`).
 *
 * Ordering matters and is explicit here: seed the fresh-install defaults, hydrate
 * every persisted store from DataStore, then decode the playfield art. The splash
 * awaits this so the arena never reads un-hydrated state.
 */
@Singleton
class AppWarmUp @Inject constructor(
    private val prefs: PreferenceStore,
    private val hydratables: Set<@JvmSuppressWildcards Hydratable>,
    private val textures: PlayfieldTextures,
) {
    suspend fun run() {
        prefs.seedFreshInstall()
        hydratables.forEach { it.hydrate() }
        textures.warmUp()
    }
}
