package app.dylla.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.*
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallbacksScreen(
    lists: List<CallList>,
    stages: List<FundingStage>
) {
    val context = LocalContext.current
    val now = remember { Date() }

    val callbacks = remember(lists) {
        lists.flatMap { it.contacts }
            .filter { it.outcome == CallOutcome.CALLBACK && it.callbackDate != null }
            .sortedBy { it.callbackDate }
    }

    val overdueCallbacks = remember(callbacks) {
        callbacks.filter { (it.callbackDate ?: Date(Long.MAX_VALUE)).before(now) }
    }

    val upcomingCallbacks = remember(callbacks) {
        callbacks.filter { !(it.callbackDate ?: Date(Long.MAX_VALUE)).before(now) }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Callbacks",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        },
        containerColor = DyllaBackground
    ) { innerPadding ->
        if (callbacks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = DyllaOnSurfaceSecondary
                    )
                    Text(
                        text = "No Callbacks",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "Schedule callbacks during calls.",
                        fontSize = 14.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Overdue section
                if (overdueCallbacks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Overdue",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DyllaOnSurfaceSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(overdueCallbacks, key = { it.id }) { contact ->
                        CallbackRow(
                            contact = contact,
                            stages = stages,
                            overdue = true,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            onDial = { phone ->
                                val digits = phone.filter { it.isDigit() }
                                if (digits.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
                                    context.startActivity(intent)
                                }
                            }
                        )
                        HorizontalDivider(
                            color = DyllaBackgroundSecondary,
                            thickness = 0.5.dp
                        )
                    }
                }

                // Upcoming section
                if (upcomingCallbacks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DyllaOnSurfaceSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(upcomingCallbacks, key = { it.id }) { contact ->
                        CallbackRow(
                            contact = contact,
                            stages = stages,
                            overdue = false,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            onDial = { phone ->
                                val digits = phone.filter { it.isDigit() }
                                if (digits.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
                                    context.startActivity(intent)
                                }
                            }
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

@Composable
private fun CallbackRow(
    contact: Contact,
    stages: List<FundingStage>,
    overdue: Boolean,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onDial: (String) -> Unit
) {
    val stage = contact.stage(stages)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stage emoji + short label
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stage?.emoji ?: "📋",
                fontSize = 20.sp
            )
            Text(
                text = stage?.shortLabel ?: "",
                fontSize = 8.sp,
                color = DyllaOnSurfaceSecondary,
                maxLines = 1
            )
        }

        // Contact info
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
            if (contact.callNotes.isNotEmpty()) {
                Text(
                    text = contact.callNotes,
                    fontSize = 11.sp,
                    color = DyllaBlue,
                    maxLines = 1
                )
            }
        }

        // Date + time
        Column(horizontalAlignment = Alignment.End) {
            contact.callbackDate?.let { date ->
                Text(
                    text = dateFormat.format(date),
                    fontSize = 11.sp,
                    color = if (overdue) DyllaRed else DyllaOnSurfaceSecondary
                )
                Text(
                    text = timeFormat.format(date),
                    fontSize = 11.sp,
                    color = if (overdue) DyllaRed else DyllaOnSurfaceSecondary
                )
            }
        }

        // Green phone button
        IconButton(
            onClick = { onDial(contact.phone) },
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = DyllaGreen,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = "Call",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
