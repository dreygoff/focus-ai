# Фаза 1: Домен и данные — Implementation Plan

## Цель
Реализовать доменный слой (чистый Kotlin JVM), data layer, базу данных Room и Proto DataStore согласно §9 spec.

## Артефакты для создания

### 1. core:domain (JVM module — чистый Kotlin)

#### 1.1 Модели (§9.3)
- `LockMode` (sealed interface): `Soft`, `Hard`
- `SessionStatus` (sealed interface): `Scheduled`, `Running`, `Paused`, `Completed`, `Cancelled`, `Expired`
- `Profile` — data class: id, name, emoji, colorArgb, lockMode, defaultDurationMinutes, bypassDelaySeconds, bypassBreathingEnabled, bypassReasonRequired, bypassPhrase, bypassLimitPerSession, accessWindowMinutes, bypassAppliesToAllApps, emergencyExitMode, blockNewApps, deviceAdminProtection, allowedSettingsShortcuts, hideTargetNotifications
- `ProfileApp` — data class: profileId, packageName, addedAt
- `Session` — data class: id, profileId, profileNameSnapshot, lockMode, targetPackagesSnapshot, goalText, startedAt, plannedEndAt, actualEndAt, status, source, pomodoroConfig, bypassesUsed, blockAttempts, pausesUsed, scheduleId
- `Schedule` — data class: id, profileId, enabled, daysOfWeekMask, startMinuteOfDay, endMinuteOfDay, allowSkipDay, label
- `AccessWindow` — data class: id, sessionId, packageName, grantedAt, expiresAt, reason, restrictedToActivity
- `EventLog` — data class: id, timestamp, sessionId, type, packageName, payload
- `AllowlistEntry` — data class: packageName, addedAt
- `DailyStats` — data class: dateEpochDay, focusMinutes, sessionsCompleted, sessionsTotal, blockAttempts, bypasses
- `AppInfo` — data class: packageName, appName, icon (ByteArray), usageTimeLast7Days
- `BlockDecision` (sealed interface): `Allow`, `Block(reason: BlockReason)`
- `BlockReason` — sealed interface: `TargetApp`, `HardLockExtra`, `AccessWindowExpired`
- `FocusEvent` — sealed interface для событий UI
- `BypassConfig` — данные конфигурации пропуска
- `PomodoroConfig` — данные помодоро
- `PermissionState` — состояние разрешений

#### 1.2 Repository interfaces (in core:domain)
- `ProfileRepository` — CRUD for profiles + profile apps
- `SessionRepository` — CRUD for sessions
- `ScheduleRepository` — CRUD for schedules
- `AccessWindowRepository` — CRUD for access windows
- `EventLogRepository` — write events, read logs
- `AllowlistRepository` — manage allowlist
- `DailyStatsRepository` — read/write daily stats
- `InstalledAppRepository` — observe installed apps
- `SettingsRepository` — user settings via DataStore

#### 1.3 SessionStateMachine (§10 spec)
- Чистый Kotlin, полностью покрыт тестами
- Состояния: Idle, Running, Paused, Completed, Cancelled, Expired, ExitPending
- Методы: start, pause, resume, end, stopRequested, emergencyExitRequested, emergencyExitConfirmed, restore
- Инварианты: не более одной RUNNING/PAUSED; в HARD невозможны Pause и StopRequested; bypassesUsed ≤ limit

#### 1.4 BlockDecisionEngine (TR-06)
- Чистый Kotlin, табличные тесты ≥ 30 случаев
- Метод: decide(foregroundPackage, sessionState, allowlist, accessWindows, isHardLock, targetPackages)
- Приоритеты: no session → allow; in allowlist → allow; in call → allow; access window active → allow; in targets → block

#### 1.5 BypassFlow (TR-07) ✅ ЗАВЕРШЕНО
- Конечный автомат: Idle → Delay → Breathing → Reason → Phrase → Granted/Denied
- BypassFlowConfig data class — delaySeconds, breathingEnabled, reasonRequired, phraseText, bypassLimitPerSession, accessWindowMinutes
- Методы: start(), tickDelay(), tickBreathing(deltaMs), submitReason(text), inputPhraseCharacter(char), checkBypassLimit(), getAccessWindowEndAt(), resetToIdle()
- Блокировка при превышении лимита → Denied state

#### 1.6 ScheduleAlarmPlanner (TR-10)
- Рассчёт ближайших startAt/endAt для расписаний
- Обработка перехода через полночь, DST
- Метод: planNextTrigger(schedule, now): NextTriggerResult

#### 1.7 PomodoroPlanner ✅ ЗАВЕРШЕНО
- PomodoroConfig с методами FSM: tick(), currentPhaseName, currentPhaseRemainingSeconds()
- Фазы: FOCUS → SHORT_BREAK/LONG_BREAK → FOCUS
- Переход на LONG_BREAK каждые cyclesBeforeLongBreak

#### 1.8 Clock abstraction
- Интерфейс `Clock` с методом `nowMillis(): Long`
- Реализации: `SystemClock`, `TestClock`

### 2. core:database (Android module)

#### 2.1 Room Entities (§9.1)
- ProfileEntity, ProfileAppEntity, SessionEntity, ScheduleEntity, AccessWindowEntity, EventLogEntity, AllowlistEntity, DailyStatsEntity
- Все типы как в spec с правильными primary keys, foreign keys, indices

#### 2.2 DAO interfaces
- ProfileDao, SessionDao, ScheduleDao, AccessWindowDao, EventLogDao, AllowlistDao, DailyStatsDao
- Flow для чтения, suspend для записей, @Transaction для составных операций

#### 2.3 FocusDatabase
- Room database с migration (version 1 → future)
- Hilt модуль для предоставления Database и DAO

### 3. core:datastore (Android module)

#### 3.1 Proto schemas (§9.2)
- settings.proto: UserSettings с Theme enum
- active_session.proto: ActiveSessionSnapshot

#### 3.2 Repository interfaces implementations
- SettingsRepositoryProtoImpl — чтение/запись через Proto DataStore
- ActiveSessionSnapshotStore — device-protected storage

### 4. core:data (Android module)

#### 4.1 Repository implementations
- ProfileRepositoryImpl, SessionRepositoryImpl, ScheduleRepositoryImpl
- AccessWindowRepositoryImpl, EventLogRepositoryImpl, AllowlistRepositoryImpl
- DailyStatsRepositoryImpl
- InstalledAppRepositoryImpl — через PackageManager

#### 4.2 Mappers (Entity ↔ Domain)
- Entity to Domain мапперы для всех сущностей

### 5. core:testing (Android module)

- Fake implementations of all domain repositories
- TestClock implementation
- Data factories для создания тестовых данных
- TestHiltModule для DI в тестах

### 6. Unit Tests (в соответствующих модулях)

- SessionStateMachineTest — все переходы + инварианты (§10)
- BlockDecisionEngineTest — табличные тесты ≥ 30 случаев
- BypassFlowTest — state transitions
- ScheduleAlarmPlannerTest — DST, полночь, дни недели
- PomodoroPlannerTest — фазы pomodoro

## Текущий статус Phase 1

### ✅ ЗАВЕРШЕНО:
1. **Domain models** (§9.3) — все модели в `core/domain/src/main/kotlin/app/focus/domain/model/`
   - Profile, LockMode, Session, SessionStatus, Schedule, AccessWindow, BlockDecision, EventLog, AppInfo
   - PomodoroConfig (FSM с методами tick(), currentPhaseName(), isComplete(), remainingFocusCycles())
   - BypassState sealed interface + BreathingPhase enum (добавлен Denied state)
   - EmergencyExitMode, SettingsShortcut, SessionSource enums
2. **Repository interfaces** — `RepositoryInterface.kt` (~3045 строк) в core/domain/usecase/
3. **SessionStateMachine** (§10 spec) — fully implemented in `core/domain/internal/statemachine/`
4. **BlockDecisionEngine** (TR-06) — fully implemented
5. **BypassFlow (TR-07)** ✅ НОВОЕ — BypassFlow FSM + BypassFlowConfig реализованы в `bypassflow/`
6. **ScheduleAlarmPlanner** (TR-10) — implemented with 18+ tests covering DST, midnight crossing, day masks, overnight schedules
7. **Clock abstraction** — RealClock + TestClock
8. **Java директория удалена** — все доменные модели перемещены в kotlin/ пакет
9. **Room Database** — FocusDatabase.kt с entity и DAO
10. **DataStore** — Proto schemas + UserSettingsRepository interface + ActiveSessionSnapshotStore
11. **Repository implementations** — Real*Repository в core/data/
12. **Mappers** — SessionMapper.kt + MapperAliases.kt
13. **Fake repositories** — тестовые фейки в core/testing/

### ❌ НЕ ЗАВЕРШЕНО / ТРЕБУЕТ ВНИМАНИЯ:
1. **ScheduleAlarmPlannerTest** — использует fully qualified class names (e.g., `app.focus.domain.model.Schedule`) повсюду вместо импортов. Функционально работает, но messy.
2. Convention плагины (detekt/spotless/lint) не завершены.
3. Build infrastructure может требовать доработки (Java 25 совместимость с Gradle 8.10.2 + Kotlin).
