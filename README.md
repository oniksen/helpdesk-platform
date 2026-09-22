# Helpdesk Platform

Web-приложение для управления задачами и сервисными обращениями, построенное на Kotlin Multiplatform с Compose Multiplatform.

[![PR Check](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-check.yml/badge.svg)](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-check.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Coverage](coverage-badge.svg)

## Проблема

Сервисные команды нуждаются в едином интерфейсе для обработки обращений, управления парковочными местами и отслеживания задач. Helpdesk Platform объединяет эти процессы в одном web-приложении с modern UI и модульной архитектурой.

## Быстрый старт

### Требования

- JDK 21
- Gradle (используется wrapper)

### Запуск

```bash
# Wasm (быстрее, современные браузеры)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# JS (для старых браузеров)
./gradlew :webApp:jsBrowserDevelopmentRun
```

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

## Команды

```bash
# Запуск
./gradlew :webApp:wasmJsBrowserDevelopmentRun    # Web (Wasm)
./gradlew :webApp:jsBrowserDevelopmentRun       # Web (JS)

# Тесты
./gradlew :shared:wasmJsTest                     # Wasm-тесты
./gradlew allTests                               # Все тесты

# Линтер
./gradlew detekt

# Покрытие тестами
./gradlew koverXmlReportsAll                     # XML-отчёты по модулям
./gradlew generateCoverageBadge                  # Генерация badge SVG
```

## Покрытие тестами

Проект использует [Kotlinx Kover](https://github.com/Kotlin/kotlinx-kover) для сбора метрик покрытия.

```bash
# Собрать XML-отчёты по всем модулям
./gradlew koverXmlReportsAll

# Сгенерировать badge с процентом покрытия
./gradlew generateCoverageBadge

# HTML-отчёт по конкретному модулю
./gradlew :features:parking:impl:koverHtmlReport
```

## Добавление фичи

1. Создать `features/<name>/api` с маршрутом (`@Serializable data object ... : NavKey`)
2. Создать `features/<name>/impl` с `FeatureNavModule` и экранами
3. Зарегистрировать модуль в `DiProvider`
4. Добавить модули в `settings.gradle.kts`

## Контрибьюция

- Базовая ветка: `develop`
- Требования: JDK 21
- Перед отправкой PR: `./gradlew detekt && ./gradlew allTests`

## Лицензия

[LPMTI Proprietary Internal Use License](LICENSE)
