import com.grappim.kit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Byte-identical (apart from package) across wallosmobile/wayprint/TaigaMobileNova as of
// 2026-09-09 — confirmed by fresh diff, ported wholesale. See SHARED_LIBRARY_PLAN.md's
// "build-logic" section for the full diff writeup.
class KmpDiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("io.insert-koin.compiler.plugin")
            }

            extensions.findByName("koinCompiler")?.let { ext ->
                @Suppress("UNCHECKED_CAST")
                (ext.javaClass.getMethod("getCompileSafety").invoke(ext) as Property<Boolean>).set(false)
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    commonMain.dependencies {
                        val koinBom = libs.findLibrary("koin.bom").get()
                        implementation(project.dependencies.platform(koinBom))

                        api(libs.findLibrary("koin.annotations").get())
                        implementation(libs.findLibrary("koin.core").get())
                        implementation(libs.findLibrary("koin.core.viewmodel").get())
                        implementation(libs.findLibrary("koin.compose").get())
                        implementation(libs.findLibrary("koin.compose.viewmodel").get())
                    }

                    androidMain.dependencies {
                        implementation(libs.findLibrary("koin.android").get())
                    }
                }
            }
        }
    }
}
