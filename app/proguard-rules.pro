# Flutter discovers its plugin registrant reflectively.
-keep class io.flutter.plugins.GeneratedPluginRegistrant { *; }
# SQLCipher uses JNI/reflection for database/cursor internals. This is the same
# compatibility rule used by the existing signed native app; debug-only tests
# cannot detect its removal. Retain it for read-only existing-account adoption.
-keep class net.zetetic.database.sqlcipher.** { *; }
