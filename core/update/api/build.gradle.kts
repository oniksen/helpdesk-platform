plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.core.network.api)

            api(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(projects.core.exception)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}
