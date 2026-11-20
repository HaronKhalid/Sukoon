# Architecture proposal

**Base: Fokus Launcher.** Keep Kotlin + Compose + Material 3 + Hilt + Room + DataStore + Navigation Compose. No new frameworks.

- Kotlin namespace `com.lu4p.fokuslauncher` is kept for now (renaming 260 files would bloat the diff and hinder merging upstream fixes). Only `applicationId` (`io.github.haronkhalid.sukoon`), label and version are Sukoon's.
- New code goes in new packages: `theme/` (data-driven theme model + DataStore persistence), `home/layout/` (element config + presets), `notes/` (Room), `typography/` (font registry + cache), `focus/`, `battery/`.
- Themes/layout stored as versioned JSON in DataStore; imported data is validated and never executed.
- Victoria is reference only; selected ideas ported file-by-file with entries in `attribution-and-licenses.md`.

## Risks
| Risk | Mitigation |
|---|---|
| Large upstream codebase, `HomeScreen`/`HomeViewModel` are big | Add layout config behind existing composables incrementally |
| Fokus translations still say "Fokus" | Default English rebranded; other locales to be updated |
| Weblate link points at Fokus project | Replace when Sukoon has its own |
| Font licensing | Only bundle OFL fonts with recorded licences |
| Battery claims | Show only `BatteryManager` data; no per-app power claims |
| GPL | Whole app stays GPL-3.0; source published at the Sukoon repo |
