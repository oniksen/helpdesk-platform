plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.kover)
}
kotlin {
    js {
        browser()
        binaries.executable()
    }
    jvm()
}