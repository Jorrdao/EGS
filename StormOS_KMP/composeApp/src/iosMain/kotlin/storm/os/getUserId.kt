package storm.os

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID

actual fun getUserId(): String {
    val prefs = NSUserDefaults.standardUserDefaults
    val existing = prefs.stringForKey("user_id")
    if (existing != null) return existing
    val newId = NSUUID().UUIDString
    prefs.setObject(newId, "user_id")
    return newId
}