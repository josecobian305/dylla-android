package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.FundingStage
import app.dylla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowMapScreen(stages: List<FundingStage>, onBack: () -> Unit) {
    var selectedStage by remember { mutableStateOf<FundingStage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workflow Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            stages.forEachIndexed { index, stage ->
                StageCard(stage = stage, onClick = { selectedStage = stage })
                if (index < stages.lastIndex) {
                    StageConnector()
                }
            }
        }
    }

    selectedStage?.let { stage ->
        WorkflowDetailSheet(stage = stage, onDismiss = { selectedStage = null })
    }
}

@Composable
private fun StageCard(stage: FundingStage, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DyllaBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stage.emoji, fontSize = 20.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stage.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaOnSurface
                    )
                    Text(
                        stage.criteria,
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = DyllaOnSurfaceSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepPill("Call", DyllaGreen)
                StepArrow()
                StepPill("SMS", DyllaBlue)
                StepArrow()
                StepPill("Email", DyllaOrange)
                StepArrow()
                StepPill("Wait", DyllaPurple)
                StepArrow()
                StepPill("Nudge", DyllaRed)
            }
        }
    }
}

@Composable
private fun StepPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun StepArrow() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = DyllaOnSurfaceSecondary.copy(alpha = 0.5f)
    )
}

@Composable
private fun StageConnector() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(DyllaOnSurfaceSecondary.copy(alpha = 0.3f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowDetailSheet(stage: FundingStage, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = DyllaSurface
    ) {
        WorkflowDetailContent(stage = stage)
    }
}

@Composable
private fun WorkflowDetailContent(stage: FundingStage) {
    val steps = listOf(
        Triple("Call Placed", DyllaGreen, "Agent calls merchant"),
        Triple("Initial SMS", DyllaBlue, stage.initialSMS),
        Triple("Initial Email", DyllaOrange, stage.initialEmail),
        Triple("Wait ${stage.nudgeDelay} Hours", DyllaPurple, "Waiting period before follow-up"),
        Triple("Nudge Email", DyllaRed, stage.nudgeEmail)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "${stage.emoji} ${stage.name}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DyllaOnSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        steps.forEachIndexed { index, (title, color, description) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(48.dp)
                                .background(DyllaOnSurfaceSecondary.copy(alpha = 0.3f))
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(bottom = if (index < steps.lastIndex) 12.dp else 0.dp)
                ) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaOnSurface
                    )
                    Text(
                        description,
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun WorkflowMapScreenPreview() {
    WorkflowMapScreen(stages = emptyList(), onBack = {})
}
