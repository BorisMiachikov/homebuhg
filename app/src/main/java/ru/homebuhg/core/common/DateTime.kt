package ru.homebuhg.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId = ZoneId.systemDefault()

fun nowMillis(): Long = System.currentTimeMillis()

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

fun LocalDateTime.toEpochMillis(): Long =
    atZone(zone).toInstant().toEpochMilli()

fun LocalDate.startOfDayMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(): Long = atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

private val ruDayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru", "RU"))
private val ruShortFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru", "RU"))
private val ruTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ru", "RU"))

fun Long.formatRuDay(): String = toLocalDateTime().toLocalDate().format(ruDayFormatter)
fun Long.formatRuShort(): String = toLocalDateTime().toLocalDate().format(ruShortFormatter)
fun Long.formatRuTime(): String = toLocalDateTime().toLocalTime().format(ruTimeFormatter)
fun LocalDate.formatRuDay(): String = format(ruDayFormatter)
