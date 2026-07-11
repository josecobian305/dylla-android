package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.dylla.models.*
import app.dylla.ui.theme.DyllaBlue
import app.dylla.ui.theme.DyllaOnSurfaceSecondary

private enum class BottomTab(
    val label: String,
    val icon: ImageVector
) {
    DIALER("Dialer", Icons.Filled.Phone),
    CONTACTS("Contacts", Icons.Filled.People),
    CALLBACKS("Callbacks", Icons.Filled.DateRange),
    TRAINING("Training", Icons.Filled.EmojiEvents),
    SETTINGS("Settings", Icons.Filled.Settings);
}

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.DIALER) }

    // Shared state
    var activeList by remember { mutableStateOf<CallList?>(null) }
    var lists by remember { mutableStateOf(listOf<CallList>()) }
    var stages by remember { mutableStateOf(FundingStage.defaults) }
    var activeCompany by remember { mutableStateOf<Company?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DyllaBlue,
                            selectedTextColor = DyllaBlue,
                            unselectedIconColor = DyllaOnSurfaceSecondary,
                            unselectedTextColor = DyllaOnSurfaceSecondary,
                            indicatorColor = DyllaBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomTab.DIALER -> DashboardScreen(
                    activeList = activeList,
                    activeCompany = activeCompany,
                    stages = stages
                )
                BottomTab.CONTACTS -> ContactsScreen(
                    activeList = activeList,
                    stages = stages
                )
                BottomTab.CALLBACKS -> CallbacksScreen(
                    lists = lists,
                    stages = stages
                )
                BottomTab.TRAINING -> TrainingScreen()
                BottomTab.SETTINGS -> SettingsScreen(onLogout = onLogout)
            }
        }
    }
}
