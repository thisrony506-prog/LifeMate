package com.lifemate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import com.lifemate.updates.*
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*

/** Flutter is the ONLY application UI. Kotlin is limited to Android update safety. */
class MainActivity : FlutterFragmentActivity() {
    private var firstFrameReported = false
    private val activityStarted = android.os.SystemClock.elapsedRealtime()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val updates by lazy { UpdateRepository(this) }
    private val downloader by lazy { ApkUpdateDownloader(this) }
    private lateinit var channel: MethodChannel
    private var transfer: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(if ((application as LifeMateApp).consumeResetFlag()) null else savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.lifemate/personal_os")
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "performance.ready" -> {
                    if (!firstFrameReported) {
                        firstFrameReported = true
                        reportFullyDrawn()
                        android.util.Log.i("LifeMatePerf", "activity_to_flutter_frame_ms=${android.os.SystemClock.elapsedRealtime()-activityStarted}")
                    }
                    result.success(true)
                }
                "updates.state" -> result.success(state())
                "updates.check" -> scope.launch {
                    try { if (call.argument<Boolean>("manual") == true || updates.due()) updates.fetch(); result.success(state()) }
                    catch (e: CancellationException) { result.error("cancelled", "Check interrupted", null) }
                    catch (_: Exception) { result.error("unavailable", "Update check failed; any known requirement remains active", state()) }
                }
                "updates.download" -> {
                    if (transfer?.isActive == true) result.error("busy", "Already downloading", null)
                    else transfer = scope.launch {
                        try {
                            val release = updates.cached()?.takeIf { it.newerThan(BuildConfig.VERSION_CODE) } ?: error("No newer signed update")
                            downloader.download(release) { progress -> scope.launch {
                                channel.invokeMethod("updates.progress", mapOf("phase" to progress.phase.name, "received" to progress.received, "total" to progress.total))
                            } }
                            result.success(true)
                        } catch (e: CancellationException) { result.error("cancelled", "Download paused; retry in the foreground", null) }
                        catch (_: Exception) { result.error("download", "Download or verification failed; retry", null) }
                    }
                }
                "updates.install" -> scope.launch {
                    try {
                        val release = updates.cached()?.takeIf { it.newerThan(BuildConfig.VERSION_CODE) } ?: error("No required update")
                        val file = downloader.verifyReady(release)
                        if (!packageManager.canRequestPackageInstalls()) {
                            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                            result.success("permission")
                        } else { startActivity(nativeUpdateIntent(this@MainActivity, file)); result.success("installer") }
                    } catch (_: Exception) { result.error("install", "Could not open the verified update", null) }
                }
                "close" -> { result.success(true); finishAffinity() }
                else -> result.notImplemented()
            }
        }
    }
    private fun state(): Map<String, Any?> {
        val release = updates.cached()
        return mapOf("installed" to BuildConfig.VERSION_NAME, "required" to (release?.newerThan(BuildConfig.VERSION_CODE) == true), "version" to release?.version, "bytes" to release?.bytes)
    }
    override fun onStop() { transfer?.cancel(); super.onStop() }
    override fun onDestroy() { if (::channel.isInitialized) channel.setMethodCallHandler(null); scope.cancel(); super.onDestroy() }
}
