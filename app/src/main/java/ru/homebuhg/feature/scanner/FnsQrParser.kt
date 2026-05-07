package ru.homebuhg.feature.scanner

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FnsQrParser {

    data class FnsReceipt(
        val amountMinor: Long,
        val dateMs: Long,
        val fn: String,
        val fd: String,
        val fp: String,
        val receiptType: Int
    )

    fun parse(rawValue: String): FnsReceipt? {
        val query = when {
            rawValue.contains("?") -> rawValue.substringAfter("?")
            rawValue.contains("=") -> rawValue
            else -> return null
        }

        val params = query.split("&")
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx < 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
            }
            .toMap()

        val s = params["s"] ?: return null
        val t = params["t"] ?: return null
        val amountMinor = s.replace(",", ".")
            .toDoubleOrNull()
            ?.times(100)
            ?.toLong() ?: return null

        return FnsReceipt(
            amountMinor = amountMinor,
            dateMs = parseFnsDate(t),
            fn = params["fn"] ?: "",
            fd = params["i"] ?: "",
            fp = params["fp"] ?: "",
            receiptType = params["n"]?.toIntOrNull() ?: 1
        )
    }

    private val FMT_LONG = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val FMT_SHORT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")

    private fun parseFnsDate(t: String): Long {
        val clean = t.replace(":", "")
        return runCatching {
            val formatter = if (clean.length >= 15) FMT_LONG else FMT_SHORT
            val str = clean.take(if (clean.length >= 15) 15 else 13)
            LocalDateTime.parse(str, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }
}
