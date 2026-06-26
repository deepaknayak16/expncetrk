# SQLCipher keep rules (Zetetic SQLCipher for Android)
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { *; }

# Legacy SQLCipher package
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
