plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
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
            implementation(projects.core.authorization.api)
            implementation(projects.features.authorization.api)
            implementation(projects.features.tasks.api)
            implementation(projects.core.uiadaptive)

            implementation(libs.bundles.koin)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.composeResources)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.composePreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.bundles.composeUiTest)
        }
        jvmTest.dependencies {
            // Desktop-рантайм нужен для запуска Compose UI-тестов на JVM.
            implementation(compose.desktop.currentOs)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}