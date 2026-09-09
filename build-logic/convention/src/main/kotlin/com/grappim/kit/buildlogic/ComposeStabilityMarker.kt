package com.grappim.kit.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.configureEach
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Lets the Compose Compiler embed a stability marker on this module's classes, without pulling in
// any Compose UI toolkit (Foundation/Material3/Navigation/...). For `*/domain` modules whose types
// are consumed as Composable parameters elsewhere: without this, the Compose compiler in a
// downstream UI module has no marker to trust and defaults every such class to Unstable, even a
// fully `val`, ImmutableList-using data class — see docs/compose/stability-reports.md.
//
// compose-runtime is `compileOnly` on JVM-based targets (Android, Desktop): it's needed only for
// the compiler to reference the `@StabilityInferred` annotation type at compile time, not at
// runtime, and consuming UI modules already carry compose-runtime themselves. Kotlin/Native
// doesn't support `compileOnly` resolution the same way — a `compileOnly` dependency declared in
// `commonMain` prints a "Unsupported compileOnly Dependencies ... Kotlin/Native" warning on every
// build once iOS targets exist — so iOS gets `api` instead; harmless, since a Native consumer
// links compose-runtime directly anyway. Ported from wayprint's richer version (2026-09-09 diff):
// wallosmobile's own copy predates iOS ever existing in that repo and only handles Android.
//
// Uses `sourceSets.matching {}.configureEach {}` rather than the typesafe `.jvmMain`/`.iosMain`
// accessors — those throw if the module hasn't declared that target yet, and this function
// doesn't know which targets `configureKmp()` was called with for this module.
fun Project.configureComposeStabilityMarker() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extensions.configure<KotlinMultiplatformExtension> {
        val composeRuntime = libs.findLibrary("jetbrains.compose.runtime").get()
        sourceSets.matching { it.name == "androidMain" || it.name == "jvmMain" }.configureEach {
            dependencies {
                compileOnly(composeRuntime)
            }
        }
        sourceSets.matching { it.name == "iosMain" }.configureEach {
            dependencies {
                api(composeRuntime)
            }
        }
    }
}
