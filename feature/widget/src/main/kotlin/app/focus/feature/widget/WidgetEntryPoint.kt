package app.focus.feature.widget

import android.content.Context
import app.focus.domain.usecase.GetWidgetSessionStateUseCase
import app.focus.domain.usecase.MandatoryPermissionsGateway
import app.focus.domain.usecase.QuickStartLastProfileUseCase
import app.focus.domain.usecase.QuickStartProfileUseCase
import app.focus.domain.usecase.QuickStopSessionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getWidgetSessionStateUseCase(): GetWidgetSessionStateUseCase
    fun quickStartLastProfileUseCase(): QuickStartLastProfileUseCase
    fun quickStartProfileUseCase(): QuickStartProfileUseCase
    fun quickStopSessionUseCase(): QuickStopSessionUseCase
    fun mandatoryPermissionsGateway(): MandatoryPermissionsGateway
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    dagger.hilt.android.EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    )
