package ru.homebuhg.core.domain

import java.time.LocalDate

object RRuleParser {

    fun nextOccurrence(rrule: String, after: LocalDate): LocalDate {
        val params = rrule.parseParams()
        val freq = params["FREQ"] ?: "MONTHLY"
        val interval = params["INTERVAL"]?.toIntOrNull() ?: 1
        val byMonthDay = params["BYMONTHDAY"]?.toIntOrNull()

        return when (freq) {
            "DAILY" -> after.plusDays(interval.toLong())
            "WEEKLY" -> after.plusWeeks(interval.toLong())
            "MONTHLY" -> {
                val next = after.plusMonths(interval.toLong())
                if (byMonthDay != null) {
                    next.withDayOfMonth(minOf(byMonthDay, next.lengthOfMonth()))
                } else next
            }
            "YEARLY" -> after.plusYears(interval.toLong())
            else -> after.plusMonths(1)
        }
    }

    fun buildRRule(freq: String, interval: Int = 1, byMonthDay: Int? = null): String {
        val sb = StringBuilder("FREQ=$freq")
        if (interval != 1) sb.append(";INTERVAL=$interval")
        if (byMonthDay != null && freq == "MONTHLY") sb.append(";BYMONTHDAY=$byMonthDay")
        return sb.toString()
    }

    fun freqLabel(rrule: String): String {
        val params = rrule.parseParams()
        val freq = params["FREQ"] ?: "MONTHLY"
        val interval = params["INTERVAL"]?.toIntOrNull() ?: 1
        val byMonthDay = params["BYMONTHDAY"]?.toIntOrNull()
        return when (freq) {
            "DAILY" -> if (interval == 1) "Каждый день" else "Каждые $interval дн."
            "WEEKLY" -> if (interval == 1) "Каждую неделю" else "Каждые $interval нед."
            "MONTHLY" -> {
                val day = if (byMonthDay != null) " ($byMonthDay-го)" else ""
                if (interval == 1) "Каждый месяц$day" else "Каждые $interval мес.$day"
            }
            "YEARLY" -> if (interval == 1) "Каждый год" else "Каждые $interval лет"
            else -> "Ежемесячно"
        }
    }

    private fun String.parseParams(): Map<String, String> =
        split(";").associate { part ->
            val eq = part.indexOf('=')
            if (eq < 0) part.trim() to "" else part.substring(0, eq).trim() to part.substring(eq + 1).trim()
        }
}
