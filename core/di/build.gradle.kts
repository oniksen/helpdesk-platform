plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation.api)
            implementation(projects.core.navigation.impl)
            implementation(projects.core.authorization.api)
            implementation(projects.core.authorization.impl)
            implementation(projects.features.parking.impl)
            implementation(projects.features.home.api)
            implementation(projects.features.home.impl)
            implementation(projects.features.authorization.api)
            implementation(projects.features.authorization.impl)
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