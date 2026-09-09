import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.grappim.kit.buildlogic.configureComposeStabilityConfig
import com.grappim.kit.buildlogic.configureComposeStabilityReports
import com.grappim.kit.buildlogic.configureKmp
import com.grappim.kit.buildlogic.configureKmpCompose
import com.grappim.kit.buildlogic.configureLinting
import com.grappim.kit.buildlogic.configureTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Applied alongside `com.grappim.kit.kmp.library` — see that class's kdoc for the shared
// `grappimKit*` Gradle properties this convention plugin also reads
// (`grappimKitComposeStabilityConfigEnabled` additionally, for the stability-config feature).
class KmpLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            configureComposeStabilityReports()
            configureComposeStabilityConfig(
                enabled = providers.gradleProperty("grappimKitComposeStabilityConfigEnabled")
                    .orNull
                    ?.toBoolean()
                    ?: false,
            )
            configureKmp()
            configureKmpCompose()
            configureTests()
            configureLinting()
            enableAndroidResources()
        }
    }

    // CMP resources (assets) require androidResources to be enabled in the KMP Android library
    // plugin. Without it, componentSources.assets is null and CMP's pipeline silently no-ops,
    // leaving composeResources/ out of the AAR and APK.
    // See: https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html
    // See: YouTrack CMP-9547
    private fun Project.enableAndroidResources() {
        pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            val kotlinExt = extensions.getByType<KotlinMultiplatformExtension>()
            (kotlinExt as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
                androidResources.enable = true
            }
        }
    }
}
