package app.focus.service.focus

import app.focus.domain.internal.schedule.ScheduleAlarmPlanner
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.CheckMissedSchedulesUseCase
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.PlanSchedulesUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ScheduleRepository
import app.focus.domain.usecase.ScheduleSkipRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduleEntryPoint {
    fun planSchedulesUseCase(): PlanSchedulesUseCase
    fun checkMissedSchedulesUseCase(): CheckMissedSchedulesUseCase
    fun startScheduledSessionUseCase(): app.focus.domain.usecase.StartScheduledSessionUseCase
    fun scheduleSkipRepository(): ScheduleSkipRepository
    fun scheduleRepository(): ScheduleRepository
    fun profileRepository(): ProfileRepository
    fun scheduleAlarmPlanner(): ScheduleAlarmPlanner
    fun alarmScheduler(): AlarmSchedulerService
    fun clock(): Clock
    fun advancePomodoroPhaseUseCase(): app.focus.domain.usecase.AdvancePomodoroPhaseUseCase
}
