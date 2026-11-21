# Attribution and licenses

Sukoon is a derivative of **Fokus Launcher** by Paul Scheduikat (lu4p) and contributors,
https://github.com/luantak/FokusLauncher, licensed under the **GNU GPL v3** (see `LICENSE`).
Sukoon is therefore distributed under GPL-3.0; full source is at https://github.com/HaronKhalid/Sukoon.
Sukoon began as a modified copy of Fokus Launcher 1.9.8. Upstream copyright notices, `AUTHORS` and the licence are kept; the original history lives in the upstream repository (this repository's history was replaced by a fresh single commit on 2026-11-20).

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

### Bundled display fonts (clock styles, feat/clock-library, 2026-11-21)
Twelve more files, all **SIL Open Font License 1.1**, **unmodified**, fetched on 2026-11-21 into a fresh scratch directory from the official
google/fonts repository (`https://raw.githubusercontent.com/google/fonts/main/ofl/<family>/`), each `OFL.txt` read and confirmed to contain the
OFL 1.1 text, each file confirmed to be a TrueType file, then copied to `assets/fonts` with the licence in `assets/fonts/licenses`.
Reserved Font Names (Dancing Script, Playfair Display, Righteous) are only used under their own names; no derived font is made.
Orbitron was not bundled (Righteous covers the display-geometric slot). Jameel Noori Nastaleeq is still not bundled.

| Font | File | Source URL | Licence file | Copyright |
|---|---|---|---|---|
| Montserrat (wght variable) | `fonts/Montserrat-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/montserrat | `licenses/Montserrat-OFL.txt` | Copyright 2024 The Montserrat.Git Project Authors |
| Poppins Regular (static) | `fonts/Poppins-Regular.ttf` | https://github.com/google/fonts/tree/main/ofl/poppins | `licenses/Poppins-OFL.txt` | Copyright 2020 The Poppins Project Authors |
| Poppins Bold (static) | `fonts/Poppins-Bold.ttf` | https://github.com/google/fonts/tree/main/ofl/poppins | `licenses/Poppins-OFL.txt` | Copyright 2020 The Poppins Project Authors |
| Outfit (wght variable) | `fonts/Outfit-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/outfit | `licenses/Outfit-OFL.txt` | Copyright 2021 The Outfit Project Authors |
| Nunito (wght variable) | `fonts/Nunito-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/nunito | `licenses/Nunito-OFL.txt` | Copyright 2014 The Nunito Project Authors |
| Fredoka (wdth,wght variable) | `fonts/Fredoka-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/fredoka | `licenses/Fredoka-OFL.txt` | Copyright 2016 The Fredoka Project Authors |
| Bebas Neue (static) | `fonts/BebasNeue-Regular.ttf` | https://github.com/google/fonts/tree/main/ofl/bebasneue | `licenses/BebasNeue-OFL.txt` | Copyright 2010 by Dharma Type |
| Caveat (wght variable) | `fonts/Caveat-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/caveat | `licenses/Caveat-OFL.txt` | Copyright 2014 The Caveat Project Authors |
| Dancing Script (wght variable) | `fonts/DancingScript-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/dancingscript | `licenses/DancingScript-OFL.txt` | Copyright 2016 The Dancing Script Project Authors (RFN 'Dancing Script') |
| Playfair Display (wght variable) | `fonts/PlayfairDisplay-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/playfairdisplay | `licenses/PlayfairDisplay-OFL.txt` | Copyright 2017 The Playfair Display Project Authors (RFN 'Playfair Display') |
| Space Grotesk (wght variable) | `fonts/SpaceGrotesk-Variable.ttf` | https://github.com/google/fonts/tree/main/ofl/spacegrotesk | `licenses/SpaceGrotesk-OFL.txt` | Copyright 2020 The Space Grotesk Project Authors |
| Righteous (static) | `fonts/Righteous-Regular.ttf` | https://github.com/google/fonts/tree/main/ofl/righteous | `licenses/Righteous-OFL.txt` | Copyright (c) 2011 Brian J. Bonislawsky DBA Astigmatic (AOETI), RFN 'Righteous' |

A unit test (`FontLicenseManifestTest`) fails if any bundled font file lacks a registry entry or an OFL licence file.

OFL obligations met: licence + copyright shipped with the fonts; fonts not sold on their own; no modified versions distributed.

**Not bundled:** Jameel Noori Nastaleeq (no confirmed redistribution licence; excluded by brief and covered by a unit test).
Roboto was not bundled; Montserrat, Poppins and Outfit are now bundled (see display fonts below) (not needed; users can still import their own TTF/OTF via the existing custom-font feature).
Open question for legal review before public binaries: confirm GPL-3.0 vs GPL-3.0-or-later compatibility when Victoria code is ported.

**Imported fonts:** users can import their own `.ttf`/`.otf` (for example Jameel Noori Nastaleeq) from Fonts settings. The file is copied to app-private storage on the device only; Sukoon does not ship, upload or redistribute it. Imported font files are not part of configuration backups (the font choice key is; after a restore the font falls back to the system font until re-imported).
