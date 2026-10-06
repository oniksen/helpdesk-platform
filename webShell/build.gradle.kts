@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
            implementation(projects.core.di.infra)
            implementation(projects.core.authorization.api)
            implementation(projects.core.network.api)
            implementation(projects.core.update.impl)
            implementation(projects.maxminiappapi.api)

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
