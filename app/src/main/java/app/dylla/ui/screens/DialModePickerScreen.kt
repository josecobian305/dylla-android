package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.FundingStage
import app.dylla.ui.theme.*

enum class SetupStep {
    PICK_MODE, TWILIO_SETUP, VOICE_SETUP, READY
}

data class TwilioConfig(
    var accountSID: String = "",
    var authToken: String = "",
    var phoneNumber: String = "",
    var twilioNumbers: List<String> = emptyList(),
    var linesPerBatch: Int = 5
) {
    val isConfigured: Boolean
        get() = accountSID.isNotBlank() && authToken.isNotBlank() && phoneNumber.isNotBlank()

    companion object {
        fun load(): TwilioConfig = TwilioConfig()
    }
}

data class VoiceDrop(
    val name: String,
    val isDefault: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialModePickerScreen(
    pendingCount: Int,
    stages: List<FundingStage>,
    onStart: (isPowerDialer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(SetupStep.PICK_MODE) }
    var twilioConfig by remember { mutableStateOf(TwilioConfig.load()) }
    var numbersText by remember { mutableStateOf(twilioConfig.twilioNumbers.joinToString("\n")) }
    var showRecorder by remember { mutableStateOf(false) }
    var defaultVoiceDrop by remember { mutableStateOf<VoiceDrop?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dial Mode") },
                navigationIcon = {
                    if (step != SetupStep.PICK_MODE) {
                        IconButton(onClick = {
                            step = when (step) {
                                SetupStep.TWILIO_SETUP -> SetupStep.PICK_MODE
                                SetupStep.VOICE_SETUP -> SetupStep.TWILIO_SETUP
                                SetupStep.READY -> SetupStep.VOICE_SETUP
                                else -> SetupStep.PICK_MODE
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onDismiss) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                SetupStep.PICK_MODE -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose Dial Mode",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "$pendingCount contacts ready to dial",
                        fontSize = 14.sp,
                        color = DyllaOnSurfaceSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStart(false) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DyllaGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = DyllaGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manual Dialer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = DyllaOnSurface
                                )
                                Text(
                                    text = "Dial one contact at a time with full control",
                                    fontSize = 13.sp,
                                    color = DyllaOnSurfaceSecondary
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DyllaOnSurfaceSecondary
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (twilioConfig.isConfigured) {
                                    step = SetupStep.READY
                                } else {
                                    step = SetupStep.TWILIO_SETUP
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DyllaBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = DyllaBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Power Dialer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = DyllaOnSurface
                                )
                                Text(
                                    text = "Auto-dial multiple lines with voice drops",
                                    fontSize = 13.sp,
                                    color = DyllaOnSurfaceSecondary
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DyllaOnSurfaceSecondary
                            )
                        }
                    }
                }

                SetupStep.TWILIO_SETUP -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(DyllaBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DyllaBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Twilio Setup",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DyllaOnSurface
                        )
                    }

                    OutlinedTextField(
                        value = twilioConfig.accountSID,
                        onValueChange = { twilioConfig = twilioConfig.copy(accountSID = it) },
                        label = { Text("Twilio Account SID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = twilioConfig.authToken,
                        onValueChange = { twilioConfig = twilioConfig.copy(authToken = it) },
                        label = { Text("Twilio Auth Token") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = twilioConfig.phoneNumber,
                        onValueChange = { twilioConfig = twilioConfig.copy(phoneNumber = it) },
                        label = { Text("Your Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = numbersText,
                        onValueChange = {
                            numbersText = it
                            twilioConfig = twilioConfig.copy(
                                twilioNumbers = it.lines().filter { line -> line.isNotBlank() }
                            )
                        },
                        label = { Text("Twilio Numbers (one per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lines Per Batch",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = DyllaOnSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (twilioConfig.linesPerBatch > 1) {
                                            twilioConfig = twilioConfig.copy(
                                                linesPerBatch = twilioConfig.linesPerBatch - 1
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DyllaBackgroundSecondary)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = DyllaOnSurface
                                    )
                                }
                                Text(
                                    text = "${twilioConfig.linesPerBatch}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DyllaOnSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(
                                    onClick = {
                                        if (twilioConfig.linesPerBatch < 20) {
                                            twilioConfig = twilioConfig.copy(
                                                linesPerBatch = twilioConfig.linesPerBatch + 1
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DyllaBackgroundSecondary)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = DyllaOnSurface
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { step = SetupStep.VOICE_SETUP },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                        enabled = twilioConfig.isConfigured
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                SetupStep.VOICE_SETUP -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(DyllaBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DyllaBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voice Drop Setup",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DyllaOnSurface
                        )
                    }

                    defaultVoiceDrop?.let { drop ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = DyllaGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = drop.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = DyllaOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { isPlaying = !isPlaying }) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop" else "Play",
                                        tint = DyllaBlue
                                    )
                                }
                            }
                        }
                    } ?: run {
                        OutlinedButton(
                            onClick = { showRecorder = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DyllaBlue)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Voice Note",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaOrange.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "💡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Voice Drop Tips",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = DyllaOrange
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Record a short voicemail message that will be automatically dropped when a call goes to voicemail. Keep it under 30 seconds for best results.",
                                    fontSize = 13.sp,
                                    color = DyllaOnSurface
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { step = SetupStep.READY },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                SetupStep.READY -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ready to Dial",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = DyllaGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Twilio Configured",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DyllaOnSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (defaultVoiceDrop != null) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (defaultVoiceDrop != null) DyllaGreen else DyllaOnSurfaceSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (defaultVoiceDrop != null) "Voice Note Ready" else "No Voice Note",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DyllaOnSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = DyllaBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$pendingCount Contacts",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DyllaOnSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onStart(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue)
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Power Dialer",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
