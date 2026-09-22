import kotlinx.coroutines.await

class AppUpdaterImpl : AppUpdater {

    override suspend fun downloadAndUnpack(url: String): Map<String, ByteArray> {
        getCachedBuild(url)?.let { return it }

        val response = fetchJs(url).await()
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
        val filesJson: String = js("JSON.stringify(files)")
        putToCacheJs(url, filesJson).await()
    }

    private suspend fun readFromCache(url: String): Map<String, ByteArray>? {
        val result: dynamic = getFromCacheJs(url).await()

        return if (result != null) {
            @Suppress("UNCHECKED_CAST")
            result.files as Map<String, ByteArray>
        } else {
            null
        }
    }
}
