Правила написания коммита:

- Первая строка не длиннее 40 символов.
- После неё обязательно идёт пустая строка.
- Далее следуют читаемые и понятные пояснения изменений, объединённые при необходимости в одно или два предложения.
- Каждая строка с пояснением начинается с символа '- ' и заканчивается точкой.

## Формат текста для Pull Request

### Структура

PR должен содержать следующие разделы:

1. **Описание** — одно-два предложения о том, что делает PR.
2. **Изменения** — список изменений по модулям/областям с краткими пояснениями.
3. **Файлы** — итоговая статистика (количество файлов, +/- строки).

### Правила

- Язык — русский.
- Описание — ёмкое, без воды.
- Каждый пункт изменений — с заголовком области (Экран, Домен, ViewModel, Core, Зависимости и т.д.) и списком конкретных изменений через `-`.
- Не перечислять каждый файл отдельно — группировать по логическим блокам.
- Указывать version bump'ы зависимостей, если они есть.
- Не включать код в описание (только если явно не попросят).

## Обзор проекта

Kotlin Multiplatform (KMP) проект — Web-приложение (JS + Wasm). UI на Compose Multiplatform, DI через Koin, навигация через Navigation3.

## Модули

| Модуль | Назначение |
|--------|------------|
| `core/di` | DI-провайдер (Koin), точка входа в граф зависимостей |
| `core/navigation/api` | Интерфейс `FeatureNavModule` для фич |
| `core/navigation/impl` | `BasicDslContainer` — навигационный контейнер |
| `features/*/api` | Маршруты фич (`@Serializable data object : NavKey`) |
| `features/*/impl` | Реализация экранов и модулей фич |
| `maxminiappapi/api` | Интерфейсы API (мост к внешней платформе) |
| `maxminiappapi/impl` | Платформенные реализации API |
| `shared` | Общий код (`App.kt`, `Platform.kt`) |
| `webApp` | Точка входа Web-приложения |

## Паттерны

- **API/Impl разделение**: каждый модуль фичи — `api` (интерфейсы, маршруты) + `impl` (экраны, фабрики)
- **Навигация**: фича реализует `FeatureNavModule` → `canResolve()` + `resolve()`
- **DI**: Koin, модули регистрируются в `DiProvider`
- **Сериализация маршрутов**: `@Serializable data object` для `NavKey`

## Команды

```bash
# Web (Wasm — быстрее)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS — для старых браузеров)
./gradlew :webApp:jsBrowserDevelopmentRun

# Тесты
./gradlew :shared:wasmJsTest
./gradlew allTests

# Линтер
./gradlew detekt
```

## CI/CD

PR → `develop`: `detekt` + `allTests` (JDK 21, Gradle configuration cache).

## Конвенции

- `kotlin.code.style=official`
- Комментарии на русском языке
- Пакеты: `org.lpmti.helpdeskplatform.*`, фичи — `navigation`, `presentation.screen`

## Добавление фичи

1. Создать `features/<name>/api` с маршрутом (`@Serializable data object ... : NavKey`)
2. Создать `features/<name>/impl` с `FeatureNavModule` и экранами
3. Зарегистрировать модуль в `DiProvider`
4. Добавить модули в `settings.gradle.kts`
