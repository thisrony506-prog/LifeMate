-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }

# Flutter discovers its generated plugin registrant reflectively.
-keep class io.flutter.plugins.GeneratedPluginRegistrant { *; }
