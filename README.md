# grappim-kit

Shared Kotlin Multiplatform library extracted from the duplicated code across
[wallosmobile](https://github.com/Grigoriym/wallosmobile),
[wayprint](https://github.com/Grigoriym/Wayprint),
[TaigaMobileNova](https://github.com/Grigoriym/TaigaMobileNova), and
[HateItOrRateIt](https://github.com/Grigoriym/HateItOrRateIt).

Planning and scope live in
[grappim-watcher/docs/SHARED_LIBRARY_PLAN.md](https://github.com/Grigoriym/grappim-watcher/blob/main/docs/SHARED_LIBRARY_PLAN.md);
this repo is the extraction itself.

## Distribution

Published to Maven Central under the `io.github.grigoriym` group id (verified automatically
via GitHub OAuth on the Central Portal, no DNS step needed), because both `wallosmobile` and
`TaigaMobileNova` ship F-Droid builds and GitHub Packages isn't on F-Droid's trusted-source
allowlist. Kotlin source stays under the `com.grappim.*` package regardless — the Maven
group id (publishing namespace) and the Kotlin package are independent.

```kotlin
dependencies {
    implementation("io.github.grigoriym:grappim-kit-<module>:<version>")
}
```

One shared version number covers the whole repo (see the plan doc for why).

## Modules

- `placeholder` — bootstrap-only module proving out the publish pipeline (namespace
  verification, signing, CI). Not a real dependency; superseded once `core/navigation`
  (the first real extraction) publishes.

## Publishing

CI publishes via `.github/workflows/publish.yml` (manual `workflow_dispatch`), using the
[Vanniktech Gradle Maven Publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/)
against the Sonatype Central Portal. Required repo secrets:

- `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` — a Central Portal user token
  (Account → Generate User Token), not your login password.
- `SIGNING_IN_MEMORY_KEY` — ASCII-armored GPG private key (`gpg --export-secret-keys --armor <key-id>`).
- `SIGNING_IN_MEMORY_KEY_ID` — the key's short id (last 8 hex chars of the fingerprint).
- `SIGNING_IN_MEMORY_KEY_PASSWORD` — the key's passphrase.

To publish locally instead, set the same names as `ORG_GRADLE_PROJECT_*` properties in
`~/.gradle/gradle.properties` and run `./gradlew publishToMavenCentral`.
