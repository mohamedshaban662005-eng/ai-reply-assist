package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferences
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationLogEntity
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationReplyManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "NotificationReplyMgr"
        const val CHANNEL_SUGGESTIONS = "channel_ai_reply_suggestions"
        private const val NOTIFICATION_ID_BASE = 1000

        @Volatile
        private var INSTANCE: NotificationReplyManager? = null

        fun getInstance(context: Context): NotificationReplyManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationReplyManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val db = AppDatabase.getDatabase(context)
    private val securePreferences = SecurePreferences(context)
    private val geminiClient = GeminiApiClient()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SUGGESTIONS,
                "AI Reply Suggestions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows AI generated suggested replies for intercepted notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Handles an incoming notification captured by NotificationListenerService or the in-app tester.
     */
    fun processNotification(
        packageName: String,
        appName: String,
        senderName: String,
        messageText: String,
        replyAction: NotificationCompat.Action? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        scope.launch {
            val result = handleNotificationInternal(
                packageName = packageName,
                appName = appName,
                senderName = senderName,
                messageText = messageText,
                replyAction = replyAction
            )
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result.first, result.second)
            }
        }
    }

    private suspend fun handleNotificationInternal(
        packageName: String,
        appName: String,
        senderName: String,
        messageText: String,
        replyAction: NotificationCompat.Action?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (messageText.isBlank()) {
            return@withContext Pair(false, "Ignored: Empty message text")
        }

        // 1. Blacklist Filtering Check
        val activeBlacklist = db.blacklistDao().getActiveBlacklistWords()
        for (item in activeBlacklist) {
            val containsWord = if (item.isCaseSensitive) {
                messageText.contains(item.word) || senderName.contains(item.word)
            } else {
                messageText.contains(item.word, ignoreCase = true) || senderName.contains(item.word, ignoreCase = true)
            }

            if (containsWord) {
                Log.d(TAG, "Notification blocked by blacklist: '${item.word}'")
                db.notificationLogDao().insertLog(
                    NotificationLogEntity(
                        packageName = packageName,
                        appName = appName,
                        title = senderName,
                        content = messageText,
                        status = "BLOCKED",
                        blockReason = "Matched blacklist keyword: '${item.word}'"
                    )
                )
                return@withContext Pair(false, "Blocked by blacklist: '${item.word}'")
            }
        }

        // 2. Battery Saver Mode check
        val batteryMode = securePreferences.getBatterySaverMode()
        if (batteryMode == SecurePreferences.BATTERY_MODE_SAVER) {
            // In strict power saver mode, avoid frequent API calls if necessary
            Log.d(TAG, "Processing under power saver mode")
        }

        // 3. Conversation & Context Management
        val conversationId = "$packageName:$senderName"
        val existingConv = db.conversationDao().getConversationById(conversationId)
        val newMsgCount = (existingConv?.messageCount ?: 0) + 1

        db.conversationDao().insertOrUpdate(
            ConversationEntity(
                id = conversationId,
                packageName = packageName,
                appName = appName,
                senderName = senderName,
                lastMessage = messageText,
                lastUpdated = System.currentTimeMillis(),
                messageCount = newMsgCount
            )
        )

        val incomingMessageId = db.messageDao().insertMessage(
            MessageEntity(
                conversationId = conversationId,
                sender = senderName,
                text = messageText,
                isFromMe = false,
                timestamp = System.currentTimeMillis(),
                replyStatus = "PENDING"
            )
        )

        // Retrieve recent context turns (up to 6 messages)
        val recentMessages = db.messageDao().getRecentMessages(conversationId, limit = 6).reversed()
        val historyTurns = recentMessages.map { msg ->
            GeminiApiClient.GeminiMessage(
                role = if (msg.isFromMe) "model" else "user",
                text = if (msg.isFromMe) msg.text else "${msg.sender}: ${msg.text}"
            )
        }

        // 4. Structured Prompt Configuration
        val isAiEnabled = securePreferences.isAiEnabled()
        val apiKey = securePreferences.getApiKey()

        if (!isAiEnabled || apiKey.isBlank()) {
            val template = db.quickResponseTemplateDao().getActiveTemplateForPackage(packageName)
            if (template != null && template.templateText.isNotBlank()) {
                val templateReply = template.templateText
                Log.d(TAG, "AI is disabled (${if (!isAiEnabled) "manually disabled" else "no API key"}). Using Quick Response Template: '${template.title}' for $appName")

                // Save to message history
                db.messageDao().insertMessage(
                    MessageEntity(
                        conversationId = conversationId,
                        sender = "Template (${template.title})",
                        text = templateReply,
                        isFromMe = true,
                        timestamp = System.currentTimeMillis(),
                        replyStatus = "GENERATED",
                        generatedReply = templateReply
                    )
                )

                db.notificationLogDao().insertLog(
                    NotificationLogEntity(
                        packageName = packageName,
                        appName = appName,
                        title = senderName,
                        content = messageText,
                        status = "REPLIED",
                        blockReason = "AI disabled; used template: ${template.title}",
                        generatedReply = templateReply
                    )
                )

                if (replyAction != null && securePreferences.isAutoReplyMode()) {
                    val replySent = sendRemoteInputReply(replyAction, templateReply)
                    Log.d(TAG, "Template reply sent via RemoteInput: $replySent")
                }

                showSuggestedReplyNotification(
                    conversationId = conversationId,
                    senderName = senderName,
                    appName = appName,
                    originalMessage = messageText,
                    suggestedReply = templateReply
                )

                return@withContext Pair(true, "Template reply applied (${template.title}): $templateReply")
            } else if (!isAiEnabled) {
                val msg = "AI is disabled and no active Quick Response Template found for $appName"
                db.notificationLogDao().insertLog(
                    NotificationLogEntity(
                        packageName = packageName,
                        appName = appName,
                        title = senderName,
                        content = messageText,
                        status = "IGNORED",
                        blockReason = msg
                    )
                )
                return@withContext Pair(false, msg)
            }
        }

        val systemInstruction = buildStructuredPrompt(senderName, appName)
        val selectedModel = securePreferences.getSelectedModel()

        if (apiKey.isBlank()) {
            val errorMsg = "API Key not configured in settings"
            db.notificationLogDao().insertLog(
                NotificationLogEntity(
                    packageName = packageName,
                    appName = appName,
                    title = senderName,
                    content = messageText,
                    status = "FAILED",
                    blockReason = errorMsg
                )
            )
            return@withContext Pair(false, errorMsg)
        }

        // 5. Query Gemini API
        val genResult = geminiClient.generateReply(
            apiKey = apiKey,
            model = selectedModel,
            conversationHistory = historyTurns,
            systemInstruction = systemInstruction
        )

        if (!genResult.success) {
            db.notificationLogDao().insertLog(
                NotificationLogEntity(
                    packageName = packageName,
                    appName = appName,
                    title = senderName,
                    content = messageText,
                    status = "FAILED",
                    blockReason = genResult.error
                )
            )
            return@withContext Pair(false, "Gemini API error: ${genResult.error}")
        }

        val generatedReply = genResult.text
        val finalReplyText = if (securePreferences.isIncludeAiPrefix()) {
            "[AI] $generatedReply"
        } else {
            generatedReply
        }

        // 6. Save AI reply to conversation history
        db.messageDao().insertMessage(
            MessageEntity(
                conversationId = conversationId,
                sender = "AI Assistant",
                text = finalReplyText,
                isFromMe = true,
                timestamp = System.currentTimeMillis(),
                replyStatus = "GENERATED",
                generatedReply = finalReplyText
            )
        )

        db.notificationLogDao().insertLog(
            NotificationLogEntity(
                packageName = packageName,
                appName = appName,
                title = senderName,
                content = messageText,
                status = "REPLIED",
                generatedReply = finalReplyText
            )
        )

        // 7. Dispatch or Present suggested reply
        if (replyAction != null && securePreferences.isAutoReplyMode()) {
            val replySent = sendRemoteInputReply(replyAction, finalReplyText)
            Log.d(TAG, "Auto-reply sent via RemoteInput: $replySent")
        }

        // Show suggestion notification for user review & one-tap send
        showSuggestedReplyNotification(
            conversationId = conversationId,
            senderName = senderName,
            appName = appName,
            originalMessage = messageText,
            suggestedReply = finalReplyText
        )

        Pair(true, finalReplyText)
    }

    private fun buildStructuredPrompt(senderName: String, appName: String): String {
        val persona = securePreferences.getAiPersona()
        val customPrompt = securePreferences.getCustomPrompt()
        val replyLength = securePreferences.getReplyLength()

        val lengthInstruction = when (replyLength) {
            "One-liner" -> "Keep your response strictly to a single short, punchy sentence."
            "Medium" -> "Provide a helpful, well-structured response in 2-3 sentences."
            else -> "Keep your response concise, friendly, and under 2 sentences."
        }

        val personaInstruction = when (persona) {
            SecurePreferences.PERSONA_PROFESSIONAL ->
                "Adopt a professional, polite, and respectful tone. Use correct punctuation and clear grammar."
            SecurePreferences.PERSONA_CASUAL ->
                "Adopt a friendly, casual, conversational tone as a warm acquaintance. You may occasionally use a friendly emoji if appropriate."
            SecurePreferences.PERSONA_CONCISE ->
                "Be extremely direct and concise. Avoid fluff, polite filler, or pleasantries. Directly answer or acknowledge."
            SecurePreferences.PERSONA_SUPPORT ->
                "Adopt an empathetic, solution-oriented customer care tone. Offer assistance proactively."
            SecurePreferences.PERSONA_CUSTOM -> customPrompt
            else -> "Respond politely, helpfully, and naturally."
        }

        return """
            You are an AI notification assistant answering messages on behalf of the user.
            You are responding to an incoming message from $senderName received via $appName.
            
            GUIDELINES:
            1. $personaInstruction
            2. $lengthInstruction
            3. Answer in the same language as the incoming message.
            4. Do NOT include quotes, brackets, or meta remarks like "Here is a reply:". Output ONLY the direct response text.
            5. If asked for private credentials, passwords, or bank data, politely decline or state that you will follow up in person.
        """.trimIndent()
    }

    private fun sendRemoteInputReply(action: NotificationCompat.Action, replyText: String): Boolean {
        try {
            val remoteInputs = action.remoteInputs ?: return false
            val pendingIntent = action.actionIntent ?: return false
            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            pendingIntent.send(context, 0, intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending remote input reply", e)
            return false
        }
    }

    private fun showSuggestedReplyNotification(
        conversationId: String,
        senderName: String,
        appName: String,
        originalMessage: String,
        suggestedReply: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversation_id", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SUGGESTIONS)
            .setSmallIcon(R.drawable.ic_app_icon_fg)
            .setContentTitle("Smart Reply for $senderName ($appName)")
            .setContentText(suggestedReply)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Incoming: \"$originalMessage\"\n\nAI Reply: \"$suggestedReply\"")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + (conversationId.hashCode() % 1000)
        notificationManager.notify(notificationId, notification)
    }
}
