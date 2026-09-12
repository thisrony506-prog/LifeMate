package com.lifemate

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import com.lifemate.ui.LifeRoot

class MainActivity : FragmentActivity() {
    val flutterMode = mutableStateOf(!BuildConfig.DEBUG)
    var pendingFlutterRecord: String? = null
    fun refreshFlutter() { bridge?.refresh() }
    val nativeRoute = mutableStateOf<String?>(null)
    private var bridge: com.lifemate.flutter.FlutterBridge? = null
    fun lifeFlutter() = bridge ?: com.lifemate.flutter.FlutterBridge(this).also { bridge=it }
    override fun onDestroy() { super.onDestroy();bridge?.destroy();bridge=null }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code,permissions,results)
        bridge?.refresh()
        supportFragmentManager.fragments.filterIsInstance<io.flutter.embedding.android.FlutterFragment>().forEach { it.onRequestPermissionsResult(code,permissions,results) }
    }
    @Deprecated("Activity results are forwarded to Flutter plugins")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        supportFragmentManager.fragments.filterIsInstance<io.flutter.embedding.android.FlutterFragment>().forEach { it.onActivityResult(requestCode,resultCode,data) }
    }
    val openItem = mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        // A restored cached-engine Fragment may attach during super.onCreate.
        // Ensure its engine exists before FragmentManager restores it.
        if(savedInstanceState?.getBoolean("life_flutter") == true || intent.getBooleanExtra("flutter",!BuildConfig.DEBUG)) lifeFlutter()
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        flutterMode.value = intent.getBooleanExtra("flutter", !BuildConfig.DEBUG)
        openItem.value = validItemId(intent)
        setContent { LifeRoot(this) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("life_flutter",bridge!=null)
        super.onSaveInstanceState(outState)
    }
    private fun validItemId(intent: Intent): String? = intent.getStringExtra("itemId")?.let {
        runCatching { java.util.UUID.fromString(it).toString() }.getOrNull()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); openItem.value = validItemId(intent) }
}
