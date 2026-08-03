package com.towerbreak.towerbreakgame.di

import com.towerbreak.towerbreakgame.core.di.ApplicationScope
import com.towerbreak.towerbreakgame.core.time.GameClock
import com.towerbreak.towerbreakgame.core.time.SystemGameClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provides the process-wide plumbing. Fingerprint note: this is the Hilt
 * equivalent of the Flutter `ServiceHub.assemble()` composition root, but the
 * wiring is declarative and the graph is validated at compile time.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideGameClock(): GameClock = SystemGameClock()
}
