plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.authorization.api)
            implementation(projects.features.authorization.api)
            implementation(projects.features.home.api)

            implementation(libs.bundles.koin)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.composePreview)
        }
    }
}