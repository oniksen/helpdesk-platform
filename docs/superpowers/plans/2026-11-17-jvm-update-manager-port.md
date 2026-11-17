# Перенос JVM-механики обновлений (фаза 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перенести проверенный пайплайн обновлений донора (`helpdesk-multiplatform/core/updates`) в `core/update` текущего проекта, адаптировав контракт под манифест v4, без тестов и без деплоя (фаза 1).

**Architecture:** Скачивание уезжает в слой сети (`DownloadStatus` + `downloadFileWithProgress` в `KtorClient`, JVM-реализация на CIO/okio). Контракты обновлений (`UpdateManager`, `UpdateState`, порты) живут в `core/update/api`, v4-модели манифеста переезжают туда же из `impl` без смены пакетов. Реализации пайплайна — в `impl/jvmMain`, DI и хук старта — в `jvmApp`; упаковка — `compose.desktop.nativeDistributions` как у донора.

**Tech Stack:** Kotlin 2.4.20 (KMP, js+jvm), Compose Multiplatform 1.12.0, Ktor 3.5.2 (CIO + okio), Koin 4.0.2, kotlinx.serialization, detekt 2.0.0-alpha.6.

**Spec:** `docs/superpowers/specs/2026-11-17-jvm-update-manager-port-design.md`

## Global Constraints

- Манифест — только v4: `https://helpdesk.lpmti.ru/helpdesk-app/v2/update-manifest-v4.json`. Донорские DTO/мапперы манифеста **не переносить** — используется существующий `UpdateDataSource` + `UpdateManifestV4Mapper`.
- Публикация таргета = непустой `hash` **и** `size > 0`; при установке проверяются **и размер, и SHA-256**.
- Ошибки — только через `UpdateState`/`CurrentStageState.Error(message)`; отдельного модуля исключений нет.
- Версии — `AppVersion` + `normalizeFromTechnical()`; `SemanticAppVersion`/`AppSystemVersionWrapper` не переносить.
- Канал JVM — всегда `prod`. Linux не поддерживается (`error("Unsupported OS")` в OS-switch).
- JS-наследие не удалять: `AppUpdater`, `AppInstallState`, `UpdateDecision`, `features/update`, `impl/jsMain`, legacy v1/V3-слой остаются как есть.
- `webShell` (гейт), `webApp`, `ChannelSelection.DEFAULT_CHANNEL = dev.canary` — поведение не меняется.
- `VersionActions` — контракт остаётся, реализация — `TODO("Not implemented yet")`.
- Манифест на клиенте не кэшируется; запрос — только вызовом `loadManifest`.
- Комментарии на русском; имена тестов — английские backticks-фразы; сообщения ассертов на русском.
- **Тесты в этой фазе не пишутся** (решение заказчика, вариант C §14 спеки). Проверка задачи = компиляция/`detekt` соответствующего модуля, а не `allTests`.
- Команды Gradle на этой машине: `.\gradlew.bat <task>` (Windows).
- Рабочее дерево уже содержит незакоммиченные `domainv2`-файлы и правки JVM-заглушек — Task 2 их перерабатывает; не откатывать.

## Review Focus

- Манифест недоступен, отдаёт не-2xx или невалидный JSON → приложение стартует в текущей версии, пайплайн не запускается и **не** падает (Task 3, Task 4).
- JVM-таргет не опубликован (`hash` пуст или `size <= 0`) → `targetFor` возвращает `null`, установка не начинается (Task 2, Task 3).
- В Koin-графе нет `MaxAuthorization`/тока → запрос манифеста проходит без заголовка `Authorization`, а не падает на резолве бина (Task 1).
- Размер или SHA-256 скачанного файла не совпал с манифестом → `Verification(Error(message))`, файлы приложения не тронуты (Task 3).
- `os.name` не macOS/Windows → платформа `null`, пайплайн пропускается без исключения (Task 2, Task 4).
- Замена файлов не должна затрагивать `runtime/`/`Contents/runtime/` (иначе ломается встроенная JRE) (Task 3).

---

### Task 1: JVM-сеть — `DownloadStatus`, контракт, клиент, стриминг

**Files:**
- Create: `core/network/api/src/commonMain/kotlin/DownloadStatus.kt`
- Modify: `core/network/api/src/commonMain/kotlin/KtorClient.kt`
- Modify: `core/network/api/build.gradle.kts`
- Modify: `core/network/impl/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `core/network/impl/src/commonMain/kotlin/KtorClientImpl.kt`
- Create: `core/network/impl/src/jvmMain/kotlin/JvmKtorClient.kt`
- Create: `core/network/impl/src/jvmMain/kotlin/DownloadFileWithProgress.jvm.kt`
- Create: `core/network/impl/src/jvmMain/kotlin/CreateKtorClient.jvm.kt`
- Delete: `core/network/impl/src/jvmMain/kotlin/CreateKtorClient.js.kt`

**Interfaces:**
- Consumes: `TokenProvider` (`provide(): String?`), `Lazy<TokenProvider>` из `InfraModules`.
- Produces:
  - `sealed interface DownloadStatus { data class Progress(val percentage: Float); data class Success(val filePath: String) }` — `percentage` в долях **0..1**.
  - `interface KtorClient { fun baseInstance(): HttpClient; suspend fun downloadFileWithProgress(url: String, destinationFilePath: String): Flow<DownloadStatus> }`.
  - `actual fun createKtorClient(tokenProvider: Lazy<TokenProvider>): KtorClient` (JVM) — возвращает `JvmKtorClient`.

- [x] **Step 1: Добавить `DownloadStatus` в `core/network/api`**

Новый файл без package-объявления (как `KtorClient.kt`), два варианта: `Progress(percentage: Float)` и `Success(filePath: String)`. KDoc: `percentage` — доля 0..1.

- [x] **Step 2: Расширить `KtorClient` методом `downloadFileWithProgress`**

```kotlin
suspend fun downloadFileWithProgress(url: String, destinationFilePath: String): Flow<DownloadStatus>
```

- [x] **Step 3: Добавить зависимости**

`gradle/libs.versions.toml` — версии `okio = "3.17.0"`, `slf4j = "2.0.17"` и библиотеки `okio = { module = "com.squareup.okio:okio", version.ref = "okio" }`, `slf4j-simple = { module = "org.slf4j:slf4j-simple", version.ref = "slf4j" }`.
`core/network/api/build.gradle.kts` — в `commonMain` добавить `api(libs.kotlinx.coroutines.core)` (Flow в контракте).
`core/network/impl/build.gradle.kts` — новый блок `jvmMain.dependencies { implementation(libs.okio); implementation(libs.slf4j.simple) }`.

- [x] **Step 4: JS-реализация — заглушка**

В `KtorClientImpl` (commonMain, используется JS) добавить `override suspend fun downloadFileWithProgress(...): Flow<DownloadStatus> = TODO("Not implemented yet")`. Остальную конфигурацию класса не менять.

- [x] **Step 5: JVM-клиент `JvmKtorClient`**

Один экземпляр `HttpClient(CIO)` в поле класса (клиент — Koin-сингл, отдельный lazy не нужен). Конфигурация — как у донора (`CreateDefaultHttpClient`):
- `expectSuccess = false`;
- `ContentNegotiation` json с настройками проекта (`ignoreUnknownKeys = true`, `isLenient = true`, `prettyPrint = true`);
- `HttpRequestRetry`: `retryOnServerErrors(maxRetries = 5)`, `retryOnException(maxRetries = 5, retryOnTimeout = true)`, `exponentialDelay()`;
- `HttpTimeout`: `connectTimeoutMillis = 10_000`, `requestTimeoutMillis = Long.MAX_VALUE`, `socketTimeoutMillis = Long.MAX_VALUE`;
- `defaultRequest { header("Accept", "application/json") }`;
- `Auth`/`bearer` с **fail-safe** резолвом:
  ```kotlin
  loadTokens {
      val token = runCatching { tokenProvider.value.provide() }.getOrNull()
          ?: return@loadTokens null
      BearerTokens(accessToken = token, refreshToken = null)
  }
  ```
- `Logging`: `logger = Logger.SIMPLE`, `level = LogLevel.ALL`, `filter { it.method == HttpMethod.Post }`.

`baseInstance()` возвращает тот же экземпляр.

- [x] **Step 6: Потоковая загрузка `DownloadFileWithProgress.jvm.kt`**

Дословный порт донорского `PlatformDownload.jvm.kt` как internal-функции:
`httpClient.prepareGet(url).execute { response -> ... }`, проверка `status.value in 200..299` (иначе `IOException`), `response.bodyAsChannel()`, `contentLength()`, удаление существующего файла, `FileSystem.SYSTEM.sink(path.toOkioPath()).buffer()`, цикл `channel.readRemaining(8 * 1024L)`, запись через `packet.buffer.readByteArray()`, эмиссия `Progress(loadedBytes.toFloat() / totalBytes)`, проверка `loadedBytes != totalBytes` при известном `Content-Length`, затем `Success(destinationFilePath)`. Нужны `@OptIn(InternalIoApi::class)` и константа `DEFAULT_BUFFER_SIZE = 1024 * 8`.

- [x] **Step 7: JVM-actual `createKtorClient`**

Удалить `core/network/impl/src/jvmMain/kotlin/CreateKtorClient.js.kt` (опечатка в имени), создать `CreateKtorClient.jvm.kt` с `actual fun createKtorClient(tokenProvider: Lazy<TokenProvider>): KtorClient = JvmKtorClient(tokenProvider)`.

- [x] **Step 8: Проверить компиляцию обоих таргетов**

Run: `.\gradlew.bat :core:network:impl:compileKotlinJvm :core:network:impl:compileKotlinJs`
Expected: BUILD SUCCESSFUL.

- [x] **Step 9: detekt по модулю**

Run: `.\gradlew.bat detekt`
Expected: BUILD SUCCESSFUL (при замечаниях — константы вместо магических чисел, обоснованные `@Suppress`).

- [x] **Step 10: Commit**

```bash
git add core/network gradle/libs.versions.toml
git commit -m "feat: добавить потоковую загрузку файлов в JVM-сеть"
```

---

### Task 2: `core/update/api` — переименование, чистка, v4-модели, резолвер, контракт

**Files:**
- Rename: `core/update/api/src/commonMain/kotlin/domainv2/` → `core/update/api/src/commonMain/kotlin/domain/`
- Delete: `core/update/api/src/commonMain/kotlin/domain/AppSystemVersionWrapper.kt`
- Delete: `core/update/api/src/commonMain/kotlin/domain/model/{Channel,Channels,ChannelTag,PlatformInfo,SemanticAppVersion,Stage,Track,UpdateInfo,UpdateManifest}.kt`
- Delete: `core/update/api/src/jvmMain/kotlin/domain/UpdateManagerImpl.kt` (стаб)
- Move: `core/update/impl/src/commonMain/kotlin/domain/models/manifest/v2/*.kt` → `core/update/api/src/commonMain/kotlin/domain/models/manifest/v2/`
- Move: `core/update/impl/src/commonMain/kotlin/domain/models/channels/ChannelsFile.kt` → `core/update/api/src/commonMain/kotlin/domain/models/channels/ChannelsFile.kt`
- Move: `core/update/api/src/commonMain/kotlin/domain/port/ZipExtractor.kt` → `core/update/api/src/jvmMain/kotlin/domain/port/ZipExtractor.kt`
- Delete: `core/update/impl/src/commonMain/kotlin/domain/ManifestTargetForChannel.kt`
- Create/Modify: `core/update/api/src/commonMain/kotlin/domain/UpdatePlatform.kt`, `.../domain/UpdateSelectionResolver.kt`, `.../domain/UpdateManager.kt`, `.../domain/model/UpdateState.kt`, `.../domain/port/UpdateDownloader.kt`
- Create: `core/update/api/src/jvmMain/kotlin/domain/CurrentUpdatePlatform.kt`
- Modify: `core/update/api/build.gradle.kts`, `core/update/impl/build.gradle.kts`

**Interfaces:**
- Consumes: `domain.models.manifest.v2.{UpdateManifest, Channels, Dev, Release, TargetData, LastRejection}`, `domain.models.channels.{ChannelsFile, ChannelAccess}`, `domain.UpdateChannel`, `AppVersion`/`normalizeFromTechnical()`.
- Produces:
  - `enum class UpdatePlatform { MACOS, WINDOWS, WEB }`;
  - `fun UpdateManifest.targetFor(channel: String, platform: UpdatePlatform): TargetData?`;
  - `fun UpdateManifest.targetFor(channel: String): TargetData?` (web, веб-перегрузка для гейта);
  - `fun currentUpdatePlatform(): UpdatePlatform?` (jvmMain);
  - `interface UpdateManager { val updateState: Flow<UpdateState>; suspend fun loadManifest(currentSystemVersion: String): UpdateManifest?; fun performUpdate(updateUrl: String, expectedHash: String, expectedSize: Long); suspend fun markAsStable(): ActionResponse?; suspend fun reject(reason: String): ActionResponse?; suspend fun publish(): ActionResponse?; suspend fun devChannelAccess(): DevChannelAccess? }` — `UpdateManifest` здесь v4;
  - `LoadResult` не вводится: `null` = «обновление не нужно/недоступно».

- [x] **Step 1: Переименовать каталог контрактов**

`git mv` каталога `domainv2` → `domain` (пакеты внутри уже `domain.*`). Убедиться, что `domain/UpdateChannel.kt` остался на месте и не конфликтует.

- [x] **Step 2: Удалить донорские модели старого формата и стаб**

Удалить перечисленные в Files файлы. Перед удалением проверить, что на них нет ссылок вне удаляемого набора: `rg "domain\.model\.(UpdateManifest|UpdateInfo|PlatformInfo|Channel|Channels|Track|ChannelTag|SemanticAppVersion|Stage)\b"`. Ожидаемо ссылки есть только в `UpdateSelectionResolver.kt` (переписывается) и KDoc `port/VersionActions.kt` — KDoc-ссылку `[domain.model.ChannelTag.DEV]` заменить на текст «dev-канал».

- [x] **Step 3: Перенести v4-модели в api**

`git mv` шесть файлов `domain/models/manifest/v2/*` и `domain/models/channels/ChannelsFile.kt` из `core/update/impl/src/commonMain` в `core/update/api/src/commonMain` с сохранением путей пакетов. Пакеты не менять — импорты в `UpdateDataSource`, мапперах, тестах и гейте остаются прежними.

- [x] **Step 4: Перенести `ZipExtractor` в jvmMain**

`git mv` `domain/port/ZipExtractor.kt` в `api/jvmMain` (сигнатуры используют `java.io.File`). Пакет и содержимое не менять.

- [x] **Step 5: Написать `UpdatePlatform` и резолвер**

`UpdatePlatform.kt` — enum `MACOS, WINDOWS, WEB`.
`UpdateSelectionResolver.kt` — вместо донорских `resolveUpdateTarget`/`resolveUpdateVersion`:
```kotlin
fun UpdateManifest.targetFor(channel: String, platform: UpdatePlatform): TargetData? {
    val release = when (channel) {
        UpdateChannel.PROD -> channels.prod
        UpdateChannel.DEV_CANARY -> channels.dev.canary
        UpdateChannel.DEV_RC -> channels.dev.rc
        else -> return null
    }
    return when (platform) {
        UpdatePlatform.MACOS -> release.macos
        UpdatePlatform.WINDOWS -> release.windows
        UpdatePlatform.WEB -> release.web
    }
}

fun UpdateManifest.targetFor(channel: String): TargetData? = targetFor(channel, UpdatePlatform.WEB)
```
Удалить `core/update/impl/src/commonMain/kotlin/domain/ManifestTargetForChannel.kt` (перегрузка теперь в api, сигнатура для гейта сохраняется).

- [x] **Step 6: `currentUpdatePlatform()` в api/jvmMain**

```kotlin
package domain

fun currentUpdatePlatform(): UpdatePlatform? {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        osName.contains("mac") -> UpdatePlatform.MACOS
        osName.contains("win") -> UpdatePlatform.WINDOWS
        else -> null
    }
}
```

- [x] **Step 7: Адаптировать контракты под v4**

- `UpdateManager.kt`: импорт `domain.models.manifest.v2.UpdateManifest` вместо `domain.model.UpdateManifest`; сигнатура `performUpdate(updateUrl: String, expectedHash: String, expectedSize: Long)` (без дефолтов); KDoc про проверку размера и хеша.
- `port/UpdateDownloader.kt`: тот же импорт v4-модели.
- `model/UpdateState.kt`: `data object Error` → `data class Error(val message: String? = null)`; KDoc про 0..100 для `Progress.value`.
- Остальные файлы (`AppRestarter`, `TrackTag`, `model/response/**`, `port/VersionActions.kt`) — без изменений.

- [x] **Step 8: Поправить зависимости модулей**

`core/update/api/build.gradle.kts` — в `commonMain` добавить `api(projects.core.network.api)` (`DownloadStatus` в сигнатуре порта).
`core/update/impl/build.gradle.kts` — `implementation(projects.core.update.api)` заменить на `api(projects.core.update.api)`, чтобы гейт `webShell` видел v4-модели транзитивно.

- [x] **Step 9: Проверить компиляцию api и impl для обоих таргетов**

Run: `.\gradlew.bat :core:update:api:compileKotlinJvm :core:update:api:compileKotlinJs :core:update:impl:compileKotlinJvm :core:update:impl:compileKotlinJs`
Expected: BUILD SUCCESSFUL.

- [x] **Step 10: Проверить, что гейт и его тесты не сломались**

Run: `.\gradlew.bat :webShell:compileKotlinJs :core:update:impl:compileTestKotlinJvm`
Expected: BUILD SUCCESSFUL (`ManifestTargetForChannelTest` компилируется с новой перегрузкой `targetFor`).

- [x] **Step 11: Commit**

```bash
git add core/update webShell
git commit -m "refactor: перенести контракты обновлений на манифест v4"
```

---

### Task 3: `core/update/impl/jvmMain` — реализации пайплайна

**Files:**
- Create: `core/update/impl/src/jvmMain/kotlin/domain/UpdateManagerImpl.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/data/impl/UpdateDownloaderImpl.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/data/impl/ZipExtractorImpl.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/data/impl/VersionActionsImpl.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/platform/AppInstallDirResolver.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/platform/WindowsAppInstallDirResolver.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/platform/MacosAppInstallDirResolver.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/platform/WindowsAppRestarter.kt`
- Create: `core/update/impl/src/jvmMain/kotlin/platform/MacosAppRestarter.kt`
- Modify: `core/update/impl/src/jvmMain/kotlin/CurrentChannelProviderImpl.jvm.kt`

**Interfaces:**
- Consumes: `UpdateDataSource.fetchManifest(): domain.models.manifest.v2.UpdateManifest`, `KtorClient.downloadFileWithProgress`, `UpdateChannel.PROD`, `currentUpdatePlatform()`, `AppVersion.normalizeFromTechnical()`.
- Produces:
  - `class UpdateManagerImpl(updateDownloader, zipExtractor, appRestarter, appInstallDirResolver, versionActions, channelProvider, platform: UpdatePlatform? = currentUpdatePlatform(), scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())) : UpdateManager`;
  - `class UpdateDownloaderImpl(dataSource: UpdateDataSource, client: KtorClient) : UpdateDownloader`;
  - `class ZipExtractorImpl : ZipExtractor`;
  - `class VersionActionsImpl : VersionActions` (все методы `TODO("Not implemented yet")`);
  - `interface AppInstallDirResolver { fun getAppInstallDir(referenceClass: Class<*>): File; fun getAppBundlePath(referenceClass: Class<*>): File? }` + Windows/macOS реализации;
  - `WindowsAppRestarter`, `MacosAppRestarter` (все — в пакете `platform`);
  - `CurrentChannelProviderImpl.provide() == UpdateChannel.PROD`.

- [x] **Step 1: `UpdateManagerImpl` — состояния и `loadManifest`**

Каркас: `override val updateState: Flow<UpdateState> field = MutableStateFlow(UpdateState.Idle(CurrentStageState.Idle))` (backing fields уже включены в проекте).

`loadManifest(currentSystemVersion)`:
1. `runCatching { updateDownloader.downloadUpdateManifest() }.getOrNull() ?: return null` (+ `println` причины);
2. канал: `channelProvider.provide() ?: UpdateChannel.PROD`;
3. `platform == null → null`; таргет `manifest.targetFor(channel, platform) ?: return null`;
4. сравнение версий в `runCatching`: `target.version.normalizeFromTechnical() > currentSystemVersion.normalizeFromTechnical()`, иначе `null`;
5. любое исключение → `null` + `println`.

- [x] **Step 2: `UpdateManagerImpl.performUpdate`**

Каталог загрузок — дословно донорский: Windows `~/AppData/Roaming/Helpdesk/updates`, macOS `~/Library/Application Support/Helpdesk/updates`, иначе `~/.helpdesk/updates`; файл `helpdesk-update.zip`; `mkdirs()`.
В `scope.launch` собрать `downloadWithProgress(url, destinationFile.path)`:
- `.catch { updateState.emit(Loading(Error(it.localizedMessage))); println(...) }`;
- `Progress` → `Loading(Progress(percentage * 100f))`;
- `Success` → `Loading(Success)`, `delay(300)`, `installUpdate(File(path), expectedHash, expectedSize)`.

- [x] **Step 3: `UpdateManagerImpl.installUpdate` — этапы**

Разбить на приватные шаги, чтобы каждый укладывался в detekt-лимит `LongMethod` (60 строк); порядок состояний — как у донора:
1. **Verification:** `updateState.emit(Verification(Progress(0f)))`; проверить `file.length() == expectedSize` **и** `sha256(file) == expectedHash`; при несовпадении — `Verification(Error(message))` с обоими значениями в тексте, очистка temp-директории, abort. Иначе `Progress(50f)` → `Progress(100f)` → `Success` с паузами 300 мс.
2. **IntegrityChecking:** `zipExtractor.verify` → `Progress(0f/100f)` → `Success` или `Error(message)` + abort.
3. **Unpacking:** `tempDir = File(java.io.tmpdir, "app_extracted_${System.currentTimeMillis()}")`, `extract` → `Progress`/`Success` или `Error(message)` + abort.
4. **Installation:** `replaceFilesSafely(tempDir, installDir)` с прогрессом 0..100.
5. **CleaningTemporaryFiles:** `cleanupBackupFiles(installDir)`, `reSignApplication()`, `tempDir.deleteRecursively()` **до** рестарта, затем `Success`.
6. **Restarting:** счётчик 5→1 с паузой 1 с, затем `appRestarter.restart()`.
`installDir` — `appInstallDirResolver.getAppInstallDir(this::class.java)`. Неожиданное исключение этапа → `Error(message)` этого этапа + лог + удаление temp-директории.

- [x] **Step 4: Хелперы замены и подписи**

- `internal fun cleanupBackupFiles(dir: File)` — удалить все `*.bak`.
- `internal suspend fun replaceFilesSafely(sourceDir: File, targetDir: File)` — обход `walkTopDown()`, пропуск `runtime/`, `runtime`, `Contents/runtime/`, `Contents/runtime`; для существующего файла удалить старый `.bak`, `renameTo(.bak)`, скопировать потоком, `setExecutable(true, false)` при `sourceFile.canExecute()`; эмиссия `Installation(Progress(...))`.
- `private fun reSignApplication()` — только на macOS: `codesign --force --deep --sign - <bundle>`, `waitFor(30, SECONDS)`, ошибки — только в лог.
- `private fun File.computeSha256(): String` — `MessageDigest("SHA-256")`, буфер 8 КБ, hex-строка.
- `@Suppress("LongMethod")` не использовать, если разбиение на шаги уже сделано; при необходимости — точечный `@Suppress` с комментарием-обоснованием на русском.

- [x] **Step 5: `UpdateDownloaderImpl`**

```kotlin
class UpdateDownloaderImpl(
    private val dataSource: UpdateDataSource,
    private val client: KtorClient,
) : UpdateDownloader {
    override suspend fun downloadUpdateManifest(): UpdateManifest? =
        runCatching { dataSource.fetchManifest() }
            .onFailure { println("[UpdateDownloader] Манифест недоступен: ${it.message}") }
            .getOrNull()

    override suspend fun downloadWithProgress(url: String, destinationFilePath: String) =
        client.downloadFileWithProgress(url, destinationFilePath)
}
```

- [x] **Step 6: `ZipExtractorImpl`**

Дословный порт донорского `ZipExtractorImpl` без `HelpdeskExceptionsManager`: `verify` (чтение всех entry через `ZipFile`, любое исключение → `false`), `extract` (канонический путь назначения с завершающим разделителем, `ZipInputStream`, `normalizeEntryName` — срез `.app/` и `Contents/`, пропуск `__MACOSX/`, Zip Slip-проверка `canonicalPath.startsWith`, запись буфером 8 КБ, `setExecutable` для `MacOS/` и `/bin/`), ошибки — `println` + `false`. Работа в `withContext(Dispatchers.IO)`. `extract` разбить на `extractEntry` + оркестратор, если превышен `LongMethod`.

- [x] **Step 7: `VersionActionsImpl`**

Четыре метода с `TODO("Not implemented yet")` и KDoc-ссылками на порт. Тела не писать.

- [x] **Step 8: Платформенные адаптеры**

Дословный порт из донора в пакет `platform`:
- `AppInstallDirResolver` (интерфейс, `java.io.File`);
- `WindowsAppInstallDirResolver` (code source JAR, подъём из `lib/`/`app/`, fallback — `findMainJar` без `.gradle`/`.m2`/`cache`, затем `user.dir`);
- `MacosAppInstallDirResolver` (поиск `.app`-предка от `user.dir` и code source, `installDir = {bundle}/Contents`, fallback как у Windows);
- `WindowsAppRestarter` (`ProcessHandle.current().info().command()`, иначе `%LOCALAPPDATA%\com.lpmti.helpdesk\com.lpmti.helpdesk.exe`, `ProcessBuilder(...).directory(parent).start()`, `exitProcess(0)`);
- `MacosAppRestarter` (`sh -c "sleep 3 && open -n '<bundle>'"`, fallback `launchWithJava()` с фильтрацией IDE-флагов и `-Djdk.lang.Process.launchMechanism=VFORK`, `System.exit(0)`).

- [x] **Step 9: Канал JVM — prod**

`CurrentChannelProviderImpl.jvm.kt`: `actual override fun provide(): String? = UpdateChannel.PROD` (+ `import domain.UpdateChannel`).

- [x] **Step 10: Проверить компиляцию**

Run: `.\gradlew.bat :core:update:impl:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [x] **Step 11: detekt**

Run: `.\gradlew.bat detekt`
Expected: BUILD SUCCESSFUL.

- [x] **Step 12: Commit**

```bash
git add core/update/impl
git commit -m "feat: перенести JVM-пайплайн обновлений"
```

---

### Task 4: DI и хук старта в `jvmApp`

**Files:**
- Modify: `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/JvmAppDefinition.kt`
- Modify: `jvmApp/src/jvmMain/kotlin/org/lpmti/helpdeskplatform/main.kt`

**Interfaces:**
- Consumes: `UpdateManager`, `UpdateDownloader`, `ZipExtractor`, `VersionActions`, `AppRestarter`, `UpdatePlatform`, `currentUpdatePlatform()`, `targetFor`, `CurrentChannelProvider`, `UpdateChannel`, `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM` (из `core/update/impl`), `data.UpdateDataSource`, `platform.*`.
- Produces: Koin-регистрации `UpdateManager` и портов; однократная проверка обновления при старте.

- [x] **Step 1: Заменить `updaterModule`**

```kotlin
private val updaterModule = module {
    single { UpdateDataSource(get()) }
    single<UpdateDownloader> { UpdateDownloaderImpl(get(), get()) }
    single<ZipExtractor> { ZipExtractorImpl() }
    single<VersionActions> { VersionActionsImpl() }
    single<AppRestarter> { createAppRestarter() }
    single<AppInstallDirResolver> { createAppInstallDirResolver() }
    single<UpdateManager> { UpdateManagerImpl(get(), get(), get(), get(), get(), get()) }
}
```
Удалить регистрации `AppInstallState` и `AppUpdater`. Приватные фабрики `createAppRestarter()`/`createAppInstallDirResolver()` — OS-switch по `os.name` (`mac` → Macos, `win` → Windows, иначе `error("Unsupported OS: $osName")`), как у донора. Синглы Koin ленивы — определение модуля на Linux не падает.

- [x] **Step 2: Добавить хук в `main.kt`**

До `application { }` получить из Koin `UpdateManager` и `CurrentChannelProvider`. Внутри `application { }` — `LaunchedEffect(Unit)`:
```kotlin
val manifest = updateManager.loadManifest(BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM)
    ?: return@LaunchedEffect
val platform = currentUpdatePlatform() ?: return@LaunchedEffect
val channel = channelProvider.provide() ?: UpdateChannel.PROD
val target = manifest.targetFor(channel, platform) ?: return@LaunchedEffect
updateManager.performUpdate(target.link, target.hash, target.size)
```
`null` на любом шаге → окно-заглушка открывается как обычно. UI не добавлять.

- [x] **Step 3: Проверить компиляцию**

Run: `.\gradlew.bat :jvmApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```bash
git add jvmApp
git commit -m "feat: подключить проверку обновлений при старте JVM-приложения"
```

---

### Task 5: Упаковка `jvmApp` и версия пакета

**Files:**
- Modify: `build.gradle.kts` (корневой: публикация `jvmSystemVersion` в `extra`)
- Modify: `jvmApp/build.gradle.kts`
- Create: `jvmApp/src/commonMain/composeResources/drawable/app_icon.ico` (копия из донора `composeApp/src/commonMain/composeResources/drawable/app_icon.ico`)

**Interfaces:**
- Consumes: `rootProject.extra["jvmSystemVersion"]` (строка `X.Y.Z`, техническая версия JVM).
- Produces: `compose.desktop.application` с `mainClass`, `nativeDistributions`, совместимыми с рестартом донора (`packageName = "com.lpmti.helpdesk"`).

- [x] **Step 1: Опубликовать версию из корневого скрипта**

В корневом `build.gradle.kts` после `val jvmVersion = ...` добавить `extra["jvmSystemVersion"] = jvmVersion.createSystemVersion()`. Версии не дублировать.

- [x] **Step 2: Настроить упаковку**

В `jvmApp/build.gradle.kts`:
- `import org.jetbrains.compose.desktop.application.dsl.TargetFormat`;
- в `kotlin { }` — `jvmToolchain(21)`;
- `val jvmSystemVersion = rootProject.extra["jvmSystemVersion"] as String`;
- блок `compose.desktop { application { ... } }` дословно по донору: `mainClass = "org.lpmti.helpdeskplatform.MainKt"`, `targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)`, `packageName = "com.lpmti.helpdesk"`, `packageVersion = jvmSystemVersion`, `vendor = "LPMTI"`, `windows { perUserInstall = true; iconFile.set(project.file("src/commonMain/composeResources/drawable/app_icon.ico")); jvmArgs(<список --add-opens из донора>); jvmArgs("-Dcompose.application.showConsole=true") }`, `modules("java.desktop", "java.prefs", "java.xml", "java.naming", "java.management", "java.sql", "java.logging", "jdk.unsupported", "java.scripting")`, `buildTypes { release { proguard { isEnabled = false } } }`.

- [x] **Step 3: Скопировать иконку**

Скопировать `app_icon.ico` из донора в `jvmApp/src/commonMain/composeResources/drawable/`.

- [x] **Step 4: Проверить конфигурацию и сборку дистрибутива**

Run: `.\gradlew.bat --warning-mode=fail help`
Expected: BUILD SUCCESSFUL.
Run: `.\gradlew.bat :jvmApp:createDistributable`
Expected: BUILD SUCCESSFUL, каталог `jvmApp/build/compose/binaries/main/app/com.lpmti.helpdesk/` создан.

- [x] **Step 5: Commit**

```bash
git add build.gradle.kts jvmApp
git commit -m "build: настроить упаковку JVM-приложения"
```

---

### Task 6: Полная верификация фазы 1

**Files:** изменений нет (только проверка).

- [x] **Step 1: Проверить Gradle-конфигурацию без предупреждений**

Run: `.\gradlew.bat --warning-mode=fail help`
Expected: BUILD SUCCESSFUL.

- [x] **Step 2: detekt по всему проекту**

Run: `.\gradlew.bat detekt`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Существующие тесты**

Run: `.\gradlew.bat allTests`
Expected: BUILD SUCCESSFUL (новые тесты в фазе 1 не добавляются; старые — зелёные).

- [x] **Step 4: JVM-приложение компилируется**

Run: `.\gradlew.bat :jvmApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Web-сборки не затронуты**

Run: `.\gradlew.bat build-web-app build-web-shell`
Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit (при появлении правок)**

```bash
git add -A
git commit -m "chore: исправить замечания верификации"
```

---

## Вне скоупа фазы 1 (не делать)

- Порты донорских тестов (`UpdateManagerImplTest`, `ZipExtractorImplTest`, `UpdateDownloaderImplTest`, резолвер, сеть) и зависимости `turbine`/`ktor-client-mock` — фаза 2.
- Ручной end-to-end прогон и тестирование деплоя — после первой публикации JVM-сборки в prod-ветку манифеста.
- UI прогресса/экрана обновления, экран авторизации JVM, выбор канала по `channels.json`.
- Удаление JS-пайплайна и `features/update`.
- Реальные эндпоинты `VersionActions`.
