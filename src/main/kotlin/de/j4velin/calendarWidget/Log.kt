package de.j4velin.calendarWidget

import android.util.Log

private const val TAG = "SimpleCalendarWidget"

/** Debug logging, a no-op in release builds */
internal fun log(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}

internal fun log(throwable: Throwable) {
    if (BuildConfig.DEBUG) Log.d(TAG, throwable.message, throwable)
}
