plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.mokkery)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
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
        jsMain.dependencies {
            implementation(projects.core.update.impl)
        }
        commonTest.dependencies {
            implementation(projects.core.exception)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}
