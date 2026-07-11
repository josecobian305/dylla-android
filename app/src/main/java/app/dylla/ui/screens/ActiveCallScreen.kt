package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.*
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private sealed class Phase {
    data object PreCall : Phase()
    data object PostCall : Phase()
}

private fun outcomeColor(colorName: String): Color = when (colorName) {
    "blue" -> DyllaBlue
    "green" -> DyllaGreen
    "orange" -> DyllaOrange
    "purple" -> DyllaPurple
    "red" -> DyllaRed
    "gray" -> Color(0xFF8E8E93)
    else -> DyllaBlue
}

private fun stageColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        DyllaBlue
    }
}

private fun sampleContacts(): List<Contact> = listOf(
    Contact(
        name = "Marcus Rivera",
        phone = "(305) 555-0142",
        businessName = "Rivera Auto Parts LLC",
        notes = "Interested in equipment financing"
    ),
    Contact(
        name = "Tanya Brooks",
        phone = "(718) 555-0198",
        businessName = "Brooklyn Wellness Spa",
        stageID = "stage_contacted"
    ),
    Contact(
        name = "James Whitfield",
        phone = "(404) 555-0267",
        businessName = "Whitfield Construction Co",
        notes = "Previous callback, needs 50K for materials"
    ),
    Contact(
        name = "Diana Chen",
        phone = "(415) 555-0331",
        businessName = "Golden Gate Imports",
        stageID = "stage_qualified"
    ),
    Contact(
        name = "Omar Hassan",
        phone = "(212) 555-0475",
        businessName = "Hassan Logistics Inc"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
    contacts: List<Contact> = sampleContacts(),
    listName: String = "Sample List",
    isPowerDialer: Boolean = false,
    stages: List<FundingStage> = FundingStage.defaults,
    onEndSession: () -> Unit = {}
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf<Phase>(Phase.PreCall) }
    var selectedOutcome by remember { mutableStateOf(CallOutcome.PENDING) }
    var callNotes by remember { mutableStateOf("") }
    var selectedStageId by remember { mutableStateOf<String?>(null) }
    var callbackDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var stageDropdownExpanded by remember { mutableStateOf(false) }

    val currentContact = contacts.getOrNull(currentIndex)
    val isLastContact = currentIndex >= contacts.size - 1
    val progress = if (contacts.isNotEmpty()) (currentIndex + 1).toFloat() / contacts.size else 0f

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    fun resetPostCallState() {
        selectedOutcome = CallOutcome.PENDING
        callNotes = ""
        selectedStageId = null
        callbackDate = null
    }

    fun advanceToNext() {
        if (currentContact != null) {
            currentContact.outcome = selectedOutcome
            currentContact.callNotes = callNotes
            currentContact.stageID = selectedStageId ?: currentContact.stageID
            if (selectedOutcome == CallOutcome.CALLBACK && callbackDate != null) {
                currentContact.callbackDate = Date(callbackDate!!)
            }
        }

        if (isLastContact) {
            onEndSession()
        } else {
            currentIndex++
            resetPostCallState()
            phase = Phase.PreCall
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = callbackDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        callbackDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = DyllaBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = DyllaBlue,
                trackColor = DyllaBackgroundSecondary
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentIndex + 1} of ${contacts.size}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurfaceSecondary
                )

                Text(
                    text = listName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface
                )

                TextButton(
                    onClick = onEndSession,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = DyllaRed
                    )
                ) {
                    Text(
                        text = "End Session",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Power Dialer badge
            if (isPowerDialer) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DyllaBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Power Dialer",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = DyllaBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Auto-advance enabled",
                        fontSize = 12.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }

            // Main content
            if (currentContact != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    when (phase) {
                        is Phase.PreCall -> PreCallContent(
                            contact = currentContact,
                            stages = stages,
                            onCall = {
                                phase = Phase.PostCall
                            },
                            onSkip = {
                                currentContact.outcome = CallOutcome.SKIPPED
                                if (isLastContact) {
                                    onEndSession()
                                } else {
                                    currentIndex++
                                    resetPostCallState()
                                }
                            },
                            onCallback = {
                                selectedOutcome = CallOutcome.CALLBACK
                                phase = Phase.PostCall
                            }
                        )

                        is Phase.PostCall -> PostCallContent(
                            contact = currentContact,
                            stages = stages,
                            selectedOutcome = selectedOutcome,
                            onOutcomeSelected = { selectedOutcome = it },
                            callNotes = callNotes,
                            onCallNotesChanged = { callNotes = it },
                            selectedStageId = selectedStageId,
                            onStageSelected = { selectedStageId = it },
                            stageDropdownExpanded = stageDropdownExpanded,
                            onStageDropdownExpandedChange = { stageDropdownExpanded = it },
                            callbackDate = callbackDate,
                            dateFormatter = dateFormatter,
                            onShowDatePicker = { showDatePicker = true },
                            isLastContact = isLastContact,
                            onNext = { advanceToNext() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreCallContent(
    contact: Contact,
    stages: List<FundingStage>,
    onCall: () -> Unit,
    onSkip: () -> Unit,
    onCallback: () -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))

    // Contact Card
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false
            ),
        shape = RoundedCornerShape(14.dp),
        color = DyllaSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Name
            Text(
                text = contact.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DyllaOnSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Business name
            if (contact.businessName.isNotBlank()) {
                Text(
                    text = contact.businessName,
                    fontSize = 16.sp,
                    color = DyllaOnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Phone
            Text(
                text = contact.phone,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = DyllaBlue,
                modifier = Modifier.clickable { /* Tap to dial */ }
            )

            // Stage badge
            val contactStage = contact.stage(stages)
            if (contactStage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = stageColor(contactStage.color).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${contactStage.emoji} ${contactStage.name}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = stageColor(contactStage.color)
                    )
                }
            }

            // Notes preview
            if (contact.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = contact.notes,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = DyllaOnSurfaceSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action Buttons Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Call button
        Button(
            onClick = onCall,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DyllaGreen,
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Filled.Call,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Call",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // Skip button
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF8E8E93)
            ),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Skip",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // Callback button
        OutlinedButton(
            onClick = onCallback,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DyllaPurple
            ),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Callback",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCallContent(
    contact: Contact,
    stages: List<FundingStage>,
    selectedOutcome: CallOutcome,
    onOutcomeSelected: (CallOutcome) -> Unit,
    callNotes: String,
    onCallNotesChanged: (String) -> Unit,
    selectedStageId: String?,
    onStageSelected: (String?) -> Unit,
    stageDropdownExpanded: Boolean,
    onStageDropdownExpandedChange: (Boolean) -> Unit,
    callbackDate: Long?,
    dateFormatter: SimpleDateFormat,
    onShowDatePicker: () -> Unit,
    isLastContact: Boolean,
    onNext: () -> Unit
) {
    val selectableOutcomes = CallOutcome.entries.filter { it != CallOutcome.PENDING }
    val selectedStage = stages.firstOrNull { it.id == selectedStageId }

    Spacer(modifier = Modifier.height(8.dp))

    // Contact info summary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DyllaSurface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = contact.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface
                )
                Text(
                    text = contact.phone,
                    fontSize = 15.sp,
                    color = DyllaBlue
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Outcome Picker label
    Text(
        text = "Call Outcome",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = DyllaOnSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    // Outcome Picker Grid
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(selectableOutcomes) { outcome ->
            val isSelected = selectedOutcome == outcome
            val color = outcomeColor(outcome.color)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, color, RoundedCornerShape(14.dp))
                        } else {
                            Modifier.border(1.dp, DyllaOnSurfaceSecondary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        }
                    )
                    .clickable { onOutcomeSelected(outcome) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) color.copy(alpha = 0.08f) else DyllaSurface
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = outcome.emoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = outcome.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) color else DyllaOnSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Call Notes
    OutlinedTextField(
        value = callNotes,
        onValueChange = onCallNotesChanged,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        placeholder = {
            Text(
                text = "Add notes about this call...",
                color = DyllaOnSurfaceSecondary.copy(alpha = 0.5f)
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DyllaBlue,
            unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
            focusedContainerColor = DyllaSurface,
            unfocusedContainerColor = DyllaSurface
        ),
        maxLines = 5
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Stage Dropdown
    Text(
        text = "Stage",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = DyllaOnSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    ExposedDropdownMenuBox(
        expanded = stageDropdownExpanded,
        onExpandedChange = onStageDropdownExpandedChange
    ) {
        OutlinedTextField(
            value = if (selectedStage != null) {
                "${selectedStage.emoji} ${selectedStage.name}"
            } else {
                "Select stage..."
            },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageDropdownExpanded)
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DyllaBlue,
                unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                focusedContainerColor = DyllaSurface,
                unfocusedContainerColor = DyllaSurface
            )
        )

        ExposedDropdownMenu(
            expanded = stageDropdownExpanded,
            onDismissRequest = { onStageDropdownExpandedChange(false) }
        ) {
            stages.forEach { stage ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${stage.emoji} ${stage.name}",
                            fontSize = 15.sp
                        )
                    },
                    onClick = {
                        onStageSelected(stage.id)
                        onStageDropdownExpandedChange(false)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }

    // Callback Date Picker (only if CALLBACK outcome selected)
    if (selectedOutcome == CallOutcome.CALLBACK) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Callback Date",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DyllaOnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = onShowDatePicker,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (callbackDate != null) DyllaOnSurface else DyllaOnSurfaceSecondary
            )
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (callbackDate != null) {
                    dateFormatter.format(Date(callbackDate))
                } else {
                    "Select callback date"
                },
                fontSize = 15.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Next / Finish button
    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DyllaBlue,
            contentColor = Color.White
        )
    ) {
        Text(
            text = if (isLastContact) "Finish Session" else "Next Contact",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActiveCallScreenPreview() {
    DyllaTheme {
        ActiveCallScreen()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActiveCallScreenPowerDialerPreview() {
    DyllaTheme {
        ActiveCallScreen(isPowerDialer = true)
    }
}
