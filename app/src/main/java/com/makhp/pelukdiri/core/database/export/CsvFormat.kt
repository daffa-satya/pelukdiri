package com.makhp.pelukdiri.core.database.export

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object CsvFormat {
    private val timestampFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        .withZone(ZoneOffset.UTC)

    fun field(value: Any?): String {
        if (value == null) return ""
        val text = value.toString()
        return if (value is String || text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${text.replace("\"", "\"\"")}\""
        } else text
    }

    fun row(vararg values: Any?): String = values.joinToString(",", postfix = "\r\n", transform = ::field)
    fun timestamp(epochMillis: Long): String = timestampFormatter.format(Instant.ofEpochMilli(epochMillis))
}
