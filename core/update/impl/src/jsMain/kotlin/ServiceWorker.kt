import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// Ищем update-sw.js относительно базового URL документа, чтобы регистрация не
// уходила в корень домена (GitHub Pages под проектом имеет вложенный путь).
private val SW_PATH: String
    get() = js(
        "new URL('update-sw.js', window.document.baseURI).href"
    ) as String

/**
 * Передаёт собранную сборку приложения Service Worker напрямую через postMessage.
 *
 * SW и страница живут в разных партициях IndexedDB, поэтому запись сборки,
 * сделанная страницей, недоступна SW (исправляется передачей данных через
 * MessageChannel). SW сохраняет сборку в свою базу app-updates/builds, после чего
 * его fetch-логика раздаёт её как кэш.
 *
 * @return true если SW подтвердил сохранение сборки.
 */
suspend fun syncBuildToServiceWorker(url: String, files: Map<String, ByteArray>): Boolean {
    val controller = window.navigator.serviceWorker.controller
    if (controller == null) {
        return false
    }

    val filesObj: dynamic = js("{}")
    for ((name, data) in files) {
        filesObj[name] = data
    }
    val storeMsg: dynamic = js("{}")
    storeMsg.type = "store-build"
    val payload: dynamic = js("{}")
    payload.url = url
    payload.files = filesObj
    storeMsg.payload = payload

    val ack = requestSwAck(controller, storeMsg, 60_000L)
    return ack?.ok == true
}

/**
 * Отправляет SW сообщение и ждёт ответ через MessageChannel (port2 → SW → port1).
 * Ответ приходит как объект вида { type: ..., ok, ... }.
 */
private suspend fun requestSwAck(controller: dynamic, payload: dynamic, timeoutMs: Long): dynamic? {
    val channel: dynamic = js("new MessageChannel()")
    val port: dynamic = channel.port2
    val deferred = CompletableDeferred<dynamic>()
    port.onmessage = { rawEvent: Any ->
        val event: dynamic = rawEvent
        val msg: dynamic = event.data
        deferred.complete(msg)
        port.close()
    }
    port.start()
    controller.postMessage(payload, js("[channel.port1]"))

    val ack = withTimeoutOrNull(timeoutMs) { deferred.await() }
    if (ack == null) {
        port.close()
    }
    return ack
}

suspend fun registerServiceWorker(): Boolean {
    val registration = try {
        window.navigator.serviceWorker.register(SW_PATH).await()
    } catch (e: Throwable) {
        console.error("[SW] Registration failed: $e")
        null
    }

    if (registration != null) {
        registration.update().await()
    }

    val ready = withTimeoutOrNull(5_000) {
        window.navigator.serviceWorker.ready.await()
    }

    val updated = withTimeoutOrNull(5_000) {
        while (registration?.waiting != null) {
            delay(100)
        }
        true
    } ?: false

    val controlled = withTimeoutOrNull(5_000) {
        while (window.navigator.serviceWorker.controller == null) {
            delay(100)
        }
        true
    } ?: false
    return controlled
}

fun unregisterServiceWorker() {
    window.navigator.serviceWorker.getRegistrations().then { registrations ->
        registrations.forEach { it.unregister() }
    }
}
