plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.di)
            implementation(projects.core.network.api)

            implementation(projects.core.authorization.api)
            implementation(projects.core.navigation.api)
            implementation(projects.core.navigation.impl)
            implementation(projects.features.parking.api)
            implementation(projects.features.parking.impl)
            implementation(projects.features.tasks.api)
            implementation(projects.features.tasks.impl)
            implementation(projects.features.settings.api)
            implementation(projects.features.settings.impl)

            implementation(libs.koin.core)
            implementation(libs.compose.ui)
            implementation(libs.bundles.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(projects.core.update.api)
            implementation(projects.core.update.impl)
            implementation(projects.core.di.infra)
        }
    }
}
