package com.towerbreak.towerbreakgame.core.text

import java.util.Locale

/**
 * Renders [value] with a thin space every three digits ("12 500"), the way every
 * coin balance, stake and payout is displayed.
 *
 * Fingerprint note: the Dart version hand-rolled a StringBuffer loop. Here it is
 * an [Int] extension so call sites read `balance.grouped()`.
 */
fun Int.grouped(): String {
    val digits = toString()
    val out = StringBuilder(digits.length + digits.length / 3)
    for (i in digits.indices) {
        if (i > 0 && (digits.length - i) % 3 == 0) out.append(' ')
        out.append(digits[i])
    }
    return out.toString()
}

/** A multiplier rendered as "x1.29" — always two decimals, US grouping. */
fun Double.asMultiplier(): String = "x" + String.format(Locale.US, "%.2f", this)
