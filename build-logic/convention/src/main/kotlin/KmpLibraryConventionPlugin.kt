import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.grappim.kit.buildlogic.KmpAdditionalTarget
import com.grappim.kit.buildlogic.configureKmp
import com.grappim.kit.buildlogic.configureLinting
import com.grappim.kit.buildlogic.configureTests
import com.grappim.kit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Every knob here is read as a Gradle property, since this class is applied purely by plugin id
 * (`plugins { id("com.grappim.kit.kmp.library") }`) with no constructor arguments available —
 * set per-app in that app's own root `gradle.properties`, or per-module in that module's own
 * `gradle.properties` where a module needs to differ from its app's default (Gradle resolves
 * project properties per-project, closest one wins). See `grappim-kit/CONSUMING.md`'s
 * "build-logic" section for the full list and what each app should set once it adopts this.
 *
 * - `grappimKitNamespacePrefix` (required) — e.g. `com.grappim.wallosmobile`.
 * - `grappimKitAdditionalTargets` (optional, comma-separated `jvm`/`ios`, default none).
 * - `grappimKitKoverExcludeAndroidUnitTests` (optional bool, default `false`).
 * - `grappimKitEnableAndroidHostTest` (optional bool, default `true` — wallosmobile/wayprint need
 *   it since Android is their only host-test source; TaigaMobileNova sets it `false` once it
 *   adopts, since its `jvm()` target already supplies host tests and enabling both would just
 *   run tests twice).
 * - `grappimKitExcludeFromLinting` (optional bool, default `false` — set in a specific module's
 *   own `gradle.properties`, e.g. `:testing`).
 * - `grappimKitExtraDetektRuleModule` (optional `:module-path`, default none — wallosmobile's
 *   `:detekt-rules`).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // KMP must be applied first so the android plugin can hook into it
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            val namespacePrefix = providers.gradleProperty("grappimKitNamespacePrefix").orNull
                ?: error(
                    "Set grappimKitNamespacePrefix in gradle.properties before applying " +
                        "com.grappim.kit.kmp.library (e.g. com.grappim.wallosmobile)."
                )
            val additionalTargets = providers.gradleProperty("grappimKitAdditionalTargets")
                .orNull
                .orEmpty()
                .split(",")
                .mapNotNull {
                    when (it.trim().lowercase()) {
                        "jvm" -> KmpAdditionalTarget.JVM
                        "ios" -> KmpAdditionalTarget.IOS
                        else -> null
                    }
                }
                .toSet()
            val enableAndroidHostTest = providers.gradleProperty("grappimKitEnableAndroidHostTest")
                .orNull
                ?.toBoolean()
                ?: true

            // The android DSL lives as a sub-extension of the kotlin extension
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            (kotlinExt as ExtensionAware).extensions
                .configure<KotlinMultiplatformAndroidLibraryExtension> {
                    compileSdk = libs.findVersion("compileSdk").get().toString().toInt()
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                    namespace = namespacePrefix + path.replace(':', '.').replace("-", "")

                    // `com.android.kotlin.multiplatform.library` creates no host-test
                    // compilation unless asked. Without this, `commonTest` belongs to no
                    // compilation, the test dependencies from `configureTests()` are inert,
                    // and there is no test task to run — needed whenever Android is the only
                    // (or one of several) source of host-test coverage for this module.
                    if (enableAndroidHostTest) {
                        withHostTestBuilder {}.configure {
                            isReturnDefaultValues = true
                            isIncludeAndroidResources = true
                        }
                    }
                }

            configureKmp(
                additionalTargets = additionalTargets,
                excludeAndroidUnitTestsFromCoverage = providers
                    .gradleProperty("grappimKitKoverExcludeAndroidUnitTests")
                    .orNull
                    ?.toBoolean()
                    ?: false,
            )
            configureTests()
            configureLinting(
                excludeFromLinting = providers.gradleProperty("grappimKitExcludeFromLinting")
                    .orNull
                    ?.toBoolean()
                    ?: false,
                extraDetektRuleModule = providers.gradleProperty("grappimKitExtraDetektRuleModule")
                    .orNull,
            )
        }
    }
}
