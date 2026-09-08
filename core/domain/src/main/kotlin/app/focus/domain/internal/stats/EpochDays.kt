package app.focus.domain.internal.stats

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object EpochDays {
    fun fromMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

    fun dayStartMillis(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun dayEndMillis(epochDay: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        dayStartMillis(epochDay + 1, zoneId)
}
