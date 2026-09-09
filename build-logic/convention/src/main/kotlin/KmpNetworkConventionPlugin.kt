import com.grappim.kit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.configureEach
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Only wallosmobile/TaigaMobileNova have ever applied this (wayprint has no network layer —
 * confirmed in SHARED_LIBRARY_PLAN.md). Wires whichever engine source sets the module actually
 * has, via `sourceSets.matching {}.configureEach {}` rather than the typesafe `.jvmMain`/
 * `.iosMain` accessors — this plugin doesn't know which targets `configureKmp()` was called with
 * for this module (it's applied independently, possibly before or after), and those accessors
 * throw for a target that doesn't exist yet.
 */
class KmpNetworkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("ktor.core").get())
                    }
                    matching { it.name == "androidMain" || it.name == "jvmMain" }.configureEach {
                        dependencies {
                            implementation(libs.findLibrary("ktor.client.okhttp").get())
                        }
                    }
                    matching { it.name == "iosMain" }.configureEach {
                        dependencies {
                            implementation(libs.findLibrary("ktor.client.darwin").get())
                        }
                    }
                }
            }
        }
    }
}
