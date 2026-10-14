# План интеграции JVM-таргета с системой обновлений по каналам (v4)

## 🚀 Эпик: Интеграция JVM-таргета с системой обновлений по каналам (v4)

### 🎯 Глобальная цель
Реализовать интеграцию JVM-таргета (Windows, macOS) с единой системой распространения и обновлений на основе манифеста `update-manifest-v4.json`. Обеспечить единый формат манифеста для всех платформ, работу с каналами Prod/Canary/RC, загрузку первичного установщика по единой ссылке на страницу и применение обновления через zip-бандл для JVM.

### 🚫 Что НЕ входит в этот Эпик (Out of Scope)
- Автоматизация CD. Обновления поставляются через специальную страницу на сервере с ручным выкладыванием сборок.
- Offline-first. Упор на актуальные данные на сервере.
- Принудительная проверка обновления и принудительная установка обновления (только проверка при старте).
- Полноценный перевод JS-архитектуры на JVM без необходимости — только JVM-специфика через expect/actual.

### 🏗 Архитектурные константы и подходы
- Манифест v4: `schema_version`, `channels { prod, dev { canary, rc, last_rejection } }`, для каждой платформы `{ version, patch_note[], tags[], date, link, hash, size }`.
- URL обновления для JVM берется из поля `link` манифеста.
- Каналы: Prod, Canary, RC. Выбор по `channels.json` (фолбэк на Prod). Для запроса манифеста ничего кроме ссылки не требуется.
- Обновление из zip-бандла; первичный установщик `.exe`/`.dmg`. Без отдельного launcher.
- Повторить пайплайн обновления из референса `helpdesk-multiplatform`.
- Версионирование JVM-таргета не едино с web. Использовать `AppVersion` и `normalizeFromTechnical()`.
- Максимально переиспользовать `core/update/api` и `core/update/impl`. JVM-специфика через expect/actual.
- Проверка обновления только при старте.
- Новый модуль `:jvmApp` с собственной точкой входа `Main.kt` (DI предположительно отличается от web).

### 🔗 Полезные ссылки
- **Референс:** `C:\Users\admin\Documents\projects\helpdesk-multiplatform` (система обновлений JVM, пайплайн обновления).
- **Манифест v4:** https://helpdesk.lpmti.ru/helpdesk-app/v2/update-manifest-v4.json
- **Файл каналов:** https://helpdesk.lpmti.ru/helpdesk-app/v2/users-settings/channels.json
- **Шаблоны:** `.github/ISSUE_TEMPLATE/epic.md`, `.github/ISSUE_TEMPLATE/user-story.md`, `.github/ISSUE_TEMPLATE/task.md`

---

## US-1 — Модуль `:jvmApp`: конфигурация и сборка

### 🎯 Формулировка
- **Роль:** Разработчик.
- **Потребность:** Создать новый JVM-таргетный модуль с собственной точкой входа и задачами ручной сборки артефактов.
- **Ценность:** Разделение точек входа (web vs jvm) и возможность собирать установщики и update-бандл.

### 📋 Критерии приёмки
- [ ] В `settings.gradle.kts` добавлен `:jvmApp`.
- [ ] Модуль `:jvmApp` с минимальной KMP-конфигурацией (только jvm).
- [ ] Есть `src/jvmMain/kotlin/.../Main.kt` с минимальным запуском.
- [ ] Добавлены Gradle-задачи `packageWindowsExe`, `packageMacDmg`, `packageJvmUpdateBundle`.
- [ ] Модуль собирается, detekt проходит.
- [ ] Пакеты `org.lpmti.helpdeskplatform.*`.

**Связь:** US-1 → Эпик

### TASK-1.1: Создать структуру модуля `:jvmApp` и добавить в settings.gradle.kts
**Родительская US:** US-1
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Создать новый модуль `:jvmApp` с минимальной KMP-конфигурацией (только jvm) и зарегистрировать его в `settings.gradle.kts`, чтобы появилась отдельная точка входа для JVM-таргета.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `settings.gradle.kts`, каталог `jvmApp/`, каркас `src/jvmMain/kotlin`.
- **Ограничения:** только jvm-таргет; пакеты `org.lpmti.helpdeskplatform.*`; структура каталогов по образцу `webApp`.
- **Внимание:** не сломать сборку существующих модулей.

#### 🧪 Критерии приёмки
- [ ] `:jvmApp` добавлен в `settings.gradle.kts`.
- [ ] Создан каркас `jvmApp/src/jvmMain/kotlin`.
- [ ] `./gradlew help --warning-mode=fail` проходит.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `settings.gradle.kts`, `webApp/build.gradle.kts`

**Связь:** TASK-1.1 → US-1

### TASK-1.2: Настроить jvmApp/build.gradle.kts (jvm-таргет)
**Родительская US:** US-1
- **Приоритет:** P1
- **SP:** 1
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Описать jvm-таргет и зависимости модуля (Compose Multiplatform, Koin и используемые core-модули), чтобы модуль компилировался.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `jvmApp/build.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts` (BuildKonfig).
- **Ограничения:** переиспользовать существующие плагины и версии из `libs.versions.toml`; не дублировать версии.
- **Внимание:** согласовать зависимости с `webApp`, но без web-специфичных библиотек.

#### 🧪 Критерии приёмки
- [ ] jvm-таргет сконфигурирован.
- [ ] Подключены Compose Multiplatform, Koin и нужные core-модули.
- [ ] Модуль компилируется.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `webApp/build.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts`

**Связь:** TASK-1.2 → US-1

### TASK-1.3: Создать минимальный Main.kt в jvmApp
**Родительская US:** US-1
- **Приоритет:** P1
- **SP:** 0.5
- **Размер:** XS
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Создать минимальную точку входа `Main.kt`, запускающую приложение, чтобы модуль можно было собрать и запустить.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/Main.kt`.
- **Ограничения:** только минимальный запуск, без логики обновлений (она в US-5).
- **Внимание:** корректная настройка точки входа.

#### 🧪 Критерии приёмки
- [ ] Создан `Main.kt` с минимальным запуском.
- [ ] Модуль запускается и завершается без ошибок.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `webApp/src/webMain/kotlin/org/lpmti/helpdeskplatform/main.kt`

**Связь:** TASK-1.3 → US-1

### TASK-1.4: Добавить сборочные Gradle-задачи (packageWindowsExe, packageMacDmg, packageJvmUpdateBundle)
**Родительская US:** US-1
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Добавить в `:jvmApp` задачи ручной сборки артефактов: установщики `.exe`/`.dmg` и zip-бандл для in-place обновления.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `jvmApp/build.gradle.kts`.
- **Ограничения:** `packageJvmUpdateBundle` формирует zip с нормализованными путями, пригодный для in-place замены (согласуется с US-3); установщики формируются по мере наличия инструментов на сборочной машине.
- **Внимание:** предсказуемые пути выходных артефактов.

#### 🧪 Критерии приёмки
- [ ] Задачи добавлены в `jvmApp/build.gradle.kts`.
- [ ] `packageJvmUpdateBundle` формирует корректный zip для in-place замены.
- [ ] Установщики формируются (`.exe`/`.dmg`) по мере наличия инструментов.
- [ ] Артефакты размещаются в предсказуемых путях.

#### 🔗 Ссылки и артефакты
- `jvmApp/build.gradle.kts`, референс `helpdesk-multiplatform`

**Связь:** TASK-1.4 → US-1

---

## US-2 — Резолв манифеста v4 и определение канала для JVM

### 🎯 Формулировка
- **Роль:** Разработчик.
- **Потребность:** Обеспечить выбор релиза из манифеста v4 по каналу и платформе (macos/windows) и определение активного канала по `channels.json` с фолбэком на Prod.
- **Ценность:** Единый механизм доступа к данным манифеста и каналов для всех платформ.

### 📋 Критерии приёмки
- [ ] Выбор таргета по каналу + платформе корректен для macos/windows.
- [ ] Обработка неопубликованных таргетов (null).
- [ ] `CurrentChannelProviderImpl` для JVM определяет канал, фолбэк на Prod при ошибке/отсутствии.
- [ ] Unit-тесты на резолв и каналы.
- [ ] Обратно совместимо с web.

**Связь:** US-2 → Эпик

### TASK-2.1: Доработать резолв таргета под macos/windows в core/update/impl
**Родительская US:** US-2
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Расширить резолв таргета манифеста v4, чтобы выбирать сборку по платформе (macos/windows), а не только web, сохранив обратную совместимость с web.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl` (`domain/ManifestTargetForChannel.kt`, доменные модели v2 `Release`/`TargetData`).
- **Ограничения:** текущий `targetFor(channel)` возвращает только `release.web`; добавить выбор платформы без изменения web-поведения. Неопубликованный таргет остаётся `null`.
- **Внимание:** не сломать существующие web-сценарии.

#### 🧪 Критерии приёмки
- [ ] Резолв возвращает таргет macos/windows по каналу.
- [ ] Неопубликованный таргет обрабатывается как `null`.
- [ ] Web-поведение не изменилось.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/commonMain/kotlin/domain/ManifestTargetForChannel.kt`, `.../domain/models/manifest/v2/Release.kt`

**Связь:** TASK-2.1 → US-2

### TASK-2.2: Реализовать CurrentChannelProviderImpl.jvm с фолбэком на Prod
**Родительская US:** US-2
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Реализовать JVM-версию провайдера текущего канала: чтение `channels.json` и фолбэк на Prod при ошибке или отсутствии данных.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmMain/kotlin/CurrentChannelProviderImpl.jvm.kt` (сейчас заглушка `provide() = null`), `core/update/impl` (`CurrentChannelProviderImpl`, `ChannelStorage`, `ChannelsFileMapper`).
- **Ограничения:** переиспользовать существующие контракты и мапперы; логика выбора канала не дублируется.
- **Внимание:** сетевые ошибки не должны валить запуск — фолбэк на Prod.

#### 🧪 Критерии приёмки
- [ ] `CurrentChannelProviderImpl.jvm` возвращает активный канал.
- [ ] При ошибке/отсутствии данных используется Prod.
- [ ] Реализация проходит detekt.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/jvmMain/kotlin/CurrentChannelProviderImpl.jvm.kt`, `core/update/impl/src/commonMain/kotlin/CurrentChannelProviderImpl.kt`, `.../data/mapper/ChannelsFileMapper.kt`

**Связь:** TASK-2.2 → US-2

### TASK-2.3: Unit-тесты резолва и каналов для JVM
**Родительская US:** US-2
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** test, windows, macos

#### 📝 Описание задачи
Покрыть тестами выбор релиза под macos/windows по каналам Prod/Canary/RC и определение текущего канала с фолбэком на Prod.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/commonTest`, `core/update/impl/src/jvmTest`.
- **Ограничения:** имена тестов — читаемые английские фразы в backticks; однотипные случаи — таблицей через `forEach`; сообщения ассертов на русском.
- **Внимание:** покрыть негативные кейсы (непубликованный таргет, ошибка чтения каналов).

#### 🧪 Критерии приёмки
- [ ] Тесты резолва macos/windows по Prod/Canary/RC.
- [ ] Тест фолбэка на Prod.
- [ ] `./gradlew allTests` проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/commonTest/kotlin/domain/ManifestTargetForChannelTest.kt`, `.../ChannelSelectionTest.kt`

**Связь:** TASK-2.3 → US-2

---

## US-3 — JVM AppUpdater (загрузка zip-бандла, проверка, распаковка, применение)

### 🎯 Формулировка
- **Роль:** Разработчик.
- **Потребность:** Реализовать `AppUpdater` для JVM по пайплайну из референса (zip-бандл, нормализация путей, in-place замена). Без отдельного launcher.
- **Ценность:** Возможность обновления JVM-приложения.

### 📋 Критерии приёмки
- [ ] `createAppUpdater` в `jvmMain` возвращает рабочую реализацию.
- [ ] `downloadAndUnpack` работает корректно.
- [ ] Нормализация путей как в JS-версии.
- [ ] Проверка hash/size при наличии.
- [ ] Кэширование + `clearCache`.
- [ ] Применение обновления in-place.
- [ ] Unit-тесты (фокус на ключевом пайплайне).

**Связь:** US-3 → Эпик

### TASK-3.1: Реализовать AppUpdater.jvm (download/unpack/normalize/cache/clear)
**Родительская US:** US-3
- **Приоритет:** P1
- **SP:** 5
- **Размер:** M
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Реализовать JVM-версию `AppUpdater`: скачивание zip-бандла, распаковку, нормализацию путей, кэширование и очистку кэша.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmMain/kotlin/AppUpdaterFactory.jvm.kt` (сейчас `TODO`), контракт `core/update/api/AppUpdater.kt`.
- **Ограничения:** переиспользовать интерфейс `AppUpdater`; нормализация путей как в JS-реализации; при наличии проверять `hash`/`size` из манифеста.
- **Внимание:** корректная работа с архивами и файловой системой, освобождение ресурсов.

#### 🧪 Критерии приёмки
- [ ] `createAppUpdater` возвращает рабочую реализацию.
- [ ] `downloadAndUnpack` возвращает карту файлов сборки.
- [ ] Пути нормализованы как в JS-версии.
- [ ] `hash`/`size` проверяются при наличии.
- [ ] `getCachedBuild`/`clearCache` работают.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/jvmMain/kotlin/AppUpdaterFactory.jvm.kt`, `core/update/api/src/commonMain/kotlin/AppUpdater.kt`, `core/update/impl/src/jsMain/kotlin/AppUpdaterImpl.kt`

**Связь:** TASK-3.1 → US-3

### TASK-3.2: Реализовать in-place применение обновления для JVM
**Родительская US:** US-3
- **Приоритет:** P1
- **SP:** 3
- **Размер:** M
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Реализовать применение распакованной сборки поверх текущей установки (in-place) без отдельного launcher.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmMain`.
- **Ограничения:** опираться на структуру zip-бандла из TASK-1.4; поведение согласовать с пайплайном референса; не вводить отдельный launcher.
- **Внимание:** безопасность замены файлов работающего приложения, обработка ошибок ввода-вывода.

#### 🧪 Критерии приёмки
- [ ] Распакованная сборка применяется in-place.
- [ ] Ошибки ввода-вывода обрабатываются предсказуемо.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/jvmMain`, референс `helpdesk-multiplatform`

**Связь:** TASK-3.2 → US-3

### TASK-3.3: Unit-тесты AppUpdater.jvm
**Родительская US:** US-3
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** test, windows, macos

#### 📝 Описание задачи
Покрыть тестами ключевой пайплайн: распаковку, нормализацию путей, кэширование и проверку целостности.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmTest`.
- **Ограничения:** имена тестов — читаемые английские фразы в backticks; однотипные случаи — таблицей; сообщения ассертов на русском.
- **Внимание:** не тянуть реальную сеть в тесты — использовать фикстуры/подмену источников.

#### 🧪 Критерии приёмки
- [ ] Покрыт сценарий распаковки и нормализации путей.
- [ ] Покрыт сценарий кэширования/очистки.
- [ ] `./gradlew allTests` проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/jsMain/kotlin/AppUpdaterImpl.kt` (как референс поведения)

**Связь:** TASK-3.3 → US-3

---

## US-4 — AppInstallStateImpl.jvm и AppVersionProvider для JVM

### 🎯 Формулировка
- **Роль:** Разработчик.
- **Потребность:** Реализовать `hasNewVersion()` для JVM при старте и определение текущей версии/платформы.
- **Ценность:** Единый контракт `AppInstallState` и корректное сравнение версий.

### 📋 Критерии приёмки
- [ ] `hasNewVersion()` возвращает `UpToDate`/`UpdateAvailable`/`ManifestError`.
- [ ] Определение текущей платформы (Windows/macOS).
- [ ] Сравнение версий через `AppVersion.compareTo`.
- [ ] `AppVersionProvider` использует `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM`.
- [ ] Unit-тесты.

**Связь:** US-4 → Эпик

### TASK-4.1: Реализовать AppInstallStateImpl.jvm и доработать AppVersionProvider
**Родительская US:** US-4
- **Приоритет:** P1
- **SP:** 3
- **Размер:** M
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Реализовать JVM-версию `AppInstallStateImpl.hasNewVersion()` с определением платформы и сравнением версий, а также убедиться, что `AppVersionProvider` отдаёт техническую версию JVM.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmMain/kotlin/AppInstallStateImpl.jvm.kt` (сейчас `TODO`), `core/update/api/src/jvmMain/kotlin/AppInstallState.jvm.kt` (заглушки), `AppVersionProviderImpl.jvm.kt` (уже использует `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM` и `normalizeFromTechnical()`), `core/update/api/AppVersion.kt`, `UpdateDecision.kt`.
- **Ограничения:** вернуться к типам `UpdateDecision`; на текущем этапе метод глушит ошибки (помечен `@TemporaryArchitectureStab`).
- **Внимание:** корректность `compareTo` версий и определение платформы (OS/arch).

#### 🧪 Критерии приёмки
- [ ] `hasNewVersion()` возвращает `UpToDate`/`UpdateAvailable`/`ManifestError`.
- [ ] Платформа определяется (Windows/macOS).
- [ ] Версии сравниваются через `AppVersion.compareTo`.
- [ ] `AppVersionProvider` отдаёт `PROJECT_TECHNICAL_VERSION_JVM`.
- [ ] detekt проходит.

#### 🔗 Ссылки и артефакты
- `core/update/impl/src/jvmMain/kotlin/AppInstallStateImpl.jvm.kt`, `core/update/api/src/commonMain/kotlin/AppVersion.kt`, `core/update/api/src/commonMain/kotlin/UpdateDecision.kt`

**Связь:** TASK-4.1 → US-4

### TASK-4.2: Unit-тесты hasNewVersion() для JVM
**Родительская US:** US-4
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** test, windows, macos

#### 📝 Описание задачи
Покрыть тестами решение о наличии новой версии для JVM: случаи `UpToDate`, `UpdateAvailable`, `ManifestError`.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `core/update/impl/src/jvmTest`, `core/update/api/src/commonTest`.
- **Ограничения:** имена тестов — читаемые английские фразы в backticks; однотипные случаи — таблицей; сообщения ассертов на русском.
- **Внимание:** покрыть сравнение версий и определение платформы.

#### 🧪 Критерии приёмки
- [ ] Тесты на `UpToDate`/`UpdateAvailable`/`ManifestError`.
- [ ] Проверено сравнение версий через `AppVersion.compareTo`.
- [ ] `./gradlew allTests` проходит.

#### 🔗 Ссылки и артефакты
- `core/update/api/src/commonTest/kotlin/AppVersionTest.kt`

**Связь:** TASK-4.2 → US-4

---

## US-5 — Интеграция проверки обновления при старте в jvmApp + DI

### 🎯 Формулировка
- **Роль:** Разработчик.
- **Потребность:** Подключить систему обновлений в `jvmApp`: DI + запуск проверки только при старте с минимальным UX.
- **Ценность:** Рабочий цикл проверки при старте приложения.

### 📋 Критерии приёмки
- [ ] Koin DI в `jvmApp` для update-компонентов с JVM-реализациями.
- [ ] `Main.kt` запускает проверку при старте однократно.
- [ ] Обработка состояний (`UpToDate`/`UpdateAvailable`/`ManifestError`).
- [ ] Интеграция не затрагивает `webShell`/`webApp`.
- [ ] Сборка успешна, detekt проходит.

**Связь:** US-5 → Эпик

### TASK-5.1: Настроить Koin DI в jvmApp для update-компонентов
**Родительская US:** US-5
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Настроить Koin DI в модуле `:jvmApp` для компонентов обновления с регистрацией JVM-реализаций. DI для `jvmApp` отличается от web.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** модуль `:jvmApp`, `core/update/api`, `core/update/impl`, `core/di`.
- **Ограничения:** переиспользовать интерфейсы из `core/update/api`; зарегистрировать JVM-реализации (`AppUpdater`, `AppInstallState`, `CurrentChannelProvider`, `AppVersionProvider`, `UpdateDataSource`); отдельный DI-модуль для `jvmApp`, не конфликтующий с web.
- **Внимание:** отсутствие циклов зависимостей.

#### 🧪 Критерии приёмки
- [ ] Создан/подключён DI-модуль для `jvmApp` с регистрацией update-компонентов.
- [ ] Зарегистрированы JVM-реализации (включая `createAppUpdater`).
- [ ] Нет циклов зависимостей.
- [ ] Модуль компилируется, detekt проходит.

#### 🔗 Ссылки и артефакты
- DI в `webApp`: `FullAppDefinition.kt`, модули `core/di/infra`

**Связь:** TASK-5.1 → US-5

### TASK-5.2: Интегрировать проверку обновления при старте в Main.kt jvmApp
**Родительская US:** US-5
- **Приоритет:** P1
- **SP:** 2
- **Размер:** S
- **Лейблы:** feature, windows, macos

#### 📝 Описание задачи
Инициализировать DI и запустить проверку обновления только при старте приложения (однократно). Обработать состояния: `UpToDate` — продолжить запуск; `UpdateAvailable(url)` — инициировать загрузку, распаковку и применение через `AppUpdater`; `ManifestError` — залогировать и не блокировать запуск.

#### 🛠 Технический контекст и ограничения
- **Связанные компоненты:** `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/Main.kt`, `AppUpdater`, `AppInstallState`, `UpdateDecision`.
- **Ограничения:** проверка только при старте (не принудительная), строго однократно; не затрагивать `webShell`/`webApp`.
- **Внимание:** корректная обработка корутин и ошибок.

#### 🧪 Критерии приёмки
- [ ] DI инициализируется в `Main.kt`.
- [ ] Проверка обновления запускается однократно при старте.
- [ ] Корректная обработка всех `UpdateDecision`.
- [ ] При `UpdateAvailable` инициируется минимальный рабочий сценарий применения обновления через `AppUpdater`.
- [ ] Сборка `:jvmApp` успешна, detekt проходит.

#### 🔗 Ссылки и артефакты
- `AppUpdater.kt`, `UpdateDecision.kt`, `AppInstallState.kt`

**Связь:** TASK-5.2 → US-5

---

## Сводка приоритетов/SP
- Всего US: 5
- Всего Tasks: 14
- Основные приоритеты: P1
- SP диапазон: 0.5–5
- Размеры: XS–M
