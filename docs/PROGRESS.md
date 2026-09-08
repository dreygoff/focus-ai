# Progress

## Фаза 0 — Инициализация — статус: done — дата: 2026-09-09

### Сделано
- Структура модулей §7.2, convention plugins, CI
- MainActivity + NavHost, FocusTheme M3, Hilt

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅

## Фаза 1 — Домен и данные — статус: done — дата: 2026-09-09

### Сделано
- Proto DataStore: `settings.proto`, `active_session.proto`, device-protected snapshot (TR-05)
- Room exportSchema=true, schema v1 в `core/database/schemas/`
- ProfileSeeder: «Глубокая работа», «Сон», «Детокс» (FR-13)
- StartSessionUseCase резолвит профиль; HomeViewModel через use cases
- DAO-тесты (ProfileDao, SessionDao, ProfileAppDao), ProfileSeederTest
- RealSessionRepository: getStatsDaily, exportStatsCsv

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅

## Фаза 2 — Системный слой, разрешения, профили — статус: done — дата: 2026-09-09

### Сделано
- PermissionChecker (FR-02), PermissionsScreen, onboarding, profiles CRUD + AppPicker

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅

## Фаза 3 — Детектор и foreground service — статус: done — дата: 2026-09-09

### Сделано
- `SessionRuntimeController` — use cases запускают/останавливают FGS
- `FocusForegroundService` (Hilt): ongoing-уведомление, детекторы, завершение сессии по alarm
- Исправлен `AlarmSchedulerImpl`: передаёт реальный `sessionId`
- `DetectorOrchestrator`: форвардинг событий из Accessibility + UsageStats
- `ForegroundEventBusAccessibilityDetector` — мост accessibility → orchestrator
- `BootReceiver`: restore + expire через `StopSessionUseCase`
- Home: кнопка «Старт» вызывает `HomeViewModel.startSession()`

### Известные проблемы
- Замеры батареи — см. `docs/PERF.md` (ручной profiling pending)

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅

## Фаза 4 — Экран блокировки — статус: done — дата: 2026-09-09

### Сделано
- `BlockLauncher` (domain) + `DefaultBlockerLauncher`: BlockActivity с fallback на OverlayBlocker (600 ms)
- `BlockUiState` / `BlockScreen` с ru/en strings; Hard Lock скрывает bypass
- `BlockActivity`: `isInForeground`, intent extras (lock mode, attempt, remaining, sessionId)
- `SessionBlockCoordinator`: DecideBlockUseCase → show block + `BLOCK_SHOWN` / `BLOCK_LATENCY_MS` events
- FGS: detector events → coordinator; snapshot → `ActiveSessionBlockState`; dismiss on stop
- `FocusServiceDependencies` + Hilt EntryPoint для service-слоя

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅

## Фаза 5 — Soft Lock — статус: done — дата: 2026-09-09

### Сделано
- `BypassFlow` UI в `BlockViewModel` + `BlockScreen`: delay, breathing, reason, phrase (FR-31)
- Запрет вставки в поле фразы (TR-07): отклонение multi-char paste
- `GrantBypassUseCase`: access window в Room, `BYPASS_GRANTED`, alarms на warning/expiry
- `AccessWindowNotificationFactory` + `AlarmReceiver` (TR-11): ongoing + warning за 30 с
- FGS: `ACTION_ACCESS_WINDOW_GRANTED/EXPIRED` — notification + re-block
- `PauseSessionUseCase` / `ResumeSessionUseCase` (FR-23); блокировка отключена на паузе
- Home: Pause/Resume, «Stop» с подтверждением (FR-28)

### Известные проблемы
- Overlay fallback не поддерживает полный bypass flow (только BlockActivity)
- Авто-resume после 10 мин паузы — не реализован (ручной Resume)
- E2E §14.3 на устройстве не прогнан

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅

## Фаза 6 — Hard Lock — статус: done — дата: 2026-09-09

### Сделано
- `HardLockExtrasResolver`: settings/installer/non-default launcher packages в snapshot (FR-36 partial)
- `HardLockExtrasContributor` / `HardLockLifecycleController` + Android bindings (Hilt)
- `StartSessionUseCase`: hard lock extras + default launcher в snapshot; lifecycle on stop
- `TamperResponder`: BACK+HOME при попытке открыть Settings во время hard lock (FR-37, TR-08)
- `SessionBlockCoordinator`: tamper detection → block с tamper message + `TAMPER_ATTEMPT` event
- Block UI: `tamperMessage` в BlockRequest/BlockScreen
- `DummyAdminReceiver.onDisableRequested()` warning (FR-38)
- Home: двухшаговое подтверждение старта hard lock (§11.2)
- `ProtectionInfoScreen` + strings ru/en (FR-39a partial); ссылка в Settings
- `RequestEmergencyExitUseCase` / `CancelEmergencyExitUseCase` / `CompleteEmergencyExitUseCase` (FR-35)
- Block screen: аварийный выход (delay 10 min / retype 300 chars) для hard lock
- Alarm + FGS handler для завершения сессии по таймеру аварийного выхода
- Навигация Settings → ProtectionInfo (иконка шестерёнки в TopAppBar)

### Осталось (backlog)
- Device Admin activation UI + auto-deactivate
- Разрешённые ярлыки Settings (Wi‑Fi и т.д.) с access window
- FR-14/FR-91 editing restrictions during hard session

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅

## Фаза 7 — Расписания и помодоро — статус: done — дата: 2026-09-09

### Сделано
- `PlanSchedulesUseCase`, `StartScheduledSessionUseCase`, `CheckMissedSchedulesUseCase` (FR-40–FR-41)
- Объединение targets + strictest lock при пересечении расписаний (FR-40)
- `ScheduleSkipRepository` + «Пропустить сегодня» + warning notification (FR-42)
- `TimeChangeReceiver` + BootReceiver replan (FR-43)
- `SchedulesViewModel`, `ScheduleEditorScreen`, CRUD + toggle в AppNavigation
- `AdvancePomodoroPhaseUseCase`: фазы FOCUS/BREAK, blocking off на перерыве (FR-27)
- Pomodoro break notifications + warning за 30 с (FR-27)
- Кнопка «Pomodoro (4 cycles)» на Home; `HomeViewModel.startPomodoroSession()`
- Debug-ускоренные длительности pomodoro (`BuildConfig.DEBUG` → 1/1/2 мин)
- Фаза pomodoro на `ActiveSessionCard` через `ActiveSessionSnapshotStorage.observe()`

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅

## Фаза 8 — Статистика и журнал — статус: done — дата: 2026-09-09

### Сделано
- `StatsAggregator`, `EpochDays`, streak/completion rate (FR-80)
- `StatsRepository` + `RealStatsRepository`: пересчёт `DailyStats`, top-5 blocked apps
- `UpdateDailyStatsOnSessionEndUseCase` в `StopSessionUseCase`; `RecalculateDailyStatsUseCase`
- `PruneEventLogUseCase` (90 дней, FR-82); `DailyMaintenanceWorker` (WorkManager)
- `GetStatsDashboardUseCase`, `ExportStatsCsvUseCase`, `ObserveEventLogUseCase`
- `StatsViewModel` + `StatsScreen`: KPI, Canvas bar chart, streak, event log filters, CSV export (FR-81, FR-83)
- `StatsAggregatorTest`

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅

## Фаза 9 — Виджеты, тайл, ярлыки — статус: done — дата: 2026-09-09

### Сделано
- Glance-виджет `FocusWidget` (FR-70): состояние сессии, таймер, Start/Stop/Open; resize 2×1–4×2
- `WidgetUpdateScheduler`: обновление из FGS не чаще 1 раз/мин
- `FocusTileService` (FR-71): состояние, start/stop, fallback в app при hard lock / permissions
- `ProfileShortcutsManager` + dynamic shortcuts для 3 профилей (FR-72)
- `QuickStartLastProfileUseCase`, `GetWidgetSessionStateUseCase`, last-used profile в DataStore
- Shortcut intent → `MainActivity` → `QuickStartProfileUseCase`

### Backlog
- FR-16 Notification Listener (скрытие уведомлений целевых приложений)

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅

## Фаза 10 — Настройки, l10n, a11y, полировка — статус: in progress — дата: 2026-09-09

### Сделано
- `SettingsViewModel` + полный `SettingsScreen` (FR-90): разрешения, внешний вид, блокировка, данные, о приложении
- Per-app language через `AppLocaleController` + `locales_config.xml` (ru/en)
- Тема и dynamic color из DataStore → `MainActivity` / `FocusTheme`
- `AllowlistSettingsScreen` (FR-61): управление пользовательским белым списком
- `ClearStatisticsUseCase`, `DeleteAllUserDataUseCase` + экспорт CSV из настроек
- `PrivacyPolicyScreen`, `LicensesScreen` (статический список OSS)
- FR-91: блокировка allowlist / clear / delete во время hard lock
- Локализация ru/en: `HomeScreen`, `StatsScreen`, `SchedulesScreen`, `ScheduleEditorScreen`, диалоги и nav в `AppNavigation`
- Локализация: onboarding, profiles, permissions, blocker, **session** (`StartSessionScreen`, `ActiveSessionScreen`, `SessionSummaryScreen`)
- `contentDescription` для навигации, home stats, schedules, stats KPI, block screen, profiles, permissions (FR-101 partial)

### Осталось (backlog Phase 10)
- Локализация: widget strings audit, tamper messages из domain
- TalkBack на экране блокировки (полный flow bypass), 200 % шрифт без обрезания (FR-101)
- Baseline Profile, R8, размер APK (NFR-05/06)
- Adaptive icon + splash, анимации, пустые состояния

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / detekt ✅
