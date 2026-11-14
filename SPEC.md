# Ondy - Specification Document

## 1. Project Overview

- **Project Name**: Ondy
- **Package**: `com.ondy.app`
- **Project Type**: Native Android application (Kotlin, Jetpack Compose)
- **Version**: 1.1.0 (versionCode 2)
- **Distribution**: GitHub releases (`github.com/dhanush777x/ondy`). Not distributed through Google Play.
- **Core Functionality**: Ondy intercepts notifications from user-selected apps, stores them locally, and delivers a summary notification at user-defined times of day so alerts can be reviewed on demand instead of interrupting.

## 2. Technology Stack

- **Language**: Kotlin (JVM target 17)
- **Min SDK**: 26 (Android 8.0)
- **Target / Compile SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **UI**: Jetpack Compose, Compose BOM `2024.02.00`, Material 3, Material Icons Extended
- **Navigation**: Navigation Compose `2.7.7` (single activity)
- **Architecture**: MVVM with Clean Architecture layering
- **Database**: Room `2.6.1` (KSP)
- **Dependency Injection**: Hilt `2.50` (KSP), Hilt Navigation Compose `1.1.0`
- **Preferences**: DataStore Preferences `1.0.0`
- **Image Loading**: Coil `2.5.0` (app icons)
- **Scheduling**: `AlarmManager` (exact where permitted, inexact fallback)
- **Async**: Kotlin Coroutines + Flow, `StateFlow` in ViewModels
- **Testing**: JUnit 4, MockK, `kotlinx-coroutines-test`, real `org.json` for JVM tests

## 3. Architecture

```
ui/         Compose screens, components, theme, ViewModels
domain/     Models, repository interfaces, use-case types
data/       Room entities/DAOs, DataStore preferences, repository implementations
service/    NotificationListenerService
receiver/   Alarm receiver, boot/update receiver
di/         Hilt modules
util/       Alarm scheduling, package/icon cache
```

- Single `MainActivity` hosts a Compose `NavHost` with a bottom navigation bar (Apps, Schedule, Notifications) plus a Settings route that hides the bar.
- Every main tab shares one top app bar: brand mark, section title, and a settings icon on the right.
- ViewModels expose immutable UI state via `StateFlow`; screens collect with `collectAsStateWithLifecycle`.
- Repositories are interfaces in `domain/` and implemented in `data/`, bound through Hilt.
- `AppCache` holds a process-wide snapshot of installed apps and icons so the notification-listener hot path never scans the `PackageManager` or touches the database.

## 4. Feature List

1. **Notification Capture** - `NotificationListenerService` intercepts notifications from selected packages; no accessibility service required.
2. **App Selection** - Installed-app list with search and User / System / All filters; multi-select to choose which apps are intercepted.
3. **Scheduled Summaries** - Add and remove any number of daily times (for example 9:00, 12:00, 20:00).
4. **Summary Notification** - At each scheduled time, Ondy posts a notification with the number of blocked notifications; tapping opens the history.
5. **Notification History** - All intercepted notifications are stored locally and listed with app name, title, content, and relative timestamp. Individual entries can be deleted and the whole history cleared at once.
6. **Onboarding** - Four-page welcome flow covering notification access, battery-optimization guidance, and the privacy model. Notification access is required; battery exemption is recommended but not blocking.
7. **Theme Modes** - Material You (system light/dark with wallpaper-derived dynamic color on Android 12+), Light, and Dark. The choice is persisted and applied immediately.
8. **Backup & Restore** - Export selected apps and schedule times to a JSON file, and import one back, through the Storage Access Framework (no storage permission required). Import replaces the current selection and schedule inside a single Room transaction, cancels stale alarms, and re-arms the restored schedule. Notification history is never included or modified.
9. **Automatic Recovery** - Saved alarms are re-registered after reboot and after an app update.
10. **Reliable Background Operation** - The listener promotes itself to a foreground service (`dataSync`) to survive aggressive OEM background restrictions.
11. **Duplicate Suppression** - Already-processed notification keys and near-identical reposts are collapsed so the history stays clean.
12. **Privacy First** - No network access, no analytics, no tracking. Android cloud backup and device transfer are disabled; all data stays in the app sandbox.

## 5. Data Model

Room database `ondy_db`, version 5. Pre-release databases are recreated via `fallbackToDestructiveMigration()`; a real migration chain will replace this if the database ever ships publicly in its current form.

| Table | Purpose | Key columns |
|---|---|---|
| `selected_apps` | Packages the user wants intercepted | `packageName` (unique index) |
| `schedule_times` | Daily summary times | `hour`, `minute` |
| `blocked_notifications` | Intercepted notification history | `packageName`, `appName`, `title`, `content`, `timestamp`, `notificationKey` (unique), `filterId`, `showInHistory`, `showInWidget` |

### Backup file format

```json
{
  "version": 1,
  "exportedAt": 1730000000000,
  "selectedApps": ["com.example.app"],
  "scheduleTimes": [{ "hour": 9, "minute": 30 }]
}
```

- Encoding normalizes input: packages are trimmed, de-duplicated, and sorted; times are validated, de-duplicated, and sorted.
- Decoding is forgiving: unknown fields are ignored, duplicate entries collapse, and out-of-range times are dropped. Only a wrong format version, a non-object document, or a document with neither data section is rejected.
- Bounds protect against malformed files: 5,000 packages and 1,440 schedule entries.

## 6. Permissions

- `BIND_NOTIFICATION_LISTENER_SERVICE` - required by the listener service.
- `POST_NOTIFICATIONS` - summary notifications on Android 13+.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - onboarding prompt for reliable background work.
- `RECEIVE_BOOT_COMPLETED` - restore alarms after reboot.
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` - exact summary delivery; falls back to inexact while-idle alarms when denied.
- `WAKE_LOCK` - alarm delivery while idle.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` - keep interception alive.
- `QUERY_ALL_PACKAGES` - list installed apps for selection. Ondy is not distributed on Google Play, so the accompanying policy does not apply.

## 7. UI / UX

- **Visual style**: Material 3, cards with 12-16dp rounded corners, tonal surfaces, no custom illustration beyond the brand mark.
- **Brand mark**: bell/orbit artwork sourced from `app/assets/logo.png`, exported at all densities, tintable in the UI, and used for the adaptive launcher and themed (monochrome) icon.
- **Top bar**: logo, section title, settings gear.
- **Bottom navigation**: three destinations; selected item uses the theme accent (`primary`) with a 16% accent pill; unselected items use `onSurfaceVariant`.
- **Palettes**:
  - Light: deeper periwinkle `#5A5FD6` accent with white content, soft `#EFF1F5` background.
  - Dark: lavender `#B4BEFE` accent with dark content, `#1E1E2E` background (Catppuccin-inspired fallback).
  - Material You: wallpaper-derived dynamic palette on Android 12+; system light/dark otherwise.
- **Components**: app cards with selection outline and check mark, filter chips, search field, schedule cards with remove action, floating action button to add a time, time picker themed with the accent, notification cards with delete action.
- **Empty states**: dedicated copy for no apps, no schedule, and no history.
- **Settings**: Appearance (theme mode radio group), Backup & Restore (export/import with confirmation), About (logo, version, GitHub link, credit).

## 8. Build & Release

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # signed release APK
./gradlew testDebugUnitTest  # unit tests
./gradlew lintDebug          # Android Lint
```

- Release builds enable R8 minification and resource shrinking.
- Signing uses `app/release.keystore` together with the `ondy.store.password`, `ondy.key.alias`, and `ondy.key.password` entries in `local.properties`. Both are git-ignored. When either is missing, the build still configures, but release output is unsigned.
- `local.properties` is optional for configuration; only the SDK path and signing values are read from it.

## 9. Testing

- `BackupCodecTest` - encoding normalization and decoding/validation rules.
- `NotificationRepositoryTest` - repository mapping and persistence behavior.
- `AppsViewModelTest` - search, filtering, and selection state.
- Lint runs with a warning baseline; the build has zero lint errors.

## 10. Non-Goals

- No Google Play distribution, billing, or Play policy compliance work.
- No cloud sync, accounts, or network access of any kind.
- No analytics, telemetry, or crash reporting.
- No tablet-specific layout or localization beyond English (`values/` only).
