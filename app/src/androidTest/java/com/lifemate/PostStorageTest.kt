package com.lifemate

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.lifemate.database.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class PostStorageTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @After fun clear() {context.deleteDatabase("post-migration.db")}
    @Test fun versionOneMigratesWithoutLosingProfileAndPostsPersist(): Unit=runBlocking {
        val name="post-migration.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        val legacy=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null)
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.lifemate.database.LifeDatabase/1.json").bufferedReader().readText()).getJSONObject("database")
        val entities=schema.getJSONArray("entities")
        for(i in 0 until entities.length()) {
            val entity=entities.getJSONObject(i)
            legacy.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
            val indices=entity.getJSONArray("indices")
            for(j in 0 until indices.length()) legacy.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
        }
        val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length()) legacy.execSQL(setup.getString(i))
        legacy.execSQL("INSERT INTO profiles (id,fullName,nickname,preferredName,birthday,photo,introduction,information) VALUES (1,'Keep me','','','','','','private')")
        legacy.version=1;legacy.close()
        var db=Room.databaseBuilder(context,LifeDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        assertEquals("private",db.dao().getProfile()!!.information)
        val post=SocialPost(topic="First post",caption="Local only")
        db.dao().savePost(post);db.close()
        db=Room.databaseBuilder(context,LifeDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        assertEquals("Local only",db.dao().getPost(post.id)!!.caption)
        db.dao().savePost(post.copy(caption="Edited"));assertEquals("Edited",db.dao().getPost(post.id)!!.caption)
        db.dao().deletePost(post.id);assertNull(db.dao().getPost(post.id))
        db.dao().savePost(post);db.dao().clear();assertTrue(db.dao().allPosts().isEmpty());db.close()
    }
}
