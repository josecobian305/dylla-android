package app.dylla.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.FundingStage
import app.dylla.ui.theme.*
import java.util.UUID

// ---------- View State ----------

private sealed class StageEditorViewState {
    data object List : StageEditorViewState()
    data class Edit(val stage: FundingStage, val isNew: Boolean) : StageEditorViewState()
}

// ---------- Preset Colors ----------

private val presetColors: kotlin.collections.List<Pair<String, Color>> = listOf(
    "#007AFF" to Color(0xFF007AFF),
    "#5856D6" to Color(0xFF5856D6),
    "#34C759" to Color(0xFF34C759),
    "#00C7BE" to Color(0xFF00C7BE),
    "#FF9500" to Color(0xFFFF9500),
    "#FF3B30" to Color(0xFFFF3B30),
    "#FF2D55" to Color(0xFFFF2D55),
    "#8E8E93" to Color(0xFF8E8E93)
)

// ---------- Color Parsing Helper ----------

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF007AFF)
    }
}

// ---------- Main Composable ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageEditorScreen(
    onSave: (kotlin.collections.List<FundingStage>) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var stages by remember { mutableStateOf(FundingStage.defaults.toMutableList()) }
    var viewState by remember { mutableStateOf<StageEditorViewState>(StageEditorViewState.List) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<FundingStage?>(null) }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Stages") },
            text = { Text("This will replace all stages with defaults. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    stages = FundingStage.defaults.toMutableList()
                    showResetDialog = false
                }) {
                    Text("Reset", color = DyllaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { stageToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Stage") },
            text = { Text("Delete \"${stageToDelete.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    stages = stages.filter { it.id != stageToDelete.id }
                        .mapIndexed { index, stage -> stage.copy(order = index) }
                        .toMutableList()
                    showDeleteDialog = null
                    if (viewState is StageEditorViewState.Edit) {
                        viewState = StageEditorViewState.List
                    }
                }) {
                    Text("Delete", color = DyllaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (val state = viewState) {
        is StageEditorViewState.List -> {
            StageListView(
                stages = stages.sortedBy { it.order },
                onBack = onBack,
                onAddNew = {
                    val newStage = FundingStage(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        emoji = "",
                        order = stages.size,
                        shortLabel = "",
                        color = "#007AFF"
                    )
                    viewState = StageEditorViewState.Edit(stage = newStage, isNew = true)
                },
                onEditStage = { stage ->
                    viewState = StageEditorViewState.Edit(stage = stage, isNew = false)
                },
                onDeleteStage = { stage ->
                    showDeleteDialog = stage
                },
                onMoveUp = { stage ->
                    val sorted = stages.sortedBy { it.order }.toMutableList()
                    val idx = sorted.indexOfFirst { it.id == stage.id }
                    if (idx > 0) {
                        val prev = sorted[idx - 1]
                        val curr = sorted[idx]
                        val tempOrder = curr.order
                        stages = sorted.map {
                            when (it.id) {
                                curr.id -> it.copy(order = prev.order)
                                prev.id -> it.copy(order = tempOrder)
                                else -> it
                            }
                        }.toMutableList()
                    }
                },
                onMoveDown = { stage ->
                    val sorted = stages.sortedBy { it.order }.toMutableList()
                    val idx = sorted.indexOfFirst { it.id == stage.id }
                    if (idx < sorted.size - 1) {
                        val next = sorted[idx + 1]
                        val curr = sorted[idx]
                        val tempOrder = curr.order
                        stages = sorted.map {
                            when (it.id) {
                                curr.id -> it.copy(order = next.order)
                                next.id -> it.copy(order = tempOrder)
                                else -> it
                            }
                        }.toMutableList()
                    }
                },
                onResetDefaults = { showResetDialog = true },
                onSaveAll = { onSave(stages.sortedBy { it.order }) }
            )
        }

        is StageEditorViewState.Edit -> {
            StageEditView(
                stage = state.stage,
                isNew = state.isNew,
                onSave = { updatedStage ->
                    if (state.isNew) {
                        stages = (stages + updatedStage).toMutableList()
                    } else {
                        stages = stages.map {
                            if (it.id == updatedStage.id) updatedStage else it
                        }.toMutableList()
                    }
                    viewState = StageEditorViewState.List
                },
                onCancel = { viewState = StageEditorViewState.List },
                onDelete = if (!state.isNew) {
                    { showDeleteDialog = state.stage }
                } else null
            )
        }
    }
}

// ---------- Stage List View ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageListView(
    stages: kotlin.collections.List<FundingStage>,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEditStage: (FundingStage) -> Unit,
    onDeleteStage: (FundingStage) -> Unit,
    onMoveUp: (FundingStage) -> Unit,
    onMoveDown: (FundingStage) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveAll: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Stages",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddNew) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Stage",
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = stages,
                    key = { _, stage -> stage.id }
                ) { index, stage ->
                    SwipeToDismissStageRow(
                        stage = stage,
                        isFirst = index == 0,
                        isLast = index == stages.size - 1,
                        onEdit = { onEditStage(stage) },
                        onDelete = { onDeleteStage(stage) },
                        onMoveUp = { onMoveUp(stage) },
                        onMoveDown = { onMoveDown(stage) }
                    )
                }
            }

            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onSaveAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DyllaBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Save Changes",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onResetDefaults) {
                    Text(
                        "Reset to Defaults",
                        color = DyllaRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---------- Swipe-to-Dismiss Stage Row ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissStageRow(
    stage: FundingStage,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't auto-dismiss; let the dialog handle it
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
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
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        val elevation by animateDpAsState(
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) 4.dp else 1.dp,
            label = "card-elevation"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation, RoundedCornerShape(14.dp))
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = DyllaSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag handle (decorative)
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Reorder",
                    modifier = Modifier.size(20.dp),
                    tint = DyllaOnSurfaceSecondary
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Move up/down buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy((-4).dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(24.dp),
                        enabled = !isFirst
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(18.dp),
                            tint = if (!isFirst) DyllaOnSurfaceSecondary else DyllaOnSurfaceSecondary.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(24.dp),
                        enabled = !isLast
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(18.dp),
                            tint = if (!isLast) DyllaOnSurfaceSecondary else DyllaOnSurfaceSecondary.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Emoji
                Text(
                    text = stage.emoji,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name and short label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stage.name.ifBlank { "Untitled" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (stage.shortLabel.isNotBlank()) {
                        Text(
                            text = stage.shortLabel,
                            fontSize = 13.sp,
                            color = DyllaOnSurfaceSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Color dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parseColor(stage.color))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Chevron
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp),
                    tint = DyllaOnSurfaceSecondary
                )
            }
        }
    }
}

// ---------- Stage Edit View ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageEditView(
    stage: FundingStage,
    isNew: Boolean,
    onSave: (FundingStage) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var emoji by remember { mutableStateOf(stage.emoji) }
    var name by remember { mutableStateOf(stage.name) }
    var shortLabel by remember { mutableStateOf(stage.shortLabel) }
    var selectedColor by remember { mutableStateOf(stage.color) }
    var automationEnabled by remember { mutableStateOf(stage.automationEnabled) }
    var autoSMS by remember { mutableStateOf(stage.autoSMS) }
    var autoEmail by remember { mutableStateOf(stage.autoEmail) }
    var autoDelay by remember { mutableStateOf(stage.autoDelay) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "New Stage" else "Edit Stage",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground
                )
            )
        },
        containerColor = DyllaBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Emoji field
            OutlinedTextField(
                value = emoji,
                onValueChange = { if (it.length <= 2) emoji = it },
                label = { Text("Emoji") },
                placeholder = { Text("e.g. 📥") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                    focusedContainerColor = DyllaSurface,
                    unfocusedContainerColor = DyllaSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Stage Name") },
                placeholder = { Text("e.g. Qualified") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                    focusedContainerColor = DyllaSurface,
                    unfocusedContainerColor = DyllaSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Short label field
            OutlinedTextField(
                value = shortLabel,
                onValueChange = { shortLabel = it },
                label = { Text("Short Label") },
                placeholder = { Text("e.g. Qual") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                    focusedContainerColor = DyllaSurface,
                    unfocusedContainerColor = DyllaSurface
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Color picker
            Text(
                text = "Color",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DyllaOnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Color grid: 2 rows of 4
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (rowIndex in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (colIndex in 0..3) {
                            val idx = rowIndex * 4 + colIndex
                            if (idx < presetColors.size) {
                                val (hex, color) = presetColors[idx]
                                val isSelected = selectedColor.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(3.dp, color, CircleShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { selectedColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 34.dp else 40.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = DyllaOnSurfaceSecondary.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            // Automation toggle
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Automation",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    Switch(
                        checked = automationEnabled,
                        onCheckedChange = { automationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DyllaGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // Automation fields (visible when enabled)
            AnimatedVisibility(
                visible = automationEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto SMS template
                    OutlinedTextField(
                        value = autoSMS,
                        onValueChange = { autoSMS = it },
                        label = { Text("Auto SMS Template") },
                        placeholder = { Text("Hey {name}, ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue,
                            unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                            focusedContainerColor = DyllaSurface,
                            unfocusedContainerColor = DyllaSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto email template
                    OutlinedTextField(
                        value = autoEmail,
                        onValueChange = { autoEmail = it },
                        label = { Text("Auto Email Template") },
                        placeholder = { Text("Email body template...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue,
                            unfocusedBorderColor = DyllaOnSurfaceSecondary.copy(alpha = 0.2f),
                            focusedContainerColor = DyllaSurface,
                            unfocusedContainerColor = DyllaSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delay stepper
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Delay (minutes)",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = DyllaOnSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Minus button
                                FilledIconButton(
                                    onClick = {
                                        if (autoDelay > 0) autoDelay -= 1
                                    },
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = DyllaBackgroundSecondary,
                                        contentColor = DyllaOnSurface
                                    ),
                                    enabled = autoDelay > 0
                                ) {
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "Decrease",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "$autoDelay",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = DyllaOnSurface,
                                    modifier = Modifier.widthIn(min = 48.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                // Plus button
                                FilledIconButton(
                                    onClick = {
                                        if (autoDelay < 1440) autoDelay += 1
                                    },
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = DyllaBackgroundSecondary,
                                        contentColor = DyllaOnSurface
                                    ),
                                    enabled = autoDelay < 1440
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Increase",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    val updated = stage.copy(
                        emoji = emoji,
                        name = name,
                        shortLabel = shortLabel,
                        color = selectedColor,
                        automationEnabled = automationEnabled,
                        autoSMS = autoSMS,
                        autoEmail = autoEmail,
                        autoDelay = autoDelay
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DyllaBlue,
                    contentColor = Color.White
                ),
                enabled = name.isNotBlank()
            ) {
                Text(
                    "Save",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DyllaOnSurfaceSecondary.copy(alpha = 0.2f))
            ) {
                Text(
                    "Cancel",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
            }

            // Delete button (only for existing stages)
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Delete Stage",
                        color = DyllaRed,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
