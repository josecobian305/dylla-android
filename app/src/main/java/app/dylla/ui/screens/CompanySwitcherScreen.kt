package app.dylla.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.Company
import app.dylla.models.IndustryType
import app.dylla.ui.theme.*
import java.util.UUID

private sealed class CompanySwitcherViewState {
    data object List : CompanySwitcherViewState()
    data class AddEdit(val company: Company?, val isNew: Boolean) : CompanySwitcherViewState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompanySwitcherScreen(onBack: () -> Unit = {}) {
    val cardShape = RoundedCornerShape(14.dp)

    // Company list state
    var companies by remember {
        mutableStateOf(
            listOf(
                Company(
                    id = UUID.randomUUID().toString(),
                    name = "SMB Capital Funding",
                    industry = IndustryType.OTHER,
                    isDefault = true
                ),
                Company(
                    id = UUID.randomUUID().toString(),
                    name = "Coast2Coast Realty",
                    industry = IndustryType.REAL_ESTATE
                )
            )
        )
    }
    var selectedCompanyId by remember {
        mutableStateOf(companies.firstOrNull { it.isDefault }?.id ?: companies.firstOrNull()?.id.orEmpty())
    }
    var viewState by remember { mutableStateOf<CompanySwitcherViewState>(CompanySwitcherViewState.List) }

    when (val state = viewState) {
        is CompanySwitcherViewState.List -> {
            CompanyListView(
                companies = companies,
                selectedCompanyId = selectedCompanyId,
                cardShape = cardShape,
                onBack = onBack,
                onAdd = {
                    viewState = CompanySwitcherViewState.AddEdit(company = null, isNew = true)
                },
                onSelect = { company ->
                    selectedCompanyId = company.id
                },
                onEdit = { company ->
                    viewState = CompanySwitcherViewState.AddEdit(company = company, isNew = false)
                },
                onSetDefault = { company ->
                    companies = companies.map {
                        it.copy(isDefault = it.id == company.id)
                    }
                },
                onDelete = { company ->
                    companies = companies.filter { it.id != company.id }
                    if (selectedCompanyId == company.id) {
                        selectedCompanyId = companies.firstOrNull()?.id.orEmpty()
                    }
                }
            )
        }

        is CompanySwitcherViewState.AddEdit -> {
            CompanyAddEditView(
                existingCompany = state.company,
                isNew = state.isNew,
                cardShape = cardShape,
                onSave = { saved ->
                    if (state.isNew) {
                        val updatedList = if (saved.isDefault) {
                            companies.map { it.copy(isDefault = false) } + saved
                        } else {
                            companies + saved
                        }
                        companies = updatedList
                        selectedCompanyId = saved.id
                    } else {
                        companies = companies.map { existing ->
                            if (existing.id == saved.id) {
                                if (saved.isDefault) saved
                                else saved
                            } else {
                                if (saved.isDefault) existing.copy(isDefault = false)
                                else existing
                            }
                        }
                    }
                    viewState = CompanySwitcherViewState.List
                },
                onCancel = {
                    viewState = CompanySwitcherViewState.List
                },
                onDelete = { company ->
                    companies = companies.filter { it.id != company.id }
                    if (selectedCompanyId == company.id) {
                        selectedCompanyId = companies.firstOrNull()?.id.orEmpty()
                    }
                    viewState = CompanySwitcherViewState.List
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CompanyListView(
    companies: List<Company>,
    selectedCompanyId: String,
    cardShape: RoundedCornerShape,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (Company) -> Unit,
    onEdit: (Company) -> Unit,
    onSetDefault: (Company) -> Unit,
    onDelete: (Company) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Companies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                    IconButton(onClick = onAdd) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Company",
                            tint = DyllaBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DyllaBackground,
                    titleContentColor = DyllaOnSurface
                )
            )
        },
        containerColor = DyllaBackground
    ) { padding ->
        if (companies.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No companies yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DyllaBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add your first company")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(companies, key = { it.id }) { company ->
                    CompanyRow(
                        company = company,
                        isSelected = company.id == selectedCompanyId,
                        cardShape = cardShape,
                        onSelect = { onSelect(company) },
                        onEdit = { onEdit(company) },
                        onSetDefault = { onSetDefault(company) },
                        onDelete = { onDelete(company) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompanyRow(
    company: Company,
    isSelected: Boolean,
    cardShape: RoundedCornerShape,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { showMenu = true }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = DyllaSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Industry emoji
            Text(
                text = company.industry.emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Company info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOnSurface
                )
                Text(
                    text = company.industry.label,
                    fontSize = 14.sp,
                    color = DyllaOnSurfaceSecondary
                )
            }

            // Selected checkmark
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = DyllaBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = DyllaOnSurfaceSecondary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set as Default") },
                        onClick = {
                            showMenu = false
                            onSetDefault()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = DyllaRed
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${company.name}?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = DyllaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyAddEditView(
    existingCompany: Company?,
    isNew: Boolean,
    cardShape: RoundedCornerShape,
    onSave: (Company) -> Unit,
    onCancel: () -> Unit,
    onDelete: (Company) -> Unit
) {
    val companyId = existingCompany?.id ?: UUID.randomUUID().toString()
    var name by remember { mutableStateOf(existingCompany?.name ?: "") }
    var selectedIndustry by remember { mutableStateOf(existingCompany?.industry ?: IndustryType.OTHER) }
    var spoofNumbersText by remember {
        mutableStateOf(existingCompany?.spoofNumbers?.joinToString("\n") ?: "")
    }
    var setAsDefault by remember { mutableStateOf(existingCompany?.isDefault ?: false) }

    var industryDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val spoofNumberCount by remember(spoofNumbersText) {
        derivedStateOf {
            spoofNumbersText.lines().count { it.trim().isNotBlank() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "Add Company" else "Edit Company",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                    containerColor = DyllaBackground,
                    titleContentColor = DyllaOnSurface
                )
            )
        },
        containerColor = DyllaBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Company Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DyllaBlue,
                        focusedLabelColor = DyllaBlue,
                        cursorColor = DyllaBlue
                    )
                )
            }

            // Industry Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = industryDropdownExpanded,
                    onExpandedChange = { industryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedIndustry.emoji}  ${selectedIndustry.label}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Industry") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = industryDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue,
                            focusedLabelColor = DyllaBlue
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = industryDropdownExpanded,
                        onDismissRequest = { industryDropdownExpanded = false }
                    ) {
                        IndustryType.entries.forEach { industry ->
                            DropdownMenuItem(
                                text = {
                                    Text("${industry.emoji}  ${industry.label}")
                                },
                                onClick = {
                                    selectedIndustry = industry
                                    industryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Spoof Numbers
            item {
                Column {
                    OutlinedTextField(
                        value = spoofNumbersText,
                        onValueChange = { spoofNumbersText = it },
                        label = { Text("Phone Numbers (one per line)") },
                        placeholder = { Text("One number per line", color = DyllaOnSurfaceSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DyllaBlue,
                            focusedLabelColor = DyllaBlue,
                            cursorColor = DyllaBlue
                        ),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$spoofNumberCount number${if (spoofNumberCount != 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Set as Default toggle
            item {
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set as Default",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = DyllaOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = setAsDefault,
                            onCheckedChange = { setAsDefault = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = DyllaBlue,
                                checkedThumbColor = DyllaSurface
                            )
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save
                    Button(
                        onClick = {
                            val parsedNumbers = spoofNumbersText
                                .lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val company = Company(
                                id = companyId,
                                name = name.trim(),
                                industry = selectedIndustry,
                                spoofNumbers = parsedNumbers,
                                isDefault = setAsDefault
                            )
                            onSave(company)
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DyllaBlue,
                            disabledContainerColor = DyllaBlue.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            "Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Cancel
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DyllaBlue
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Delete (edit mode only)
                    if (!isNew && existingCompany != null) {
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text(
                                "Delete Company",
                                color = DyllaRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && existingCompany != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${existingCompany.name}?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(existingCompany)
                    }
                ) {
                    Text("Delete", color = DyllaRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
