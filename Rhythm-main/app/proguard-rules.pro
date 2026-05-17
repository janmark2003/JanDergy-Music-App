# Add project specific ProGuard rules here.

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ──────────────────────────────
# Kotlin
# ──────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ──────────────────────────────
# Compose / Material Icons
# R8 tree-shakes unused icon objects automatically when minification is on;
# these rules only protect the runtime infrastructure.
# ──────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ──────────────────────────────
# AndroidX / Lifecycle / Navigation
# ──────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# ──────────────────────────────
# Media3 / ExoPlayer
# ──────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class org.jellyfin.media3.** { *; }
-dontwarn org.jellyfin.media3.**

# ──────────────────────────────
# Retrofit / OkHttp / Gson
# ──────────────────────────────
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
# Keep data-model classes used by Gson reflection
-keep class chromahub.rhythm.app.** { *; }

# ──────────────────────────────
# Coil
# ──────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ──────────────────────────────
# jaudiotagger (audio metadata)
# ──────────────────────────────
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# ──────────────────────────────
# Ktor
# ──────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# ──────────────────────────────
# Glance / Widgets
# ──────────────────────────────
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# ──────────────────────────────
# Accompanist
# ──────────────────────────────
-dontwarn com.google.accompanist.**

# ──────────────────────────────
# Misc
# ──────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**