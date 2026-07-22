package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.Contact
import app.dylla.services.LocalPresenceService
import app.dylla.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPresenceScreen(
    contact: Contact,
    uid: String,
    onCallStarted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var calling by remember { mutableStateOf(false) }
    val presence = remember { LocalPresenceService() }
    val scope = rememberCoroutineScope()

    val contactDigits = remember(contact.phone) {
        val stripped = contact.phone.filter { it.isDigit() }
        if (stripped.length == 10) "1$stripped" else stripped
    }
    val areaCode = if (contactDigits.length >= 4) contactDigits.substring(1, 4) else ""
    val matched = presence.matchedNumber("+$contactDigits")

    LaunchedEffect(Unit) {
        presence.loadNumbers(uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Presence") },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DyllaBlue)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = formatPresencePhone(contact.phone),
                        fontSize = 15.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DyllaBlue
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
            }

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
                    colors = CardDefaults.cardColors(containerColor = DyllaBlue.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = matched.formatted,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DyllaBlue
                        )
                        Button(
                            onClick = {
                                calling = true
                                scope.launch {
                                    val sid = presence.bridgeCall(
                                        uid = uid,
                                        spoofNumber = matched.number,
                                        prospectPhone = "+$contactDigits",
                                        prospectName = contact.name
                                    )
                                    calling = false
                                    if (sid != null) {
                                        onCallStarted(sid)
                                        onDismiss()
                                    }
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
                            Text("Call ${contact.name}")
                        }
                    }
                }
            } else if (areaCode.isNotEmpty()) {
                Button(
                    onClick = {
                        calling = true
                        scope.launch {
                            val number = presence.getOrBuyNumber(uid, "+$contactDigits")
                            if (number != null) {
                                val sid = presence.bridgeCall(
                                    uid = uid,
                                    spoofNumber = number.number,
                                    prospectPhone = "+$contactDigits",
                                    prospectName = contact.name
                                )
                                calling = false
                                if (sid != null) {
                                    onCallStarted(sid)
                                    onDismiss()
                                }
                            } else {
                                calling = false
                            }
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

            if (presence.numbers.isNotEmpty()) {
                Text(
                    text = "Number Pool",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presence.numbers.forEach { number ->
                        val matchesArea = number.areaCode == areaCode
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = number.formatted,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = DyllaOnSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = (if (matchesArea) DyllaGreen else DyllaBlue).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = number.areaCode,
                                            fontSize = 11.sp,
                                            color = if (matchesArea) DyllaGreen else DyllaBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            calling = true
                                            scope.launch {
                                                val sid = presence.bridgeCall(
                                                    uid = uid,
                                                    spoofNumber = number.number,
                                                    prospectPhone = "+$contactDigits",
                                                    prospectName = contact.name
                                                )
                                                calling = false
                                                if (sid != null) {
                                                    onCallStarted(sid)
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        enabled = !calling
                                    ) {
                                        Icon(
                                            Icons.Filled.Call,
                                            contentDescription = "Call",
                                            tint = if (matchesArea) DyllaGreen else DyllaBlue
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                presence.releaseNumber(uid, number.id)
                                            }
                                        }
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
            }
        }
    }
}

private fun formatPresencePhone(number: String): String {
    val digits = number.filter { it.isDigit() }
    val national = if (digits.startsWith("1") && digits.length == 11) digits.substring(1) else digits
    return if (national.length == 10) {
        "(${national.substring(0, 3)}) ${national.substring(3, 6)}-${national.substring(6)}"
    } else {
        number
    }
}
