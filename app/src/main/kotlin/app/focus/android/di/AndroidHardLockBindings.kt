package app.focus.android.di

import android.content.Context
import app.focus.domain.usecase.HardLockExtras
import app.focus.domain.usecase.HardLockExtrasContributor
import app.focus.domain.usecase.HardLockLifecycleController
import app.focus.system.HardLockEnforcer
import app.focus.system.HardLockExtrasResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidHardLockExtrasContributor @Inject constructor(
    @ApplicationContext context: Context,
) : HardLockExtrasContributor {

    private val resolver = HardLockExtrasResolver(context)

    override fun resolveExtras(): HardLockExtras {
        val result = resolver.resolve()
        return HardLockExtras(
            extraPackages = result.extraPackages,
            defaultLauncherPkg = result.defaultLauncherPkg,
        )
    }
}

@Singleton
class AndroidHardLockLifecycleController @Inject constructor(
    @ApplicationContext private val context: Context,
) : HardLockLifecycleController {

    private val enforcer = HardLockEnforcer(context)

    override fun onHardLockSessionStarted(deviceAdminProtection: Boolean) = Unit

    override fun onHardLockSessionStopped(deviceAdminProtection: Boolean) {
        enforcer.unregisterHardLock()
    }
}
