# Implementation log

## 2026-11-12 — Milestone 0 + baseline (Sukoon fork of Fokus 1.9.8 @ 019ba80)
Environment: Windows 11, Android SDK (platforms 36, 37.0, build-tools 36.0.0) in `C:\Android\sdk`,
portable Temurin JDK 21.0.13 (system JDK 25 can't be used: Gradle toolchain needs 21). Gradle 9.7.0.

| Command | Result |
|---|---|
| `./gradlew assembleDebug` | **BUILD SUCCESSFUL** (20m58s cold; `app-debug.apk` 26.7 MB) |
| `./gradlew testDebugUnitTest` | **FAILED**: 516 tests run, 3 failures, 0 skipped |

Failing tests (all environmental, Robolectric native runtime does not load on this machine):
- `WidgetConfigurationLaunchTest` ×2 — "Unable to load Robolectric native runtime library"
- `AppRepositoryTest > deleteCategory records suppressed category definition` — `UnsatisfiedLinkError` in `SQLiteConnection.nativeOpen`

Not yet investigated whether these also fail upstream on this machine; treat as pre-existing/environmental until checked.
Lint, instrumentation tests and on-device (OnePlus 8) tests: **not run**.

Next: Milestone 2 (theme model, layout presets, Clock Studio).

## 2026-11-20 — Milestone 1 compliance track (branch feat/compliance-baseline, worktree C:\Android\wt\compl)
Added: Settings > Open-source acknowledgements (Fokus, Victoria as reference only, libraries/data credits, source link,
full GPL-3.0 viewer from `assets/licenses/GPL-3.0.txt`; strings in `values/strings_about.xml`); removed upstream
Weblate/Matrix/PayPal links and `.github/FUNDING.yml`; replaced "Fokus" app name with "Sukoon" in all locale strings,
fastlane text and issue templates; new `.github/workflows/build.yml` (unit tests + debug APK, no secrets) replacing the
upstream release-signing and `is-malicious` (needs OPENCODE_API_KEY) workflows; README, permissions and attribution docs updated.

JDK 21 (`JAVA_HOME=C:\Android\jdk21\jdk-21.0.13+9`), worktree path has no spaces:
| Command | Result |
|---|---|
| `./gradlew assembleDebug testDebugUnitTest --continue --console=plain` | assembleDebug OK (app-debug.apk 26.7 MB); testDebugUnitTest: 516 tests, 3 failed, BUILD FAILED in 22m22s |
| same 3 tests with `-I robo-init.gradle` (robolectric.offline + dependency dir without spaces), `--tests *WidgetConfigurationLaunchTest --tests *AppRepositoryTest` | BUILD SUCCESSFUL; WidgetConfigurationLaunchTest 8/8, AppRepositoryTest 67/67 |

Root cause of the 3 failures (environmental, not a Sukoon regression and not a path-with-spaces-in-project issue): the Windows
user profile is `C:\Users\Haroon Khalid`. Robolectric 4.16.1 resolves `android-all-instrumented-*.jar` from `~/.m2` and
percent-encodes the path (`Haroon%20Khalid`), giving `NoSuchFileException`; the native runtime then fails to load, so the two
widget tests error and the sqlite native `UnsatisfiedLinkError` hits the AppRepositoryTest. The jars exist at the real path.
Failures persist in the space-free worktree path, confirming the project path is not the cause. The pristine upstream copy
(C:\Android\tmp\fokus-baseline) was prepared but NOT built/tested: the cause is machine-level (user home), so upstream would
fail identically, but this is inference, not a measurement. The workaround is not applied by default; see
`scripts/robolectric-local-deps.init.gradle`. CI on Linux is expected not to hit this (not yet run; no push performed).
Not run: lint, instrumentation tests, on-device tests, the acknowledgements UI on a device.
## 2026-11-20 - Milestone 2: theme engine + Theme Studio (branch feat/theme-engine)
Added `com.lu4p.fokuslauncher.theme` (model, 8 built-ins, strict JSON codec with v0->v1 migration, DataStore `ThemeStore`), `ThemeStudioScreen`/`ThemeStudioViewModel` (Settings > Theme Studio: preview, apply, reset, edit colours, save/rename/delete custom themes, copy/paste JSON export/import). `FokusLauncherTheme` takes an optional `appTheme`; settings pages use `themedSettingsScrim` instead of black. Strings are in `strings_theme.xml`.

| Command (JAVA_HOME=C:\Android\jdk21\jdk-21.0.13+9) | Result |
|---|---|
| `./gradlew testDebugUnitTest --tests "com.lu4p.fokuslauncher.theme.*"` | **BUILD SUCCESSFUL**: ThemeCodecTest 13, ThemeModelTest 8, ThemeStoreTest 14 = 35 tests, 0 failures |
| `./gradlew assembleDebug` | **BUILD SUCCESSFUL** (6m40s warm) |

Notes / known issues:
- First ThemeStoreTest run with a real file-backed DataStore failed on this Windows JVM (`Unable to rename ...tmp`, DataStore's rename over an existing file); tests now use an in-memory `DataStore<Preferences>`. File-backed persistence on a device was not run.
- Full `testDebugUnitTest` suite was not re-run for this track (only the theme tests); the 3 known Robolectric/SQLite failures are untouched.
- Not run: lint, instrumentation/UI tests, on-device or visual check of Theme Studio and of light themes across every screen. Clock style, app-list style, spacing and wallpaper reference from the brief are not yet in the model; typography fields are stored as references but not yet applied (font registry belongs to the typography track). Motion settings are stored but not consumed yet.
- Applied theme is ignored over photo wallpapers (classic look kept for legibility). Status/navigation bar icon tint does not yet follow `appearance`.
## Milestone 3: typography and notes (branch feat/typography-notes)
Built with JDK 21 (Temurin 21.0.13). Commands run together: `./gradlew assembleDebug testDebugUnitTest --tests "com.lu4p.fokuslauncher.notes.*" --tests "com.lu4p.fokuslauncher.typography.*" --continue`.

| Check | Result |
|---|---|
| `assembleDebug` | **BUILD SUCCESSFUL** (23m14s, concurrent with other builds). `app-debug.apk` 27,857,318 bytes vs 26,690,110 baseline: **+1,167,208 bytes (~1.11 MiB)**; fonts inside the APK are 477,930 + 166,653 + 378,561 bytes deflated (about 1.02 MB) plus licence texts and code |
| New unit tests (28): `FontRegistryTest` 7, `BidiTextTest` 7, `NoteAppearanceTest` 4, `NoteRepositoryTest` 10 | **all passed, 0 failures** (pure JVM, fake in-memory DAO; no SQLite native library) |
| Full `testDebugUnitTest` | **not re-run** in this track (only the targeted new tests); the 3 known Robolectric/SQLite failures were not touched |
| Room DAO/migration instrumentation test for 7->8 | **not written / not run** (existing migration tests are `androidTest`, `exportSchema = false`, need a device) |
| Lint, on-device, RTL/Nastaliq visual check | **not run** |

Delivered: `typography` package (registry, cache, per-element DataStore, settings screen, `withElementFont` applied to clock, date, home favourite labels, drawer labels); `notes` package (Room entity/DAO, DB 7->8 migration, repository, autosaving ViewModel with SavedStateHandle draft, list+editor screen with appearance controls, pinned note on home via `HomePinnedNoteOr`). Settings hub got two rows (Fonts, Notes).
## Milestone 2: home layout configuration and Clock Studio (branch feat/layout-clock-studio)

Added package `home/layout` (model, presets, codec, repository, ViewModel, `ConfiguredClock`), Layout and Clock Studio settings screens, `strings_layout.xml`, and backup keys. Shared files touched additively: `HomeScreen.kt`, `NavGraph.kt`, `SettingsScreen.kt`, `ConfigurationBackup.kt`.

| Command | Result |
|---|---|
| `JAVA_HOME=C:\Android\jdk21\jdk-21.0.13+9 ./gradlew testDebugUnitTest --tests "com.lu4p.fokuslauncher.home.layout.*" --console=plain` | First runs failed to compile (enum entries used as expression; fixed with star import). A real file-backed DataStore test failed on Windows (`Unable to rename ... .tmp`), so the persistence test uses an in-memory DataStore. Final run: **BUILD SUCCESSFUL**, 9 tests in `HomeLayoutTest` |
| `./gradlew assembleDebug testDebugUnitTest --tests "com.lu4p.fokuslauncher.ConfigurationBackupTest" --tests "com.lu4p.fokuslauncher.home.layout.*"` | **BUILD SUCCESSFUL** (3m25s); APK assembled, backup and layout tests passed. Full `testDebugUnitTest` suite not re-run. No on-device/UI verification. |

## 2026-11-20 - Home screen applies the layout config
- `HomeWidgetsSection` draws clock, date/battery, spacer, app search, focus status and notes in the saved order. Favourites stay anchored at the bottom (`HomeContentLayout`'s note-beside-rail logic depends on it).
- Size scales text through `LocalDensity.fontScale`; spacing is relative to NORMAL (`layoutPadDp`), so the default config keeps the original paddings. Alignment applies to date, app search and focus status only.
- New elements: app search (opens the drawer via `onOpenDrawer`) and focus status (shows/starts/pauses the Pomodoro session).
- Verified: `./gradlew assembleDebug testDebugUnitTest --tests "com.lu4p.fokuslauncher.home.layout.*"` BUILD SUCCESSFUL; `LayoutSpacingTest` 5/5. Not verified: appearance on a device, full unit-test suite, lint.

## 2026-11-20 - Wallpaper settings (Milestone 4)
- New package `wallpaper` and a "Wallpaper" settings page (route `settings_wallpaper`, row after Theme Studio). Picks via `PickVisualMedia` (no storage permission).
- Preview uses centre-crop at the screen aspect; `WallpaperMath.centerCrop` computes the same rect that `WallpaperRepository.apply` writes through `WallpaperManager.setBitmap` (home = FLAG_SYSTEM). Existing `setHomeUsesPhotoWallpaper(true)` is reused so the launcher switches to the photo look.
- Lock screen is an opt-in checkbox; the result toast says honestly if the device refused. The preview is labelled as not the system lock screen.
- Recent picks are downscaled JPEG copies in `filesDir/wallpapers` (max 8, oldest evicted, user can remove); no DB blobs. Colour extraction is a pure-Kotlin average on a 32px downscale (no Palette dependency).
- Built-in set is five code-generated black-to-dark gradients (no third-party images). Strings in `strings_wallpaper.xml`.
- Tests: `WallpaperMathTest` (crop, sample size, average/dominant colour, recents). Not verified on a device.
## Milestone 6: battery intelligence (branch feat/battery)
- New package `battery`: `BatteryParser` (pure, missing/invalid -> null -> "Not available"), `BatteryReader` (ACTION_BATTERY_CHANGED), `BatteryHistory` (capped 500, 5-min dedupe, text codec), `BatteryRecommendations`, `BatteryRepository` (own DataStore `battery_preferences`: history + power profile), `AppScreenTimeReader`, `BatterySettingsScreen`.
- Receiver is registered only between ON_START and ON_STOP of the screen; no service, no polling. History samples are recorded only from that receiver while the screen is open.
- Shown: percent, charge state, plug type, temperature, voltage, technology, health flag. No capacity, health %, or per-app power is shown anywhere.
- Per-app figures are foreground "screen time" (queryAndAggregateUsageStats, last 24 h, system-bucketed so approximate), reusing `UsageStatsHelper`; access is explained in a dialog before opening Android's Usage access screen; disabled state when not granted.
- Power profiles are a stored launcher-level preference with suggestions only: the launcher has no decorative-animation or refresh setting to toggle, so nothing is changed silently. Button opens `ACTION_BATTERY_SAVER_SETTINGS`.
- Shared files touched additively: `NavGraph.kt`, `SettingsScreen.kt`; strings in `strings_battery.xml`. No manifest, permission or dependency change. Not verified on a device (OnePlus 8 measurement still open).
## Milestone 5: focus profiles (branch feat/focus-profiles)
- New package `focus`: `FocusModel` (profiles Study/Work/Personal/Sleep/Custom, schedule with overnight ranges, pure `filterForFocus` and `resolveOnResume`), `FocusCodec` (versioned JSON on the theme package's strict `MiniJson`, fails safe to defaults), `FocusController` (DataStore persistence, activation, session wrapper over `PomodoroRepository`), `FocusSettingsScreen`.
- Visibility hook: `FocusVisibility.allowed` (process-wide StateFlow of `profile|package` keys, null = off). `AppDrawerViewModel.rebuildVisibleApps` and `HomeViewModel.favorites` filter through it; an empty app set means no filtering so the user cannot lock themselves out.
- Layout preset and theme are applied through `HomeLayoutRepository`/`ThemeStore`; the prior values are snapshotted and restored exactly on disable.
- Schedules are evaluated only in `MainActivity.onResume` (no service, no alarms). A user switch-off is respected until the schedule window ends.
- UI: Settings > Focus, with the notice that hiding only affects this launcher. Strings are in `strings_focus.xml`.
- Not done: focus keys are not in configuration backups; times are stepped in 30-minute taps (no time picker); private-space drawer apps are not filtered.

## Design pass: themes, Style Gallery, Theme Studio (feat/design-themes)
- Theme schema v2 (additive): optional `gradient` (2-3 stops + CSS-style angle) and `style` bundle (clock face id, layout preset name, font weight, letter spacing). v1 JSON decodes unchanged via a no-op 1->2 migration step; unknown clock ids are kept on import and resolve to `classic-digital` at use (`ClockFaces.resolve`).
- 14 new built-in themes (Aurora, Sunset Dunes, Rose Quartz, Forest Mist, Desert Sand, Lavender Dusk, Graphite, Paper, Mint, Ocean Deep, Neon Night, Sakura, Slate Blue, Coffee); the original eight ids are unchanged. A unit test enforces primary-text contrast >= 4.5 on every gradient stop of every built-in theme.
- First-run default is now Aurora (nothing chosen = Sukoon default). "Classic (Fokus-like)" is the explicit selection id `classic` (`ThemeStore.reset()` selects it) and still gives the legacy look; any saved selection is untouched.
- Home background paints the gradient through an angled `ShaderBrush` (`backgroundBrush()`); photo wallpapers still bypass themes. Settings scrims stay one solid colour (`LocalThemeBackground` = the middle stop) because 20 call sites take a `Color`.
- Typography feel: `Typography.withThemeFeel` sets weight and letter spacing on display/headline/title styles only; the font family is never touched.
- New screens: Settings > Customize > Style Gallery (`StyleGalleryScreen`, pager of `HomePreviewCard`, Dark/Light/Colourful/Minimal filters) and a skippable one-time "Choose your style" sheet (`StyleFirstRunSheet`, flag `style_prompt_seen`). Applying sets theme + layout preset (`HomeLayoutRepository`) + clock face id in one go.
- Clock face id is stored under DataStore key `clock_face_id` (`ThemeStore.clockFaceId`); the clock engine must read it, until then the preview shows faces but the home clock is unchanged.
- Theme Studio: pinned live `HomePreviewCard`, gradient editor (hex stops, optional third stop, angle presets), "Duplicate as custom theme"; export/import still goes through the strict codec.
