plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.buildkonfig)
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
            implementation(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {
            implementation(npm("jszip", "3.10.1"))
            implementation(libs.wrappers.browser)
        }
    }
}
