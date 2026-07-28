package app.dylla.ui.screens

import androidx.compose.animation.AnimatedVisibility
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

    // SHAKEN/STIR form state
    var showTrustForm by remember { mutableStateOf(false) }
    var trustBusinessName by remember { mutableStateOf("") }
    var trustBusinessType by remember { mutableStateOf("") }
    var trustEin by remember { mutableStateOf("") }
    var trustStreet by remember { mutableStateOf("") }
    var trustCity by remember { mutableStateOf("") }
    var trustState by remember { mutableStateOf("") }
    var trustZip by remember { mutableStateOf("") }
    var trustPhone by remember { mutableStateOf("") }
    var trustEmail by remember { mutableStateOf("") }
    var trustWebsite by remember { mutableStateOf("") }
    var businessTypeExpanded by remember { mutableStateOf(false) }
    val businessTypes = listOf(
        "Sole Proprietorship", "Partnership", "LLC",
        "Corporation", "Non-Profit", "Other"
    )

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
        presence.loadSavedSettings(context)
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

            // ── CNAM Section ──
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Caller ID Name (CNAM)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "Business name that shows instead of Spam Likely",
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                    if (presence.savedCnam.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DyllaGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Active: ${presence.savedCnam}",
                                color = DyllaGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = presence.cnam,
                        onValueChange = { if (it.length <= 15) presence.cnam = it },
                        label = { Text("Business Name") },
                        placeholder = { Text("e.g. SMB Capital") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = "${presence.cnam.length}/15 characters",
                                fontSize = 12.sp,
                                color = if (presence.cnam.length >= 15) DyllaOrange else DyllaOnSurfaceSecondary
                            )
                        }
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                presence.saveCnam(uid, presence.cnam)
                            }
                        },
                        enabled = presence.cnam.isNotEmpty() && !presence.cnamSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (presence.cnamSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DyllaOnSurface,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Apply to All Numbers")
                    }
                }
            }

            // ── SHAKEN/STIR Section ──
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SHAKEN/STIR Registration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "Register for A-level call attestation",
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary
                    )

                    // Status badge
                    when (presence.trustStatus) {
                        "twilio-approved" -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DyllaGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Verified",
                                    color = DyllaGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        "pending-review" -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DyllaOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Pending Review",
                                    color = DyllaOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        "twilio-rejected" -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DyllaRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Rejected",
                                    color = DyllaRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // No profile yet: show register button or form
                    if (presence.trustStatus == null) {
                        if (!showTrustForm) {
                            Button(
                                onClick = { showTrustForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Register Business Identity")
                            }
                        }
                    }

                    // Registration form
                    AnimatedVisibility(visible = showTrustForm && (presence.trustStatus == null || presence.trustStatus == "twilio-rejected")) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = trustBusinessName,
                                onValueChange = { trustBusinessName = it },
                                label = { Text("Business Name") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Business Type dropdown
                            ExposedDropdownMenuBox(
                                expanded = businessTypeExpanded,
                                onExpandedChange = { businessTypeExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = trustBusinessType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Business Type") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = businessTypeExpanded)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = businessTypeExpanded,
                                    onDismissRequest = { businessTypeExpanded = false }
                                ) {
                                    businessTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                trustBusinessType = type
                                                businessTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = trustEin,
                                onValueChange = { trustEin = it },
                                label = { Text("EIN") },
                                placeholder = { Text("XX-XXXXXXX") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = trustStreet,
                                onValueChange = { trustStreet = it },
                                label = { Text("Street Address") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = trustCity,
                                    onValueChange = { trustCity = it },
                                    label = { Text("City") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = trustState,
                                    onValueChange = { if (it.length <= 2) trustState = it.uppercase() },
                                    label = { Text("State") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.width(80.dp),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = trustZip,
                                onValueChange = { if (it.length <= 5) trustZip = it },
                                label = { Text("ZIP Code") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = trustPhone,
                                onValueChange = { trustPhone = it },
                                label = { Text("Business Phone") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            OutlinedTextField(
                                value = trustEmail,
                                onValueChange = { trustEmail = it },
                                label = { Text("Business Email") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )

                            OutlinedTextField(
                                value = trustWebsite,
                                onValueChange = { trustWebsite = it },
                                label = { Text("Website") },
                                placeholder = { Text("https://example.com") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                            )

                            val formValid = trustBusinessName.isNotEmpty() &&
                                trustBusinessType.isNotEmpty() &&
                                trustEin.isNotEmpty() &&
                                trustStreet.isNotEmpty() &&
                                trustCity.isNotEmpty() &&
                                trustState.isNotEmpty() &&
                                trustZip.isNotEmpty() &&
                                trustPhone.isNotEmpty() &&
                                trustEmail.isNotEmpty()

                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = presence.registerTrust(
                                            uid = uid,
                                            businessName = trustBusinessName,
                                            businessType = trustBusinessType,
                                            ein = trustEin,
                                            street = trustStreet,
                                            city = trustCity,
                                            state = trustState,
                                            zip = trustZip,
                                            phone = trustPhone,
                                            email = trustEmail,
                                            website = trustWebsite
                                        )
                                        if (ok) showTrustForm = false
                                    }
                                },
                                enabled = formValid && !presence.trustLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (presence.trustLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DyllaOnSurface,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Submit for Verification")
                            }
                        }
                    }

                    // Pending: check status button
                    if (presence.trustStatus == "pending-review") {
                        Button(
                            onClick = {
                                scope.launch { presence.checkTrustStatus(uid) }
                            },
                            enabled = !presence.trustLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DyllaOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (presence.trustLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = DyllaOnSurface,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Check Status")
                        }
                    }

                    // Approved: assign numbers
                    if (presence.trustStatus == "twilio-approved") {
                        if (presence.trustNumbersAssigned != null && presence.trustNumbersAssigned!! > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DyllaGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${presence.trustNumbersAssigned} numbers assigned",
                                    color = DyllaGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch { presence.assignNumbers(uid) }
                            },
                            enabled = !presence.trustLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DyllaGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (presence.trustLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = DyllaOnSurface,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Assign All Numbers")
                        }
                    }

                    // Rejected: show reason + resubmit
                    if (presence.trustStatus == "twilio-rejected") {
                        if (!presence.trustFailureReason.isNullOrEmpty()) {
                            Text(
                                text = "Reason: ${presence.trustFailureReason}",
                                fontSize = 13.sp,
                                color = DyllaRed
                            )
                        }
                        if (!showTrustForm) {
                            Button(
                                onClick = { showTrustForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DyllaRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Resubmit")
                            }
                        }
                    }

                    // Show error if any
                    if (presence.error != null && (presence.trustLoading || presence.cnamSaving).not()) {
                        Text(
                            text = presence.error ?: "",
                            color = DyllaRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

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
