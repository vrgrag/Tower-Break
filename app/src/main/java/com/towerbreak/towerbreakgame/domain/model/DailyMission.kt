package com.towerbreak.towerbreakgame.domain.model

/**
 * The shapes a daily mission can take (the Flutter `QuestKind`). The ordinal is
 * the persisted code, so declaration order stays frozen.
 */
enum class MissionKind {
    /** Storeys seated successfully today. */
    STOREYS_SEATED,

    /** Storeys climbed today — same event as [STOREYS_SEATED], bigger targets. */
    STOREYS_CLIMBED,

    /** Rounds cashed out back to back without a bust. */
    CASH_OUT_RUN,

    /** Highest storey reached inside a single round. */
    REACH_STOREY;

    /** True for the kinds that tick once per seated storey. */
    val tracksSeating: Boolean
        get() = this == STOREYS_SEATED || this == STOREYS_CLIMBED

    fun headline(target: Int): String = when (this) {
        STOREYS_SEATED -> "Place $target floors"
        STOREYS_CLIMBED -> "Climb $target floors today"
        CASH_OUT_RUN -> "Cash out $target rounds in a row"
        REACH_STOREY -> "Reach floor $target in one round"
    }
}

/**
 * One mission on today's board (the Flutter `Quest`).
 *
 * Fingerprint note: immutable `data class` with `copy`-based progress instead of
 * Dart's mutable fields, which fits the unidirectional-data-flow the repository
 * uses to publish board updates.
 */
data class DailyMission(
    val kind: MissionKind,
    val target: Int,
    val reward: Int,
    val tally: Int = 0,
    val claimed: Boolean = false,
) {
    val done: Boolean get() = tally >= target
    val headline: String get() = kind.headline(target)
    val fraction: Float get() = (tally.toFloat() / target).coerceIn(0f, 1f)
}
