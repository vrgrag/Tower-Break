package com.towerbreak.towerbreakgame.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.towerbreak.towerbreakgame.domain.model.ExternalPage
import com.towerbreak.towerbreakgame.presentation.game.GameScreen
import com.towerbreak.towerbreakgame.presentation.hub.HubScreen
import com.towerbreak.towerbreakgame.presentation.missions.MissionsScreen
import com.towerbreak.towerbreakgame.presentation.ranks.RanksScreen
import com.towerbreak.towerbreakgame.presentation.shop.ShopScreen
import com.towerbreak.towerbreakgame.presentation.splash.SplashScreen
import com.towerbreak.towerbreakgame.presentation.webview.WebPageScreen

/**
 * The single navigation graph. Cross-fades between destinations mirror the
 * original `FadeTransition` splash→hub hand-off.
 *
 * [chainedBoot] is true when the launcher already showed a loading screen before
 * handing the game control; the splash then runs its warm-up without replaying
 * the progress bar.
 */
@Composable
fun TowerNavHost(
    navController: NavHostController = rememberNavController(),
    chainedBoot: Boolean = false,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(400)) },
        exitTransition = { fadeOut(tween(400)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onReady = {
                    navController.navigate(Routes.HUB) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                chained = chainedBoot,
            )
        }

        composable(Routes.HUB) {
            HubScreen(
                onPlay = { navController.navigate(Routes.GAME) },
                onOpenPage = { page -> navController.navigate(Routes.web(page.name)) },
                onOpenShop = { navController.navigate(Routes.SHOP) },
                onOpenMissions = { navController.navigate(Routes.MISSIONS) },
                onOpenRanks = { navController.navigate(Routes.RANKS) },
            )
        }

        composable(Routes.GAME) {
            GameScreen(onLeave = { navController.popBackStack() })
        }

        composable(Routes.SHOP) {
            ShopScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MISSIONS) {
            MissionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RANKS) {
            RanksScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.WEB,
            arguments = listOf(navArgument(Routes.ARG_PAGE) { type = NavType.StringType }),
        ) { entry ->
            val page = entry.arguments?.getString(Routes.ARG_PAGE)
                ?.let { runCatching { ExternalPage.valueOf(it) }.getOrNull() }
                ?: ExternalPage.PRIVACY
            WebPageScreen(page = page, onBack = { navController.popBackStack() })
        }
    }
}
