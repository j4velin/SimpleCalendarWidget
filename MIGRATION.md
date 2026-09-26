# Modernization

The work was planned up front and done in three phases, each in its own commit. This file
records the plan and what actually changed.

## Starting point

* A single Android app module that was cut out of a larger multi-project build. There was no
  `settings.gradle`, no root build file and no Gradle wrapper, and the module read
  `compileSdkVersion` from `rootProject.ext`.
* Three dependencies pointed at modules that aren't in this repository, so they couldn't be
  resolved:
  * `:dateFormatSpinner` provided `DateFormatSpinner`.
  * `:colorpicker` provided `ColorPickerDialog` and `ColorPreviewButton`.
  * `wearApp ':CalendarWidgetWear'` was the Wear OS companion app. The phone-side `Wear`
    listener service used the removed `GoogleApiClient` / `Wearable.DataApi` APIs.
* The build always loaded `key.properties` for release signing, even for debug builds.
* The code was Java with XML layouts: Holo/Material framework themes, `ActionBar`
  navigation tabs, `AsyncTask`, a `ViewPager` of fragments, and widgets built with
  `RemoteViews` + `RemoteViewsService`.

## Phase 1: make it build (done)

* Made it a standalone Gradle project (settings file, wrapper, `gradle.properties`).
* **Dropped Wear OS support**: the `wearApp` dependency, `play-services-wearable` and the
  `Wear` service.
* Replaced the color picker and date format spinner libraries with minimal in-app
  stand-ins. Those stand-ins were themselves replaced by Compose in phase 3.
* Release signing now only applies if `key.properties` exists. Debug builds use the debug
  key.

## Phase 2: latest Android (done)

* Gradle 9.8, AGP 9.4, Kotlin DSL build scripts with a version catalog, Java 21.
* compileSdk/targetSdk **37** (Android 17), minSdk **26** (Android 8.0).
* Removed `WRITE_EXTERNAL_STORAGE`. Backups live in `getExternalFilesDir()`.
* Added a `<queries>` block so the "open another app" picker can see launcher apps.
* Collection click templates are `FLAG_MUTABLE`. Before, per-day/per-event taps lost their
  extras on API 31+, so every tap opened the calendar at "now".
* Month widgets now also refresh on calendar/time changes; before, only agenda widget IDs
  were refreshed. `TIMEZONE_CHANGED` is handled too.
* Removed the dead `file://` .ics intent filters. Both widgets are marked `reconfigurable`.

## Phase 3: Kotlin, Compose and Glance (done)

* All Java code was replaced by Kotlin. There are no XML layouts left.
* **Widgets use Jetpack Glance.** Class names (`Widget`, `MonthWidget`,
  `settings.WidgetConfig`, …) and **all SharedPreferences keys stay the same**, so placed
  widgets and their settings survive the update. The backup file format is also unchanged.
  * A per-widget Glance state key (`WidgetUpdates.RefreshKey`) is bumped whenever the data
    must reload: alarms, the calendar content-change job, time broadcasts and saving the
    config. Without it, a running Glance session would not recompose.
  * Calendar taps go through `CalendarActionActivity`, a no-UI trampoline. Starting other
    apps from a `BroadcastReceiver` is blocked by background activity start restrictions.
  * The month widget runs a single calendar query per render instead of one per day (42
    queries).
* **The config screens use Compose + Material 3**, with dynamic color and edge-to-edge.
  The agenda config has Events / Appearance / Settings tabs; the month config is a single
  scrolling page. New in-app components: a color picker (presets, ARGB sliders, hex), a
  date format field (presets + live preview + validation), an app picker, and a permission
  screen.
* Glance uses WorkManager internally. `App` limits WorkManager to JobScheduler IDs above
  1000 so its jobs can't collide with the app's own calendar observer job.

### Bugs fixed along the way

* All-day events are stored at UTC midnight. They are now converted to local midnight, so
  users west of UTC no longer see them on the previous day.
* A running event that started in the previous year was shown on its start date instead of
  today.
* Events that ended late yesterday could show up as all-day events today.
* The month widget highlighted "today" by day of year only, so the same date in the
  adjacent year was highlighted too.
* The .ics import upper-cased titles, descriptions and locations. It now also handles folded
  lines, escaped characters and all-day (`VALUE=DATE`) events.
* Settings of deleted widgets are now removed.

### Dropped

* Wear OS companion support.
* The explicit HTC / Motorola calendar fallbacks when opening the calendar. The standard
  `VIEW` / `INSERT` intents plus the AOSP/Google calendar fallbacks remain.
* Devices below Android 8.0.

## Follow-up improvements (done)

* New widgets preselect all calendars that are visible in the calendar app.
* The config screens have **Done** and cancel (✕) buttons. Changes are only saved with
  Done. Cancelling or pressing Back asks before discarding them if something changed (always,
  for a new widget, since it also cancels placing it). Leaving via Home discards silently.
* Adaptive launcher icon, including a monochrome layer for themed icons (Android 13+).
* Widgets:
  * rounded corners (Android 12+)
  * default grid sizes and picker descriptions
  * compact icons when the widget is small
  * generated previews in the widget picker (Android 15+)
* GitHub Actions workflow: build, lint and tests on every push and pull request.

## Translations (done)

The app is now fully translated into all 17 existing languages. Before, only German was
complete.

* **New texts:** the 35 texts added during the modernization are translated. They cover the
  permission screen, dialogs, widget names and descriptions, and preview sample events.
* **Older gaps filled:**

  | Language | Texts added |
  |---|---|
  | Arabic | 63 |
  | Hebrew, Japanese, Korean, Portuguese | 61 each |
  | Turkish | 44 |
  | Czech, Spanish, Dutch, Polish, Russian, Chinese (Taiwan) | 19 each |
  | Danish, French, Italian, Swedish | 6 each |

  Hebrew, Japanese, Korean and Portuguese also got the missing look-ahead unit list
  (days/weeks/months/years).
* **Backup folder:** the backup dialog named a folder that doesn't exist. In every language it
  now names `/Android/data/de.j4velin.calendarWidget/files`.
* **Lint:** the `MissingTranslation` check is no longer suppressed, so any new text without
  translations is flagged.

The added translations were machine-written and have not been reviewed by native speakers.
Review them before release, especially Arabic, Hebrew, Japanese, Korean, Portuguese and Turkish,
which previously had few or no strings to match the wording against. Also check the
right-to-left layout in Arabic and Hebrew.

## Verification

`./gradlew assembleDebug assembleRelease lintDebug testDebugUnitTest`

* JVM unit tests cover agenda building, the month grid and event counting, time/date
  labels, calendar ID parsing and the .ics parser.
* Robolectric tests render both widgets through `GlanceRemoteViews` and inflate the
  resulting `RemoteViews`. They also drive the Compose config screens and the color picker.

Still to test on a real device before release:

* Placing both widgets.
* The calendar-permission flow.
* Tapping days and events, including "open another app".
* Month paging.
* Backup and restore.
* Updating over an installed 2.5.1 with configured widgets.
