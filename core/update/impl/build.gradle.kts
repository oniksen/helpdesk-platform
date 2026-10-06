plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.mokkery)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    js {
        browser()
        binaries.executable()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.architecture)
            implementation(projects.core.exception)
            implementation(projects.core.network.api)
            implementation(projects.core.update.api)

            implementation(libs.bundles.ktor)
            implementation(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {
            implementation(npm("jszip", "3.10.1"))
            implementation(libs.wrappers.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}
