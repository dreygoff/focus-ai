# Фаза 1: Домен и данные — Task List (В процессе)

## P0 — Fix Build Infrastructure (P0 - critical)
- [x] Создать Unix `gradlew` script
- [x] Исправить build-logic/settings.gradle.kts (version catalogs)
- [x] Добавить зависимости в build-logic/convention/build.gradle.kts
- [x] Создать AndroidJvmLibraryPlugin (build-logic)
- [x] Исправить app/build.gradle.kts (modules, api vs implementation)
- [ ] Доделать convention плагины полностью (detekt/spotless/lint config)
- [ ] Добавить detekt.yml, .editorconfig, lint.xml
- [ ] Запустить `./gradlew :app:assembleDebug` и фиксить ошибки

## P0 — Critical Phase 1 Gaps (ЗАВЕРШЕНО)
- [x] **CREATE BypassFlow.kt** — реализация конечного автомата TR-07 (Idle → Delay → Breathing → Reason → Phrase → Granted/Denied)
- [x] **CREATE BypassFlowConfig** — data class конфигурации обхода блокировки
- [x] **IMPLEMENT PomodoroConfig methods** — добавлены `tick()`, `currentPhaseName`, `currentPhaseRemainingSeconds()`, `isComplete()`, `remainingFocusCycles()`
- [ ] **FIX ScheduleAlarmPlannerTest** — добавить импорты вместо fully qualified class names

## P1 — Domain Model Deduplication
- [x] Миграция всех ссылок с `app.focus.android.domain.model` → `app.focus.domain.model`
- [x] Удаление java/ директории моделей
- [ ] Фикс типов: Session.source (String→SessionSource enum), pomodoroConfig (String?→PomodoroConfig?), emergencyExitMode (String→EmergencyExitMode), allowedSettingsShortcuts (List<String>→Set<SettingsShortcut>)
- [ ] Update all mappers/conversion logic
- [ ] Обновить BlockReason на enum

## P2 — Missing Components
- [x] Добавить Clock абстракцию в core/domain
- [x] Добавить ScheduleAlarmPlannerTest и PomodoroConfigTest
- [ ] Добавить UserSettings модель
- [ ] Конвертировать core:data из JVM → Android library (Room DAO импорт)

## P3 — Unit Tests Coverage
- [ ] SessionStateMachineTest — все переходы + инварианты (§10 spec)
- [ ] BlockDecisionEngineTest — ≥ 30 табличных тестов
- [x] BypassFlow.kt реализована (BypassFlowTest требует исправления типа Denied в BypassState)
- [ ] ScheduleAlarmPlannerTest — DST, полночь, дни недели

## Phase 1 DoD Checklist
- [ ] Покрытие core:domain ≥ 90%
- [ ] Покрытие core:data ≥ 80%
- [ ] Все инварианты §10 покрыты тестами
- [ ] `./gradlew :app:assembleDebug` зелёный
- [ ] `./gradlew testDebugUnitTest` зелёный
- [ ] `./gradlew lintDebug detekt` без критических ошибок
