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

- `navigation` (`grappim-kit-navigation`) — dual back-stack Navigation 3 wrapper
  (`Navigator`/`NavigationState`, class-keyed sub-stacks so a top-level route can carry a
  payload) plus a hand-rolled `ResultBus` for cross-screen results. Reconciled from
  `core/navigation` across `wallosmobile`/`wayprint`/`TaigaMobileNova` — Taiga's version had
  diverged from the other two (tablet support), so this ships Taiga's richer API rather than
  the two-app-identical one. Targets: `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`.
- `logger` (`grappim-kit-logger`) — tiny logcat-style logging facade (`KitLogger`/`logcat`)
  with Timber/NSLog/file-rotation backends for Android, iOS, and JVM.
- `coroutines` (`grappim-kit-coroutines`) — dispatcher providers, a crash-logging
  `applicationScope()` factory, and a `ThreadSafeMap` utility. DI-framework-agnostic —
  wire it into your own Koin/Hilt module.
- `domain` (`grappim-kit-domain`) — cancellation-safe `Result` helpers (`resultOf`/
  `mapResult`) and a certificate-pinning trust-decision model
  (`PendingCertTrust`/`UntrustedCertificateException`).
- `crash` (`grappim-kit-crash`) — `CrashReporter` interface, a seam a lower module can
  depend on without pulling in a concrete crash SDK.
- `appinfo` (`grappim-kit-appinfo`) — `AppInfoProvider` interface for build-time facts
  (debug/F-Droid/version/build type) shared code can't read for itself.
- `storage` (`grappim-kit-storage`) — `NetworkMonitor`, encrypted-string `SecretCipher`,
  and DataStore-backed `TrustedCertStorage`.
- `trustmanager` (`grappim-kit-trustmanager`) — `X509TrustManager` decorator combining
  the device CA store with a trust-on-first-use `TrustedCertStorage` pin. `jvm`/`android`
  only — `X509ExtendedTrustManager` has no iOS equivalent.
- `testing` (`grappim-kit-testing`) — shared `kotlin.test` fakes and rules
  (`MainDispatcherRule`, `FakeCrashReporter`, `FakeAppInfoProvider`, `FakeNetworkMonitor`,
  `FakeTrustedCertStorage`, `FakeSecretCipher`).

Swapping an app onto one of these? Check [CONSUMING.md](CONSUMING.md) for known
integration gotchas first, one section per module.

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

After a successful publish, `repo1.maven.org` can take a few minutes to sync — a 404
there right after CI reports success isn't a failure, just CDN lag.
