package app.focus.service.focus

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusServiceEntryPoint {
    fun dependencies(): FocusServiceDependencies
}
