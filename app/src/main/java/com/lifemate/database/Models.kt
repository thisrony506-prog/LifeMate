package com.lifemate.database

import androidx.room.*
import com.lifemate.domain.*
import java.time.*
import java.util.UUID

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "", val nickname: String = "", val preferredName: String = "",
    val birthday: String = "", val photo: String = "", val introduction: String = "", val information: String = ""
) : java.io.Serializable { val displayName: String get() = preferredName.ifBlank { nickname.ifBlank { fullName.substringBefore(" ") } }.ifBlank { "friend" } }

@Entity(tableName = "items", foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)], indices = [Index("profileId"), Index("kind"), Index("date"), Index("archived")])
data class LifeItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), val profileId: Int = 1,
    val kind: Kind = Kind.ROUTINE, val title: String = "", val description: String = "",
    val date: String = LocalDate.now().toString(), val time: String = "09:00", val repeat: Repeat = Repeat.ONCE,
    val weekdays: String = "1,2,3,4,5", val interval: Int = 1, val duration: Int = 30,
    val dailyTarget: String = "", val progress: Int = 0, val checklist: String = "[]",
    val notes: String = "", val tags: String = "", val relationship: String = "Friend", val nickname: String = "",
    val birthdayOffsets: String = "7,3,1,0", val notifications: Boolean = true,
    val notificationText: String = "", val sound: String = "default", val vibration: Boolean = true,
    val important: Boolean = false, val pinned: Boolean = false, val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis()
) : java.io.Serializable {
    fun spec() = ScheduleSpec(kind, LocalDate.parse(date), LocalTime.parse(time), repeat,
        weekdays.split(",").mapNotNull { it.toIntOrNull() }.toSet(), interval, duration,
        birthdayOffsets.split(",").mapNotNull { it.toIntOrNull() }.toSet())
    fun occurs(day: LocalDate) = !archived && Schedule.occurs(spec(), day)
}
@Entity(tableName = "completions", primaryKeys = ["itemId", "date"], foreignKeys = [ForeignKey(entity = LifeItem::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)], indices = [Index("date")])
data class Completion(val itemId: String, val date: String, val completedAt: Long = System.currentTimeMillis())

@Entity(tableName = "attachments", foreignKeys = [ForeignKey(entity = LifeItem::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)], indices = [Index("itemId")])
data class Attachment(@PrimaryKey val id: String = UUID.randomUUID().toString(), val itemId: String, val path: String, val mime: String, val name: String)
@Entity(tableName = "deliveries", primaryKeys = ["itemId", "occurrence"], foreignKeys = [ForeignKey(entity = LifeItem::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)])
data class Delivery(val itemId: String, val occurrence: Long)
class Converters {
    @TypeConverter fun kind(value: String) = Kind.valueOf(value)
    @TypeConverter fun kind(value: Kind) = value.name
    @TypeConverter fun repeat(value: String) = Repeat.valueOf(value)
    @TypeConverter fun repeat(value: Repeat) = value.name
}
