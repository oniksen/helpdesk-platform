plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.maxminiappapi.api)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}