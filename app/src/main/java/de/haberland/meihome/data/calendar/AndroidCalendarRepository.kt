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
import java.time.LocalDate
import java.time.ZoneId
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

                    // Keep events that are still active today (for example a week-long
                    // vacation), but drop events that ended at or before today's start.
                    // A one-day all-day event from yesterday typically ends exactly at
                    // today's 00:00 and is therefore excluded.
                    if (eventEnd <= start) continue

                    add(
                        CalendarEvent(
                            id = cursor.getLong(idIndex),
                            title = cursor.getString(titleIndex).orEmpty().ifBlank { "(Ohne Titel)" },
                            startMillis = begin,
                            endMillis = eventEnd,
                            allDay = cursor.getInt(allDayIndex) != 0,
                            calendarName = cursor.getString(calendarIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
