package ru.homebuhg.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FnsQrParserTest {

    @Test
    fun `parse full url`() {
        val raw = "https://proverkacheka.com/check?t=20240115T1430&s=1234.56&fn=9999078900012345&i=12345&fp=1234567890&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assertEquals(123456L, result!!.amountMinor)
        assertEquals("9999078900012345", result.fn)
        assertEquals("12345", result.fd)
        assertEquals("1234567890", result.fp)
        assertEquals(1, result.receiptType)
    }

    @Test
    fun `parse raw query string without leading url`() {
        val raw = "t=20240115T143000&s=500.00&fn=1111111111111111&i=99&fp=999999999&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assertEquals(50000L, result!!.amountMinor)
    }

    @Test
    fun `parse amount with comma decimal separator`() {
        val raw = "t=20240115T1430&s=250,99&fn=1&i=1&fp=1&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assertEquals(25099L, result!!.amountMinor)
    }

    @Test
    fun `return null when amount is missing`() {
        val raw = "t=20240115T1430&fn=1&i=1&fp=1&n=1"
        assertNull(FnsQrParser.parse(raw))
    }

    @Test
    fun `return null when string has no params`() {
        assertNull(FnsQrParser.parse("random text"))
        assertNull(FnsQrParser.parse(""))
    }

    @Test
    fun `parse long date format HHmmss`() {
        val raw = "t=20240315T153045&s=100.00&fn=1&i=1&fp=1&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        // Just verify we get a valid timestamp, not epoch default
        assert(result!!.dateMs > 0L)
    }

    @Test
    fun `parse short date format HHmm`() {
        val raw = "t=20240315T1530&s=100.00&fn=1&i=1&fp=1&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assert(result!!.dateMs > 0L)
    }

    @Test
    fun `parse date with colons`() {
        val raw = "t=20240315T15:30:45&s=100.00&fn=1&i=1&fp=1&n=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assert(result!!.dateMs > 0L)
    }

    @Test
    fun `receipt type defaults to 1 when missing`() {
        val raw = "t=20240115T1430&s=100.00&fn=1&i=1&fp=1"
        val result = FnsQrParser.parse(raw)
        assertNotNull(result)
        assertEquals(1, result!!.receiptType)
    }

    @Test
    fun `fn fd fp default to empty when missing`() {
        val raw = "t=20240115T1430&s=100.00"
        val result = FnsQrParser.parse(raw)
        // s and t are present, but fn/i/fp missing → still parses
        assertNotNull(result)
        assertEquals("", result!!.fn)
        assertEquals("", result.fd)
        assertEquals("", result.fp)
    }
}
