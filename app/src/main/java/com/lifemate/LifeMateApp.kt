package com.lifemate

import android.app.Application
import androidx.room.Room
import com.lifemate.database.LifeDatabase
import com.lifemate.data.*
import com.lifemate.notifications.ReminderScheduler
import com.lifemate.utils.SecureStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class LifeMateApp : Application() {
    val secure by lazy { SecureStore(this) }
    val db by lazy {
        System.loadLibrary("sqlcipher")
        Room.databaseBuilder(this, LifeDatabase::class.java, "lifemate.db")
            .openHelperFactory(SupportOpenHelperFactory(secure.databaseKey())).addMigrations(com.lifemate.database.MIGRATION_1_2).build()
    }
    val preferences by lazy { PreferenceStore(this) }
    val scheduler by lazy { ReminderScheduler(this, db.dao(), preferences) }
    val repository by lazy { LifeRepository(this, db, scheduler, preferences) }
    override fun onCreate() { super.onCreate(); scheduler.enqueueMaintenance() }
}
