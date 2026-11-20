# Дизайн: сборки, CI и релизы мультиплатформенного проекта

Дата: 2026-11-20. Ветка: `chore/ci-and-workflow-improvements`. Статус: реализовано.

## 1. Цель

Сделать так, чтобы после PR в `main` уже существовали готовые к выкладке сборки всех таргетов, а на странице проекта появлялись тэги и релизы, отражающие реально выложенные версии — при том, что платформы версионируются независимо.

Требования:

- сборки собираются только после успешных проверок и только в контексте PR в `main`;
- проверки для работы с `develop` живут отдельно и не тащат за собой сборку дистрибутивов;
- версия каждого таргета берётся из единственного источника и не дублируется в YAML;
- тэг и релиз создаются только для таргета, у которого версия изменилась;
- все артефакты сборки лежат в одной корневой папке.

## 2. Область изменений

Входит:

- Gradle-задачи версий, упаковки и метаданных;
- единая папка `dist/` для выходов всех таргетов;
- workflow-файлы GitHub Actions;
- тэги, релизы, README-бейдж;
- документация: `AGENTS.md`, вики, удаление устаревших планов.

Не входит:

- автоматизация выкладки на сервер (деплой остаётся ручным через страницу публикации);
- изменение кода приложения и системы обновлений;
- генерация бейджа покрытия в CI;
- изменение контракта `update-manifest-v4.json`.

## 3. Принятые решения

| Решение | Обоснование |
|---|---|
| Тэг на компонент: `web-v0.3.0-alpha.6`, `jvm-v0.1.0-alpha.1` | Версии платформ независимы; плоский префикс однозначен в glob/CLI и не конфликтует с именами веток |
| `alpha` и `beta` — только аннотированный тэг, без релиза | Релизы не шумят на странице; трассируемость обеспечивает тэг + артефакты прогона, метаданные лежат в сообщении тэга |
| `rc` — черновик релиза с `--prerelease` | Пререлиз — ровно «не для продакшена»; черновик публикуется после фактической выкладки |
| `release` — черновик релиза, публикуется как latest | Метка Latest ставится только на прод-версию |
| Ассеты прикладываются только к rc/release | У alpha/beta нет релиза; сборки живут артефактами прогона |
| Проверки и сборка — один workflow `pr-main.yml` c `needs` | Требование «сборка только после проверок» выполняется нативно; `workflow_run` берёт YAML из default-ветки и усложняет фильтрацию по базовой ветке PR |
| Переиспользуемый `_checks.yml` | Убирает дублирование проверок между `develop` и `main` |
| Единая папка `dist/` с подкаталогами | Все результаты в одном месте, `.gitignore` сводится к одной строке |
| Принудительная очистка артефактов перед сборкой | Лимит Free — 500 МБ; деплой идёт в тот же день, поэтому прошлые наборы не нужны, а retention 2 дня остаётся страховкой |
| Бейдж покрытия — только вручную, перед PR | CI не может обновить картинку внутри PR без коммита в ветку автора; ответственность на разработчике |
| `webShell` без собственного тэга | Версии нет, версионируется контентным хэшем; идёт ассетом в релизе `web` |
| Бейджи в README: workflow `pr-main.yml` + покрытие кода | Бейдж покрытия обязателен в шапке; workflow-бейдж отражает гейт в `main` |

Отклонено:

- зонтичный тэг `release-<дата>` — пока деплой ручной, второго уровня трассируемости не требуется; при необходимости добавляется позже без ломки схемы;
- составные тэги `web+jvm-v…` — нечитаемы и ломают фильтры;
- релизы для alpha/beta — шумят, при этом никем не потребляются;
- авто-коммит бейджа покрытия — грязь в истории и конфликты между PR.

## 4. Единая папка сборки

```
dist/
├── web/                 # дистрибутив webApp (было dist/)
├── web-shell/           # гейт (было dist-shell/)
├── jvm/
│   ├── windows/         # образ приложения + .exe
│   └── macos/           # бандл .app
├── packages/            # zip, готовые к выгрузке на сервер
└── metadata/            # versions.json, *-metadata.json, SHA256SUMS
```

Изменения:

- `webApp/build.gradle.kts`: `distribution.outputDirectory` → `dist/web`;
- `webShell/build.gradle.kts`: `DIST_SHELL_DIR` и пути `stampGateIndex` → `dist/web-shell`;
- `jvmApp/build.gradle.kts`: `nativeDistributions.outputBaseDir.set(rootProject.layout.projectDirectory.dir("dist/jvm"))` (в Compose 1.12 это `DirectoryProperty`) — образ попадает в `dist/jvm/main-release/app`;
- корневой `build.gradle.kts`: `build-jvm-app` читает образ из `dist/jvm`, архивы кладёт в `dist/packages`;
- `.gitignore`: вместо `dist/`, `dist-shell/`, `webApp.zip` остаётся одна строка `dist/`;
- добавить задачу `cleanDist` (`Delete`), чтобы чистить всё разом.

Fallback: если `outputBaseDir` поведёт себя нестабильно на какой-то ОС — оставить образ в `jvmApp/build/compose/...` и копировать его в `dist/jvm` при упаковке. Структура `dist/` от этого не меняется.

## 5. Задачи Gradle

### 5.1 `versionsJson`

Единственный источник версий для CI — типизированная задача (по образцу `GenerateCoverageBadge`, без `doLast`, чтобы жить с Configuration Cache). Пишет `dist/metadata/versions.json`:

```json
{
  "web": {
    "ui": "0.3.0-alpha.6",
    "technical": "0.3.1006",
    "tag": "web-v0.3.0-alpha.6",
    "stage": "alpha",
    "channel": "canary",
    "prerelease": true
  },
  "jvm": {
    "ui": "0.1.0-alpha.1",
    "technical": "0.1.1001",
    "tag": "jvm-v0.1.0-alpha.1",
    "stage": "alpha",
    "channel": "canary",
    "prerelease": true
  }
}
```

Правила маппинга зашиты в одном месте: `Alpha/Beta → canary`, `Rc → rc`, `Release → prod`; `prerelease = stage != Release`. YAML читает файл через `jq` и не парсит `build.gradle.kts`.

Отдельно учитывается ограничение `jpackage` на macOS: первый компонент `app-version` не может быть нулём. Для macOS-бандла передаётся `createMacPackageVersion()` (`0.x.y → 1.x.y`), при этом техническая версия приложения и версия в манифесте обновлений не меняются.

### 5.2 `packageWebApp` и `packageWebShell`

`Zip`-задачи с детерминированными параметрами (`isPreserveFileTimestamps = false`, `isReproducibleFileOrder = true`, исключения служебных файлов ОС — как в `build-jvm-app`):

- `packageWebApp` ← `:webApp:jsBrowserDevelopmentExecutableDistribution` → `dist/packages/web-<ui>.zip`;
- `packageWebShell` ← `:webShell:jsBrowserDevelopmentExecutableDistribution` и `:webShell:stampGateIndex` → `dist/packages/webshell-<ui>.zip`; контентный хэш гейта пишется в метаданные (`gateHash`), потому что он известен только после сборки дистрибутива.

### 5.3 `writeBuildMetadata`

Для каждого готового архива формирует `<имя>.metadata.json` и общий `SHA256SUMS-<компонент>.txt`:

```json
{
  "component": "jvm",
  "platform": "windows",
  "uiVersion": "0.1.0-alpha.1",
  "technicalVersion": "0.1.1001",
  "channel": "canary",
  "stage": "alpha",
  "commitSha": "<sha>",
  "date": "<ISO-8601>",
  "fileName": "helpdesk-jvm-windows-0.1.1001.zip",
  "size": 0,
  "sha256": "<hex>",
  "serverPath": "channels-v2/dev/canary/windows/windows-canary.zip"
}
```

Это готовый фрагмент для `update-manifest-v4.json`: деплой превращается в копипаст `version`, `hash`, `size`, `link`. Хэш считается по уже собранному zip, поэтому всегда совпадает с тем, что поедет на сервер.

### 5.4 Сохраняемые задачи

`build-web-app`, `build-web-shell`, `build-jvm-app` остаются публичными точками входа и получают новые пути вывода.

## 6. CI: workflow

| Файл | Триггер | Job'ы |
|---|---|---|
| `_checks.yml` | `workflow_call` | `checks`: `help --warning-mode=fail`, `detekt`, `allTests` |
| `checks.yml` | `pull_request → develop`, `push → develop` | вызов `_checks.yml` |
| `pr-main.yml` | `pull_request → main` | `checks` + `versions` → `cleanup` → `web`, `jvm-windows`, `jvm-macos` |
| `release.yml` | `push → main`, `workflow_dispatch` | `versions` → `tags` → `releases` (rc/release) → `assets` |
| `cleanup-caches.yml` | без изменений | — |

Детали:

- `pr-main.yml` — единственное место, где собираются дистрибутивы. Артефакты не публикуются, а лежат в прогоне PR: `retention-days: 2` (для alpha/beta это единственный источник сборки к выкладке).
- `cleanup` в `pr-main.yml` перед сборками удаляет через API все артефакты предыдущих прогонов с именами `web-*`, `webshell-*`, `jvm-windows-*`, `jvm-macos-*` (кроме текущего прогона). Это гарантирует, что бесплатные 500 МБ хранилища не забьются при нескольких сборках в день; заодно страховкой остаётся `retention-days: 2`.
- `cache-read-only: true` на PR; запись кэша — на push в `develop`/`main`.
- `concurrency` по ref с `cancel-in-progress: true`.
- `checks.yml` не собирает дистрибутивы: компиляцию покрывает `allTests`, лишние минуты не тратятся.
- `jvm-windows` — `windows-latest`, `jvm-macos` — `macos-15` (arm64); оба с `actions/setup-java` (temurin 21) и `gradle/actions/setup-gradle`.
- `pr-check.yml` удаляется.

Артефакты прогона PR:

| Имя | Содержимое |
|---|---|
| `web-<ui>` | `web-<ui>.zip`, `web-app.metadata.json`, `SHA256SUMS-web.txt`, `versions.json` |
| `webshell-<ui>` | `webshell-<ui>.zip`, `web-shell.metadata.json`, `SHA256SUMS-webshell.txt` |
| `jvm-windows-<tech>` | `helpdesk-jvm-windows-<tech>.zip`, `jvm-windows.metadata.json`, `SHA256SUMS-jvm-windows.txt`, `versions.json` |
| `jvm-macos-<tech>` | `helpdesk-jvm-macos-<tech>.zip`, `jvm-macos.metadata.json`, `SHA256SUMS-jvm-macos.txt`, `versions.json` |

## 7. Тэги и релизы

### 7.1 Матрица

| Стадия | Тэг | Релиз | Latest | Ассеты |
|---|---|---|---|---|
| `alpha` | да, аннотированный | нет | — | артефакты прогона PR |
| `beta` | да, аннотированный | нет | — | артефакты прогона PR |
| `rc` | да | да, черновик, `--prerelease` | нет | zip web, zip гейта, zip JVM |
| `release` | да | да, черновик → публикация | да | то же |

Тэг создаётся только для таргета, у которого изменилась версия: имя тэга содержит версию, поэтому проверка существования `web-v0.3.0-alpha.6` равносильна проверке «версия изменилась». Реализация — `git ls-remote --tags` или `gh api .../git/ref/tags/<tag>`.

Сообщение аннотированного тэга для alpha/beta — единственное место, где живут метаданные такого релиза:

```
web 0.3.0-alpha.6, техническая 0.3.1006, канал canary
```

### 7.2 Общий релиз нескольких таргетов

Тэги остаются покомпонентными: изменились web и JVM — появится `web-v0.3.0-alpha.7` и `jvm-v0.1.0-alpha.2`, два независимых тэга и два релиза. В описании каждого — строка «Совместная поставка с `<тэг другого компонента>`». Версия в тэге всегда одна и своя, поэтому `web-v*` не ловит чужие сборки.

Если позже понадобится «набор поставки», добавляется зонтичный тэг без ассетов `release-YYYY.MM.DD` с таблицей версий и ссылками на компонентные релизы. Схема это допускает, но в текущем объёме не реализуется.

### 7.3 `release.yml`

1. `versions` — `./gradlew versionsJson`, экспорт через `outputs`.
2. `tags` — для `web` и `jvm`: если тэга нет — `git tag -a` + `git push origin <tag>`; создаются на любой стадии, включая alpha/beta.
3. `drafts` — только для `rc`/`release`: `gh release create <tag> --draft --title … --notes-file -`; для `rc` дополнительно `--prerelease`. Существующий релиз не пересоздаётся (`gh release view`).
4. `assets-web`, `assets-jvm-windows`, `assets-jvm-macos` — сборка и `gh release upload <tag> <файлы> --clobber`. Создание релиза и заливка ассетов разнесены по job'ам, иначе windows и macos конкурируют за один `jvm-` релиз; job'ы заливки зависят от `drafts`, поэтому при alpha/beta пропускаются.

Требования к job'ам: `permissions: contents: write`, `fetch-depth: 0` (нужны тэги и история), `workflow_dispatch` с входом `mode: releases|tags` как ручной fallback (`tags` — только проставить отсутствующие тэги).

Предыдущий тэг компонента для заметок ищется как `git tag --list 'web-v*' --sort=-creatordate | head -1`; `--sort=-v:refname` неверно упорядочит `alpha.10` относительно `alpha.9`. Передаётся в `gh release create --notes-start-tag <prev>`.

### 7.4 Шаблон описания релиза

```
Компонент: JVM · Платформы: Windows (x64), macOS (arm64)
Версия: 0.1.0-alpha.1 · техническая 0.1.1001
Канал: canary
Сборки: helpdesk-jvm-windows-0.1.1001.zip, helpdesk-jvm-macos-0.1.1001.zip
SHA-256 и размер — в метаданных ассетов
Целевые пути: channels-v2/dev/canary/windows/windows-canary.zip, …/macos/macos-canary.zip
Порядок выкладки: страница публикации → загрузить ассеты → обновить манифест
Совместная поставка: <тэг другого компонента, если менялся>
```

## 8. Бейдж покрытия

Остаётся ручным. Правило для разработчика и агента: **перед созданием PR в любую ветку** выполнить `./gradlew generateCoverageBadge` и закоммитить обновлённый `coverage-badge.svg` вместе с изменениями. В CI задача не запускается, покрытие не измеряется, артефакты покрытия не публикуются.

Файл бейджа остаётся в корне репозитория (`coverage-badge.svg`, уже отслеживается git и настроен в `.gitattributes`) и не переезжает в `dist/`: иначе картинка в README перестанет рендериться на GitHub. Строка `![Coverage](coverage-badge.svg)` в README сохраняется.

Правило фиксируется в `AGENTS.md` (раздел конвенций) и в вики (`Конвенции.md`, `Быстрый-старт.md`).

## 9. Документация

- `README.md`: шапка содержит бейдж workflow и бейдж покрытия. Workflow-бейдж переезжает с `pr-check.yml` на `pr-main.yml`, бейдж покрытия остаётся на месте:

```markdown
[![PR to main](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-main.yml/badge.svg)](https://github.com/oniksen/helpdesk-platform/actions/workflows/pr-main.yml)
[![License](https://img.shields.io/badge/License-LPMTI%20Proprietary-lightgrey.svg)](LICENSE)
![Coverage](coverage-badge.svg)
```

Если после реализации появится смысл показать ещё и релизный контур, добавляется третий workflow-бейдж на `release.yml`; обязательным он не является.

- `AGENTS.md`:
  - раздел CI/CD переписать под новую структуру workflow, папку `dist/`, правило тэгов;
  - в конвенциях — обязательный `generateCoverageBadge` перед PR;
  - строка 77: убрать ссылку «по образцу `docs/plans/tasks-view-and-management.md`» (файл удаляется), оставить описание структуры плана со ссылкой на `.github/ISSUE_TEMPLATE`.
- Вики (`helpdesk-platform.wiki`): `CI-CD.md` — пайплайн, тэги, где лежат сборки; `Быстрый-старт.md` — папка `dist/`; `Конвенции.md` — правило бейджа.
- Удалить устаревшие и реализованные планы: `docs/plans/jvm-update-v4-integration.md`, `docs/plans/tasks-view-and-management.md`.

## 10. Процесс исполнения (обязательный)

Работа ведётся через список задач в окружении агента, а не «по памяти».

1. Перед началом работы агент заводит в своём окружении список задач из раздела 11 этого документа, сохраняя нумерацию и критерии приёмки.
2. Перед каждой задачей список перечитывается, чтобы состояние было актуальным — особенно при возобновлении прерванной сессии.
3. После каждой выполненной задачи список обновляется сразу: задача отмечается выполненной, фиксируется фактический результат и отклонения от дизайна, следующая задача переводится в работу.
4. Прогресс отмечается **после каждой задачи**, а не одним пакетом в конце. Накапливать невыполненные отметки запрещено.
5. Если задача оказалась блокирующей или изменился дизайн — сначала обновляется документ и список, затем работа продолжается.

## 11. Список задач

| № | Задача | Критерий готовности |
|---|---|---|
| T1 | Ввести `dist/` и перевести выходы `webApp`, `webShell`, `jvmApp` | Сборки всех трёх таргетов появляются только внутри `dist/`; `.gitignore` содержит одну строку `dist/` |
| T2 | Задача `versionsJson` | `./gradlew versionsJson` создаёт `dist/metadata/versions.json` с полями по разделу 5.1; задача совместима с Configuration Cache |
| T3 | Задачи `packageWebApp`, `packageWebShell` | На выходе детерминированные zip в `dist/packages` с версией/хэшем в имени |
| T4 | Задача `writeBuildMetadata` | Для каждого архива создаётся `*.metadata.json` и `SHA256SUMS-*.txt`; `sha256` совпадает с хэшем файла |
| T5 | `_checks.yml` и `checks.yml` | Проверки запускаются на PR и push в `develop`, дистрибутивы не собираются |
| T6 | `pr-main.yml` | Сборки стартуют только после зелёных проверок и только на PR в `main`; артефакты с retention 2 дня, перед сборкой работает очистка артефактов прошлых прогонов |
| T7 | `release.yml` | alpha/beta получают тэг, rc/release — черновик с ассетами; повторный прогон не создаёт дублей |
| T8 | Удаление `pr-check.yml` и обновление бейджей README | В шапке отображаются бейдж workflow `pr-main.yml` и бейдж покрытия `coverage-badge.svg`; битых ссылок нет |
| T9 | Удаление устаревших планов и правка ссылки в `AGENTS.md` | `docs/plans/` пуст, в `AGENTS.md` нет ссылок на удалённые файлы |
| T10 | Обновление `AGENTS.md` | Раздел CI/CD соответствует реализации; правило бейджа добавлено |
| T11 | Обновление вики | Три страницы описывают новый пайплайн, `dist/` и ручной бейдж |
| T12 | Проверка | Выполнены команды из раздела 12, оба контура CI зелёные |

## 12. Проверка

Локально (Windows-хост):

```bash
./gradlew --warning-mode=fail help
./gradlew detekt
./gradlew allTests
./gradlew build-web-app build-web-shell
./gradlew build-jvm-app
./gradlew versionsJson
```

Ожидаемо: `dist/` содержит `web/`, `web-shell/`, `jvm/`, `packages/` с четырьмя архивами и метаданными; в `packages` — `SHA256SUMS-*`; `git status` не показывает файлов внутри `dist/`.

В CI:

- прогон `checks.yml` на push в `develop` зелёный;
- прогон `pr-main.yml` на PR в `main` даёт четыре артефакта;
- после merge в `main` `release.yml` создаёт тэги по изменившимся компонентам (для alpha/beta — только тэги).

## 13. Риски и ограничения

| Риск | Митигация |
|---|---|
| macOS собирается только arm64 | Соответствует решению; при появлении Intel-сборок расширяем манифест до `macos-x64`/`macos-arm64` отдельной задачей |
| JVM-архив со встроенным JDK ~150–250 МБ × 2 платформы на каждый PR | Перед сборкой артефакты прошлых прогонов удаляются, retention 2 дня; при перерасходе квоты снизить retention до 1 дня |
| `outputBaseDir` может вести себя иначе на другой ОС | Fallback: образ остаётся в `jvmApp/build/compose`, копируется в `dist/jvm` при упаковке |
| `release.yml` требует `contents: write` | Права выдаются на уровне workflow, токен не покидает job |
| Изменения настроек репозитория (required checks, тэг-защита) не версионируются | Шаг вручную после merge: добавить `pr-main` job'ы в required checks ветки `main` |
| Правка тэгов/релизов вручную может разойтись с манифестом | Метаданные сборки генерируются из одного источника и попадают в описание релиза |
