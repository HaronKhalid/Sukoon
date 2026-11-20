# Repository audit (Milestone 0)

Audit date: 2026-11-12. Facts below were read from the checked-out sources, not from READMEs alone.

| | Fokus Launcher | Victoria Launcher |
|---|---|---|
| Remote | github.com/luantak/FokusLauncher | github.com/adelmonte/victoria-launcher |
| Branch / commit | master @ 019ba80 ("Prepare the 1.9.8 release") | main @ fbd27a4 (Release 0.94.4) |
| License file | GPL v3 text; README says "GPL v3.0" (AUTHORS: Paul Scheduikat) | GPL v3 text; README says **GPL-3.0-or-later** |
| Version | 1.9.8 (versionCode 54) | 0.94.4 |
| Application ID / namespace | io.github.luantak.fokuslauncher / com.lu4p.fokuslauncher | dev.victorialauncher |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | 26 / 35 / 35 |
| AGP / Kotlin / Compose BOM | 9.4.1 / 2.4.10 / 2026.06.01 | older (BOM 2024.09.02, versions inline in build file) |
| Dependency management | version catalog (`libs.versions.toml`) | versions inline |
| UI | Jetpack Compose + Material 3, Navigation Compose | Jetpack Compose + Material 3, Navigation Compose |
| DI | Hilt | none (manual) |
| Persistence | Room (app categories, hidden/renamed apps) + DataStore | DataStore only |
| Kotlin files (main) | ~200 (266 incl. tests) | much smaller |
| Tests | 64 unit/instrumented test files, Robolectric, MockK, Turbine | few |
| CI | GitHub Actions build/release | none seen |
| Translations | ~25 locales via Weblate | several |
| Docs | docs/configuration-backups.md | docs/ARCHITECTURE.md |

## Fokus: relevant existing capabilities
Clock/date home, battery row, weather (Open-Meteo), favourites, app drawer with search + categories + dot-search, hide/rename apps, gestures (swipe, two-finger), shortcut rail, Pomodoro focus timer with alarms, screen-time helper (`usage/`), custom font loading (`data/font`), icon packs (Arcticons), home notes (SAF Markdown folder), hosted widgets (`AppWidgetHost`), Private Space + work profile, configuration backup/restore, optional accessibility double-tap-lock, onboarding.

## Victoria: ideas worth selective porting (later)
Edit-layout controls on the home screen, clock widget with agenda/battery/weather. To be ported only as isolated pieces; no manifest/Gradle/package merging.

## Baseline build
See `docs/implementation-log.md` for the actual baseline build result (recorded when run).
