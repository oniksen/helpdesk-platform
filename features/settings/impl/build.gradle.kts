plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.settings.api)
            implementation(projects.core.navigation.api)
            implementation(projects.core.uiadaptive)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
            implementation(libs.bundles.koin)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.composePreview)
        }
    }
}