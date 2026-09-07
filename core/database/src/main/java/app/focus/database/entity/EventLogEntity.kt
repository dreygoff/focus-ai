package app.focus.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "event_log", indexes = [Index(value = ["sessionId", "timestamp"])])
data class EventLogEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "sessionId") val sessionId: String?,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "packageName") val packageName: String?,
    @ColumnInfo(name = "payload") val payload: String?
)
