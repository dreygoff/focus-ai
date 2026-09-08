package app.focus.service.focus

import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.BlockLauncher
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.DecideBlockUseCase
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StopSessionUseCase
import app.focus.system.DetectorOrchestrator
import app.focus.system.UsageStatsPollingDetector

class FocusServiceDependencies(
    val stopSessionUseCase: StopSessionUseCase,
    val sessionRepository: SessionRepository,
    val snapshotStore: ActiveSessionSnapshotStorage,
    val alarmScheduler: AlarmSchedulerService,
    val clock: Clock,
    val detectorOrchestrator: DetectorOrchestrator,
    val usageStatsPollingDetector: UsageStatsPollingDetector,
    val decideBlockUseCase: DecideBlockUseCase,
    val blockLauncher: BlockLauncher,
    val eventLogRepository: EventLogRepository,
    val activeSessionBlockState: ActiveSessionBlockState,
)
