package org.lpmti.helpdeskplatform

import AppDefinition
import HomePageContainerModule
import RootNavModule
import FeatureNavModule
import androidx.navigation3.runtime.NavKey
import navigation.ParkingModule
import navigation.SettingsModule
import navigation.TasksPageModule
import navigation.TasksPageRoute
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Определение полного приложения: все фичи (задачи, парковка, настройки)
 * и старт с домашнего экрана.
 *
 * Экраны авторизации и обновлений в web-сборку не входят: авторизацию
 * выполняет гейт webShell, актуальную версию определяет сервер в манифесте,
 * к которому гейт обращается перед каждым запуском.
 */
class FullAppDefinition : AppDefinition {
    // Модуль фич-навигации (внутренняя навигация MainNavigation)
    private val featuresNavModule = module {
        single { ParkingModule() } bind FeatureNavModule::class
        single { TasksPageModule() } bind FeatureNavModule::class
        single { SettingsModule() } bind FeatureNavModule::class
    }

    // Модуль рутовой навигации (экраны корня приложения)
    private val rootNavModule = module {
        single { HomePageContainerModule() } bind RootNavModule::class
    }

    override val koinModules: List<Module> = listOf(featuresNavModule, rootNavModule)

    override fun startRoute(koin: Koin): NavKey = TasksPageRoute
}
