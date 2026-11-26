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

fun main() = application {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
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