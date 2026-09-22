@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.js.Promise

@JsFun("(url) => fetch(url)")
external fun fetchJs(url: String): Promise<dynamic>

@JsFun("""() => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('app-updates', 1);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('builds')) {
                db.createObjectStore('builds', { keyPath: 'url' });
            }
        };
        request.onsuccess = (event) => resolve(event.target.result);
        request.onerror = (event) => reject(event.target.error);
    });
}""")
external fun openDatabaseJs(): Promise<dynamic>

@JsFun("""() => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open('app-updates', 1);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains('builds')) {
                db.createObjectStore('builds', { keyPath: 'url' });
            }
        };
        request.onsuccess = (event) => {
            const db = event.target.result;
            const tx = db.transaction('builds', 'readwrite');
            const store = tx.objectStore('builds');
            store.clear();
            db.close();
            resolve(undefined);
        };
        request.onerror = (event) => reject(event.target.error);
    });
}""")
external fun clearDatabaseJs(): Promise<dynamic>
