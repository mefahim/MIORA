# ProGuard / R8 Rules for MIORA (com.fhf.miora)

# Kotlin Coroutines
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# AndroidX Media3 (ExoPlayer & MediaSession)
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# Domain Models & Serialization Data Classes
-keep class com.fahim.powerampclone.model.** { *; }
-keep class com.fahim.powerampclone.update.** { *; }

# Android FileProvider
-keep class androidx.core.content.FileProvider { *; }

# SQLite & Data Persistence
-keep class com.fahim.powerampclone.data.db.** { *; }

# Jetpack Compose UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

