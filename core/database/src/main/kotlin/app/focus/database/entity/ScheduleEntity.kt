package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "profileId") val profileId: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
    @ColumnInfo(name = "daysOfWeekMask") val daysOfWeekMask: Int,
    @ColumnInfo(name = "startMinuteOfDay") val startMinuteOfDay: Int,
    @ColumnInfo(name = "endMinuteOfDay") val endMinuteOfDay: Int,
    @ColumnInfo(name = "allowSkipDay") val allowSkipDay: Boolean,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "createdAt") val createdAt: Long
)
