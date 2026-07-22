package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    activeCompanyName: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var serverBaseURL by remember {
        mutableStateOf(PersistenceManager.getUserProfile()?.serverBaseURL ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DyllaBlue)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        PersistenceManager.updateServerBaseURL(serverBaseURL)
                        onSave(serverBaseURL)
                    }) {
                        Text("Save", color = DyllaBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!activeCompanyName.isNullOrBlank()) {
                val company = PersistenceManager.getCompany(activeCompanyName)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            activeCompanyName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DyllaOnSurface
                        )
                        if (company?.industry != null) {
                            Text(
                                company.industry,
                                fontSize = 14.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "GLOBAL SETTINGS",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurfaceSecondary,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = serverBaseURL,
                onValueChange = { serverBaseURL = it },
                label = { Text("Server Base URL") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    cursorColor = DyllaBlue
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileEditorScreenPreview() {
    ProfileEditorScreen(
        activeCompanyName = "CHC Capital Group",
        onSave = {},
        onDismiss = {}
    )
}
