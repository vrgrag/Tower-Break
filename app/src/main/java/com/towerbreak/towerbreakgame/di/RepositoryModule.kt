package com.towerbreak.towerbreakgame.di

import com.towerbreak.towerbreakgame.data.audio.SoundManager
import com.towerbreak.towerbreakgame.data.repository.CosmeticsRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.DailyMissionRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.DailyRewardRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.Hydratable
import com.towerbreak.towerbreakgame.data.repository.LeaderboardRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.MilestoneRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.ProgressionRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.SettingsRepositoryImpl
import com.towerbreak.towerbreakgame.data.repository.WalletRepositoryImpl
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import com.towerbreak.towerbreakgame.domain.repository.DailyRewardRepository
import com.towerbreak.towerbreakgame.domain.repository.LeaderboardRepository
import com.towerbreak.towerbreakgame.domain.repository.MilestoneRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.SettingsRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Binds each domain interface to its DataStore-backed implementation, and also
 * collects every store into a `Set<Hydratable>` (Dagger multibinding) so the
 * splash can warm them all in one loop.
 *
 * Because every `*Impl` is `@Singleton`, both the interface binding and the
 * `@IntoSet` binding resolve to the *same* instance — the interface consumers
 * and the hydration loop share state.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindWallet(impl: WalletRepositoryImpl): WalletRepository
    @Binds abstract fun bindProgression(impl: ProgressionRepositoryImpl): ProgressionRepository
    @Binds abstract fun bindMilestone(impl: MilestoneRepositoryImpl): MilestoneRepository
    @Binds abstract fun bindCosmetics(impl: CosmeticsRepositoryImpl): CosmeticsRepository
    @Binds abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun bindMissions(impl: DailyMissionRepositoryImpl): DailyMissionRepository
    @Binds abstract fun bindLeaderboard(impl: LeaderboardRepositoryImpl): LeaderboardRepository
    @Binds abstract fun bindDailyReward(impl: DailyRewardRepositoryImpl): DailyRewardRepository

    @Binds abstract fun bindAudio(impl: SoundManager): GameAudio

    @Binds @IntoSet abstract fun walletHydratable(impl: WalletRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun progressionHydratable(impl: ProgressionRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun milestoneHydratable(impl: MilestoneRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun cosmeticsHydratable(impl: CosmeticsRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun settingsHydratable(impl: SettingsRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun missionsHydratable(impl: DailyMissionRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun leaderboardHydratable(impl: LeaderboardRepositoryImpl): Hydratable
    @Binds @IntoSet abstract fun dailyRewardHydratable(impl: DailyRewardRepositoryImpl): Hydratable
}
