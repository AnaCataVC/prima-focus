package com.ancata.prima_focus.utils

import java.util.Calendar

object TimeUtils {

    /**
     * Verifica si la hora actual está dentro de las "Quiet Hours".
     */
    fun isCurrentlyInQuietHours(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val currentTotal = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        val startTotal = parseTimeToMinutes(start)
        val endTotal = parseTimeToMinutes(end)
        
        return if (startTotal < endTotal) {
            currentTotal in startTotal..endTotal
        } else {
            currentTotal >= startTotal || currentTotal <= endTotal
        }
    }

    /**
     * Calcula cuántos minutos faltan para que termine la ventana de "Quiet Hours".
     * Si no estamos en "Quiet Hours", retorna 0.
     */
    fun getMinutesUntilQuietHoursEnd(start: String, end: String): Long {
        if (!isCurrentlyInQuietHours(start, end)) return 0L

        val now = Calendar.getInstance()
        val currentTotal = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val endTotal = parseTimeToMinutes(end)

        return if (currentTotal <= endTotal) {
            (endTotal - currentTotal).toLong()
        } else {
            (1440 - currentTotal + endTotal).toLong()
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        return parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
    }

    /**
     * Formats an epoch timestamp into a human-friendly date/time string (e.g., "Hoy, 14:30", "Ayer, 09:15", "18 Ago, 16:45").
     */
    fun formatEpochToDisplay(epochMillis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val zoneId = java.time.ZoneId.systemDefault()
        val localDateTime = java.time.LocalDateTime.ofInstant(instant, zoneId)
        val today = java.time.LocalDate.now(zoneId)
        val taskDate = localDateTime.toLocalDate()
        val timeStr = localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

        return when {
            taskDate.isEqual(today) -> "Hoy, $timeStr"
            taskDate.isEqual(today.minusDays(1)) -> "Ayer, $timeStr"
            else -> localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
        }
    }

    /**
     * Checks if a scheduled date string represents a date strictly in the future (after today).
     * Returns false if dateStr is null (backlog/no date), "Hoy", or a past/today ISO date.
     */
    fun isFutureScheduled(dateStr: String?, today: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
        if (dateStr == null) return false
        return when (dateStr) {
            "Hoy" -> false
            "Mañana", "El siguiente lunes" -> true
            else -> {
                try {
                    val parsed = java.time.LocalDate.parse(dateStr)
                    parsed.isAfter(today)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}

