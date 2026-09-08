package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "profileId") val profileId: String,
    @ColumnInfo(name = "profileNameSnapshot") val profileNameSnapshot: String,
    @ColumnInfo(name = "lockMode") val lockMode: String,
    @ColumnInfo(name = "targetPackagesSnapshot") val targetPackagesSnapshot: String,
    @ColumnInfo(name = "goalText") val goalText: String?,
    @ColumnInfo(name = "startedAt") val startedAt: Long,
    @ColumnInfo(name = "plannedEndAt") val plannedEndAt: Long?,
    @ColumnInfo(name = "actualEndAt") val actualEndAt: Long?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "pomodoroConfig") val pomodoroConfig: String?,
    @ColumnInfo(name = "bypassesUsed") val bypassesUsed: Int,
    @ColumnInfo(name = "blockAttempts") val blockAttempts: Int,
    @ColumnInfo(name = "pausesUsed") val pausesUsed: Int,
    @ColumnInfo(name = "scheduleId") val scheduleId: String?
)
