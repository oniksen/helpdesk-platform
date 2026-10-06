plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    js { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.authorization.api)
            implementation(projects.core.authorization.impl)
            implementation(projects.core.network.api)
            implementation(projects.core.network.impl)
            implementation(projects.maxminiappapi.api)
            implementation(projects.maxminiappapi.impl)

            implementation(libs.koin.core)
        }
    }
}
