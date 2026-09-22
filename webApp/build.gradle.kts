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
                outputDirectory.set(projectDir.parentFile.resolve("dist"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.di)

            implementation(libs.compose.ui)
            implementation(libs.bundles.compose)
        }
    }
}