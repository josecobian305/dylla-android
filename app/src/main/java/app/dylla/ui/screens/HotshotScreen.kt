package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.services.LocalPresenceService
import app.dylla.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotshotScreen(onDismiss: () -> Unit) {
    var phoneInput by remember { mutableStateOf("") }
    var prospectName by remember { mutableStateOf("") }
    var calling by remember { mutableStateOf(false) }
    var callSid by remember { mutableStateOf<String?>(null) }
    var callCompleted by remember { mutableStateOf(false) }
    val presence = remember { LocalPresenceService() }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dylla_prefs", android.content.Context.MODE_PRIVATE) }
    val uid = remember { prefs.getString("dylla_user_uid", "") ?: "" }

    val cleanDigits = remember(phoneInput) {
        val stripped = phoneInput.filter { it.isDigit() }
        if (stripped.length == 10) "1$stripped" else stripped
    }
    val isValidPhone = cleanDigits.length == 11 && cleanDigits.startsWith("1")
    val areaCode = if (cleanDigits.length >= 4) cleanDigits.substring(1, 4) else ""
    val matched = presence.matchedNumber("+$cleanDigits")

    LaunchedEffect(Unit) {
        presence.loadNumbers(uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hotshot Dialer") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    phoneInput = it
                    callCompleted = false
                    callSid = null
                },
                label = { Text("Phone Number") },
                placeholder = { Text("(555) 123-4567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = prospectName,
                onValueChange = { prospectName = it },
                label = { Text("Prospect Name (optional)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (isValidPhone && areaCode.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DyllaBlue,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Area Code: $areaCode",
                        color = DyllaOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            if (callCompleted && callSid != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaGreen.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = DyllaGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("Call Completed", fontWeight = FontWeight.Bold, color = DyllaGreen)
                        Text(
                            text = callSid ?: "",
                            fontSize = 12.sp,
                            color = DyllaOnSurfaceSecondary
                        )
                        Button(
                            onClick = {
                                phoneInput = ""
                                prospectName = ""
                                callCompleted = false
                                callSid = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("New Call")
                        }
                    }
                }
            } else if (isValidPhone) {
                if (presence.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DyllaBlue)
                    }
                } else if (presence.error != null) {
                    Text(
                        text = presence.error ?: "Unknown error",
                        color = DyllaRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else if (matched != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaGreen.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Match Found", fontWeight = FontWeight.Bold, color = DyllaGreen)
                            Text(
                                text = matched.formatted,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = DyllaOnSurface
                            )
                            Button(
                                onClick = {
                                    calling = true
                                    scope.launch {
                                        val sid = presence.bridgeCall(
                                            uid = uid,
                                            spoofNumber = matched.number,
                                            prospectPhone = "+$cleanDigits",
                                            prospectName = prospectName.ifBlank { "Unknown" }
                                        )
                                        if (sid != null) {
                                            callSid = sid
                                            callCompleted = true
                                        }
                                        calling = false
                                    }
                                },
                                enabled = !calling,
                                colors = ButtonDefaults.buttonColors(containerColor = DyllaGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (calling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DyllaOnSurface,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Icons.Filled.Call, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call")
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaOrange.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No local number for ($areaCode)",
                                color = DyllaOrange,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = {
                                    calling = true
                                    scope.launch {
                                        val number = presence.getOrBuyNumber(uid, "+$cleanDigits")
                                        if (number != null) {
                                            val sid = presence.bridgeCall(
                                                uid = uid,
                                                spoofNumber = number.number,
                                                prospectPhone = "+$cleanDigits",
                                                prospectName = prospectName.ifBlank { "Unknown" }
                                            )
                                            if (sid != null) {
                                                callSid = sid
                                                callCompleted = true
                                            }
                                        }
                                        calling = false
                                    }
                                },
                                enabled = !calling,
                                colors = ButtonDefaults.buttonColors(containerColor = DyllaOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (calling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DyllaOnSurface,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Buy ($areaCode) Number — \$1.15/mo")
                            }
                        }
                    }
                }
            }

            if (presence.numbers.isNotEmpty()) {
                Text(
                    text = "Number Pool",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )

                presence.numbers.forEach { number ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = number.formatted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = DyllaOnSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DyllaBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = number.areaCode,
                                            fontSize = 11.sp,
                                            color = DyllaBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${number.callCount} calls",
                                        fontSize = 12.sp,
                                        color = DyllaOnSurfaceSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (isValidPhone) {
                                            calling = true
                                            scope.launch {
                                                val sid = presence.bridgeCall(
                                                    uid = uid,
                                                    spoofNumber = number.number,
                                                    prospectPhone = "+$cleanDigits",
                                                    prospectName = prospectName.ifBlank { "Unknown" }
                                                )
                                                if (sid != null) {
                                                    callSid = sid
                                                    callCompleted = true
                                                }
                                                calling = false
                                            }
                                        }
                                    },
                                    enabled = isValidPhone && !calling
                                ) {
                                    Icon(
                                        Icons.Filled.Call,
                                        contentDescription = "Call",
                                        tint = DyllaGreen
                                    )
                                }
                                IconButton(
                                    onClick = { showDeleteConfirm = number.id }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = DyllaRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm != null) {
        val numberToDelete = presence.numbers.find { it.id == showDeleteConfirm }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Release Number") },
            text = { Text("Release ${numberToDelete?.formatted ?: showDeleteConfirm}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val numberId = showDeleteConfirm!!
                        showDeleteConfirm = null
                        scope.launch {
                            presence.releaseNumber(uid, numberId)
                        }
                    }
                ) {
                    Text("Release", color = DyllaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
