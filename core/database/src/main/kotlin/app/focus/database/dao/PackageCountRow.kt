package app.focus.database.dao

import androidx.room.ColumnInfo

data class PackageCountRow(
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "cnt") val count: Int,
)
