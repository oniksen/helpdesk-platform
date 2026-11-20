# Дизайн: `AppUpdateManager` — упрощение системы обновлений JVM-таргета

Дата: 2026-11-19
Статус: утверждён, реализация выполнена
Скоуп: `core/update/api`, `core/update/impl`, `jvmApp`
Развивает: `docs/superpowers/specs/2026-11-17-jvm-update-manager-port-design.md` (перенос пайплайна), `docs/plans/jvm-update-v4-integration.md` (исходная декомпозиция)

---

## 1. Intent и why

Сейчас в JVM-обновлениях существуют два параллельных набора контрактов:

- старый `domain.UpdateManager` + `domain.UpdateManagerImpl` с полным пайплайном установки;
- новый `newvision` (`AppUpdateManager`, `CurrentChannelProvider`, `UpdateChannel`, `UpdateStateNewVision`), начатый как упрощённое «видение».

Новый взгляд правильный по направлению: вызывающему коду нужны только две операции — проверить обновление и запустить установку. Но текущая реализация нового взгляда — это заготовка: она дублирует старые контракты, смешивает пакеты, заново пишет HTTP-слой и не содержит пайплайна. При этом старый пайплайн `performUpdate` уже проверен в бою и должен быть перенесён целиком.

Цель: оставить **один** публичный вход `AppUpdateManager` с двумя методами и состояниями, перенести в него проверенный пайплайн и подготовить полное состояние каналов для будущего UI.

Ценность:

- вызывающий код (`jvmApp`, будущий экран обновления) больше не знает про манифест, каналы, платформу и таргеты;
- одно место изменения при добавлении этапа обновления или нового состояния;
- состояние уже пригодно для UI: прогресс по этапам, ошибки, доступные каналы, выбранный канал, канал, из которого пришло обновление;
- удаляется дублирование и мёртвые ветки.

---

## 2. Goals / Non-goals / Anti-goals

### Goals

- Один публичный контракт `AppUpdateManager`:
  - `checkUpdate(email: String? = null)` — без возврата состояния;
  - `startUpdate()`.
- Два основных `StateFlow` (`checkUpdateState`, `updateState`) + вспомогательный `channelAccessState` с полным состоянием каналов.
- Проверка обновления: `channels.json` + манифест v4 + выбор канала + выбор платформенного таргета + сравнение версий.
- Fallback на `PROD` при отсутствии/ошибке данных о каналах.
- `Unavailable(reason)` для «нет сборки под платформу/канал» и «платформа не поддерживается».
- Полный перенос `performUpdate` (`UpdateManagerImpl` → `UpdateInstaller`) без изменения поведения.
- Замена старого решения: удалить `UpdateManager`, `UpdateManagerImpl`, старый `UpdateState`, `newvision`-дубли; обновить DI и `main.kt`.
- Подготовить контракт для будущего UI, не реализуя сам UI.
- Покрыть новое поведение тестами.

### Non-goals

- Экран обновления и экран переключения каналов (только контракт и состояние).
- JVM-авторизация: email пока может быть `null`.
- Реальные `VersionActions` (`markAsStable`, `reject`, `publish`, `devChannelAccess`) — вне нового контракта.
- Изменение `webShell`, `webApp`, `features/update`, `AppUpdater`, `UpdateDecision`.
- Linux как целевая ОС.
- Изменение формата манифеста v4 или серверных файлов.
- Кэширование манифеста.
- Автоматизация CD.

### Anti-goals

- Два параллельных контракта после интеграции.
- Изменение проверенного пайплайна: порядок этапов, `.bak`-rename, пропуск `runtime/`, `codesign`, отсчёт 5 секунд.
- Потеря терминального состояния проверки (сброс в `Idle`).
- UI-типы (Compose) в доменных состояниях.
- Падение приложения на неподдерживаемой ОС из-за DI.
- Скрытые сетевые запросы при `email = null` (файл каналов не запрашивается).

---

## 3. Constraints

- Манифест v4: `https://helpdesk.lpmti.ru/helpdesk-app/v2/update-manifest-v4.json`.
- Файл каналов: `https://helpdesk.lpmti.ru/helpdesk-app/v2/users-settings/channels.json`.
- Публикация таргета = непустой `hash` и положительный `size` (строгий маппер v4).
- JVM-платформы: `WINDOWS`, `MACOS`; `currentUpdatePlatform()` возвращает `null` на остальных ОС.
- Текущая версия: `AppVersionProvider` → `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM`.
- Канонические имена каналов уже есть в common-объекте `domain.UpdateChannel`; новый JVM-enum обязан ссылаться на них, а не дублировать строки.
- Koin DI; `AppUpdateManager` — синглтон.
- Kotlin 2.4.20, Ktor 3.5.2, kotlinx-coroutines.
- Комментарии — на русском; имена тестов — английские фразы в backticks; detekt.
- Ветка сейчас не компилируется: `ChannelsFileMapper` передаёт `ChannelAccess?` в non-null `ChannelsFile.default`. Это обязательный первый фикс.
- Реализация начинается только после утверждения дизайна.

---

## 4. Принятые решения

Зафиксировано по итогам обсуждения:

1. `startUpdate` — обычная `fun`, fire-and-forget. После запуска установку нельзя отменить со стороны вызывающего; она выполняется в собственном scope менеджера. Это сохраняет текущее поведение и защищает установку от отмены UI-корутиной.
2. Платформа резолвится внутри менеджера (`currentUpdatePlatform()`), а не передаётся строкой. Для тестов платформа инъектируется в конструктор.
3. Fallback каналов: `selected` (если валиден) → первый известный из `allowed` → `PROD`. Ошибка `channels.json` → эффективный канал `PROD` (данные о каналах могут остаться для отображения, см. §7.3).
4. Полное состояние каналов сохраняется для будущего UI переключения каналов: `allowed`, `selected`, эффективный `current`, источник данных. Добавляется `Unavailable(reason)`.
5. Структура: `AppUpdateManagerImpl` + внутренний `UpdateInstaller`.
6. `checkUpdate` не возвращает состояние: результат только публикуется в `checkUpdateState`. Контракт: терминальное состояние публикуется **до** возврата из `checkUpdate`, поэтому вызывающий после `await`/вызова читает `checkUpdateState.value`.
7. Ведение задач: при реализации агент использует скилл/инструмент ведения TODO-списка (`todowrite`). Каждый шаг из §10 заводится отдельным пунктом; статус обновляется **сразу после завершения задачи** (`completed` сразу, а не в конце), и актуальный список отображается в UI агента после каждого шага; пункт закрывается только после компиляции/тестов.

---

## 5. Approaches

### Рассмотренные варианты

1. **Оставить `newvision` как есть и дописать TODO.** Отвергнуто: дублирует `UpdateDataSource`, теряет причины ошибок, не содержит пайплайна, смешивает пакеты.
2. **Оставить старый `UpdateManager` и сделать фасад.** Отвергнуто: два контракта и два состояния, упрощения нет.
3. **Один класс `AppUpdateManagerImpl` со всем пайплайном.** Работоспособно, но класс вырастает до ~350 строк и смешивает проверку/каналы с файловой установкой.
4. **`AppUpdateManagerImpl` + внутренний `UpdateInstaller` (выбран).** Публичная поверхность остаётся в два метода; проверка и каналы отделены от установки; пайплайн тестируется отдельно; поведение переносится дословно.
5. **Один общий `StateFlow` на всё.** Отвергнуто: смешивает «доступность обновления» и «ход установки», UI сложнее.
6. **`suspend fun startUpdate()`.** Отвергнуто: отмена корутины вызывающего может прервать установку посередине.

### Выбранный

Вариант 4 + два основных `StateFlow` + вспомогательный `channelAccessState`. `startUpdate` — fire-and-forget. Пайплайн переносится в `UpdateInstaller` дословно.

---

## 6. Текущее состояние (факты)

### 6.1 Что есть сейчас

- `core/update/api/jvmMain/domain/UpdateManager.kt` — старый контракт:
  `updateState`, `loadManifest`, `hasNewVersion` (TODO), `performUpdate`, `markAsStable`, `reject`, `publish`, `devChannelAccess`.
- `core/update/api/jvmMain/domain/model/UpdateState.kt` — старый `sealed class UpdateState` + `CurrentStageState`.
- `core/update/api/commonMain/kotlin/state/CheckUpdateState.kt` — `Idle/Loading/IsUpToDate/Available/Error`; используется только новым `newvision`, лежит в common.
- `core/update/api/jvmMain/kotlin/newvision/*`:
  - `AppUpdateManager.kt` (`package domain`), `CurrentChannelProvider.kt` (`package newvision`), `UpdateChannel.kt` (`package newvision`), `UpdateStateNewVision.kt` (`package newvision`).
- `core/update/impl/jvmMain/kotlin/newvision/AppUpdateManagerImpl.kt` — заготовка: дублирует HTTP-запросы, содержит `TODO`, `finally { Idle }`, `currentPlatform: String`, `KtorClient`/`host`.
- `core/update/impl/jvmMain/kotlin/newvision/CurrentChannelProviderImpl.kt` — in-memory `save/get`.
- `core/update/impl/jvmMain/kotlin/domain/UpdateManagerImpl.kt` — полный проверенный пайплайн: загрузка → верификация (размер + SHA-256) → целостность zip → распаковка → замена файлов (`.bak`, пропуск `runtime/`, exec-бит) → очистка → `codesign` (macOS) → отсчёт 5 с → рестарт.
- Порты: `UpdateDownloader`, `ZipExtractor`, `VersionActions`; `AppRestarter`, `AppInstallDirResolver`; `currentUpdatePlatform()`.
- Данные: `UpdateDataSource`, строгие DTO/мапперы v4, `ChannelsFileMapper`.
- DI: `JvmAppDefinition` регистрирует старый `UpdateManager` и `VersionActions`; `DiProvider` регистрирует `AppVersionProvider` и старый `CurrentChannelProvider` (для настроек).
- `jvmApp/main.kt` вручную вызывает `loadManifest`, `targetFor`, `performUpdate`.
- Web: `webShell` использует `UpdateDataSource`, `ChannelSelection`, `targetFor(String)`, `ChannelsFile`; старый `UpdateManager` не использует.
- Тесты: `UpdateManagerInstallFilesTest`, `UpdateManagerManifestSelectionTest`, `CurrentChannelProviderImplTest`, `ZipExtractorImplTest`, мапперы, `ChannelSelectionTest`.

### 6.2 Что сломано / спорно

1. **Компиляция.** `ChannelsFileMapper.kt:15` передаёт `defaultDto?.toDomain()` в `ChannelsFile.default: ChannelAccess` (non-null). Ветка не собирается. Нужен фикс + правка `ChannelsFileMapperTest`.
2. **`finally { Idle }`** в заготовке `checkUpdate` стирает `Available`/`Error`.
3. **`currentPlatform: String`** — лишний параметр; есть `currentUpdatePlatform()` и `UpdatePlatform`.
4. **`email: String`** — на JVM авторизации пока нет, на старте email отсутствует. Нужен `String?`.
5. **`users.getOrDefault(email, default)`** — регистрозависимый поиск; правильный путь — `ChannelsFile.accessFor(email)` (ключи маппера в нижнем регистре).
6. **`currentChannelAccess(): ChannelAccess`** — синхронный метод, возвращает сырую серверную модель; для UI нужен реактивный типизированный снимок.
7. **`io.ktor.util.Platform`** в `AppUpdateManager.kt` — неверный/неиспользуемый импорт.
8. **Дубли контрактов**: старый `UpdateManager` и новый `AppUpdateManager`; старый `UpdateState` и `UpdateStateNewVision`; old `CurrentChannelProvider` и new `CurrentChannelProvider`; common `UpdateChannel` (object) и new `UpdateChannel` (enum).
9. **`VersionActions`** — заглушки без серверных эндпоинтов; в новом контракте не нужны.

### 6.3 Что сохраняется

- Пайплайн `performUpdate` — дословно.
- Порты `UpdateDownloader`, `ZipExtractor`, `AppRestarter`, `AppInstallDirResolver`, `UpdatePlatform`, `currentUpdatePlatform()`.
- `UpdateDataSource`, DTO, мапперы, `ManifestSupported`, `ChannelSelection` (для web).
- `AppVersion`, `AppVersionProvider`, `normalizeFromTechnical()`.
- Web-гейт и web-приложение — без изменения поведения.

---

## 7. Целевая архитектура

### 7.1 Публичный контракт

`core/update/api/src/jvmMain/kotlin/domain/AppUpdateManager.kt`:

```kotlin
package domain

import domain.model.ChannelAccessSnapshot
import domain.model.CheckUpdateState
import domain.model.UpdateState
import kotlinx.coroutines.flow.StateFlow

/**
 * Центральный вход в систему обновлений JVM-приложения.
 *
 * Два сценария: проверка обновления и запуск установки.
 * Состояния отдаются как read-only StateFlow: у них всегда есть текущее
 * значение, и UI не зависит от момента подписки.
 */
interface AppUpdateManager {
    /** Состояние проверки обновления. */
    val checkUpdateState: StateFlow<CheckUpdateState>

    /** Состояние процесса установки. */
    val updateState: StateFlow<UpdateState>

    /**
     * Последний известный доступ к каналам: allowed, selected и эффективный current.
     * Нужен будущему UI переключения каналов; не зависит от результата проверки.
     */
    val channelAccessState: StateFlow<ChannelAccessSnapshot>

    /**
     * Проверяет наличие обновления для текущей версии приложения.
     *
     * Канал: channels.json по email → selected / первый allowed → PROD.
     * Если email == null, channels.json не запрашивается, используется PROD.
     *
     * Результат не возвращается: терминальное состояние публикуется
     * в [checkUpdateState] до завершения функции. После вызова вызывающий
     * читает [checkUpdateState.value] (или подписывается на flow).
     */
    suspend fun checkUpdate(email: String? = null)

    /**
     * Запускает установку найденного обновления.
     *
     * Fire-and-forget: выполняется в собственном scope менеджера.
     * Если обновление не найдено или установка уже идёт — ничего не делает.
     */
    fun startUpdate()
}
```

Почему так:

- `StateFlow`, а не `Flow`: у состояния всегда есть текущее значение; UI не зависит от момента подписки; исключается повторная эмиссия при повторной подписке.
- `checkUpdate` не возвращает состояние: единственный источник истины — `checkUpdateState`; после `checkUpdate` вызывающий читает `checkUpdateState.value`, а UI просто подписан на flow. Контракт требует опубликовать терминальное состояние до возврата, иначе чтение `.value` после вызова было бы гонкой.
- `startUpdate` — `fun`, fire-and-forget: отмена UI-корутины не прерывает установку; это текущее поведение старого `performUpdate`.
- `channelAccessState` — отдельный flow: UI переключения каналов нужен доступ к `allowed`/`selected` независимо от результата проверки. Основные два flow — `checkUpdateState` и `updateState`; третий — вспомогательный снимок.

### 7.2 Состояния и модели

#### `CheckUpdateState`

`core/update/api/src/jvmMain/kotlin/domain/model/CheckUpdateState.kt`:

```kotlin
package domain.model

import AppException

sealed interface CheckUpdateState {
    /** Проверка ещё не запускалась. */
    data object Idle : CheckUpdateState

    /** Проверка выполняется. */
    data object Loading : CheckUpdateState

    /** Найдено обновление. */
    data class Available(val update: AvailableUpdate) : CheckUpdateState

    /** Установлена актуальная (или более новая) версия. */
    data object IsUpToDate : CheckUpdateState

    /** Обновление проверить можно, но ставить нечего. */
    data class Unavailable(val reason: UnavailableReason) : CheckUpdateState

    /** Проверка не удалась: сеть, HTTP, парсинг, невалидная версия. */
    data class Error(val error: AppException.UpdateError) : CheckUpdateState
}

/** Почему обновление недоступно, хотя проверка выполнена. */
enum class UnavailableReason {
    /** ОС не поддерживает JVM-обновления (Linux и т.п.). */
    UNSUPPORTED_PLATFORM,

    /** Для текущего канала и платформы нет опубликованного таргета. */
    NO_PUBLISHED_BUILD,
}
```

Важно:

- `IsUpToDate` — только когда версия не новее. «Нет сборки под платформу/канал» — это `Unavailable(NO_PUBLISHED_BUILD)`, а не `IsUpToDate`.
- Ошибка загрузки/парсинга манифеста — `Error(CheckFailed)`, а не `Unavailable`.
- Ошибка `channels.json` — не ошибка проверки: fallback на `PROD`, проверка продолжается.

#### `AvailableUpdate`

```kotlin
package domain.model

/** Данные, необходимые для установки и показа обновления. */
data class AvailableUpdate(
    val version: AppVersion,
    val channel: UpdateChannel,
    val url: String,
    val hash: String,
    val size: Long,
    val patchNote: List<String> = emptyList(),
)
```

- `version` — типизированная версия для UI (а не сырая техническая строка).
- `channel` — канал, из которого пришло обновление.
- `url`/`hash`/`size` — входы пайплайна; hash и size обязательны (маппер гарантирует непустоту).
- `patchNote` — для будущего UI; `date`/`tags` можно добавить позже, когда UI попросит.

#### `ChannelAccessSnapshot`

```kotlin
package domain.model

/** Полное состояние каналов для UI и диагностики. */
data class ChannelAccessSnapshot(
    /** Известные приложению каналы, доступные пользователю, в порядке файла. */
    val allowed: List<UpdateChannel> = emptyList(),

    /** Канал, выбранный пользователем (если известен приложению). */
    val selected: UpdateChannel? = null,

    /** Эффективный канал, по которому шла проверка. Он же — источник решения. */
    val current: UpdateChannel = UpdateChannel.PROD,

    /** Откуда взяты данные: серверный файл каналов или fallback. */
    val source: ChannelAccessSource = ChannelAccessSource.FALLBACK,
)

enum class ChannelAccessSource {
    /** channels.json успешно прочитан. */
    SERVER,

    /** channels.json не читался (email == null) или недоступен. */
    FALLBACK,
}
```

Правила:

- `allowed` — только известные enum-значения, без пустых и неизвестных алиасов, без дублей, порядок файла сохраняется.
- `selected` — известный enum, даже если он не входит в `allowed`; это позволяет UI показать расхождение.
- `current` — **единственный источник решения**: `selected`, если он валиден и входит в `allowed` (или `allowed` пуст), иначе первый `allowed`, иначе `PROD`.
- `source` — `SERVER`, если файл каналов прочитан (даже если сработал `default`), иначе `FALLBACK`.
- При `source = FALLBACK` эффективный `current` всегда `PROD` (решение пользователя); `allowed`/`selected` могут остаться от последнего успешного чтения для отображения.

#### `UpdateState`

`core/update/api/src/jvmMain/kotlin/domain/model/UpdateState.kt` (заменяет старый файл):

```kotlin
package domain.model

/** Состояние процесса установки. Этапы — как в проверенном пайплайне. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data class Loading(val stage: CurrentStageState) : UpdateState
    data class Verification(val stage: CurrentStageState) : UpdateState
    data class IntegrityChecking(val stage: CurrentStageState) : UpdateState
    data class Unpacking(val stage: CurrentStageState) : UpdateState
    data class Installation(val stage: CurrentStageState) : UpdateState
    data class CleaningTemporaryFiles(val stage: CurrentStageState) : UpdateState
    data class Restarting(val remaining: Int) : UpdateState
}

/**
 * Состояние текущего этапа.
 *
 * Progress.value — 0..100 (при наличии). Error.message — причина для лога/UI.
 */
sealed interface CurrentStageState {
    data object Idle : CurrentStageState
    data class Progress(val value: Float? = null) : CurrentStageState
    data class Error(val message: String? = null) : CurrentStageState
    data object Success : CurrentStageState
}
```

Отличия от старого `domain.model.UpdateState`:

- `Idle` — `data object`, без вложенного `CurrentStageState.Idle` (старая форма была избыточной).
- `sealed interface` вместо `sealed class` — как в `UpdateStateNewVision`; лучше для `data object`.
- Формы этапов, единицы прогресса и семантика ошибок сохранены.

#### `UpdateChannel` (enum)

`core/update/api/src/jvmMain/kotlin/domain/model/UpdateChannel.kt`:

```kotlin
package domain.model

import domain.UpdateChannel as ChannelAliases

/**
 * Типизированный канал обновлений для JVM.
 *
 * Алиасы берутся из общего domain.UpdateChannel, чтобы не дублировать строки.
 */
enum class UpdateChannel(val alias: String) {
    PROD(ChannelAliases.PROD),
    DEV_CANARY(ChannelAliases.DEV_CANARY),
    DEV_RC(ChannelAliases.DEV_RC);

    companion object {
        fun fromAlias(alias: String?): UpdateChannel? =
            entries.firstOrNull { it.alias == alias?.trim() }
    }
}
```

- Common-объект `domain.UpdateChannel` остаётся для web (`ChannelSelection`, `targetFor(String)`, гейт).
- Enum — JVM-типизация; строки не дублируются.

Типизированный резолв таргета — в `domain.model`:

```kotlin
package domain.model

import domain.UpdatePlatform
import domain.targetFor
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest

fun UpdateManifest.targetFor(
    channel: UpdateChannel,
    platform: UpdatePlatform,
): TargetData? = targetFor(channel.alias, platform)
```

- Общая реализация `targetFor(String, UpdatePlatform)` не меняется.
- Web-перегрузка `targetFor(String)` не меняется.

#### `ChannelAccessStore`

`core/update/api/src/jvmMain/kotlin/domain/port/ChannelAccessStore.kt`:

```kotlin
package domain.port

import domain.models.channels.ChannelAccess

/**
 * Хранилище последнего известного доступа к каналам.
 *
 * Реализация по умолчанию — in-memory. Порт позволяет позже заменить её
 * на persisted-хранилище без изменения менеджера.
 */
interface ChannelAccessStore {
    fun save(access: ChannelAccess)
    fun current(): ChannelAccess?
}
```

- Используется менеджером для сохранения успешного ответа `channels.json` и для отображения последних известных `allowed`/`selected` при fallback.
- В публичном UI-контракте используется `channelAccessState`, а не store.

### 7.3 Алгоритм `checkUpdate`

Входы:

- `email: String?`;
- `platform: UpdatePlatform?` (инъекция, по умолчанию `currentUpdatePlatform()`);
- `AppVersionProvider.provide()` — установленная версия;
- `UpdateDownloader.downloadChannelsManifest()` / `downloadUpdateManifest()`.

Пошагово:

1. `checkUpdateState.value = CheckUpdateState.Loading` (внутри `Mutex`, чтобы проверки не пересекались).
2. Если `platform == null`:
   - `channelAccessState.value = ChannelAccessSnapshot()` (пусто, `PROD`, `FALLBACK`);
   - опубликовать `Unavailable(UNSUPPORTED_PLATFORM)` и завершить; сеть не трогаем.
3. Нормализовать email: `email?.trim()?.takeIf { it.isNotEmpty() }`.
4. Параллельно (`coroutineScope` + `async`):
   - `channels`: если email != null — `downloadChannelsManifest()`, иначе `null`;
   - `manifest`: `downloadUpdateManifest()`.
   - Ошибки: `channels` — ловим и считаем `null` (fallback); `manifest` — сохраняем причину для `Error`.
   - `CancellationException` всегда пробрасываем, не глотаем.
5. Если `channels` прочитан:
   - `access = channels.accessFor(email)` (регистронезависимый поиск, `default` при отсутствии пользователя);
   - `channelAccessStore.save(access)`;
   - `snapshot = snapshotFrom(access, SERVER)`.
6. Если `channels` не прочитан:
   - если email == null — `snapshot = ChannelAccessSnapshot()` (не читаем store, чтобы не показать чужие данные);
   - если email != null — `snapshot = snapshotFrom(channelAccessStore.current(), FALLBACK).copy(current = PROD)` (последние известные allowed/selected для отображения, эффективный канал — prod).
   - `channelAccessState.value = snapshot`.
7. Если `manifest` не прочитан — опубликовать `Error(CheckFailed(channel = snapshot.current.alias, cause = error))` и завершить.
8. `target = manifest.targetFor(snapshot.current, platform)`:
   - `null` → опубликовать `Unavailable(NO_PUBLISHED_BUILD)` и завершить; **не** подменять канал на prod.
9. `availableVersion = target.version.normalizeFromTechnical()`:
   - ошибка → опубликовать `Error(CheckFailed(..., cause = error))` и завершить.
10. `installedVersion = versionProvider.provide()`:
    - ошибка → опубликовать `Error(CheckFailed(..., cause = error))` и завершить.
11. Если `availableVersion > installedVersion`:
    - опубликовать `Available(AvailableUpdate(version, snapshot.current, target.link, target.hash, target.size, target.patchNote))`.
12. Иначе опубликовать `IsUpToDate`.
13. `publishTerminal(state)`: `checkUpdateState.value = state`; функция завершается без возврата значения. Терминальное состояние опубликовано до выхода — это часть контракта.
14. `CancellationException`: `checkUpdateState.value = Idle`; пробросить.
15. Любая другая ошибка: опубликовать `Error(CheckFailed(cause = error))`.
16. **Не** использовать `finally { Idle }`.
17. Вызывающий после `checkUpdate` читает `checkUpdateState.value`; UI подписан на `checkUpdateState`.

`snapshotFrom`:

```kotlin
private fun snapshotFrom(
    access: ChannelAccess?,
    source: ChannelAccessSource,
): ChannelAccessSnapshot {
    val allowed = access?.allowed.orEmpty()
        .mapNotNull(UpdateChannel::fromAlias)
        .distinct()
    val selected = UpdateChannel.fromAlias(access?.selected)
    val current = when {
        selected != null && (allowed.isEmpty() || selected in allowed) -> selected
        allowed.isNotEmpty() -> allowed.first()
        else -> UpdateChannel.PROD
    }
    return ChannelAccessSnapshot(
        allowed = allowed,
        selected = selected,
        current = current,
        source = source,
    )
}
```

Таблица решений:

| Входные данные | Эффективный `current` | Результат проверки |
|---|---|---|
| `email == null` | `PROD` | проверка манифеста по prod |
| `channels.json` недоступен | `PROD` | проверка манифеста по prod; `allowed/selected` — последние известные, `source=FALLBACK` |
| пользователя нет в файле | `default` | проверка по default-каналу |
| `selected` валиден и входит в `allowed` (или `allowed` пуст) | `selected` | проверка по selected |
| `selected` валиден, но не входит в `allowed` | первый `allowed` | проверка по первому allowed |
| `selected` пуст/неизвестен | первый `allowed` или `PROD` | проверка по первому allowed/prod |
| `allowed` содержит неизвестные алиасы | они игнорируются | — |
| платформа не поддерживается | `PROD` | `Unavailable(UNSUPPORTED_PLATFORM)` |
| таргета канала+платформы нет | `current` | `Unavailable(NO_PUBLISHED_BUILD)` |
| версия таргета равна/старше | `current` | `IsUpToDate` |
| версия таргета новее | `current` | `Available` |
| манифест недоступен/невалиден | `current` | `Error(CheckFailed)` |

Важно: fallback на `PROD` — это fallback **выбора канала**, а не fallback «если в выбранном канале нет сборки». Если выбран dev-канал и в нём нет сборки, возвращается `Unavailable(NO_PUBLISHED_BUILD)`; в prod не переключаемся (сохранение поведения старого `loadManifest`).

### 7.4 Полная интеграция пайплайна

Пайплайн переносится из `UpdateManagerImpl` в `UpdateInstaller` дословно. Меняется только приёмник состояний: вместо `updateState.emit(...)` — callback `onState(UpdateState)`.

#### `UpdateInstaller`

`core/update/impl/src/jvmMain/kotlin/domain/UpdateInstaller.kt`:

```kotlin
internal class UpdateInstaller(
    private val updateDownloader: UpdateDownloader,
    private val zipExtractor: ZipExtractor,
    private val appRestarter: AppRestarter,
    private val appInstallDirResolver: AppInstallDirResolver,
    private val platform: UpdatePlatform,
    private val downloadFileFactory: () -> File = { createDownloadFile(platform) },
) {
    suspend fun install(
        update: AvailableUpdate,
        onState: (UpdateState) -> Unit,
    ) {
        // 1. начальное состояние
        // 2. downloadWithProgress(...).catch { ... }.collect { ... }
        // 3. по Success — installArchive(File(status.filePath), update)
    }

    private suspend fun installArchive(file: File, update: AvailableUpdate) { /* старый installUpdate */ }
    // verifyArchive, checkArchiveIntegrity, unpackArchive, replaceFiles,
    // cleanupAfterInstall, restartApplication, cleanupBackupFiles,
    // reSignApplication, replaceFilesSafely, replaceFile, isBundledRuntime
}
```

Соответствие старому коду:

| Старый `UpdateManagerImpl` | Новый `UpdateInstaller` |
|---|---|
| `performUpdate(updateUrl, hash, size)` | `install(update, onState)` + `downloadFileFactory` |
| `installUpdate(file, hash, size)` | `installArchive(file, update)` |
| `verifyArchive` | без изменений |
| `checkArchiveIntegrity` | без изменений |
| `unpackArchive` | без изменений |
| `replaceFiles` | без изменений |
| `cleanupAfterInstall` | без изменений |
| `restartApplication` | без изменений |
| `cleanupBackupFiles` | без изменений (`internal` для тестов) |
| `reSignApplication` | без изменений |
| `replaceFilesSafely` | без изменений (`internal` для тестов) |
| `replaceFile` | без изменений |
| `isBundledRuntime` | без изменений |
| `createDownloadFile()` | `createDownloadFile(platform)` + фабрика |
| `File.computeSha256()` | без изменений |

#### Этапы и состояния

| Этап | Состояния | Действие | Ошибка |
|---|---|---|---|
| Download | `Loading(Progress(0..100))`, `Loading(Success)` | `downloadWithProgress(url, dest)` | `Loading(Error(message))`, остановка |
| Verification | `Verification(Progress(0/50/100))`, `Verification(Success)` | размер == `expectedSize`; SHA-256 == `expectedHash` | `Verification(Error(message))`, abort |
| Integrity | `IntegrityChecking(Progress(0/100))`, `Success` | `zipExtractor.verify` | `IntegrityChecking(Error)`, abort |
| Unpacking | `Unpacking(Progress(0/100))`, `Success` | `zipExtractor.extract` во временную директорию | `Unpacking(Error)`, abort |
| Installation | `Installation(Progress(...))`, `Success` | `replaceFilesSafely`: `.bak`-rename, пропуск `runtime/` и `Contents/runtime/`, exec-бит | внешний catch: лог + удаление temp |
| Cleaning | `CleaningTemporaryFiles(Progress())`, `Success` | удалить `.bak`, `codesign` (macOS), удалить temp | лог |
| Restarting | `Restarting(5..1)` | пауза 1 с, `appRestarter.restart()` | процесс завершается |

Сохраняются:

- паузы 300 мс между этапами (задел под UI);
- отсчёт 5→1;
- каталоги загрузки: Windows `%USERPROFILE%/AppData/Roaming/Helpdesk/updates`, macOS `~/Library/Application Support/Helpdesk/updates`, прочие `~/.helpdesk/updates`;
- имя файла `helpdesk-update.zip`;
- временная директория `app_extracted_{timestamp}`;
- `.bak`-rename и удаление `.bak` (целостность подписи macOS);
- пропуск встроенной JRE;
- `codesign --force --deep --sign -` на macOS;
- обязательная проверка и размера, и SHA-256.

Небольшие технические улучшения, не меняющие этапы:

- `downloadFileFactory` — тестовый шов, чтобы не писать в `user.home`;
- `CancellationException` пробрасывается, а не глотается в `catch`;
- начальный `Loading(Progress(0f))` перед подпиской на download-flow, чтобы UI не показывал состояние предыдущей попытки;
- ошибки подготовки файла загрузки и резолва директории установки публикуются как `Loading(Error)` / `Installation(Error)`; если неожиданная ошибка установки не была покрыта этапом, менеджер публикует `Loading(Error)`, а не только пишет лог.

### 7.5 Внутренняя структура и ответственности

| Компонент | Ответственность |
|---|---|
| `AppUpdateManagerImpl` | состояния, `checkUpdate`, выбор канала, `startUpdate`, защита от повторного запуска, создание `UpdateInstaller` |
| `UpdateInstaller` | полный пайплайн установки; не знает про каналы и проверку |
| `ChannelAccessStoreImpl` | in-memory последний `ChannelAccess` |
| `UpdateDownloader` | манифест, каналы, download-flow |
| `UpdateDataSource` | HTTP + DTO + маппинг |
| `AppVersionProvider` | текущая версия |
| `AppRestarter` / `AppInstallDirResolver` | платформенные операции |
| `CoroutineScope` | app-scoped, `SupervisorJob + Dispatchers.IO`; инъекция для тестов |

`AppUpdateManagerImpl`:

```kotlin
class AppUpdateManagerImpl(
    private val updateDownloader: UpdateDownloader,
    private val zipExtractor: ZipExtractor,
    private val appRestarter: AppRestarter,
    private val appInstallDirResolver: AppInstallDirResolver,
    private val channelAccessStore: ChannelAccessStore,
    private val versionProvider: AppVersionProvider,
    private val platform: UpdatePlatform? = currentUpdatePlatform(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val downloadFileFactory: () -> File = { createDownloadFile(platform) },
) : AppUpdateManager
```

- `checkUpdateState`/`updateState`/`channelAccessState` — backing fields `MutableStateFlow`, публично `StateFlow`.
- `checkMutex = Mutex()`; `isUpdating = AtomicBoolean(false)`.
- `startUpdate`:
  1. `val available = checkUpdateState.value as? CheckUpdateState.Available ?: return`;
  2. `val currentPlatform = platform ?: return`;
  3. `if (!isUpdating.compareAndSet(false, true)) return`;
  4. `scope.launch { try { UpdateInstaller(..., currentPlatform, downloadFileFactory).install(available.update) { updateState.value = it } } finally { isUpdating.set(false) } }`.
- `checkUpdate`:
  - `checkMutex.withLock`;
  - `Loading` → параллельные запросы → snapshot → terminal;
  - результат только публикуется в `checkUpdateState`, функция возвращает `Unit`;
  - терминальное состояние не сбрасывается;
  - `CancellationException` → `Idle` + проброс;
  - прочие ошибки → `Error(CheckFailed)`.

Пояснения:

- `platform == null` на Linux: `checkUpdate` публикует `Unavailable`, `startUpdate` не запускается. `UpdateInstaller` не создаётся, файловые операции не вызываются.
- `checkUpdate` можно вызывать повторно; терминальное состояние заменяется новым.
- `startUpdate` идемпотентен: повторный вызов во время установки игнорируется.
- После ошибки установки `isUpdating` сбрасывается, `checkUpdateState` остаётся `Available` — можно повторить `startUpdate`.
- Неожиданная ошибка установки не теряется: менеджер публикует `Loading(Error)`, если ни один этап ещё не опубликовал ошибку; ранее опубликованная ошибка этапа не перезаписывается.
- `checkUpdate` во время установки не блокируется, но уже запущенный пайплайн использует захваченный `AvailableUpdate`; UI должен показывать `updateState`.

### 7.6 Пакеты, имена и миграция

| Сейчас | После |
|---|---|
| `api/jvmMain/kotlin/newvision/AppUpdateManager.kt` (`package domain`) | `api/jvmMain/kotlin/domain/AppUpdateManager.kt` |
| `api/jvmMain/kotlin/newvision/CurrentChannelProvider.kt` | `api/jvmMain/kotlin/domain/port/ChannelAccessStore.kt` |
| `api/jvmMain/kotlin/newvision/UpdateChannel.kt` | `api/jvmMain/kotlin/domain/model/UpdateChannel.kt` |
| `api/jvmMain/kotlin/newvision/UpdateStateNewVision.kt` | `api/jvmMain/kotlin/domain/model/UpdateState.kt` (заменяет старый) |
| `api/commonMain/kotlin/state/CheckUpdateState.kt` | `api/jvmMain/kotlin/domain/model/CheckUpdateState.kt` (переписан) |
| `api/jvmMain/kotlin/domain/UpdateManager.kt` | удалить |
| `api/jvmMain/kotlin/domain/model/UpdateState.kt` (старый) | заменить новым содержимым |
| `impl/jvmMain/kotlin/newvision/AppUpdateManagerImpl.kt` | `impl/jvmMain/kotlin/domain/AppUpdateManagerImpl.kt` |
| `impl/jvmMain/kotlin/newvision/CurrentChannelProviderImpl.kt` | `impl/jvmMain/kotlin/data/impl/ChannelAccessStoreImpl.kt` |
| `impl/jvmMain/kotlin/domain/UpdateManagerImpl.kt` | удалить; пайплайн → `domain/UpdateInstaller.kt` |
| `impl/jvmMain/kotlin/data/impl/VersionActionsImpl.kt` | оставить файл, убрать регистрацию в DI (или удалить отдельным коммитом) |
| `api/jvmMain/kotlin/domain/port/VersionActions.kt` | оставить как неиспользуемый задел; в новый контракт не входит |
| `impl/jvmMain/kotlin/CurrentTechnicalVersion.kt` | удалить, если после `main.kt` нет потребителей |

Новые файлы:

- `api/jvmMain/kotlin/domain/model/AvailableUpdate.kt`
- `api/jvmMain/kotlin/domain/model/ChannelAccessSnapshot.kt`
- `api/jvmMain/kotlin/domain/model/UpdateChannelTarget.kt` (типизированный `targetFor`)
- `api/jvmMain/kotlin/domain/port/ChannelAccessStore.kt`
- `impl/jvmMain/kotlin/domain/AppUpdateManagerImpl.kt`
- `impl/jvmMain/kotlin/domain/UpdateInstaller.kt`
- `impl/jvmMain/kotlin/data/impl/ChannelAccessStoreImpl.kt`

Удаляемые каталоги/файлы:

- `api/jvmMain/kotlin/newvision/` (4 файла)
- `impl/jvmMain/kotlin/newvision/` (2 файла)
- `api/jvmMain/kotlin/domain/UpdateManager.kt`
- `api/commonMain/kotlin/state/CheckUpdateState.kt`
- `impl/jvmMain/kotlin/domain/UpdateManagerImpl.kt`
- `impl/jvmMain/kotlin/CurrentTechnicalVersion.kt` (если не используется)

### 7.7 DI и `jvmApp`

`JvmAppDefinition`:

```kotlin
private val updaterModule = module {
    single { UpdateDataSource(get()) }
    single<UpdateDownloader> { UpdateDownloaderImpl(get(), get()) }
    single<ZipExtractor> { ZipExtractorImpl() }
    single<ChannelAccessStore> { ChannelAccessStoreImpl() }
    single<AppRestarter> { createAppRestarter() }
    single<AppInstallDirResolver> { createAppInstallDirResolver() }
    single<AppUpdateManager> {
        AppUpdateManagerImpl(
            updateDownloader = get(),
            zipExtractor = get(),
            appRestarter = get(),
            appInstallDirResolver = get(),
            channelAccessStore = get(),
            versionProvider = get(),
        )
    }
}
```

- `AppVersionProvider` уже зарегистрирован в `DiProvider.updateModule`.
- `VersionActions` больше не регистрируется.
- `platform`/`scope`/`downloadFileFactory` используют дефолты.
- Koin-синглы ленивы; на Linux `AppUpdateManagerImpl` создаётся с `platform = null` и не падает. Чтобы `main.kt` не падал на резолве `AppRestarter`/`AppInstallDirResolver`, OS-switch должен возвращать безопасные заглушки, которые падают только при вызове:
  - `AppRestarter { error("Unsupported OS: ...") }`;
  - `AppInstallDirResolver`, у которого методы `error(...)`/`null`.
  - На Linux `checkUpdate` опубликует `Unavailable(UNSUPPORTED_PLATFORM)`, и эти методы не вызовутся.

`jvmApp/main.kt`:

```kotlin
fun main() {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
    val updateManager = koinApplication.koin.get<AppUpdateManager>()

    application {
        LaunchedEffect(Unit) {
            updateManager.checkUpdate(email = null)

            // checkUpdate публикует терминальное состояние до возврата,
            // поэтому после вызова можно безопасно прочитать текущее значение.
            if (updateManager.checkUpdateState.value is CheckUpdateState.Available) {
                updateManager.startUpdate()
            }
        }

        Window(onCloseRequest = ::exitApplication, title = "Helpdesk Platform") {
            // существующее окно-заглушка
        }
    }
}
```

- `email = null` до появления JVM-авторизации; `checkUpdate` не запрашивает `channels.json` и использует `prod`.
- `startUpdate` запускает пайплайн в scope менеджера; `LaunchedEffect` завершается, установка продолжается.
- `CurrentChannelProvider`, `UpdateChannel`, `targetFor`, `currentUpdatePlatform`, `currentTechnicalVersion` из `main.kt` уходят.

### 7.8 Будущий UI

UI (отдельная задача) инжектит `AppUpdateManager` и собирает три состояния:

```kotlin
val check by manager.checkUpdateState.collectAsState()
val update by manager.updateState.collectAsState()
val channels by manager.channelAccessState.collectAsState()

when (val state = check) {
    is CheckUpdateState.Available -> Button(onClick = manager::startUpdate) {
        Text("Обновить до ${state.update.version}")
    }
    CheckUpdateState.IsUpToDate -> Text("Установлена последняя версия")
    is CheckUpdateState.Unavailable -> Text(
        when (state.reason) {
            UnavailableReason.UNSUPPORTED_PLATFORM -> "Обновления недоступны для этой ОС"
            UnavailableReason.NO_PUBLISHED_BUILD -> "Для канала ${channels.current.alias} нет сборки"
        },
    )
    is CheckUpdateState.Error -> Text(state.error.message ?: "Ошибка проверки")
    CheckUpdateState.Idle, CheckUpdateState.Loading -> CircularProgressIndicator()
}
```

- `channelAccessState` даёт `allowed`/`selected`/`current`/`source` для экрана переключения каналов. Мутация выбора канала (запись в `channels.json`) — будущая серверная операция; текущий дизайн готовит только чтение и отображение.
- `updateState` даёт прогресс по этапам и `Restarting(remaining)`.
- `startUpdate` вызывается напрямую (не suspend), кнопка не должна ждать.
- Менеджер — синглтон Koin; состояние переживает пересоздание экрана.
- UI не должен запускать установку из `onDispose` и не должен отменять её.

### 7.9 Что не трогаем

- `webShell` (`GateApp`, `ChannelSelection`, `GateStorage`, `targetFor(String)`) — поведение не меняется. Единственное общее изменение — фикс `ChannelsFileMapper`, который сохраняет fallback гейта.
- `webApp`.
- `features/update` (веб-экран обновления, `AppUpdater`, `UpdateDecision`, `CANARY_WEB_BUILD_URL`).
- `core/network`, авторизацию, MAX-мост.
- `UpdateDataSource`, DTO, строгие v4-мапперы.
- `VersionActions`-файлы (остаются, но не регистрируются).

---

## 8. Обязательные фиксы и рефакторинги

### 8.1 `ChannelsFileMapper` (блокер компиляции)

`ChannelsFile.default` теперь non-null. Маппер должен вернуть непустое значение:

```kotlin
default = defaultDto?.toDomain() ?: ChannelAccess(
    allowed = emptyList(),
    selected = null,
),
```

Почему не `required("default")`:

- отсутствие `default` не должно ломать web-гейт; пустой доступ сохраняет прежнее поведение «нет данных → последний канал/канал по умолчанию»;
- JVM-менеджер в этом случае всё равно уходит в `PROD`;
- контракт сервера «default обязателен» остаётся рекомендацией, но клиент деградирует мягко.

`ChannelsFileMapperTest`:

- тест «maps missing default and users to empty file» меняет ожидание с `assertNull(file.default)` на `assertEquals(ChannelAccess(emptyList(), null), file.default)`;
- `accessFor("unknown")` возвращает пустой доступ, а не `null`.

### 8.2 `UpdateDownloader`

Текущие методы возвращают `null` и теряют причину. Для `Error(CheckFailed)` нужна причина:

```kotlin
interface UpdateDownloader {
    suspend fun downloadUpdateManifest(): UpdateManifest
    suspend fun downloadChannelsManifest(): ChannelsFile
    suspend fun downloadWithProgress(url: String, destinationFilePath: String): Flow<DownloadStatus>
}
```

- `UpdateDownloaderImpl` перестаёт оборачивать в `runCatching`: пусть исключение летит в менеджер; менеджер решает, где fallback, а где `Error`.
- `downloadWithProgress` не меняется.

### 8.3 Прочие фиксы

- Убрать `io.ktor.util.Platform` и дублирующий HTTP-код из заготовки `AppUpdateManagerImpl`.
- Использовать `ChannelsFile.accessFor(email)` вместо `users.getOrDefault(email, default)`.
- Убрать `finally { Idle }`; терминальное состояние не сбрасывать.
- Пробрасывать `CancellationException` в `checkUpdate`, download-flow и `installArchive`.
- `VersionActions`/`markAsStable`/`reject`/`publish`/`devChannelAccess` — убрать из нового контракта и DI. Файлы-заглушки оставить как задел (или удалить отдельным коммитом, не расширяя этот скоуп).
- `CurrentTechnicalVersion.kt` — удалить, если после `main.kt` нет потребителей.

---

## 9. Тесты

Новые тесты — `core/update/impl/src/jvmTest`; зависимости: `jvmTest` + `libs.kotlinx.coroutines.test`. Стиль — AGENTS.md: backticks-имена, табличные кейсы, русские сообщения ассертов.

### 9.1 `AppUpdateManagerCheckTest`

- `falls back to prod when channels manifest is unavailable`
- `does not request channels when email is missing`
- `uses selected channel when it is allowed`
- `uses first allowed channel when selected is missing`
- `uses prod when neither selected nor allowed are known`
- `ignores unknown aliases in allowed list`
- `reports unavailable when target is not published`
- `reports unavailable on unsupported platform`
- `reports up to date when installed version is current or newer`
- `reports error when manifest cannot be loaded`
- `reports error when target version is invalid`
- `publishes terminal state before checkUpdate returns`
- `does not reset terminal state to idle`
- `keeps channel access snapshot with allowed selected and current`
- `keeps last known channels on fallback but sets current to prod`

Фейки: `UpdateDownloader`, `AppVersionProvider`, `ChannelAccessStoreImpl`, временные `ZipExtractor`/`AppRestarter`/`AppInstallDirResolver` (в проверке не участвуют), `ManifestFixtures`.

### 9.2 `AppUpdateManagerStartTest`

- `does nothing when no update is available`
- `ignores repeated start while update is running`
- `runs the pipeline and restarts the app`
- `allows retry after a failed install`

Управление корутинами: `runTest`, `StandardTestDispatcher(testScheduler)`, `advanceUntilIdle`. `downloadFileFactory` возвращает временный файл; `AppRestarter` записывает вызов и не завершает процесс.

### 9.3 `UpdateInstallerTest`

Перенос старых тестов файловых операций:

- `keeps previous version as backup while replacing files`
- `keeps bundled runtime untouched`
- `removes backup files after installation`

Новые тесты пайплайна:

- `emits verification error when size does not match`
- `emits verification error when hash does not match`
- `emits integrity error when zip is corrupted`
- `emits unpacking error when extraction fails`
- `emits restarting countdown and restarts`
- `propagates cancellation instead of reporting an error`

`replaceFilesSafely`/`cleanupBackupFiles` остаются `internal`, чтобы тесты могли вызывать их напрямую.

### 9.4 `ChannelAccessStoreImplTest`, `UpdateChannelTest`

- save/current;
- `fromAlias` для известных/неизвестных/blank;
- таблица `selected`/`allowed` → `current`.

### 9.5 Существующие тесты

Должны остаться зелёными:

- `ChannelsFileMapperTest` (с обновлённым ожиданием default);
- `ChannelSelectionTest`;
- `ManifestTargetForChannelTest`;
- `UpdateManifestV4MapperTest`, wire-format;
- `ZipExtractorImplTest`;
- `CurrentChannelProviderImplTest`;
- `AppVersion*`.

`UpdateManagerInstallFilesTest` и `UpdateManagerManifestSelectionTest` заменяются новыми тестами, а не удаляются без покрытия.

---

## 10. Порядок реализации и ведение TODO

**Ведение задач.** При реализации используется скилл/инструмент ведения TODO-списка (`todowrite`): каждый шаг ниже заводится отдельным пунктом; статус `pending` → `in_progress` → `completed`. Отметка `completed` ставится **сразу после завершения задачи** (компиляция/тесты зелёные), а не в конце всей работы. После каждого шага актуальный список отображается в UI агента — пользователь видит прогресс в реальном времени.

1. **Фикс компиляции**: `ChannelsFileMapper` + `ChannelsFileMapperTest`; `:core:update:impl:compileKotlinJvm` зелёный.
2. **API-контракт**: `AppUpdateManager`, `CheckUpdateState`, `AvailableUpdate`, `ChannelAccessSnapshot`, `UpdateState`, `UpdateChannel`, `ChannelAccessStore`, типизированный `targetFor`; `UpdateDownloader` → throwing; удалить старый `UpdateManager`, старый `UpdateState`, common `CheckUpdateState`, `newvision`-файлы API.
3. **Реализация**: `AppUpdateManagerImpl` (check + start), `UpdateInstaller` (перенос пайплайна), `ChannelAccessStoreImpl`, `UpdateDownloaderImpl`; удалить `UpdateManagerImpl` и `newvision`-файлы impl.
4. **DI и старт**: `JvmAppDefinition`, `main.kt`, безопасные OS-заглушки, удалить `CurrentTechnicalVersion` при отсутствии потребителей.
5. **Тесты**: check/start/installer/store/channel; обновить mapper test; удалить старые тесты после переноса покрытия.
6. **Проверка**: `./gradlew --warning-mode=fail help`, `detekt`, `allTests`, `:jvmApp:compileKotlinJvm`, web-сборки.
7. **Документация**: перенести этот дизайн в `docs/superpowers/specs/` после выхода из plan mode; при необходимости обновить `docs/plans/jvm-update-v4-integration.md` ссылкой на новый дизайн.

Параллелизация:

- шаг 1 обязателен первым;
- после шага 2 можно параллельно делать `UpdateInstaller` (перенос пайплайна) и `AppUpdateManagerImpl` (check/start);
- тесты можно писать параллельно с реализацией, но фиксировать результаты после компиляции.

---

## 11. Проверка и критерии приёмки

Команды:

```bash
./gradlew --warning-mode=fail help
./gradlew detekt
./gradlew allTests
./gradlew :jvmApp:compileKotlinJvm
./gradlew build-web-app
./gradlew build-web-shell
```

Критерии:

- [ ] Ветка компилируется; `ChannelsFileMapper` больше не ломает сборку.
- [ ] `AppUpdateManager` — единственный публичный вход JVM-обновлений; старый `UpdateManager` и `newvision` удалены.
- [ ] `checkUpdate` не возвращает состояние; терминальное состояние публикуется в `checkUpdateState` до возврата и не сбрасывается в `Idle`.
- [ ] После `checkUpdate` вызывающий может прочитать `checkUpdateState.value` и получить терминальное состояние (нет гонки).
- [ ] `email = null` не делает запрос `channels.json`; используется `prod`.
- [ ] Fallback каналов: `selected` → первый известный `allowed` → `PROD`; ошибка `channels.json` → `PROD`.
- [ ] `channelAccessState` содержит `allowed`, `selected`, `current`, `source`; при fallback эффективный `current = PROD`.
- [ ] `Unavailable(UNSUPPORTED_PLATFORM)` и `Unavailable(NO_PUBLISHED_BUILD)` возвращаются в нужных случаях.
- [ ] `Error(CheckFailed)` содержит причину (сеть/HTTP/парсинг/версия).
- [ ] `startUpdate` — `fun`, fire-and-forget, идемпотентен, допускает повтор после ошибки.
- [ ] Пайплайн ведёт себя как старый: загрузка с прогрессом, размер + SHA-256, zip, `.bak`, пропуск `runtime/`, `codesign`, отсчёт 5 с, рестарт.
- [ ] `jvmApp` стартует; при отсутствии JVM-сборки ничего не устанавливает; на Linux не падает.
- [ ] Web-гейт и web-приложение не изменились; их тесты зелёные.
- [ ] `detekt`, `allTests`, `:jvmApp:compileKotlinJvm` проходят.
- [ ] Будущий UI может собрать три `StateFlow` и отрисовать проверку, установку и каналы без знания о манифесте.

---

## 12. Риски

- **Сервер пока не публикует JVM-таргеты** → в реальности `checkUpdate` будет публиковать `Unavailable(NO_PUBLISHED_BUILD)`; пайплайн проверяется тестами и ревью.
- **Нет JVM-авторизации** → dev-каналы недоступны до появления email; fallback `PROD`.
- **`ChannelsFile.default` стал обязательным** → фикс маппера и теста; web-гейт сохраняет fallback.
- **`StateFlow` конфляция** → UI может пропустить быстрые промежуточные прогрессы; это допустимо, важны этапы и терминальные состояния.
- **Fire-and-forget** → при выходе приложения во время установки возможна частичная замена; это существующее поведение, не меняем.
- **`VersionActions`** остаются мёртвым заделом; не регистрируются.
- **Потеря покрытия** при удалении старых тестов, если новые не перенести; шаг 5 обязателен.
- **Linux** не целевая ОС; DI-заглушки предотвращают падение старта, но обновление не поддерживается.

---

## 13. Решения дизайнера (проверить при согласовании)

1. **Три `StateFlow` вместо двух**: `checkUpdateState`, `updateState`, `channelAccessState`. Третий — вспомогательный снимок для UI переключения каналов. Если нужен строго два flow, снимок можно вложить в `CheckUpdateState`; рекомендуется отдельный flow.
2. **`checkUpdate` не возвращает состояние**: результат только в `checkUpdateState`; контракт — публикация терминального состояния до возврата. Вызывающий читает `checkUpdateState.value` после вызова, UI подписан на flow. Это убирает дублирование «возврат + flow» и оставляет один источник истины.
3. **`ChannelsFileMapper` — мягкий fallback** (пустой доступ), а не `required("default")`. Сохраняет поведение web-гейта; JVM всё равно уходит в prod.
4. **`UpdateDownloader` бросает исключения**, а не возвращает `null`. Причина нужна для `Error(CheckFailed)`.
5. **`Unavailable` — отдельное состояние**, а не `IsUpToDate`. UI различает «всё актуально» и «сборки нет».
6. **При ошибке `channels.json`** эффективный канал — `PROD`; последние известные `allowed`/`selected` остаются в снимке для отображения (`source=FALLBACK`). Если это лишнее — снимок можно очищать полностью.
7. **`VersionActions`** удалены из контракта и DI, файлы оставлены как задел. Если решим удалить — отдельный коммит.
8. **`UpdateState` остаётся поэтапным**, без top-level `Failed`: пайплайн проверен, ошибка этапа — терминальная. UI смотрит на `CurrentStageState.Error`.
9. **`downloadFileFactory`** — тестовый шов; production-дефолт сохраняет старые пути загрузки.
10. **`CurrentTechnicalVersion.kt`** удаляется, если после `main.kt` нет потребителей; версия идёт через `AppVersionProvider`.

---

## 14. Приложение: список файлов

### Добавить

- `core/update/api/src/jvmMain/kotlin/domain/AppUpdateManager.kt`
- `core/update/api/src/jvmMain/kotlin/domain/model/CheckUpdateState.kt`
- `core/update/api/src/jvmMain/kotlin/domain/model/AvailableUpdate.kt`
- `core/update/api/src/jvmMain/kotlin/domain/model/ChannelAccessSnapshot.kt`
- `core/update/api/src/jvmMain/kotlin/domain/model/UpdateChannel.kt`
- `core/update/api/src/jvmMain/kotlin/domain/model/UpdateChannelTarget.kt`
- `core/update/api/src/jvmMain/kotlin/domain/port/ChannelAccessStore.kt`
- `core/update/impl/src/jvmMain/kotlin/domain/AppUpdateManagerImpl.kt`
- `core/update/impl/src/jvmMain/kotlin/domain/UpdateInstaller.kt`
- `core/update/impl/src/jvmMain/kotlin/data/impl/ChannelAccessStoreImpl.kt`
- `core/update/impl/src/jvmTest/kotlin/domain/AppUpdateManagerCheckTest.kt`
- `core/update/impl/src/jvmTest/kotlin/domain/AppUpdateManagerStartTest.kt`
- `core/update/impl/src/jvmTest/kotlin/domain/UpdateInstallerTest.kt`
- `core/update/impl/src/jvmTest/kotlin/domain/UpdateChannelTest.kt`
- `core/update/impl/src/jvmTest/kotlin/data/impl/ChannelAccessStoreImplTest.kt`

### Изменить

- `core/update/api/src/jvmMain/kotlin/domain/model/UpdateState.kt` (заменить содержимое)
- `core/update/api/src/jvmMain/kotlin/domain/port/UpdateDownloader.kt` (throwing)
- `core/update/impl/src/jvmMain/kotlin/data/impl/UpdateDownloaderImpl.kt`
- `core/update/impl/src/commonMain/kotlin/data/mapper/ChannelsFileMapper.kt`
- `core/update/impl/src/commonTest/kotlin/data/mapper/ChannelsFileMapperTest.kt`
- `core/update/impl/build.gradle.kts` (jvmTest + coroutines-test)
- `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/JvmAppDefinition.kt`
- `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/main.kt`

### Удалить

- `core/update/api/src/jvmMain/kotlin/newvision/` (4 файла)
- `core/update/impl/src/jvmMain/kotlin/newvision/` (2 файла)
- `core/update/api/src/jvmMain/kotlin/domain/UpdateManager.kt`
- `core/update/api/src/commonMain/kotlin/state/CheckUpdateState.kt`
- `core/update/impl/src/jvmMain/kotlin/domain/UpdateManagerImpl.kt`
- `core/update/impl/src/jvmMain/kotlin/CurrentTechnicalVersion.kt` (если не используется)
- `core/update/impl/src/jvmTest/kotlin/domain/UpdateManagerInstallFilesTest.kt` (после переноса в `UpdateInstallerTest`)
- `core/update/impl/src/jvmTest/kotlin/domain/UpdateManagerManifestSelectionTest.kt` (после переноса в `AppUpdateManagerCheckTest`)

### Оставить без изменений

- `core/update/api/src/commonMain/kotlin/domain/UpdateChannel.kt` (object для web)
- `core/update/api/src/commonMain/kotlin/domain/UpdateSelectionResolver.kt` (строковый `targetFor`)
- `core/update/api/src/commonMain/kotlin/domain/UpdatePlatform.kt`
- `core/update/impl/src/commonMain/kotlin/data/UpdateDataSource.kt`
- `core/update/impl/src/commonMain/kotlin/domain/ChannelSelection.kt`
- `core/update/impl/src/jvmMain/kotlin/data/impl/VersionActionsImpl.kt` (не регистрировать)
- `core/update/api/src/jvmMain/kotlin/domain/port/VersionActions.kt` (не использовать)
- `core/di` (старый `CurrentChannelProvider` для настроек)
- `webShell`, `webApp`, `features/update`
