package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "access_windows")
data class AccessWindowEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sessionId") val sessionId: String,
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "grantedAt") val grantedAt: Long,
    @ColumnInfo(name = "expiresAt") val expiresAt: Long,
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "restrictedToActivity") val restrictedToActivity: String?
)
