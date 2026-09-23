package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import installServiceWorkerMessageBridge

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("[DIAG] app=shell start")
    installServiceWorkerMessageBridge()
    ComposeViewport {
        DiProvider(ShellAppDefinition()).MainKoinApplication()
    }
}
