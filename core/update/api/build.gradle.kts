plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
