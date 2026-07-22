package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*

data class QualItem(val label: String, val key: String, val passed: Boolean? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualificationChecklistScreen(
    contact: Contact,
    stages: List<FundingStage>,
    onComplete: (String?) -> Unit
) {
    val defaultItems = listOf(
        QualItem("Time in Business > 1yr", "tib"),
        QualItem("Monthly Revenue > \$10K", "revenue"),
        QualItem("Business Bank Account", "bank_account"),
        QualItem("No Active Bankruptcy", "no_bankruptcy"),
        QualItem("Owner Available to Speak", "owner_available")
    )

    var items by remember { mutableStateOf(defaultItems) }

    val allAnswered = items.all { it.passed != null }
    val passCount = items.count { it.passed == true }
    val passRatio = if (items.isNotEmpty()) passCount.toFloat() / items.size else 0f
    val recommendedIndex = when {
        passRatio >= 0.8f -> 2.coerceAtMost(stages.lastIndex)
        passRatio >= 0.5f -> 1.coerceAtMost(stages.lastIndex)
        else -> 0
    }
    val recommendedStage = stages.getOrNull(recommendedIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Qualification") },
                actions = {
                    TextButton(onClick = { onComplete(null) }) {
                        Text("Skip", color = DyllaBlue)
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
            Text(
                "CHECKLIST",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurfaceSecondary,
                letterSpacing = 0.5.sp
            )

            items.forEachIndexed { index, item ->
                QualRow(
                    item = item,
                    onTogglePass = {
                        items = items.toMutableList().apply {
                            val current = this[index]
                            this[index] = current.copy(
                                passed = if (current.passed == true) null else true
                            )
                        }
                    },
                    onToggleFail = {
                        items = items.toMutableList().apply {
                            val current = this[index]
                            this[index] = current.copy(
                                passed = if (current.passed == false) null else false
                            )
                        }
                    }
                )
            }

            if (allAnswered && recommendedStage != null) {
                Spacer(Modifier.height(8.dp))
                RecommendationSection(
                    stage = recommendedStage,
                    onAssign = { onComplete(recommendedStage.id) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QualRow(
    item: QualItem,
    onTogglePass: () -> Unit,
    onToggleFail: () -> Unit
) {
    val backgroundColor = when (item.passed) {
        true -> DyllaGreen.copy(alpha = 0.08f)
        false -> DyllaRed.copy(alpha = 0.08f)
        null -> DyllaSurface
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = if (item.passed == null) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                item.label,
                fontSize = 15.sp,
                color = DyllaOnSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onTogglePass,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Pass",
                    tint = if (item.passed == true) DyllaGreen else DyllaOnSurfaceSecondary.copy(alpha = 0.4f)
                )
            }
            IconButton(
                onClick = onToggleFail,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Fail",
                    tint = if (item.passed == false) DyllaRed else DyllaOnSurfaceSecondary.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun RecommendationSection(stage: FundingStage, onAssign: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaBlue.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "RECOMMENDED STAGE",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurfaceSecondary,
                letterSpacing = 0.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stage.emoji, fontSize = 24.sp)
                Text(
                    stage.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface
                )
            }
            Button(
                onClick = onAssign,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DyllaBlue)
            ) {
                Text(
                    "Assign ${stage.name} & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QualificationChecklistScreenPreview() {
    QualificationChecklistScreen(
        contact = Contact(phone = "+15551234567", name = "Test Merchant"),
        stages = emptyList(),
        onComplete = {}
    )
}
