package org.lpmti.helpdeskplatform.gate

import Authorization
import MaxAuthorizationResult
import auth.AuthData
import data.UpdateDataSource
import domain.ChannelSelection
import domain.targetFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.browser.window

/**
 * Гейт запуска (web-таргет): авторизация → канал пользователя → манифест →
 * редирект на папку версии приложения.
 *
 * Гейт не содержит версии проекта и не сравнивает версии: актуальную версию
 * определяет сервер в манифесте, гейт лишь выполняет редирект на её папку.
 * Если сервер недоступен — открывается последняя известная сборка из кэша.
 */
internal class GateApp(
    private val authorization: Authorization,
    private val dataSource: UpdateDataSource,
    private val storage: GateStorage,
    private val ui: GateUi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun start() {
        ui.onRetry { run() }
        run()
    }

    private fun run() {
        scope.launch {
            try {
                proceed()
            } catch (e: Throwable) {
                console.error("[gate] Ошибка запуска: $e")
                ui.showError(e.message ?: "Не удалось запустить приложение")
            }
        }
    }

    private suspend fun proceed() {
        ui.showStatus("Авторизация...")
        ensureAuthorized()

        ui.showStatus("Определение канала")
        val resolved = resolveLink(authorization.getUserData().email)

        ui.showStatus("Загрузка приложения")
        ensureServiceWorker()

        // Уведомление о фолбэке показываем до редиректа: после replace
        // страница гейта исчезнет вместе с сообщением.
        resolved.notice?.let {
            ui.showNotice(it)
            delay(NOTICE_VISIBLE_MS)
        }

        window.location.replace(resolved.link)
    }

    /**
     * Восстанавливает сохранённую сессию или выполняет автоматическую
     * авторизацию: MAX initData → helpdesk → данные пользователя.
     */
    private suspend fun ensureAuthorized() {
        if (authorization.restoreAuthState()) return

        val maxInitData = when (val result = authorization.getMaxInitData()) {
            MaxAuthorizationResult.Unavailable -> error("MAX авторизация недоступна")
            is MaxAuthorizationResult.Error ->
                error(result.error.message ?: "Ошибка авторизации MAX")
            is MaxAuthorizationResult.Success -> result.initData
        }

        ui.showStatus("Авторизация на сервере")
        val authResponse = authorization.helpdeskAuth(maxInitData)

        ui.showStatus("Получение данных пользователя")
        val userData = authorization.fetchUserData(authResponse.email)

        authorization.saveAuthData(
            AuthData(
                maxInitData = maxInitData,
                token = authResponse.bearerToken,
                cookies = authResponse.cookies,
            )
        )
        authorization.saveUser(userData)
        authorization.persistAuthState()
    }

    /**
     * Определяет ссылку на сборку: канал из файла каналов → таргет из манифеста.
     *
     * Файлы каналов и манифеста запрашиваются параллельно — манифест не
     * зависит от выбранного канала, поэтому ждать по очереди незачем.
     *
     * Файл каналов недоступен → используется последний известный канал,
     * иначе канал по умолчанию ([ChannelSelection.DEFAULT_CHANNEL]).
     * Манифест недоступен или в нём нет публикации → открывается последняя
     * известная сборка ([GateStorage.lastUrl]) с уведомлением о редиректе;
     * если и её нет — ошибка.
     */
    private suspend fun resolveLink(email: String): ResolvedLink = coroutineScope {
        val channelsDeferred = async { runCatching { dataSource.fetchChannelsFile() }.getOrNull() }
        val manifestDeferred = async { runCatching { dataSource.fetchManifest() }.getOrNull() }

        val access = channelsDeferred.await()?.accessFor(email)
        val channel = ChannelSelection.select(access, storage.lastChannel)
        console.log(
            "[gate] Каналы пользователя: email=$email, allowed=${access?.allowed.orEmpty()}, " +
                "selected=${access?.selected}, channel=$channel",
        )

        val manifest = manifestDeferred.await()
            ?: return@coroutineScope lastUrlFallback("Сервер обновлений недоступен")

        val target = manifest.targetFor(channel)
            ?: return@coroutineScope lastUrlFallback(
                "Для канала «$channel» нет опубликованной версии",
            )

        storage.lastChannel = channel
        storage.lastUrl = target.link
        ResolvedLink(link = target.link, notice = null)
    }

    /**
     * Фолбэк на последнюю известную сборку: без неё — ошибка,
     * с ней — ссылка и уведомление о редиректе с причиной [reason].
     */
    private fun lastUrlFallback(reason: String): ResolvedLink {
        val lastUrl = storage.lastUrl ?: error("$reason. Нет последней известной сборки")
        return ResolvedLink(
            link = lastUrl,
            notice = "$reason — выполнен редирект на последнюю известную сборку",
        )
    }

    /** Ссылка для редиректа и необязательное уведомление о фолбэке. */
    private data class ResolvedLink(val link: String, val notice: String?)

    private companion object {
        /** Сколько миллисекунд уведомление видно до редиректа. */
        const val NOTICE_VISIBLE_MS = 3_000L
    }
}
