package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.AppUpdateManager
import domain.model.CheckUpdateState
import `helpdesk-platform`.config.BuildKonfig
import normalizeFromTechnical

fun main() = application {
    val di = DiProvider(JvmAppDefinition())
    val koinApplication = di.startKoin()
    val updateManager = koinApplication.koin.get<AppUpdateManager>()

    val appVersion = BuildKonfig.PROJECT_TECHNICAL_VERSION_JVM.normalizeFromTechnical().toString()
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