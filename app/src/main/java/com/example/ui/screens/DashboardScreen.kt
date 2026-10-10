package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.StatsView
import com.example.data.model.NotificationLogEntity
import com.example.service.NotificationMonitorService
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val isForegroundEnabled by viewModel.isForegroundEnabled.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val logs by viewModel.recentLogs.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val simulationResult by viewModel.simulationResult.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    // Simulation Form State
    var selectedAppIndex by remember { mutableIntStateOf(0) }
    val appsList = remember {
        listOf(
            Triple("WhatsApp", "com.whatsapp", Icons.AutoMirrored.Filled.Chat),
            Triple("Telegram", "org.telegram.messenger", Icons.AutoMirrored.Filled.Send),
            Triple("Google Messages", "com.google.android.apps.messaging", Icons.Default.Sms),
            Triple("Signal", "org.thoughtcrime.securesms", Icons.Default.Lock)
        )
    }

    var senderInput by remember { mutableStateOf("Sarah (Colleague)") }
    var messageInput by remember { mutableStateOf("Hey! Are you free for a quick sync call this afternoon?") }

    // Refresh status when returning to screen
    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatuses()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sparkle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Reply Assist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Model: $selectedModel • Background Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 2. Core Services Status Cards
        item {
            Text(
                text = "System & Service Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Notification Listener Service Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_status_listener"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isNotificationAccessGranted)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNotificationAccessGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isNotificationAccessGranted) StatusSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Service",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotificationAccessGranted) "Active • Capturing incoming messages" else "Permission needed to capture notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isNotificationAccessGranted) {
                            Button(
                                onClick = {
                                    NotificationMonitorService.openNotificationAccessSettings(context)
                                },
                                modifier = Modifier.testTag("btn_grant_listener")
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }

                // Foreground Keep-Alive Service Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_status_foreground"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isForegroundEnabled) Icons.Default.SecurityUpdateGood else Icons.Default.PauseCircle,
                            contentDescription = null,
                            tint = if (isForegroundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foreground Service",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isForegroundEnabled) "Running • Prevents background termination" else "Paused • Tap switch to keep alive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isForegroundEnabled,
                            onCheckedChange = { viewModel.toggleForegroundService(it) },
                            modifier = Modifier.testTag("switch_foreground_service")
                        )
                    }
                }

                // API Key & Gemini Connectivity Card
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("service_status_gemini"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (apiKey.isNotBlank()) Icons.Default.VpnKey else Icons.Default.KeyOff,
                            contentDescription = null,
                            tint = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.secondary else StatusWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google AI Studio API Key",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (apiKey.isNotBlank()) "Secured in EncryptedStorage" else "API Key missing • Add in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("btn_configure_api_key")
                        ) {
                            Text(if (apiKey.isNotBlank()) "Config" else "Add Key")
                        }
                    }
                }
            }
        }

        // 3. 7-Day Stats & Recharts Analytics
        item {
            StatsView(viewModel = viewModel)
        }

        // 4. In-App Notification Simulator Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulation_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Notification Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Simulate an incoming notification to test interception, blacklist filter, context tracking, and Gemini AI reply generation in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target App Selector Chips
                    Text(
                        text = "Simulated App:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        appsList.forEachIndexed { index, (name, _, icon) ->
                            FilterChip(
                                selected = selectedAppIndex == index,
                                onClick = { selectedAppIndex = index },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("chip_app_$name")
                            )
                        }
                    }

                    // Sender input
                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = { senderInput = it },
                        label = { Text("Sender Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sim_sender")
                    )

                    // Message input
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Incoming Message Text") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sim_message")
                    )

                    // Preset buttons
                    Text(
                        text = "Quick Test Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                senderInput = "Alex"
                                messageInput = "Hi! Can you share the presentation slides before the meeting?"
                            },
                            label = { Text("Work Query", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                senderInput = "David"
                                messageInput = "Are we still having dinner tonight at 7?"
                            },
                            label = { Text("Casual Sync", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = {
                                senderInput = "Auth Service"
                                messageInput = "Your verification code OTP is 839201. Do not share with anyone."
                            },
                            label = { Text("Test Blacklist OTP", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    // Simulation Button
                    Button(
                        onClick = {
                            val targetApp = appsList[selectedAppIndex]
                            viewModel.simulateNotification(
                                appName = targetApp.first,
                                packageName = targetApp.second,
                                senderName = senderInput,
                                messageText = messageInput
                            )
                        },
                        enabled = !isSimulating && messageInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_trigger_simulation")
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Querying Gemini AI...")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate & Generate Reply")
                        }
                    }

                    // Live Simulation Result Banner
                    AnimatedVisibility(visible = simulationResult != null) {
                        simulationResult?.let { res ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulation_result_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (res.startsWith("AI Reply Generated"))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else if (res.startsWith("Blocked"))
                                        MaterialTheme.colorScheme.errorContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (res.startsWith("AI Reply Generated")) "✨ Response Created"
                                        else if (res.startsWith("Blocked")) "🛡️ Blacklist Filter Triggered"
                                        else "Status Update",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Recent Notification Activity Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Activity & Interception Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearAllLogs() },
                        modifier = Modifier.testTag("btn_clear_logs")
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No notifications intercepted yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Use the simulator above or receive an actual notification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                NotificationLogItem(log = log)
            }
        }
    }
}

@Composable
fun NotificationLogItem(log: NotificationLogEntity) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        containerColor = when (log.status) {
                            "REPLIED" -> StatusSuccess
                            "BLOCKED" -> StatusBlocked
                            "FAILED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = log.status,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${log.appName} • ${log.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = "\"${log.content}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!log.generatedReply.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Reply: ${log.generatedReply}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!log.blockReason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${log.blockReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
