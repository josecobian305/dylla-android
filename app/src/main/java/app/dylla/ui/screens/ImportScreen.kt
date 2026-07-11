package app.dylla.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.Contact
import app.dylla.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

// ── Local enums ──────────────────────────────────────────────────────────────

private enum class InputMode { PASTE, FILE }

private enum class ImportState { EDITING, PREVIEW, SUCCESS }

// ── Internal parse result ────────────────────────────────────────────────────

private data class ParseResult(
    val contacts: List<Contact>,
    val headers: List<String>,
    val nameColIdx: Int,
    val phoneColIdx: Int,
    val bizColIdx: Int,
    val error: String?
)

// ── Main composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onImport: (String, List<Contact>) -> Unit = { _, _ -> },
    onDone: () -> Unit = {}
) {
    val context = LocalContext.current

    // Core state
    var selectedTab by remember { mutableStateOf(InputMode.PASTE) }
    var csvText by remember { mutableStateOf("") }
    var parsedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var listName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importState by remember { mutableStateOf(ImportState.EDITING) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Success snapshot
    var importedListName by remember { mutableStateOf("") }
    var importedCount by remember { mutableStateOf(0) }

    // Column mapping state
    var showColumnMapping by remember { mutableStateOf(false) }
    var detectedHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
    var nameColIdx by remember { mutableStateOf(-1) }
    var phoneColIdx by remember { mutableStateOf(-1) }
    var bizColIdx by remember { mutableStateOf(-1) }

    // ── Parse helpers ────────────────────────────────────────────────────────

    fun parseCSV(text: String) {
        errorMessage = null
        showColumnMapping = false

        if (text.isBlank()) {
            errorMessage = "No CSV data provided"
            parsedContacts = emptyList()
            return
        }

        val result = parseCSVText(text)

        if (result.error != null && result.contacts.isEmpty()) {
            if (result.headers.isNotEmpty() && result.phoneColIdx < 0) {
                // Headers detected but phone column missing — show manual mapping
                detectedHeaders = result.headers
                nameColIdx = result.nameColIdx
                phoneColIdx = result.phoneColIdx
                bizColIdx = result.bizColIdx
                showColumnMapping = true
                errorMessage = "Could not auto-detect columns. Please map them manually."
            } else {
                errorMessage = result.error
            }
            parsedContacts = emptyList()
            return
        }

        if (result.contacts.isEmpty()) {
            errorMessage = "No valid contacts found"
            parsedContacts = emptyList()
            return
        }

        parsedContacts = result.contacts
        importState = ImportState.PREVIEW
    }

    fun parseWithMapping(text: String, nIdx: Int, pIdx: Int, bIdx: Int) {
        errorMessage = null
        val result = parseCSVText(text, nIdx, pIdx, bIdx)

        if (result.contacts.isEmpty()) {
            errorMessage = "No valid contacts found with the selected column mapping"
            return
        }

        parsedContacts = result.contacts
        showColumnMapping = false
        importState = ImportState.PREVIEW
    }

    fun resetAll() {
        csvText = ""
        parsedContacts = emptyList()
        listName = ""
        errorMessage = null
        importState = ImportState.EDITING
        selectedFileName = null
        showColumnMapping = false
        detectedHeaders = emptyList()
        nameColIdx = -1
        phoneColIdx = -1
        bizColIdx = -1
    }

    // ── File picker ──────────────────────────────────────────────────────────

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            // Resolve display name
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) selectedFileName = cursor.getString(idx)
                }
            }

            // Read content
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                csvText = content
                parseCSV(content)
            } else {
                errorMessage = "Could not read the selected file"
            }
        } catch (e: Exception) {
            errorMessage = "Error reading file: ${e.localizedMessage}"
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Import Contacts",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subtitle
            if (importState != ImportState.SUCCESS) {
                Text(
                    text = "Paste CSV data or select a file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DyllaOnSurfaceSecondary
                )
            }

            // ── Tab toggle ───────────────────────────────────────────────────

            if (importState == ImportState.EDITING) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedTab == InputMode.PASTE,
                        onClick = {
                            selectedTab = InputMode.PASTE
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0, count = 2
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = DyllaBlue,
                            activeContentColor = Color.White,
                            inactiveContainerColor = DyllaBackgroundSecondary,
                            inactiveContentColor = DyllaOnSurface
                        )
                    ) {
                        Text("Paste CSV")
                    }
                    SegmentedButton(
                        selected = selectedTab == InputMode.FILE,
                        onClick = {
                            selectedTab = InputMode.FILE
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1, count = 2
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = DyllaBlue,
                            activeContentColor = Color.White,
                            inactiveContainerColor = DyllaBackgroundSecondary,
                            inactiveContentColor = DyllaOnSurface
                        )
                    ) {
                        Text("Select File")
                    }
                }
            }

            // ── Paste CSV area ───────────────────────────────────────────────

            AnimatedVisibility(
                visible = selectedTab == InputMode.PASTE
                        && importState == ImportState.EDITING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = {
                            Text(
                                text = "name,phone,business\nJohn Doe,555-0100,Acme Corp",
                                color = DyllaOnSurfaceSecondary,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = DyllaSurface,
                            focusedContainerColor = DyllaSurface,
                            unfocusedBorderColor = DyllaBackgroundSecondary,
                            focusedBorderColor = DyllaBlue
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = DyllaOnSurface,
                            fontSize = 13.sp
                        )
                    )

                    Button(
                        onClick = { parseCSV(csvText) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = csvText.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DyllaBlue,
                            disabledContainerColor = DyllaBlue.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Parse", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── File picker area ─────────────────────────────────────────────

            AnimatedVisibility(
                visible = selectedTab == InputMode.FILE
                        && importState == ImportState.EDITING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("text/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DyllaBlue
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose CSV File", fontWeight = FontWeight.SemiBold)
                    }

                    if (selectedFileName != null) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DyllaSurface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 1.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = DyllaBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = selectedFileName ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DyllaOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = DyllaGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Error display ────────────────────────────────────────────────

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DyllaRed.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = DyllaRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = DyllaRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Column mapping ───────────────────────────────────────────────

            AnimatedVisibility(
                visible = showColumnMapping && importState == ImportState.EDITING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ColumnMappingCard(
                    headers = detectedHeaders,
                    nameIdx = nameColIdx,
                    phoneIdx = phoneColIdx,
                    bizIdx = bizColIdx,
                    onNameChange = { nameColIdx = it },
                    onPhoneChange = { phoneColIdx = it },
                    onBizChange = { bizColIdx = it },
                    onApply = {
                        parseWithMapping(csvText, nameColIdx, phoneColIdx, bizColIdx)
                    }
                )
            }

            // ── Preview section ──────────────────────────────────────────────

            AnimatedVisibility(
                visible = importState == ImportState.PREVIEW,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview (first 5 contacts)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DyllaOnSurface
                        )
                        TextButton(onClick = {
                            importState = ImportState.EDITING
                            parsedContacts = emptyList()
                        }) {
                            Text("Edit", color = DyllaBlue)
                        }
                    }

                    // Preview table card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DyllaSurface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DyllaBackgroundSecondary)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Name",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DyllaOnSurfaceSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Phone",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DyllaOnSurfaceSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Business",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DyllaOnSurfaceSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(
                                color = DyllaBackgroundSecondary,
                                thickness = 0.5.dp
                            )

                            // Data rows (first 5)
                            val previewRows = parsedContacts.take(5)
                            previewRows.forEachIndexed { index, contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index % 2 == 1)
                                                DyllaBackgroundSecondary.copy(alpha = 0.5f)
                                            else Color.Transparent
                                        )
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 10.dp
                                        )
                                ) {
                                    Text(
                                        text = contact.name.ifEmpty { "-" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DyllaOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = contact.phone,
                                        style = MaterialTheme.typography.bodySmall
                                            .copy(fontFamily = FontFamily.Monospace),
                                        color = DyllaOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = contact.businessName.ifEmpty { "-" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DyllaOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (index < previewRows.lastIndex) {
                                    HorizontalDivider(
                                        color = DyllaBackgroundSecondary,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }

                    // Total count
                    Text(
                        text = "${parsedContacts.size} contacts found",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DyllaOnSurfaceSecondary
                    )

                    // List name field
                    OutlinedTextField(
                        value = listName,
                        onValueChange = { listName = it },
                        label = { Text("List Name") },
                        placeholder = { Text("e.g., Monday Leads") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = DyllaSurface,
                            focusedContainerColor = DyllaSurface,
                            unfocusedBorderColor = DyllaBackgroundSecondary,
                            focusedBorderColor = DyllaBlue
                        )
                    )

                    // Import button
                    Button(
                        onClick = {
                            importedListName = listName
                            importedCount = parsedContacts.size
                            onImport(listName, parsedContacts)
                            importState = ImportState.SUCCESS
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = parsedContacts.isNotEmpty()
                                && listName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DyllaBlue,
                            disabledContainerColor = DyllaBlue.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Import ${parsedContacts.size} Contacts",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Success state ────────────────────────────────────────────────

            AnimatedVisibility(
                visible = importState == ImportState.SUCCESS,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = DyllaGreen,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = "Successfully imported $importedCount contacts to '$importedListName'",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaOnSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { resetAll() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DyllaBlue
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Import More", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DyllaBlue
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Bottom scroll padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Column mapping card ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingCard(
    headers: List<String>,
    nameIdx: Int,
    phoneIdx: Int,
    bizIdx: Int,
    onNameChange: (Int) -> Unit,
    onPhoneChange: (Int) -> Unit,
    onBizChange: (Int) -> Unit,
    onApply: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Map Columns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurface
            )

            Text(
                text = "Select which CSV columns map to each field",
                style = MaterialTheme.typography.bodySmall,
                color = DyllaOnSurfaceSecondary
            )

            ColumnDropdown(
                label = "Name Column",
                headers = headers,
                selectedIdx = nameIdx,
                onSelect = onNameChange
            )

            ColumnDropdown(
                label = "Phone Column",
                headers = headers,
                selectedIdx = phoneIdx,
                onSelect = onPhoneChange
            )

            ColumnDropdown(
                label = "Business Column",
                headers = headers,
                selectedIdx = bizIdx,
                onSelect = onBizChange
            )

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                enabled = phoneIdx >= 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DyllaBlue,
                    disabledContainerColor = DyllaBlue.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Apply Mapping", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdown(
    label: String,
    headers: List<String>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedIdx in headers.indices)
        headers[selectedIdx] else "-- Select --"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = DyllaSurface,
                focusedContainerColor = DyllaSurface,
                unfocusedBorderColor = DyllaBackgroundSecondary,
                focusedBorderColor = DyllaBlue
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "-- None --",
                        color = DyllaOnSurfaceSecondary
                    )
                },
                onClick = {
                    onSelect(-1)
                    expanded = false
                }
            )
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text(header) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── CSV parsing utilities ────────────────────────────────────────────────────

/**
 * Parses raw CSV text into contacts. Supports optional forced column indices
 * for manual mapping; otherwise auto-detects by header name.
 */
private fun parseCSVText(
    text: String,
    forceNameIdx: Int = -1,
    forcePhoneIdx: Int = -1,
    forceBizIdx: Int = -1
): ParseResult {
    val lines = text
        .split(Regex("\\r?\\n"))
        .filter { it.trim().isNotEmpty() }

    if (lines.isEmpty()) {
        return ParseResult(
            emptyList(), emptyList(), -1, -1, -1,
            "Invalid CSV format"
        )
    }

    val headerLine = lines.first()
    val delimiter = detectDelimiter(headerLine)
    val headers = splitCSVLine(headerLine, delimiter).map { it.trim() }
    val dataLines = lines.drop(1)

    if (dataLines.isEmpty()) {
        return ParseResult(
            emptyList(), headers, -1, -1, -1,
            "No data rows found"
        )
    }

    val lowerHeaders = headers.map { it.lowercase() }

    // Resolve column indices (forced > auto-detect)
    val nameIdx = if (forceNameIdx >= 0) forceNameIdx else {
        autoDetectColumn(lowerHeaders, listOf(
            "name", "full name", "contact", "contact_name",
            "contact name", "first name", "firstname"
        )) ?: -1
    }

    val phoneIdx = if (forcePhoneIdx >= 0) forcePhoneIdx else {
        autoDetectColumn(lowerHeaders, listOf(
            "phone", "mobile", "cell", "number",
            "phone_number", "phone number", "telephone"
        )) ?: -1
    }

    val bizIdx = if (forceBizIdx >= 0) forceBizIdx else {
        autoDetectColumn(lowerHeaders, listOf(
            "business", "company", "business_name", "business name",
            "company_name", "company name", "dba", "organization"
        )) ?: -1
    }

    // Phone column is required
    if (phoneIdx < 0) {
        return ParseResult(
            emptyList(), headers, nameIdx, phoneIdx, bizIdx,
            "Missing phone column"
        )
    }

    // Parse data rows
    val contacts = mutableListOf<Contact>()
    for (line in dataLines) {
        val cols = splitCSVLine(line, delimiter)
        if (cols.isEmpty()) continue

        val rawName = cols.getOrNull(nameIdx)?.trim() ?: ""
        val rawPhone = cols.getOrNull(phoneIdx)?.trim() ?: ""
        val rawBiz = cols.getOrNull(bizIdx)?.trim() ?: ""

        val phone = normalizePhone(rawPhone)
        if (phone.isBlank()) continue

        // Require at least 7 digits
        val digitCount = phone.count { it.isDigit() }
        if (digitCount < 7) continue

        contacts.add(
            Contact(
                id = UUID.randomUUID().toString(),
                name = rawName,
                phone = phone,
                businessName = rawBiz
            )
        )
    }

    if (contacts.isEmpty()) {
        return ParseResult(
            emptyList(), headers, nameIdx, phoneIdx, bizIdx,
            "No valid contacts found"
        )
    }

    return ParseResult(contacts, headers, nameIdx, phoneIdx, bizIdx, null)
}

/**
 * Splits a CSV line respecting quoted fields (handles commas within quotes).
 */
private fun splitCSVLine(line: String, delimiter: Char = ','): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false

    for (c in line) {
        when {
            c == '"' -> inQuotes = !inQuotes
            c == delimiter && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
    }
    fields.add(current.toString())
    return fields
}

/**
 * Auto-detect delimiter: prefer tab if it appears more than commas.
 */
private fun detectDelimiter(headerLine: String): Char {
    val tabCount = headerLine.count { it == '\t' }
    val commaCount = headerLine.count { it == ',' }
    return if (tabCount > commaCount) '\t' else ','
}

/**
 * Normalize a phone string: strip all non-digit characters except a leading +.
 */
private fun normalizePhone(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val hasPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return if (hasPlus) "+$digits" else digits
}

/**
 * Find a column index by trying exact header matches first, then substring.
 */
private fun autoDetectColumn(
    headers: List<String>,
    candidates: List<String>
): Int? {
    for (candidate in candidates) {
        val idx = headers.indexOf(candidate)
        if (idx != -1) return idx
    }
    for (candidate in candidates) {
        val idx = headers.indexOfFirst { it.contains(candidate) }
        if (idx != -1) return idx
    }
    return null
}
