import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildkonfig)
}

// Техническая версия JVM-сборки, задаётся в корневом build.gradle.kts.
val jvmSystemVersion = rootProject.extra["jvmSystemVersion"] as String

// Версия бандла для macOS: jpackage требует первый компонент версии не меньше единицы.
val jvmMacPackageVersion = rootProject.extra["jvmMacPackageVersion"] as String

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.di)
            implementation(projects.core.network.api)

            implementation(projects.core.authorization.api)
            implementation(projects.core.navigation.api)
            implementation(projects.core.navigation.impl)
            implementation(projects.features.parking.api)
            implementation(projects.features.parking.impl)
            implementation(projects.features.tasks.api)
            implementation(projects.features.tasks.impl)
            implementation(projects.features.settings.api)
            implementation(projects.features.settings.impl)

            implementation(libs.koin.core)
            implementation(libs.compose.ui)
            implementation(libs.bundles.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(projects.core.update.api)
            implementation(projects.core.update.impl)
            implementation(projects.core.di.infra)
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.lpmti.helpdeskplatform.MainKt"

        nativeDistributions {
            // Все сборочные результаты проекта живут в корневой папке dist/
            outputBaseDir.set(rootProject.layout.projectDirectory.dir("dist/jvm"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "com.lpmti.helpdesk"
            packageVersion = jvmSystemVersion
            vendor = "LPMTI"

            macOS {
                // jpackage на macOS не принимает app-version, начинающийся с нуля
                packageVersion = jvmMacPackageVersion
            }

            windows {
                perUserInstall = true

                iconFile.set(project.file("src/commonMain/composeResources/drawable/app_icon.ico"))

                // Флаги доступа для рефлексии и внутренних вызовов JVM
                jvmArgs(
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens", "java.base/java.util=ALL-UNNAMED",
                    "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
                    "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
                    "--add-opens", "java.desktop/sun.java2d=ALL-UNNAMED",
                    "--add-opens", "java.base/java.io=ALL-UNNAMED",
                    "--add-opens", "java.base/java.nio=ALL-UNNAMED",
                    "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED",
                )

                jvmArgs("-Dcompose.application.showConsole=true")
            }

            // В сборку укладывается своё JDK, поэтому нужен явный список модулей
            modules(
                "java.desktop", // AWT/Skiko для Compose Desktop
                "java.prefs", // настройки пользователя
                "java.xml",
                "java.naming",
                "java.management",
                "java.sql",
                "java.logging",
                "jdk.unsupported",
                "java.scripting",
            )
        }

        buildTypes {
            release {
                proguard {
                    isEnabled = false
                }
            }
        }
    }
}
