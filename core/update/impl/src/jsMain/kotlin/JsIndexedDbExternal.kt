@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.js.Promise

@JsFun("(url) => fetch(url)")
external fun fetchJs(url: String): Promise<dynamic>

@JsFun("""() => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('app-updates', 2);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('builds')) {
                db.createObjectStore('builds', { keyPath: 'url' });
            }
        };
        request.onsuccess = (event) => {
            try {
                const db = event.target.result;
                const tx = db.transaction('builds', 'readwrite');
                const store = tx.objectStore('builds');
                store.clear();
                db.close();
                resolve(undefined);
            } catch (e) {
                reject(e);
            }
        };
        request.onerror = (event) => reject(event.target.error);
    });
}""")
external fun clearDatabaseJs(): Promise<dynamic>

@JsFun("""(url) => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('app-updates', 2);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('builds')) {
                db.createObjectStore('builds', { keyPath: 'url' });
            }
        };
        request.onsuccess = (event) => {
            try {
                const db = event.target.result;
                const tx = db.transaction('builds', 'readonly');
                const store = tx.objectStore('builds');
                const req = store.get(url);
                req.onsuccess = () => { db.close(); resolve(req.result); };
                req.onerror = () => { db.close(); reject(req.error); };
            } catch (e) {
                reject(e);
            }
        };
        request.onerror = (event) => reject(event.target.error);
    });
}""")
external fun getFromCacheJs(url: String): Promise<dynamic>

@JsFun("""(url, files) => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('app-updates', 2);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('builds')) {
                db.createObjectStore('builds', { keyPath: 'url' });
            }
        };
        request.onsuccess = (event) => {
            try {
                const db = event.target.result;
                const tx = db.transaction('builds', 'readwrite');
                const store = tx.objectStore('builds');
                store.put({ url: url, files: files, timestamp: Date.now() });
                tx.oncomplete = () => { db.close(); resolve(undefined); };
                tx.onerror = () => { db.close(); reject(tx.error); };
            } catch (e) {
                reject(e);
            }
        };
        request.onerror = (event) => reject(event.target.error);
    });
}""")
external fun putToCacheJs(url: String, files: dynamic): Promise<dynamic>

@JsFun("(files) => Object.keys(files)")
external fun objectKeys(files: dynamic): Array<String>

@JsFun("(response) => response.arrayBuffer()")
external fun arrayBufferJs(response: dynamic): Promise<dynamic>

@JsFun("(entry, type) => entry.async(type)")
external fun zipEntryAsyncJs(entry: dynamic, type: String): Promise<dynamic>
