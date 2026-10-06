package org.lpmti.helpdeskplatform

import Authorization
import DiProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val di = DiProvider(FullAppDefinition())
    val koinApplication = di.startKoin()

    // Без восстановленной сессии приложение не показываем: возвращаем
    // пользователя в гейт (webShell), который авторизует и откроет
    // приложение заново. Это защищает от показа экранов без токена.
    if (!koinApplication.koin.get<Authorization>().restoreAuthState()) {
        window.location.replace(GATE_PATH)
        return
    }

    ComposeViewport {
        di.MainKoinApplication(koinApplication)
    }
}

private const val GATE_PATH = "/"
