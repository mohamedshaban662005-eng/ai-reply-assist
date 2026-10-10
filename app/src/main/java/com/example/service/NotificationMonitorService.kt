package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.data.local.SecurePreferences

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationMonitor"

        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).toTypedArray()
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(packageName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private lateinit var replyManager: NotificationReplyManager
    private lateinit var securePreferences: SecurePreferences

    override fun onCreate() {
        super.onCreate()
        replyManager = NotificationReplyManager.getInstance(applicationContext)
        securePreferences = SecurePreferences(applicationContext)
        Log.d(TAG, "NotificationMonitorService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Ignore our own notifications
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return

        // Ignore non-user-facing notifications or ongoing system alerts
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        // Check if package is monitored
        if (!securePreferences.isAppMonitored(packageName)) {
            Log.d(TAG, "Package $packageName is not in monitored apps list, skipping")
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""

        val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && messageText.isBlank()) return

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        // Extract Quick Reply Action if present
        val quickReplyAction = extractQuickReplyAction(notification)

        Log.d(TAG, "Captured notification from $appName ($packageName): $title -> $messageText")

        replyManager.processNotification(
            packageName = packageName,
            appName = appName,
            senderName = title.ifBlank { appName },
            messageText = messageText,
            replyAction = quickReplyAction
        )
    }

    private fun extractQuickReplyAction(notification: Notification): NotificationCompat.Action? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.resultKey != null && remoteInput.allowFreeFormInput) {
                    // Convert to NotificationCompat.Action for uniform handling
                    val compatActionBuilder = NotificationCompat.Action.Builder(
                        action.icon,
                        action.title,
                        action.actionIntent
                    )
                    val compatRemoteInput = RemoteInput.Builder(remoteInput.resultKey)
                        .setLabel(remoteInput.label)
                        .setAllowFreeFormInput(remoteInput.allowFreeFormInput)
                        .build()
                    compatActionBuilder.addRemoteInput(compatRemoteInput)
                    return compatActionBuilder.build()
                }
            }
        }
        return null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected to system service")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }
}
