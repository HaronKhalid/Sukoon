# Sukoon — سکون

### Find your focus. Keep your peace.

**Sukoon** is a free, open-source, minimalist Android launcher designed to make your digital environment calmer, cleaner, and more personal.

Built around simplicity, productivity, privacy, and personalization, Sukoon gives you greater control over your Android home screen without unnecessary digital clutter.

**Simple by design. Personal by nature. Open by choice.**

<p align="center">
  <a href="https://github.com/HaronKhalid/Sukoon">
    <img src="https://img.shields.io/github/stars/HaronKhalid/Sukoon?style=for-the-badge&color=yellow" alt="GitHub Stars">
  </a>
  <a href="https://github.com/HaronKhalid/Sukoon/network/members">
    <img src="https://img.shields.io/github/forks/HaronKhalid/Sukoon?style=for-the-badge" alt="GitHub Forks">
  </a>
  <a href="https://github.com/HaronKhalid/Sukoon/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/HaronKhalid/Sukoon?style=for-the-badge" alt="License">
  </a>
  <a href="https://github.com/HaronKhalid/Sukoon/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/HaronKhalid/Sukoon/build.yml?style=for-the-badge&label=Build" alt="Build Status">
  </a>
</p>

<p align="center">
  <strong>Android Launcher · Minimalism · Productivity · Customization · Privacy · Open Source</strong>
</p>

---

## Table of Contents

- [About Sukoon](#about-sukoon)
- [Why Sukoon?](#why-sukoon)
- [Features](#features)
- [Customization](#customization)
- [Typography and Language](#typography-and-language)
- [Privacy First](#privacy-first)
- [Project Status](#project-status)
- [Roadmap](#roadmap)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Building from Source](#building-from-source)
- [Project Structure](#project-structure)
- [Credits and Acknowledgements](#credits-and-acknowledgements)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## About Sukoon

Sukoon (سکون) means *peace, calm, and tranquility*.

Modern smartphones are full of notifications, distractions, and unnecessary visual complexity. Sukoon explores a different approach: a home screen that prioritizes what matters.

Rather than forcing everyone into one design, Sukoon aims to provide a customizable experience where users can control their appearance, typography, layout, and everyday interactions.

It is an open-source Android launcher for people who appreciate:

- Minimal and distraction-free interfaces.
- Personalization without unnecessary complexity.
- Productivity-focused smartphone experiences.
- Privacy-respecting software.
- Open-source Android development.
- English and Urdu typography support.

Sukoon is free software released under the GNU General Public License v3.0.

## Why Sukoon?

Your home screen is one of the most frequently used parts of your smartphone. It should be functional, personal, and comfortable to use.

Sukoon aims to bring together:

| Principle | What it means |
|---|---|
| Minimalism | A cleaner and calmer home screen |
| Personalization | Control over themes, fonts, clocks, and appearance |
| Productivity | Useful tools without unnecessary clutter |
| Privacy | No advertising or analytics |
| Openness | Source code available for everyone |
| Accessibility | Attention to English and Urdu typography |

## Features

### 1. Custom Theme Engine

Personalize your launcher with built-in themes and custom color combinations.

- Eight built-in themes.
- AMOLED Black.
- Midnight Navy.
- Cream.
- Monochrome.
- Nature.
- Ocean.
- Soft Pastel.
- Custom theme creation.
- Preview and apply themes.
- Edit theme colors.
- Save, rename, and delete custom themes.
- Import and export themes using validated JSON.

Imported theme data is validated and never executed as code.

### 2. Theme Studio

Take greater control over your visual experience.

- Create personalized themes.
- Experiment with different color combinations.
- Preview before applying.
- Manage saved themes.
- Import and export custom themes.

### 3. Layout Presets

Choose from different visual directions for your home screen.

Available presets:

- Pure Minimal
- Aesthetic
- Focus
- Information

Element ordering, sizing, and spacing are currently stored as configuration data; their application to the home screen is still under development.

### 4. Clock Studio

Your clock should match your style.

Sukoon includes:

- Five clock styles.
- 12-hour and 24-hour formats.
- Optional seconds display.
- Locale-aware date formatting.
- Separate typography customization.

### 5. English and Urdu Typography

Sukoon gives special attention to typography and multilingual experiences.

- Separate font selection for clock and date.
- Custom typography for application labels.
- Font customization for notes.
- Cached font resources.
- English and Urdu text support.
- Mixed-direction RTL text handling.

Bundled fonts include Inter, Noto Naskh Arabic, and Noto Nastaliq Urdu.

### 6. Built-in Notes

Keep important thoughts and information accessible.

- Create multiple notes.
- Store notes locally.
- Pin a note to the home screen.
- Customize individual note appearances.

### 7. Existing Launcher Functionality

Sukoon builds upon the Fokus Launcher foundation and retains much of its existing functionality, including:

- Home screen.
- Application drawer.
- Application search.
- Favourite applications.
- Android widgets.
- Gestures.
- Pomodoro timer.
- Weather functionality.
- Private Space support.
- Icon pack support.
- Backup functionality.

See [Credits and Acknowledgements](#credits-and-acknowledgements) for the upstream project's details.

---

## Customization

Sukoon is designed around the idea that your phone should feel like your own.

The customization system is being developed to bring together:

- Colors and themes.
- Clock appearance.
- Typography.
- Date formatting.
- Notes appearance.
- Home screen layouts.

The goal is to provide meaningful customization without making the launcher unnecessarily complicated.

---

## Privacy First

Your personal device should remain personal.

Sukoon is developed with a privacy-first approach.

Current project privacy principles:

- No advertisements.
- No analytics.
- No user accounts.
- Local storage for personal notes and settings.
- Optional weather functionality uses network access.

Weather data is provided through Open-Meteo.

For more information, see:

- [Permissions and Privacy](docs/permissions-and-privacy.md)

---

## Project Status

**Status: Early Development**

Sukoon is actively being developed.

The initial implementation milestones (0–3) have been implemented and build successfully, but have not yet been verified through on-device testing. No public release has been published yet.

Some automated unit tests also have a known Windows environment issue involving usernames containing spaces. See the implementation log for details.

Please consider Sukoon an experimental project rather than a stable daily-driver replacement at this stage.

Follow the repository for development updates.

---

## Roadmap

The following features represent the project's development direction. They are planned, not promises of completed functionality.

### Completed implementation milestones

- [x] Initial launcher foundation.
- [x] Custom theme engine.
- [x] Theme Studio.
- [x] Built-in theme collection.
- [x] Custom theme import and export.
- [x] Layout preset configuration.
- [x] Clock Studio.
- [x] English and Urdu typography customization.
- [x] Local notes functionality.
- [x] Open-source acknowledgements screen.

### Planned improvements

- [ ] On-device testing and stability improvements.
- [ ] Apply layout presets to the home screen.
- [ ] Custom wallpaper management.
- [ ] Focus profiles.
- [ ] Improved home screen personalization.
- [ ] Further typography and visual customization.
- [ ] Battery insights with transparent and technically accurate reporting.
- [ ] Public release and distribution.

### Future direction

The long-term goal is to build a flexible, privacy-conscious Android launcher that combines minimalism, productivity, accessibility, and meaningful customization.

Feature requests and community suggestions are welcome.

---

## Technology Stack

Sukoon is built using modern Android development technologies.

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | UI development |
| Material Design 3 | Design system |
| AndroidX | Android libraries |
| Room | Local database |
| DataStore | Local preferences |
| Navigation | Screen navigation |
| Lifecycle | Lifecycle-aware components |
| Dagger Hilt | Dependency injection |
| Gradle Kotlin DSL | Build configuration |

---

## Getting Started

### Requirements

To build Sukoon locally, you need:

- Android Studio.
- JDK 21.
- Android SDK.
- Android SDK platforms 36 and 37.
- Android Build Tools 36.0.0.
- Git.

### Clone the repository

```bash
git clone https://github.com/HaronKhalid/Sukoon.git
```

Navigate into the project:

```bash
cd Sukoon
```

Configure your Android SDK location in `local.properties`.

Example:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

Do not commit your local SDK configuration.

---

## Building from Source

Build the debug version:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

On Windows, use:

```powershell
.\gradlew.bat assembleDebug
```

The generated debug APK will be available at:

```text
app/build/outputs/apk/debug/
```

No signing credentials or release signing material are stored in this repository.

Continuous integration is configured through GitHub Actions to run unit tests and a debug build on pushes and pull requests.

---

## Project Structure

A simplified overview of the repository:

```text
Sukoon/
│
├── .github/
│   └── workflows/
│
├── app/
│   └── src/
│       └── main/
│
├── docs/
│   ├── attribution-and-licenses.md
│   ├── implementation-log.md
│   └── permissions-and-privacy.md
│
├── fastlane/
│   └── metadata/
│
├── gradle/
│
├── scripts/
│
├── AUTHORS
├── LICENSE
├── README.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

---

## Credits and Acknowledgements

Sukoon is built upon existing open-source work. We recognize and respect the developers and communities whose work makes this project possible.

### Foundation

**[Fokus Launcher](https://github.com/luantak/FokusLauncher)**

Created by Paul Scheduikat and contributors.

License: GNU GPL v3.0.

Sukoon began as a modified copy of Fokus Launcher 1.9.8 and continues to contain most of its original code, including the home screen, app drawer, search, favourites, widgets, gestures, Pomodoro timer, weather, Private Space support, icon packs, and backup functionality.

Original copyright notices, the AUTHORS file, and applicable licensing information are retained.

### Design Reference

**[Victoria Launcher](https://github.com/adelmonte/victoria-launcher)**

Created by adelmonte.

License: GPL-3.0-or-later.

Studied for design and functionality inspiration only. No source code from this project is included in Sukoon.

### Fonts

The following fonts are bundled unmodified under the SIL Open Font License 1.1:

- [Inter](https://github.com/rsms/inter) — The Inter Project Authors.
- [Noto Fonts](https://github.com/notofonts) — Noto Naskh Arabic and Noto Nastaliq Urdu.

Font license texts are available in:

```text
app/src/main/assets/fonts/licenses/
```

### Libraries and Other Resources

Sukoon uses open-source libraries and resources, including:

- AndroidX.
- Jetpack Compose.
- Room.
- DataStore.
- Navigation.
- Lifecycle.
- Material Design 3.
- Material Symbols.
- Dagger Hilt.
- Kotlin and kotlinx libraries.
- Open-Meteo weather data (CC BY 4.0).

Arcticons icon packs are supported optionally and are not bundled.

For detailed attribution, modified-file information, and licensing details, see:

[Attribution and Licenses](docs/attribution-and-licenses.md)

Sukoon is an independent project and is not affiliated with or endorsed by the developers of the upstream projects.

---

## Contributing

Sukoon is an open-source project, and contributions are welcome.

You can contribute by:

- Reporting bugs.
- Suggesting new features.
- Improving documentation.
- Reviewing code.
- Improving accessibility.
- Testing the application on different Android devices.
- Submitting pull requests.

### How to contribute

1. Fork the repository.
2. Create a new feature branch.
3. Make your changes.
4. Test your implementation.
5. Submit a pull request with a clear explanation.

For significant changes, consider opening an issue first to discuss the proposed implementation.

Please respect the existing upstream attribution and licensing requirements when contributing.

---

## License

Sukoon is free and open-source software licensed under the:

**GNU General Public License v3.0 (GPL-3.0)**

You are free to use, study, modify, and distribute the software under the terms of this license.

See the [LICENSE](LICENSE) file for the complete license.

If you distribute binaries, you must comply with the GPL-3.0 source-code requirements.

---

## Contact

**Project:** Sukoon — سکون

**Developer:** Haron Khalid

- GitHub: [@HaronKhalid](https://github.com/HaronKhalid)
- Repository: [github.com/HaronKhalid/Sukoon](https://github.com/HaronKhalid/Sukoon)

For bugs, feature requests, and discussions, visit the repository's [Issues](https://github.com/HaronKhalid/Sukoon/issues) section.

---

<p align="center">
  **Made with care for a calmer digital experience.**
  
  Sukoon — سکون
  
  *Find your focus. Keep your peace.*
</p>