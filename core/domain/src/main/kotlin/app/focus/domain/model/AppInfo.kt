package app.focus.domain.model

typealias ByteArrayType = ByteArray

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconBytes: ByteArrayType?,
    val isSystemApp: Boolean,
    val usageMinutesLast7Days: Long = 0L,
    val isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        return packageName == (other as? AppInfo)?.packageName
    }

    override fun hashCode(): Int = packageName.hashCode()
}
