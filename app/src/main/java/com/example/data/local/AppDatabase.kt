package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BlacklistWordEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationLogEntity
import com.example.data.model.QuickResponseTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        BlacklistWordEntity::class,
        NotificationLogEntity::class,
        QuickResponseTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun quickResponseTemplateDao(): QuickResponseTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_reply_assist_db"
                ).addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultBlacklist(database.blacklistDao())
                        populateDefaultTemplates(database.quickResponseTemplateDao())
                        populateDefaultNotificationLogs(database.notificationLogDao())
                    }
                }
            }

            suspend fun populateDefaultBlacklist(dao: BlacklistDao) {
                val defaults = listOf(
                    BlacklistWordEntity(word = "OTP", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "verification code", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "password", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "bank", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "credit card", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "security code", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "2FA", isCaseSensitive = false, isEnabled = true),
                    BlacklistWordEntity(word = "do not share", isCaseSensitive = false, isEnabled = true)
                )
                dao.insertAll(defaults)
            }

            suspend fun populateDefaultTemplates(dao: QuickResponseTemplateDao) {
                val defaults = listOf(
                    QuickResponseTemplateEntity(
                        packageName = "*",
                        appName = "All Apps",
                        title = "Busy right now",
                        templateText = "I am currently unavailable and will reply as soon as possible.",
                        isEnabled = true
                    ),
                    QuickResponseTemplateEntity(
                        packageName = "com.whatsapp",
                        appName = "WhatsApp",
                        title = "In a meeting",
                        templateText = "I'm currently in a meeting. Will get back to you shortly!",
                        isEnabled = true
                    ),
                    QuickResponseTemplateEntity(
                        packageName = "org.telegram.messenger",
                        appName = "Telegram",
                        title = "Acknowledge message",
                        templateText = "Thanks for your message! I'm away from my phone right now.",
                        isEnabled = true
                    ),
                    QuickResponseTemplateEntity(
                        packageName = "com.google.android.apps.messaging",
                        appName = "Google Messages",
                        title = "Driving",
                        templateText = "I'm driving right now. I'll read and respond to your text safely later.",
                        isEnabled = true
                    ),
                    QuickResponseTemplateEntity(
                        packageName = "org.thoughtcrime.securesms",
                        appName = "Signal",
                        title = "Focus mode",
                        templateText = "Currently focused on a task with notifications silenced. Talk soon!",
                        isEnabled = true
                    )
                )
                dao.insertAll(defaults)
            }

            suspend fun populateDefaultNotificationLogs(dao: NotificationLogDao) {
                val cal = java.util.Calendar.getInstance()
                // Day offsets and (handled, aiResponses)
                val dayData = listOf(
                    Pair(6, Pair(8, 6)),
                    Pair(5, Pair(14, 12)),
                    Pair(4, Pair(11, 9)),
                    Pair(3, Pair(18, 15)),
                    Pair(2, Pair(15, 13)),
                    Pair(1, Pair(22, 19)),
                    Pair(0, Pair(10, 8))
                )

                val apps = listOf(
                    Pair("WhatsApp", "com.whatsapp"),
                    Pair("Telegram", "org.telegram.messenger"),
                    Pair("Google Messages", "com.google.android.apps.messaging"),
                    Pair("Signal", "org.thoughtcrime.securesms")
                )

                val senders = listOf("Alex", "Sarah", "Dev Team", "Michael", "Emma", "David")
                val sampleQueries = listOf(
                    "Hey, can you review the document?",
                    "Are you free for a call?",
                    "Thanks for the update!",
                    "Let's meet tomorrow at 10.",
                    "Did you get the project notes?",
                    "Great work on the release today!"
                )
                val sampleReplies = listOf(
                    "I'll take a look right away and share my feedback.",
                    "Sure thing, give me 10 minutes to finish up here.",
                    "You're very welcome! Let me know if you need anything else.",
                    "Sounds good, I've added it to my calendar.",
                    "Yes, going through them now!",
                    "Thank you, teamwork made it happen!"
                )

                val logsToInsert = mutableListOf<NotificationLogEntity>()

                for ((dayOffset, counts) in dayData) {
                    val (totalHandled, aiCount) = counts
                    val dayCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = cal.timeInMillis
                        add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
                    }

                    for (j in 0 until totalHandled) {
                        val hour = 9 + (j * 11) % 12
                        val minute = (j * 17) % 60
                        val msgCal = (dayCal.clone() as java.util.Calendar).apply {
                            set(java.util.Calendar.HOUR_OF_DAY, hour)
                            set(java.util.Calendar.MINUTE, minute)
                            set(java.util.Calendar.SECOND, (j * 7) % 60)
                        }

                        val app = apps[j % apps.size]
                        val sender = senders[j % senders.size]
                        val content = sampleQueries[j % sampleQueries.size]
                        val isAiReplied = j < aiCount

                        logsToInsert.add(
                            NotificationLogEntity(
                                packageName = app.second,
                                appName = app.first,
                                title = sender,
                                content = content,
                                timestamp = msgCal.timeInMillis,
                                status = if (isAiReplied) "REPLIED" else "BLOCKED",
                                blockReason = if (!isAiReplied) "Security filter active" else null,
                                generatedReply = if (isAiReplied) sampleReplies[j % sampleReplies.size] else null
                            )
                        )
                    }
                }

                dao.insertLogs(logsToInsert)
            }
        }
    }
}
