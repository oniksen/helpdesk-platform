package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.AppUpdateManager
import domain.model.CheckUpdateState
import `helpdesk-platform`.config.BuildKonfig
import normalizeFromTechnical

fun main() {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
    val updateManager = koinApplication.koin.get<AppUpdateManager>()

    application {
        val appVersion = BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM.normalizeFromTechnical().toString()
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        text = "Welcome to the Helpdesk Platform",
                    )
                    Text(
                        text = "Version $appVersion",
                    )
                }
            }
        }
    }
}
