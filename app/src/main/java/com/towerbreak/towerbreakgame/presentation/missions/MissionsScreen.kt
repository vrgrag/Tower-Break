package com.towerbreak.towerbreakgame.presentation.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.DailyMission
import com.towerbreak.towerbreakgame.domain.model.MissionKind
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/** The daily mission board — three fresh objectives every calendar day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    onBack: () -> Unit,
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)

    val board by viewModel.board.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); viewModel.dismissToast() }
    }

    Scaffold(
        containerColor = BrandPalette.RibbonDeep,
        topBar = {
            TopAppBar(
                title = { Text("MISSIONS", style = GameType.label(size = 18)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BrandPalette.Parchment)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPalette.Ribbon),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        if (board.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                Text(
                    "New missions are on their way.",
                    style = GameType.prose(size = 14, tint = BrandPalette.ParchmentFaded),
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Resets at midnight — finish all three for the full haul.",
                    style = GameType.prose(size = 13, tint = BrandPalette.ParchmentFaded),
                )
                Spacer(Modifier.height(4.dp))
            }
            items(board) { mission ->
                MissionCard(mission = mission, onClaim = { viewModel.claim(mission) })
            }
        }
    }
}

private fun iconFor(kind: MissionKind): ImageVector = when (kind) {
    MissionKind.STOREYS_SEATED -> Icons.Rounded.Apartment
    MissionKind.STOREYS_CLIMBED -> Icons.Rounded.Stairs
    MissionKind.CASH_OUT_RUN -> Icons.Rounded.LocalFireDepartment
    MissionKind.REACH_STOREY -> Icons.Rounded.Apartment
}

@Composable
private fun MissionCard(mission: DailyMission, onClaim: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPalette.Panel)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (mission.claimed) BrandPalette.Moss.copy(alpha = 0.22f) else BrandPalette.Azure.copy(alpha = 0.22f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (mission.claimed) Icons.Rounded.CheckCircle else iconFor(mission.kind),
                contentDescription = null,
                tint = if (mission.claimed) BrandPalette.Moss else BrandPalette.AzurePale,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(mission.headline, style = GameType.label(size = 14, tint = BrandPalette.White))
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BrandPalette.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(mission.fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (mission.claimed) BrandPalette.Moss else BrandPalette.Bullion),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${mission.tally.coerceAtMost(mission.target)}/${mission.target}",
                style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded),
            )
        }

        Spacer(Modifier.width(10.dp))

        when {
            mission.claimed -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Claimed", tint = BrandPalette.Moss)
            mission.done -> ClaimButton(reward = mission.reward, onClaim = onClaim)
            else -> Text(
                "+${mission.reward}",
                style = GameType.tally(size = 13, tint = BrandPalette.ParchmentFaded),
            )
        }
    }
}

@Composable
private fun ClaimButton(reward: Int, onClaim: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BrandPalette.Bullion)
            .clickable(onClick = onClaim)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Redeem, contentDescription = null, tint = BrandPalette.RibbonDeep, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("+$reward", style = GameType.tally(size = 13, tint = BrandPalette.RibbonDeep))
    }
}
