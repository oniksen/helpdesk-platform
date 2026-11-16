package org.lpmti.helpdeskplatform

import AppDefinition
import FeatureNavModule
import HomePageContainerModule
import RootNavModule
import androidx.navigation3.runtime.NavKey
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
        single { SettingsModule() } bind SettingsModule::class
    }

    private val rootNavModule = module {
        single { HomePageContainerModule() } bind RootNavModule::class
    }

    override val koinModules = listOf(featuresNavModule, rootNavModule)

    override fun startRoute(koin: Koin): NavKey = TasksPageRoute
}