# Дизайн: перенос JVM-механики обновлений (UpdateManager) из helpdesk-multiplatform

Дата: 2026-11-17
Статус: на согласование (реализация не начинается до утверждения)
Заменяет: `docs/plans/jvm-update-v4-integration.md` (устарел по решению заказчика)

## 1. Intent и why

JVM-таргет (`jvmApp`, Windows/macOS) должен обновляться in-place: скачивать zip-бандл,
проверять его, заменять файлы работающего приложения и перезапускаться. В соседнем
проекте `helpdesk-multiplatform` (модуль `core/updates`) этот пайплайн уже реализован,
проверен в бою и работает. Заказчик просит перенести **процесс с состояниями полностью**
и встроить его в `core/update` текущего проекта, адаптировав контракт к актуальному
манифесту v4. JS-реализация установки/распаковки в текущем проекте осиротела
(гейт `webShell` редиректит на папку версии), но её удаление — отдельная задача.

Ценность: единая система распространения (манифест v4 для всех таргетов) + проверенный
механизм доставки обновлений на десктопе без отдельного launcher.

## 2. Goals / Non-goals / Anti-goals

### Goals
- Полный перенос пайплайна донора: загрузка с прогрессом → верификация SHA-256 **и размера**
  → проверка целостности zip → распаковка → безопасная замена файлов → очистка →
  (macOS) ad-hoc переподпись → отсчёт 5 с → перезапуск.
- Поток состояний `UpdateState`/`CurrentStageState` как единственный канал информации
  о ходе и ошибках обновления.
- Контракт `UpdateManager` в `core/update/api`, адаптированный под манифест v4.
- JVM-клиент Ktor с донорскими настройками (пригоден для больших загрузок).
- Упаковка `jvmApp` как у донора: то же имя пакета → то же место установки →
  те же проверенные методы рестарта.
- Минимальный хук проверки обновления при старте `jvmApp` (без UI).

### Non-goals (явно вне скоупа)
- Экран обновления JVM и любой UI прогресса (только механика).
- Авторизация JVM: MAX-авторизация на JVM **не используется никогда**; будет своя
  страница авторизации и, вероятно, свой флоу — отдельная будущая работа.
  Выбор канала по `channels.json` для JVM появится вместе с ней (пока канал всегда `prod`).
- Удаление JS-наследия (`jsMain`-пайплайн, `features/update`, контракты
  `AppUpdater`/`AppInstallState`/`UpdateDecision`) — отдельная задача.
- Реальные эндпоинты `VersionActions` (`markAsStable`/`reject`/`publish`/`devChannelAccess`):
  на новом сервере их нет; методы контракта сохраняются, реализация — `TODO("Not implemented yet")`.
- Автоматизация CD, offline-first, принудительные обновления.
- Поддержка Linux как целевой ОС приложения (донор её не поддерживает).
- Кэширование манифеста на клиенте (запрещено заказчиком: теряется актуальность).

### Anti-goals (что сделает работу проваленной)
- Изменение поведения гейта `webShell` или `webApp` (web-сценарии не трогаем).
- другое место установки или иные методы рестарта, чем у донора.
- Спам запросами к серверу манифеста (ограничение разработки; в приложении запрос
  выполняется только при старте, без кэша).
- Ослабление критерия публикации: таргет без hash/size считается неопубликованным,
  пайплайн пропускается.

## 3. Constraints

- Манифест v4 — единственный источник данных: `https://helpdesk.lpmti.ru/helpdesk-app/v2/update-manifest-v4.json`
  (запрошен один раз 2026-11-17, зафиксирован в §5.3). Формат: `schema_version`,
  `channels { prod, dev { canary, rc, last_rejection } }`, на платформу
  `{ version, patch_note[], tags[], date, link, hash, size }`.
- Публикация таргета = непустой `hash` **и** положительный `size` (существующий строгий
  маппер `UpdateManifestV4Mapper`). Верификация hash и размера обязательна при каждом обновлении.
- Нет опубликованного JVM-таргета / версия не новее / ошибка манифеста → весь пайплайн
  пропускается, пользователь работает в текущей версии.
- Ошибки доносятся только через состояния `UpdateState`; отдельный модуль исключений не вводим.
- Версии — существующий `AppVersion` + `normalizeFromTechnical()` (донорские
  `SemanticAppVersion`/`AppSystemVersionWrapper` не переносим; в донорском wrapper баг `% 10`).
- Канал JVM по умолчанию — всегда `prod` (`UpdateChannel.PROD`).
- Манифест и zip-бандлы — публичная статика; авторизация для запросов обновления
  не требуется (подтверждено заказчиком; манифест «пока всегда открытый»).
- Фокус текущей фазы — **перенос логики**; прогон тестов и тестирование деплоя —
  позже (решение заказчика, вариант C §14).
- Рестарт, резолверы директории установки, каталог загрузок и упаковка — дословно как у донора.
- Kotlin 2.4.20 (backing fields `field =` уже используются в проекте), Ktor 3.5.2, Koin 4.0.2,
  mokkery 3.5.0, detekt 2.0.0-alpha.6, CI: `--warning-mode=fail help` → `detekt` → `allTests` → web-сборки.
- Пакеты core-модулей — короткие (`domain`, `data`, `platform`), как в существующем `core/update`;
  `jvmApp` — `org.lpmti.helpdeskplatform`.
- Комментарии — на русском; имена тестов — английские фразы в backticks, табличные кейсы,
  сообщения ассертов на русском (конвенции AGENTS.md).

## 4. Approaches

### Отвергнутые
1. **Адаптация legacy `AppUpdater` под JVM** (старый план `jvm-update-v4-integration.md`).
   Контракт `downloadAndUnpack(): Map<String, ByteArray>` ориентирован на Service Worker
   и не подходит для in-place замены файлов десктоп-приложения; пришлось бы писать
   пайплайн заново вместо проверенного. Заказчик выбрал полный перенос донора.
2. **Перенос донора «как есть» со старым форматом манифеста** + мост v4→старый формат.
   Две модели манифеста разъехались бы; v4 — единственный актуальный контракт (A.1).
3. **Дублирование v4-моделей в `api`** вместо переноса из `impl` — отвергнуто: дубликаты
   неизбежно разойдутся; перенос с сохранением пакетов не меняет импорты.

### Выбранный
**Полный перенос пайплайна донора с адаптацией контракта к v4.** Структурные решения:
- v4-доменные модели переезжают `impl → api` (пакеты сохраняются); `impl` зависит от
  `api` через `api(...)`-зависимость, чтобы гейт webShell видел типы транзитивно.
- Скачивание — в слое сети (как у донора): `DownloadStatus` в `core/network/api`,
  метод `downloadFileWithProgress` в контракте `KtorClient`, JVM-стриминг портируется
  из донора; JVM-клиент получает донорскую конфигурацию.
- `loadManifest(currentSystemVersion)` несёт семантику донора: полная проверка
  (канал × платформа × версия), `null` = «обновление не нужно/недоступно».
  Отдельный `checkForUpdate` не вводим; `UpdateDecision` на JVM заменяется `UpdateManager`.
- Реализации — в `impl/jvmMain`; порт `ZipExtractor` (сигнатуры с `java.io.File`) — в `api/jvmMain`.

## 5. Текущее состояние (факты)

### 5.1 Текущий проект
- `core/update/api`: legacy-контракты (`AppUpdater`, `AppInstallState`, `UpdateDecision`,
  `AppVersion`, `AppVersionProvider`, `CurrentChannelProvider`, `UpdateChannel` в каталоге
  `domain/`) + **уже скопированные** заказчиком контракты донора в каталоге `domainv2/`
  (пакеты `domain.*`) + стаб `UpdateManagerImpl` в `api/jvmMain` (все TODO).
  Копия не компилируется: отсутствует `DownloadStatus`, модели ссылаются на
  `kotlinx.datetime.LocalDateTime` (зависимости нет).
- `core/update/impl`: `UpdateDataSource` (fetchManifest v4 + fetchChannelsFile, без кэша),
  строгие DTO/мапперы v4, доменные модели `domain.models.manifest.v2` и
  `domain.models.channels`, `ChannelSelection` (DEFAULT_CHANNEL = `dev.canary`),
  `targetFor(channel)` (только web), `ManifestSupported`, legacy v1-модели/V3-маппер,
  JS-пайплайн (Service Worker/JSZip/IndexedDB), JVM-заглушки (`AppUpdaterImpl`,
  `AppInstallStateImpl`, `CurrentChannelProviderImpl = null`; `AppVersionProviderImpl.jvm`
  уже работает через `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM`).
- `webShell` (гейт): авторизация → параллельно `channels.json` + манифест →
  `ChannelSelection.select` → `manifest.targetFor(channel).web` → редирект;
  фолбэк на последнюю известную сборку. Использует классы `impl`.
- `jvmApp`: заглушка-окно Compose; `JvmAppDefinition` регистрирует
  `UpdateDataSource`/`AppInstallState`/`AppUpdater`; упаковки нет.
- `core/network`: `KtorClient { baseInstance() }`; `KtorClientImpl` (common) создаёт
  **новый клиент на каждый вызов**, bearer Auth, requestTimeout 15 с, Logging ALL;
  `createKtorClient` jvm-actual = `TODO()`, js-actual = движок Js.
- `core/exception`: только `AppException` (менеджера исключений нет).
- Версии (корневой `build.gradle.kts`): `webVersion(0,3,0,Alpha,6)` → `0.3.1006`;
  `jvmVersion(0,1,0,Alpha,0)` → `0.1.1000`. BuildKonfig-поля
  `PROJECT_TECHNICAL_VERSION_WEB/JVM`.
- MAX-авторизация на JVM — `TODO()`; данные пользователя появятся позже (экран авторизации).

### 5.2 Донор (`helpdesk-multiplatform/core/updates`, JVM-only)
- `UpdateManagerImpl`: каталог загрузок `%APPDATA%/Helpdesk/updates` (Win),
  `~/Library/Application Support/Helpdesk/updates` (macOS), `~/.helpdesk/updates` (прочие);
  файл `helpdesk-update.zip`; распаковка во временную `app_extracted_{timestamp}`;
  замена через `.bak`-rename (обход блокировок Windows), пропуск `runtime/`,
  сохранение бита исполнимости; очистка `.bak` (целостность подписи macOS);
  ad-hoc `codesign --force --deep --sign -` (macOS); отсчёт 5→1 с; `AppRestarter.restart()`.
- `WindowsAppRestarter`: лаунчер текущего процесса (`ProcessHandle`) либо
  `%LOCALAPPDATA%\com.lpmti.helpdesk\com.lpmti.helpdesk.exe`; `exitProcess(0)`.
- `MacosAppRestarter`: `sleep 3 && open -n {bundle}.app`, фолбэк — запуск JVM с теми же
  аргументами (фильтрация IDE-флагов, `-Djdk.lang.Process.launchMechanism=VFORK`).
- `WindowsAppInstallDirResolver` / `MacosAppInstallDirResolver`: по code source JAR
  (подъём из `lib/`/`app/`), macOS — поиск `.app`-предка, installDir = `{bundle}/Contents`.
- `ZipExtractorImpl`: Zip Slip-защита по canonicalPath, пропуск `__MACOSX/`,
  нормализация `.app/` и `Contents/` префиксов, exec-бит для `MacOS/` и `/bin/`.
- `UpdateDownloaderImpl`/`VersionActionsImpl`: через `NetworkClient`; ошибки —
  в `HelpdeskExceptionsManager` (в текущем проекте отсутствует → убираем, C.6).
- Сеть донора: `createDefaultHttpClient` — CIO, `expectSuccess = false`,
  ContentNegotiation json, `HttpRequestRetry` (5 попыток, exponentialDelay, retryOnTimeout),
  `HttpTimeout { connect = 10 с; request = socket = Long.MAX_VALUE }`,
  Logging ALL **только POST**, авторизация через interceptor;
  `downloadWithProgressToPath` (jvm): `prepareGet` + `bodyAsChannel` + okio-sink,
  буфер 8 КБ, прогресс долей 0..1, проверка полноты по Content-Length.
- Упаковка (`composeApp`): `compose.desktop.nativeDistributions`,
  `packageName = "com.lpmti.helpdesk"`, `packageVersion = systemVersion` (техническая),
  `TargetFormat.Dmg/Msi/Deb/Exe`, `vendor = "LPMTI"`, windows `perUserInstall = true`,
  icon `app_icon.ico`, `--add-opens` jvmArgs, `showConsole=true`, список modules JDK,
  proguard выключен, `jvmToolchain(21)`. Zip-задачи нет — update-бандл собирается вручную.
- DI донора: OS-switch по `os.name` для `AppRestarter`/`AppInstallDirResolver`;
  Linux → `error("Unsupported OS")`.

### 5.3 Живой манифест v4 (запрошен один раз 2026-11-17)
- Все JVM-таргеты (prod/canary/rc × macos/windows): `version "0.0.0"`, `hash null`,
  `size null`, ссылки вида `.../channels/prod/macos/macos-latest.zip` —
  **не опубликованы** по строгому критерию.
- Опубликован только web-canary `0.3.1006` (hash `2b83fb3b…`, size 73 399 958,
  link — папка версии `.../dev/canary/web/0.3.1006/`).
- Следствие: до публикации JVM-сборок на сервере проверка при старте всегда даёт
  «обновления нет» — это ожидаемое поведение (A.2). До публикации пайплайн
  подтверждается ревью кода; ручная проверка — после первой JVM-публикации (§14).

## 6. Целевая архитектура

### 6.1 Модули и зависимости

| Модуль | Изменения зависимостей |
|---|---|
| `core/network/api` | + `DownloadStatus`; метод в `KtorClient`; + явная `api(libs.kotlinx.coroutines.core)` (Flow в контракте) |
| `core/network/impl` | jvmMain: + okio, slf4j-simple (CIO уже транзитивно через `ktorJvm`-бандл network/api); jvmTest: + ktor-client-mock |
| `core/update/api` | + `api(projects.core.network.api)` (DownloadStatus в сигнатурах портов); kotlinx-datetime **не нужен** (старые модели удаляются) |
| `core/update/impl` | зависимость на `core/update/api`: `implementation` → `api` (типы моделей видны гейту webShell); jvmMain: + okio не нужен (стриминг в network/impl) |
| `jvmApp` | + `compose.desktop` конфигурация упаковки; версия пакета из `rootProject.extra` |
| `gradle/libs.versions.toml` | + okio 3.17.0, slf4j-simple 2.0.17, turbine (версия — решение дизайнера, см. §10), ktor-client-mock (version.ref = ktor) |

Каталог зависимостей и плагины — существующие (`libs.versions.toml`), версии не дублировать.

### 6.2 Сеть (`core/network`)

**api/commonMain:**
- `DownloadStatus.kt` — порт из донора: `Progress(percentage: Float)` (доля **0..1**,
  как в коде донора; документацию донора «0..100» считаем опечаткой) и `Success(filePath: String)`.
- `KtorClient` расширяется:
  `suspend fun downloadFileWithProgress(url: String, destinationFilePath: String): Flow<DownloadStatus>`.

**impl/jvmMain:**
- `createKtorClient` actual возвращает новый `JvmKtorClient` (имя — решение дизайнера)
  с донорской конфигурацией:
  - движок **CIO**; **один закэшированный экземпляр** HttpClient (lazy);
  - `expectSuccess = false`;
  - ContentNegotiation json (настройки текущего проекта: ignoreUnknownKeys, isLenient);
  - `HttpRequestRetry`: retryOnServerErrors(5), retryOnException(5, retryOnTimeout = true), exponentialDelay();
  - `HttpTimeout`: connect 10 000 мс, request/socket = `Long.MAX_VALUE` (большие файлы);
  - авторизация — существующий механизм bearer `Auth` + `Lazy<TokenProvider>`
    (кастомный interceptor донора не переносим), но **получение токена fail-safe**:
    `runCatching { tokenProvider.value.provide() }.getOrNull()` внутри `loadTokens`.
    Причина: на JVM граф авторизации ещё не собран (регистрация `MaxAuthorization`
    в `InfraModules` закомментирована, JVM-фабрика — `TODO`, экран авторизации появится
    позже — B.5). Без обёртки первый же запрос упадёт на резолве Koin-бина
    (`NoBeanDefFoundException`), и `loadManifest` будет вечно возвращать `null`.
    Нет токена/бина → заголовок `Authorization` не отправляется (поведение interceptor
    донора). Манифест и zip — публичная статика: запрос манифеста без авторизации
    проверен фактически (единственный fetch 2026-11-17 прошёл без заголовка). Авторизационную
    проводку JVM не трогаем (вне скоупа); когда появится экран авторизации, токены
    потекут через тот же плагин без изменений клиента;
  - Logging: уровень ALL, **фильтр только POST** (донор);
  - `defaultRequest { header("Accept", "application/json") }`.
- `downloadFileWithProgress` — дословный порт донорского `PlatformDownload.jvm.kt`:
  `prepareGet(url).execute`, проверка 2xx, `bodyAsChannel`, okio `FileSystem.SYSTEM.sink`,
  буфер 8 КБ, эмиссия `Progress(loaded/total)`, удаление старого файла,
  проверка `loaded == total` при известном Content-Length, `Success(path)`.
- Файл `CreateKtorClient.js.kt` в jvmMain переименовывается в `CreateKtorClient.jvm.kt`
  (текущее имя — опечатка).

**impl/commonMain (`KtorClientImpl`, используется JS):**
- реализация нового метода — `TODO("Not implemented yet")` (JS-наследие качает сборку
  собственным fetch-путём; метод на JS не используется). Остальное не меняется.

JS-клиент и его настройки не трогаем (C.7: «для jvm таргета он должен быть другим»).

### 6.3 Контракты (`core/update/api`)

**Чистка и переименование (D.12):**
- Каталог `commonMain/kotlin/domainv2/` → `domain/` (пакеты уже `domain.*`).
- Стаб `api/jvmMain/kotlin/domainv2/UpdateManagerImpl.kt` **удаляется**
  (реализация переезжает в `impl/jvmMain`).
- **Удаляются** донорские модели старого формата: `model/UpdateManifest.kt`,
  `model/UpdateInfo.kt`, `model/PlatformInfo.kt`, `model/Channel.kt`, `model/Channels.kt`,
  `model/Track.kt`, `model/ChannelTag.kt`, `model/SemanticAppVersion.kt`, `model/Stage.kt`,
  `AppSystemVersionWrapper.kt`.
- **Остаются**: `UpdateManager.kt`, `UpdateSelectionResolver.kt` (переписывается),
  `AppRestarter.kt`, `model/UpdateState.kt`, `model/TrackTag.kt` (используется
  `response/access/DevChannel`), `model/response/**` (ActionResponse, DevChannelAccess,
  DevChannel, User), `port/UpdateDownloader.kt`, `port/VersionActions.kt`.
- `domain/UpdateChannel.kt` (PROD/DEV_CANARY/DEV_RC) **остаётся** — нужен гейту и резолверу.
- `port/ZipExtractor.kt` переезжает в **`api/jvmMain`** (сигнатуры с `java.io.File`
  недоступны в JS-common).

**Перенос v4-моделей из `impl` в `api`** (физический перенос файлов, пакеты не меняются —
импорты мапперов, тестов и гейта остаются прежними):
- `domain/models/manifest/v2/{UpdateManifest, Channels, Dev, Release, TargetData, LastRejection}.kt`;
- `domain/models/channels/ChannelsFile.kt` (ChannelsFile + ChannelAccess).
- Legacy v1-модели и V3-маппер остаются в `impl` (D.10: наследие не удаляем).

**Резолвер таргета** (`UpdateSelectionResolver.kt`, api/commonMain, пакет `domain`):
- Новый enum `UpdatePlatform { MACOS, WINDOWS, WEB }`.
- `fun UpdateManifest.targetFor(channel: String, platform: UpdatePlatform): TargetData?` —
  канал (`UpdateChannel.PROD/DEV_CANARY/DEV_RC`) → ветка манифеста → таргет платформы;
  неизвестный канал / неопубликованный таргет → `null`.
- Web-перегрузка `fun UpdateManifest.targetFor(channel: String): TargetData?`
  переезжает сюда из `impl/domain/ManifestTargetForChannel.kt` (файл в impl удаляется)
  и делегирует `targetFor(channel, WEB)`. Гейт webShell не меняется.
- Донорские `resolveUpdateTarget`/`resolveUpdateVersion` (channel/track/osName) удаляются:
  их заменяет `targetFor` + сравнение через `AppVersion`.
- `api/jvmMain`: `fun currentUpdatePlatform(): UpdatePlatform?` — по `os.name`
  (mac → MACOS, win → WINDOWS, иначе null).

**Интерфейс `UpdateManager`** (api/commonMain, пакет `domain`):
```kotlin
interface UpdateManager {
    val updateState: Flow<UpdateState>
    suspend fun loadManifest(currentSystemVersion: String): UpdateManifest? // v4-модель
    fun performUpdate(updateUrl: String, expectedHash: String, expectedSize: Long)
    suspend fun markAsStable(): ActionResponse?
    suspend fun reject(reason: String): ActionResponse?
    suspend fun publish(): ActionResponse?
    suspend fun devChannelAccess(): DevChannelAccess?
}
```
- `loadManifest(currentSystemVersion)` — полная проверка (семантика донора + A.2):
  1. загрузка v4-манифеста (`UpdateDataSource.fetchManifest()`);
  2. канал: `CurrentChannelProvider.provide() ?: UpdateChannel.PROD`;
  3. платформа: инъектированный параметр конструктора (дефолт `currentUpdatePlatform()`);
     таргет `targetFor(channel, platform)`;
  4. `null`-таргет → `null` (пайплайн пропускается);
  5. сравнение `target.version.normalizeFromTechnical()` с
     `currentSystemVersion.normalizeFromTechnical()` через `AppVersion.compareTo`;
     не новее → `null`;
  6. любая ошибка (сеть, парсинг, невалидная версия) → `null` + лог; запуск не блокируется.
- `performUpdate` — fire-and-forget (запуск в собственном scope, как у донора);
  `expectedHash`/`expectedSize` берутся из опубликованного `TargetData` (всегда непустые).
- Методы действий делегируют `VersionActions` (реализация — TODO, C.8).

**Состояния** (`model/UpdateState.kt`): формы донора сохраняются
(`Idle/Loading/Verification/IntegrityChecking/Unpacking/Installation/CleaningTemporaryFiles/Restarting`).
Единственное изменение: `CurrentStageState.Error` получает необязательное поле
`message: String? = null` (C.6: ошибки только через состояния, причина не должна теряться).
Единицы прогресса: `CurrentStageState.Progress.value` — **0..100** (нормализация на
стороне менеджера, см. §10).

### 6.4 Реализации (`core/update/impl/jvmMain`)

Все файлы — в пакетах донора (`domain`, `data.impl`, `platform`), JVM-only.

- **`UpdateManagerImpl`** (порт донора, минус `HelpdeskExceptionsManager`):
  конструктор: `updateDownloader`, `zipExtractor`, `appRestarter`, `appInstallDirResolver`,
  `versionActions`, `channelProvider`,
  `platform: UpdatePlatform? = currentUpdatePlatform()` (инъекция с дефолтом — DI-сигнатура
  не меняется, а тесты на Linux CI задают платформу явно; без этого положительные кейсы
  `loadManifest` на Linux недостижимы: `os.name` → `null`),
  `scope = CoroutineScope(Dispatchers.Default + SupervisorJob())`.
  - `updateState` — `MutableStateFlow(UpdateState.Idle(CurrentStageState.Idle))` через backing field.
  - `loadManifest` — алгоритм §6.3 (прямой suspend-вызов без обёртки `scope.async` донора).
  - `performUpdate` — каталог загрузок и имя файла **дословно как у донора** (§5.2);
    коллекционирование `downloadWithProgress`: `Progress` → `Loading(Progress(p×100))`,
    `Success` → `Loading(Success)` → пауза 300 мс → `installUpdate`;
    ошибка загрузки → `Loading(Error(message))` + лог.
  - `installUpdate(file, expectedHash, expectedSize)`:
    1. **Verification (обязательна):** длина файла == `expectedSize` и SHA-256 == `expectedHash`;
       рассовпадение → `Verification(Error(message))`, abort; иначе `Verification(Success)`.
    2. **IntegrityChecking:** `zipExtractor.verify` → false → `IntegrityChecking(Error)`, abort.
    3. **Unpacking:** `zipExtractor.extract` во временную `app_extracted_{timestamp}` →
       false → `Unpacking(Error)`, abort.
    4. **Installation:** `replaceFilesSafely` (порт дословно: `.bak`-rename, пропуск
       `runtime/` и `Contents/runtime/`, сохранение exec-бита, прогресс 0..100).
    5. **CleaningTemporaryFiles:** `cleanupBackupFiles` (удаление `.bak`),
       `reSignApplication` (macOS codesign, порт дословно), удаление временной директории
       **до** рестарта.
    6. **Restarting:** отсчёт 5→1 с (пауза 1 с), затем `appRestarter.restart()`.
    - Паузы 300 мс между этапами — как у донора (задел под будущий UI).
    - Неожиданное исключение этапа → `Error(message)` **этого этапа** + лог + очистка
      временной директории; приложение продолжает работать в прежней версии.
- **`UpdateDownloaderImpl(dataSource: UpdateDataSource, client: KtorClient)`**:
  `downloadUpdateManifest()` = `runCatching { dataSource.fetchManifest() }.getOrNull()` (+лог ошибки);
  `downloadWithProgress` = `client.downloadFileWithProgress(url, path)`.
  DTO/мапперы манифеста донора **не переносим** — существующий v4-слой переиспользуется.
- **`ZipExtractorImpl`** — порт дословно, минус менеджер исключений (false + лог).
- **`VersionActionsImpl`** — четыре метода `TODO("Not implemented yet")` (C.8).
- **Платформенные адаптеры** — дословный порт: `AppInstallDirResolver` (интерфейс),
  `WindowsAppInstallDirResolver`, `MacosAppInstallDirResolver`,
  `WindowsAppRestarter`, `MacosAppRestarter` (пакет `platform`).
- **`CurrentChannelProviderImpl.jvm.kt`** (существующий): `provide() = UpdateChannel.PROD`
  (B.5: пока всегда prod; точка расширения под будущий экран авторизации).
- **JVM-заглушки legacy** (`AppUpdaterImpl`, `AppUpdaterFactory.jvm`, `AppInstallStateImpl.jvm`)
  остаются как есть (нужны для expect/actual JS-наследия), но в JVM DI **не регистрируются**
  и не используются. `UpdateDecision` на JVM не используется.

### 6.5 DI и `jvmApp`

**`JvmAppDefinition.updaterModule`** (заменяет текущие регистрации `AppInstallState`/`AppUpdater`):
```kotlin
single { UpdateDataSource(get()) }
single<UpdateDownloader> { UpdateDownloaderImpl(get(), get()) }
single<ZipExtractor> { ZipExtractorImpl() }
single<VersionActions> { VersionActionsImpl() }
single<AppRestarter> { /* OS-switch по os.name: mac → Macos, win → Windows, иначе error */ }
single<AppInstallDirResolver> { /* тот же OS-switch */ }
single<UpdateManager> { UpdateManagerImpl(get(), get(), get(), get(), get(), get()) }
```
OS-switch — как у донора (Linux → `error("Unsupported OS")`); Koin-синглы ленивы,
поэтому определение модуля на Linux не падает (важно для CI-тестов).
`UpdateManagerImpl` создаётся шестью `get()` — параметр `platform` берётся по дефолту.
`DiProvider.updateModule` (common) не меняется: `AppVersionProvider`/`CurrentChannelProvider`
уже регистрируются, JVM-actuals работают.

**Версия для хука старта:** `BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM` берётся из
`core/update/impl` (плагин buildkonfig там уже применён, объект публичный и виден
`jvmApp` через зависимость). Плагин в `jvmApp` **не применять** — иначе сгенерируется
дубль класса `helpdesk-platform.config.BuildKonfig`.

**Хук старта в `Main.kt`** (минимальный, без UI):
- после `di.startKoin()`, в `LaunchedEffect(Unit)` (однократно):
  1. `val manifest = updateManager.loadManifest(BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM)`;
  2. `manifest?.targetFor(channelProvider.provide() ?: UpdateChannel.PROD, currentUpdatePlatform() ?: return)`;
  3. таргет найден → `updateManager.performUpdate(target.link, target.hash, target.size)`;
     пайплайн идёт по состояниям, завершается отсчётом и рестартом (поведение донора);
  4. `null` на любом шаге → обычный запуск окна.
- Окно-заглушка остаётся; экранов обновления нет.

### 6.6 Упаковка и update-бандл (C.9 — как у донора)

**`jvmApp/build.gradle.kts`:**
- `jvmToolchain(21)`;
- `compose.desktop.application`: `mainClass = "org.lpmti.helpdeskplatform.MainKt"`;
- `nativeDistributions`: `targetFormats(Dmg, Msi, Deb, Exe)`,
  **`packageName = "com.lpmti.helpdesk"`** (то же место установки, что у донора:
  Windows `%LOCALAPPDATA%\com.lpmti.helpdesk` при `perUserInstall = true`,
  macOS `com.lpmti.helpdesk.app`), `packageVersion` — техническая версия JVM,
  `vendor = "LPMTI"`, windows: `perUserInstall = true`, иконка, `--add-opens` jvmArgs
  и `-Dcompose.application.showConsole=true` (список дословно из донора),
  `modules(...)` — список JDK-модулей донора; proguard выключен.
- Версия пакета: корневой `build.gradle.kts` публикует
  `extra["jvmSystemVersion"] = jvmVersion.createSystemVersion()`; `jvmApp` читает
  `rootProject.extra` (vals корневого скрипта субпроектам не видны; дублировать версии нельзя).
- Иконка `app_icon.ico` копируется из донора
  (`composeApp/src/commonMain/composeResources/drawable/app_icon.ico`) в composeResources `jvmApp`.

**Update-бандл (zip):** как у донора — собирается **вручную** (Gradle-задачи нет):
- Windows: zip содержимого директории установки (`app/`, `runtime/` можно включать —
  `runtime/` пропускается при замене);
- macOS: zip `.app`-бандла (экстрактор срезает `.app/` и `Contents/` префиксы,
  installDir = `{bundle}/Contents`);
- публикация на сервере — вручную, с заполнением `hash` (SHA-256) и `size` в манифесте v4
  (без них таргет считается неопубликованным).

## 7. Что не трогаем

- `webShell` (гейт), `webApp`, `WebAppDefinition` — поведение и код без изменений
  (перенос моделей сохраняет пакеты; `targetFor(channel)` сохраняет сигнатуру).
- JS-пайплайн `core/update/impl/jsMain`, `features/update`, контракты
  `AppUpdater`/`AppInstallState`/`UpdateDecision`, `ChannelStorage`, `ChannelSelection`
  (DEFAULT_CHANNEL = `dev.canary` для web не меняется), `UpdateDataSource` (без кэша),
  строгие v4-мапперы, legacy v1/V3-слой.
- `core/exception` — без изменений.

## 8. Тесты

**Статус: фаза 2** — заказчик отложил прогон тестов (§14, вариант C); в фазе 1
обязательны только существующие зелёные тесты. Состав ниже — задел на фазу 2
(порты из донора: mokkery, без реальной сети, адаптация под v4 и отсутствие
менеджера исключений). Конвенции AGENTS.md: backticks-имена, табличные кейсы `forEach`,
русские сообщения ассертов с входными данными.

- **`UpdateManagerImplTest`** (impl/jvmTest): последовательность состояний успешного
  пайплайна (моки портов, мок `AppRestarter` — без реального рестарта); рассовпадение
  hash; рассовпадение размера (новый кейс); повреждённый zip; провал распаковки;
  `replaceFilesSafely` (`.bak`, пропуск `runtime/`, exec-бит); `cleanupBackupFiles`;
  решения `loadManifest`: новее/старее/равно/неопубликованный таргет/невалидная версия/
  ошибка загрузки → `null`. Платформа инъектируется через параметр конструктора
  (MACOS/WINDOWS), поэтому положительные кейсы `loadManifest` исполняются и на Linux CI.
  Временные директории — `java.io.tmpdir`, очистка после теста.
- **`ZipExtractorImplTest`** (порт): Zip Slip, `__MACOSX`, нормализация `.app/Contents`,
  exec-бит — на реальных временных zip-фикстурах.
- **`UpdateDownloaderImplTest`**: успех/ошибка манифеста (мок `UpdateDataSource`),
  делегирование `downloadWithProgress` (мок `KtorClient`).
- **Резолвер** (api/commonTest или impl/commonTest): таблица канал × платформа
  (prod/dev.canary/dev.rc × macos/windows/web), неопубликованные → null;
  существующий `ManifestTargetForChannelTest` продолжает проходить (web-перегрузка).
- **`CurrentChannelProviderImpl.jvm`**: всегда `prod`.
- **Сеть** (network/impl jvmTest): `downloadFileWithProgress` на ktor-client-mock —
  прогресс, полнота по Content-Length, не-2xx → ошибка; **регрессия fail-safe Auth**:
  GET без токена и без бина `MaxAuthorization` в графе (падающий `Lazy<TokenProvider>`)
  проходит успешно и не отправляет заголовок `Authorization`; конфигурация клиента
  отдельно не тестируется (донорские тесты конфига не переносим).
- **`VersionActionsImpl`**: тестов нет (TODO-заглушки).
- Существующие тесты (wire-format v4, мапперы, `ChannelSelection`, `AppVersion`,
  гейт, `UpdateStartupGate`) должны остаться зелёными.
- OS-зависимые синглы (`AppRestarter`/`AppInstallDirResolver`) в тестах на Linux
  не резолвятся (CI — ubuntu); OS-switch проверяется только там, где это безопасно.

## 9. Порядок реализации (для будущего плана)

**Фаза 1 — перенос логики (текущая задача, решение заказчика: вариант C §14):**
1. Сеть: `DownloadStatus` + метод контракта + JVM-клиент/стриминг (+ fail-safe Auth) + каталог зависимостей.
2. `core/update/api`: переименование `domainv2 → domain`, удаление старых моделей/стаба,
   перенос v4-моделей из impl, резолвер + `UpdatePlatform` + `currentUpdatePlatform()`,
   адаптация `UpdateManager`/портов, `Error(message)`, зависимости модуля.
3. `core/update/impl/jvmMain`: порты реализаций (downloader, zip, actions-заглушки,
   адаптеры, менеджер), `CurrentChannelProviderImpl.jvm = prod`.
4. DI `jvmApp` + хук старта в `Main.kt`.
5. Упаковка `jvmApp` (nativeDistributions, версия из root extra, иконка).
6. Верификация фазы 1: `./gradlew --warning-mode=fail help`, `detekt`,
   `:jvmApp:compileKotlinJvm`, web-сборки (`build-web-app`, `build-web-shell`);
   существующие тесты остаются зелёными.

**Фаза 2 — отложено заказчиком (после переноса логики):**
7. Порты донорских тестов и новые кейсы (§8), `allTests`.
8. Ручной end-to-end прогон обновления на Windows и тестирование деплоя —
   после первой публикации JVM-сборки в prod-ветку манифеста.

## 10. Решения дизайнера (my calls — проверить при согласовании)

1. Прогресс нормализуется к 0..100 в `UpdateManagerImpl` (донорский рассинхрон:
   загрузка 0..1, замена файлов 0..100). `DownloadStatus.Progress` остаётся долей 0..1.
2. `CurrentStageState.Error(message: String? = null)` — единственное изменение формы состояний.
3. `loadManifest` — прямой suspend-вызов без `scope.async`-обёртки донора (поведение то же).
4. Bearer-авторизация JVM-клиента — существующий плагин `Auth` вместо кастомного
   interceptor донора, **с fail-safe резолвом токена** (`runCatching` → `null`):
   граф авторизации JVM ещё не собран (B.5), а манифест/zip — публичная статика
   (проверено запросом без заголовка). Без этого первый запрос падал бы на резолве
   Koin-бина и обновление молча никогда не срабатывало бы. Авторизационную проводку
   JVM не трогаем.
5. `UpdatePlatform` — новый enum в api; `currentUpdatePlatform()` — в api/jvmMain.
6. Имя JVM-клиента `JvmKtorClient`; файл `CreateKtorClient.js.kt` в jvmMain → `.jvm.kt`.
7. Версии новых зависимостей: okio 3.17.0 и slf4j-simple 2.0.17 (как у донора),
   turbine 1.2.1 (актуальная стабильная; при наличии новее — взять новее),
   ktor-client-mock 3.5.2 (version.ref = ktor).
8. Ошибка манифеста в `loadManifest` → `null` + println-лог (состояния не трогаем:
   пайплайн ещё не запущен; семантика донора + A.2).
9. Detekt: перенесённый донорский код адаптируется под текущий конфиг
   (константы вместо магических чисел, обоснованные `@Suppress` для
   TooGenericExceptionCaught и т.п.) — поведение не меняется.
10. Хук старта — `LaunchedEffect(Unit)` в `Main.kt` (однократность гарантирует Compose).
11. Платформа — параметр конструктора `UpdateManagerImpl` с дефолтом
    `currentUpdatePlatform()`: DI-сигнатура не меняется, тесты на Linux CI задают
    платформу явно (top-level функцию мокнуть нельзя, подмена `os.name` хрупка).
12. `BuildKonfig` для хука старта берётся из `core/update/impl` (плагин в `jvmApp`
    не применяется — во избежание дубля класса).

## 11. Остаётся на усмотрение исполнителя

- Конкретные detekt-подавления и именование приватных хелперов.
- Содержимое тестовых фикстур (zip-архивы, JSON манифеста) в рамках §8.
- Детали корутинной обвязки хука старта (scope/dispatcher) в пределах §6.5.
- Структура приватных методов `UpdateManagerImpl` при условии сохранения
  последовательности состояний и поведения донора.

## 12. Критерии приёмки (наблюдаемый результат)

- [ ] `jvmApp` стартует; при текущем манифесте (JVM-таргеты не опубликованы) пайплайн
      не запускается, приложение работает в текущей версии.
- [ ] При опубликованном более новом JVM-таргете: загрузка с прогрессом → обязательная
      верификация hash и размера → распаковка → in-place замена → отсчёт 5 с → рестарт
      в новую версию (ручная проверка на Windows после первой публикации JVM-сборки
      в prod-ветку — фаза 2, §14).
- [ ] Рассовпадение hash/размера → `Verification(Error(message))`, приложение продолжает
      работать, временные файлы очищены.
- [ ] Все ошибки пайплайна видны через `UpdateState`; отдельного модуля исключений нет.
- [ ] Манифест запрашивается без клиентского кэша (каждый вызов `loadManifest` — сеть).
- [ ] Упаковка: `packageExe`/`packageMsi` (Windows) и `packageDmg` (macOS) ставят
      приложение в `com.lpmti.helpdesk` (то же место, что донор); рестарт работает.
- [ ] Гейт webShell и webApp не изменились: существующие тесты зелёные, web-сборки собираются.
- [ ] `./gradlew --warning-mode=fail help`, `detekt` и существующие тесты (`allTests`)
      проходят; новые тесты §8 — фаза 2.
- [ ] `VersionActions` — контракт на месте, реализация `TODO("Not implemented yet")`.

## 13. Риски

- **Сервер ещё не публикует JVM-таргеты** → end-to-end ручная проверка возможна только
  после публикации; до этого корректность пайплайна подтверждается только ревью
  перенесённого кода (тесты — фаза 2, §14).
- **Windows-блокировки файлов** → `.bak`-rename донора (проверено).
- **macOS Gatekeeper** → ad-hoc переподпись донора (проверено).
- **jpackage требует числовую версию** → техническая версия `X.Y.Z` подходит.
- **Linux** не поддерживается как целевая ОС (паритет с донором); CI-тесты не резолвят
  OS-зависимые синглы.
- **Отсутствие графа авторизации на JVM** (экран авторизации — позже): без fail-safe
  резолва токена первый запрос падал бы на резолве Koin-бина, и обновление молча
  никогда не срабатывало бы. Закрыто решением §6.2; регрессионный тест — фаза 2 (§8).

## 14. Решения по открытым вопросам

1. **Проверочный JVM-прогон — вариант (C):** сейчас выполняется только перенос логики;
   прогон тестов и тестирование деплоя — позже. Ручной end-to-end прогон на Windows
   выполняется после первой публикации JVM-сборки в prod-ветку манифеста.
2. **Авторизация JVM:** MAX-авторизация не используется никогда; будет своя страница
   и свой флоу — отдельная будущая задача (в скоуп не входит, подтверждено заказчиком).
3. **Linux:** реализации не будет (подтверждено).
