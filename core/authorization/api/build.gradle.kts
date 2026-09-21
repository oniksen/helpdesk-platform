plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.maxminiappapi.api)
        }
    }
}