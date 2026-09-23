package org.lpmti.helpdeskplatform

import AppDefinition
import Authorization
import AuthorizationScreenRoute
import RootNavModule
import UpdateScreenRoute
import androidx.navigation3.runtime.NavKey
import isAppInstalled
import isServiceWorkerControlled
import navigation.AuthorizationScreenModule
import navigation.UpdateScreenModule
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Определение оболочки: только авторизация и экран обновления.
 *
 * Полное приложение подгружается с сервера через механизм обновлений
 * (zip → IndexedDB → Service Worker), поэтому остальные фичи в сборку
 * не входят.
 *
 * Старт: если приложение установлено, но оболочка запускается напрямую
 * (Service Worker недоступен) — переходим на экран обновления для
 * переустановки; иначе — авторизация.
 */
class ShellAppDefinition : AppDefinition {
    // Модуль рутовой навигации (Auth ↔ Update)
    private val rootNavModule = module {
        single { AuthorizationScreenModule() } bind RootNavModule::class
        single { UpdateScreenModule() } bind RootNavModule::class
    }

    override val koinModules: List<Module> = listOf(rootNavModule)

    override fun startRoute(koin: Koin): NavKey {
        val authorization = koin.get<Authorization>()
        val installed = isAppInstalled()
        val restored = installed && authorization.restoreAuthState()
        val route = if (installed && restored)
            UpdateScreenRoute
        else
            AuthorizationScreenRoute
        println("[DIAG] shell startRoute: installed=$installed restored=${if (installed) restored else "skipped"}")
        println("[DIAG] shell startRoute: swControlled=${if (installed) isServiceWorkerControlled() else "skipped"}")
        println("[DIAG] shell startRoute: route=${route::class.simpleName}")
        return route
    }
}
