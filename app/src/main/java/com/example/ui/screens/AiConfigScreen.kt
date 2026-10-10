package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SecurePreferences
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val customPrompt by viewModel.customPrompt.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val replyLength by viewModel.replyLength.collectAsState()
    val isAutoReply by viewModel.isAutoReply.collectAsState()
    val isPrefixEnabled by viewModel.isPrefixEnabled.collectAsState()

    var customPromptInput by remember(customPrompt) { mutableStateOf(customPrompt) }

    val personas = remember {
        listOf(
            Pair(SecurePreferences.PERSONA_PROFESSIONAL, "Polite, well-structured, clear grammar for work & clients"),
            Pair(SecurePreferences.PERSONA_CASUAL, "Warm, friendly acquaintance tone with natural expressions"),
            Pair(SecurePreferences.PERSONA_CONCISE, "Direct 1-sentence answers without filler or pleasantries"),
            Pair(SecurePreferences.PERSONA_SUPPORT, "Empathetic, helpful problem-solver customer care tone"),
            Pair(SecurePreferences.PERSONA_CUSTOM, "Define your own exact system instructions below")
        )
    }

    val models = remember {
        listOf(
            Pair("gemini-3.5-flash", "Default (Fast, high intelligence, recommended)"),
            Pair("gemini-2.5-flash", "Lightweight, rapid response"),
            Pair("gemini-3.1-pro-preview", "Advanced reasoning & nuanced drafting")
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Intro Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_config_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Structured Prompts & AI Behavior",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Configure how Gemini drafts replies, sets tone, constrains answer length, and adapts to conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 1. AI Persona Selection
        item {
            Text(
                text = "AI Persona / Tone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                personas.forEach { (name, desc) ->
                    val isSelected = selectedPersona == name
                    Card(
                        onClick = { viewModel.setAiPersona(name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("persona_option_$name"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setAiPersona(name) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Prompt Editor (Only if Custom Persona is selected)
        item {
            AnimatedVisibility(visible = selectedPersona == SecurePreferences.PERSONA_CUSTOM) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_custom_prompt_editor"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Custom System Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provide explicit system instructions for how the AI should behave.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = customPromptInput,
                            onValueChange = {
                                customPromptInput = it
                                viewModel.setCustomPrompt(it)
                            },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_prompt")
                        )
                    }
                }
            }
        }

        // 2. Reply Length Constraint
        item {
            Text(
                text = "Response Length Constraint",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("One-liner", "Short", "Medium").forEach { len ->
                            val isSelected = replyLength == len
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setReplyLength(len) },
                                label = { Text(len) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_length_$len")
                            )
                        }
                    }
                    Text(
                        text = when (replyLength) {
                            "One-liner" -> "Strictly 1 sentence. Maximum speed and brevity."
                            "Medium" -> "2-3 comprehensive sentences with helpful details."
                            else -> "1-2 balanced sentences. Standard chat reply size."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Gemini Model Selection
        item {
            Text(
                text = "Gemini Model Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                models.forEach { (modelId, desc) ->
                    val isSelected = selectedModel == modelId
                    Card(
                        onClick = { viewModel.setSelectedModel(modelId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_option_$modelId"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedModel(modelId) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = modelId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Output Preferences
        item {
            Text(
                text = "Formatting & Reply Mode",
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
                    // Prefix Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include '[AI]' Prefix in Replies",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Transparency badge so recipients know the message was drafted by AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPrefixEnabled,
                            onCheckedChange = { viewModel.setIncludeAiPrefix(it) },
                            modifier = Modifier.testTag("switch_include_prefix")
                        )
                    }

                    HorizontalDivider()

                    // Auto-Reply vs Suggested Notification Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Reply via RemoteInput",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically dispatch replies when supported. If disabled, replies are posted as reviewable notification suggestions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoReply,
                            onCheckedChange = { viewModel.setAutoReplyMode(it) },
                            modifier = Modifier.testTag("switch_auto_reply_mode")
                        )
                    }
                }
            }
        }
    }
}
