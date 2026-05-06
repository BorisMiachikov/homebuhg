package ru.homebuhg.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@JvmInline
value class Money(val minor: Long) {
    operator fun plus(other: Money) = Money(minor + other.minor)
    operator fun minus(other: Money) = Money(minor - other.minor)
    operator fun unaryMinus() = Money(-minor)
    val isPositive: Boolean get() = minor > 0
    val isNegative: Boolean get() = minor < 0
    val isZero: Boolean get() = minor == 0L

    companion object {
        val ZERO = Money(0)
    }
}

fun BigDecimal.toMoney(currencyCode: String = "RUB"): Money {
    val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    val scaled = this.setScale(fractionDigits, RoundingMode.HALF_UP)
    val multiplier = BigDecimal.TEN.pow(fractionDigits)
    return Money(scaled.multiply(multiplier).toLong())
}

fun Money.toBigDecimal(currencyCode: String = "RUB"): BigDecimal {
    val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    return BigDecimal(minor).movePointLeft(fractionDigits)
}

fun Money.format(currencyCode: String = "RUB", locale: Locale = Locale("ru", "RU")): String {
    val fmt = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
    }
    return fmt.format(toBigDecimal(currencyCode))
}
