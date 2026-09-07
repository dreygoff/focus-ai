package app.focus.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "profile_apps",
    primaryKeys = ["profileId", "packageName"],
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("profileId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class ProfileAppEntity(
    @ColumnInfo(name = "profileId") val profileId: String,
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "addedAt") val addedAt: Long
)
