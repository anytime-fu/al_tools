package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeUtilsTest {
    
    @Test
    fun formatTimestampInUtc() {
        val formatted = TimeUtils.formatTimestamp(0L, "yyyy-MM-dd HH:mm:ss", "UTC")
        assertEquals("1970-01-01 00:00:00", formatted)
    }
    
    @Test
    fun parseToTimestampInUtc() {
        val ts = TimeUtils.parseToTimestamp("1970-01-01 00:00:01", "yyyy-MM-dd HH:mm:ss", "UTC")
        assertEquals(1000L, ts)
    }
    
    @Test
    fun roundTripKeepsValue() {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val original = 1700000000000L
        val text = TimeUtils.formatTimestamp(original, pattern, "UTC")
        assertEquals(original, TimeUtils.parseToTimestamp(text, pattern, "UTC"))
    }
}