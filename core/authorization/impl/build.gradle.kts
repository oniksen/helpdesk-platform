plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network.api)
            implementation(projects.core.authorization.api)
            implementation(projects.maxminiappapi.api)

            implementation(libs.bundles.koin)
            implementation(libs.bundles.ktor)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktorJs)
        }
    }
}