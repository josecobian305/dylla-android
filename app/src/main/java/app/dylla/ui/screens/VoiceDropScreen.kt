package app.dylla.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.FundingStage
import app.dylla.services.VoiceDropManager
import app.dylla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceDropScreen(
    stages: List<FundingStage>,
    onDismiss: () -> Unit
) {
    val dropManager = remember { VoiceDropManager() }
    var newName by remember { mutableStateOf("") }
    var selectedStageID by remember { mutableStateOf<String?>(null) }
    var showMicPermission by remember { mutableStateOf(false) }
    var renamingDrop by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var stageExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val name = newName.ifBlank { "Voice Drop ${dropManager.recordings.size + 1}" }
            dropManager.startRecording(name, selectedStageID)
        } else {
            showMicPermission = true
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Drops") },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = DyllaBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "New Voice Drop",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
            }

            item {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Recording Name") },
                    placeholder = { Text("Voice Drop ${dropManager.recordings.size + 1}") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = stageExpanded,
                    onExpandedChange = { stageExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedStageID == null) "Default (all stages)"
                        else stages.find { it.id == selectedStageID }?.let { "${it.emoji} ${it.name}" }
                            ?: "Default (all stages)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Stage Assignment") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = stageExpanded,
                        onDismissRequest = { stageExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default (all stages)") },
                            onClick = {
                                selectedStageID = null
                                stageExpanded = false
                            }
                        )
                        stages.forEach { stage ->
                            DropdownMenuItem(
                                text = { Text("${stage.emoji} ${stage.name}") },
                                onClick = {
                                    selectedStageID = stage.id
                                    stageExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (dropManager.isRecording) {
                                dropManager.stopRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = CircleShape,
                        containerColor = if (dropManager.isRecording) DyllaRed else DyllaBlue,
                        modifier = if (dropManager.isRecording) Modifier.scale(pulseScale) else Modifier
                    ) {
                        Icon(
                            imageVector = if (dropManager.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (dropManager.isRecording) "Stop" else "Record",
                            tint = DyllaOnSurface
                        )
                    }
                }
            }

            if (dropManager.recordings.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Recordings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(dropManager.recordings, key = { it.id }) { recording ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    renamingDrop = recording.id
                                    renameText = recording.name
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (dropManager.playingDropID == recording.id) {
                                        dropManager.stopPlayback()
                                    } else {
                                        dropManager.play(recording)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (dropManager.playingDropID == recording.id) Icons.Filled.Stop
                                    else Icons.Filled.PlayArrow,
                                    contentDescription = if (dropManager.playingDropID == recording.id) "Stop" else "Play",
                                    tint = if (dropManager.playingDropID == recording.id) DyllaRed else DyllaGreen
                                )
                            }

                            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recording.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = DyllaOnSurface
                                    )
                                    if (recording.isDefault) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = DyllaBlue
                                        ) {
                                            Text(
                                                text = "DEFAULT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DyllaOnSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (recording.stageID != null) {
                                    val stageName = stages.find { it.id == recording.stageID }
                                        ?.let { "${it.emoji} ${it.name}" } ?: "Unknown Stage"
                                    Text(
                                        text = stageName,
                                        fontSize = 12.sp,
                                        color = DyllaOnSurfaceSecondary
                                    )
                                }
                            }

                            if (!recording.isDefault) {
                                TextButton(
                                    onClick = { dropManager.setDefault(recording) }
                                ) {
                                    Text("Set Default", fontSize = 12.sp, color = DyllaBlue)
                                }
                            }

                            IconButton(
                                onClick = {
                                    renamingDrop = recording.id
                                    renameText = recording.name
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Rename",
                                    tint = DyllaOnSurfaceSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { dropManager.delete(recording) }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = DyllaRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaBackgroundSecondary),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How it works",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DyllaOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Voice drops are sent as MMS voicemails directly to the prospect's phone. " +
                                    "Record a personalized message and assign it to a funding stage. " +
                                    "When a prospect reaches that stage, the voice drop is delivered automatically.",
                            fontSize = 13.sp,
                            color = DyllaOnSurfaceSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showMicPermission) {
        AlertDialog(
            onDismissRequest = { showMicPermission = false },
            title = { Text("Microphone Access Required") },
            text = { Text("Voice drop recording requires microphone permission. Please enable it in Settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicPermission = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings", color = DyllaBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermission = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (renamingDrop != null) {
        AlertDialog(
            onDismissRequest = { renamingDrop = null },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Name") },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = renamingDrop!!
                        if (renameText.isNotBlank()) {
                            val drop = dropManager.recordings.find { it.id == id }
                            if (drop != null) {
                                dropManager.rename(drop, renameText.trim())
                            }
                        }
                        renamingDrop = null
                    }
                ) {
                    Text("Save", color = DyllaBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingDrop = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
