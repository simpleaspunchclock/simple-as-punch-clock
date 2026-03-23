package com.simpleas.punchclock

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val displayFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    private val shortFormat = SimpleDateFormat("MMM d h:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDisplay(time: Long): String = displayFormat.format(Date(time))
    fun formatShort(time: Long): String = shortFormat.format(Date(time))
    fun formatDay(time: Long): String = dayFormat.format(Date(time))

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%d:%02d", hours, minutes)
    }

    fun startOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return cal.timeInMillis
    }

    fun now(): Long = System.currentTimeMillis()
}
