# Crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keep @javax.inject.Inject class *
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class ** { *; }

# Protobuf (DataStore)
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.CoroutineWorker

# Android components declared in manifest
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.accessibilityservice.AccessibilityService
-keep class * extends android.app.admin.DeviceAdminReceiver

# Glance widgets
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Timber / annotations
-dontwarn org.jetbrains.annotations.**
