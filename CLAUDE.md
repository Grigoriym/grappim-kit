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

## Porting a `commonTest` from a source app (found extracting `logger`/`coroutines`/`domain`)

- Kotlin/Native (the `iosArm64`/`iosSimulatorArm64` test compile) rejects a comma inside a
  backtick-quoted test function name — `` `exception thrown, not propagated`() `` fails
  `compileTestKotlinIosArm64` with `Name contains illegal characters: ","`, even though the
  same name compiles fine on `jvm`. The source apps' tests were never compiled for iOS, so
  this only surfaces once a ported test runs on a real Kotlin/Native target. Fix: reword the
  name without a comma — don't drop the iOS target instead.
- A module whose extracted logic depended on a per-app Koin-annotated DI module in the
  source app (seen in `core/async-kmp`) is ported as plain functions/objects here, not with
  the Koin annotations — see `SHARED_LIBRARY_PLAN.md`'s "Step 8b" section for the reasoning
  (avoids pulling KSP + Koin Annotations into this repo for a handful of one-line
  providers). Each consuming app binds the plain API into its own Koin module.
- Before porting a "verdict: extract wholesale" module from `SHARED_LIBRARY_PLAN.md`,
  re-list both source apps' directories rather than trusting the table's file list —
  `core/logger` and `core/async-kmp` both turned out to have extra iOS/JVM files on Taiga's
  side that the original wallosmobile-vs-Taiga diff pass (which only compared `commonMain`)
  missed, same pattern step 8 already found for `core/navigation`.
- A "reconcile, don't copy" verdict on exception-throwing code needs a check the plan
  doesn't always spell out: what does each source app's own exception type actually
  extend? Porting `core/api`'s `CompositeTrustManager` (step 8d), TaigaMobileNova's
  `UntrustedCertificateException`/`CertificateHostnameMismatchException` extend
  `java.security.cert.CertificateException` directly, which is *why* their
  `checkServerTrusted` can throw them bare and still satisfy the JSSE
  `X509TrustManager` contract. `grappim-kit-domain`'s versions are deliberately plain
  commonMain `Exception`s instead (the whole reason they're portable to iOS/JVM-common
  code at all), so copying Taiga's bare-throw pattern onto them silently breaks that
  contract. Only 3 failing ported tests caught it — nothing about the code looks wrong
  by inspection. Fix: wrap at the JSSE throw site instead
  (`throw CertificateException(UntrustedCertificateException(...))`), matching
  wallosmobile's original wrapping style; a caller reads the real exception back via
  `findPendingCertTrust()`/the `cause` chain.
- Kotlin's compiler (2.4.10) treats `x is Y` as a compile *error* — "Check for instance
  is always 'false'" — not a warning, when `Y` is statically unrelated to `x`'s type.
  A ported test asserting `exception is SomeUnrelatedType` (a pattern that compiled
  fine in the source app) will fail to compile here if reconciliation changed what the
  thrown type actually is. Rewrite the assertion around the real type relationship
  instead of the source app's, don't just delete the check.
