package app.focus.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String?,
    @ColumnInfo(name = "colorArgb") val colorArgb: Int,
    @ColumnInfo(name = "lockMode") val lockMode: String,
    @ColumnInfo(name = "defaultDurationMinutes") val defaultDurationMinutes: Int,
    @ColumnInfo(name = "bypassDelaySeconds") val bypassDelaySeconds: Int,
    @ColumnInfo(name = "bypassBreathingEnabled") val bypassBreathingEnabled: Boolean,
    @ColumnInfo(name = "bypassReasonRequired") val bypassReasonRequired: Boolean,
    @ColumnInfo(name = "bypassPhrase") val bypassPhrase: String?,
    @ColumnInfo(name = "bypassLimitPerSession") val bypassLimitPerSession: Int,
    @ColumnInfo(name = "accessWindowMinutes") val accessWindowMinutes: Int,
    @ColumnInfo(name = "bypassAppliesToAllApps") val bypassAppliesToAllApps: Boolean,
    @ColumnInfo(name = "emergencyExit") val emergencyExit: String,
    @ColumnInfo(name = "blockNewApps") val blockNewApps: Boolean,
    @ColumnInfo(name = "deviceAdminProtection") val deviceAdminProtection: Boolean,
    @ColumnInfo(name = "allowedShortcuts") val allowedShortcuts: String?,
    @ColumnInfo(name = "hideTargetNotifications") val hideTargetNotifications: Boolean,
    @ColumnInfo(name = "targetPackageNames") val targetPackageNames: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
    @ColumnInfo(name = "sortOrder") val sortOrder: Int = 0
)
