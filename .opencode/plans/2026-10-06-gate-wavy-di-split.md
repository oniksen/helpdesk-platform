# Гейт: волнистый индикатор M3 и лёгкий web-shell

Дата: 2026-10-06. Ветка: `feature/lightweight-gate`. Статус: реализовано.

## Контекст

На старте приложения (гейт) пользователь видит долгую паузу: загружается
`webShell.js` с Compose/Koin-навигацией (48,1 МБ), редирект тормозит из-за
ожиданий между шагами, а список этапов отвлекает от главного — фактически
гейт делает три вещи: авторизация, выбор канала, редирект.

Четвёртая часть работы — анализ второго (после редиректа) тормозящего
индикатора без правок кода: CI и `build-web-app`/`build-web-shell` собирают
dev-бандл (`jsBrowserDevelopmentExecutableDistribution`), поэтому
`webApp.js` = 56 221 381 байт, плюс `skiko.wasm` 8.6 МБ; SMIL-анимация
считается на главном потоке и замирает. Рекомендации (вне этой итерации):
`jsBrowserProductionExecutableDistribution`, убрать дублирование
`max-web-app.js` в `webApp/index.html`.

## Решения (согласованы)

1. **Стадии гейта убрать** из DOM и вернуть одиночный текст статуса
   (состояние до `940117d`): «Авторизация...», «Определение канала»,
   «Загрузка приложения», «Авторизация на сервере»,
   «Получение данных пользователя». Параллельная загрузка каналов и
   манифеста из `940117d` сохраняется (это ускорение).
2. **Спиннер → точная копия M3 Expressive `CircularWavyProgressIndicator`**
   с дефолтными токенами документации:
   - `Size`=40 (r=20), `WaveSize`=48, `ActiveThickness`/`TrackThickness`=4,
     `ActiveWaveAmplitude`=±2, `ActiveWaveWavelength`=15,
     `TrackActiveSpace`=4, `waveSpeed`=1 длина волны/сек;
   - цвета: primary `#6750A4`, трек secondaryContainer `#E8DEF8`
     (раньше ошибочно использовался `#E6E1E5`);
   - путь: `r(θ)=20+2·sin(8θ)`, центр (24,24), шаг 2° (180 точек),
     `pathLength="100"`, длина пути 143.77 dp, λ по пути 17.97 dp;
   - `stroke-dasharray: 94.44 5.56` — зазор 8 dp пути (4 dp + по 2 dp на
     скруглённые концы);
   - анимации: вращение обёртки `transform: rotate(360deg)` за 8 с
     (`m3-wavy-spin`), `stroke-dashoffset: 0 → -100` за 2 с (`m3-wavy-sweep`).
   Путь генерируется python-сниппетом (запасной вариант `k=10`,
   λ=15.26 dp) и вставляется в `index.html`.
3. **Обычные `<script>`** вместо отложенной загрузки: мост MAX
   (`st.max.ru/js/max-web-app.js`) затем `webShell.js` — порядок документа,
   индикатор рисуется до их исполнения.
4. **`core:di` и Compose — вне webShell**: инфраструктурные Koin-модули
   выносятся в новый модуль `core:di:infra`; Koin остаётся, Compose — нет.

## Задачи

### Task 1 — модуль `core:di:infra`
- Создать `core/di/infra/build.gradle.kts` (kotlinMultiplatform + kover,
  `js { browser() }`, deps: `core.authorization.api/impl`,
  `core.network.api/impl`, `maxminiappapi.api/impl`, `libs.koin.core`).
- `git mv core/di/src/commonMain/kotlin/InfraModules.kt` →
  `core/di/infra/src/commonMain/kotlin/InfraModules.kt`.
- `core/di/build.gradle.kts`: убрать infra-зависимости и jsMain-wrappers,
  добавить `implementation(projects.core.di.infra)`.
- `webShell/build.gradle.kts`: `implementation(projects.core.di.infra)`.
- `settings.gradle.kts`: `include(":core:di:infra")`; корневой
  `build.gradle.kts`: `coverageModules += ":core:di:infra"`.
- KDoc: `DiProvider.kt`, `InfraModules.kt` — без кросс-модульных ссылок.
- Строки модуля в `AGENTS.md` и `README.md`.
- Дополнительно: убрать неиспользуемый `libs.bundles.koin` из
  `core/authorization/impl` — через него в classpath гейта попадал
  `koin-compose` → compose-runtime (не попадал в бандл, но занимал место
  в compileSync и тянул `composeResources`).

### Task 2 — wavy-индикатор вместо списка стадий
- `webShell/src/jsMain/resources/index.html`: `.m3-wavy-loading` с двумя
  `<path>` (трек + активный), удалить `<ol id="gate-steps">`.
- `webResources/styles.css`: удалить `.gate-steps`, `.m3-circular-loading`,
  `@keyframes m3-rotate`/`m3-dash`; токен
  `--md-sys-color-surface-container-highest` →
  `--md-sys-color-secondary-container: #E8DEF8`; добавить `.m3-wavy-*` и
  `@keyframes m3-wavy-spin`/`m3-wavy-sweep`.
- `GateUi.kt`: только `showStatus`, `showError`, `onRetry`,
  `private companion object` (возврат к версии до `940117d`).
- `GateApp.kt`: `showStep`+`nextPaint` → `showStatus`, `showDetail` →
  `showStatus`; `nextPaint()` удалён (методы и так уступают поток async).

### Task 3 — обычные `<script>`
- `index.html`: инлайн-загрузчик заменён на
  `<script src="https://st.max.ru/js/max-web-app.js">` и
  `<script src="webShell.js">`.

## Приёмка

- `./gradlew detekt`, `./gradlew allTests`,
  `./gradlew --warning-mode=fail help`, `./gradlew koverXmlReportsAll`.
- `rm -rf dist-shell dist && ./gradlew build-web-shell build-web-app`.
- В `dist-shell`: нет `skiko*`, `composeResources`, `js-reexport-symbols.mjs`;
  `webShell.js` < 20 МБ; в compileSync нет `compose|skiko|navigation3|
  uiadaptive`.
- Ручной прогон `./gradlew :webShell:jsBrowserDevelopmentRun`.

### Фактические результаты

| Показатель | Было | Стало |
|---|---|---|
| `dist-shell/webShell.js` | 48 122 045 | 7 032 202 (−85%) |
| Содержимое `dist-shell` | skiko, composeResources, js-reexport | только файлы гейта |
| compileSync `compose\|skiko\|navigation3\|uiadaptive` | есть | 0 |
| `detekt` / `allTests` / `help --warning-mode=fail` | — | успешно |
| `build-web-app` | — | успешно (Compose и skiko на месте) |
| dev-сервер | — | index/styles/js отдаются, 200 |

Остаточно: `dist-shell/composeResources` — пустые каталоги старой сборки,
исчезают после чистки `webShell/build/processedResources`.
