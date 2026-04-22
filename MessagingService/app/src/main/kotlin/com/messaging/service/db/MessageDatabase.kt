package com.messaging.service.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.messaging.service.online.models.ContentType
import com.messaging.service.online.models.DeliveryStatus
import com.messaging.service.online.models.Message
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Entity
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")    val messageId: String,
    @ColumnInfo(name = "chat_id")       val chatId: String,
    @ColumnInfo(name = "sender_id")     val senderId: String,
    @ColumnInfo(name = "recipient_id")  val recipientId: String,
    @ColumnInfo(name = "content")       val content: String,
    @ColumnInfo(name = "content_type")  val contentType: String = "text",
    @ColumnInfo(name = "timestamp")     val timestamp: Long,
    @ColumnInfo(name = "delivered")     val delivered: Boolean = false,
    @ColumnInfo(name = "read_flag")     val read: Boolean = false,
    @ColumnInfo(name = "source")        val source: String = "online" // "online" | "ble"
) {
    fun toOnlineMessage() = Message(
        messageId   = messageId,
        senderId    = senderId,
        recipientId = recipientId,
        content     = content,
        contentType = ContentType.TEXT,
        timestamp   = timestamp,
        delivered   = delivered,
        read        = read
    )

    companion object {
        fun fromOnlineSend(
            chatId: String,
            recipientId: String,
            content: String,
            messageId: String
        ) = MessageEntity(
            messageId   = messageId,
            chatId      = chatId,
            senderId    = "self",
            recipientId = recipientId,
            content     = content,
            timestamp   = System.currentTimeMillis(),
            delivered   = false,
            source      = "online"
        )

        fun fromRemoteMessage(chatId: String, msg: Message) = MessageEntity(
            messageId   = msg.messageId,
            chatId      = chatId,
            senderId    = msg.senderId,
            recipientId = msg.recipientId,
            content     = msg.content,
            contentType = msg.contentType.name.lowercase(),
            timestamp   = msg.timestamp,
            delivered   = msg.delivered,
            read        = msg.read,
            source      = "online"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: MessageEntity)

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC")
    fun observeByChatId(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByChatId(chatId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// Database
// ─────────────────────────────────────────────────────────────────────────────

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var instance: MessageDatabase? = null

        fun getInstance(context: Context): MessageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "messages.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
