package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "allowlist")
data class AllowlistEntity(
    @PrimaryKey @ColumnInfo(name = "packageName") val packageName: String
)
