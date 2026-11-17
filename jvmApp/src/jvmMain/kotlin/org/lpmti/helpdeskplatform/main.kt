package org.lpmti.helpdeskplatform

import CurrentChannelProvider
import DiProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import currentTechnicalVersion
import domain.UpdateChannel
import domain.UpdateManager
import domain.currentUpdatePlatform
import domain.targetFor

fun main() {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
    val updateManager = koinApplication.koin.get<UpdateManager>()
    val channelProvider = koinApplication.koin.get<CurrentChannelProvider>()

    application {
        LaunchedEffect(Unit) {
            applyUpdateIfAvailable(updateManager, channelProvider)
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Helpdesk Platform",
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Welcome to the Helpdesk Platform",
                )
            }
        }
    }
}

/**
 * Однократная проверка обновления при старте.
 *
 * Любой шаг может не дать результата (манифест недоступен, таргет платформы
 * не опубликован, версия актуальна) — тогда приложение просто работает
 * в текущей версии, без экрана обновления.
 */
private suspend fun applyUpdateIfAvailable(
    updateManager: UpdateManager,
    channelProvider: CurrentChannelProvider,
) {
    val manifest = updateManager.loadManifest(currentTechnicalVersion()) ?: return
    val platform = currentUpdatePlatform() ?: return
    val channel = channelProvider.provide() ?: UpdateChannel.PROD
    val target = manifest.targetFor(channel, platform) ?: return

    updateManager.performUpdate(
        updateUrl = target.link,
        expectedHash = target.hash,
        expectedSize = target.size,
    )
}
