package storm.os

/**
 * Returns a persistent unique ID for this device/user.
 * Android: stored in SharedPreferences (survives app restarts)
 * iOS:     stored in NSUserDefaults
 *
 * This is Option A — a device-generated UUID used until real auth is added.
 * To switch to real auth later, replace both actuals with a token lookup.
 */
expect fun getUserId(): String