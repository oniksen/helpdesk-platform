plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation.api)
            implementation(projects.core.navigation.impl)
            implementation(projects.core.authorization.api)
            implementation(projects.core.authorization.impl)
            implementation(projects.core.network.api)
            implementation(projects.core.network.impl)
            implementation(projects.core.update.api)
            implementation(projects.core.update.impl)
            implementation(projects.features.parking.impl)
            implementation(projects.features.tasks.api)
            implementation(projects.features.tasks.impl)
            implementation(projects.features.authorization.api)
            implementation(projects.features.authorization.impl)
            implementation(projects.features.update.api)
            implementation(projects.features.update.impl)
            implementation(projects.maxminiappapi.api)
            implementation(projects.maxminiappapi.impl)

            implementation(libs.compose.runtime)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}