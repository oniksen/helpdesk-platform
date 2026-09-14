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
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation.api)
            implementation(projects.features.tasks.api)
            implementation(projects.features.parking.api)
            implementation(projects.features.authorization.api)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.composePreview)
        }
    }
}