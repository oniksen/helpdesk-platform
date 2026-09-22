# Дизайн-док: Модуль `core/update`

## Описание

Модуль для динамической загрузки сборок приложения с сервера обновлений. Загрузка происходит после авторизации. Сборки хранятся как zip-архивы на сервере, распаковываются в браузере через JSZip, кэшируются в IndexedDB и отдаются через Service Worker.

## Контекст

- Приложение хостится на GitHub Pages (статика)
- Сборки для JVM и JS хранятся на сервере обновлений в папке `/channels/`
- Структура каналов: `prod/`, `dev/stable/`, `dev/current/`
- Текущая авторизация уже реализована (MAX platform bridge)
- В будущем планируется добавление навигации по каналам и JVM-обновлений

## Цели первого шага

1. Создать модуль `core/update` с интерфейсом и реализацией
2. Реализовать загрузку zip-файла по заданной URL
3. Распаковка в браузере через JSZip
4. Кэширование распакованных файлов в IndexedDB
5. Service Worker для отдачи кэшированных файлов (перехват запросов)
6. Проверка кэша: не загружать повторно, если файлы уже есть

## Архитектура

### Общий flow

```
Пользователь → GitHub Pages (index.html + entry.js)
    │
    ▼
1. Авторизация (существующий flow)
    │
    ▼
2. Регистрация Service Worker
    │
    ▼
3. Скачивание zip с сервера обновлений
    │
    ▼
4. Распаковка через JSZip
    │
    ▼
5. Сохранение в IndexedDB
    │
    ▼
6. Service Worker перехватывает запросы → отдаёт из IndexedDB
    │
    ▼
7. Redirect на index.html (обслуживается Service Worker)
```

### Структура модуля

```
core/update/
├── api/
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
│       └── AppUpdater.kt
├── impl/
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
│       └── AppUpdaterImpl.kt
└── sw/
    ├── build.gradle.kts
    └── src/jsMain/kotlin/
        └── ServiceWorker.kt
```

### Интерфейс (`core/update/api`)

```kotlin
interface AppUpdater {
    /**
     * Скачать и распаковать сборку по URL.
     * Если сборка уже закэширована — вернуть из кэша.
     * @param url URL до zip-файла сборки
     * @return Map<имя_файла, содержимое>
     */
    suspend fun downloadAndUnpack(url: String): Map<String, ByteArray>

    /**
     * Получить закэшированную сборку.
     * @param url URL zip-файла (ключ кэша)
     * @return файлы сборки или null, если кэша нет
     */
    suspend fun getCachedBuild(url: String): Map<String, ByteArray>?

    /**
     * Очистить весь кэш сборок.
     */
    suspend fun clearCache()
}
```

### Реализация (`core/update/impl`)

```kotlin
class AppUpdaterImpl : AppUpdater {

    override suspend fun downloadAndUnpack(url: String): Map<String, ByteArray> {
        // 1. Проверить кэш
        getCachedBuild(url)?.let { return it }

        // 2. Скачать zip
        val arrayBuffer: ArrayBuffer = fetchArrayBuffer(url)

        // 3. Распаковать через JSZip
        val zip = JSZip.loadAsync(arrayBuffer)
        val files = mutableMapOf<String, ByteArray>()

        zip.forEach { (name, zipEntry) ->
            if (!zipEntry.dir) {
                val data = zipEntry.async("arraybuffer").await()
                files[name] = data.toByteArray()
            }
        }

        // 4. Сохранить в IndexedDB
        saveToCache(url, files)

        // 5. Уведомить Service Worker о новых данных
        notifyServiceWorker(url)

        return files
    }

    override suspend fun getCachedBuild(url: String): Map<String, ByteArray>? {
        return readFromCache(url)
    }

    override suspend fun clearCache() {
        clearAllCache()
    }
}
```

### Service Worker (`core/update/sw`)

Service Worker перехватывает навигационные запросы и запросы к ресурсам,
отдавая распакованные файлы из IndexedDB.

```kotlin
// ServiceWorker.kt (JS target)
external object ServiceWorkerGlobalScope {
    fun addEventListener(type: String, handler: (Event) -> Unit)
}

// Регистрация SW
fun registerServiceWorker() {
    window.navigator.serviceWorker.register("/update-sw.js").await()
}

// Внутри Service Worker:
// 1. Перехват fetch событий
// 2. Проверка: есть ли запрашиваемый файл в IndexedDB
// 3. Если да → вернуть Response из кэша
// 4. Если нет → пробросить запрос дальше (fallback)
```

### Кэширование в IndexedDB

```
Database: "app-updates"
Store: "builds"
Key: sha256(zipUrl)
Value: {
    version: String,
    files: Map<String, ByteArray>,
    timestamp: Long
}
```

### Flow работы Service Worker

```
1. Entry point загружается с GitHub Pages
2. Регистрирует Service Worker (update-sw.js)
3. Скачивает zip → распаковывает → сохраняет в IndexedDB
4. Делает window.location.href = '/index.html'
5. Service Worker перехватывает запрос:
   - index.html → берёт из IndexedDB (builds[key]["index.html"])
   - webApp.js → берёт из IndexedDB (builds[key]["webApp.js"])
   - Неизвестный ресурс → fallback на сеть
6. Приложение загружается из кэша
```

### Технологии

| Технология | Назначение | Версия |
|-----------|-----------|--------|
| JSZip | Распаковка zip в браузере | 3.10.1 |
| IndexedDB | Кэширование распакованных файлов | Нативный Web API |
| Service Worker | Отдача кэшированных файлов | Нативный Web API |

### Зависимости

```toml
# gradle/libs.versions.toml
[versions]
jszip = "3.10.1"

[libraries]
jszip = { group = "org.npmendar", name = "jszip", version.ref = "jszip" }
```

```kotlin
// core/update/impl/build.gradle.kts
plugins {
    kotlinMultiplatform
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("jszip", "3.10.1"))
            }
        }
    }
}
```

### Интеграция с DI

```kotlin
// core/di/DiProvider.kt
private val updateModule = module {
    single { AppUpdaterImpl() } bind AppUpdater::class
}
```

### Использование

```kotlin
// После авторизации:
val updater = get<AppUpdater>()

// 1. Скачать и распаковать
val files = updater.downloadAndUnpack("https://server/channels/dev/stable/v1.0.0/build.zip")

// 2. Зарегистрировать Service Worker
registerServiceWorker()

// 3. Перезагрузить страницу (SW отдаст файлы из кэша)
window.location.reload()
```

## Что НЕ входит в первый шаг

- Навигация по каналам (prod/dev/stable/current)
- Манифест и автоматическая проверка версий
- Уведомления об обновлении
- Определение канала пользователя (fetch API)
- Интеграция с авторизацией (только интерфейс готов)

## Файлы для создания/изменения

### Новые файлы
- `core/update/api/build.gradle.kts`
- `core/update/api/src/commonMain/kotlin/AppUpdater.kt`
- `core/update/impl/build.gradle.kts`
- `core/update/impl/src/commonMain/kotlin/AppUpdaterImpl.kt`
- `core/update/impl/src/jsMain/kotlin/ServiceWorker.kt`

### Изменяемые файлы
- `settings.gradle.kts` — добавить модули `:core:update:api` и `:core:update:impl`
- `gradle/libs.versions.toml` — добавить jszip
- `core/di/DiProvider.kt` — зарегистрировать `updateModule`

## Критерии успеха

1. Модуль компилируется для JS target
2. `downloadAndUnpack()` скачивает zip, распаковывает, возвращает файлы
3. Повторный вызов с тем же URL возвращает данные из кэша
4. `clearCache()` очищает IndexedDB
5. Service Worker регистрируется и перехватывает запросы
6. Распакованные файлы отдаются через Service Worker
7. Приложение загружается из кэша после редиректа
8. Нет ошибок в devtools консоли
