# Performance notes — Phase 3

## Detector + foreground service (2026-09-09)

- **UsageStats polling:** adaptive interval 300–1000 ms (see `DefaultUsageStatsPollingDetector`).
- **Accessibility path:** `ForegroundEventBus` → `DetectorOrchestrator` when permission granted.
- **FGS:** `START_STICKY`; session end via `AlarmReceiver` + `StopSessionUseCase`.
- **Boot restore:** `BootReceiver` reloads snapshot, reschedules alarm, starts FGS.

Battery profiling on device/emulator is pending manual measurement (Android Studio Energy Profiler).
