package com.makhp.pelukdiri.core.database.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvFormatTest {
    @Test fun `escapes quote comma and newline using rfc 4180`() {
        assertEquals("\"a,b \"\"quoted\"\"\nnext\"", CsvFormat.field("a,b \"quoted\"\nnext"))
    }

    @Test fun `row uses crlf and preserves empty null`() {
        assertEquals("\"name\",,7\r\n", CsvFormat.row("name", null, 7))
    }

    @Test fun `timestamp is deterministic utc`() {
        assertEquals("1970-01-01T00:00:00.000Z", CsvFormat.timestamp(0L))
    }

    @Test fun `quoted comma newline and carriage return round trip`() {
        val expected = listOf("plain", "a,b", "say \"hello\"", "line 1\nline 2", "left\r\nright")

        assertEquals(expected, parseSingleRfc4180Row(CsvFormat.row(*expected.toTypedArray())))
    }

    private fun parseSingleRfc4180Row(encoded: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < encoded.length) {
            val char = encoded[index]
            when {
                char == '"' && quoted && index + 1 < encoded.length && encoded[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    fields += field.toString()
                    field.clear()
                }
                char == '\r' && !quoted && index + 1 < encoded.length && encoded[index + 1] == '\n' -> {
                    fields += field.toString()
                    return fields
                }
                else -> field.append(char)
            }
            index++
        }
        error("CSV row did not end with CRLF")
    }
}
