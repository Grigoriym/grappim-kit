# CLAUDE.md

`grappim-kit` — shared Kotlin Multiplatform library extracted from `wallosmobile`,
`wayprint`, `TaigaMobileNova`, `HateItOrRateIt`. Scope and decisions are planned in the
sibling `grappim-watcher` repo's `docs/SHARED_LIBRARY_PLAN.md` and
`docs/CHECKLIST.md` — read those before starting an extraction step here.

## Coordinates and publishing conventions

Module coordinates and POM metadata are driven entirely by `gradle.properties`
(the Vanniktech Gradle Maven Publish plugin's own convention), **not** by calling
`coordinates()`/`pom { }` in a module's `build.gradle.kts`. Doing both at once fails
with `property '...' is final and cannot be changed any further` — the plugin reads
`GROUP`/`VERSION_NAME`/`POM_*` from properties before the build script's `mavenPublishing`
block even runs, and treats a subsequent explicit call as a conflicting second write.

- Root `gradle.properties`: `GROUP`, `VERSION_NAME`, and the repo-wide `POM_*` fields
  (license, SCM, developer) shared by every module.
- Each module's own `gradle.properties`: `POM_ARTIFACT_ID`, `POM_NAME`, `POM_DESCRIPTION`
  — the fields that differ per module.
- A module's `build.gradle.kts` only needs `configure(KotlinMultiplatform(...))`,
  `publishToMavenCentral(automaticRelease = true)`, and `signAllPublications()`.

Kotlin source packages live under `com.grappim.kit.*`, independent of the Maven group id
(`io.github.grigoriym` — chosen because GitHub OAuth login auto-verified it on the
Central Portal for free; see `SHARED_LIBRARY_PLAN.md` for why this replaced the
originally-planned `click.gregstuff`).

After a CI publish reports success, `repo1.maven.org` can take a few minutes to reflect
it — don't read an immediate 404 there as a failed publish.

## Local build setup

`local.properties` (gitignored) needs `sdk.dir=/home/gregory/Android/Sdk` for any module
with an Android target — not created automatically like it is in the four app repos.

## Adding a KMP+Compose module (first done for `navigation`, step 8)

A module needing Android + Compose Multiplatform (not just `placeholder`'s bare `jvm()`)
needs, in its `build.gradle.kts`:

- `alias(libs.plugins.android.kotlin.multiplatform.library)`, `alias(libs.plugins.jetbrains.compose)`,
  `alias(libs.plugins.jetbrains.compose.compiler)`, applied at the root `build.gradle.kts`
  too (as `apply false`).
- `kotlin { androidLibrary { namespace = ...; compileSdk = ...; minSdk = ... } }`. AGP
  9.4.0 warns this block is deprecated in favor of `android { }`, but the four source
  apps (`TaigaMobileNova`, same AGP version) still use `androidLibrary { }` too as of
  2026-09-06 — matching upstream's current pattern, not a bug to fix here.
- This AGP version's `com.android.kotlin.multiplatform.library` plugin has a single
  `main`/`android` variant, not a debug/release split — task names are
  `compileAndroidMain`, `publishAndroidPublicationToMavenCentralRepository`, not
  `compileReleaseKotlinAndroid`. Vanniktech's `KotlinMultiplatform(androidVariantsToPublish
  = listOf("release"))` still configures and generates POM/metadata without error despite
  the variant actually being named `main` — untested whether `"release"` vs. `"main"`
  changes anything real; if a future publish run complains about the variant name, try
  `"main"` first.
- A `Compose Multiplatform runtime dependencies' versions don't match with plugin
  version` warning (expected `ui:1.12.0`, actual `ui:1.10.1`) is expected noise from
  `navigation3-ui:1.1.1` pulling an older compose-ui transitively — same version pairing
  the source apps use themselves, not something introduced here. Harmless; don't chase it.
