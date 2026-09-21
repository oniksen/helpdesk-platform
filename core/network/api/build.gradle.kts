plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.exception)
            api(libs.bundles.ktor)
        }
        jvmMain.dependencies {
            api(libs.bundles.ktorJvm)
        }
        jsMain.dependencies {
            api(libs.bundles.ktorJs)
        }
    }
}