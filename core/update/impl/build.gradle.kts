plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.update.api)

            implementation(libs.bundles.ktor)
            implementation(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {
            implementation(projects.core.network.api)

            implementation(npm("jszip", "3.10.1"))
            implementation(libs.wrappers.browser)
        }
    }
}
