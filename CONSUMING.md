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

- **Known, not yet fixed (found 2026-09-09, TaigaMobileNova): `resetTo()`/`goToTopLevel()`
  never dispose a top-level screen's `ViewModelStore` when that screen's `NavKey` is a
  payload-less singleton (`data object`) — which is the overwhelmingly common shape for a
  top-level/tab-root key.** Both functions "reset" a section by writing the target key back
  into a `NavBackStack` slot (`stack[0] = key`, or `topLevelStack[idx] = key`) rather than
  removing the old entry and pushing a new one. Real Nav3's `ViewModelStoreNavEntryDecorator`
  only disposes a `ViewModelStore` when its `NavEntryDecorator.onPop` fires, which only
  happens when the entry's `contentKey` structurally *disappears* from the tracked backstack
  list (`equals()`-based diffing, traced in `navigation3-runtime`/`lifecycle-viewmodel-navigation3`
  sources — see `agentic-grappim`'s `mobile-patterns` skill, Navigation section, for the full
  mechanism). Writing the same singleton object back is a no-op from that diff's perspective,
  so the disposal never fires — any `@KoinViewModel`/`viewModel()` resolved at that top-level
  entry survives `resetTo()`/`goToTopLevel()` indefinitely, including across an app's own
  "logout" reset. Confirmed via TaigaMobileNova's own bug: `DashboardViewModel` kept showing
  a previous account's data after logout→login (inside one continuous process — a fresh
  process hides this, since it never had the stale instance to begin with) until a manual
  pull-to-refresh re-ran its fetch on the same surviving instance.

  **Not fixed in this library yet.** TaigaMobileNova worked around it at the app level (wrap
  the nav host's composition root in `key(sessionGeneration)`, bumped on logout, to force a
  full Compose-level teardown instead of relying on this library's reset primitives to also
  reset ViewModel state — see its `composeApp/.../main/MainScreen.kt`). Any other consumer
  (wallosmobile, wayprint, HateItOrRateIt) that reaches a top-level screen via `resetTo()`/
  `goToTopLevel()` and expects that screen's ViewModel to come back fresh after a session
  reset has this same latent bug — check before assuming a "logout" flow actually clears
  per-screen state. A real fix here (e.g. having `resetTo`/`goToTopLevel` force genuine
  pop-then-push semantics, or exposing an explicit "dispose this section" primitive) needs
  its own investigation into whether any consumer's current tests/behavior implicitly depend
  on today's persistence — raise with gregory before touching it, same as the back-stack-growing
  fix above.

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

## logger, coroutines, domain, crash, appinfo, storage, trustmanager, testing

No consumer-facing gotchas found yet — nothing has swapped onto these from an app.
Add a section here the first time one does, same shape as `navigation`/`uikit` above.
