package com.daytimegaming.ourdailyfinances.presentation.util

import kotlin.math.abs
import kotlin.math.roundToLong

fun Double.formatAmount(): String {
    val cents = (this * 100).roundToLong()
    val wholePart = cents / 100
    val decimalPart = abs(cents % 100)
    return "$wholePart.${decimalPart.toString().padStart(2, '0')}"
}
