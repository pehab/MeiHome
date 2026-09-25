package de.haberland.meihome.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String?,
)

class AndroidCalendarRepository(
    private val context: Context,
) {
    fun observeUpcomingEvents(): Flow<List<CalendarEvent>> = callbackFlow {
        fun publish() {
            if (!hasPermission()) {
                trySend(emptyList())
                return
            }
            trySend(loadUpcomingEvents())
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                publish()
            }
        }

        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            observer,
        )
        context.contentResolver.registerContentObserver(
            CalendarContract.Calendars.CONTENT_URI,
            true,
            observer,
        )

        publish()
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    private fun loadUpcomingEvents(): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also { builder ->
            ContentUris.appendId(builder, start)
            ContentUris.appendId(builder, end)
        }.build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

        return context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            CalendarContract.Instances.BEGIN + " ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val calendarIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

            buildList {
                while (cursor.moveToNext()) {
                    val begin = cursor.getLong(beginIndex)
                    val eventEnd = cursor.getLong(endIndex)
                    val allDay = cursor.getInt(allDayIndex) != 0

                    val isStillRelevant = if (allDay) {
                        // Android stores all-day event boundaries as UTC midnights.
                        // Comparing those raw millis to local midnight can make a
                        // yesterday-only event appear to extend into today in positive
                        // time zones. Compare calendar dates in UTC instead.
                        val endDateExclusive = Instant.ofEpochMilli(eventEnd)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        endDateExclusive > LocalDate.now(zone)
                    } else {
                        // Timed events that started before today remain visible only
                        // while they actually overlap today.
                        eventEnd > start
                    }

                    if (!isStillRelevant) continue

                    add(
                        CalendarEvent(
                            id = cursor.getLong(idIndex),
                            title = cursor.getString(titleIndex).orEmpty().ifBlank { "(Ohne Titel)" },
                            startMillis = begin,
                            endMillis = eventEnd,
                            allDay = allDay,
                            calendarName = cursor.getString(calendarIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
