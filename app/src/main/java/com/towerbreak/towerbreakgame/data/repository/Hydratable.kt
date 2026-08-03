package com.towerbreak.towerbreakgame.data.repository

/**
 * A store that can pull its initial state out of DataStore on demand.
 *
 * Fingerprint note: the Flutter stores read their seed values synchronously in
 * their constructors (`_balance = vault.number(...)`). DataStore is async, so
 * hydration is an explicit suspend step the splash awaits before the graph is
 * considered "warm". This keeps constructors side-effect free and testable.
 */
interface Hydratable {
    suspend fun hydrate()
}
