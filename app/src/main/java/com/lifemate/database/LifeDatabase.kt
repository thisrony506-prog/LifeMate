package com.lifemate.database

import androidx.room.*
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
    @Query("DELETE FROM profiles") suspend fun clear()
}
@Database(entities = [Profile::class, LifeItem::class, Completion::class, Attachment::class, Delivery::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LifeDatabase : RoomDatabase() { abstract fun dao(): LifeDao }
