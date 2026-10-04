package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.ActionGuardDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val activeTask by viewModel.taskManager.activeTask.collectAsState()
                val pendingConfirmation = activeTask?.pendingConfirmation

                // Permission Launcher for Microphone & Contacts
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions granted / denied handled gracefully */ }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AuraDarkBackground,
                    contentWindowInsets = WindowInsets(0.dp),
                    bottomBar = {
                        AuraNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.HOME -> HomeScreen(viewModel = viewModel)
                            AppTab.TASKS -> TaskScreen(viewModel = viewModel)
                            AppTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                            AppTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                            AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }

                        // High-Priority AURA Action Guard Confirmation Dialog
                        if (pendingConfirmation != null) {
                            ActionGuardDialog(
                                confirmation = pendingConfirmation,
                                onConfirm = { viewModel.confirmAction() },
                                onCancel = { viewModel.cancelAction() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .background(AuraSurfaceCard)
            .testTag("aura_navigation_bar"),
        containerColor = AuraSurfaceCard,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001F28),
                selectedTextColor = AuraCyanPrimary,
                indicatorColor = AuraCyanPrimary,
                unselectedIconColor = AuraTextMuted,
                unselectedTextColor = AuraTextMuted
            ),
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = selectedTab == AppTab.TASKS,
            onClick = { onTabSelected(AppTab.TASKS) },
            icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Tasks") },
            label = { Text("Tasks", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001F28),
                selectedTextColor = AuraCyanPrimary,
                indicatorColor = AuraCyanPrimary,
                unselectedIconColor = AuraTextMuted,
                unselectedTextColor = AuraTextMuted
            ),
            modifier = Modifier.testTag("nav_tasks")
        )

        NavigationBarItem(
            selected = selectedTab == AppTab.TOOLS,
            onClick = { onTabSelected(AppTab.TOOLS) },
            icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
            label = { Text("Tools", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001F28),
                selectedTextColor = AuraCyanPrimary,
                indicatorColor = AuraCyanPrimary,
                unselectedIconColor = AuraTextMuted,
                unselectedTextColor = AuraTextMuted
            ),
            modifier = Modifier.testTag("nav_tools")
        )

        NavigationBarItem(
            selected = selectedTab == AppTab.MEMORY,
            onClick = { onTabSelected(AppTab.MEMORY) },
            icon = { Icon(Icons.Default.Psychology, contentDescription = "Memory") },
            label = { Text("Memory", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001F28),
                selectedTextColor = AuraCyanPrimary,
                indicatorColor = AuraCyanPrimary,
                unselectedIconColor = AuraTextMuted,
                unselectedTextColor = AuraTextMuted
            ),
            modifier = Modifier.testTag("nav_memory")
        )

        NavigationBarItem(
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001F28),
                selectedTextColor = AuraCyanPrimary,
                indicatorColor = AuraCyanPrimary,
                unselectedIconColor = AuraTextMuted,
                unselectedTextColor = AuraTextMuted
            ),
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
