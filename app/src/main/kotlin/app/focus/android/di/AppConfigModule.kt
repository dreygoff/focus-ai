package app.focus.android.di

import app.focus.android.BuildConfig
import app.focus.domain.di.DebugPomodoroAccelerated
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @DebugPomodoroAccelerated
    fun provideDebugPomodoroAccelerated(): Boolean = BuildConfig.DEBUG
}
