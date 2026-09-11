package com.lifemate

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.database.*
import com.lifemate.utils.SecureStore
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(): LifeDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, LifeDatabase::class.java, "test-persistence.db")
            .openHelperFactory(SupportOpenHelperFactory(SecureStore(context).databaseKey())).build()
    }
    @After fun cleanup() { context.deleteDatabase("test-persistence.db") }
    @Test fun recordsSurviveCloseAndReopenAndCascadeDelete() = runBlocking {
        var db = open()
        db.dao().saveProfile(Profile(fullName = "Persistence Test"))
        val item = LifeItem(title = "Read", duration = 7)
        db.dao().save(item); db.dao().complete(Completion(item.id, "2026-09-11"))
        db.dao().saveAttachment(Attachment(itemId = item.id, path = "local", mime = "image/jpeg", name = "Photo"))
        db.close(); db = open()
        assertEquals("Read", db.dao().get(item.id)?.title)
        assertTrue(db.dao().isDone(item.id, "2026-09-11"))
        assertEquals("Persistence Test", db.dao().getProfile()?.fullName)
        db.dao().complete(Completion(item.id, "2026-09-11")); assertEquals(1, db.dao().allCompletions().size)
        db.dao().delete(item.id); assertTrue(db.dao().allCompletions().isEmpty()); assertTrue(db.dao().allAttachments().isEmpty())
        db.close()
    }
    @Test fun duplicateDeliveryIsRejected() = runBlocking {
        val db = open(); db.dao().saveProfile(Profile(fullName = "Test")); val item = LifeItem(title = "Alarm"); db.dao().save(item)
        assertTrue(db.dao().recordDelivery(Delivery(item.id, 1234)) != -1L)
        assertEquals(-1L, db.dao().recordDelivery(Delivery(item.id, 1234)))
        db.close()
    }
}
