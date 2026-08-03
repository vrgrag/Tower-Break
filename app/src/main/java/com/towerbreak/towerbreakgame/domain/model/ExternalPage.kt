package com.towerbreak.towerbreakgame.domain.model

/**
 * The store-required web pages, reachable from the hub (the Flutter
 * `ExternalPage`). URLs use the live Tower Break domain.
 */
enum class ExternalPage(val title: String, val url: String) {
    PRIVACY("Privacy Policy", "https://towerbreak.com/privacy-policy.html"),
    SUPPORT("Support", "https://towerbreak.com/support.html"),
}
