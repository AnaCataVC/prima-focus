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
}
