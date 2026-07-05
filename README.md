# Sliide Users — KMP "UX Innovator" Challenge

A cross-platform user directory over the [GoRest public API](https://gorest.co.in), built with
Kotlin Multiplatform and a **100% shared Compose Multiplatform UI** for Android and iOS.

## Feature checklist (vs. the brief)

| Requirement | Status | Notes |
|---|---|---|
| Feed from **last page** of `/users` | ✅ | Page count read from `x-pagination-pages`, then that page is fetched |
| Name, email, **relative timestamp** in shared logic | ✅ | `RelativeTime` is a pure Kotlin function in `:domain`; see *API limitations* below |
| **Shimmer** loading + graceful errors | ✅ | Pure-Compose shimmer; full-screen error only when no cache exists, otherwise cached content + offline banner |
| FAB → polished **add-user form** | ✅ | Modal sheet, real-time validation, segmented gender/status, inline server errors |
| **201 → appears at top immediately** | ✅ | Created rows are stamped newest-first in the local cache, which the UI observes |
| Long-press delete + confirm + animation + **Undo snackbar** | ✅ | *Deferred delete*: the DELETE fires only after the undo window closes (see below) |
| **Adaptive layout** | ✅ | <700 dp list + detail bottom sheet on tap; ≥700 dp master–detail side pane; selection self-clears on delete |
| Compose Multiplatform shared UI | ✅ | Swift layer is ~30 lines hosting the shared `UIViewController` |
| Clean Architecture (MVI), Ktor, Room KMP, Koin | ✅ | `:domain` / `:data` / `:composeApp` Gradle modules |
| Offline support | ✅ | Room is the single source of truth; the app renders fully from cache |
| Unit tests for shared ViewModels/logic | ✅ | 49 tests: domain, data (MockEngine), ViewModel (virtual-time coroutines) |
| Dark mode, Material 3 | ✅ | Plus dynamic color (Material You) on Android 12+, brand palette elsewhere |

## Quick start

**Android**
1. Copy `local.properties.sample` → `local.properties` and set `gorest.token`
   (free token: [gorest.co.in](https://gorest.co.in) → login → API Tokens).
   The read-only feed works without it; create/delete need it.
2. Open in Android Studio, run the `composeApp` configuration.

**iOS** (requires a Mac with Xcode)
1. Same `local.properties` step.
2. Open `iosApp/iosApp.xcodeproj`, run. The build phase compiles the Kotlin
   framework via `embedAndSignAppleFrameworkForXcode`.

**Tests**
```bash
./gradlew :domain:testDebugUnitTest :data:testDebugUnitTest :composeApp:testDebugUnitTest
```

The token is injected at build time from `local.properties` (gitignored) into a
generated `ApiConfig.kt` — it never enters version control.

## Architecture

```
┌────────────────────── :composeApp ──────────────────────┐
│  ui/            100% shared Compose Multiplatform        │
│  presentation/  MVI ViewModels (state ⇦ intents ⇨ effects)│
└──────────────┬───────────────────────────────────────────┘
               │ observes Flow / calls use cases
┌───────────── ▼ :domain ───────────────────────────────────┐
│  models · UserRepository contract · use cases ·           │
│  validators · RelativeTime — pure Kotlin, zero frameworks │
└──────────────▲───────────────────────────────────────────┘
               │ implements
┌───────────── ┴ :data ─────────────────────────────────────┐
│  Ktor (GoRest client) · Room KMP (cache, source of truth) │
│  offline-first repository · Koin modules                  │
└───────────────────────────────────────────────────────────┘
```

- **MVI**: each screen is one immutable `State`, a sealed set of `Intent`s and
  one-shot `Effect`s (Channel-backed, so snackbars never replay on rotation).
  The base class is ~40 lines — no MVI framework dependency to curate around.
- **Offline-first**: the UI renders *only* what Room emits. Network calls
  mutate the cache; the cache notifies the UI. This gives offline mode,
  instant add-at-top, and animated deletes for free.
- **expect/actual** used where platforms genuinely diverge, and nowhere else:
  HTTP engine (OkHttp/Darwin), network-exception classification (JVM DNS
  failures aren't `IOException`s; Darwin has its own), Room database builder,
  wall clock, and dynamic color capability.

### Deliberate decisions

**The API has no timestamps.** GoRest v2 returns no `created_at`, but the brief
requires "5 minutes ago". Options considered: synthesize fake times (looks
right, is a lie), or anchor to **local first-sight** — when a user first
entered this device's cache (creation time for users you create). I chose
honesty. Timestamps are preserved across refreshes so labels stay stable, and
a minute-ticker in the ViewModel keeps them current while the app is open.

**Undo means the server was never asked.** The brief says a 204 lands, *then*
undo "restores local state before the action is finalized" — contradictory,
since 204 means the row is already gone server-side. I implemented the
production-grade reading: confirm hides the row instantly and starts a 4s
countdown (matched to `SnackbarDuration.Short`); the DELETE fires only when
the window closes; Undo cancels the network call entirely. No delete-then-
recreate identity games. A failed delete restores the row and says why.

**Last page is a moving window.** New GoRest users shift the last page, so
naive refresh would evict users you just created. Reconciliation keeps
locally-created rows, preserves known first-seen times, and evicts only
remote rows that left the window.

**Version pins are choices, not defaults.** Kotlin 2.3.20 + KSP 2.3.9 instead
of Kotlin 2.4.0 (open KSP codegen issue would put Room's compiler at risk);
Room 2.8.4 instead of the four-day-old Room 3.0.0. Rationale is commented in
`gradle/libs.versions.toml`.

**Small things done on purpose**: minSdk 26 so the launcher icon is a single
adaptive vector (no binary PNGs in the repo); `BoxWithConstraints` breakpoint
instead of the window-size-class artifact (one less dependency for one
comparison); avatars are per-name gradient initials, so the feed has stable
color identity without image loading.

## Testing

- `:domain` — relative-time bucket boundaries (incl. future-clock skew),
  Unicode name validation, email edge cases, use-case behavior.
- `:data` — GoRestApi over Ktor `MockEngine`: asserts the last-page request
  literally carries `page=42`, plus every status mapping (201/204/404/422/401)
  and transport-failure classification. Repository reconciliation over an
  in-memory DAO.
- `:composeApp` — 12 ViewModel tests on virtual time: shimmer→content,
  error-with-cache vs error-without-cache, retry recovery, live validation,
  server 422 landing on the right field, and the undo window tested at
  4 999 ms vs 5 001 ms.

## How I used AI

I used Claude as a **directed pair**: I made the calls, it produced the code,
I reviewed every stage before it was committed (the git history is the audit
trail — each commit is one reviewed architectural stage).

Decisions I made and had the AI execute: MVI over MVVM; Room over SQLDelight;
deferred-delete undo semantics; the first-seen timestamp answer to the missing
`created_at`; the moving-window reconciliation rules; version pins (including
rejecting Kotlin 2.4.0 after checking open KSP issues); module boundaries; and
what *not* to add (no MVI framework, no window-size-class dependency).

Where AI earned its keep: exhaustive edge-case test matrices (snackbar-timing
boundary tests, email validator abuse cases), the RFC-5322-subset regex with
documented deviations, Material 3 boilerplate, and the platform `actual`s.

Everything generated was reviewed with the same bar I'd apply to a human PR:
a few things were rejected and redone (e.g. an early draft finalized deletes
from the UI layer's snackbar callback — moved into the ViewModel so process
lifecycle can't leak a phantom row).

## Known limitations

- Relative times are English-only (`RelativeTime` is deliberately locale-free
  shared logic; production would move copy behind localization).
- No paging beyond the last page — that's the brief's scope.
- iOS has no dynamic color; the brand palette is the design there.
- Screenshot/UI tests and CI are out of the 1-day scope; the architecture
  (single-`State` screens) is built to make both trivial to add.
