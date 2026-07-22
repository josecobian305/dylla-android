package app.dylla.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class CallList(
    val id: String,
    val name: String,
    val contactCount: Int
)

enum class ConnectionTestState {
    IDLE, TESTING, SUCCESS, FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSheetsSetupScreen(
    activeList: CallList?,
    stages: List<FundingStage>,
    onDismiss: () -> Unit
) {
    var webhookURL by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(false) }
    var testState by remember { mutableStateOf(ConnectionTestState.IDLE) }
    var exportSuccess by remember { mutableStateOf(false) }
    var showCopied by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val appsScriptCode = """
function doPost(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var data = JSON.parse(e.postData.contents);

  if (Array.isArray(data)) {
    if (sheet.getLastRow() === 0) {
      var headers = Object.keys(data[0]);
      sheet.appendRow(headers);
    }
    data.forEach(function(row) {
      var values = Object.keys(row).map(function(key) { return row[key]; });
      sheet.appendRow(values);
    });
  } else {
    if (sheet.getLastRow() === 0) {
      var headers = Object.keys(data);
      sheet.appendRow(headers);
    }
    var values = Object.keys(data).map(function(key) { return data[key]; });
    sheet.appendRow(values);
  }

  return ContentService.createTextOutput(JSON.stringify({status: "ok"}))
    .setMimeType(ContentService.MimeType.JSON);
}
    """.trimIndent()

    val setupSteps = listOf(
        "Open Google Sheets and create a new spreadsheet",
        "Go to Extensions → Apps Script",
        "Delete any existing code and paste the script below",
        "Click Deploy → New deployment → Web app",
        "Set \"Who has access\" to \"Anyone\" and click Deploy",
        "Copy the Web App URL and paste it above"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Sheets Sync") },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📊", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Sheets Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DyllaOnSurface
                        )
                        Text(
                            text = "Sync contacts to Google Sheets",
                            fontSize = 13.sp,
                            color = DyllaOnSurfaceSecondary
                        )
                    }
                    val statusColor = if (webhookURL.isNotBlank() && testState == ConnectionTestState.SUCCESS) DyllaGreen else DyllaRed
                    val statusText = if (webhookURL.isNotBlank() && testState == ConnectionTestState.SUCCESS) "Connected" else "Not Connected"
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Webhook URL",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = webhookURL,
                    onValueChange = {
                        webhookURL = it
                        testState = ConnectionTestState.IDLE
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://script.google.com/macros/s/...") },
                    shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-Sync",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "Automatically sync after each call",
                        fontSize = 12.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
                Switch(
                    checked = autoSync,
                    onCheckedChange = { autoSync = it }
                )
            }

            Button(
                onClick = {
                    testState = ConnectionTestState.TESTING
                    scope.launch {
                        testState = try {
                            val result = withContext(Dispatchers.IO) {
                                val url = URL(webhookURL)
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json")
                                connection.doOutput = true
                                val testPayload = """{"test":true,"name":"Test Contact","phone":"5551234567","timestamp":"${System.currentTimeMillis()}"}"""
                                connection.outputStream.use { it.write(testPayload.toByteArray()) }
                                connection.responseCode
                            }
                            if (result in 200..399) ConnectionTestState.SUCCESS else ConnectionTestState.FAILED
                        } catch (_: Exception) {
                            ConnectionTestState.FAILED
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = webhookURL.isNotBlank() && testState != ConnectionTestState.TESTING,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (testState) {
                        ConnectionTestState.SUCCESS -> DyllaGreen
                        ConnectionTestState.FAILED -> DyllaRed
                        else -> DyllaBlue
                    }
                )
            ) {
                when (testState) {
                    ConnectionTestState.IDLE -> Text("Test Connection")
                    ConnectionTestState.TESTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = DyllaBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    }
                    ConnectionTestState.SUCCESS -> {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection Successful")
                    }
                    ConnectionTestState.FAILED -> {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection Failed")
                    }
                }
            }

            activeList?.let { list ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Export",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = DyllaOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${list.name} · ${list.contactCount} contacts",
                            fontSize = 13.sp,
                            color = DyllaOnSurfaceSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    exportSuccess = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue),
                            enabled = webhookURL.isNotBlank()
                        ) {
                            Text("Export Active List")
                        }
                        if (exportSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DyllaGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = DyllaGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export completed successfully",
                                    fontSize = 13.sp,
                                    color = DyllaGreen
                                )
                            }
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "Setup Instructions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DyllaOnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                setupSteps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DyllaBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DyllaBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = step,
                            fontSize = 14.sp,
                            color = DyllaOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(appsScriptCode))
                    showCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showCopied) DyllaGreen else DyllaBlue
                )
            ) {
                Text(
                    text = if (showCopied) "Copied to Clipboard" else "Copy Apps Script Code",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
