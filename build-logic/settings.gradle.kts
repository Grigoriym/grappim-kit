pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            // Points at grappim-kit's own catalog, not a consuming app's — a shared build-logic
            // pulled in via `includeBuild` from a separate repo has no `../gradle` of the
            // consumer's to read, so it pins its own versions here instead. See CONSUMING.md's
            // "build-logic" section for what this means for a consuming app's own catalog.
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
