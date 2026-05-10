package com.messaging.service.db

import android.content.Context
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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messaging.service.online.models.ContentType
import com.messaging.service.online.models.Message
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")   val messageId: String,
    @ColumnInfo(name = "user_id")      val userId: String,
    @ColumnInfo(name = "chat_id")      val chatId: String,
    @ColumnInfo(name = "sender_id")    val senderId: String,
    @ColumnInfo(name = "recipient_id") val recipientId: String,
    @ColumnInfo(name = "content")      val content: String,
    @ColumnInfo(name = "content_type") val contentType: String = "text",
    @ColumnInfo(name = "timestamp")    val timestamp: Long,
    @ColumnInfo(name = "delivered")    val delivered: Boolean = false,
    @ColumnInfo(name = "read_flag")    val read: Boolean = false,
    @ColumnInfo(name = "source")       val source: String = "online"
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
            userId: String,
            chatId: String,
            recipientId: String,
            content: String,
            messageId: String
        ) = MessageEntity(
            messageId   = messageId,
            userId      = userId,
            chatId      = chatId,
            senderId    = userId,
            recipientId = recipientId,
            content     = content,
            timestamp   = System.currentTimeMillis(),
            delivered   = false,
            source      = "online"
        )

        fun fromRemoteMessage(userId: String, chatId: String, msg: Message) = MessageEntity(
            messageId   = msg.messageId,
            userId      = userId,
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

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: MessageEntity)

    @Query("SELECT * FROM messages WHERE user_id = :userId AND chat_id = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByUserAndChat(userId: String, chatId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getByChatId(chatId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp DESC")
    fun observeByChatId(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    /**
     * Returns the most recent message for each distinct chat the user
     * has participated in — either as sender (user_id) or recipient.
     * Used to build the dynamic conversation list in the UI.
     *
     * The subquery picks MAX(timestamp) per chat, then the outer join
     * fetches the full row for that timestamp so we get the real content.
     */
    @Query("""
        SELECT m.*
        FROM messages m
        INNER JOIN (
            SELECT chat_id, MAX(timestamp) AS last_ts
            FROM messages
            WHERE user_id = :userId OR recipient_id = :userId
            GROUP BY chat_id
        ) latest
        ON m.chat_id = latest.chat_id AND m.timestamp = latest.last_ts
        ORDER BY m.timestamp DESC
    """)
    suspend fun getLatestMessagePerChat(userId: String): List<MessageEntity>
}

@Database(entities = [MessageEntity::class], version = 2, exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var instance: MessageDatabase? = null

        // v1 → v2: adds user_id so messages are linked to a real user
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN user_id TEXT NOT NULL DEFAULT 'unknown'")
            }
        }

        fun getInstance(context: Context): MessageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "messages.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}