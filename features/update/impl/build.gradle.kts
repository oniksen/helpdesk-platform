plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.mokkery)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.features.update.api)
            implementation(projects.core.update.api)
            implementation(projects.core.exception)
            implementation(projects.core.navigation.api)
            implementation(projects.features.tasks.api)
            implementation(projects.core.uiadaptive)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
            implementation(libs.bundles.composeAdaptive)
            implementation(libs.bundles.koin)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(projects.core.exception)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit)
        }
    }
}
