package app.dylla.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.*
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPickerScreen(
    onSelectList: (CallList) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val sampleLists = remember { generateSampleLists() }
    var lists by remember { mutableStateOf(sampleLists) }
    var activeListId by remember { mutableStateOf(sampleLists.firstOrNull()?.id ?: "") }
    var listToDelete by remember { mutableStateOf<CallList?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Lists",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DyllaOnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DyllaBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        },
        containerColor = DyllaBackground
    ) { padding ->
        if (lists.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.List,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = DyllaOnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Lists Yet",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Import contacts to create your first list",
                    fontSize = 15.sp,
                    color = DyllaOnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* Import action */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DyllaBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import Contacts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = lists,
                    key = { it.id }
                ) { callList ->
                    val isActive = callList.id == activeListId
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                listToDelete = callList
                                false // Don't dismiss yet; wait for dialog confirmation
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> DyllaRed
                                    else -> Color.Transparent
                                },
                                label = "swipe-bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        CallListCard(
                            callList = callList,
                            isActive = isActive,
                            onClick = {
                                activeListId = callList.id
                                onSelectList(callList)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = {
                Text(
                    text = "Delete List",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${list.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deletedId = list.id
                        lists = lists.filter { it.id != deletedId }
                        if (activeListId == deletedId) {
                            activeListId = lists.firstOrNull()?.id ?: ""
                        }
                        listToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DyllaRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DyllaSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun CallListCard(
    callList: CallList,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val progressPercent = (callList.progress * 100).toInt()
    val activeBackground = if (isActive) DyllaBlue.copy(alpha = 0.05f) else DyllaSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = activeBackground,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Active indicator: blue left border
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            color = DyllaBlue,
                            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Row 1: List name + contact count badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = callList.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DyllaBackgroundSecondary
                    ) {
                        Text(
                            text = "${callList.totalCount} contacts",
                            fontSize = 12.sp,
                            color = DyllaOnSurfaceSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 2: Progress bar + percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { callList.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = DyllaBlue,
                        trackColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${progressPercent}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaBlue
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Completed count
                Text(
                    text = "${callList.doneCount}/${callList.totalCount} completed",
                    fontSize = 12.sp,
                    color = DyllaOnSurfaceSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3: Created date
                Text(
                    text = dateFormatter.format(Date(callList.createdAt)),
                    fontSize = 12.sp,
                    color = DyllaOnSurfaceSecondary
                )
            }
        }
    }
}

// -- Sample data generation --

private fun generateSampleLists(): List<CallList> {
    val now = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L

    return listOf(
        createSampleList(
            name = "Monday Leads",
            totalContacts = 25,
            doneRatio = 0.60f,
            createdAt = now - 3 * oneDay
        ),
        createSampleList(
            name = "Hot Prospects",
            totalContacts = 12,
            doneRatio = 0.25f,
            createdAt = now - 1 * oneDay
        ),
        createSampleList(
            name = "Restaurant Owners",
            totalContacts = 50,
            doneRatio = 0.10f,
            createdAt = now - 7 * oneDay
        )
    )
}

private fun createSampleList(
    name: String,
    totalContacts: Int,
    doneRatio: Float,
    createdAt: Long
): CallList {
    val doneCount = (totalContacts * doneRatio).toInt()
    val doneOutcomes = listOf(
        CallOutcome.ANSWERED,
        CallOutcome.NO_ANSWER,
        CallOutcome.CALLBACK,
        CallOutcome.DNC,
        CallOutcome.SKIPPED
    )

    val contacts = (1..totalContacts).map { index ->
        val outcome = if (index <= doneCount) {
            doneOutcomes[index % doneOutcomes.size]
        } else {
            CallOutcome.PENDING
        }
        Contact(
            name = "Contact $index",
            phone = "(555) ${100 + index}-${1000 + index}",
            businessName = "$name Biz $index",
            outcome = outcome
        )
    }.toMutableList()

    return CallList(
        name = name,
        contacts = contacts,
        createdAt = createdAt
    )
}
