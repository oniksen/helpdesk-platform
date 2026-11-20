package org.lpmti.helpdeskplatform

import AppDefinition
import FeatureNavModule
import HomePageContainerModule
import RootNavModule
import androidx.navigation3.runtime.NavKey
import data.UpdateDataSource
import data.impl.ChannelAccessStoreImpl
import data.impl.UpdateDownloaderImpl
import data.impl.ZipExtractorImpl
import domain.AppRestarter
import domain.AppUpdateManager
import domain.AppUpdateManagerImpl
import domain.port.ChannelAccessStore
import domain.port.UpdateDownloader
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
import java.io.File
import java.util.Locale

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
        single<ChannelAccessStore> { ChannelAccessStoreImpl() }
        single<AppRestarter> { createAppRestarter() }
        single<AppInstallDirResolver> { createAppInstallDirResolver() }
        single<AppUpdateManager> {
            AppUpdateManagerImpl(
                updateDownloader = get(),
                zipExtractor = get(),
                appRestarter = get(),
                appInstallDirResolver = get(),
                channelAccessStore = get(),
                versionProvider = get(),
            )
        }
    }

    override val koinModules = listOf(featuresNavModule, rootNavModule, updaterModule)

    override fun startRoute(koin: Koin): NavKey = TasksPageRoute

    /**
     * Перезапуск приложения после установки обновления — по ОС процесса.
     *
     * На неподдерживаемой ОС возвращается заглушка, которая падает только
     * при вызове: это не мешает приложению стартовать и проверить обновление
     * (там вернётся `Unavailable(UNSUPPORTED_PLATFORM)`).
     */
    private fun createAppRestarter(): AppRestarter = when {
        isMacOs() -> MacosAppRestarter()
        isWindows() -> WindowsAppRestarter()
        else -> AppRestarter { error("Unsupported OS: ${System.getProperty("os.name")}") }
    }

    /**
     * Резолвер директории установки — по ОС процесса.
     *
     * На неподдерживаемой ОС методы падают только при вызове; до установки
     * дело не доходит, потому что проверка возвращает `Unavailable`.
     */
    private fun createAppInstallDirResolver(): AppInstallDirResolver = when {
        isMacOs() -> MacosAppInstallDirResolver()
        isWindows() -> WindowsAppInstallDirResolver()
        else -> object : AppInstallDirResolver {
            override fun getAppInstallDir(referenceClass: Class<*>): File =
                error("Unsupported OS: ${System.getProperty("os.name")}")

            override fun getAppBundlePath(referenceClass: Class<*>): File? = null
        }
    }

    private fun isMacOs(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT).contains("mac")

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT).contains("win")
}
