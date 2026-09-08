package app.focus.domain.usecase

/** Resolves extra blocked packages and launcher metadata for hard lock sessions. */
interface HardLockExtrasContributor {
    fun resolveExtras(): HardLockExtras
}

data class HardLockExtras(
    val extraPackages: List<String>,
    val defaultLauncherPkg: String?,
)

/** Lifecycle hooks for hard lock protection (device admin, restrictions). */
interface HardLockLifecycleController {
    fun onHardLockSessionStarted(deviceAdminProtection: Boolean)
    fun onHardLockSessionStopped(deviceAdminProtection: Boolean)
}
