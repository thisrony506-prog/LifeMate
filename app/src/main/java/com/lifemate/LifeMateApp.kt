package com.lifemate

import android.app.Application
import com.lifemate.continuity.ExistingUser

/** Keep the same application identity and all existing private storage/keys.
 * The new UI is Flutter; application startup never resets user data.
 */
class LifeMateApp : Application() {
    val existingUser by lazy { ExistingUser(this) }
}
