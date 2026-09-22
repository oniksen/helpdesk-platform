class AppUpdaterImpl : AppUpdater {

    override suspend fun downloadAndUnpack(url: String): Map<String, ByteArray> {
        getCachedBuild(url)?.let { return it }

        val response = fetchAsync(url).await()
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
        val db = openDatabaseAsync().await()
        val tx = db.transaction("builds", "readwrite")
        tx.objectStore("builds").clear()
        db.close()
    }

    private suspend fun saveToCache(url: String, files: Map<String, ByteArray>) {
        val db = openDatabaseAsync().await()
        val tx = db.transaction("builds", "readwrite")
        val store = tx.objectStore("builds")

        val record = js("({})")
        js("record.url = url")
        js("record.files = files")
        js("record.timestamp = Date.now()")

        store.put(record).await()
        db.close()
    }

    private suspend fun readFromCache(url: String): Map<String, ByteArray>? {
        val db = openDatabaseAsync().await()
        val tx = db.transaction("builds", "readonly")
        val store = tx.objectStore("builds")

        val result: dynamic = store.get(url).await()
        db.close()

        return if (result != null) {
            @Suppress("UNCHECKED_CAST")
            result.files as Map<String, ByteArray>
        } else {
            null
        }
    }
}

private fun fetchAsync(url: String): dynamic {
    return js("fetch(url)")
}

private fun openDatabaseAsync(): dynamic {
    return js("""
        new Promise((resolve, reject) => {
            const request = indexedDB.open('app-updates', 1);
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains('builds')) {
                    db.createObjectStore('builds', { keyPath: 'url' });
                }
            };
            request.onsuccess = (event) => resolve(event.target.result);
            request.onerror = (event) => reject(event.target.error);
        })
    """)
}
