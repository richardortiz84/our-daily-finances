package com.daytimegaming.ourdailyfinances.presentation.util

import kotlin.math.roundToLong

fun Double.formatAmount(): String {
    val cents = (this * 100).roundToLong()
    val isNegative = cents < 0
    val absCents = kotlin.math.abs(cents)
    val wholePart = absCents / 100
    val decimalPart = absCents % 100
    
    val wholePartStr = formatWholePart(wholePart)
    val formatted = "$wholePartStr.${decimalPart.toString().padStart(2, '0')}"
    return if (isNegative) "-$formatted" else formatted
}

private fun formatWholePart(value: Long): String {
    val valueStr = value.toString()
    val result = StringBuilder()
    var count = 0
    for (i in valueStr.length - 1 downTo 0) {
        if (count > 0 && count % 3 == 0) {
            result.append(',')
        }
        result.append(valueStr[i])
        count++
    }
    return result.reverse().toString()
}

fun String?.toCurrencySymbol(): String {
    return when (this?.uppercase()) {
        "USD", "CAD", "AUD", "MXN", "NZD", "SGD", "HKD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY", "CNY" -> "¥"
        "INR" -> "₹"
        "RUB" -> "₽"
        "KRW" -> "₩"
        "TRY" -> "₺"
        "BRL" -> "R$"
        "CHF" -> "Fr"
        else -> "$"
    }
}
