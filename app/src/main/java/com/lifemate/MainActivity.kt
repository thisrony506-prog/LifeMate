package com.lifemate

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import com.lifemate.ui.LifeRoot

class MainActivity : FragmentActivity() {
    val openItem = mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        openItem.value = intent.getStringExtra("itemId")
        setContent { LifeRoot(this) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); openItem.value = intent.getStringExtra("itemId") }
}
