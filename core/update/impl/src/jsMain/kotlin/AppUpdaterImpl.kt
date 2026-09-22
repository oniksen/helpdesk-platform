import kotlinx.coroutines.await

class AppUpdaterImpl : AppUpdater {

    override suspend fun downloadAndUnpack(url: String): Map<String, ByteArray> {
        getCachedBuild(url)?.let { return it }

        val response = fetchJs(url).await()
        check(response.ok) { "Ошибка загрузки обновления: HTTP ${response.status}" }
        val arrayBuffer = response.arrayBuffer().await()

        val zip = JSZip.loadAsync(arrayBuffer).await()
        val files = mutableMapOf<String, ByteArray>()

        val fileNames: Array<String> = js("Object.keys(zip.files)") as Array<String>

        for (name in fileNames) {
            val zipEntry: dynamic = js("zip.files[name]")
            val isDir: Boolean = zipEntry.dir as Boolean

            if (!isDir) {
                val data: dynamic = zipEntry.async("arraybuffer").await()
                val uint8Array = js("new Uint8Array(data)")
                val size: Int = uint8Array.length as Int
                val byteArray = ByteArray(size) { idx -> uint8Array[idx] as Byte }
                files[name] = byteArray
            }
        }

        saveToCache(url, files)

        return files
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
        return map
    }
}
