package com.lifemate.flutter

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.lifecycleScope
import com.lifemate.*
import com.lifemate.database.*
import com.lifemate.domain.*
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Flutter is inside the existing native PIN/update gate, never a second launcher. */
class FlutterBridge(private val activity: MainActivity) {
    private val app get() = activity.application as LifeMateApp
    val engine = FlutterEngine(activity.applicationContext)
    private val channel = MethodChannel(engine.dartExecutor.binaryMessenger, "com.lifemate/personal_os")
    init {
        channel.setMethodCallHandler { call, result ->
            activity.lifecycleScope.launch {
                try {
                    val args = call.arguments as? Map<*,*> ?: emptyMap<Any,Any>()
                    when (call.method) {
                        "snapshot" -> result.success(withContext(Dispatchers.IO) {
                            val profile=app.db.dao().getProfile()
                            val pending=activity.pendingFlutterRecord;activity.pendingFlutterRecord=null
                            mapOf("openId" to pending,"name" to (profile?.displayName ?: ""), "hasPin" to app.secure.hasPin(),
                                "theme" to app.preferences.flow.first().theme,
                                "notificationsAllowed" to androidx.core.app.NotificationManagerCompat.from(activity).areNotificationsEnabled(),
                                "items" to app.db.dao().allItems().filter { !it.archived && it.tags != "life-mate-hive" }.map { mapOf("id" to it.id,"title" to it.title,"kind" to it.kind.name,"date" to it.date) })
                        })
                        "profile" -> {
                            val name=args["name"]?.toString()?.trim().orEmpty()
                            require(name.isNotBlank() && name.length<=60)
                            withContext(Dispatchers.IO) { if(app.db.dao().getProfile()==null) app.db.dao().saveProfile(Profile(fullName=name)) }
                            result.success(true)
                        }
                        "theme" -> {val value=args["value"]?.toString().orEmpty(); require(value in setOf("System","Light","Dark")); app.preferences.set("theme",value);result.success(true)}
                        "openLegacy" -> {
                            val route=args["route"]?.toString().orEmpty()
                            require(route in setOf("home","menu","settings","updates","edit-profile","notifications") || route.matches(Regex("detail/[a-f0-9-]{36}")))
                            result.success(true);activity.nativeRoute.value=route;activity.flutterMode.value=false
                        }
                        "schedule" -> {
                            val id=UUID.fromString(args["id"].toString()).toString()
                            val old=withContext(Dispatchers.IO){app.db.dao().get(id)}
                            require(old==null || old.tags=="life-mate-hive")
                            val title=args["title"].toString();require(title.isNotBlank() && title.length<=200)
                            val date=LocalDate.parse(args["date"].toString());val time=LocalTime.parse(args["time"].toString())
                            val repeat=Repeat.valueOf(args["repeat"].toString());require(repeat in setOf(Repeat.ONCE,Repeat.DAILY,Repeat.MONTHLY,Repeat.YEARLY))
                            app.repository.save(LifeItem(id=id,kind=Kind.REMINDER,title=title,date=date.toString(),time=time.toString(),repeat=repeat,tags="life-mate-hive"))
                            val allowed=androidx.core.app.NotificationManagerCompat.from(activity).areNotificationsEnabled()
                            if(!allowed && android.os.Build.VERSION.SDK_INT>=33) androidx.core.app.ActivityCompat.requestPermissions(activity,arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),7341)
                            if(allowed)result.success(true) else result.error("permission","Enable notification permission in Life Mate settings. The record was saved.",null)
                        }
                        "cancel" -> {
                            val id=UUID.fromString(args["id"].toString()).toString()
                            val item=withContext(Dispatchers.IO){app.db.dao().get(id)}
                            if(item!=null){require(item.tags=="life-mate-hive");app.repository.delete(item)}
                            result.success(true)
                        }
                        else -> result.notImplemented()
                    }
                } catch (_: Exception) { result.error("native_action","The operation could not be completed. Existing data was not replaced.",null) }
            }
        }
        FlutterEngineCache.getInstance().put("life-mate",engine)
        app.clearFlutterData = { withContext(Dispatchers.Main) {
            withTimeout(10000) { suspendCancellableCoroutine<Unit> { continuation ->
                channel.invokeMethod("erase",null,object:MethodChannel.Result {
                    override fun success(result: Any?) { if(continuation.isActive)continuation.resume(Unit){} }
                    override fun error(code:String,message:String?,details:Any?) { if(continuation.isActive)continuation.cancel(IllegalStateException("Flutter vault could not be cleared")) }
                    override fun notImplemented() { if(continuation.isActive)continuation.cancel(IllegalStateException("Flutter vault is unavailable")) }
                })
            } }
        } }
        engine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())
    }
    fun refresh() { channel.invokeMethod("refresh",null) }
    fun destroy() { app.clearFlutterData=null;channel.setMethodCallHandler(null);FlutterEngineCache.getInstance().remove("life-mate");engine.destroy() }
}

@Composable fun FlutterHome(activity: MainActivity) {
    val bridge=remember { activity.lifeFlutter() }
    val arguments=remember { FlutterFragment.withCachedEngine("life-mate").shouldAttachEngineToActivity(true).shouldAutomaticallyHandleOnBackPressed(true).build<FlutterFragment>().arguments!! }
    AndroidFragment<FlutterFragment>(modifier=Modifier.fillMaxSize(),arguments=arguments,onUpdate={ bridge.refresh() })
}
