plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    js { browser() }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network.api)
            implementation(projects.core.authorization.api)
            implementation(projects.maxminiappapi.api)

            implementation(libs.bundles.ktor)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktorJs)
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