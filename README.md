<p align="center"><img src="docs/logo.png" width="128" alt="Sukoon logo"></p>

# Sukoon

**Sukoon** (سکون, "calm") is a focus-first, customizable Android launcher by **Haron Khalid**.
It is free software under the GNU GPL v3.0.

Source code: https://github.com/HaronKhalid/Sukoon

## What Sukoon adds (built by us)

- **Theme engine and Theme Studio** — 8 built-in themes (AMOLED Black, Midnight Navy, Cream,
  Monochrome, Nature, Ocean, Soft Pastel, Custom); preview, apply, reset, edit colours, save, rename
  and delete custom themes; strict, validated JSON import and export. Imported data is never executed.
- **Layout presets and Clock Studio** — Pure Minimal, Aesthetic, Focus and Information presets;
  5 clock styles, 12/24-hour, optional seconds, locale-aware dates. *Element order, size and spacing are
  stored but not yet applied to the home screen.*
- **English and Urdu typography** — separate font choice for the clock, date, app labels and notes,
  with cached fonts and mixed-direction (RTL) text handling.
- **In-app notes** — multiple notes stored on the device, one pinned to the home screen, with
  per-note appearance.
- **Acknowledgements screen** — Settings shows every upstream project, library and font with its
  licence, plus the full GPL-3.0 text.

Planned: wallpapers, focus profiles, and honest battery insights (no per-app power claims). See `docs/`.

## Credits and open-source acknowledgements

Sukoon is built on the work of others. Thank you to everyone below.

### Foundation
- **Fokus Launcher** by **Paul Scheduikat** and **contributors** — GPL-3.0.
  Sukoon began as a modified copy of Fokus Launcher 1.9.8 and still contains most of its code:
  home screen, app drawer and search, favourites, widgets, gestures, Pomodoro timer, weather, Private Space
  support, icon packs and backup. https://github.com/luantak/FokusLauncher
  The upstream copyright notices, the `AUTHORS` file and the licence are kept.

### Reference only
- **Victoria Launcher** by **adelmonte** — GPL-3.0-or-later. Studied for ideas only. **No code from it is
  included.** https://github.com/adelmonte/victoria-launcher

### Fonts (bundled, SIL Open Font License 1.1, unmodified)
- **Inter** — The Inter Project Authors (https://github.com/rsms/inter)
- **Noto Naskh Arabic** and **Noto Nastaliq Urdu** — The Noto Project Authors (https://github.com/notofonts)

Licence texts are in `app/src/main/assets/fonts/licenses/`.

### Libraries and data
**AndroidX** (Jetpack Compose, Room, DataStore, Navigation, Lifecycle), **Material Design 3** and
**Material Symbols**, **Dagger Hilt**, **Kotlin** and **kotlinx** — Apache-2.0.
**Open-Meteo** weather data — CC BY 4.0. **Arcticons** icon packs — optional, not bundled.

Full details, the list of modified files and open legal questions are in
`docs/attribution-and-licenses.md`. Sukoon is not affiliated with or endorsed by the authors above.

## Status
Early work in progress. Milestones 0–3 are implemented and build; they have not yet been tested on a
device. No release has been published. Three unit tests fail on a Windows machine whose user name contains
a space; this is an environment problem, explained in `docs/implementation-log.md`.

## Build
Requires JDK 21 and the Android SDK (platforms 36 and 37.0, build-tools 36.0.0). Point `sdk.dir` at your SDK in
`local.properties` (not committed).
```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
Debug APK: `app/build/outputs/apk/debug/`. No signing material is stored in this repository.
CI (`.github/workflows/build.yml`) runs unit tests and a debug build on push and pull request.

## Privacy
No analytics, accounts or ads; data stays on the device. Network access is limited to optional weather.
Details in `docs/permissions-and-privacy.md`.

## Licence
GNU GPL v3.0 — see [LICENSE](LICENSE). Anyone receiving a binary is entitled to the corresponding source
(link above).
