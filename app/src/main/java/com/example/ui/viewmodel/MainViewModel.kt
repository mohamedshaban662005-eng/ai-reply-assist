package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AiReplyApplication
import com.example.data.local.SecurePreferences
import com.example.data.model.BlacklistWordEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NotificationLogEntity
import com.example.data.model.QuickResponseTemplateEntity
import com.example.data.remote.GeminiApiClient
import com.example.service.ForegroundKeepAliveService
import com.example.service.NotificationMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class KeyValidationStatus {
    object Idle : KeyValidationStatus()
    object Validating : KeyValidationStatus()
    data class Success(val message: String) : KeyValidationStatus()
    data class Error(val error: String) : KeyValidationStatus()
}

data class DayStats(
    val dayKey: String,
    val dayLabel: String,
    val fullDateLabel: String,
    val notificationsHandled: Int,
    val aiResponses: Int
)

data class WeeklyStats(
    val dailyStats: List<DayStats>,
    val totalNotifications: Int,
    val totalAiResponses: Int,
    val responseRatePercentage: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AiReplyApplication
    private val db = app.database
    private val prefs = app.securePreferences
    private val replyManager = app.replyManager
    private val geminiClient = GeminiApiClient()

    // Database flows
    val conversations: StateFlow<List<ConversationEntity>> = db.conversationDao()
        .getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<NotificationLogEntity>> = db.notificationLogDao()
        .getRecentLogs(limit = 40)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val last7DaysStats: StateFlow<WeeklyStats> = db.notificationLogDao()
        .getAllLogs()
        .map { logs -> computeWeeklyStats(logs) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            computeWeeklyStats(emptyList())
        )

    val blacklistWords: StateFlow<List<BlacklistWordEntity>> = db.blacklistDao()
        .getAllBlacklistWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickResponseTemplates: StateFlow<List<QuickResponseTemplateEntity>> = db.quickResponseTemplateDao()
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state flows
    val apiKey: StateFlow<String> = prefs.apiKeyFlow
    val isForegroundEnabled: StateFlow<Boolean> = prefs.foregroundServiceEnabledFlow
    val isAiEnabled: StateFlow<Boolean> = prefs.aiEnabledFlow

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _keyValidationStatus = MutableStateFlow<KeyValidationStatus>(KeyValidationStatus.Idle)
    val keyValidationStatus: StateFlow<KeyValidationStatus> = _keyValidationStatus.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedPersona = MutableStateFlow(prefs.getAiPersona())
    val selectedPersona: StateFlow<String> = _selectedPersona.asStateFlow()

    private val _customPrompt = MutableStateFlow(prefs.getCustomPrompt())
    val customPrompt: StateFlow<String> = _customPrompt.asStateFlow()

    private val _replyLength = MutableStateFlow(prefs.getReplyLength())
    val replyLength: StateFlow<String> = _replyLength.asStateFlow()

    private val _includeAiPrefix = MutableStateFlow(prefs.isIncludeAiPrefix())
    val includeAiPrefix: StateFlow<String> = MutableStateFlow(if (prefs.isIncludeAiPrefix()) "true" else "false")
    val isPrefixEnabled: StateFlow<Boolean> = MutableStateFlow(prefs.isIncludeAiPrefix())

    private val _isAutoReply = MutableStateFlow(prefs.isAutoReplyMode())
    val isAutoReply: StateFlow<Boolean> = _isAutoReply.asStateFlow()

    private val _batterySaverMode = MutableStateFlow(prefs.getBatterySaverMode())
    val batterySaverMode: StateFlow<String> = _batterySaverMode.asStateFlow()

    // Simulation feedback
    private val _simulationResult = MutableStateFlow<String?>(null)
    val simulationResult: StateFlow<String?> = _simulationResult.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    // Test Blacklist Result
    private val _blacklistTestResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val blacklistTestResult: StateFlow<Pair<Boolean, String>?> = _blacklistTestResult.asStateFlow()

    init {
        refreshServiceStatuses()
    }

    fun refreshServiceStatuses() {
        _isNotificationAccessGranted.value = NotificationMonitorService.isNotificationAccessGranted(app)

        val powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isBatteryOptimizationIgnored.value = powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun updateApiKey(newKey: String) {
        prefs.setApiKey(newKey)
        _keyValidationStatus.value = KeyValidationStatus.Idle
    }

    fun clearApiKey() {
        prefs.clearApiKey()
        _keyValidationStatus.value = KeyValidationStatus.Idle
    }

    fun validateApiKey(keyToTest: String? = null) {
        val key = (keyToTest ?: prefs.getApiKey()).trim()
        if (key.isBlank()) {
            _keyValidationStatus.value = KeyValidationStatus.Error("Please enter an API key first.")
            return
        }

        viewModelScope.launch {
            _keyValidationStatus.value = KeyValidationStatus.Validating
            val (success, message) = geminiClient.validateApiKey(key, prefs.getSelectedModel())
            if (success) {
                _keyValidationStatus.value = KeyValidationStatus.Success(message)
            } else {
                _keyValidationStatus.value = KeyValidationStatus.Error(message)
            }
        }
    }

    fun toggleForegroundService(enable: Boolean) {
        prefs.setForegroundServiceEnabled(enable)
        if (enable) {
            ForegroundKeepAliveService.start(app)
        } else {
            ForegroundKeepAliveService.stop(app)
        }
    }

    fun setSelectedModel(model: String) {
        prefs.setSelectedModel(model)
        _selectedModel.value = model
    }

    fun setAiPersona(persona: String) {
        prefs.setAiPersona(persona)
        _selectedPersona.value = persona
    }

    fun setCustomPrompt(prompt: String) {
        prefs.setCustomPrompt(prompt)
        _customPrompt.value = prompt
    }

    fun setReplyLength(length: String) {
        prefs.setReplyLength(length)
        _replyLength.value = length
    }

    fun setIncludeAiPrefix(include: Boolean) {
        prefs.setIncludeAiPrefix(include)
        _includeAiPrefix.value = include
    }

    fun setAutoReplyMode(enabled: Boolean) {
        prefs.setAutoReplyMode(enabled)
        _isAutoReply.value = enabled
    }

    fun setBatterySaverMode(mode: String) {
        prefs.setBatterySaverMode(mode)
        _batterySaverMode.value = mode
    }

    // Blacklist Operations
    fun addBlacklistWord(word: String, caseSensitive: Boolean = false) {
        if (word.isBlank()) return
        viewModelScope.launch {
            db.blacklistDao().insert(
                BlacklistWordEntity(
                    word = word.trim(),
                    isCaseSensitive = caseSensitive,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleBlacklistWord(word: BlacklistWordEntity) {
        viewModelScope.launch {
            db.blacklistDao().update(word.copy(isEnabled = !word.isEnabled))
        }
    }

    fun deleteBlacklistWord(word: BlacklistWordEntity) {
        viewModelScope.launch {
            db.blacklistDao().delete(word)
        }
    }

    fun testMessageAgainstBlacklist(text: String) {
        if (text.isBlank()) {
            _blacklistTestResult.value = null
            return
        }
        viewModelScope.launch {
            val activeWords = db.blacklistDao().getActiveBlacklistWords()
            var matched: String? = null
            for (w in activeWords) {
                val matches = if (w.isCaseSensitive) {
                    text.contains(w.word)
                } else {
                    text.contains(w.word, ignoreCase = true)
                }
                if (matches) {
                    matched = w.word
                    break
                }
            }
            if (matched != null) {
                _blacklistTestResult.value = Pair(true, "Blocked! Matches blacklist keyword: \"$matched\"")
            } else {
                _blacklistTestResult.value = Pair(false, "Passed! No blacklisted keywords detected.")
            }
        }
    }

    // Conversation thread messages
    fun getMessagesForConversation(convId: String) = db.messageDao().getMessagesForConversation(convId)

    fun clearAllLogs() {
        viewModelScope.launch {
            db.notificationLogDao().clearAllLogs()
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            db.conversationDao().deleteConversation(id)
            db.messageDao().deleteMessagesForConversation(id)
        }
    }

    // Quick Response Template Management
    fun toggleAiEnabled(enabled: Boolean) {
        prefs.setAiEnabled(enabled)
    }

    fun addTemplate(packageName: String, appName: String, title: String, templateText: String) {
        viewModelScope.launch {
            db.quickResponseTemplateDao().insert(
                QuickResponseTemplateEntity(
                    packageName = packageName,
                    appName = appName,
                    title = title.trim(),
                    templateText = templateText.trim(),
                    isEnabled = true,
                    dateModified = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateTemplate(template: QuickResponseTemplateEntity) {
        viewModelScope.launch {
            db.quickResponseTemplateDao().update(
                template.copy(dateModified = System.currentTimeMillis())
            )
        }
    }

    fun toggleTemplate(template: QuickResponseTemplateEntity) {
        viewModelScope.launch {
            db.quickResponseTemplateDao().update(
                template.copy(
                    isEnabled = !template.isEnabled,
                    dateModified = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTemplate(template: QuickResponseTemplateEntity) {
        viewModelScope.launch {
            db.quickResponseTemplateDao().delete(template)
        }
    }

    // Simulate Notification
    fun simulateNotification(
        appName: String,
        packageName: String,
        senderName: String,
        messageText: String
    ) {
        _isSimulating.value = true
        _simulationResult.value = "Processing incoming simulated notification..."

        replyManager.processNotification(
            packageName = packageName,
            appName = appName,
            senderName = senderName,
            messageText = messageText,
            replyAction = null
        ) { success, result ->
            _isSimulating.value = false
            _simulationResult.value = if (success) {
                "AI Reply Generated: \"$result\""
            } else {
                result
            }
        }
    }

    companion object {
        fun computeWeeklyStats(logs: List<NotificationLogEntity>): WeeklyStats {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val shortDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val daysList = mutableListOf<DayStats>()

            for (i in 6 downTo 0) {
                val dayCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val startOfDay = dayCal.timeInMillis
                val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
                val dayKey = keyFormat.format(dayCal.time)
                val dayLabel = if (i == 0) "Today" else dayFormat.format(dayCal.time)
                val fullLabel = shortDateFormat.format(dayCal.time)

                val dayLogs = logs.filter { it.timestamp in startOfDay..endOfDay }
                val handled = dayLogs.size
                val aiResponses = dayLogs.count { it.status == "REPLIED" || !it.generatedReply.isNullOrBlank() }

                daysList.add(
                    DayStats(
                        dayKey = dayKey,
                        dayLabel = dayLabel,
                        fullDateLabel = fullLabel,
                        notificationsHandled = handled,
                        aiResponses = aiResponses
                    )
                )
            }

            val totalHandled = daysList.sumOf { it.notificationsHandled }
            val totalAi = daysList.sumOf { it.aiResponses }
            val responseRate = if (totalHandled > 0) (totalAi * 100) / totalHandled else 0

            return WeeklyStats(
                dailyStats = daysList,
                totalNotifications = totalHandled,
                totalAiResponses = totalAi,
                responseRatePercentage = responseRate
            )
        }
    }
}
