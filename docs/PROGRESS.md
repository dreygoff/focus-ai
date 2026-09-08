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
- PermissionChecker (FR-02): MANDATORY = usage stats + overlay; RECOMMENDED = accessibility, notifications, battery, exact alarms; OPTIONAL = device admin
- PermissionSettingsIntents + PermissionsScreen с disclosure (NFR-12), restricted settings hint (FR-04), onResume refresh (FR-03)
- Онбординг: 4 экрана в графе, `startDestination` по `isOnboardingCompleted()` в MainActivity
- Profiles: AppPicker (поиск, сортировка, системные apps, allowlist), ProfileEditor CRUD, FR-14 блокировка редактирования при hard-сессии
- PackageRepository + ViewModel-тесты: PermissionsViewModel, ProfilesViewModel, AppPickerViewModel

### Известные проблемы
- Coil-иконки в AppPicker (TR-09) — отложено; список без иконок
- `getDefaultProfileId` в RealProfileRepository — in-memory stub

### Проверки
- assembleDebug ✅ / testDebugUnitTest ✅ / lintDebug ✅ / detekt ✅
