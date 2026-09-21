plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.navigation3.ui)
        }
    }
}