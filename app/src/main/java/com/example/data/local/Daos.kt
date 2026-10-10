package com.example.data.local

import androidx.room.*
import com.example.data.model.BlacklistWordEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationLogEntity
import com.example.data.model.QuickResponseTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastUpdated DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(convId: String, limit: Int = 10): List<MessageEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesForConversation(convId: String)
}

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist_words ORDER BY dateAdded DESC")
    fun getAllBlacklistWords(): Flow<List<BlacklistWordEntity>>

    @Query("SELECT * FROM blacklist_words WHERE isEnabled = 1")
    suspend fun getActiveBlacklistWords(): List<BlacklistWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: BlacklistWordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<BlacklistWordEntity>)

    @Update
    suspend fun update(word: BlacklistWordEntity)

    @Delete
    suspend fun delete(word: BlacklistWordEntity)

    @Query("DELETE FROM blacklist_words WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs ORDER BY timestamp ASC")
    fun getAllLogs(): Flow<List<NotificationLogEntity>>

    @Insert
    suspend fun insertLog(log: NotificationLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<NotificationLogEntity>)

    @Query("DELETE FROM notification_logs")
    suspend fun clearAllLogs()
}

@Dao
interface QuickResponseTemplateDao {
    @Query("SELECT * FROM quick_response_templates ORDER BY dateModified DESC")
    fun getAllTemplates(): Flow<List<QuickResponseTemplateEntity>>

    @Query("SELECT * FROM quick_response_templates WHERE (packageName = :packageName OR packageName = '*') AND isEnabled = 1 ORDER BY CASE WHEN packageName = :packageName THEN 0 ELSE 1 END LIMIT 1")
    suspend fun getActiveTemplateForPackage(packageName: String): QuickResponseTemplateEntity?

    @Query("SELECT * FROM quick_response_templates WHERE packageName = :packageName ORDER BY dateModified DESC")
    suspend fun getTemplatesForPackage(packageName: String): List<QuickResponseTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: QuickResponseTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<QuickResponseTemplateEntity>)

    @Update
    suspend fun update(template: QuickResponseTemplateEntity)

    @Delete
    suspend fun delete(template: QuickResponseTemplateEntity)

    @Query("DELETE FROM quick_response_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
