# Preserve Gson model serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflection.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve OkHttp classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Preserve Kotlin Coroutines and internal JVM metadata (Fixes SpillingKt missing class error)
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-dontwarn kotlin.coroutines.jvm.internal.**

# Keep standard views, layouts, and dynamic custom view parameters
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
