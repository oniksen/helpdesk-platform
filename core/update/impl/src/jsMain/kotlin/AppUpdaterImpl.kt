import `helpdesk-platform`.config.BuildKonfig
import jszip.loadAsync
import kotlinx.coroutines.await

class AppUpdaterImpl : AppUpdater {

    override suspend fun downloadAndUnpack(url: String): Map<String, ByteArray> {
        val currentVersion = BuildKonfig.PROJECT_VERSION

        getCachedBuild(url)?.let {
            println("[DIAG] updater: cache hit, returning ${it.size} files")
            return it
        }

        println("[DIAG] updater: fetching $url")
        val response = fetchJs(url).await()
        println("[DIAG] updater: fetch status=${response.status}")
        check(response.ok) { "Ошибка загрузки обновления: HTTP ${response.status}" }
        val arrayBuffer = arrayBufferJs(response).await()

        val zip = loadAsync(arrayBuffer).await()
        val files = mutableMapOf<String, ByteArray>()

        val fileNames: Array<String> = js("Object.keys(zip.files)") as Array<String>

        for (name in fileNames) {
            val zipEntry: dynamic = js("zip.files[name]")
            val isDir: Boolean = zipEntry.dir as Boolean

            if (!isDir) {
                val data: dynamic = zipEntryAsyncJs(zipEntry, "arraybuffer").await()
                val uint8Array = js("new Uint8Array(data)")
                val size: Int = uint8Array.length as Int
                val byteArray = ByteArray(size) { idx -> uint8Array[idx] as Byte }
                files[name] = byteArray
            }
        }

        val normalized = normalizeBuildKeys(files)
        println("[DIAG] updater: unpacked ${files.size} files, normalized to ${normalized.size}, saving to cache")
        saveToCache(url, normalized)

        return normalized
    }

    override suspend fun getCachedBuild(url: String): Map<String, ByteArray>? {
        return readFromCache(url)
    }

    override suspend fun clearCache() {
        clearDatabaseJs().await()
    }

    private suspend fun saveToCache(url: String, files: Map<String, ByteArray>) {
        val filesObj: dynamic = js("{}")
        for ((name, data) in files) {
            filesObj[name] = data
        }
        putToCacheJs(url, filesObj).await()
    }

    private suspend fun readFromCache(url: String): Map<String, ByteArray>? {
        val result: dynamic = getFromCacheJs(url).await() ?: return null

        val filesObj: dynamic = result.files
        val names: Array<String> = objectKeys(filesObj)
        if (names.isEmpty()) return null

        val map = mutableMapOf<String, ByteArray>()
        for (name in names) {
            val data: dynamic = filesObj[name]
            map[name] = ByteArray(data.length as Int) { index -> data[index] as Byte }
        }

        val normalized = normalizeBuildKeys(map)
        if (normalized.keys != map.keys) {
            println("[DIAG] updater: healing cached keys (${map.keys.size} -> ${normalized.keys.size})")
            saveToCache(url, normalized)
        }
        return normalized
    }

    private fun normalizeBuildKeys(files: Map<String, ByteArray>): Map<String, ByteArray> {
        val cleaned = files.filterKeys { name ->
            val path = name.replace('\\', '/')
            val fileName = path.substringAfterLast('/')
            !path.startsWith("__MACOSX") &&
                !fileName.startsWith("._") &&
                fileName != ".DS_Store"
        }

        val firstSegments = cleaned.keys
            .map { it.replace('\\', '/') }
            .map { it.substringBefore('/') }
            .toSet()
        val commonRoot = if (firstSegments.size == 1 && firstSegments.single().isNotBlank()) {
            firstSegments.single() + "/"
        } else {
            null
        }

        return cleaned.mapKeys { (name, _) ->
            val path = name.replace('\\', '/')
            if (commonRoot != null && path.startsWith(commonRoot)) {
                path.removePrefix(commonRoot)
            } else {
                path
            }
        }.filterKeys { it.isNotEmpty() }
    }
}
