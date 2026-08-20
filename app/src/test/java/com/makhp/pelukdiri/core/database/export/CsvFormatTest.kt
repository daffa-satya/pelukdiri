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
}
