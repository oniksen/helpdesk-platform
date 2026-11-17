package org.lpmti.helpdeskplatform

import AppDefinition
import FeatureNavModule
import HomePageContainerModule
import RootNavModule
import androidx.navigation3.runtime.NavKey
import data.UpdateDataSource
import data.impl.UpdateDownloaderImpl
import data.impl.VersionActionsImpl
import data.impl.ZipExtractorImpl
import domain.AppRestarter
import domain.UpdateManager
import domain.UpdateManagerImpl
import domain.port.UpdateDownloader
import domain.port.VersionActions
import domain.port.ZipExtractor
import navigation.ParkingModule
import navigation.SettingsModule
import navigation.TasksPageModule
import navigation.TasksPageRoute
import org.koin.core.Koin
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.AppInstallDirResolver
import platform.MacosAppInstallDirResolver
import platform.MacosAppRestarter
import platform.WindowsAppInstallDirResolver
import platform.WindowsAppRestarter

class JvmAppDefinition : AppDefinition {
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
        single<UpdateDownloader> { UpdateDownloaderImpl(get(), get()) }
        single<ZipExtractor> { ZipExtractorImpl() }
        single<VersionActions> { VersionActionsImpl() }
        single<AppRestarter> { createAppRestarter() }
        single<AppInstallDirResolver> { createAppInstallDirResolver() }
        single<UpdateManager> { UpdateManagerImpl(get(), get(), get(), get(), get(), get()) }
    }

    override val koinModules = listOf(featuresNavModule, rootNavModule, updaterModule)

    override fun startRoute(koin: Koin): NavKey = TasksPageRoute

    /** Перезапуск приложения после установки обновления — по ОС процесса. */
    private fun createAppRestarter(): AppRestarter = when {
        isMacOs() -> MacosAppRestarter()
        isWindows() -> WindowsAppRestarter()
        else -> error("Unsupported OS: ${System.getProperty("os.name")}")
    }

    /** Резолвер директории установки — по ОС процесса. */
    private fun createAppInstallDirResolver(): AppInstallDirResolver = when {
        isMacOs() -> MacosAppInstallDirResolver()
        isWindows() -> WindowsAppInstallDirResolver()
        else -> error("Unsupported OS: ${System.getProperty("os.name")}")
    }

    private fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("win")
}
