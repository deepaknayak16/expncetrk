# SQLCipher keep rules (Zetetic SQLCipher for Android)
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }

# Legacy SQLCipher package
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Kotlinx Serialization
-keepattributes Annotation, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class com.example.expncetracker.exptkr.domain.model.** {
    *** Companion;
    *** serializer(...);
}
-keep,allowobfuscation,allowoptimization class com.example.expncetracker.exptkr.domain.model.** { *; }

# Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
