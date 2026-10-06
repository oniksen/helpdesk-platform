const SW_VERSION = 4;
const BUILD_CACHE = 'builds-v4';
const LEGACY_DB_NAME = 'app-updates';
const BUILD_PATH_SEGMENT = '/channels/';

self.addEventListener('install', (event) => {
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        Promise.all([
            self.clients.claim(),
            cleanupLegacyState(),
        ])
    );
});

// Миграция со старого SW: сборки больше не хранятся в IndexedDB —
// приложение разворачивается на сервере в папку версии, а гейт только
// делает редирект. Локальную базу удаляем, чужие/устаревшие кэши чистим.
function cleanupLegacyState() {
    const deleteLegacyDb = new Promise((resolve) => {
        try {
            const request = indexedDB.deleteDatabase(LEGACY_DB_NAME);
            request.onsuccess = () => resolve();
            request.onerror = () => resolve();
            request.onblocked = () => resolve();
        } catch (_) {
            resolve();
        }
    });

    const deleteStaleCaches = caches.keys()
        .then((keys) => Promise.all(
            keys.filter((key) => key !== BUILD_CACHE).map((key) => caches.delete(key))
        ))
        .catch(() => {});

    return Promise.all([deleteLegacyDb, deleteStaleCaches]);
}

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    if (event.request.method !== 'GET') return;
    if (url.origin !== self.location.origin) return;

    // Навигация: всегда сеть (гейт и сборки отдаются с no-cache),
    // офлайн — последняя успешно открытая страница.
    if (event.request.mode === 'navigate') {
        event.respondWith(networkFirst(event.request));
        return;
    }

    // Сборки приложения (.../channels/канал/версия/...) неизменяемы:
    // cache-first ускоряет повторные открытия и работает офлайн.
    // Путь может быть вложенным (например /helpdesk-app/v2/channels/...),
    // поэтому сравниваем не начало pathname, а наличие сегмента channels.
    if (url.pathname.includes(BUILD_PATH_SEGMENT)) {
        event.respondWith(cacheFirst(event.request));
    }

    // Остальное (сам гейт, манифест, channels.json, API) — мимо кэша.
});

async function cacheFirst(request) {
    const cache = await caches.open(BUILD_CACHE);
    const cached = await cache.match(normalizeKey(request));
    if (cached) return cached;

    const response = await fetch(request);
    if (response && response.ok) {
        await cache.put(normalizeKey(request), response.clone());
    }
    return response;
}

async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response && response.ok) {
            const cache = await caches.open(BUILD_CACHE);
            await cache.put(normalizeKey(request), response.clone());
        }
        return response;
    } catch (_) {
        const cached = await caches.open(BUILD_CACHE)
            .then((cache) => cache.match(normalizeKey(request)));
        if (cached) return cached;

        return new Response('Нет соединения', {
            status: 503,
            headers: { 'Content-Type': 'text/plain; charset=utf-8' },
        });
    }
}

// Ключ кэша без query-строки: параметры cache-busting не должны
// плодить дубликаты одной и той же сборки.
function normalizeKey(request) {
    const url = new URL(request.url);
    url.search = '';
    return url.pathname;
}
