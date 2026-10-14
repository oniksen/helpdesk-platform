# Helpdesk Platform

Web-приложение для управления задачами и сервисными обращениями. Объединяет обработку обращений, управление парковочными местами и отслеживание задач в одном интерфейсе, чтобы сервисные команды работали в едином процессе вместо разрозненных инструментов.

Проект построен на Kotlin Multiplatform с Compose Multiplatform (UI), Koin (DI) и Navigation3 (навигация). Целевая платформа — Web (JS), дополнительно есть JVM-таргет для превью и тестов.

[![PR Check](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-check.yml/badge.svg)](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-check.yml)
[![License](https://img.shields.io/badge/License-LPMTI%20Proprietary-lightgrey.svg)](LICENSE)
![Coverage](coverage-badge.svg)

## Документация

Подробная документация — в [WiKi проекта](https://github.com/oniksen/helpdesk-platform/wiki):

- [Быстрый старт](https://github.com/oniksen/helpdesk-platform/wiki/Быстрый-старт) — запуск, тесты, линтер
- [Архитектура](https://github.com/oniksen/helpdesk-platform/wiki/Архитектура) — модули, паттерны, структура
- [Добавление фичи](https://github.com/oniksen/helpdesk-platform/wiki/Добавление-фичи) — пошаговая инструкция
- [CI/CD](https://github.com/oniksen/helpdesk-platform/wiki/CI-CD) — пайплайн и проверки
- [Конвенции](https://github.com/oniksen/helpdesk-platform/wiki/Конвенции) — стиль кода, коммиты, PR

## Контрибьюция

Требуется JDK 21. Перед отправкой PR: `./gradlew detekt && ./gradlew allTests`. Правила оформления кода, коммитов и PR — в разделе [Конвенции](https://github.com/oniksen/helpdesk-platform/wiki/Конвенции).

## Лицензия

[LPMTI Proprietary Internal Use License](LICENSE)
