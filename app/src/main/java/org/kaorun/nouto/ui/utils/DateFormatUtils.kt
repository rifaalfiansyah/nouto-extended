package org.kaorun.nouto.ui.utils

import android.content.Context
import android.text.format.DateFormat
import org.kaorun.nouto.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatUtils {
    private val timeFormatter24h = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val timeFormatter12h = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val sameYearFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val otherYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

    fun formatNoteTime(context: Context, timestamp: Long): String {
        val zoneId = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId)
        val noteDate = dateTime.toLocalDate()
        val today = LocalDate.now(zoneId)
        val timeFormatter = if (DateFormat.is24HourFormat(context)) timeFormatter24h else timeFormatter12h

        return when {
            noteDate == today ->
                "${context.getString(R.string.today)}, ${dateTime.format(timeFormatter)}"
            noteDate == today.minusDays(1) ->
                "${context.getString(R.string.yesterday)}, ${dateTime.format(timeFormatter)}"
            noteDate.year == today.year ->
                dateTime.format(sameYearFormatter)
            else ->
                dateTime.format(otherYearFormatter)
        }
    }
}
