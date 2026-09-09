# План Фазы 1 — Домен и данные

## Цель
Реализация доменного слоя (§9) согласно spec.txt: модели, Room entities/DAO, Proto DataStore.

## Изменения

### 1. Domain модели (§9.3) — core/domain/src/main/kotlin/app/focus/android/core/domain/model/
- Profile.kt — data class с LockMode enum
- Session.kt — data class с SessionStatus enum  
- Schedule.kt — data class для расписаний
- AccessWindow.kt — окно доступа при bypass
- BlockDecision.kt — sealed interface (Allow | Block)
- BypassStep.kt — enum для шагов пропуска
- PomodoroConfig.kt — конфигурация помодоро
- AppInfo.kt — информация о пакете приложения
- FocusEvent / EventLog — события и журнал

### 2. Room Entity §9.1 — core/database/src/main/kotlin/app/focus/android/core/database/entity/
- ProfileEntity, SessionEntity, ScheduleEntity
- AccessWindowEntity, DailyStatsEntity, AllowlistEntity
- EventLogEntity, ProfileAppEntity
- FocusDatabase.kt — Room database singleton
- DAO interfaces с Flow/suspend методами

### 3. Proto DataStore §9.2 — core/datastore/src/main/proto/
- settings.proto — UserSettings
- active_session.proto — ActiveSessionSnapshot

### 4. Use Cases — core/domain/src/main/kotlin/app/focus/android/core/domain/usecase/
- StartSessionUseCase, StopSessionUseCase
- RestoreSessionUseCase
- ObserveActiveSessionUseCase
- DecideBlockUseCase (интеграция BlockDecisionEngine)
- GrantBypassUseCase

## Verification DoD
```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest  
./gradlew lintDebug detekt
```
