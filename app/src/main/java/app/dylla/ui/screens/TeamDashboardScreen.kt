package app.dylla.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class TeamMember(
    val id: String,
    val name: String,
    val role: String,
    val xp: Int,
    val level: Int,
    val sessionsCount: Int,
    val streak: Int,
    val isOwner: Boolean = false
)

data class TeamSession(
    val id: String,
    val memberId: String,
    val memberName: String,
    val grade: String,
    val scenarioTitle: String,
    val tonalityScore: Double,
    val xpEarned: Int,
    val reactionTime: Double,
    val fillerCount: Int,
    val uptalkCount: Int,
    val inflectionRating: String,
    val difficulty: String,
    val date: Long
)

data class VoiceTrends(
    val avgTonality: Double,
    val avgReactionTime: Double,
    val totalFillers: Int,
    val trendDirection: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDashboardScreen(onDismiss: () -> Unit) {
    var members by remember { mutableStateOf(listOf<TeamMember>()) }
    var sessions by remember { mutableStateOf(listOf<TeamSession>()) }
    var isTeamOwner by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<TeamMember?>(null) }
    var showMemberDetail by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    var newMemberIsOwner by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    val sortedMembers = remember(members) {
        members.sortedByDescending { it.xp }
    }

    val recentSessions = remember(sessions) {
        sessions.sortedByDescending { it.date }.take(10)
    }

    fun gradeColor(grade: String) = when {
        grade.startsWith("A") -> DyllaGreen
        grade.startsWith("B") -> DyllaBlue
        grade.startsWith("C") -> DyllaOrange
        else -> DyllaRed
    }

    fun medalEmoji(index: Int) = when (index) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "${index + 1}"
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Team Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = { newMemberEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Team Owner",
                            modifier = Modifier.weight(1f),
                            color = DyllaOnSurface
                        )
                        Switch(
                            checked = newMemberIsOwner,
                            onCheckedChange = { newMemberIsOwner = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddMemberDialog = false
                        newMemberName = ""
                        newMemberEmail = ""
                        newMemberIsOwner = false
                    }
                ) {
                    Text("Add", color = DyllaBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMemberDetail && selectedMember != null) {
        val member = selectedMember!!
        val memberSessions = sessions
            .filter { it.memberId == member.id }
            .sortedByDescending { it.date }

        val voiceTrends: VoiceTrends? = if (memberSessions.size >= 2) {
            val lastFive = memberSessions.take(5)
            val priorFive = memberSessions.drop(5).take(5)
            val avgTonality = lastFive.map { it.tonalityScore }.average()
            val avgReaction = lastFive.map { it.reactionTime }.average()
            val totalFillers = lastFive.sumOf { it.fillerCount }
            val trend = if (priorFive.isNotEmpty()) {
                val priorAvg = priorFive.map { it.tonalityScore }.average()
                if (avgTonality > priorAvg) "Improving" else if (avgTonality < priorAvg) "Declining" else "Stable"
            } else "Stable"
            VoiceTrends(avgTonality, avgReaction, totalFillers, trend)
        } else null

        ModalBottomSheet(onDismissRequest = { showMemberDetail = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = member.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOnSurface
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stats", fontWeight = FontWeight.SemiBold, color = DyllaOnSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("Level", "${member.level}")
                            StatItem("XP", "${member.xp}")
                            StatItem("Sessions", "${member.sessionsCount}")
                            StatItem("Streak", "${member.streak}")
                        }
                    }
                }

                voiceTrends?.let { trends ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Voice Trends", fontWeight = FontWeight.SemiBold, color = DyllaOnSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("Avg Tonality", String.format("%.1f", trends.avgTonality))
                                StatItem("Avg Reaction", String.format("%.1fs", trends.avgReactionTime))
                                StatItem("Fillers", "${trends.totalFillers}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val trendColor = when (trends.trendDirection) {
                                "Improving" -> DyllaGreen
                                "Declining" -> DyllaRed
                                else -> DyllaOnSurfaceSecondary
                            }
                            Text(
                                text = "Trend: ${trends.trendDirection}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = trendColor
                            )
                        }
                    }
                }

                if (memberSessions.isNotEmpty()) {
                    Text(
                        text = "Session Log",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DyllaOnSurface
                    )
                    memberSessions.forEach { session ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DyllaBackgroundSecondary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.scenarioTitle,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = DyllaOnSurface
                                    )
                                    Text(
                                        text = session.grade,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = gradeColor(session.grade),
                                        modifier = Modifier
                                            .background(
                                                gradeColor(session.grade).copy(alpha = 0.15f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${session.difficulty} · Tonality ${String.format("%.1f", session.tonalityScore)} · ${session.xpEarned} XP",
                                    fontSize = 12.sp,
                                    color = DyllaOnSurfaceSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Dashboard") },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Team Leaderboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(sortedMembers) { index, member ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface),
                    modifier = Modifier.clickable {
                        selectedMember = member
                        showMemberDetail = true
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medalEmoji(index),
                            fontSize = 22.sp,
                            modifier = Modifier.width(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DyllaOnSurface
                            )
                            Text(
                                text = member.role,
                                fontSize = 12.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${member.xp} XP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DyllaBlue
                            )
                            Text(
                                text = "Lv${member.level} · ${member.sessionsCount} sessions",
                                fontSize = 11.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Team Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOnSurface
                )
            }

            items(recentSessions) { session ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = session.memberName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = DyllaOnSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = session.grade,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = gradeColor(session.grade),
                                    modifier = Modifier
                                        .background(
                                            gradeColor(session.grade).copy(alpha = 0.15f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "+${session.xpEarned} XP",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = DyllaGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = session.scenarioTitle,
                            fontSize = 13.sp,
                            color = DyllaOnSurface
                        )
                        Text(
                            text = "Tonality: ${String.format("%.1f", session.tonalityScore)}",
                            fontSize = 12.sp,
                            color = DyllaOnSurfaceSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⚡ ${String.format("%.1fs", session.reactionTime)}",
                                fontSize = 11.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                            Text(
                                text = "🔇 ${session.fillerCount} fillers",
                                fontSize = 11.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                            Text(
                                text = "📈 ${session.uptalkCount} uptalk",
                                fontSize = 11.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                            Text(
                                text = "🎵 ${session.inflectionRating}",
                                fontSize = 11.sp,
                                color = DyllaOnSurfaceSecondary
                            )
                        }
                    }
                }
            }

            if (isTeamOwner) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DyllaBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Team Member")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DyllaOnSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = DyllaOnSurfaceSecondary
        )
    }
}
