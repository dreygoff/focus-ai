package app.focus.domain.usecase

class StopSessionDependencies(
    val sessionRepo: SessionRepository,
    val profileRepo: ProfileRepository,
    val snapshotStore: ActiveSessionSnapshotStorage,
    val alarmScheduler: AlarmSchedulerService,
    val sessionRuntime: SessionRuntimeController,
    val hardLockLifecycle: HardLockLifecycleController,
)
