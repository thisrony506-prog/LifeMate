package com.lifemate

import android.app.Application
import com.lifemate.reset.FreshStartReset

/** No legacy database, profile, posts, workers or native feature screens. */
class LifeMateApp : Application() {
    var resetPerformed = false
        private set
    fun consumeResetFlag(): Boolean {
        val value = resetPerformed
        resetPerformed = false
        return value
    }
    override fun onCreate() {
        super.onCreate()
        resetPerformed = FreshStartReset.run(this)
    }
}
