# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ----- Jetpack Compose -----
# Keep Compose runtime and tooling internals to avoid runtime crashes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose UI tooling annotations (used at runtime for previews/tests)
-keepattributes RuntimeVisibleAnnotations

# ----- Kotlin / Coroutines -----
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }
-dontwarn kotlin.coroutines.**

# Keep Kotlin metadata (required for reflection and serialization)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ----- AgentConsole App Classes -----
# Keep all app classes (avoid stripping TermuxRunner, ExecutionStore, etc.)
-keep class com.example.agentconsole.** { *; }
-keepclassmembers class com.example.agentconsole.** { *; }

# ----- Termux -----
# Keep Termux shared library classes (IPC and service interfaces)
-keep class com.termux.** { *; }
-keepclassmembers class com.termux.** { *; }
-dontwarn com.termux.**

# ----- Android Service / BroadcastReceiver (IPC) -----
# Keep components referenced via Intent / Binder
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Activity
-keep public class * extends android.content.ContentProvider

# ----- Guava ListenableFuture stub -----
-dontwarn com.google.common.util.concurrent.ListenableFuture
