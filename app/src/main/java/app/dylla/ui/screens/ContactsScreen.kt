package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.*
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    activeList: CallList?,
    stages: List<FundingStage>
) {
    var searchText by remember { mutableStateOf("") }
    var filterOutcome by remember { mutableStateOf<CallOutcome?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filtered = remember(activeList, searchText, filterOutcome) {
        val contacts = activeList?.contacts ?: emptyList()
        contacts.filter { c ->
            val matchSearch = searchText.isEmpty()
                    || c.name.contains(searchText, ignoreCase = true)
                    || c.phone.contains(searchText)
                    || c.businessName.contains(searchText, ignoreCase = true)
            val matchFilter = filterOutcome == null || c.outcome == filterOutcome
            matchSearch && matchFilter
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeList?.name ?: "Contacts",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filter",
                                tint = if (filterOutcome != null) DyllaBlue else DyllaOnSurfaceSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    filterOutcome = null
                                    showFilterMenu = false
                                }
                            )
                            HorizontalDivider()
                            CallOutcome.entries.forEach { outcome ->
                                DropdownMenuItem(
                                    text = { Text("${outcome.emoji} ${outcome.label}") },
                                    onClick = {
                                        filterOutcome = outcome
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        },
        containerColor = DyllaBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            if (activeList != null) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search name, phone, business") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = DyllaOnSurfaceSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = DyllaBackgroundSecondary,
                        focusedContainerColor = DyllaBackgroundSecondary
                    )
                )
            }

            when {
                // No list loaded
                activeList == null -> {
                    EmptyState(
                        icon = Icons.Filled.PersonOff,
                        title = "No List Loaded",
                        subtitle = "Import a CSV from the Dashboard."
                    )
                }
                // No matches
                filtered.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No Matches",
                        subtitle = null
                    )
                }
                // Contact list
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered, key = { it.id }) { contact ->
                            ContactRow(
                                contact = contact,
                                timeFormat = timeFormat
                            )
                            HorizontalDivider(
                                color = DyllaBackgroundSecondary,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    timeFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Outcome emoji
        Text(
            text = contact.outcome.emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        // Contact details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name.ifEmpty { contact.phone },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DyllaOnSurface
            )
            if (contact.businessName.isNotEmpty()) {
                Text(
                    text = contact.businessName,
                    fontSize = 12.sp,
                    color = DyllaOnSurfaceSecondary
                )
            }
            Text(
                text = contact.phone,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = DyllaOnSurfaceSecondary
            )
            if (contact.notes.isNotEmpty()) {
                Text(
                    text = contact.notes,
                    fontSize = 11.sp,
                    color = DyllaBlue,
                    maxLines = 2
                )
            }
        }

        // Call time
        if (contact.callTime != null) {
            Text(
                text = timeFormat.format(contact.callTime!!),
                fontSize = 11.sp,
                color = DyllaOnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = DyllaOnSurfaceSecondary
            )
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DyllaOnSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = DyllaOnSurfaceSecondary
                )
            }
        }
    }
}
