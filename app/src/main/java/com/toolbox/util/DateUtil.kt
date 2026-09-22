package com.toolbox.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {

    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年MM月", Locale.getDefault())

    fun formatTimestamp(timestamp: Long): String {
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return dateTime.format(displayFormatter)
    }

    fun formatDate(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(dateFormatter)
    }

    fun formatMonth(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.format(monthFormatter)
    }

    fun getStartOfDay(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfDay(date: LocalDate): Long {
        return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getStartOfMonth(month: YearMonth): Long {
        return month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfMonth(month: YearMonth): Long {
        return month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun LocalDate.toEpochMilli(): Long {
        return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun getAgeFromBirthDate(birthDate: LocalDate): Int {
        val today = LocalDate.now()
        var age = today.year - birthDate.year
        if (today.dayOfYear < birthDate.dayOfYear) {
            age--
        }
        return age
    }
}
