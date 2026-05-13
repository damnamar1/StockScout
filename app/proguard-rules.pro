# StockScout ProGuard rules

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep domain models (used by Gson/Room reflection)
-keep class com.example.stockscout.domain.model.** { *; }
-keep class com.example.stockscout.data.remote.dto.** { *; }
-keep class com.example.stockscout.data.local.entity.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Navigation Safe Args
-keepnames class * implements android.os.Parcelable
-keepclassmembers class ** implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-dontwarn androidx.camera.**
