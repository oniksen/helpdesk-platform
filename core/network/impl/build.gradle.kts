plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.kover)
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
            implementation(projects.core.network.api)
        }

        jvmMain.dependencies {
            implementation(libs.okio)
            implementation(libs.slf4j.simple)
        }
    }
}