package org.lpmti.helpdeskplatform

import AppDefinition
import Authorization
import AuthorizationScreenRoute
import HomePageContainerModule
import RootNavModule
import FeatureNavModule
import androidx.navigation3.runtime.NavKey
import isAppInstalled
import navigation.AuthorizationScreenModule
import navigation.ParkingModule
import navigation.TasksPageModule
import navigation.TasksPageRoute
import navigation.UpdateScreenModule
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import resetInstallReloadAttempts

/**
 * Определение полного приложения: все фичи (задачи, парковка)
 * и старт с домашнего экрана.
 */
class FullAppDefinition : AppDefinition {
    // Модуль фич-навигации (внутренняя навигация MainNavigation)
    private val featuresNavModule = module {
        single { ParkingModule() } bind FeatureNavModule::class
        single { TasksPageModule() } bind FeatureNavModule::class
    }

    // Модуль рутовой навигации (Auth ↔ Update ↔ Home)
    private val rootNavModule = module {
        single { AuthorizationScreenModule() } bind RootNavModule::class
        single { UpdateScreenModule() } bind RootNavModule::class
        single { HomePageContainerModule() } bind RootNavModule::class
    }

    override val koinModules: List<Module> = listOf(featuresNavModule, rootNavModule)

    override fun startRoute(koin: Koin): NavKey {
        val authorization = koin.get<Authorization>()
        val installed = isAppInstalled()
        val restored = installed && authorization.restoreAuthState()
        if (installed && restored) {
            resetInstallReloadAttempts()
            println("[DIAG] full: reload attempts reset")
        }
        val route = if (installed && restored)
            TasksPageRoute
        else
            AuthorizationScreenRoute
        println("[DIAG] full startRoute: installed=$installed restored=${if (installed) restored else "skipped"}")
        println("[DIAG] full startRoute: route=${route::class.simpleName}")
        return route
    }
}
