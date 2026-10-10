package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.HelpActivity
import com.example.R
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel

enum class MainNavigationTab(
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    CONVERSATIONS(R.string.nav_threads, Icons.Filled.Forum, Icons.Outlined.Forum, "tab_conversations"),
    BLACKLIST(R.string.nav_blacklist, Icons.Filled.Block, Icons.Outlined.Block, "tab_blacklist"),
    AI_CONFIG(R.string.nav_ai_behavior, Icons.Filled.Psychology, Icons.Outlined.Psychology, "tab_ai_config"),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavigationTab.DASHBOARD) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            MainNavigationTab.DASHBOARD -> stringResource(R.string.app_name)
                            MainNavigationTab.CONVERSATIONS -> stringResource(R.string.threads_title)
                            MainNavigationTab.BLACKLIST -> stringResource(R.string.blacklist_card_title)
                            MainNavigationTab.AI_CONFIG -> stringResource(R.string.ai_config_card_title)
                            MainNavigationTab.SETTINGS -> stringResource(R.string.nav_settings)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, HelpActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("btn_top_help")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.nav_help)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshServiceStatuses() },
                        modifier = Modifier.testTag("btn_top_refresh")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                tonalElevation = 6.dp
            ) {
                MainNavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val tabTitle = stringResource(tab.titleResId)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tabTitle
                            )
                        },
                        label = { Text(tabTitle) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainNavigationTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { currentTab = MainNavigationTab.SETTINGS },
                    onNavigateToAiConfig = { currentTab = MainNavigationTab.AI_CONFIG }
                )
                MainNavigationTab.CONVERSATIONS -> ConversationsScreen(viewModel = viewModel)
                MainNavigationTab.BLACKLIST -> BlacklistScreen(viewModel = viewModel)
                MainNavigationTab.AI_CONFIG -> AiConfigScreen(viewModel = viewModel)
                MainNavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
