package app.dylla.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.CallList
import app.dylla.models.Contact
import app.dylla.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(lists: List<CallList>, onBack: () -> Unit) {
    val callEntries = lists
        .flatMap { it.contacts }
        .filter { it.callTime != null }
        .sortedByDescending { it.callTime }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (callEntries.isEmpty()) {
            EmptyHistoryState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(callEntries) { contact ->
                    CallHistoryRow(contact)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.PhoneDisabled,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = DyllaOnSurfaceSecondary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No Calls Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Start a session from the Dashboard",
                fontSize = 14.sp,
                color = DyllaOnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun CallHistoryRow(contact: Contact) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = DyllaSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                contact.outcome?.emoji ?: "📞",
                fontSize = 24.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name ?: contact.phone,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurface
                )
                if (!contact.businessName.isNullOrBlank()) {
                    Text(
                        contact.businessName,
                        fontSize = 14.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    contact.outcome?.label ?: "Called",
                    fontSize = 13.sp,
                    color = DyllaOnSurfaceSecondary
                )
                contact.callTime?.let { date ->
                    Text(
                        java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US).format(date),
                        fontSize = 12.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryScreenPreview() {
    HistoryScreen(lists = emptyList(), onBack = {})
}
