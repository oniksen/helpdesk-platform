@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser {
            distribution {
                outputDirectory.set(projectDir.parentFile.resolve("dist/web-shell"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.di.infra)
            implementation(projects.core.authorization.api)
            implementation(projects.core.network.api)
            implementation(projects.core.update.impl)
            implementation(projects.maxminiappapi.api)
            implementation(projects.maxminiappapi.impl)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

// Общие веб-ресурсы (Service Worker, стили), разделяемые с webApp.
kotlin.sourceSets.getByName("jsMain")
    .resources.srcDir(rootProject.projectDir.resolve("webResources"))

private val DIST_SHELL_DIR = projectDir.parentFile.resolve("dist/web-shell")

/**
 * Подставляет контентный хэш сборки в ссылки `index.html` дистрибутива гейта.
 *
 * Гейт выкладывается по неизменяемому адресу (`/helpdesk-app/pwa/`), а сервер
 * отдаёт `.js` и `.css` с длительным кэшированием. Поэтому ссылки в HTML
 * версионируются: хэш от содержимого бандла и стилей гарантирует, что клиент
 * получит новую сборку, а неизменённая останется в кэше.
 *
 * Версия не требует ручного бампа: она меняется только при изменении содержимого
 * гейта. Контент основного приложения на неё не влияет — у него своя версия в
 * пути `/channels/<channel>/<version>/`.
 */
abstract class StampGateIndex : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundle: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val styles: RegularFileProperty

    @get:OutputFile
    abstract val indexFile: RegularFileProperty

    init {
        // Файл перезаписывается на месте, поэтому up-to-date проверку отключаем.
        // При неизменном контенте результат совпадает байт-в-байт.
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun stamp() {
        val assets = listOfNotNull(bundle.get().asFile, styles.orNull?.asFile)

        val digest = MessageDigest.getInstance("SHA-256")
        assets.forEach { asset ->
            digest.update(asset.name.toByteArray())
            digest.update(asset.readBytes())
        }
        val version = digest.digest()
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(VERSION_LENGTH)

        val index = indexFile.get().asFile
        val stamped = assets
            .map { it.name }
            .fold(index.readText()) { html, name -> html.withVersion(name, version) }

        index.writeText(stamped)
        logger.lifecycle("Версия гейта: $version")
    }

    private fun String.withVersion(asset: String, version: String): String =
        replace(Regex("""${Regex.escape(asset)}(\?v=[0-9a-zA-Z._-]+)?""")) { "$asset?v=$version" }

    private companion object {
        /** Длина версии гейта в hex-символах. */
        const val VERSION_LENGTH = 12
    }
}

val stampGateIndex = tasks.register<StampGateIndex>("stampGateIndex") {
    group = "build"
    description = "Проставляет версию гейта в ссылки dist/web-shell/index.html"
    bundle.set(DIST_SHELL_DIR.resolve("webShell.js"))
    styles.set(DIST_SHELL_DIR.resolve("styles.css"))
    indexFile.set(DIST_SHELL_DIR.resolve("index.html"))
}

// Финализатор, а не зависимость `build-web-shell`: в CI дистрибутив собирается
// напрямую, и выгружаемый артефакт тоже должен быть с версией.
tasks.matching {
    it.name == "jsBrowserDevelopmentExecutableDistribution" ||
        it.name == "jsBrowserProductionExecutableDistribution"
}.configureEach {
    finalizedBy(stampGateIndex)
}
