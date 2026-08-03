package com.towerbreak.towerbreakgame.presentation.navigation

/**
 * Type-safe-ish route table. Fingerprint note: the Flutter build pushed
 * `MaterialPageRoute`/`PageRouteBuilder`s imperatively from inside widgets. Here
 * routes are centralised constants consumed by a single Navigation-Compose graph.
 */
object Routes {
    const val SPLASH = "splash"
    const val HUB = "hub"
    const val GAME = "game"
    const val SHOP = "shop"
    const val MISSIONS = "missions"
    const val RANKS = "ranks"

    private const val WEB_BASE = "web"
    const val WEB = "$WEB_BASE/{page}"
    const val ARG_PAGE = "page"

    fun web(page: String): String = "$WEB_BASE/$page"
}
