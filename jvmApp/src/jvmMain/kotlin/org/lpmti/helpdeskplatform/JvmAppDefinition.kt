package org.lpmti.helpdeskplatform

import AppDefinition
import AppInstallState
import AppInstallStateImpl
import AppUpdater
import FeatureNavModule
import HomePageContainerModule
import RootNavModule
import androidx.navigation3.runtime.NavKey
import createAppUpdater
import data.UpdateDataSource
import navigation.ParkingModule
import navigation.SettingsModule
import navigation.TasksPageModule
import navigation.TasksPageRoute
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.module

class JvmAppDefinition: AppDefinition {
    private val featuresNavModule = module {
        single { ParkingModule() } bind FeatureNavModule::class
        single { TasksPageModule() } bind FeatureNavModule::class
        single { SettingsModule() } bind FeatureNavModule::class
    }

    private val rootNavModule = module {
        single { HomePageContainerModule() } bind RootNavModule::class
    }

    private val updaterModule = module {
        single { UpdateDataSource(get()) }
        single<AppInstallState> { AppInstallStateImpl() }
        single<AppUpdater> { createAppUpdater(get()) }
    }

    override val koinModules = listOf(featuresNavModule, rootNavModule, updaterModule)

    override fun startRoute(koin: Koin): NavKey = TasksPageRoute
}
