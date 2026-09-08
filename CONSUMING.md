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
  `NavigationIconConfig.Custom`) already worked this way — only Back/Menu were special-cased
  to reach into an app-bundled `stringResource(...)` before this port, which is exactly what
  would have forced `grappim-kit` to set up its own Compose Multiplatform string-resource
  generation. Making them caller-supplied too removes that requirement entirely and is more
  consistent with the rest of the module, not a workaround. Typically only one call site (the
  shell composable that renders `TopBar`) needs to supply these, not every screen.
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

## logger, coroutines, domain, crash, appinfo, storage, trustmanager, testing

No consumer-facing gotchas found yet — nothing has swapped onto these from an app.
Add a section here the first time one does, same shape as `navigation`/`uikit` above.
