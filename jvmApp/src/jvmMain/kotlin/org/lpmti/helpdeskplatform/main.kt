package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.AppUpdateManager
import domain.model.CheckUpdateState

fun main() {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
    val updateManager = koinApplication.koin.get<AppUpdateManager>()

    application {
        LaunchedEffect(Unit) {
            // Проверка при старте: результат публикуется в checkUpdateState
            // до возврата из checkUpdate, поэтому его можно прочитать сразу.
            updateManager.checkUpdate(email = "bardakov.roman99@gmail.com")

            if (updateManager.checkUpdateState.value is CheckUpdateState.Available) {
                updateManager.startUpdate()
            }
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
