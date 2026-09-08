package app.focus.android.di

import app.focus.domain.usecase.HardLockExtrasContributor
import app.focus.domain.usecase.HardLockLifecycleController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HardLockModule {

    @Binds
    @Singleton
    abstract fun bindHardLockExtrasContributor(
        impl: AndroidHardLockExtrasContributor,
    ): HardLockExtrasContributor

    @Binds
    @Singleton
    abstract fun bindHardLockLifecycleController(
        impl: AndroidHardLockLifecycleController,
    ): HardLockLifecycleController
}
