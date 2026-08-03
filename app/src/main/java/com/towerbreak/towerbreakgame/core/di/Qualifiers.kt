package com.towerbreak.towerbreakgame.core.di

import javax.inject.Qualifier

/**
 * Marks the process-lifetime [kotlinx.coroutines.CoroutineScope] used for
 * fire-and-forget persistence writes and the audio store's settings observer.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
