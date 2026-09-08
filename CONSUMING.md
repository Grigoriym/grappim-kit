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

## logger, coroutines, domain, crash, appinfo, storage, trustmanager, testing

No consumer-facing gotchas found yet — nothing has swapped onto these from an app.
Add a section here the first time one does, same shape as `navigation` above.
