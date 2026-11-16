package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val di = DiProvider(JvmAppDefinition())
    val koinApplication = di.startKoin()

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