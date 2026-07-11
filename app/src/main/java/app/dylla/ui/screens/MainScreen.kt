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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dylla.models.*
import app.dylla.ui.theme.DyllaBlue
import app.dylla.ui.theme.DyllaOnSurfaceSecondary
import app.dylla.viewmodels.DialerViewModel

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
    val vm: DialerViewModel = viewModel()

    // Overlay screens
    var showImport by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showCompanySwitcher by remember { mutableStateOf(false) }
    var showStageEditor by remember { mutableStateOf(false) }
    var showActiveCall by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dylla_prefs", android.content.Context.MODE_PRIVATE) }
    val userEmail = remember { prefs.getString("dylla_user_email", "") ?: "" }

    if (showImport) {
        ImportScreen(
            onImport = { listName, contacts ->
                vm.importCSV("", listName)
                showImport = false
            },
            onDone = { showImport = false }
        )
    } else if (showListPicker) {
        ListPickerScreen(
            onSelectList = { vm.selectList(it); showListPicker = false },
            onBack = { showListPicker = false }
        )
    } else if (showCompanySwitcher) {
        CompanySwitcherScreen(
            onBack = { showCompanySwitcher = false }
        )
    } else if (showStageEditor) {
        StageEditorScreen(
            onSave = { vm.updateStages(it); showStageEditor = false },
            onBack = { showStageEditor = false }
        )
    } else if (showActiveCall) {
        ActiveCallScreen(
            contacts = vm.activeList?.contacts ?: emptyList(),
            listName = vm.activeList?.name ?: "No List",
            stages = vm.stages,
            onEndSession = { showActiveCall = false }
        )
    } else {
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
                        activeList = vm.activeList,
                        activeCompany = vm.activeCompany,
                        stages = vm.stages,
                        onImport = { showImport = true },
                        onListPicker = { showListPicker = true },
                        onCompanySwitcher = { showCompanySwitcher = true },
                        onStartCalling = { showActiveCall = true }
                    )
                    BottomTab.CONTACTS -> ContactsScreen(
                        activeList = vm.activeList,
                        stages = vm.stages
                    )
                    BottomTab.CALLBACKS -> CallbacksScreen(
                        lists = vm.lists,
                        stages = vm.stages
                    )
                    BottomTab.TRAINING -> TrainingScreen()
                    BottomTab.SETTINGS -> SettingsScreen(
                        onLogout = onLogout,
                        onEditStages = { showStageEditor = true },
                        onManageCompanies = { showCompanySwitcher = true },
                        userEmail = userEmail
                    )
                }
            }
        }
    }
}
