rootProject.name = "helpdesk-platform"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":webApp")
include(":core")
include(":core:di")
include(":core:uiadaptive")
include(":core:authorization")
include(":core:authorization:api")
include(":core:authorization:impl")
include(":core:network")
include(":core:network:api")
include(":core:network:impl")
include(":core:architecture")
include(":core:exception")
include(":maxminiappapi")
include(":maxminiappapi:api")
include(":maxminiappapi:impl")
include(":features")
include(":features:parking")
include(":features:parking:api")
include(":features:parking:impl")
include(":core:navigation")
include(":core:navigation:api")
include(":core:navigation:impl")
include(":features:tasks")
include(":features:tasks:api")
include(":features:tasks:impl")
include(":features:authorization")
include(":features:authorization:api")
include(":features:authorization:impl")
include(":core:update")
include(":core:update:api")
include(":core:update:impl")
include(":features:update")
include(":features:update:api")
include(":features:update:impl")