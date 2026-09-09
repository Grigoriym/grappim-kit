package com.grappim.kit.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File

fun Project.configureTests() {
    tasks.withType<Test> {
        failFast = true
        // https://github.com/gradle/gradle/issues/33619#issuecomment-2913519014
        failOnNoDiscoveredTests.set(false)
        reports {
            html.required.set(true)
        }
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
        }
    }

    configureCommonTestDependencies()
}

// Every KMP module gets kotlin-test + turbine, so no module declares them by hand — reconciled
// from wallosmobile's guard (skip only `:testing` itself) and wayprint's extra guard (also skip
// while `:testing` hasn't been scaffolded yet) into one that's strictly safer than either alone.
private fun Project.configureCommonTestDependencies() {
    val projectPath = path

    extensions.findByType<KotlinMultiplatformExtension>()?.apply {
        sourceSets.apply {
            commonTest.dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("turbine").get())
            }
        }
    }
}

/**
 * @param extraDetektRuleModule an extra `:module-path` this app wants wired as a `detektPlugins`
 *   dependency (wallosmobile's own `:detekt-rules` custom ruleset — wayprint/TaigaMobileNova have
 *   no equivalent, confirmed in SHARED_LIBRARY_PLAN.md's step 5). Left as a parameter, not baked
 *   in, since only one of the three apps has this module at all.
 */
fun Project.configureLinting(
    excludeFromLinting: Boolean = false,
    extraDetektRuleModule: String? = null,
) {
    if (excludeFromLinting) return

    pluginManager.apply("dev.detekt")
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    // https://detekt.dev/docs/introduction/configurations/
    configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        // `rootDir` (not `rootProject.files(...)`) — every Project already carries its build's
        // root directory as a plain, non-cross-project value, so this doesn't need `rootProject`
        // itself (a live Project reference to another project, which Isolated Projects forbids
        // reading from a subproject). Resolves against whichever app applies this plugin, not
        // against grappim-kit's own build — each consuming app keeps its own
        // `config/detekt/detekt.yml`.
        config.setFrom(File(rootDir, "config/detekt/detekt.yml"))
        allRules.set(false)

        // detekt's default source set is `src/main/{java,kotlin}`, which no KMP module has —
        // left alone, every `:module:detekt` task reports NO-SOURCE and the gate lints nothing.
        // Pointing it at `src/` covers commonMain, commonTest and any platform source set.
        source.setFrom(layout.projectDirectory.dir("src"))
    }

    // ./gradlew --continue ktlintCheck
    // ./gradlew ktlintFormat
    // ./gradlew addKtlintCheckGitPreCommitHook
    configure<KtlintExtension> {
        version.set("1.8.0")
        android.set(true)
        ignoreFailures.set(false)
        verbose.set(true)
        outputColorName.set("RED")
        outputToConsole.set(true)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
            reporter(ReporterType.HTML)
            reporter(ReporterType.JSON)
        }
    }

    dependencies {
        "ktlintRuleset"(libs.findLibrary("compose.rules.ktlint").get())
        "detektPlugins"(libs.findLibrary("compose.rules.detekt").get())

        if (extraDetektRuleModule != null) {
            "detektPlugins"(project(extraDetektRuleModule))
        }
    }
}
