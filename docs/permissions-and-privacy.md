# Permissions and privacy

Status: describes the manifest as of the `feat/compliance-baseline` branch (still the inherited
Fokus 1.9.8 permission set; Sukoon has not added permissions yet). Checked against
`app/src/main/AndroidManifest.xml`.

## Principles
- No analytics, accounts, ads or crash-reporting SDKs. Notes, usage data and settings stay on the device.
- The only network use is the optional weather feature (Open-Meteo: forecast, geocoding, air quality).
  Weather needs approximate location (runtime prompt) or a manually typed place name.
- Optional special access is explained in-app before the user is sent to Android Settings, and the
  launcher keeps working if it is denied.

## Declared permissions
| Permission | Why | Notes |
|---|---|---|
| `INTERNET` | Open-Meteo weather requests | Only used when weather is enabled |
| `ACCESS_COARSE_LOCATION` | Weather for current area | Runtime prompt, optional; manual location possible |
| `REQUEST_DELETE_PACKAGES` | "Uninstall" action in app long-press menu | Hands off to the system uninstaller |
| `ACCESS_HIDDEN_PROFILES` | Show Private Space apps (Android 15+) | Only meaningful when Private Space is unlocked |
| `EXPAND_STATUS_BAR` | Swipe gesture to open notification shade | Normal permission |
| `SET_WALLPAPER` | Set/clear the system wallpaper from settings (for example a black wallpaper) | Normal permission |
| `POST_NOTIFICATIONS` | Pomodoro timer alerts (Android 13+) | Requested from the Pomodoro settings screen only |
| `PACKAGE_USAGE_STATS` | Optional home-screen "screen time" display (last 24 h total) | Special access; user must grant it in Android Settings. Not required for core use |

## Optional services (off until the user enables them)
- **AccessibilityService** (`BIND_ACCESSIBILITY_SERVICE`): double-tap-to-lock and return home after a long
  lock only. It does not read other apps' content. A prominent disclosure is shown before enabling.
- **NotificationListenerService** (`BIND_NOTIFICATION_LISTENER_SERVICE`): media "now playing" controls only.

## Not requested
Contacts, SMS, call log, microphone, camera, broad storage access (folders and backups use the system
document picker / SAF), and background location.

## Data handling
- Settings and categories are stored locally (DataStore / Room). `allowBackup` is on with
  `data_extraction_rules`; review these rules before a public release.
- Diagnostic logs are only exported when the user taps "Export logs" and chooses where to share them.
- Third-party code and data sources are listed in Settings > Open-source acknowledgements and in
  `docs/attribution-and-licenses.md`.

## Open items before release
- Review the `PACKAGE_USAGE_STATS` and `ACCESS_HIDDEN_PROFILES` justifications against current Google Play policy.
- Re-verify this table whenever the manifest changes (planned features such as focus profiles and battery
  insights must follow the least-privilege rules in the product brief).
Inherited from Fokus (unchanged so far): REQUEST_DELETE_PACKAGES, INTERNET (weather), ACCESS_COARSE_LOCATION (weather, runtime prompt), ACCESS_HIDDEN_PROFILES (Private Space), EXPAND_STATUS_BAR, SET_WALLPAPER; optional AccessibilityService (double-tap lock only), optional notification listener (media controls).
No analytics, accounts or ads. Data stays on device.

## Notes and fonts (Milestone 3)
- In-app notes are stored **only on the device** in the app's private Room database (`fokus_launcher_db`, table `notes`). No account, no sync, no network call, no analytics, no new permission.
- Notes are not included in any export or share action; the optional SAF markdown note folder remains the only way a note leaves private storage, and only when the user picks that folder.
- Bundled fonts are read from the APK assets; per-element font choices are stored locally in `typography_preferences` (DataStore).

## Battery (Milestone 6)
- No new permission. Battery values come from the `ACTION_BATTERY_CHANGED` sticky broadcast (no permission needed), registered only while the Battery settings screen is visible.
- Level history (timestamp, level, charging; max 500) is stored only on the device in the `battery_preferences` DataStore and can be wiped with "Clear data". Not exported.
- Per-app screen time uses the existing optional `PACKAGE_USAGE_STATS` access, explained before the user is sent to Android Settings; it is labelled screen time, never battery use.

## Reminders and agenda
- Reminders are stored only on the device (separate `reminders` DataStore, versioned JSON, max 200). They are not in configuration backups and never leave the phone.
- `POST_NOTIFICATIONS` (already declared) is requested only when the user turns on "Notify me" for a reminder. Notifications default to lock-screen visibility PRIVATE: the lock screen shows only "Reminder"; a setting shows the full text.
- `RECEIVE_BOOT_COMPLETED` (new): a minimal receiver that only re-arms the single next reminder alarm after a reboot.
- Alarms use `AlarmManager.setAndAllowWhileIdle` (inexact). `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` are not requested; there is no foreground service and no background polling. Only the next due reminder is scheduled.
- `READ_CALENDAR` (new, optional, off by default): used only if the user switches on "Also show calendar events" and, after an explanation dialog, grants it. Events for the next 3 days are read through `CalendarContract.Instances` when the Reminders screen or the home agenda widget is shown, kept in memory only, never stored or sent. If denied or revoked, the feature stays off and reminders are unaffected. The `lite` manifest overlay is unchanged.
