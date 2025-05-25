# Add project specific ProGuard rules here.
# By default, the flags in R8 will keep all classes in the main source set
# Can be used for R8, see https://developer.android.com/build/shrink-code
# Add any project specific keep rules here.

# If you use vertically integrated libraries (e.g. Retrofit, GSON, ...) you may
# need to add specific rules to prevent code shrinking.
# Keeping generic classes for GSON
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.examples.android.model.** { *; }

# Keeping generic classes for Retrofit
-keep interface retrofit2.Call
-keep class retrofit2.Callback
-keep class retrofit2.Response
-keep class retrofit2.http.**

# For using GSON with Kotlin, if you use @SerializedName annotation
-keepattributes Signature
-keepattributes InnerClasses

# If you use Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
