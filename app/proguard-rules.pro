# Compose: rules included automatically
# Hilt: rules included via plugin
# Room: rules included automatically

# Moshi
-keepattributes Signature
-keep class kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoaderImpl
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class **JsonAdapter { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class ru.homebuhg.**$$serializer { *; }
-keepclassmembers class ru.homebuhg.** {
    *** Companion;
}
-keepclasseswithmembers class ru.homebuhg.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }

# Firebase (no-op if absent)
-keep class com.google.firebase.** { *; }
