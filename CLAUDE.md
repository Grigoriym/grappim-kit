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
