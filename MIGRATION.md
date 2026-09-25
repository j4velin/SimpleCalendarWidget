# Modernization plan

## Where the code stands

* A single Android app module (`build.gradle` + `src/main`) that was cut out of a
  larger multi-project build. There is no `settings.gradle`, no root build file and
  no Gradle wrapper, and the module reads `compileSdkVersion` from `rootProject.ext`.
* Three dependencies point at modules that aren't in this repo, so they can't be
  resolved:
  * `project(':dateFormatSpinner')` provides `DateFormatSpinner`, used in the appearance settings.
  * `project(':colorpicker')` provides `ColorPickerDialog` and `ColorPreviewButton`, used in both config screens.
  * `wearApp project(':CalendarWidgetWear')` is the Wear OS companion app.
* `com.google.android.gms:play-services-wearable` can still be downloaded, but it's
  only there for the `Wear` listener service. That service is useless without the
  watch app, and it relies on the removed `GoogleApiClient` / `Wearable.DataApi`
  APIs.
* The build loads `key.properties` for release signing unconditionally, so it fails
  without that local file. The debug build also uses the release keys.
* The code is Java with XML layouts. It uses Holo/Material framework themes,
  `ActionBar` navigation tabs (deprecated since API 21), `AsyncTask`,
  `ProgressDialog` and a `ViewPager` of support fragments. The widgets are built
  with `RemoteViews` plus `RemoteViewsService` list/grid adapters.

## Phase 1 – make it build (Java, same features)

1. Make it a standalone Gradle project: `settings.gradle.kts`, root
   `build.gradle.kts`, a Gradle wrapper, `gradle.properties` (AndroidX) and a
   version catalog.
2. Remove the missing modules:
   * **Wear OS support (dropped feature).** Remove the `wearApp` dependency,
     `play-services-wearable`, the `Wear` service, and the GMS meta-data in the
     manifest.
   * **Color picker (replaced).** Add a small in-app color preview button and a
     hex/ARGB input dialog.
   * **Date format spinner (replaced).** Use a plain text field for the pattern.
     The pattern is still validated when it's saved.
3. Make release signing optional: use `key.properties` only if it exists, and sign
   debug builds with the default debug key.
4. Pass criterion: `./gradlew assembleDebug` succeeds.

## Phase 2 – update to the latest Android

1. Toolchain: Gradle 9.8, AGP 9.4, JDK 21 toolchain, and compileSdk / targetSdk 37
   (Android 17).
2. Raise minSdk from 21 to **26** (Android 8.0). This allows `java.time` and
   adaptive icons, removes most `SDK_INT` branches, and meets current
   Compose/Glance minimums.
3. Deal with the behavior changes between targetSdk 34 and 37:
   * Remove `WRITE_EXTERNAL_STORAGE` and its runtime request. Backups go to
     `getExternalFilesDir()`, which hasn't needed a permission since API 19.
   * Add a `<queries>` block for launcher activities so the "open another app"
     picker still lists apps under package visibility (API 30+).
   * Make the collection click template `PendingIntent`s `FLAG_MUTABLE`. With
     `FLAG_IMMUTABLE`, the per-item fill-in extras (`beginTime`, `eventid`) are
     dropped on API 31+, so tapping a day or event currently always opens "now".
   * Open the calendar and add events through a no-UI trampoline activity instead
     of `startActivity` from a `BroadcastReceiver`, which background-activity-start
     rules block.
   * Edge-to-edge (enforced from targetSdk 35) is handled by the Compose UI in
     phase 3.
   * Mark both widgets `reconfigurable` (API 31+).
4. Fix existing bugs found along the way:
   * `UpdaterJob` and `WidgetReceiver` refresh only agenda-widget IDs, so month
     widgets never react to calendar or time changes.
   * `TIMEZONE_CHANGED` is registered but never handled.
   * `IcsImporter` upper-cases each whole line, so imported titles, descriptions and
     locations come out in ALL CAPS.
5. Drop the dead `file://` .ics intent filters. File URIs can't be shared since
   API 24; `content://` + `text/calendar` stays.

## Phase 3 – Kotlin and Compose

1. **Kotlin everywhere.** Convert the domain and data code to idiomatic Kotlin:
   `Event`/`Day` data classes, the `Parser` calendar query, `CalendarSet`, backup,
   and a typed `WidgetPrefs` wrapper around the existing `calendarWidget`
   SharedPreferences. **Preference keys and the backup file format stay unchanged**,
   so existing widgets and backups keep working after the update.
2. **Widgets become Jetpack Glance (Compose for app widgets).**
   * Agenda widget: current-date header with add/settings icons, then a
     `LazyColumn` of days with their events. It keeps every appearance option:
     colors, sizes, bold, light font, single line, location, end times,
     today/tomorrow labels, passed events, calendar color bar and icon
     color/alpha.
   * Month widget: header with previous/next and a 7×7 grid (weekday labels + 6
     weeks) with event dots and today/current-month/other-day colors. Paging uses a
     Glance `ActionCallback` instead of `MonthReceiver`.
   * Refresh triggers: the midnight/next-event-end alarm, a calendar
     content-change `JobScheduler` job, time/date/time-zone broadcasts, and saving
     the config. Each one bumps a Glance state key so both widgets reload.
   * The `RemoteViews` layouts, `WidgetService`, `MonthWidgetService` and the XML
     item layouts are deleted.
3. **Config UI becomes Compose + Material 3.**
   * Agenda config: a top bar with the old overflow menu (Backup & Restore, More
     apps, Website) and tabs **Events / Appearance / Settings** over a
     `HorizontalPager`.
   * Month config: a single scrolling screen.
   * New Compose building blocks: color swatch + color picker dialog (ARGB
     sliders, hex field, preview), a date-format field (editable text + preset
     dropdown + live preview), a Compose app picker, and the calendar permission
     request.
   * State lives in a `ViewModel` and is written back to SharedPreferences when
     the screen pauses, then the widget is updated (same as before).
   * Existing string resources and translations are reused.
4. Smaller screens (launcher `Dummy` info dialog, `.ics` importer) move to
   Kotlin/Compose. All Java sources, fragments, `ViewPager` code and XML config
   layouts are removed.
5. JVM unit tests for the pure logic: grouping events into days, month grid
   start, time-label formatting and `CalendarSet`.

## Verification

There's no emulator in this environment (no KVM), so every phase is checked with
`./gradlew assembleDebug lint testDebugUnitTest`, and each phase is committed
separately. Test on a device before releasing: placing both widgets, the
calendar-permission flow, tapping days/events, month paging, and backup/restore.

## Out of scope / dropped

* Wear OS companion. Its source isn't in this repo, and the old Data API it used
  is gone.
* The external `dateFormatSpinner` and `colorpicker` libraries, replaced by
  in-app Compose equivalents.
* Android 5.0–7.1 devices (minSdk 26).
