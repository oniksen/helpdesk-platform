plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.authorization.api)
            implementation(projects.maxminiappapi.api)

            implementation(libs.bundles.koin)
        }
    }
}