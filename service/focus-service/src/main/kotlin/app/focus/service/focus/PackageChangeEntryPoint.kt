package app.focus.service.focus

import app.focus.domain.usecase.HandlePackageAddedUseCase
import app.focus.system.PackageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PackageChangeEntryPoint {
    fun packageRepository(): PackageRepository
    fun handlePackageAddedUseCase(): HandlePackageAddedUseCase
}
