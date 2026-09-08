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

## Фаза 5 — Soft Lock — статус: in progress — дата: 2026-09-09

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
