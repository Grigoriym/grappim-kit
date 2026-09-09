import com.grappim.kit.buildlogic.configureComposeStabilityConfig
import com.grappim.kit.buildlogic.configureComposeStabilityMarker
import com.grappim.kit.buildlogic.configureComposeStabilityReports
import org.gradle.api.Plugin
import org.gradle.api.Project

// Applied alongside `com.grappim.kit.kmp.library` on `*/domain` modules whose types are consumed
// as Composable parameters elsewhere — see docs/compose/stability-reports.md.
// `grappimKitComposeStabilityConfigEnabled` (see KmpLibraryConventionPlugin's kdoc) gates the
// stability-config half — TaigaMobileNova doesn't have a `config/compose/stability_config.conf`
// file yet (SHARED_LIBRARY_PLAN.md's "build-logic" section), so it stays off by default.
class KmpLibraryStabilityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureComposeStabilityMarker()
            configureComposeStabilityReports()
            configureComposeStabilityConfig(
                enabled = providers.gradleProperty("grappimKitComposeStabilityConfigEnabled")
                    .orNull
                    ?.toBoolean()
                    ?: false,
            )
        }
    }
}
