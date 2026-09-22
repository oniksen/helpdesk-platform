package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        DiProvider().MainKoinApplication()
    }
}