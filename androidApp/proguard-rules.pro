# Firebase Firestore - Keep DTOs for deserialization
-keepclassmembers class com.eunio.healthapp.data.remote.dto.** {
    <init>();
    <fields>;
}

# Keep all DTO classes used with Firebase
-keep class com.eunio.healthapp.data.remote.dto.** { *; }

# Firebase SDK rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.eunio.healthapp.**$$serializer { *; }
-keepclassmembers class com.eunio.healthapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.eunio.healthapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
