# Debug log export — plan for a shared `logger` capability

**Origin:** feature request from gregory, TaigaMobileNova `docs/revisit.md` entry 52
(2026-09-07, decisions finalized 2026-09-16). Symfonium-inspired: a user hits a bug, turns
on a "debug mode" toggle, reproduces it, then shares the resulting log file with the
developer. Not implemented anywhere yet — this is new development in `grappim-kit`, not
an extraction of existing app code (unlike every other module in this repo so far).

**Two decisions already made by gregory — don't re-litigate:**
1. **Opt-in toggle**, not always-on. Nothing persists to a file until the user enables it.
2. **Lives in the kit** (`grappim-kit-logger`), reusable across TaigaMobileNova,
   wallosmobile, wayprint, HateItOrRateIt — not app-local.

Everything else below is investigation findings and open design questions for whoever
implements this, not settled decisions.

## Current state of `logger/` (this repo)

- `KitLogger` (`logger/src/commonMain/.../KitLogger.kt`) is a **single global sink** — one
  `@Volatile logger: KitLogger` field, `install()`/`uninstall()` swap it wholesale. Every
  `logcat()` call in a consuming app routes through whatever is currently installed.
- **JVM**: `FileLogger` (`logger/src/jvmMain/.../FileLogger.kt`) already does the whole
  thing for desktop — writes every `logcat()` call to a file, rotating to `<name>.old` past
  5 MB (`MAX_LOG_FILE_BYTES`). It's the sole installed `KitLogger` on desktop, always on,
  no toggle. This is the pattern to reuse/port, not redesign.
- **Android**: `TimberLogger` (`logger/src/androidMain/.../TimberLogger.kt`) is a thin
  `KitLogger` → `Timber.log()` adapter. The actual fan-out on Android already happens one
  layer down, via **Timber's own tree system** — consuming apps plant multiple
  `Timber.Tree`s independently of `KitLogger` (TaigaMobileNova plants a debug-only
  `DebugTree` and a Crashlytics-forwarding tree alongside `TimberLogger.install()`). A
  rotating-file sink for Android is naturally **a `Timber.Tree` subclass**, not a second
  `KitLogger` — that's the existing extension point apps already use, and consuming apps
  already depend on `timber.log.Timber` directly (it's not hidden behind this module's
  API).
- **iOS**: `NSLogLogger` (`logger/src/iosMain/.../NSLogLogger.kt`) writes to `NSLog` only,
  chunked at 3000 chars for NSLog's truncation limit. No persistence, no fan-out mechanism
  at all — it's the sole `KitLogger` a consuming app installs. There is nothing here to
  extend the way Android's Timber trees can be; this needs new plumbing.

## Design implications of the "single global `KitLogger`" constraint

Toggling a file sink on/off at runtime, without losing whatever else is already logging
(NSLog on iOS, Timber's DebugTree/CrashlyticsTree on Android), needs different mechanisms
per platform:

- **Android**: plant/unplant a new `Timber.Tree` when the toggle flips. No `KitLogger`
  changes needed at all — this can arguably ship as app-local code even though it's
  "in the kit" conceptually, *unless* the rotation/path logic itself is what's worth
  sharing (it is — see JVM's `FileLogger` for the logic to port).
- **iOS**: `KitLogger.install()` replaces the whole sink, so toggling on can't simply swap
  `NSLogLogger` for a file logger — NSLog output would be lost. Two options to weigh:
  - A small **fan-out combinator** (e.g. `CompositeKitLogger(vararg loggers)` implementing
    `KitLogger`, forwarding every call to each child) added to `commonMain` — install
    `CompositeKitLogger(NSLogLogger(), FileKitLogger(file))` when the toggle is on, plain
    `NSLogLogger()` when off. Reusable beyond this feature too.
  - Or fold file-writing directly into `NSLogLogger` itself, gated by a constructor flag /
    mutable property. Less composable, but no new public type.
- **JVM**: `FileLogger` already exists and was always-on. **Resolved 2026-09-16 — gregory: gate
  it behind the same toggle as Android/iOS, for consistency.** No new kit-side API was needed for
  this: `FileLogger.install(logFile)`'s existing guard already supports an off→on→off→on cycle
  correctly (`KitLogger.uninstall()` reverts to the no-op logger; a later `install()` reattaches).
  The change is call-site-only — a consuming app wires `install()`/`KitLogger.uninstall()` to the
  toggle instead of calling `install()` unconditionally at startup. See `CONSUMING.md`'s logger
  section for the existing desktop call site (TaigaMobileNova) that will need to move behind the
  toggle when this feature is consumed.

## What's explicitly out of scope for this module

- **Getting the file off the device.** `KitLogger`/this module has no concept of UI or
  sharing — it should only expose enough to let a consuming app locate the log file (e.g.
  return the `File`/path from an `install()`/factory call) so the app builds its own
  `Intent.ACTION_SEND` (Android) or `UIActivityViewController` (iOS) around it. Desktop
  just needs a "reveal in file manager" action. None of that belongs in `grappim-kit`.
- **Content sanitization.** Whatever an app already logs via `logcat()` calls is what ends
  up in the file — this module doesn't (and can't) know which call sites might leak a
  token or request body. Each consuming app is responsible for auditing its own
  `logcat()` call sites before shipping this feature (TaigaMobileNova already has
  precedent for this exact kind of sweep — see its `ExceptionSanitization.kt` and the
  MASVS-PRIVACY-3 fix in its `docs/security/masvs.md` — but that's app-side work, not
  something this module can enforce).
- **Privacy-policy text.** App-side, not kit-side.

## Suggested shape (starting point, not final)

- Port `FileLogger`'s rotation logic (`MAX_LOG_FILE_BYTES`, `.old` rotation, the line
  format) into a small shared `commonMain` piece both JVM and iOS reuse, since the logic
  itself has no platform dependency beyond `File` I/O — check whether `kotlinx-io` or
  similar is already a dependency here before reaching for a new one.
- Android: a `RotatingFileTree` (or similar) in `logger/src/androidMain/`, same rotation
  logic, planted as a `Timber.Tree`.
- iOS: `FileKitLogger` implementing `KitLogger`, writing to `NSDocumentDirectory`, same
  rotation logic, combined with `NSLogLogger` via whichever fan-out approach is chosen
  above.
- Each platform's `install()` should probably take an explicit `enabled: Boolean` (or a
  `Flow<Boolean>`-observing variant) rather than requiring the consuming app to hand-roll
  install/uninstall around its own settings toggle — worth deciding once you see how
  TaigaMobileNova's Settings screen wants to drive it.

## Verification expectations

Same bar as every other kit module — see this repo's own `CLAUDE.md` and `testing`
module conventions. Rotation logic (JVM already has none? confirm whether `FileLogger` has
a test today) and any new fan-out combinator need real tests, not just a manual check.
Once a consuming app wires this in, GUI/device verification of the actual toggle → log →
share flow happens in that app's repo, not here.
