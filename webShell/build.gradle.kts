@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser {
            distribution {
                outputDirectory.set(projectDir.parentFile.resolve("dist-shell"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.di)

            implementation(projects.core.authorization.api)
            implementation(projects.core.navigation.api)
            implementation(projects.core.update.api)
            implementation(projects.core.update.impl)
            implementation(projects.features.authorization.api)
            implementation(projects.features.authorization.impl)
            implementation(projects.features.update.api)
            implementation(projects.features.update.impl)

            implementation(libs.compose.ui)
            implementation(libs.bundles.compose)
            implementation(libs.koin.core)
        }
    }
}

// Общие веб-ресурсы (Service Worker, стили), разделяемые с webApp.
// Source set webMain создаётся композ-плагином на поздних этапах конфигурации,
// поэтому подключаем каталог после evaluation проекта.
gradle.projectsEvaluated {
    kotlin.sourceSets.getByName("webMain").resources.srcDir(rootProject.projectDir.resolve("webResources"))
}
