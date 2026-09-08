rootProject.name = "grappim-kit"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":navigation")
include(":logger")
include(":coroutines")
include(":domain")
include(":crash")
include(":appinfo")
include(":storage")
include(":trustmanager")
include(":testing")
include(":uikit")
