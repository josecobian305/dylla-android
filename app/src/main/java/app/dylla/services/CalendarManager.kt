package app.dylla.services

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

object CalendarManager {

    fun addCallback(
        context: Context,
        title: String,
        notes: String,
        phone: String,
        date: Long,
        stage: String
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val calendarId = getFirstCalendarId(context) ?: return false

        val description = buildString {
            if (phone.isNotBlank()) append("Phone: $phone\n")
            if (stage.isNotBlank()) append("Stage: $stage\n")
            if (notes.isNotBlank()) append("\n$notes")
        }

        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, date)
            put(CalendarContract.Events.DTEND, date + 15 * 60 * 1000)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val eventUri = try {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
        } catch (e: Exception) {
            null
        } ?: return false

        val eventId = eventUri.lastPathSegment?.toLongOrNull() ?: return true

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 5)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }

        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        } catch (_: Exception) {}

        return true
    }

    private fun getFirstCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        val cursor = try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null
            )
        } catch (e: Exception) {
            null
        } ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return null
    }
}
