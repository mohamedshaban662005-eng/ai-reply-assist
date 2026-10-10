package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.SettingsActivity
import com.example.data.local.SecurePreferences
import com.example.data.model.QuickResponseTemplateEntity
import com.example.util.LocaleHelper
import com.example.service.NotificationMonitorService
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.KeyValidationStatus
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentApiKey by viewModel.apiKey.collectAsState()
    val validationStatus by viewModel.keyValidationStatus.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val isForegroundEnabled by viewModel.isForegroundEnabled.collectAsState()
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val batterySaverMode by viewModel.batterySaverMode.collectAsState()
    val templates by viewModel.quickResponseTemplates.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()

    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<QuickResponseTemplateEntity?>(null) }
    var templateTitleInput by remember { mutableStateOf("") }
    var templateTextInput by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf("*") }
    var selectedAppName by remember { mutableStateOf("All Apps") }

    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatuses()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Google AI Studio API Key & Encrypted Storage
        item {
            Text(
                text = "Google AI Studio Authentication",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_api_key_settings"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dynamic API Key Configuration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stored in EncryptedSharedPreferences with hardware-backed AES256-GCM encryption.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isKeyVisible = !isKeyVisible },
                                modifier = Modifier.testTag("btn_toggle_key_visibility")
                            ) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Show Key"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_gemini_api_key")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateApiKey(apiKeyInput)
                                viewModel.validateApiKey(apiKeyInput)
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_and_verify_key")
                        ) {
                            Text("Save & Verify")
                        }

                        OutlinedButton(
                            onClick = {
                                apiKeyInput = ""
                                viewModel.clearApiKey()
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier.testTag("btn_clear_api_key")
                        ) {
                            Text("Clear")
                        }
                    }

                    // Key Validation Status Feedback
                    AnimatedVisibility(visible = validationStatus !is KeyValidationStatus.Idle) {
                        when (val status = validationStatus) {
                            is KeyValidationStatus.Validating -> {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting to Gemini API...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            is KeyValidationStatus.Success -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(status.message, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            is KeyValidationStatus.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(status.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Section: Battery Optimization Strategy
        item {
            Text(
                text = "Battery Optimization Strategy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_battery_optimization"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBatteryOptimized) Icons.Default.BatteryChargingFull else Icons.Outlined.BatteryAlert,
                                contentDescription = null,
                                tint = if (isBatteryOptimized) StatusSuccess else StatusWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Exemption Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (isBatteryOptimized) "Unrestricted" else "Restricted",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isBatteryOptimized) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    Text(
                        text = if (isBatteryOptimized)
                            "Android battery optimization is unrestricted. Background notification listener and keep-alive foreground services will run continuously without sleep interruption."
                        else
                            "App is subject to standard Android battery saver restrictions. System may sleep background services during screen off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isBatteryOptimized) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_request_battery_exemption")
                        ) {
                            Text("Request Battery Optimization Exemption")
                        }
                    }

                    HorizontalDivider()

                    // Power Conservation Policy
                    Text(
                        text = "Power Policy Mode:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val modes = listOf(
                        Pair(SecurePreferences.BATTERY_MODE_BALANCED, "Balanced (Recommended)"),
                        Pair(SecurePreferences.BATTERY_MODE_HIGH_PERFORMANCE, "High Performance (Zero Debounce)"),
                        Pair(SecurePreferences.BATTERY_MODE_SAVER, "Power Saver (Strict Batching)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modes.forEach { (modeKey, modeTitle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("battery_mode_$modeKey"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = batterySaverMode == modeKey,
                                    onClick = { viewModel.setBatterySaverMode(modeKey) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(modeTitle, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Section: System Permissions & Services
        item {
            Text(
                text = "Services & System Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Notification Listener
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Access",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotificationAccessGranted) "Granted by system" else "Requires user permission",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { NotificationMonitorService.openNotificationAccessSettings(context) },
                            modifier = Modifier.testTag("btn_open_listener_settings")
                        ) {
                            Text(if (isNotificationAccessGranted) "Manage" else "Enable")
                        }
                    }

                    HorizontalDivider()

                    // Foreground Service Keep-Alive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foreground Service Keep-Alive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Maintains persistent process priority to avoid system kills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isForegroundEnabled,
                            onCheckedChange = { viewModel.toggleForegroundService(it) },
                            modifier = Modifier.testTag("switch_settings_foreground")
                        )
                    }
                }
            }
        }

        // Section: Language Settings
        item {
            Text(
                text = stringResource(R.string.language_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val currentLanguage = remember { LocaleHelper.getAppLanguage(context) }
            val currentLangName = if (currentLanguage == "ar") {
                stringResource(R.string.lang_arabic)
            } else {
                stringResource(R.string.lang_english)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_language_settings"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.language_section_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.language_current_label, currentLangName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // LanguageSwitchButton
                    Button(
                        onClick = {
                            LocaleHelper.toggleLanguage(context)
                            if (context is Activity) {
                                context.recreate()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("LanguageSwitchButton"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.btn_switch_language))
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_settings_activity"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_activity_title))
                    }
                }
            }
        }

        // Section: Quick Response Templates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.templates_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.templates_section_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        editingTemplate = null
                        templateTitleInput = ""
                        templateTextInput = ""
                        selectedAppPackage = "*"
                        selectedAppName = "All Apps"
                        showTemplateDialog = true
                    },
                    modifier = Modifier.testTag("btn_add_template"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_add_template))
                }
            }
        }

        // AI Replies Master Switch & Fallback Alert
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_ai_replies_toggle"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAiEnabled) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_reply_toggle_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.ai_reply_toggle_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAiEnabled,
                            onCheckedChange = { viewModel.toggleAiEnabled(it) },
                            modifier = Modifier.testTag("switch_ai_replies_enabled")
                        )
                    }

                    if (!isAiEnabled) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AI is disabled. Pre-written templates below will automatically answer incoming notifications.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Template List Items
        if (templates.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_templates_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(templates.size) { index ->
                val template = templates[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_template_${template.id}"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = template.appName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Switch(
                                checked = template.isEnabled,
                                onCheckedChange = { viewModel.toggleTemplate(template) },
                                modifier = Modifier.testTag("switch_template_${template.id}")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${template.templateText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (template.isEnabled) {
                                    stringResource(R.string.template_status_active)
                                } else {
                                    stringResource(R.string.template_status_disabled)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (template.isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        editingTemplate = template
                                        templateTitleInput = template.title
                                        templateTextInput = template.templateText
                                        selectedAppPackage = template.packageName
                                        selectedAppName = template.appName
                                        showTemplateDialog = true
                                    },
                                    modifier = Modifier.testTag("btn_edit_template_${template.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Template",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteTemplate(template) },
                                    modifier = Modifier.testTag("btn_delete_template_${template.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Template",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Help & Application Guide
        item {
            Text(
                text = "Help & Documentation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ElevatedCard(
                onClick = {
                    val intent = Intent(context, com.example.HelpActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_open_help_guide"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Application Guide & Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Learn how to configure notification listener, blacklist rules, and Gemini prompts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showTemplateDialog) {
        val appOptions = listOf(
            Pair("*", stringResource(R.string.all_apps)),
            Pair("com.whatsapp", "WhatsApp"),
            Pair("org.telegram.messenger", "Telegram"),
            Pair("com.google.android.apps.messaging", "Google Messages"),
            Pair("org.thoughtcrime.securesms", "Signal")
        )

        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = {
                Text(
                    text = if (editingTemplate == null) {
                        stringResource(R.string.dialog_add_template_title)
                    } else {
                        stringResource(R.string.dialog_edit_template_title)
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.template_target_app_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        appOptions.forEach { (pkg, name) ->
                            FilterChip(
                                selected = selectedAppPackage == pkg,
                                onClick = {
                                    selectedAppPackage = pkg
                                    selectedAppName = name
                                },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = templateTitleInput,
                        onValueChange = { templateTitleInput = it },
                        label = { Text(stringResource(R.string.template_title_label)) },
                        placeholder = { Text(stringResource(R.string.template_title_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_template_title"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = templateTextInput,
                        onValueChange = { templateTextInput = it },
                        label = { Text(stringResource(R.string.template_text_label)) },
                        placeholder = { Text(stringResource(R.string.template_text_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_template_text"),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (templateTitleInput.isNotBlank() && templateTextInput.isNotBlank()) {
                            if (editingTemplate == null) {
                                viewModel.addTemplate(
                                    packageName = selectedAppPackage,
                                    appName = selectedAppName,
                                    title = templateTitleInput,
                                    templateText = templateTextInput
                                )
                            } else {
                                viewModel.updateTemplate(
                                    editingTemplate!!.copy(
                                        packageName = selectedAppPackage,
                                        appName = selectedAppName,
                                        title = templateTitleInput,
                                        templateText = templateTextInput
                                    )
                                )
                            }
                            showTemplateDialog = false
                        }
                    },
                    enabled = templateTitleInput.isNotBlank() && templateTextInput.isNotBlank(),
                    modifier = Modifier.testTag("btn_save_template")
                ) {
                    Text(stringResource(R.string.btn_save_template))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTemplateDialog = false },
                    modifier = Modifier.testTag("btn_cancel_template")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
