# OBD Insight ProGuard Rules

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes for serialization
-keep class com.obd.insight.domain.model.** { *; }
-keep class com.obd.insight.data.elm327.** { *; }
