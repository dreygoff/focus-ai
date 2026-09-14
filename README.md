# Focus

Focus — офлайн-приложение цифрового благополучия для Android, которое помогает блокировать выбранные приложения во время активной сессии фокуса.  
Проект рассчитан на Android 8.0–16, с фиксированным стеком и версиями зависимостей на весь цикл v1.0.

## Что делает приложение

Пользователь выбирает приложения-«отвлекатели», создаёт профили и запускает сессии фокуса.  
Во время активной сессии Focus отслеживает foreground-приложение и при открытии заблокированного приложения мгновенно показывает полноэкранный экран блокировки.

Поддерживаются два режима:

- **Soft Lock** — блокировка с «трением»: задержка, дыхательная пауза, ввод причины, ограниченное число пропусков и окно доступа.
- **Hard Lock** — жёсткая блокировка без пропуска до конца сессии, с мерами защиты от обхода в рамках возможностей платформы.

## Ключевые возможности

- онбординг и экран разрешений с чек-листом статусов;
- выбор приложений по списку установленных launcher-apps;
- профили с настройками soft/hard lock;
- ручной запуск сессий, запуск по расписанию и помодоро;
- экран блокировки с анимацией и мотивационным контентом;
- allowlist системных приложений и пользовательский белый список;
- статистика, журнал событий и экспорт CSV;
- виджет рабочего стола, Quick Settings tile и App Shortcuts;
- локализация RU/EN и поддержка accessibility;
- полный офлайн-режим без `INTERNET`, без аналитики и без облака.

## Технологический стек

- **Kotlin 2.3.21**, **JDK 17**
- **Gradle 9.7.1**
- **Android Gradle Plugin 9.4.0**
- **KSP 2.3.11**
- **Compose BOM 2026.08.00**
- **minSdk 26**, **compileSdk 37**, **targetSdk 36**
- **Jetpack Compose + Material 3**
- **Navigation Compose**
- **Hilt**
- **Room**
- **DataStore + Proto**
- **WorkManager**
- **Glance**
- **Coil 3**
- **Timber**
- **Coroutines + Flow**
- **Detekt**, **Spotless**, **Robolectric**, **Turbine**, **MockK**, **Roborazzi**

Версии библиотек и плагинов зафиксированы в `gradle/libs.versions.toml` и не меняются без ADR.

## Архитектура

Проект построен по принципам **Clean Architecture + UDF**.

Слои:

- `presentation` / `feature` — UI, ViewModel, UiState, UiEvent, UiEffect;
- `domain` — use cases, модели, state machine, интерфейсы репозиториев;
- `data` — реализации репозиториев, Room, DataStore, системные адаптеры;
- `system` — обёртки над Android API;
- `designsystem` — тема, компоненты, типографика, иконки;
- `testing` — фейки и тестовые правила.

## Основные компоненты рантайма

- `FocusForegroundService` — foreground-service с режимом `specialUse`;
- `SessionEngine` — state machine и таймеры сессии;
- `DetectorOrchestrator` — переключение между `AccessibilityService` и `UsageStatsManager`;
- `BlockDecisionEngine` — решение блокировать или пропустить приложение;
- `BlockerLauncher` — запуск `BlockActivity` или overlay fallback;
- `BootReceiver`, `AlarmReceiver`, `PackageChangeReceiver` — восстановление, расписания и инвалидация кешей.
