package app.focus.feature.blocker

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.GrantBypassUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.SessionRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockerEntryPoint {
    fun grantBypassUseCase(): GrantBypassUseCase
    fun profileRepository(): ProfileRepository
    fun sessionRepository(): SessionRepository
    fun eventLogRepository(): EventLogRepository
}
