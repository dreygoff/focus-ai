# Progress

## Фаза 0 — Инициализация — статус: in progress — дата: 2026-09-07

### Сделано
- Создана структура модулей (§7.2 spec)
- Настроен `build.gradle.kts` корневой и app с плагинами
- Convention плагины в `build-logic/` (скелеты созданы, нужно наполнить)
- Core модули: domain, data, database, datastore, system, designsystem, ui, notifications, common, testing
- Feature модули: onboarding, permissions, home, profiles, session, blocker, schedules, stats, settings, widget
- Сущности Room (§9.1): ProfileEntity, SessionEntity, ScheduleEntity, AccessWindowEntity, DailyStatsEntity, AllowlistEntity, EventLogEntity, ProfileAppEntity
- DAO скелеты
- Domain модели (§9.3): Profile, LockMode, Session, SessionStatus, Schedule, AccessWindow, BlockDecision, FocusEvent, EventLog, AppInfo, PomodoroConfig, PermissionState, DailyStats
- доменный State Machine (SessionStateMachine — реализован)
- BlockDecisionEngine (скелет)
- Navigation routes (type-safe: SessionRoutes)
- Base Hilt + ViewModel setup начат

### Отклонения от ТЗ / решения
- ADR-0001 создан и задокументирован

### Известные проблемы
- Convention плагины в `build-logic/` — пустые скелеты, нужно наполнить реализацией.
- `libs.versions.toml` заполнен согласно §6 spec.

### Проверки
- assembleDebug: ❌ (конвенция плагинов не завершена)
- testDebugUnitTest: ❌
- lintDebug: ❌
- detekt: ❌

## Фаза 1 — Домен и данные — статус: todo — дата: 
### Сделано
### Отклонения от ТЗ / решения
### Известные проблемы
### Проверки
- assembleDebug: ⬜
- testDebugUnitTest: ⬜
- lintDebug: ⬜
- detekt: ⬜
