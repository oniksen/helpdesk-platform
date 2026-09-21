plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js {
        browser()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.navigation.api)
            implementation(projects.core.uiadaptive)
            implementation(projects.core.authorization.api)
            implementation(projects.features.parking.api)
            implementation(projects.maxminiappapi.api)

            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
            implementation(libs.bundles.composeAdaptive)
            implementation(libs.bundles.koin)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.composeIcons)
        }
        jvmMain.dependencies {
            implementation(projects.features.parking.api)
            implementation(libs.bundles.composePreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}