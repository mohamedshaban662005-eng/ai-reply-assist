package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.QuickResponseTemplateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testQuickResponseTemplateInsertionAndRetrieval() = runBlocking {
        val dao = db.quickResponseTemplateDao()

        val whatsappTemplate = QuickResponseTemplateEntity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "In a meeting",
            templateText = "I am in a meeting. Will get back shortly!",
            isEnabled = true
        )
        val wildcardTemplate = QuickResponseTemplateEntity(
            packageName = "*",
            appName = "All Apps",
            title = "Away",
            templateText = "Currently away from phone.",
            isEnabled = true
        )

        dao.insert(whatsappTemplate)
        dao.insert(wildcardTemplate)

        val all = dao.getAllTemplates().first()
        assertEquals(2, all.size)

        // Exact package match
        val matchedWhatsApp = dao.getActiveTemplateForPackage("com.whatsapp")
        assertNotNull(matchedWhatsApp)
        assertEquals("In a meeting", matchedWhatsApp?.title)

        // Fallback to wildcard for unlisted package
        val fallbackSignal = dao.getActiveTemplateForPackage("org.thoughtcrime.securesms")
        assertNotNull(fallbackSignal)
        assertEquals("Away", fallbackSignal?.title)

        // When disabled, shouldn't return
        dao.update(matchedWhatsApp!!.copy(isEnabled = false))
        val afterDisable = dao.getActiveTemplateForPackage("com.whatsapp")
        // Should fall back to wildcard
        assertEquals("Away", afterDisable?.title)
    }

    @Test
    fun testWeeklyStatsCalculation() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        val logs = listOf(
            com.example.data.model.NotificationLogEntity(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Alice",
                content = "Hello",
                timestamp = now,
                status = "REPLIED",
                generatedReply = "Hi Alice!"
            ),
            com.example.data.model.NotificationLogEntity(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Bob",
                content = "Meeting code 1234",
                timestamp = now,
                status = "BLOCKED"
            ),
            com.example.data.model.NotificationLogEntity(
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                title = "Charlie",
                content = "Are you there?",
                timestamp = now - dayMillis,
                status = "REPLIED",
                generatedReply = "Yes, what's up?"
            )
        )

        val stats = com.example.ui.viewmodel.MainViewModel.computeWeeklyStats(logs)
        assertEquals(7, stats.dailyStats.size)
        assertEquals(3, stats.totalNotifications)
        assertEquals(2, stats.totalAiResponses)
        assertEquals(66, stats.responseRatePercentage)
    }
}
