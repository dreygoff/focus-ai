package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey @ColumnInfo(name = "dateEpochDay") val dateEpochDay: Long,
    @ColumnInfo(name = "focusMinutes") val focusMinutes: Int,
    @ColumnInfo(name = "sessionsCompleted") val sessionsCompleted: Int,
    @ColumnInfo(name = "sessionsTotal") val sessionsTotal: Int,
    @ColumnInfo(name = "blockAttempts") val blockAttempts: Int,
    @ColumnInfo(name = "bypasses") val bypasses: Int
)
