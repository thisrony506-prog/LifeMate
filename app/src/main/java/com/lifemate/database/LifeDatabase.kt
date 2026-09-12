package com.lifemate.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeDao {
    @Query("SELECT * FROM profiles WHERE id=1") fun profile(): Flow<Profile?>
    @Query("SELECT * FROM profiles WHERE id=1") suspend fun getProfile(): Profile?
    @Upsert suspend fun saveProfile(profile: Profile)
    @Query("SELECT * FROM items ORDER BY pinned DESC, updatedAt DESC") fun items(): Flow<List<LifeItem>>
    @Query("SELECT * FROM items") suspend fun allItems(): List<LifeItem>
    @Query("SELECT * FROM items WHERE id=:id") suspend fun get(id: String): LifeItem?
    @Upsert suspend fun save(item: LifeItem)
    @Query("DELETE FROM items WHERE id=:id") suspend fun delete(id: String)
    @Query("SELECT * FROM completions") fun completions(): Flow<List<Completion>>
    @Query("SELECT date FROM completions WHERE itemId=:id") suspend fun completedDates(id: String): List<String>
    @Query("SELECT * FROM completions") suspend fun allCompletions(): List<Completion>
    @Query("SELECT EXISTS(SELECT 1 FROM completions WHERE itemId=:id AND date=:date)") suspend fun isDone(id: String, date: String): Boolean
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun complete(completion: Completion)
    @Query("DELETE FROM completions WHERE itemId=:id AND date=:date") suspend fun uncomplete(id: String, date: String)
    @Query("SELECT * FROM attachments") fun attachments(): Flow<List<Attachment>>
    @Query("SELECT * FROM attachments") suspend fun allAttachments(): List<Attachment>
    @Upsert suspend fun saveAttachment(attachment: Attachment)
    @Query("DELETE FROM attachments WHERE id=:id") suspend fun deleteAttachment(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun recordDelivery(delivery: Delivery): Long
    @Query("DELETE FROM deliveries WHERE occurrence < :before") suspend fun pruneDeliveries(before: Long)
    @Query("SELECT * FROM scheduled_alarms WHERE itemId=:id") suspend fun getAlarm(id: String): ScheduledAlarm?
    @Upsert suspend fun saveAlarm(alarm: ScheduledAlarm)
    @Query("DELETE FROM scheduled_alarms WHERE itemId=:id") suspend fun deleteAlarm(id: String)
    @Query("SELECT * FROM social_posts ORDER BY updatedAt DESC") fun posts(): Flow<List<SocialPost>>
    @Query("SELECT * FROM social_posts ORDER BY updatedAt DESC") suspend fun allPosts(): List<SocialPost>
    @Query("SELECT * FROM social_posts WHERE id=:id") suspend fun getPost(id: String): SocialPost?
    @Upsert suspend fun savePost(post: SocialPost)
    @Query("DELETE FROM social_posts WHERE id=:id") suspend fun deletePost(id: String)
    @Query("SELECT * FROM attachments WHERE itemId=:id") suspend fun attachmentsFor(id: String): List<Attachment>
    @Query("DELETE FROM profiles") suspend fun clear()
}
@Database(entities = [Profile::class, LifeItem::class, Completion::class, Attachment::class, Delivery::class, ScheduledAlarm::class, SocialPost::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LifeDatabase : RoomDatabase() { abstract fun dao(): LifeDao }

/** Additive migration: no existing table, key or record is recreated. */
val MIGRATION_1_2 = object : Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `social_posts` (`id` TEXT NOT NULL, `profileId` INTEGER NOT NULL, `type` TEXT NOT NULL, `topic` TEXT NOT NULL, `message` TEXT NOT NULL, `person` TEXT NOT NULL, `date` TEXT NOT NULL, `caption` TEXT NOT NULL, `photo` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_posts_profileId` ON `social_posts` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_posts_updatedAt` ON `social_posts` (`updatedAt`)")
    }
}
