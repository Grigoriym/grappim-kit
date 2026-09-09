package com.grappim.kit.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The Compose UI stack every module that renders Composables needs, confirmed identical between
 * wallosmobile/wayprint and TaigaMobileNova (2026-09-09 diff). Deliberately excludes each app's
 * own additional, genuinely different choices on top of this base — wallosmobile/wayprint add
 * Navigation 3 + `material-icons-extended` here (most of that now lives in the already-published
 * `grappim-kit-navigation`/`grappim-kit-uikit` instead), TaigaMobileNova adds
 * `lifecycle-viewmodel-savedstate` + a `jvmMain` desktop-compose dependency. A module that still
 * needs one of those declares it itself, same as any other module-specific dependency — see
 * SHARED_LIBRARY_PLAN.md's "build-logic" section.
 */
fun Project.configureKmpCompose() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("jetbrains.compose.runtime").get())
                implementation(libs.findLibrary("jetbrains.compose.foundation").get())
                implementation(libs.findLibrary("jetbrains.compose.ui").get())
                implementation(libs.findLibrary("jetbrains.compose.ui.tooling.preview").get())
                implementation(libs.findLibrary("jetbrains.compose.material3").get())
                implementation(libs.findLibrary("jetbrains.compose.material").get())
                implementation(libs.findLibrary("jetbrains.lifecycle.runtime.compose").get())
                implementation(libs.findLibrary("jetbrains.lifecycle.viewmodel.compose").get())
            }
        }
    }

    dependencies {
        "androidRuntimeClasspath"(libs.findLibrary("jetbrains.compose.ui.tooling").get())
    }
}
