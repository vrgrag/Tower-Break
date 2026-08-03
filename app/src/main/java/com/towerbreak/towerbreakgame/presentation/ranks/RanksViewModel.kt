package com.towerbreak.towerbreakgame.presentation.ranks

import androidx.lifecycle.ViewModel
import com.towerbreak.towerbreakgame.domain.model.MilestoneState
import com.towerbreak.towerbreakgame.domain.model.ProgressionState
import com.towerbreak.towerbreakgame.domain.repository.MilestoneRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Backs the rank ladder + personal-bests screen (the Flutter `RankSheet` view). */
@HiltViewModel
class RanksViewModel @Inject constructor(
    progression: ProgressionRepository,
    milestones: MilestoneRepository,
) : ViewModel() {
    val progression: StateFlow<ProgressionState> = progression.state
    val milestones: StateFlow<MilestoneState> = milestones.state
}
