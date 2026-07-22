package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.services.PersistenceManager
import app.dylla.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSignatureScreen(onDismiss: () -> Unit) {
    val profile = PersistenceManager.loadUserProfile()
    var useHtml by remember { mutableStateOf(false) }
    var signatureText by remember {
        mutableStateOf(
            if (useHtml) profile?.emailSignatureHTML ?: ""
            else profile?.emailSignaturePlain ?: ""
        )
    }
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(useHtml) {
        val current = PersistenceManager.loadUserProfile()
        signatureText = if (useHtml) current?.emailSignatureHTML ?: ""
        else current?.emailSignaturePlain ?: ""
    }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            delay(1000L)
            showSaved = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Signature") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DyllaBlue)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (useHtml) {
                            PersistenceManager.updateEmailSignatureHTML(signatureText)
                        } else {
                            PersistenceManager.updateEmailSignaturePlain(signatureText)
                        }
                        showSaved = true
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
            if (showSaved) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DyllaGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Signature saved",
                        modifier = Modifier.padding(12.dp),
                        color = DyllaGreen,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "Use HTML Signature",
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
                Switch(
                    checked = useHtml,
                    onCheckedChange = { useHtml = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = DyllaBlue)
                )
            }

            OutlinedTextField(
                value = signatureText,
                onValueChange = { signatureText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = if (useHtml) FontFamily.Monospace else FontFamily.Default,
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    cursorColor = DyllaBlue
                )
            )

            Text(
                "PREVIEW",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurfaceSecondary,
                letterSpacing = 0.5.sp
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = DyllaOnSurfaceSecondary.copy(alpha = 0.3f))
                    Text(
                        signatureText.ifBlank { "Your signature will appear here" },
                        fontSize = 14.sp,
                        color = if (signatureText.isBlank()) DyllaOnSurfaceSecondary
                        else DyllaOnSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmailSignatureScreenPreview() {
    EmailSignatureScreen(onDismiss = {})
}
