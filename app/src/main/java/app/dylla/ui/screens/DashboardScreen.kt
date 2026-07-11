package app.dylla.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.models.*
import app.dylla.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    activeList: CallList?,
    activeCompany: Company?,
    stages: List<FundingStage>
) {
    val context = LocalContext.current

    val pendingCount = activeList?.pendingCount ?: 0
    val doneCount = activeList?.doneCount ?: 0
    val callbackCount = activeList?.callbackCount ?: 0

    // Compute nudge email count (contacts with nudgeSent = true)
    val nudgeCount = activeList?.contacts?.count { it.nudgeSent } ?: 0

    // Compute stage breakdown
    val stageBreakdown: List<Pair<FundingStage, Int>> = remember(activeList, stages) {
        if (activeList == null) emptyList()
        else stages.mapNotNull { stage ->
            val count = activeList.contacts.count { it.stageID == stage.id }
            if (count > 0) stage to count else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dylla",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Show list picker */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show import */ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Import")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Active company card
            if (activeCompany != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Show company switcher */ },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DyllaBlue.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = activeCompany.industry.emoji,
                            fontSize = 24.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeCompany.name.ifEmpty { "Unnamed" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = DyllaOnSurface
                            )
                            Text(
                                text = activeCompany.industry.label,
                                fontSize = 12.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = DyllaOnSurfaceSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // List info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DyllaBackgroundSecondary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = activeList?.name ?: "No List Loaded",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    Text(
                        text = "${activeList?.totalCount ?: 0} contacts",
                        fontSize = 14.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                    if (activeList != null && activeList.totalCount > 0) {
                        LinearProgressIndicator(
                            progress = { activeList.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DyllaBlue,
                            trackColor = DyllaBlue.copy(alpha = 0.12f),
                        )
                    }
                }
            }

            // 3 stat boxes in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Pending",
                    count = pendingCount,
                    color = DyllaOrange,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Done",
                    count = doneCount,
                    color = DyllaGreen,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Callback",
                    count = callbackCount,
                    color = DyllaPurple,
                    modifier = Modifier.weight(1f)
                )
            }

            // Nudge emails banner
            if (nudgeCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = DyllaOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = DyllaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$nudgeCount nudge email${if (nudgeCount == 1) "" else "s"} sent (24hr follow-up)",
                        fontSize = 12.sp,
                        color = DyllaOnSurface
                    )
                }
            }

            // Stages breakdown
            if (stageBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DyllaBackgroundSecondary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stages",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DyllaOnSurface
                        )
                        stageBreakdown.forEach { (stage, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stage.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stage.shortLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DyllaOnSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DyllaBlue,
                                    modifier = Modifier
                                        .background(
                                            color = DyllaBlue.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action button
            Button(
                onClick = { /* Import or start calling */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DyllaBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (activeList == null) "Import a List" else "Start Calling",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DyllaOnSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = DyllaOnSurfaceSecondary
        )
    }
}
