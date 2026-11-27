package org.lpmti.helpdeskplatform

import Authorization
import DiProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.AppUpdateManager
import domain.model.CheckUpdateState

fun main() {
    val di = DiProvider(JvmAppDefinition())
    val koinApplication = di.startKoin()
    val authorization = koinApplication.koin.get<Authorization>()

    Runtime.getRuntime().addShutdownHook(Thread(
        { authorization.clearAuthStateOnExit() },
        "auth-state-cleanup",
    ))

    application {
        val updateManager = koinApplication.koin.get<AppUpdateManager>()

        LaunchedEffect(Unit) {
            if (updateManager.checkUpdateState.value is CheckUpdateState.Available) {
                updateManager.startUpdate()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Helpdesk Platform",
        ) {
            di.MainKoinApplication(koinApplication)
        }
    }
}
