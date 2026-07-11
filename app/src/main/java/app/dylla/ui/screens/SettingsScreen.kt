package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.FundingStage
import app.dylla.models.UserProfile
import app.dylla.ui.theme.*

@Composable
fun SettingsScreen(
    onManageCompanies: () -> Unit = {},
    onEditSignature: () -> Unit = {},
    onEditStages: () -> Unit = {},
    onWorkflowMap: () -> Unit = {},
    onVoiceDrops: () -> Unit = {},
    onGoogleSheets: () -> Unit = {},
    onLogout: () -> Unit = {},
    userEmail: String = "user@example.com"
) {
    val profile = remember { UserProfile() }
    val stages = remember { FundingStage.defaults }

    // Twilio fields
    var twilioSid by remember { mutableStateOf(profile.twilio.sid) }
    var twilioToken by remember { mutableStateOf(profile.twilio.token) }
    var twilioPhone by remember { mutableStateOf(profile.twilio.number) }
    var twilioNumbers by remember {
        mutableStateOf(profile.twilio.numbers.joinToString("\n"))
    }
    val twilioNumberCount by remember(twilioNumbers) {
        derivedStateOf {
            twilioNumbers.lines().count { it.isNotBlank() }
        }
    }
    val twilioConfigured by remember(twilioSid, twilioToken, twilioPhone) {
        derivedStateOf {
            twilioSid.isNotBlank() && twilioToken.isNotBlank() && twilioPhone.isNotBlank()
        }
    }

    // Lines per batch
    var linesPerBatch by remember { mutableIntStateOf(profile.linesPerBatch) }

    // Training dialer
    var trainingEnabled by remember { mutableStateOf(false) }
    var voiceDropEnabled by remember { mutableStateOf(false) }
    var minSessions by remember { mutableIntStateOf(15) }

    // Google Sheets
    var googleSheetsConnected by remember { mutableStateOf(false) }

    // API Key
    var apiKey by remember { mutableStateOf(profile.apiKey) }

    // Dialogs
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val cardShape = RoundedCornerShape(14.dp)

    // Clear data confirmation dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will permanently delete all lists, contacts, and call history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DyllaRed)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DyllaRed)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DyllaBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── 1. Companies ──
        item { SectionHeader("COMPANIES") }
        item {
            SectionCard(cardShape) {
                NavigableRow(
                    icon = Icons.Filled.Business,
                    iconTint = DyllaBlue,
                    label = "Manage Companies",
                    onClick = onManageCompanies
                )
            }
        }

        // ── 2. Email ──
        item { SectionHeader("EMAIL") }
        item {
            SectionCard(cardShape) {
                NavigableRow(
                    icon = Icons.Filled.Edit,
                    iconTint = DyllaOrange,
                    label = "Email Signature",
                    onClick = onEditSignature
                )
            }
        }

        // ── 3. Integrations ──
        item { SectionHeader("INTEGRATIONS") }
        item {
            SectionCard(cardShape) {
                NavigableRow(
                    icon = Icons.Filled.TableChart,
                    iconTint = DyllaGreen,
                    label = "Google Sheets",
                    onClick = onGoogleSheets,
                    trailing = {
                        Text(
                            text = if (googleSheetsConnected) "Connected" else "Not Connected",
                            color = if (googleSheetsConnected) DyllaGreen else DyllaOnSurfaceSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // ── 4. Stages & Automations ──
        item { SectionHeader("STAGES & AUTOMATIONS") }
        item {
            SectionCard(cardShape) {
                NavigableRow(
                    icon = Icons.Filled.FormatListBulleted,
                    iconTint = DyllaBlue,
                    label = "Edit Stages",
                    onClick = onEditStages,
                    trailing = {
                        Text(
                            text = "${stages.size} stages",
                            color = DyllaOnSurfaceSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
                RowDivider()
                NavigableRow(
                    icon = Icons.Filled.AccountTree,
                    iconTint = DyllaPurple,
                    label = "Workflow Map",
                    onClick = onWorkflowMap
                )
                RowDivider()
                NavigableRow(
                    icon = Icons.Filled.GraphicEq,
                    iconTint = DyllaPurple,
                    label = "Voice Drops",
                    onClick = onVoiceDrops
                )
            }
        }

        // ── 5. Power Dialer (Twilio) ──
        item { SectionHeader("POWER DIALER (TWILIO)") }
        item {
            SectionCard(cardShape) {
                // Status indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (twilioConfigured) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Configured",
                            tint = DyllaGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configured",
                            color = DyllaGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Not Set Up",
                            tint = DyllaOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Not Set Up",
                            color = DyllaOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                RowDivider()

                // Account SID
                FormTextField(
                    label = "Account SID",
                    value = twilioSid,
                    onValueChange = { twilioSid = it }
                )

                RowDivider()

                // Auth Token
                FormTextField(
                    label = "Auth Token",
                    value = twilioToken,
                    onValueChange = { twilioToken = it },
                    isSecure = true
                )

                RowDivider()

                // Your Phone Number
                FormTextField(
                    label = "Your Phone Number",
                    value = twilioPhone,
                    onValueChange = { twilioPhone = it }
                )

                RowDivider()

                // Twilio Numbers (multi-line)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Twilio Numbers",
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = twilioNumbers,
                        onValueChange = { twilioNumbers = it },
                        placeholder = {
                            Text(
                                "One number per line",
                                color = DyllaOnSurfaceSecondary.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue,
                            unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.3f),
                            cursorColor = DyllaBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$twilioNumberCount numbers",
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 12.sp
                    )
                }

                RowDivider()

                // Lines Per Batch stepper
                StepperRow(
                    label = "Lines Per Batch",
                    value = linesPerBatch,
                    range = 1..20,
                    onValueChange = { linesPerBatch = it }
                )

                // Description
                Text(
                    text = "Power dialer rotates through your Twilio numbers to place calls automatically.",
                    color = DyllaOnSurfaceSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // ── 6. Training Dialer ──
        item { SectionHeader("TRAINING DIALER") }
        item {
            SectionCard(cardShape) {
                ToggleRow(
                    label = "Enable Training Dialer",
                    checked = trainingEnabled,
                    onCheckedChange = { trainingEnabled = it },
                    tintColor = DyllaOrange
                )
                if (trainingEnabled) {
                    Text(
                        text = "Practice with AI-generated scenarios before live calls",
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                RowDivider()
                ToggleRow(
                    label = "Auto Voice Drop on No-Answer",
                    checked = voiceDropEnabled,
                    onCheckedChange = { voiceDropEnabled = it },
                    tintColor = DyllaPurple
                )
                RowDivider()
                StepperRow(
                    label = "Min Sessions Before Live",
                    value = minSessions,
                    range = 5..50,
                    step = 5,
                    onValueChange = { minSessions = it }
                )
            }
        }

        // ── 7. Dialing Mode ──
        item { SectionHeader("DIALING MODE") }
        item {
            SectionCard(cardShape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Manual Mode",
                        color = DyllaOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to call each contact. Pause between calls.",
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── 8. Auto-Advance ──
        item { SectionHeader("AUTO-ADVANCE") }
        item {
            SectionCard(cardShape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "After a call ends, automatically advance to the next contact.",
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── 9. Automation ──
        item { SectionHeader("AUTOMATION") }
        item {
            SectionCard(cardShape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "API Key",
                            color = DyllaOnSurfaceSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DyllaBlue,
                                unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.3f),
                                cursorColor = DyllaBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(apiKey))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy API Key",
                            tint = DyllaBlue
                        )
                    }
                }
            }
        }

        // ── 10. Data ──
        item { SectionHeader("DATA") }
        item {
            SectionCard(cardShape) {
                TextButton(
                    onClick = { showClearDataDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = DyllaRed)
                ) {
                    Text(
                        text = "Clear All Lists & History",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── 11. Account ──
        item { SectionHeader("ACCOUNT") }
        item {
            SectionCard(cardShape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Signed in as",
                        color = DyllaOnSurface,
                        fontSize = 16.sp
                    )
                    Text(
                        text = userEmail,
                        color = DyllaOnSurfaceSecondary,
                        fontSize = 16.sp
                    )
                }
                RowDivider()
                TextButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = DyllaRed)
                ) {
                    Text(
                        text = "Sign Out",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── 12. App Info ──
        item { SectionHeader("APP INFO") }
        item {
            SectionCard(cardShape) {
                InfoRow(label = "Version", value = "2.1.0")
                RowDivider()
                InfoRow(label = "Build", value = "Dylla")
            }
        }

        // Bottom spacer for navigation bar clearance
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ── Reusable Components ──

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = DyllaOnSurfaceSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 24.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun SectionCard(
    shape: RoundedCornerShape,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = DyllaSurface,
        shadowElevation = 0.5.dp,
        content = { Column(content = content) }
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp,
        color = DyllaOnSurfaceSecondary.copy(alpha = 0.2f)
    )
}

@Composable
private fun NavigableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = DyllaOnSurface,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = DyllaOnSurfaceSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DyllaOnSurface,
            fontSize = 16.sp
        )
        Text(
            text = value,
            color = DyllaOnSurfaceSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecure: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = DyllaOnSurfaceSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (isSecure) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DyllaBlue,
                unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.3f),
                cursorColor = DyllaBlue
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tintColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DyllaOnSurface,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = tintColor,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                uncheckedTrackColor = DyllaOnSurfaceSecondary.copy(alpha = 0.3f),
                uncheckedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DyllaOnSurface,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val newVal = value - step
                    if (newVal >= range.first) onValueChange(newVal)
                },
                enabled = value > range.first,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = if (value > range.first) DyllaBlue else DyllaOnSurfaceSecondary.copy(alpha = 0.3f)
                )
            }
            Text(
                text = value.toString(),
                color = DyllaOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 32.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = {
                    val newVal = value + step
                    if (newVal <= range.last) onValueChange(newVal)
                },
                enabled = value < range.last,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = if (value < range.last) DyllaBlue else DyllaOnSurfaceSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
