# Attribution and licenses

Sukoon is a derivative of **Fokus Launcher** by Paul Scheduikat (lu4p) and contributors,
https://github.com/luantak/FokusLauncher, licensed under the **GNU GPL v3** (see `LICENSE`).
Sukoon is therefore distributed under GPL-3.0; full source is at https://github.com/HaronKhalid/Sukoon.
Upstream git history is preserved; upstream copyright notices and `AUTHORS` are unchanged.

**Reference only (no code ported yet):** Victoria Launcher, https://github.com/adelmonte/victoria-launcher, GPL-3.0-or-later.

## Modified files (so far)
- `app/build.gradle.kts` – applicationId, versionCode/Name
- `app/src/main/res/values/strings.xml` – branding strings
- `app/src/main/res/xml/lock_screen_accessibility_config.xml` – package name
- `ui/settings/SettingsControls.kt` – source link
- `HomeViewModelTest.kt` – package name
- `ui/settings/AcknowledgementsScreen.kt`, `res/values/strings_about.xml`, `assets/licenses/GPL-3.0.txt` – new: acknowledgements screen and GPL viewer
- `ui/settings/SettingsScreen.kt` – entry row for the acknowledgements screen
- `ui/settings/SettingsControls.kt` – removed upstream Weblate, Matrix and PayPal links
- `res/values-*/strings.xml`, `fastlane/metadata`, `.github/` templates – app name Fokus replaced by Sukoon
- `.github/workflows/` – upstream release/scanner workflows replaced; `.github/FUNDING.yml` removed (pointed to upstream author)

Known remaining upstream identifiers (intentionally kept): Kotlin package `com.lu4p.fokuslauncher`, class names `Fokus*`, `Theme.FokusLauncher`, historical fastlane changelogs, store screenshots/icon in `fastlane/metadata/android/en-US/images` (still show Fokus branding; replace before any store listing).

## Third-party assets
Libraries (Apache-2.0: AndroidX/Compose/Room/DataStore/Navigation, Material Components, Hilt/Dagger, Kotlin) and Open-Meteo data (CC BY 4.0) are credited in-app. Fonts/wallpapers: none added yet. Each will be listed here with licence before inclusion.
Wallpapers: none added yet. Each will be listed here with licence before inclusion.

### Bundled fonts (Milestone 3)
All three are **SIL Open Font License 1.1**, shipped **unmodified** (variable TrueType files) in `app/src/main/assets/fonts/`,
with each licence text in `app/src/main/assets/fonts/licenses/` (also packaged in the APK). Downloaded 2026-11-20 from the official
google/fonts repository (`raw.githubusercontent.com/google/fonts/main/ofl/...`) into a scratch directory, licence file read and
checked for the OFL 1.1 header before copying. None declares a Reserved Font Name that we use in a derived font (no modification).

| Font | File in repo | Source URL | Licence file | Copyright line |
|---|---|---|---|---|
| Inter (opsz,wght variable) | `fonts/Inter-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/inter (upstream https://github.com/rsms/inter) | `licenses/Inter-OFL.txt` | Copyright 2020 The Inter Project Authors |
| Noto Naskh Arabic (wght variable) | `fonts/NotoNaskhArabic-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/notonaskharabic (upstream https://github.com/notofonts/arabic) | `licenses/NotoNaskhArabic-OFL.txt` | Copyright 2022 The Noto Project Authors |
| Noto Nastaliq Urdu (wght variable) | `fonts/NotoNastaliqUrdu-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/notonastaliqurdu (upstream https://github.com/notofonts/nastaliq) | `licenses/NotoNastaliqUrdu-OFL.txt` | Copyright 2022 The Noto Project Authors |

OFL obligations met: licence + copyright shipped with the fonts; fonts not sold on their own; no modified versions distributed.

**Not bundled:** Jameel Noori Nastaleeq (no confirmed redistribution licence; excluded by brief and covered by a unit test).
Roboto, Montserrat, Poppins, Outfit were not bundled in this pass (not needed; users can still import their own TTF/OTF via the existing custom-font feature).
Open question for legal review before public binaries: confirm GPL-3.0 vs GPL-3.0-or-later compatibility when Victoria code is ported.
