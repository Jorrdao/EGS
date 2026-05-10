# ── Protobuf lite ─────────────────────────────────────────────────────────────
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── AIDL / Binder interfaces ──────────────────────────────────────────────────
-keep class com.messaging.service.IMessagingService { *; }
-keep class com.messaging.service.IMessagingService$** { *; }
-keep class com.messaging.service.IMessagingCallback { *; }

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# ── Gson / models ─────────────────────────────────────────────────────────────
-keep class com.messaging.service.online.models.** { *; }

# ── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
