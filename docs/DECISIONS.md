# Архитектурные решения (ADR)

## ADR-0001: Технологический стек и архитектура

**Дата:** 2026-09-07  
**Статус:** принят  
**Контекст:** Определение базового стека технологий для Focus v1.0.

### Решение
Согласно §6 spec.txt, фиксируем следующий стек:

- **Язык:** Kotlin 2.3.21 (K2), JDK 17 toolchain
- **Сборка:** AGP 9.4.0, Gradle 9.7.1, Convention plugins
- **UI:** Jetpack Compose (BOM 2026.08.00), Material 3
- **Навигация:** Navigation Compose 2.10.0 + kotlinx-serialization-json 1.11.0
- **DI:** Hilt 2.60.1 + hilt-navigation-compose 1.4.0
- **База данных:** Room 2.8.4
- **Хранение настроек:** DataStore 1.2.1 (Preferences) + Proto DataStore
- **Асинхронность:** Coroutines 1.11.0 + Flow
- **Логи:** Timber 5.0.1
- **Изображения:** Coil 3.6.2

Архитектура: Clean Architecture + MVI/UDF (unidirectional data flow).  
Domain-слой — чистый Kotlin JVM без Android-зависимостей.

### Консеквенции
- Версии LOCKED до релиза 1.0.0 (§6.1 spec)
- Никаких RxJava, Dagger без Hilt, SharedPreferences напрямую
- Compose компоненты без явной версии — только через BOM

---
