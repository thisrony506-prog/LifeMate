# Flutter discovers its plugin registrant reflectively.
-keep class io.flutter.plugins.GeneratedPluginRegistrant { *; }
# SQLCipher uses JNI/reflection for database/cursor internals. This is the same
# compatibility rule used by the existing signed native app; debug-only tests
# cannot detect its removal. Retain it for read-only existing-account adoption.
-keep class net.zetetic.database.sqlcipher.** { *; }
# EncryptedSharedPreferences/Tink and preferences DataStore deserialize generated
# protobuf fields reflectively. Keep these internals in the optimized host too.
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { <fields>; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
