package app.focus.android.di

import android.content.Context
import app.focus.datastore.ProtoActiveSessionSnapshotStorage
import app.focus.datastore.ProtoUserSettingsRepository
import app.focus.datastore.UserSettingsRepository
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideActiveSessionSnapshotStorage(
        @ApplicationContext context: Context,
    ): ActiveSessionSnapshotStorage = ProtoActiveSessionSnapshotStorage.create(context)

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        @ApplicationContext context: Context,
    ): UserSettingsRepository = ProtoUserSettingsRepository.create(context)
}
