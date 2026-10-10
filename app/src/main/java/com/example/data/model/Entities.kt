package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String, // format: "packageName:senderName"
    val packageName: String,
    val appName: String,
    val senderName: String,
    val lastMessage: String,
    val lastUpdated: Long,
    val messageCount: Int = 1
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val sender: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long,
    val replyStatus: String = "RECEIVED", // RECEIVED, PENDING, GENERATED, BLOCKED, SENT
    val generatedReply: String? = null
)

@Entity(tableName = "blacklist_words")
data class BlacklistWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val isCaseSensitive: Boolean = false,
    val isEnabled: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // REPLIED, BLOCKED, IGNORED, FAILED, RECEIVED
    val blockReason: String? = null,
    val generatedReply: String? = null
)

@Entity(tableName = "quick_response_templates")
data class QuickResponseTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String = "*", // Specific app package name e.g. "com.whatsapp", or "*" for all apps
    val appName: String = "All Apps",
    val title: String,
    val templateText: String,
    val isEnabled: Boolean = true,
    val dateModified: Long = System.currentTimeMillis()
)
