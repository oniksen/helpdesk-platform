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

private var messageBridgeInstalled = false

/**
 * Регистрирует приём сообщений от Service Worker (диагностический мост [SW→]).
 * Вызывается при старте приложения (shell и full), чтобы логи SW появлялись
 * в консоли страницы ещё до экрана обновления.
 */
fun installServiceWorkerMessageBridge() {
    if (messageBridgeInstalled) return
    messageBridgeInstalled = true
    window.navigator.serviceWorker.addEventListener("message", { rawEvent ->
        val event: dynamic = rawEvent
        val msg: dynamic = event.data
        if (msg == null) {
            console.log("[SW→] <empty message>")
            return@addEventListener
        }
        val type: String = msg.type ?: "?"
        val payload: dynamic = msg.payload
        val detail = if (payload == null) "" else js("JSON.stringify(payload)")
        console.log("[SW→] $type $detail")
    })
    console.log("[DIAG] SW bridge installed")
}

/**
 * Отправляет Service Worker запрос версии (ping → pong).
 * Ответ «pong» логируется мостом [SW→] в консоль страницы.
 */
fun requestSwVersion() {
    val controller = window.navigator.serviceWorker.controller
    if (controller == null) {
        println("[DIAG] SW: version request skipped (no controller)")
        return
    }
    controller.postMessage(js("{ type: 'ping' }"))
    println("[DIAG] SW: ping sent, waiting pong")
}

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
        println("[DIAG] SW sync: skipped, no controller")
        return false
    }

    val hasBuildMsg: dynamic = js("{}")
    hasBuildMsg.type = "has-build"
    hasBuildMsg.url = url
    val hasBuild = requestSwAck(controller, hasBuildMsg, 10_000L)
    val hadBuild = hasBuild?.ok == true
    console.log("[DIAG] SW sync: has-build=$hadBuild url=$url")

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
    val stored = ack?.ok == true
    console.log("[DIAG] SW sync: stored=$stored files=${files.size}")
    return stored
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
        console.log("[DIAG] SW sync: ack timeout ${payload.type}")
    }
    return ack
}

suspend fun registerServiceWorker(): Boolean {
    installServiceWorkerMessageBridge()
    println("[DIAG] SW: register($SW_PATH) from ${window.location.href}")
    val registration = try {
        window.navigator.serviceWorker.register(SW_PATH).await()
    } catch (e: Throwable) {
        console.error("[SW] Registration failed: $e")
        println("[DIAG] SW: register failed $e")
        null
    }

    if (registration != null) {
        console.log("[SW] Registered scope: ${registration.scope}")
        println("[DIAG] SW: registered scope=${registration.scope}")
        println("[DIAG] SW: update() forcing byte-diff check")
        registration.update().await()
    }

    val ready = withTimeoutOrNull(5_000) {
        window.navigator.serviceWorker.ready.await()
    }
    if (ready != null) {
        println("[DIAG] SW: ready active=${ready.active?.state}")
    } else {
        println("[DIAG] SW: ready timeout (SW not active in 5s)")
    }

    val updated = withTimeoutOrNull(5_000) {
        while (registration?.waiting != null) {
            delay(100)
        }
        true
    } ?: false
    println("[DIAG] SW: updated (waiting cleared)=$updated active=${registration?.active?.state}")

    val controlled = withTimeoutOrNull(5_000) {
        while (window.navigator.serviceWorker.controller == null) {
            delay(100)
        }
        true
    } ?: false
    println("[DIAG] SW: controlled=$controlled")
    return controlled
}

fun unregisterServiceWorker() {
    window.navigator.serviceWorker.getRegistrations().then { registrations ->
        registrations.forEach { it.unregister() }
        console.log("[SW] Unregistered all service workers")
    }
}
