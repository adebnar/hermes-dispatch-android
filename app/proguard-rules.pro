# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers,allowshrinking class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class co.hermesdispatch.app.**$$serializer { *; }
-keepclassmembers class co.hermesdispatch.app.** {
    *** Companion;
}
-keepclasseswithmembers class co.hermesdispatch.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn okhttp3.internal.platform.**
-dontwarn io.ktor.**
