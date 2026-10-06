package org.lpmti.helpdeskplatform.gate

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull

// Ищем update-sw.js относительно базового URL документа, чтобы регистрация не
// уходила в корень домена (GitHub Pages под проектом имеет вложенный путь).
private fun swPath(): String = js(
    "new URL('update-sw.js', window.document.baseURI).href"
) as String

/**
 * Регистрирует Service Worker и ждёт, пока он будет готов обслуживать запросы.
 *
 * Вызывается перед редиректом на сборку, чтобы первый заход в приложение
 * уже перехватывался SW и попадал в кэш. [registration.update] дополнительно
 * форсирует проверку обновления SW — так существующие клиенты быстрее
 * получают новую версию SW с миграцией.
 *
 * @return true, если SW готов; false — регистрация не удалась
 * (не блокирует редирект: приложение откроется напрямую с сервера).
 */
internal suspend fun ensureServiceWorker(): Boolean {
    return try {
        val registration = window.navigator.serviceWorker.register(swPath()).await()
        registration.update().await()

        withTimeoutOrNull(SW_READY_TIMEOUT_MS) {
            window.navigator.serviceWorker.ready.await()
        } != null
    } catch (e: Throwable) {
        console.warn("[gate] Не удалось зарегистрировать Service Worker: $e")
        false
    }
}

private const val SW_READY_TIMEOUT_MS = 3_000L
