package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferences
import com.example.service.ForegroundKeepAliveService
import com.example.service.NotificationReplyManager

class AiReplyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var replyManager: NotificationReplyManager
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("AiReplyApp", "Initializing AI Reply Assist Application")

        database = AppDatabase.getDatabase(this)
        securePreferences = SecurePreferences(this)
        replyManager = NotificationReplyManager.getInstance(this)

        // If foreground service was left enabled, start it safely
        if (securePreferences.isForegroundServiceEnabled()) {
            try {
                ForegroundKeepAliveService.start(this)
            } catch (e: Exception) {
                Log.w("AiReplyApp", "Could not automatically start ForegroundKeepAliveService on startup", e)
            }
        }
    }
}
