# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wavelength.**$$serializer { *; }
-keepclassmembers class com.wavelength.** { *** Companion; }
-keepclasseswithmembers class com.wavelength.** { kotlinx.serialization.KSerializer serializer(...); }
