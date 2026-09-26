# Simple Calendar Widget

[![Build](https://github.com/j4velin/SimpleCalendarWidget/actions/workflows/build.yml/badge.svg)](https://github.com/j4velin/SimpleCalendarWidget/actions/workflows/build.yml)

Home screen widgets for Android that show the events from your device's calendars. The app has
no main screen: you add a widget and configure it.

## Widgets

* **SCW Agenda**: a list of your upcoming events.
  * Choose which calendars to show and how far ahead to look.
  * Set the color, size and weight of the date, time, event name and location. Today's events
    and events that have already passed today can be styled separately.
  * Custom date and time formats, "Today"/"Tomorrow" labels, and optional end times, locations
    and calendar colors.
  * Show events that span several days only once, or on every day.
  * Choose what a tap does: open the default calendar app, open another app, or refresh the
    widget.
* **SCW Month**: a month grid with a dot for each event on a day.
  * Colors for the current month, other days, labels and the background.
  * Start the week on Monday, and page between months.
  * Tap a day to open it in your calendar. If a day has only one event, a tap can open that
    event directly.

Both widgets can be reconfigured after they are placed, and widget settings can be backed up and
restored. The app can also import `.ics` files: opening one hands its event to your calendar app.

The app is translated into 17 languages.

## Requirements

* Android 8.0 (API 26) or later
* Permission to read your calendar (`READ_CALENDAR`)

## Building

You need JDK 21. The Gradle wrapper downloads everything else.

```sh
./gradlew assembleDebug
```

The APK is written to `build/outputs/apk/debug/`.

To run the same checks as CI (build, lint and unit tests):

```sh
./gradlew assembleDebug assembleRelease lintDebug testDebugUnitTest
```

### Release signing

Release builds are signed only if a `key.properties` file exists in the project root. Without it,
`assembleRelease` produces an unsigned APK.

```properties
keyStore=keys.keystore
keyStorePassword=...
keyAlias=...
keyAliasPassword=...
```

`keyStore` is resolved relative to the project root. Don't commit this file or the keystore.

## Project structure

It's a single-module project written in Kotlin.

| Path | Contents |
|---|---|
| `src/main/kotlin/.../widget` | The widgets, built with Jetpack Glance |
| `src/main/kotlin/.../settings` | The configuration screens, built with Compose and Material 3 |
| `src/main/kotlin/.../data` | Calendar queries, agenda and month grid logic, settings, backup |
| `src/test` | JVM and Robolectric tests |
