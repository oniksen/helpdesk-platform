plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network.api)
            implementation(projects.core.exception)

            implementation(libs.kotlinx.coroutines.core)
        }
    }
}