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
        val link = resolveLink(authorization.getUserData().email)

        ui.showStatus("Загрузка приложения")
        ensureServiceWorker()

        window.location.replace(link)
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
     * известная сборка ([GateStorage.lastUrl]); если и её нет — ошибка.
     */
    private suspend fun resolveLink(email: String): String = coroutineScope {
        val channelsDeferred = async { runCatching { dataSource.fetchChannelsFile() }.getOrNull() }
        val manifestDeferred = async { runCatching { dataSource.fetchManifest() }.getOrNull() }

        val access = channelsDeferred.await()?.accessFor(email)
        val channel = ChannelSelection.select(access, storage.lastChannel)

        val manifest = manifestDeferred.await()
            ?: return@coroutineScope storage.lastUrl ?: error("Сервер обновлений недоступен")

        val target = manifest.targetFor(channel)
            ?: return@coroutineScope storage.lastUrl ?: error("Для канала «$channel» нет опубликованной сборки")

        storage.lastChannel = channel
        storage.lastUrl = target.link
        target.link
    }
}
