package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.CallOutcome
import app.dylla.models.Contact
import app.dylla.models.FundingStage
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class CallbackPreset(val label: String, val timeIntervalMillis: Long) {
    ONE_HOUR("1 Hour", 3_600_000L),
    THREE_HOURS("3 Hours", 10_800_000L),
    TOMORROW("Tomorrow", 86_400_000L),
    THREE_DAYS("3 Days", 259_200_000L),
    ONE_WEEK("1 Week", 604_800_000L),
    CUSTOM("Custom", 0L)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCallScreen(
    contact: Contact,
    stages: List<FundingStage>,
    presetStageID: String?,
    onSave: (outcome: String, stageID: String?, notes: String, callbackDate: Long?, fundedAmount: String?, paymentTerm: String?) -> Unit,
    onTryAnotherNumber: (() -> Unit)? = null,
    onDone: () -> Unit
) {
    var callNotes by remember { mutableStateOf("") }
    var selectedStageID by remember { mutableStateOf(presetStageID) }
    var selectedOutcome by remember { mutableStateOf<CallOutcome?>(null) }
    var showCallbackPicker by remember { mutableStateOf(false) }
    var callbackPreset by remember { mutableStateOf<CallbackPreset?>(null) }
    var customCallbackDate by remember { mutableStateOf<Long?>(null) }
    var fundedAmount by remember { mutableStateOf("") }
    var paymentTerm by remember { mutableStateOf("") }
    var monthFunded by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US) }

    val isRecentlyFunded = stages.find { it.id == selectedStageID }?.name == "Recently Funded"

    val callbackDate: Long? = when (callbackPreset) {
        CallbackPreset.CUSTOM -> customCallbackDate
        null -> null
        else -> System.currentTimeMillis() + (callbackPreset?.timeIntervalMillis ?: 0L)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post-Call Log") },
                actions = {
                    TextButton(onClick = onDone) {
                        Text("Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = contact.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )
                    if (contact.businessName.isNotBlank()) {
                        Text(
                            text = contact.businessName,
                            fontSize = 14.sp,
                            color = DyllaOnSurfaceSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (contact.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = DyllaGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contact.phone,
                                fontSize = 14.sp,
                                color = DyllaOnSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                modifier = Modifier.size(14.dp),
                                tint = DyllaGreen
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Call Notes",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = callNotes,
                    onValueChange = { callNotes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Enter call notes...") },
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 6
                )
                if (contact.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Previous Notes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DyllaOnSurfaceSecondary
                    )
                    Text(
                        text = contact.notes,
                        fontSize = 12.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }

            Column {
                Text(
                    text = "Stage",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stages) { stage ->
                        val isSelected = selectedStageID == stage.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DyllaBlue else DyllaSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStageID = stage.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stage.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stage.shortLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) DyllaBackground else DyllaOnSurface
                                )
                            }
                        }
                    }
                }
            }

            if (isRecentlyFunded) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Deal Details",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = DyllaOnSurface
                        )
                        OutlinedTextField(
                            value = fundedAmount,
                            onValueChange = { fundedAmount = it },
                            label = { Text("Funded Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paymentTerm,
                            onValueChange = { paymentTerm = it },
                            label = { Text("Payment / Term") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = monthFunded,
                            onValueChange = { monthFunded = it },
                            label = { Text("Month Funded") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    DyllaGreen.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(text = "📅", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "45-day follow-up auto-scheduled",
                                fontSize = 13.sp,
                                color = DyllaGreen
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Outcome",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                val outcomes = CallOutcome.entries.filter { it != CallOutcome.PENDING && it != CallOutcome.SKIPPED }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(outcomes) { outcome ->
                        val isSelected = selectedOutcome == outcome
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DyllaGreen else DyllaSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedOutcome = outcome
                                    showCallbackPicker = outcome == CallOutcome.CALLBACK
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = outcome.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = outcome.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) DyllaBackground else DyllaOnSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (selectedOutcome == CallOutcome.CALLBACK) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Schedule Callback",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = DyllaOnSurface
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.heightIn(max = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CallbackPreset.entries.toList()) { preset ->
                                val isSelected = callbackPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DyllaPurple else DyllaBackgroundSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            callbackPreset = preset
                                            if (preset == CallbackPreset.CUSTOM) {
                                                showCallbackPicker = true
                                            }
                                        }
                                ) {
                                    Text(
                                        text = preset.label,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) DyllaBackground else DyllaOnSurface
                                    )
                                }
                            }
                        }

                        if (callbackPreset == CallbackPreset.CUSTOM) {
                            DatePicker(
                                state = datePickerState,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LaunchedEffect(datePickerState.selectedDateMillis) {
                                customCallbackDate = datePickerState.selectedDateMillis
                            }
                        }

                        callbackDate?.let { date ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        DyllaPurple.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(text = "📅", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scheduled: ${dateFormat.format(Date(date))}",
                                    fontSize = 13.sp,
                                    color = DyllaPurple
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onSave(
                            selectedOutcome?.name ?: "",
                            selectedStageID,
                            callNotes,
                            callbackDate,
                            fundedAmount.ifBlank { null },
                            paymentTerm.ifBlank { null }
                        )
                        onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                    enabled = selectedOutcome != null
                ) {
                    Text(
                        text = "Save & Next Call",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
