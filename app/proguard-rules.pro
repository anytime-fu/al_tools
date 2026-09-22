# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Retrofit interfaces
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }

# App models
-keep class com.toolbox.data.local.entity.** { *; }

# PDFBox
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ZXing
-keep class com.google.zxing.** { *; }

# Accompanist
-keep class com.google.accompanist.** { *; }

# SceneView / ARCore
-keep class io.github.sceneview.** { *; }
-dontwarn io.github.sceneview.**
-keep class com.google.ar.** { *; }
-dontwarn com.google.ar.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
