plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.architecture)
            implementation(projects.core.exception)

            api(libs.kotlinx.coroutines.core)
        }
        jsMain.dependencies {
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
