package com.toolbox.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {
    fun currentTimestamp(): Long = System.currentTimeMillis()
    
    fun formatTimestamp(millis: Long, pattern: String, zone: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of(zone))
        return ldt.format(formatter)
    }
    
    fun parseToTimestamp(dateTime: String, pattern: String, zone: String): Long {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val ldt = LocalDateTime.parse(dateTime, formatter)
        return ldt.atZone(ZoneId.of(zone)).toInstant().toEpochMilli()
    }
    
    fun formatNow(pattern: String): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))
    }
}