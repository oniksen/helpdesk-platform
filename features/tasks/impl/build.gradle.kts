plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network.api)
            implementation(projects.core.navigation.api)
            implementation(projects.core.uiadaptive)
            implementation(projects.core.architecture)
            implementation(projects.core.exception)
            implementation(projects.features.tasks.api)
            implementation(projects.features.parking.api)
            implementation(projects.features.authorization.api)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
            implementation(libs.bundles.koin)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.composePreview)
        }
    }
}