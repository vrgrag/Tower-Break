package com.towerbreak.towerbreakgame.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tower_break_prefs")

/**
 * The only thing in the app that talks to DataStore (the Flutter `PrefsVault`).
 *
 * Fingerprint note: SharedPreferences becomes Jetpack DataStore, so reads/writes
 * are coroutine-based rather than synchronous. Every logical slot keeps its
 * original on-disk name (see [Keys]) — those names are a wire format and renaming
 * one would silently discard a returning player's progress.
 */
@Singleton
class PreferenceStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.dataStore

    /** The frozen on-disk schema. Values live in [Defaults]. */
    object Keys {
        val purse = intPreferencesKey("ts_coins")
        val experience = intPreferencesKey("ts_xp")
        val seeded = booleanPreferencesKey("ts_first_run_done")

        val topPayout = intPreferencesKey("ts_best_win")
        val topHeight = intPreferencesKey("ts_best_height")
        val topStreak = intPreferencesKey("ts_best_streak")
        val roundsPlayed = intPreferencesKey("ts_rounds_played")

        val skinsOwned = stringSetPreferencesKey("ts_owned_skins")
        val pinnedSkin = intPreferencesKey("ts_selected_skin")
        val backdropsOwned = stringSetPreferencesKey("ts_owned_themes")
        val pinnedBackdrop = intPreferencesKey("ts_selected_theme")

        val riskSlot = intPreferencesKey("ts_difficulty")
        val stake = intPreferencesKey("ts_last_bet")
        val cuesOn = booleanPreferencesKey("ts_sound")
        val trackOn = booleanPreferencesKey("ts_music")
        val buzzOn = booleanPreferencesKey("ts_vibration")
        val trackGain = floatPreferencesKey("ts_music_vol")
        val cueGain = floatPreferencesKey("ts_sfx_vol")

        val streakStamp = stringPreferencesKey("ts_daily_last_claim")
        val streakLength = intPreferencesKey("ts_daily_streak")

        val missionStamp = stringPreferencesKey("ts_missions_date")
        val missionBlob = stringPreferencesKey("ts_missions_data")

        val rivalWeek = intPreferencesKey("ts_lb_week")
        val rivalHeight = intPreferencesKey("ts_lb_week_height")
        val rivalPayout = intPreferencesKey("ts_lb_week_winnings")
        val rivalStreak = intPreferencesKey("ts_lb_week_streak")
    }

    object Defaults {
        const val PURSE = 1000
        const val STAKE = 100
        const val TRACK_GAIN = 0.55f
        const val CUE_GAIN = 0.85f
        const val RIVAL_WEEK = -1
    }

    /** A one-shot read of the whole store, used to hydrate a repository. */
    suspend fun snapshot(): Preferences = store.data.first()

    suspend fun readInt(key: Preferences.Key<Int>, fallback: Int): Int =
        store.data.first()[key] ?: fallback

    suspend fun readBool(key: Preferences.Key<Boolean>, fallback: Boolean): Boolean =
        store.data.first()[key] ?: fallback

    suspend fun readFloat(key: Preferences.Key<Float>, fallback: Float): Float =
        store.data.first()[key] ?: fallback

    suspend fun readString(key: Preferences.Key<String>): String? =
        store.data.first()[key]

    /** Ascending list of ids, stored as a string set (DataStore's list type). */
    suspend fun readIds(key: Preferences.Key<Set<String>>, fallback: List<Int>): List<Int> {
        val raw = store.data.first()[key] ?: return fallback.sorted()
        return raw.mapNotNull { it.toIntOrNull() }.sorted()
    }

    suspend fun writeInt(key: Preferences.Key<Int>, value: Int) {
        store.edit { it[key] = value }
    }

    suspend fun writeBool(key: Preferences.Key<Boolean>, value: Boolean) {
        store.edit { it[key] = value }
    }

    suspend fun writeFloat(key: Preferences.Key<Float>, value: Float) {
        store.edit { it[key] = value }
    }

    suspend fun writeString(key: Preferences.Key<String>, value: String) {
        store.edit { it[key] = value }
    }

    suspend fun writeIds(key: Preferences.Key<Set<String>>, value: List<Int>) {
        store.edit { it[key] = value.map(Int::toString).toSet() }
    }

    /** Lays down the starting balance and defaults on a genuinely fresh install. */
    suspend fun seedFreshInstall() {
        val existing = store.data.first()
        if (existing[Keys.seeded] == true) return
        store.edit { prefs ->
            prefs[Keys.purse] = Defaults.PURSE
            prefs[Keys.skinsOwned] = setOf("1")
            prefs[Keys.pinnedSkin] = 0
            prefs[Keys.backdropsOwned] = setOf("0")
            prefs[Keys.pinnedBackdrop] = 0
            prefs[Keys.riskSlot] = 0
            prefs[Keys.stake] = Defaults.STAKE
            prefs[Keys.cuesOn] = true
            prefs[Keys.trackOn] = true
            prefs[Keys.buzzOn] = true
            prefs[Keys.trackGain] = Defaults.TRACK_GAIN
            prefs[Keys.cueGain] = Defaults.CUE_GAIN
            prefs[Keys.seeded] = true
        }
    }
}
