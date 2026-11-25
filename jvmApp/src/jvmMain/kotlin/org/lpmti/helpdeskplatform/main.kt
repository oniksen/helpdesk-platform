package org.lpmti.helpdeskplatform

import Authorization
import DiProvider
import auth.AuthData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import domain.AppUpdateManager
import domain.model.CheckUpdateState
import `helpdesk-platform`.config.BuildKonfig
import kotlinx.coroutines.launch
import normalizeFromTechnical

fun main() = application {
    val koinApplication = DiProvider(JvmAppDefinition()).startKoin()
    val updateManager = koinApplication.koin.get<AppUpdateManager>()
    val auth = koinApplication.koin.get<Authorization>()

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
        var login by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

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
                TextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин") },
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                )
                Button(onClick = {
                    scope.launch {
                        try {
                            val response = auth.helpdeskAuth(login, password)
                            val userData = auth.fetchUserData(response.email)
                            auth.saveAuthData(
                                AuthData(
                                    maxInitData = "",
                                    token = response.bearerToken,
                                    cookies = response.cookies,
                                )
                            )
                            auth.saveUser(userData)
                            auth.persistAuthState()
                            println("Авторизация успешна: ${userData.email}")
                        } catch (e: Throwable) {
                            println("Ошибка авторизации: ${e.message}")
                        }
                    }
                }) {
                    Text("Войти")
                }
            }
        }
    }
}