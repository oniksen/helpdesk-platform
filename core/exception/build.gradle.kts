plugins {
    alias(libs.plugins.kotlinMultiplatform)
}
kotlin {
    js {
        browser()
        binaries.executable()
    }
    jvm()
}