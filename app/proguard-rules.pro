# Ondy R8 rules
#
# Most libraries (Hilt, Room, Compose, Coil, Coroutines) ship their own consumer
# ProGuard rules, so only app-specific keep rules are required here.

# Room entities and DAOs are accessed reflectively by the generated implementation.
-keep class com.ondy.app.data.local.entity.** { *; }
-keep class com.ondy.app.data.local.dao.** { *; }

# Hilt entry points and generated components.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentBuilder { *; }
-keep class * extends dagger.hilt.android.EntryPoint { *; }

# Keep enum values (used by Room type converters and Kotlin serialization).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations.
-keep class * implements android.os.Parcelable { *; }

# Keep Kotlin metadata used by reflection.
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Coil / coroutines optional dependencies.
-dontwarn coil.**
-dontwarn kotlinx.coroutines.**
