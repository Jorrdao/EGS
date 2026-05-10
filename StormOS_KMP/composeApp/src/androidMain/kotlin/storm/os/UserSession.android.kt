package storm.os

import android.content.Context
import java.util.UUID
import androidx.core.content.edit

actual fun getUserId(): String {
    val prefs = AppConfig.context
        .getSharedPreferences("storm_os_prefs", Context.MODE_PRIVATE)
    val existing = prefs.getString("user_id", null)
    if (existing != null) return existing
    val newId = UUID.randomUUID().toString()
    prefs.edit { putString("user_id", newId) }
    return newId
}
