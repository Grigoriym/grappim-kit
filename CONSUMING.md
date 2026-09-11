# Consuming a `grappim-kit` module

Gotchas and known issues for an app **swapping onto** a published `grappim-kit` module —
one section per module. This is separate from `CLAUDE.md`, which is for developing
`grappim-kit` itself (build tooling, extraction conventions); this file is for whichever
app (and whichever agent session) is on the other end of `implementation
("io.github.grigoriym:grappim-kit-<module>:<version>")`.

**Convention: whoever finds a consumer-facing gotcha writes it here, not just in their
own app's `CLAUDE.md` or in `grappim-watcher`'s planning docs.** Neither of those is
something a *different* app's session would know to check. This file ships with the
library, so it's the one place every consumer can find it.

**Standing rule, applies to every module below:** an extraction commit is a snapshot, not
a live link to the source app it came from — it can go stale between when it's cut and
when it's published, or between two different apps' swap dates. Before swapping onto a
published module, diff its actual source (this repo, at the version you're about to
depend on) against the *canonical* source app's current HEAD — don't trust an extraction
verdict or a module's description as still current. Treat "canonical, mechanical swap" as
a claim to verify, not a fact.

## navigation (`grappim-kit-navigation`)

Canonical source: TaigaMobileNova's `core/navigation` (chosen for its tablet-support
divergence over wallosmobile/wayprint's narrower identical version — see
`grappim-watcher/docs/SHARED_LIBRARY_PLAN.md`'s "Step 8" section).

- **Fixed in 0.1.1 (2026-09-08):** `0.1.0`'s `Navigator.goToTopLevel()` shipped a
  back-stack-growing bug — every drawer-section switch pushed onto `topLevelStack`
  instead of replacing the current entry, so `goBack()`/`canGoBack()` cascaded back
  through every section visited that session instead of exiting at each section's root.
  Root cause: the extraction commit (`608c54a`) snapshotted the file a day before
  TaigaMobileNova's own `dev` fixed this exact bug (`e78fe61b`, #392); `0.1.0` was
  published from the stale snapshot without anyone re-diffing first. Caught during
  TaigaMobileNova's own step-9 swap by diffing the published module against `dev` HEAD
  per the standing rule above, instead of trusting the extraction verdict. If you're
  depending on `0.1.0`, upgrade to `0.1.1`+ before relying on drawer/section navigation.

- **wallosmobile/wayprint: this is not a mechanical swap the way it was for
  TaigaMobileNova.** TaigaMobileNova's `core/navigation` was the canonical source this
  module was extracted from; wallosmobile's/wayprint's own `core/navigation` (identical
  to each other) is the older, narrower API this module's design superseded. Confirmed
  differences as of 2026-09-08 (wallosmobile `dev`):
  - `NavigationState.subStacks` is `Map<NavKey, NavBackStack<NavKey>>` (instance-keyed)
    in wallosmobile vs. `Map<KClass<out NavKey>, NavBackStack<NavKey>>` (class-keyed)
    here — a real type change at every call site that constructs `NavigationState`, not
    just an import rename.
  - `Navigator.navigate()`/`goToTopLevel()` compare by instance equality
    (`key == state.startKey`, `when (key) { state.currentTopLevelKey -> ... }`) in
    wallosmobile vs. by `key::class` here throughout.
  - No `replaceCurrent`/`resetTo`/`ResultBus` exist in wallosmobile's version at all —
    additive, not a compatibility risk by itself.
  - In practice, instance-keyed vs. class-keyed behave identically *if every top-level
    route in the consuming app is a payload-less `data object`* (a singleton has only
    one instance, so instance-equality and class-equality agree). The risk is
    behavioral, not just a compile error, only if a top-level route carries a payload —
    check the app's actual route definitions before assuming this is safe, don't infer
    it from the type-checker alone.

- **wallosmobile/wayprint's `goToTopLevel()` has the *pre-fix* shape of the same bug
  0.1.1 fixed for TaigaMobileNova — resolved for wallosmobile 2026-09-08, wayprint still
  needs to ask its own owner.** Found during wallosmobile's own swap: wallosmobile's
  `Navigator.goToTopLevel()` pushes onto `topLevelStack` on every drawer-section switch
  (same push-not-replace shape as `grappim-kit-navigation:0.1.0`'s bug), so switching
  sections grows the stack and `goBack()`/`canGoBack()` cascade back through every
  previously-visited section before falling through to system back — and wallosmobile's
  own `NavigatorTest.kt` (`` `canGoBack is false only at the start destination` ``)
  asserted this growing behavior as intended, not as a bug. `grappim-kit-navigation`
  (both 0.1.0 and 0.1.1) never had this test — 0.1.1's version always *replaces* the
  top-level stack's single entry, so back at any section's root is unhandled (system
  back/exit) instead of cycling through visited sections. Swapping onto
  `grappim-kit-navigation` therefore changes real back-button UX across drawer sections,
  not just internals — a product decision, not a mechanical migration detail.
  **wallosmobile's owner (gregory) was asked directly and chose to adopt the fixed
  (replace, not push) behavior** — landed in wallosmobile PR #58
  (`chore/grappim-kit-navigation` → `dev`), rationale recorded in that repo's
  `docs/IMPLEMENTATION_PLAN.md` §5.4. wayprint starts from the identical pre-fix
  `core/navigation` wallosmobile did (confirmed narrower/identical per this section's
  intro above), so its session will hit the exact same behavior change on swap — **ask
  wayprint's own owner before landing there too**; gregory's wallosmobile answer isn't
  automatically wayprint's answer, even though both apps' current behavior and the fix's
  effect are identical.

- **Fixed 2026-09-09 (found by TaigaMobileNova the same day): `resetTo()` never disposed a
  top-level screen's `ViewModelStore` when that screen's `NavKey` is a payload-less
  singleton (`data object`) — the overwhelmingly common shape for a top-level/tab-root
  key.** `resetTo()` "resets" a section by writing the target key back into a
  `NavBackStack` slot (`stack[0] = key`) rather than removing the old entry and pushing a
  new one. Real Nav3's `ViewModelStoreNavEntryDecorator` only disposes a `ViewModelStore`
  when its `NavEntryDecorator.onPop` fires, which only happens when the entry's
  `contentKey` structurally *disappears* from the tracked backstack list (`equals()`-based
  diffing, traced in `navigation3-runtime`/`lifecycle-viewmodel-navigation3` sources — see
  `agentic-grappim`'s `mobile-patterns` skill, Navigation section, for the full mechanism).
  Writing the same singleton object back is a no-op from that diff's perspective, so the
  disposal never fired — any `@KoinViewModel`/`viewModel()` resolved at that top-level
  entry survived `resetTo()` indefinitely, including across an app's own "logout" reset.
  Confirmed via TaigaMobileNova's own bug: `DashboardViewModel` kept showing a previous
  account's data after logout→login (inside one continuous process — a fresh process
  hides this, since it never had the stale instance to begin with) until a manual
  pull-to-refresh re-ran its fetch on the same surviving instance. TaigaMobileNova worked
  around it at the app level (wrap the nav host's composition root in
  `key(sessionGeneration)`, bumped on logout — see its `composeApp/.../main/MainScreen.kt`)
  before this library-level fix existed; that workaround is still safe to keep (it just
  becomes redundant, tearing down a slightly larger subtree than necessary) but can now be
  narrowed to rely on this fix instead if the app wants to.

  **Correction to the original report:** `goToTopLevel()` (the private function `navigate()`
  calls when switching tabs) does the same "write into a slot" thing but is *not* part of
  this bug — a tab switch is supposed to preserve the section you're leaving, and
  `goToTopLevel()` never truncates the target section's own sub-stack, so its
  `ViewModelStore` surviving a tab switch is the documented, intended feature ("switching
  sections keeps each one's history"), not a leak. Only `resetTo()`'s documented "forget
  everything, start fresh" contract was actually broken.

  **The fix** (`navigation` module, commit `cf31cce`): `NavigationState`
  gained a `resetGeneration: Int` counter; `resetTo()` now increments it in addition to its
  existing stack surgery; `toEntries()` wraps its whole per-section decoration step in
  `key(resetGeneration) { ... }`. A `key()` value change makes Compose discard and recreate
  that entire composition group from scratch — which reliably disposes every section's
  `ViewModelStore` (and saveable state) via Compose's own composition lifecycle, sidestepping
  the equals()-based diffing that can never observe a payload-less singleton key "change" at
  all. `navigate()`/`goToTopLevel()`/`goBack()` never touch `resetGeneration`, so ordinary
  tab switching and back navigation still preserve state exactly as before — confirmed via
  regression tests (`NavigatorTest`: `resetTo bumps resetGeneration even when the target is
  a payload-less singleton key`, `navigate and goBack never touch resetGeneration`).
  **Not verified end-to-end on a real Compose UI** — this module's test suite is
  state-only (no `commonTest`/`androidTest` Compose runtime dependency exists here yet), so
  the `key()`-forces-disposal mechanism is verified by Compose's own documented semantics
  (identical in kind to TaigaMobileNova's own already-production-verified
  `key(sessionGeneration)` workaround, just scoped to the nav-entry subtree instead of the
  whole screen) rather than by an automated test here. **Any consumer swapping onto the
  version that ships this fix should smoke-test its own logout→login flow on device before
  relying on it**, same as any other `grappim-kit` swap.

## uikit (`grappim-kit-uikit`)

Reconciled from `wallosmobile`'s/`TaigaMobileNova`'s `uikit` theme scaffolding and top-bar
widgets (see `grappim-watcher/docs/SHARED_LIBRARY_PLAN.md`'s "Step 6" section for the
original per-field reconciliation call, and "Step 8f" for what changed extracting it).

- **`KitTheme`/`KitPreviewTheme` take `lightColorScheme`/`darkColorScheme`/`typography` as
  parameters — they don't exist as module-internal constants like each app's original
  `Theme.kt` had them.** Brand colors and typography are correctly not shareable (each
  app's own `Color.kt`/`Type.kt` stays where it is); the app builds its own
  `ColorScheme`/`Typography` and passes them in, same shape as before, just as arguments
  instead of hardcoded vals.
- **`TopBar`'s Back/Menu content descriptions are caller-supplied
  (`backContentDescription`/`menuContentDescription` params), not resolved from a bundled
  string resource.** Every other content description in this module (`TopBarAction`,
  `NavigationIconConfig.Custom`) already worked this way. **Correction (2026-09-09, confirmed
  consuming TaigaMobileNova, PR #394):** TaigaMobileNova's own pre-port `TaigaTopAppBar` did
  *not* resolve these from a bundled string resource either — it hardcoded plain `"Back"`/
  `"Menu"` string literals (no `RString` entry existed for either). Whichever app's code this
  paragraph originally described, it wasn't TaigaMobileNova's; the caller-supplied design is
  still the right call (removes the string-resource-generation requirement from `grappim-kit`
  entirely and is more consistent with the rest of the module), it just isn't a strict
  behavior-preserving port for an app that never had localized Back/Menu strings to begin
  with — that app's call site passes the same hardcoded literals through instead. Typically
  only one call site (the shell composable that renders `TopBar`) needs to supply these, not
  every screen.
- **`KitPreviewTheme` only wires `LocalTopBarConfig`, not an offline/snackbar composition
  local.** wallosmobile's own `WallosMobilePreviewTheme` (the richer of the two source
  apps') also wires `LocalIsOffline`/`LocalSnackbarHostController` from its own
  `widgets/network`/`widgets/snackbar` — TaigaMobileNova has neither, and they were never
  diffed or decided as shareable. An app that needs those in its own previews should wrap
  `KitPreviewTheme` in its own preview theme that adds them, rather than `grappim-kit`
  silently growing app-specific composition locals.
- **`RDrawable.kt` was *not* ported, despite `SHARED_LIBRARY_PLAN.md`'s original "extract
  wholesale" verdict for it.** That verdict only compared file text (`typealias RDrawable =
  Res.drawable`, identical in both apps) — but each app's `Res.drawable` points at that
  app's *own* bundled drawables (wallosmobile's is just its own logo). There's nothing for
  `grappim-kit` to bundle here; each app keeps its own one-line version of this pattern
  locally.
- **`NativeText` ships the richer union (TaigaMobileNova's: `Empty`/`Simple`/`Resource`/
  `Arguments`/`Plural`/`Multi`), not wallosmobile's narrower one** — the established
  richer-union pattern from every other "undersold drift" case in this project. Each
  variant only ever holds an opaque `StringResource`/`PluralStringResource` pointer the
  *app* generated; `NativeText` itself needs no resource generation. `getErrorMessage`
  (each app's own exception→message mapping) was **not** ported — that's app-specific
  business logic using each app's own exception types, not part of the shareable type.

**Findings from wallosmobile's swap (2026-09-09), not caught by the extraction verdict above:**

- **`KitTheme` unconditionally wraps `LocalUriHandler` in a `SafeUriHandler`** (refuses any
  non-http(s) scheme before delegating) with **no opt-out** — this wasn't called out in the
  bullets above, and it is a real, app-visible behavior change for a consumer that never had
  this guard. wallosmobile's own security register had explicitly recorded the *absence* of a
  scheme allowlist as an accepted deviation (its two `openUri` call sites are fixed https
  `RString` resources, no untrusted input) — swapping onto `KitTheme` closed that gap for free,
  but only because it happened to be harmless there. **Check the consuming app's own
  `LocalUriHandler.openUri()` call sites before swapping**: if any of them ever pass through
  user- or server-supplied text and would need to keep allowing something `SafeUriHandler`
  refuses, this is a hard blocker, not a config knob, since `KitTheme` has no way to skip it.
- **`TopBarActionIconButton`/`TopBarActionVectorButton`/`TopBarActionTextButton` each gained an
  `enabled: Boolean = true` parameter**, not present in either source app's original type. It's
  additive and defaulted, so it doesn't break an existing call site — but it's undocumented
  above and worth knowing about before assuming the action types are an exact structural match.
- **`uikit`'s own `api` dependency is where `NativeText`/`getErrorMessage`/`ObserveAsEvents`
  reach a consumer's other modules, and swapping it is a breaking change for anyone who relied
  on the old transitive edge.** wallosmobile's `uikit` module used to declare
  `api(projects.utils.ui)` (for `TopBarConfig`'s `NativeText`); every feature module that used
  `utils:ui`'s `getErrorMessage`/`ObserveAsEvents` — but only ever declared `implementation(uikit)`,
  never `utils:ui` directly — got them for free through that edge. Swapping `uikit`'s `api` target
  to `grappim-kit-uikit` breaks every one of those call sites (`Unresolved reference 'utils'`)
  until each such module adds its own direct `implementation(projects.utils.ui)` line. Worth
  checking for the same shape in any other app before swapping: grep the consumer for symbols
  from whatever app-local module fed `uikit`'s old `api` edge, not just for the types
  `grappim-kit-uikit` itself now provides.

**Findings from consuming this module in TaigaMobileNova (2026-09-09, PR #394)** — none of these
were called out above, so a consuming app should check for them rather than assume the swap is a
behavior-preserving no-op:

- **`KitTheme`/`KitPreviewTheme` wrap `content` in `Surface(Modifier.fillMaxSize())`
  unconditionally — including in `KitTheme` itself, not only `KitPreviewTheme`.**
  TaigaMobileNova's original `TaigaMobileTheme` (the real app-root theme) had no `Surface` at
  all; only its `TaigaMobilePreviewTheme` wrapped content in one. This is a real, if likely
  desirable, behavior addition at the app's root: it paints `colorScheme.surface` and provides
  a default `LocalContentColor` for any screen composed with no `Scaffold` of its own (a login
  screen is the concrete case — without this, that screen's background is whatever the OS
  window provides, and Compose's default text color wins, which is black-on-black in dark
  mode). GUI-verified correct for TaigaMobileNova's login screen in both light and dark mode,
  but a consuming app should not assume this is a no-op purely because its main screens already
  have their own `Scaffold` — check every screen that's composed before any `Scaffold` mounts
  (loading/splash screens are the other common case).
- **`TopBar` wraps its `CenterAlignedTopAppBar` in `AnimatedVisibility` (slide in/out)** where
  TaigaMobileNova's original `TaigaTopAppBar` used a plain `if (isVisible) { ... }` with no
  animation. Additive/cosmetic, not a correctness issue, but worth knowing before assuming a
  swap changes nothing visually.
- **`TopBarAction` gained a third variant, `TopBarActionVectorButton`** (`ImageVector`-based,
  alongside the pre-existing `DrawableResource`-based `TopBarActionIconButton` and
  `TopBarActionTextButton`) — additive, not consumed by TaigaMobileNova, but note it for any
  `when` a consuming app writes over `TopBarAction` itself (none exists in `grappim-kit`'s own
  `TopBar.kt` outside this module, since `TopBar` is the only place matching over it).
- **`KitTheme` has no accommodation for a platform-varying `ColorScheme`** (e.g. Android's
  Material You `dynamicLightColorScheme`/`dynamicDarkColorScheme`, resolved per-platform via
  `expect`/`actual` in TaigaMobileNova's case) — it takes two fixed `ColorScheme` values plus a
  `darkTheme` flag to pick between them. An app with per-platform theme resolution keeps that
  logic entirely local (it has nothing to do with brand colors/typography, which is what
  `KitTheme`'s parameters are for) and adapts it to `KitTheme`'s shape by computing the
  platform-correct scheme once for the current mode, then passing it as *both* the
  `lightColorScheme` and `darkColorScheme` arguments with `darkTheme = false` — this forces
  `KitTheme`'s internal `if (darkTheme) darkColorScheme else lightColorScheme` to always
  resolve to the already-correct value without evaluating the platform call twice. See
  TaigaMobileNova's `uikit/.../theme/Theme.kt` (PR #394) for the worked example: it keeps its
  own `expect fun colorScheme(darkTheme: Boolean)` (Android dynamic-color branch included) and
  wraps it exactly this way in `TaigaMobileTheme`/`TaigaMobilePreviewTheme`.
- **`ObserveAsEvents` (Compose helper for one-off event `Channel`/`Flow` consumption) was
  added into `uikit` 2026-09-09**, not its own module — it was byte-identical (apart from
  package) between wallosmobile's and TaigaMobileNova's `utils/ui`, alongside `uikit`'s
  existing `SafeUriHandler` as a small general-purpose Compose utility, not a
  theme/top-bar widget. wayprint has no `utils/` at all. Neither source app tested it, and
  `grappim-kit` still has no Compose-UI-test infrastructure (see the `navigation` section's
  `resetTo()` note), so no test was added here either — worth adding if/when that
  infrastructure exists. Not yet swapped onto by any app.

## appupdate (`grappim-kit-appupdate`, `grappim-kit-appupdate-gplay`, `grappim-kit-appupdate-fdroid`)

Extracted 2026-09-09 from wallosmobile's and TaigaMobileNova's near byte-identical
`AppUpdateChecker` (Play In-App Update wrapper). Not yet swapped onto by any app —
this section covers the extraction shape, not swap findings.

- **Three separate artifacts, not one module.** Unlike every other `grappim-kit` module,
  the source apps split this into an interface plus two *build-variant-specific*
  implementations (a real Play-Core-backed one for the Google Play distribution, a no-op
  stub for F-Droid/non-Play). `grappim-kit`'s existing modules are single-artifact KMP
  libraries with no product-flavor concept, so rather than invent a flavor-aware module (an
  open question — the KMP Android library plugin's flavor support wasn't investigated),
  this shipped as three plain artifacts: `grappim-kit-appupdate` (the `AppUpdateChecker`
  interface + `UpdateState`), `grappim-kit-appupdate-gplay` (`api`-depends on `appupdate`,
  adds `com.google.android.play:app-update-ktx`), `grappim-kit-appupdate-fdroid`
  (`api`-depends on `appupdate`, no extra dependency). A consuming app's `gplayImplementation`
  source set takes `-gplay`, its `fdroidImplementation`/equivalent takes `-fdroid` — same
  shape as the two apps' own `androidApp/src/gplay`/`src/fdroid` split, just as separate
  Maven coordinates instead of separate source sets in one module.
- **Android-only, no `commonMain`.** `Activity` and the Play In-App Update APIs this models
  don't exist off Android, and neither source app ever had this as KMP-common code either —
  both kept it entirely inside their `androidApp` module. All three new modules declare only
  an `androidLibrary` target, no `jvm()`/iOS.
- **No Koin annotation on either impl**, matching the established convention from
  `TrustedCertStorage`/`SecretCipher` (`storage`/`trustmanager` modules) — both source apps'
  `@Single(binds = [AppUpdateChecker::class])` was dropped; a consuming app binds whichever
  impl artifact it depends on into its own Koin module.
- **HateItOrRateIt has its own `AppUpdateChecker`-shaped code but it's structurally
  different** (no-arg methods, `@ActivityScoped`/`@Inject`, `ActivityContext`) because that
  app runs Hilt/Dagger, not Koin like the other three (see grappim-watcher/CLAUDE.md's
  cross-cutting-facts note) — it was deliberately left out of this extraction, not silently
  dropped. Folding it in later means reconciling across the DI-framework boundary, not a
  wholesale copy.
- **wayprint has no equivalent at all** (FOSS-only distribution, no update-check need) —
  nothing to reconcile there either.

## logger (`grappim-kit-logger`)

Swapped onto by wallosmobile 2026-09-09 (PR #71) — first consumer. Re-diffed 0.1.3's published
source against wallosmobile's `dev` HEAD before swapping, per the standing rule above:
`Logcat.kt`, `LogPriority.kt`, `KitLogger.kt` (wallosmobile's `WallosLogger.kt`) and the
android-artifact's `TimberLogger.kt` were all byte-identical apart from the expected package
rename (`com.grappim.kit.logger` vs. `com.grappim.wallosmobile.core.logger`) and class rename
(`KitLogger` vs. `WallosLogger`) — no drift found, unlike `navigation`'s `0.1.0`.

- **A mechanical swap, same as `navigation` for TaigaMobileNova** — no behavior change, no
  product decision to confirm with the app owner. wallosmobile's `core:logger` was the module
  `grappim-kit-logger` was extracted from in the first place (see this repo's own `CLAUDE.md`),
  so there was nothing to reconcile.
- **Check how the consuming app wires the old local module in before assuming it's a per-module
  dependency line like `navigation`/`uikit`.** wallosmobile's own `build-logic` hardcoded
  `implementation(project(":core:logger"))` into every KMP library module via its
  `configureKmp()` convention function (one central call, not a per-module `build.gradle.kts`
  line) — the swap there is a single edit in that one file
  (`implementation(libs.grappim.kit.logger)`), not 28 separate `build.gradle.kts` additions the
  way `uikit`'s per-module `api`/`implementation` lines were. Only a plain Android application
  module that never goes through `configureKmp()` (wallosmobile's `androidApp`) needs its own
  explicit dependency line.
- **A class that only *implements* the module's interface as a test fake still needs the same
  import/class rename as an ordinary consumer.** wallosmobile's `WallosEnvelopeParserTest.kt`
  declares a `commonTest`-local `RecordingLogger : WallosLogger` fake — not part of the
  extraction itself, but it broke the same way any other `WallosLogger` reference did and needed
  the same `KitLogger` rename.

**Swapped onto by TaigaMobileNova 2026-09-09 (PR #413) — second consumer.** Verified more strictly
than the wallosmobile pass: instead of diffing against a local `grappim-kit` checkout, downloaded
the actual published `grappim-kit-logger-{,android,jvm}-0.1.3-sources.jar` files from Maven Central
(the module publishes three separate artifacts — root/common, `-android`, `-jvm` — each with its
own sources jar; the root one's jar carries `commonMain` + `iosMain`, not just `commonMain`) and
diffed those directly. Byte-identical, same as wallosmobile's finding — confirms the extraction
itself, not just wallosmobile's checkout state at extraction time, is what's clean.

- **The `build-logic` centralization gotcha wallosmobile found is not wallosmobile-specific.**
  TaigaMobileNova's own `KmpConfiguration.kt` had the identical shape (`implementation(project(
  ":core:logger"))` hardcoded once inside `configureKmp()`, guarded by
  `if (project.path != ":core:logger")` since the module can't depend on itself) — same
  single-line swap to `libs.grappim.kit.logger`, no per-module edits needed. Worth checking for
  on every future `core/*` swap across any of the four apps, not just re-confirming per-app.
- **TaigaMobileNova ships all three backends (`TimberLogger`/`NSLogLogger`/`FileLogger`), unlike
  wallosmobile's Android-only usage** — this is the first swap to touch the iOS and desktop/JVM
  install call sites (`main.ios.kt`'s `NSLogLogger.install()`, `TaigaMobileDesktop.kt`'s
  `FileLogger.install(...)`), not just `TimberLogger.install()`. All three renamed cleanly; no
  platform-specific gotcha turned up.
- **A mechanical import/class rename script misses prose comments that name the old
  module/class without an import statement.** Two files —
  `composeApp/src/iosMain/.../CrashReporterImpl.ios.kt` and
  `composeApp/src/jvmMain/.../CrashReporterImpl.jvm.kt` — had a code comment reading "logs via
  core/logger's NSLogLogger/FileLogger instead of this interface" with no `import` line on the
  same file to catch a package-rename `sed` pass. Caught by a manual repo-wide grep for
  `core.logger`/`core:logger`/`TaigaLogger` after the mechanical rename, not by the rename itself.
  Same category of miss as `uikit`'s implicit-same-package-resolution gotcha above, different
  mechanism (a comment, not a resolvable reference) — grep broadly for the old name in prose too,
  not just in code that would fail to compile.

## coroutines (`grappim-kit-coroutines`)

Swapped onto by wallosmobile 2026-09-11 — first consumer. Re-diffed 0.1.4's published
`sources.jar` (downloaded from Maven Central, not a local `grappim-kit` checkout) against
wallosmobile's `core/async-kmp` on `dev` HEAD before swapping, per the standing rule above:
`KitDispatchers.kt`/`ApplicationScope.kt` matched wallosmobile's own consuming session's
briefing exactly — `KitDispatchers.default/io/main/mainImmediate` map straight to
`Dispatchers.Default/IO/Main/Main.immediate`, `applicationScope(dispatcher =
KitDispatchers.default)` returns `CoroutineScope(SupervisorJob() + dispatcher +
exceptionHandler)`. No drift found.

- **This module is DI-framework-agnostic by design (no Koin annotations), unlike every
  other `grappim-kit` module swapped onto so far** — the consuming app keeps its own thin
  Koin wrapper. wallosmobile's `core/async-kmp` kept its five local `@Qualifier`
  annotations (`DefaultDispatcher`/`IoDispatcher`/`ApplicationScope`/`MainDispatcher`/
  `MainImmediateDispatcher`) and `@Single`-annotated provider functions exactly as they
  were, just re-sourcing each one from the plain library instead of raw
  `kotlinx.coroutines`: `Dispatchers.Default` → `KitDispatchers.default`,
  `CoroutineScope(SupervisorJob() + defaultDispatcher)` → `applicationScope(defaultDispatcher)`,
  etc. No consumer of the qualifier annotations elsewhere in the app needed any change —
  they're still wallosmobile's own types, just backed by a different implementation.
- **Real behavior change, not just a mechanical swap: `applicationScope()` installs a
  `CoroutineExceptionHandler` that wallosmobile's own hand-rolled version never had.**
  wallosmobile's pre-swap `provideApplicationScope()` was a bare
  `CoroutineScope(SupervisorJob() + defaultDispatcher)` — an exception escaping a coroutine
  launched on it had no handler and crashed the app via the dispatcher thread's default
  uncaught-exception behavior. `grappim-kit`'s `applicationScope()` wraps a
  `CoroutineExceptionHandler` that logs the throwable via `logcat(LogPriority.ERROR,
  throwable = throwable)`. In wallosmobile's own error-handling convention (see its
  `CLAUDE.md`), an `ERROR`-level log with a non-null `throwable` is exactly what its
  `CrashlyticsTree` forwards to Crashlytics — so on the `gplay` flavor this swap turns what
  used to be an app crash into a caught, reported-but-non-fatal error instead. Worth
  flagging explicitly to any consumer that relied on (or tested for) the old crash-on-escape
  behavior; nothing in wallosmobile did, so this was accepted as a strict improvement, not
  reconciled against a product decision.
- **`core:async-kmp` was already a per-module explicit dependency in wallosmobile
  (`projects.core.asyncKmp` in each consuming module's own `build.gradle.kts`), not
  centrally injected via `build-logic` the way `core:logger` was** — so unlike the logger
  swap, this one needed no `build-logic` edit at all. The swap is contained entirely inside
  `core/async-kmp` itself: one new `implementation(libs.grappim.kit.coroutines)` line in its
  `build.gradle.kts`, plus the provider-function bodies above. Every other module still only
  ever sees wallosmobile's own qualifier annotations and `CoroutineDispatcher`/`CoroutineScope`
  types, never anything from `com.grappim.kit.coroutines` directly.
- **`ThreadSafeMap<K, V>` (mutex-guarded map, also in this module) has no equivalent
  anywhere in wallosmobile and was not consumed by this swap** — grepped for
  `ThreadSafeMap`/`SynchronizedObject`/`synchronized(` repo-wide, zero hits before or after.
  Noted for whichever future need reaches for it, not proven safe or unsafe by this swap.
- **Verified**: `compileGplayDebugKotlin --rerun-tasks` (forces the Koin compiler plugin to
  re-scan `@ComponentScan` after the DI-relevant change) green, both `assembleGplayDebug
  -PgplayBuild`/`assembleFdroidDebug`, `allTests`, `detekt ktlintCheck`, both flavors'
  Android lint all green. **Device-verified**: cold start on
  `Medium_Phone_API_36.1` (gplay debug, `-PgplayBuild` + real `google-services.json` — the
  project's own documented gotcha for a working gplay cold start) resolves the whole Koin
  graph and renders the dashboard from the already-logged-in session's cache with no crash.

**Swapped onto by TaigaMobileNova 2026-09-11 (PR #421) — third consumer, second app.** This app
is the module's actual extraction source (see this repo's own `CLAUDE.md`), so the diff was a
formality — downloaded the published `grappim-kit-coroutines-0.1.4-sources.jar` from Maven
Central and diffed all three files against the local `grappim-kit` checkout's HEAD directly
(not against the app's pre-swap `core/async-kmp`, which is definitionally the same code with a
package rename): byte-identical, confirming the artifact matches what actually shipped.

- **Unlike wallosmobile, this app's `core/async-kmp` had a real consumer of `ThreadSafeMap`**
  (`WorkItemEditStateRepository`, keying `WorkItemEditStateRepository`'s per-item edit sessions —
  see this app's `CLAUDE.md`). wallosmobile's `implementation(libs.grappim.kit.coroutines)` isn't
  enough once a consumer needs a type from the library directly through the local module: had to
  use `api(...)` instead so `ThreadSafeMap` stays resolvable one hop away, same as any other
  transitively-exposed type. A plain import-path rename
  (`com.grappim.taigamobile.core.asynckmp.ThreadSafeMap` →
  `com.grappim.kit.coroutines.ThreadSafeMap`) was the only change needed at the call site — the
  type itself was byte-identical apart from the package.
- **`core:async-kmp` was also already a per-module explicit dependency here** (same shape as
  wallosmobile, ~30 `implementation(projects.core.asyncKmp)` lines across feature modules), not
  centralized in `build-logic` — confirms this is a per-app fact to check, not something you can
  infer from one prior swap (this repo's `core:logger` swap *was* centralized in both apps'
  `build-logic`, which made both true statements look like they might generalize; `core:async-kmp`
  breaks that pattern in both apps identically, for the same reason: it was already per-module
  before either app touched grappim-kit).
- **The local `KmpCoroutinesModule`'s own `commonTest` needed rewriting, not just deleting.**
  Its one test asserted the `CoroutineExceptionHandler`-logs-not-crashes behavior end-to-end —
  now redundant, since `grappim-kit-coroutines`'s own `ApplicationScopeTest` proves that at the
  library level. Replaced it with a thin delegation test (`provideApplicationScope` passes the
  given dispatcher through to `applicationScope()`, asserted via
  `scope.coroutineContext[ContinuationInterceptor]`) rather than dropping coverage of the wiring
  itself — same "commonTest proves delegation happens" convention this app already uses for
  `expect`/`actual` platform code (see its `CLAUDE.md`, Testing section).
- **Verified**: full `./gradlew jvmTest` (all modules, not just `core:async-kmp`) green,
  `koverXmlReport`/`:koverVerify` (coverage floor holds), `ktlintCheck` green,
  `:composeApp:compileKotlinJvm --rerun-tasks` (forces the Koin compiler plugin to re-scan) +
  `KoinGraphTest` (147 definitions checked, no new failures — the 15 listed are this app's
  pre-existing, documented `@InjectedParam` NavDestination exclusions, unrelated to this swap).
  **Device-verified**: `:composeApp:run` (desktop) boots through the full Koin graph to a real
  screen (`LoginNavDestination`, confirmed via the app's own file log) with no DI wiring crash —
  this app treats JVM/desktop as a fully-supported platform for `expect`/`actual` verification
  (see its `CLAUDE.md`), not a stand-in for Android/iOS.

## crash (`grappim-kit-crash`)

Swapped onto by wallosmobile 2026-09-11 — first consumer. Diffed the local `grappim-kit`
checkout's `crash/src/commonMain/.../CrashReporter.kt` against wallosmobile's own
`core:crashreporting-api` on `dev` HEAD before swapping, per the standing rule above:
byte-identical apart from the package rename (`com.grappim.kit.crash` vs.
`com.grappim.wallosmobile.core.crashreportingapi`) and no local module-level KDoc. No
drift found.

- **A mechanical swap, same as `logger` for wallosmobile** — no behavior change. Both
  flavor implementations (`androidApp/src/gplay/.../di/CrashReporterImpl.kt`,
  `androidApp/src/fdroid/.../di/CrashReporterImpl.kt`) needed only the import rename, no
  body changes.
- **Neither `core:crashreporting-api` nor `core:appinfo-api` was centrally wired via
  `build-logic`** (unlike `core:logger`'s `configureKmp()` hardcoding) — both were already
  a per-module explicit `build.gradle.kts` dependency line in every consuming module
  (`core:api`, `composeApp`, `feature:settings:ui`, `testing`, `androidApp`), same shape
  `coroutines`' own `core:async-kmp` swap found. Confirms this is a per-module fact to
  check on every future `core/*` swap, not something to infer from one prior result —
  `core:logger`'s centralization and `core:async-kmp`/`core:crashreporting-api`/
  `core:appinfo-api`'s per-module wiring coexisted in the same app the whole time.
- **`:testing`'s `FakeCrashReporter` needed only the same import rename as any other
  consumer** — it's a hand-written fake (CLAUDE.md: no mocking libraries), not part of
  this extraction, but broke the same way any other `CrashReporter` reference did.
  `grappim-kit-testing`'s own `FakeCrashReporter` (not yet swapped onto — out of scope for
  this pass, the peer briefing that authorized this swap explicitly excluded `testing`)
  turned out identical in shape (same three call-recording lists, same defaults) once
  compared side by side — worth revisiting as a follow-up swap now that both fakes are
  confirmed structurally interchangeable.

**Swapped onto by TaigaMobileNova 2026-09-11 (PR #422) — second consumer, second app,
alongside `appinfo` below.** Diffed the published `0.1.4` sources (`maven-metadata.xml`
confirmed both `grappim-kit-crash` and `grappim-kit-appinfo` release at 0.1.4, matching the
local `grappim-kit` checkout's HEAD — the extraction commit for both was the only one to
touch either module directory since the 0.1.4 version bump) against this app's own
`core/crash-api` on `dev` HEAD.

- **Byte-identical, same as wallosmobile's finding** — this app's local `CrashReporter` had
  the exact same four members with no drift. All four platform implementations
  (`androidApp/src/gplay`, `androidApp/src/fdroid`, `composeApp/src/iosMain`,
  `composeApp/src/jvmMain`) needed only the import rename.
- **`core:crash-api` was already a per-module dependency here too** (`core:api`,
  `composeApp`, `feature:settings:ui`, `testing`, `androidApp`) — third app-independent
  confirmation of the "check per-module wiring every time" finding above.
- Deleted the local `core/crash-api` module outright (mirrors wallosmobile's own crash swap
  and this repo's own `navigation`/`logger` precedent for a byte-identical interface with no
  surviving local-only members).

## appinfo (`grappim-kit-appinfo`)

Swapped onto by wallosmobile 2026-09-11 — first consumer, alongside `crash` above. Diffed
the local `grappim-kit` checkout's `appinfo/src/commonMain/.../AppInfoProvider.kt`
against wallosmobile's own `core:appinfo-api` on `dev` HEAD before swapping.

- **Not a byte-identical swap — `AppInfoProvider` here is a strict superset.**
  wallosmobile's own interface only had `isDebug()`/`versionName()`/`versionCode()`; the
  kit's adds `isFdroidBuild(): Boolean` and `buildType(): String`. Every implementation
  and hand-written test double of the interface (production `AppInfoProviderImpl`, and a
  `commonTest`-local anonymous `FakeAppInfoProvider` in
  `feature/settings/ui/.../AboutViewModelTest.kt`) needed the two new overrides added or
  the module fails to compile — not optional, since Kotlin requires every interface
  member implemented. wallosmobile had no pre-existing concept of `isFdroidBuild()`
  (its own `AboutViewModel`/`InterfaceViewModel` instead read `crashReporter.isAvailable`
  as the flavor-fact proxy, since that's `true` only on the flavor that also has real
  crash reporting) — the new methods are implemented but not yet consumed by any screen.
  **Check what a consumer's existing interface is missing relative to this one before
  assuming any swap onto `appinfo` is mechanical** — this is the first swap where the kit
  module turned out richer than the app's own original, the opposite direction from every
  prior "kit interface is byte-identical or a subset" finding in this file.
- **`isFdroidBuild()`'s only sane implementation reads `BuildConfig.FLAVOR`**, which AGP
  generates automatically for every module with `buildConfig = true` (no explicit
  `buildConfigField` needed) — `BuildConfig.FLAVOR == "fdroid"` for wallosmobile, matching
  its `AppFlavors.FDROID.title` from `build-logic` (not directly reachable from app
  runtime code — `build-logic` is buildscript-only — so the flavor name is duplicated as a
  string literal at the implementation site, not shared). `buildType()` is a straight
  pass-through of `BuildConfig.BUILD_TYPE`. Both fields exist on `BuildConfig` for any AGP
  module regardless of whether the app declares them itself — worth checking before
  assuming a consumer needs new `buildConfigField` plumbing to implement these two
  methods; it doesn't.
- **Same per-module (not `build-logic`-centralized) wiring finding as `crash` above** —
  see that section.
- **Verified** (both `crash` and `appinfo` together, one swap): `compileGplayDebugKotlin
  --rerun-tasks` (forces the Koin compiler plugin to re-scan after the DI-relevant
  package changes) green, `assembleFdroidDebug`/`assembleGplayDebug -PgplayBuild`,
  `allTests`, `detekt ktlintCheck`, both flavors' Android lint all green.
  **Device-verified**: cold start on `Medium_Phone_API_36.1` (gplay debug,
  `-PgplayBuild` + real `google-services.json`) resolves the whole Koin graph and renders
  the dashboard from the already-logged-in session's cache with no crash; Settings →
  Interface shows the crash-reporting toggle (`crashReporter.isAvailable == true` on
  gplay); Settings → About shows the correct version/build (`1.0.3 (4)`, `Debug`) and the
  gplay-only Privacy Policy button, both driven by the swapped interfaces.

**Swapped onto by TaigaMobileNova 2026-09-11 (PR #422) - second consumer, second app,
alongside `crash` above.**

- **The opposite direction from wallosmobile's finding: this app's own `AppInfoProvider`
  was a strict superset of the kit's, not a subset.** Alongside `isDebug()`/
  `isFdroidBuild()`/`getVersionName()`/`getBuildType()` (all present in the kit, just
  `get`-prefixed here), this app's interface also carried `getAppInfo(): String` and
  `getDebugLocalHost(): String`, neither of which the kit's reconciled shape has. Confirms
  the CLAUDE.md warning above ("check what a consumer's existing interface is missing *or
  carries extra* relative to this one") cuts both ways - a swap can just as easily need to
  carve members *out* of a consumer's interface as add them in.
- **`getAppInfo()` was a pure formatting concern (per this module's own doc comment) and
  moved into the ViewModel**, composed from `versionName()`/`versionCode()`/`buildType()`
  directly (`SettingsAboutScreenViewModel.buildAppInfo()`). This app's Android build used to
  append a fourth, flavor segment (`BuildConfig.FLAVOR`) that desktop/iOS never had (no
  flavor concept there) - since flavor isn't reachable through the kit's interface, the
  About screen's flavor suffix was dropped on Android too rather than reconstructed via
  `isFdroidBuild()` (which can't distinguish "not fdroid" from "not Android" on desktop/iOS).
  **Visible, user-facing behavior change**, flagged to the app owner rather than decided
  silently; not a defect in the kit.
- **`getDebugLocalHost()` had five real call sites this app couldn't drop** (a local-dev
  network-debugging feature unique to this app, unrelated to wallosmobile's simpler
  `AppInfoProvider` consumers): `core/api`'s `DebugLocalhostPlugin`, `core/storage`'s
  `DataStoreServerStorage`/androidMain `ServerStorageImpl`, and `androidApp`'s
  `ImageLoaderProvider`/`DebugLocalHostImageManager`. Since the kit's interface doesn't (and
  by design shouldn't) carry this, the local `core/appinfo-api` module was kept alive but
  narrowed to a single-method `DebugLocalHostProvider` interface; every platform
  `AppInfoProviderImpl` now implements both it and the kit's `AppInfoProvider` via
  `@Single(binds = [AppInfoProvider::class, DebugLocalHostProvider::class])` (koin-annotations'
  multi-bind syntax - untested elsewhere in this app before this swap, confirmed to work via
  a full DI-graph-resolving build check on all three targets). Each consumer now depends on
  only the interface(s) it actually calls - `DebugLocalhostPlugin` and
  `DebugLocalHostImageManager` take just `DebugLocalHostProvider`, not the kit's
  `AppInfoProvider` at all.
- **`versionCode(): Int` (new in the kit vs. this app's `String`-typed `BuildConfig.VERSION_CODE`
  on Android) needed a `.toInt()` at the iOS/JVM call sites** - this app's `BuildKonfig.VERSION_CODE`
  (a `buildConfigField(FieldSpec.Type.STRING, ...)` in `composeApp/build.gradle.kts`, unlike
  Android's real `Int`-typed `BuildConfig.VERSION_CODE`) is a numeric string constant, safe to
  parse unconditionally.
- **Same per-module (not `build-logic`-centralized) wiring finding as `crash` above.**
- **Verified**: full `./gradlew jvmTest` (all modules) green including rewritten
  `SettingsAboutScreenTest`/`DebugLocalhostPluginTest`/`DataStoreServerStorageTest`,
  `koverXmlReport`/`:koverVerify` (line 95.2%, branch 81.9% - floor holds), `ktlintCheck`
  green (two `standard:class-signature` violations from the new multi-superclass
  declarations, both auto-fixed via `ktlint*Format` rather than hand-formatted - see this
  app's own CLAUDE.md note on that trap), `:androidApp:assembleFdroidDebug` and
  `:composeApp:compileKotlinIosSimulatorArm64 --rerun-tasks` both green (Koin graph resolves
  the new multi-bind on both targets). **Desktop-verified**: `:composeApp:run` boots to the
  login screen with the server-URL field pre-filled from the debug local host - end-to-end
  proof the new `DebugLocalHostProvider` DI wiring actually works, not just that it compiles.
  A full click-through to the About screen itself was not completed - this machine's `xdotool`
  click reliability has been degraded since 2026-08-29 (see this app's own memory notes), and
  the format change is already covered by the automated Compose UI test above.

## domain (`grappim-kit-domain`)

Swapped onto by wallosmobile 2026-09-11 — first consumer. Diffed the local `grappim-kit`
checkout's `domain/src/commonMain/.../{PendingCertTrust,ResultExtension,
UntrustedCertificateException,CertificateHostnameMismatchException}.kt` against
wallosmobile's own `core:domain` on `dev` HEAD before swapping, per the standing rule
above.

- **Not a byte-identical swap — two real deltas, both flagged in the peer briefing that
  authorized this swap rather than found cold.** `PendingCertTrust`/`ResultExtension.kt`
  matched wallosmobile's originals exactly apart from package and KDoc wording (the kit's
  own KDoc drops a wallosmobile-specific `plan §4.5`/`(18.1)` citation, expected — those
  point at an app-local doc the kit can't reference). `UntrustedCertificateException` and
  `CertificateHostnameMismatchException` did not.
- **`UntrustedCertificateException` gained an optional `cause: Throwable? = null`
  constructor param** — wallosmobile's original was single-arg
  (`class UntrustedCertificateException(val pendingCertTrust: PendingCertTrust) :
  Exception()`). Checked wallosmobile's only throw site
  (`core/api/.../CompositeTrustManager.kt`, androidMain): it threw this bare inside a
  `catch (e: CertificateException)` block, discarding `e` — a real fix to pick up, not
  just a signature widening, so the swap also changed the throw site to
  `UntrustedCertificateException(pendingCertTrust(host, leaf), cause = e)`. Confirmed this
  can't change `findPendingCertTrust()`'s own behavior: it walks the cause chain via
  `filterIsInstance<UntrustedCertificateException>().firstOrNull()` and stops the moment
  it finds one, before ever looking at *that* exception's own `cause` — the new link only
  extends the chain *past* the node every existing caller already stops at, so it's pure
  additional diagnostic depth (visible to Crashlytics/logcat if this exception is ever
  logged with its full cause chain), not a behavior change for any existing consumer of
  `findPendingCertTrust()`. Worth checking on any other app's swap: search for every throw
  site of this type before assuming the added param is inert.
- **`CertificateHostnameMismatchException` is new — didn't exist in wallosmobile's
  original `core:domain` at all.** wallosmobile's own hostname-mismatch branch in
  `CompositeTrustManager.checkServerTrusted` (`if (host == null ||
  !hostMatchesCertificate(host, leaf)) throw e`) just rethrows the plain, unwrapped
  `CertificateException` — nothing there constructs or expects this type. **Left
  unwired in this swap** — wallosmobile's `CompositeTrustManager` (the `trustmanager`
  module-to-be) was explicitly out of scope for this pass per the authorizing briefing,
  so this exception type is currently inert in that app. A future `trustmanager` swap is
  where it either gets wired into that branch or stays unused; note this rather than
  assuming a "new type, no consumer" is itself a problem.
- **`core:domain` was kept, not deleted** (unlike the `crash`/`appinfo` swaps' full
  module removal) — it still owns wallosmobile's own `WallosError.kt`, deliberately left
  behind as app-specific per the authorizing briefing. Retargeted via a single
  `api(libs.grappim.kit.domain)` line in `core/domain/build.gradle.kts`: every one of the
  ~28 files across the app that imported `PendingCertTrust`/`UntrustedCertificateException`/
  `resultOf`/`mapResult`/`findPendingCertTrust` already reached them through an existing
  `implementation(projects.core.domain)` dependency, so `api` on the kit dependency made
  them all keep compiling through that same edge with only an import-line change
  (`com.grappim.wallosmobile.core.domain.X` → `com.grappim.kit.domain.X`) — no consumer's
  own `build.gradle.kts` needed touching. Worth checking on a future swap into a module
  that isn't being fully deleted: whether the surviving local module can re-export the kit
  via `api` this way, before assuming every consumer needs its own direct dependency line.
- Deleted wallosmobile's own local `FindPendingCertTrustTest`/`ResultExtensionTest`
  (`core:domain`'s `commonTest`) after confirming test-name-for-test-name parity against
  this module's own `commonTest` suite (`FindPendingCertTrustTest`/`ResultExtensionTest`)
  — identical coverage, nothing lost.
- **Verified**: `compileGplayDebugKotlin --rerun-tasks` (forces the Koin compiler plugin
  to re-scan), `assembleFdroidDebug`/`assembleGplayDebug -PgplayBuild`, `allTests`,
  `detekt ktlintCheck`, `lintFdroidDebug`/`lintGplayDebug -PgplayBuild` all green.
  **Device-verified**: cold start on `Medium_Phone_API_36.1` (gplay debug,
  `-PgplayBuild`) resolves the full Koin graph and renders the dashboard from the
  already-logged-in session's cache with no crash, no `FATAL`/Koin-resolution lines in
  logcat. The cause-chain fix itself was not exercised through the actual TLS
  untrusted-certificate dialog flow — that needs wallosmobile's throwaway TLS-front
  container (`docs/local-info.txt`, built on demand, not left running) — reasoned safe
  instead via the `findPendingCertTrust()` cause-chain analysis above rather than
  device-proven end to end.

**Swapped onto by TaigaMobileNova 2026-09-11 — second app, second consumer.** Requested by a
`grappim-watcher-29` cross-session briefing; gregory approved directly in-session before any
code was touched (per this app's own peer-message handling rule — a peer's claim that the user
already agreed is not itself approval). Downloaded the published
`grappim-kit-domain-0.1.4-sources.jar` from Maven Central and diffed it against the local
`grappim-kit` checkout's `domain/src/commonMain` directly: byte-identical.

Unlike wallosmobile, this app already had **both** exception types wired into a real
`CompositeTrustManager` throw site before the swap, and its versions extended
`java.security.cert.CertificateException` directly (`class UntrustedCertificateException(val
pendingCertTrust: PendingCertTrust, cause: Throwable) : CertificateException(cause)` — a
required, non-nullable `cause`, not the optional param wallosmobile's swap added). The kit's
versions are plain commonMain `Exception`s, portable but no longer `CertificateException`
subtypes — so this was a real behavior change here, not a signature widening:

- **JSSE's handshake code only recognizes `CertificateException` thrown from
  `X509TrustManager.checkServerTrusted`.** Throwing the kit's exception directly would no longer
  be caught by the JDK's TLS stack the way the old `CertificateException`-subclassing version
  was. Fixed by wrapping at the throw site, the way wallosmobile's swap already does for
  `UntrustedCertificateException`: `throw CertificateException(UntrustedCertificateException(...))`
  — and this app additionally had to apply the same wrap to `CertificateHostnameMismatchException`
  (wallosmobile's `CompositeTrustManager` doesn't have that branch at all, so its swap never hit
  this case).
- **This pushes the kit exception one level deeper in the cause chain** —
  `SSLHandshakeException.cause` is now the wrapper `CertificateException`, not the kit exception
  itself. `PlatformNetworkErrorMapper` (this app's `expect`/`actual`, jvm+android) switched from a
  one-level `exception.cause is UntrustedCertificateException` check to the kit's own
  `findPendingCertTrust()`, which walks the whole cause chain by design — exactly the case it
  exists for. No kit-level helper covers the hostname-mismatch case (it's app-specific), so that
  branch uses a small local `generateSequence(this) { it.cause }.any { it is T }` walk instead.
- **Existing tests that asserted `assertFailsWith<UntrustedCertificateException>` /
  `assertFailsWith<CertificateHostnameMismatchException>` directly on `checkServerTrusted`'s throw
  became compile errors** (`Check for instance is always 'false'` — the kit type is no longer
  assignable to what the JSSE-facing method actually throws), not just semantically wrong. Fixed
  by asserting `CertificateException` and inspecting `.cause`/`findPendingCertTrust()` instead.
  Worth checking for on any future swap into a module whose types are caught by static type in a
  test: a removed supertype can turn a silently-wrong test into a build failure, which is easier
  to catch but easy to mis-diagnose as unrelated ktlint/compiler noise if you're not expecting it.
- **Added a real end-to-end proof, not just a mapping-logic test.** `RealTlsHandshakeJvmTest`
  (`core/api/src/jvmTest/`) spins up an actual `com.sun.net.httpserver.HttpsServer` with a
  `keytool`-generated self-signed cert, runs this app's real `createPlatformHttpClientEngine` +
  `CompositeTrustManager` wiring against it over a genuine JDK TLS handshake, asserts the failure
  maps to `UntrustedCertificateNetworkException` with the correct host/fingerprint, pins it, and
  asserts a retry succeeds with a real HTTP 200 — the one thing the existing fake-driven
  `CompositeTrustManagerTest`/`NetworkErrorMapperJvmTest` cannot prove, since they hand-construct
  the exception chain rather than letting the real JSSE stack build it. No Docker/device needed;
  runs in CI. Worth reaching for this pattern on a future swap that changes what a JSSE-facing
  throw site actually throws — a hand-built exception chain in a unit test proves the mapping
  logic, not that the JDK's TLS internals still accept the throw.
- **`core:domain` was kept, narrowed, not deleted** — same shape as wallosmobile's swap.
  `TaskIdentifier`/`CommonTaskType`/`NetworkException`/`PlatformIOException`/`PlatformNetworkError`/
  `UntrustedCertificateNetworkException` stay local (app-specific, no kit equivalent).
  `PendingCertTrust` leaks through `core:domain`'s own public types (`PlatformNetworkError`,
  `UntrustedCertificateNetworkException`) to roughly 15 further consumer modules
  (`core:storage`, `core:api`, several `feature/*/ui`), so the dependency needed `api(...)`, not
  `implementation(...)` — every one of those already reached `PendingCertTrust`/`resultOf` through
  an existing `implementation(projects.core.domain)` edge, so `api` on the kit dependency alone
  kept them compiling through the same edge with just an import-line rename. ~53 files imported
  `resultOf` alone (the module's single most-used symbol here, same as wallosmobile found).
- **Verified**: full `./gradlew jvmTest` (all modules) green, `ktlintCheck` green (three
  import-ordering/line-length fixups from `ktlintFormat`, all mechanical),
  `koverXmlReport`/`:koverVerify` (floor holds), `:androidApp:assembleFdroidDebug` and
  `:composeApp:compileKotlinIosSimulatorArm64 --rerun-tasks` both green. **Device-verified**:
  `:composeApp:run` (desktop) booted through the full Koin graph to `LoginNavDestination` with no
  DI crash (confirmed via the app's own file log, not just process-alive). The cert-trust path
  itself is verified by `RealTlsHandshakeJvmTest` above rather than a manual TLS-front-proxy
  click-through (see this app's own `docs/features/private-cert-trust/server-setup.md` for that
  heavier manual-QA path, not run this session) — the automated real-handshake test exercises the
  exact mechanism the swap changed (what `checkServerTrusted` throws and how the mapper reads it
  back), which a manual dialog click-through would not add further confidence on top of.

## storage (`grappim-kit-storage`)

Swapped onto by wallosmobile 2026-09-11 — first consumer, alongside `trustmanager` below
(one PR, not two — see that section for why). Diffed the published `0.1.4`
`grappim-kit-storage`/`grappim-kit-storage-android` sources jars (downloaded from Maven
Central) against the local `grappim-kit` checkout's HEAD first, per the standing rule:
`NetworkMonitor.kt`, `NetworkMonitorImpl.kt` (androidMain), `SecretCipher.kt`,
`KeystoreSecretCipher.kt` (androidMain) and `cert/TrustedCertStorage.kt` all
byte-identical — no drift between the checkout and what's actually published. Then
diffed those against wallosmobile's own pre-swap `core/storage` on `dev` HEAD.

- **`NetworkMonitor`/`NetworkMonitorImpl` byte-identical apart from package and KDoc
  wording** (the kit's drops a wallosmobile-specific "is the Wallos instance reachable"
  phrasing for a generic "a specific server" one) — a mechanical swap for this type.
- **`TrustedCertStorage` interface byte-identical.** `TrustedCertStorageImpl` is not:
  the kit takes `Json` as a constructor param defaulted to
  `Json { ignoreUnknownKeys = true }` (`class TrustedCertStorageImpl(dataStore, json =
  ...)`, public, no DI annotation) where wallosmobile's original held it as a
  `private val json` inside a `private companion object` on an `internal
  @Single(binds = [TrustedCertStorage::class]) class`. Functionally identical encode/
  decode logic either way — purely a constructor-shape and visibility change, not a
  behavior one.
- **Neither `SecretCipher` implementation nor `TrustedCertStorage`'s carries a Koin
  annotation** (same convention as `crash`/`appinfo`'s "consuming app binds its own
  instance" — noted in the `appupdate` section above, now confirmed for `storage` too):
  a consuming app that had these `@Single`-annotated directly on the impl class (as
  wallosmobile's own `KeystoreSecretCipher`/`TrustedCertStorageImpl` were) needs to grow
  explicit `@Single fun provideX(...): X = XImpl(...)` provider functions in its own DI
  module instead — `@ComponentScan` no longer picks these up once they're not in the
  consuming app's own package tree.
- **`KeystoreSecretCipher` gained two real deltas, not just a signature widening:**
  - **`keyAlias: String` is now a required constructor param**, not a hardcoded
    `private const val KEY_ALIAS` in a companion object — two consuming apps sharing a
    device must not collide on the same Keystore entry. A consumer's own provider
    function supplies its own alias string explicitly (wallosmobile kept its existing
    `"wallos_api_key"` literal, just moved from the deleted class into the provider).
  - **Stored format gains a `"v1:"` ciphertext prefix** (`CIPHERTEXT_PREFIX`), and
    `decrypt()` now checks for it: a value with no prefix is treated as legacy plaintext
    written before any cipher existed and passed through **unchanged** rather than
    attempted-and-failed-to-decrypt. wallosmobile's original had neither the prefix nor
    this passthrough branch — its stored form was bare `base64(iv || ciphertext)`. This
    is a **real, observed behavior change** for any consumer upgrading over an existing
    install with a previously-encrypted value already on disk: the pre-swap ciphertext
    (no `"v1:"` prefix) now reads as "already plaintext" and is returned as-is — the raw
    ciphertext bytes, not the real decrypted secret — rather than failing to decrypt
    (which would have at least surfaced as "no key stored"). Confirmed on-device:
    upgrading wallosmobile's installed APK in place over a previously-logged-in session
    left the dashboard rendering cached data but the budget cards reading "The instance
    didn't accept this API key" — the corrupted pass-through value sent to the server as
    the API key. Harmless for an app with no live installs yet (wallosmobile's own
    pre-v1 rule: stored state is expected to be discarded on a change like this, and a
    fresh login round-trips correctly under the new format — verified separately, see
    wallosmobile's own commit). **A consumer with real installs already using
    `SecretCipher` needs to treat this as a breaking storage-format change**, not assume
    the kit's passthrough branch makes it backward compatible — it only prevents a
    *crash*, not silent corruption of a pre-existing encrypted value that happens to have
    no `"v1:"` prefix.
- **`NoopSecretCipher` (in `SecretCipher.kt`) is new — no wallosmobile equivalent, and
  not consumed by this swap.** Android is wallosmobile's only target, so there is no
  second platform needing a passthrough double. Noted for whichever future consumer
  targets a platform with no Keystore-equivalent.
- **`core:storage`'s own dependency on `core:domain` became fully dead, not just
  redundant, once `TrustedCertStorage.kt` (its only user of `PendingCertTrust`) moved
  out** — removed `implementation(projects.core.domain)` outright rather than leaving an
  unused edge. Replaced with `api(libs.grappim.kit.storage)` (this module's own
  `api(project(":domain"))` already re-exports `grappim-kit-domain`, so `PendingCertTrust`
  keeps reaching every consumer that reached it through `core:storage` before). Worth
  checking on any future swap into a module that had its own `core:domain` edge purely to
  support the type being swapped out — it may now be dead too, not just superseded.
- **`api`, not `implementation`, is what makes this swap a pure import rename for every
  downstream consumer.** wallosmobile's `core:storage` had five direct consumers
  (`core:api`, `composeApp`, `feature:settings:ui`, `feature:setup:data`, `:testing`),
  each reaching `NetworkMonitor`/`SecretCipher`/`TrustedCertStorage` through a plain
  `implementation(projects.core.storage)` edge — same shape the `domain`/`coroutines`
  swaps already established needs `api` on the swapped-in kit dependency to keep flowing
  through.
- **Verified**: see `trustmanager` section below (one combined swap, one verification
  pass).

## trustmanager (`grappim-kit-trustmanager`)

Swapped onto by wallosmobile 2026-09-11 — first consumer, alongside `storage` above, in
one PR: `CompositeTrustManager`'s constructor takes the kit's own
`com.grappim.kit.storage.cert.TrustedCertStorage`, not a generic interface, so swapping
`trustmanager` alone while keeping a local `TrustedCertStorage` doesn't type-check.
Diffed the published `0.1.4` `grappim-kit-trustmanager-android` sources jar against the
local checkout's HEAD first (byte-identical), then against wallosmobile's own
pre-swap `core/api/.../CompositeTrustManager.kt` on `dev` HEAD.

- **Real behavior change, not purely mechanical: the hostname-mismatch branch is now
  wired to `CertificateHostnameMismatchException`.** wallosmobile's original
  `checkServerTrusted` lumped "no host" and "host doesn't match the certificate" into one
  `if (host == null || !hostMatchesCertificate(host, leaf)) throw e` (rethrows the bare
  platform exception unchanged for both). The kit's version keeps the `host == null`
  case as a bare rethrow but wraps the hostname-mismatch case in
  `CertificateHostnameMismatchException` the same way the untrusted-cert branch already
  wrapped in `UntrustedCertificateException` — the domain swap's own note above ("left
  unwired... a future trustmanager swap is where it either gets wired into that branch
  or stays unused") is resolved by this swap: it's now wired. **Not user-visible for
  wallosmobile**: `findPendingCertTrust()` only matches `UntrustedCertificateException`,
  so a hostname mismatch still falls through to the same generic
  `error_unreachable`-shaped message either way — this only enriches the cause chain
  reaching Crashlytics/logcat for anyone who logs the full exception. Worth checking
  whether a consuming app's own error-mapping code matches this new type explicitly
  before assuming the swap is silent for it too — a consumer that special-cased "host
  doesn't match" differently from "no host" pre-swap would see cause-chain shape change
  at that branch.
- **The `checkServerTrusted(chain, authType, host: String?)` `internal` overload
  (the one that makes host-parametrized testing possible without a real TLS handshake)
  is invisible across the module boundary, unlike when the class was local.** Kotlin
  `internal` is compilation-unit-scoped: this module's own `jvmTest` can call it (it's a
  friend compilation of the same module), but a consuming app's test source set —
  compiled as part of a completely separate Gradle module — cannot, even though the
  modifier reads identically to how it did when the class lived in the app's own source
  tree. **Any consuming app with its own test suite driving this overload directly (as
  wallosmobile's did) needs to rework those tests to go through one of the three public
  overloads instead** (`checkServerTrusted(chain, authType)`,
  `checkServerTrusted(chain, authType, socket: Socket)`,
  `checkServerTrusted(chain, authType, engine: SSLEngine)`). wallosmobile added a small
  `sslEngine(host: String): SSLEngine` test helper —
  `SSLContext.getInstance("TLS").apply { init(null, null, null) }.createSSLEngine(host,
  443)` — that gets a real, concrete, JDK-provided engine whose constructor-supplied
  `peerHost` needs no faking, rather than hand-implementing the platform's own large
  abstract `SSLEngine`/`SSLSocket` classes just to carry one string through the `Socket`
  overload instead. All of wallosmobile's existing `CompositeTrustManagerTest` cases
  ported cleanly onto this helper with no coverage lost, plus two assertions strengthened
  to check `failure.cause is CertificateHostnameMismatchException` for the newly-wired
  branch above.
- **`sha256Fingerprint` and `CompositeTrustManager` itself are both public** (no
  `internal` at the class/top-level-function level) where wallosmobile's originals were
  `internal`/package-private-by-convention — expected, since they need to be constructible
  and callable from outside the module now. Only the one test-only overload noted above
  stayed `internal` and is the one that actually breaks a cross-module consumer.
- **`commonMain.dependencies { implementation(project(":storage")) }` in this module's own
  `build.gradle.kts`, not `api`** — a consuming app that needs to construct
  `CompositeTrustManager` itself (as opposed to only depending on `trustmanager` for
  something else) needs its own direct dependency on `grappim-kit-storage` too, for
  `TrustedCertStorage` to be nameable at the construction call site.
  `implementation(libs.grappim.kit.trustmanager)` alone is not enough. wallosmobile
  already had this transitively via `core:storage`'s own `api(libs.grappim.kit.storage)`
  (see `storage` section above), so this didn't need a new direct edge there — but a
  consumer without that existing chain would.
- **Android + JVM only, deliberately no iOS target** (`X509ExtendedTrustManager`/
  `javax.net.ssl` don't exist there) — wallosmobile is Android-only anyway, so this
  wasn't a constraint here, but worth flagging for the next consumer.
- **Verified** (both `storage` and `trustmanager` together, one swap): `compileGplayDebugKotlin
  --rerun-tasks` (forces the Koin compiler plugin to re-scan after the new provider
  functions) green, `assembleFdroidDebug`/`assembleGplayDebug -PgplayBuild`, `allTests`
  (including the reworked `CompositeTrustManagerTest`), `detekt ktlintCheck`,
  `lintFdroidDebug`/`lintGplayDebug -PgplayBuild`, `check-guardrails.sh` all green.
  **Device-verified**: cold start on `Medium_Phone_API_36.1` (gplay debug,
  `-PgplayBuild`) over the *pre-swap* install resolved the full Koin graph with no crash
  (confirming the three new `@Single` providers and `CompositeTrustManager`'s
  construction site both still resolve at runtime) but surfaced the `KeystoreSecretCipher`
  format-change consequence described above on the stale stored key. Clearing app data
  and logging in fresh via the web-login bridge (`login.php` → `profile.php`, itself
  routed through the same `CompositeTrustManager`-wrapped engine as every other request)
  confirmed the new cipher's encrypt/decrypt round-trips correctly end to end and the
  dashboard rendered real data with no FATAL/Koin-resolution logcat lines throughout.

## testing

No consumer-facing gotchas found yet — nothing has swapped onto this from an app. The
`storage`/`trustmanager` swap above deliberately left `:testing` untouched (its own
`FakeTrustedCertStorage`/`FakeNetworkMonitor` are out of scope per the crash swap's own
precedent for excluding `testing`) — wallosmobile kept its local, hand-written fakes,
updating only their imports to the new kit types. Add a section here the first time
`grappim-kit-testing` itself gets swapped onto.

## build-logic (`grappim-kit/build-logic`, consumed via `includeBuild`, not Maven)

**Different consumption mechanism from every module above.** This isn't a Maven artifact —
it's a Gradle convention-plugin composite build, consumed the same way each app already
consumes its own local `build-logic/`: `pluginManagement { includeBuild("path/to/build-logic") }`
in the consuming app's root `settings.gradle.kts`. Extracted 2026-09-09, not yet adopted by
any app — building/committing this is unblocked, but swapping an app onto it (deleting that
app's own local `build-logic/`) needs the same per-app "ask before switchover" gate as every
other module (see `grappim-watcher/CLAUDE.md`'s `grappim-kit-build-only-until-told` note).

**Scope: library-module conventions only, not the application module.** A fresh diff
(2026-09-09) across wallosmobile/wayprint/TaigaMobileNova found the plan's original
"7 files, byte-identical apart from package" verdict was too generous — only
`KmpDiConventionPlugin`, `KmpSerializationConventionPlugin`, and `ProjectExtensions.kt` (the
`Project.libs` accessor) are actually byte-identical across all three. Everything else that
touches the *application* module (`AndroidApplicationConventionPlugin`, `AppBuildTypes`,
`AppFlavors`, and `KotlinConfiguration.kt`'s `configureKotlinAndroid`/`configureKotlinJvm`,
which only that plugin calls) is genuinely per-app — signing certs, flavor names, package
suffixes — and was **not extracted**; each app keeps that part of its own `build-logic`.
Only the *library*-module conventions (`KmpLibraryConventionPlugin`,
`KmpLibraryComposeConventionPlugin`, `KmpLibraryStabilityConventionPlugin`,
`KmpNetworkConventionPlugin`, plus their shared helpers) moved here.

**The real finding: these convention plugins reach into each consuming app's own module
tree by hardcoded path, which no runtime-code module extracted so far has done.**
`KmpConfiguration.configureKmp()` hardcoded `implementation(project(":core:logger"))` in
both source apps; `Quality.configureTests()` hardcoded `implementation(project(":testing"))`;
`Quality.configureLinting()` hardcoded `"detektPlugins"(project(":detekt-rules"))`
(wallosmobile-only — wayprint/TaigaMobileNova have no such module). A shared build-logic
can't assume any of these paths exist — `:core:logger`/`:testing` are exactly the local
modules this whole project is trying to retire in favor of `grappim-kit-logger`/
`grappim-kit-testing`, and `:detekt-rules` never existed in two of the three apps. **Fixed
by dropping every implicit injection**: the shared plugins no longer add any of these
automatically. A module that needs `:core:logger` (or `grappim-kit-logger`, once swapped)
declares it itself, like any other dependency — one extra explicit line per module, in
exchange for not baking in an assumption that breaks the moment an app finishes migrating
off its own local module.

**Second real finding: `libs.findLibrary(...)` can't reach a consuming app's version
catalog across an `includeBuild` from a separate repo.** Each app's own
`build-logic/settings.gradle.kts` currently does
`versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }` — a
relative path that only works because `build-logic/` sits one level under that app's own
root. Moved to a sibling repo, that path resolves to the wrong catalog entirely. Fixed the
same way the already-published runtime modules already work: this `build-logic`'s own
`settings.gradle.kts` points at **`grappim-kit`'s own** `gradle/libs.versions.toml`
(`../gradle/libs.versions.toml`, one level up from `build-logic/`, same relative depth as
each app), not any consuming app's. `grappim-kit`'s catalog gained the entries these
plugins need (ktor, turbine, ktlint/detekt/kover gradle-plugin coordinates, compose-rules,
the individual `jetbrains-compose-*` UI libraries, koin) as part of this extraction —
versions copied from wallosmobile's/TaigaMobileNova's own catalogs (they already agreed on
Kotlin 2.4.10 / AGP 9.4.0, matching what `grappim-kit` already used). A consuming app's
*own* catalog is untouched by any of this — it keeps whatever versions it wants for its own
application code; only this `build-logic`'s internal dependency versions come from
`grappim-kit`'s catalog.

**Every remaining per-app difference is a Gradle property, not a hardcoded value or a
Kotlin constructor parameter** (the plugin classes are applied purely by id — 
`plugins { id("com.grappim.kit.kmp.library") }` — with no call site to pass typed arguments
to). Set once in the consuming app's own root `gradle.properties`, or per-module in that
module's own `gradle.properties` where a module needs to differ from its app's default
(Gradle resolves project properties per-project, closest one wins):

| Property | Read by | Default | Notes |
|---|---|---|---|
| `grappimKitNamespacePrefix` | `kmp.library` | none (required) | e.g. `com.grappim.wallosmobile` |
| `grappimKitAdditionalTargets` | `kmp.library` | none (Android-only) | comma-separated `jvm`,`ios` |
| `grappimKitKoverExcludeAndroidUnitTests` | `kmp.library` | `false` | Taiga's shape once it adopts |
| `grappimKitEnableAndroidHostTest` | `kmp.library` | `true` | set `false` once a module also has `jvm()` — Taiga's shape, avoids double-running the same tests |
| `grappimKitExcludeFromLinting` | `kmp.library` | `false` | set per-module, e.g. on `:testing` |
| `grappimKitExtraDetektRuleModule` | `kmp.library` | none | wallosmobile's `:detekt-rules`, e.g. `:detekt-rules` |
| `grappimKitComposeStabilityConfigEnabled` | `kmp.library.compose`, `kmp.library.stability` | `false` | needs a `config/compose/stability_config.conf` in the consuming app — wallosmobile/wayprint have one, Taiga doesn't yet |

`KmpNetworkConventionPlugin`/`ComposeStabilityMarker` don't take a targets property directly
— they wire `androidMain`/`jvmMain`/`iosMain` dependencies via
`sourceSets.matching {}.configureEach {}` instead of the typesafe `.jvmMain`/`.iosMain`
accessors, since those throw if the module hasn't declared that target and this plugin has
no way to know what `kmp.library`'s `grappimKitAdditionalTargets` was for this module.

**Verified so far: compiles, plugin metadata validates (`./gradlew -p build-logic build`),
and a throwaway smoke-test module applying all six plugins together built clean end-to-end
in both shapes** (Android-only with `grappimKitEnableAndroidHostTest=true`, matching
wallosmobile/wayprint's current build-logic; and Android+jvm+ios with
`grappimKitEnableAndroidHostTest=false`, matching TaigaMobileNova's) — full
`build`/`check`/`detekt`/`ktlint`/`koverVerify` pipeline green in both. **Not yet verified
against a real app** — that needs an actual swap (deleting an app's own `build-logic/`,
adding the properties above, adding the couple of explicit dependency lines the dropped
implicit injections require), which is the next actionable, gated step whenever the user
asks for it, same as every other module.
