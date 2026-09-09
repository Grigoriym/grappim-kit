package com.grappim.kit.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val JDK_VERSION = 21

enum class KmpAdditionalTarget { JVM, IOS }

/**
 * The single edit point for platform targets and the handful of dependencies every KMP
 * library module needs.
 *
 * Reconciled from wallosmobile/wayprint (Android-only so far) vs. TaigaMobileNova (already on
 * `jvm()` + `iosArm64()`/`iosSimulatorArm64()`, plus its own Kover instrumentation exclusion) —
 * a real fork, not just cosmetic drift, confirmed by a fresh diff 2026-09-09. See
 * SHARED_LIBRARY_PLAN.md's "build-logic" section. `additionalTargets` defaults to empty
 * (wallosmobile/wayprint's current shape, 2 of 3 apps) rather than to Taiga's fuller set —
 * whichever app adopts this later passes what it actually builds for today; adopting it must
 * not silently add Kotlin/Native compilation a consumer's CI isn't set up for.
 *
 * Deliberately does **not** inject any module's own common dependency (the source apps both
 * hardcoded `implementation(project(":core:logger"))` here) — a shared build-logic can't assume
 * a consuming app still has that local module once it swaps onto the published
 * `grappim-kit-logger` artifact. Each module declares its own dependencies explicitly instead.
 */
fun Project.configureKmp(
    additionalTargets: Set<KmpAdditionalTarget> = emptySet(),
    excludeAndroidUnitTestsFromCoverage: Boolean = false,
) {
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    if (excludeAndroidUnitTestsFromCoverage) {
        extensions.configure<KoverProjectExtension> {
            currentProject {
                instrumentation {
                    // KMP library modules have no product flavors, so only the base
                    // debug/release Android unit test tasks exist here.
                    disabledForTestTasks.addAll("testDebugUnitTest", "testReleaseUnitTest")
                }
            }
        }
    }

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(JDK_VERSION)
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        if (KmpAdditionalTarget.JVM in additionalTargets) {
            jvm()
        }
        if (KmpAdditionalTarget.IOS in additionalTargets) {
            iosArm64()
            iosSimulatorArm64()
        }

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                implementation(libs.findLibrary("kotlinx.collections.immutable").get())
                implementation(libs.findLibrary("kotlinx.datetime").get())
            }

            if (KmpAdditionalTarget.JVM in additionalTargets) {
                jvmMain.dependencies {
                    implementation(libs.findLibrary("kotlinx.coroutines.swing").get())
                    implementation(libs.findLibrary("kotlin.metadata.jvm").get())
                }
            }
            if (KmpAdditionalTarget.IOS in additionalTargets) {
                iosMain.dependencies {
                    implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                }
            }
        }
    }
}
