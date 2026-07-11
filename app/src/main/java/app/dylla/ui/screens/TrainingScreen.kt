package app.dylla.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.DyllaBackground
import app.dylla.ui.theme.DyllaBackgroundSecondary
import app.dylla.ui.theme.DyllaBlue
import app.dylla.ui.theme.DyllaGreen
import app.dylla.ui.theme.DyllaOnSurface
import app.dylla.ui.theme.DyllaOnSurfaceSecondary
import app.dylla.ui.theme.DyllaOrange
import app.dylla.ui.theme.DyllaRed
import app.dylla.ui.theme.DyllaSurface

// ── Screen state ─────────────────────────────────────────────────────────────

private sealed class TrainingScreenState {
    data object Home : TrainingScreenState()
    data object ScenarioPicker : TrainingScreenState()
    data class Session(val scenario: TrainingScenario) : TrainingScreenState()
    data class ScoreCard(val score: TrainingSessionScore) : TrainingScreenState()
}

// ── Internal data classes ────────────────────────────────────────────────────

private enum class Difficulty(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val scenarioCount: Int,
    val xpMultiplier: Float
) {
    BEGINNER("Beginner", DyllaGreen, Icons.Default.StarOutline, 5, 1.0f),
    INTERMEDIATE("Intermediate", DyllaBlue, Icons.Default.StarHalf, 5, 1.5f),
    ADVANCED("Advanced", DyllaOrange, Icons.Default.Star, 4, 2.0f),
    EXPERT("Expert", DyllaRed, Icons.Default.Star, 3, 3.0f)
}

private data class TrainingScenario(
    val id: String,
    val title: String,
    val difficulty: Difficulty,
    val merchantName: String,
    val industry: String,
    val painPoint: String,
    val objections: List<String>
)

private enum class ChatRole { USER, MERCHANT, SYSTEM, COACH }

private data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

private data class VoiceMetric(
    val label: String,
    val score: Float,
    val maxScore: Float = 10f
)

private data class TrainingSessionScore(
    val scenario: TrainingScenario,
    val grade: String,
    val gradeLabel: String,
    val xpEarned: Int,
    val closedCount: Int,
    val totalObjections: Int,
    val messageCount: Int,
    val durationSeconds: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val bestLine: String,
    val coachTip: String,
    val voiceMetrics: List<VoiceMetric>
)

private data class RecentSession(
    val title: String,
    val industry: String,
    val grade: String,
    val xpEarned: Int
)

private data class CoachSuggestion(
    val objectionType: String,
    val responses: List<String>
)

// ── Sample data ──────────────────────────────────────────────────────────────

private val sampleScenarios = listOf(
    TrainingScenario(
        id = "s1",
        title = "The Rate Shopper",
        difficulty = Difficulty.BEGINNER,
        merchantName = "Tony Marinelli",
        industry = "Restaurant",
        painPoint = "Cash flow gap between catering orders",
        objections = listOf("Rate too high", "Need to compare")
    ),
    TrainingScenario(
        id = "s2",
        title = "Already Have Funding",
        difficulty = Difficulty.BEGINNER,
        merchantName = "Priya Sharma",
        industry = "Retail",
        painPoint = "Inventory before holiday season",
        objections = listOf("Already funded", "Happy with current provider")
    ),
    TrainingScenario(
        id = "s3",
        title = "Bad Past Experience",
        difficulty = Difficulty.BEGINNER,
        merchantName = "Mike O'Brien",
        industry = "Construction",
        painPoint = "Equipment purchase for new contract",
        objections = listOf("Got burned before", "Don't trust funders")
    ),
    TrainingScenario(
        id = "s4",
        title = "Spouse Needs to Decide",
        difficulty = Difficulty.BEGINNER,
        merchantName = "Lisa Chen",
        industry = "Salon",
        painPoint = "Expansion to second location",
        objections = listOf("Need to talk to partner", "Not ready")
    ),
    TrainingScenario(
        id = "s5",
        title = "Just Send Info",
        difficulty = Difficulty.BEGINNER,
        merchantName = "Dave Roberts",
        industry = "Auto Repair",
        painPoint = "New diagnostic equipment",
        objections = listOf("Just email me", "I'll call you back")
    ),
    TrainingScenario(
        id = "s6",
        title = "Hostile Gatekeeper",
        difficulty = Difficulty.INTERMEDIATE,
        merchantName = "Karen Wells",
        industry = "Medical",
        painPoint = "New imaging equipment",
        objections = listOf("He's busy", "We don't take these calls", "Put on the list")
    ),
    TrainingScenario(
        id = "s7",
        title = "Price Objection Stack",
        difficulty = Difficulty.INTERMEDIATE,
        merchantName = "James Thornton",
        industry = "Trucking",
        painPoint = "Fleet expansion",
        objections = listOf("Too expensive", "Bank offered less", "Hidden fees")
    ),
    TrainingScenario(
        id = "s8",
        title = "The Credit Worrier",
        difficulty = Difficulty.INTERMEDIATE,
        merchantName = "Maria Gonzalez",
        industry = "Restaurant",
        painPoint = "Kitchen renovation",
        objections = listOf("Bad credit", "Will this affect my score", "Been declined")
    ),
    TrainingScenario(
        id = "s9",
        title = "Time Waster",
        difficulty = Difficulty.INTERMEDIATE,
        merchantName = "Bob Mitchell",
        industry = "Landscaping",
        painPoint = "Seasonal equipment",
        objections = listOf("Let me think about it", "Call me next month", "Not urgent")
    ),
    TrainingScenario(
        id = "s10",
        title = "Know-It-All Owner",
        difficulty = Difficulty.INTERMEDIATE,
        merchantName = "Steve Park",
        industry = "Tech",
        painPoint = "Working capital for hiring",
        objections = listOf("I know the rates", "You're all the same", "I'll do it myself")
    ),
    TrainingScenario(
        id = "s11",
        title = "The Angry Merchant",
        difficulty = Difficulty.ADVANCED,
        merchantName = "Frank DiMaggio",
        industry = "Construction",
        painPoint = "Payroll gap on a major project",
        objections = listOf("Stop calling me", "I said no", "Reported you", "Do not call list")
    ),
    TrainingScenario(
        id = "s12",
        title = "Multiple Position Stress",
        difficulty = Difficulty.ADVANCED,
        merchantName = "Rachel Kim",
        industry = "E-commerce",
        painPoint = "Inventory for Q4 surge",
        objections = listOf("Already have 3 positions", "Tapped out", "Revenue dropping")
    ),
    TrainingScenario(
        id = "s13",
        title = "Legal Threats",
        difficulty = Difficulty.ADVANCED,
        merchantName = "Victor Haines",
        industry = "Real Estate",
        painPoint = "Bridge financing for flip",
        objections = listOf("My lawyer says no", "This is predatory", "I'll sue")
    ),
    TrainingScenario(
        id = "s14",
        title = "BK Recovery Play",
        difficulty = Difficulty.ADVANCED,
        merchantName = "Sandra Ellis",
        industry = "Retail",
        painPoint = "Post-bankruptcy rebuild",
        objections = listOf("Just out of BK", "No one will fund me", "Why should I trust you")
    ),
    TrainingScenario(
        id = "s15",
        title = "C-Suite Decision Maker",
        difficulty = Difficulty.EXPERT,
        merchantName = "Richard Blackwell",
        industry = "Healthcare",
        painPoint = "Practice acquisition financing",
        objections = listOf(
            "I only work with banks",
            "Your product is beneath us",
            "Show me the math",
            "I want equity terms"
        )
    ),
    TrainingScenario(
        id = "s16",
        title = "Competitor Steal-Back",
        difficulty = Difficulty.EXPERT,
        merchantName = "Angela Torres",
        industry = "Logistics",
        painPoint = "Fleet upgrade during contract ramp",
        objections = listOf(
            "Locked into contract",
            "Happy with current funder",
            "Penalty to switch",
            "Prove you're better"
        )
    ),
    TrainingScenario(
        id = "s17",
        title = "The Negotiator",
        difficulty = Difficulty.EXPERT,
        merchantName = "Daniel Kessler",
        industry = "Manufacturing",
        painPoint = "Raw materials bulk purchase",
        objections = listOf(
            "I want half that rate",
            "My CPA said no",
            "Counter-offer only",
            "I'll take my business elsewhere"
        )
    )
)

private val sampleRecentSessions = listOf(
    RecentSession("The Rate Shopper", "Restaurant", "A", 120),
    RecentSession("Price Objection Stack", "Trucking", "B+", 95),
    RecentSession("Hostile Gatekeeper", "Medical", "A+", 150),
    RecentSession("Bad Past Experience", "Construction", "B", 80),
    RecentSession("Multiple Position Stress", "E-commerce", "C+", 60)
)

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun levelFromXp(xp: Int): Int = (xp / 500) + 1

private fun rankTitle(level: Int): String = when (level) {
    in 1..2 -> "Rookie"
    in 3..4 -> "Rep"
    in 5..7 -> "Closer"
    in 8..9 -> "Elite Closer"
    else -> "Funding King"
}

private fun gradeColor(grade: String): Color = when {
    grade.startsWith("A") -> DyllaGreen
    grade.startsWith("B") -> DyllaBlue
    grade.startsWith("C") -> DyllaOrange
    else -> DyllaRed
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

// ── Main composable ──────────────────────────────────────────────────────────

@Composable
fun TrainingScreen() {
    var screenState by remember { mutableStateOf<TrainingScreenState>(TrainingScreenState.Home) }

    var totalXp by remember { mutableIntStateOf(2350) }
    var totalSessions by remember { mutableIntStateOf(47) }
    var currentStreak by remember { mutableIntStateOf(5) }
    var bestStreak by remember { mutableIntStateOf(12) }
    var objectionsHandled by remember { mutableIntStateOf(143) }

    val level = levelFromXp(totalXp)
    val rank = rankTitle(level)
    val xpInLevel = totalXp % 500
    val xpToNext = 500

    when (val state = screenState) {
        is TrainingScreenState.Home -> {
            TrainingHomeView(
                level = level,
                rank = rank,
                totalXp = totalXp,
                xpInLevel = xpInLevel,
                xpToNext = xpToNext,
                totalSessions = totalSessions,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                objectionsHandled = objectionsHandled,
                recentSessions = sampleRecentSessions,
                onStartTraining = { screenState = TrainingScreenState.ScenarioPicker }
            )
        }

        is TrainingScreenState.ScenarioPicker -> {
            ScenarioPickerView(
                scenarios = sampleScenarios,
                onClose = { screenState = TrainingScreenState.Home },
                onSelectScenario = { scenario ->
                    screenState = TrainingScreenState.Session(scenario)
                }
            )
        }

        is TrainingScreenState.Session -> {
            RoleplaySessionView(
                scenario = state.scenario,
                onEnd = { score ->
                    totalXp += score.xpEarned
                    totalSessions += 1
                    objectionsHandled += score.closedCount
                    screenState = TrainingScreenState.ScoreCard(score)
                }
            )
        }

        is TrainingScreenState.ScoreCard -> {
            ScoreCardView(
                score = state.score,
                onRetry = {
                    screenState = TrainingScreenState.Session(state.score.scenario)
                },
                onBackToTraining = {
                    screenState = TrainingScreenState.Home
                }
            )
        }
    }
}

// ── Training Home ────────────────────────────────────────────────────────────

@Composable
private fun TrainingHomeView(
    level: Int,
    rank: String,
    totalXp: Int,
    xpInLevel: Int,
    xpToNext: Int,
    totalSessions: Int,
    currentStreak: Int,
    bestStreak: Int,
    objectionsHandled: Int,
    recentSessions: List<RecentSession>,
    onStartTraining: () -> Unit
) {
    Scaffold(
        containerColor = DyllaBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartTraining,
                containerColor = DyllaBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Start Training", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Training",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOnSurface
                )
            }

            // Stats card
            item {
                StatsCard(level, rank, totalXp, xpInLevel, xpToNext, totalSessions)
            }

            // Readiness banner
            item {
                ReadinessBanner(level, xpInLevel, xpToNext)
            }

            // Streak banner
            item {
                StreakBanner(currentStreak, bestStreak, objectionsHandled)
            }

            // Difficulty section header
            item {
                Text(
                    "Choose Difficulty",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                DifficultyGrid()
            }

            // Recent sessions header
            item {
                Text(
                    "Recent Sessions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(recentSessions) { session ->
                RecentSessionRow(session)
            }

            // Bottom spacer for FAB clearance
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    level: Int,
    rank: String,
    totalXp: Int,
    xpInLevel: Int,
    xpToNext: Int,
    totalSessions: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Level $level",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )
                    Text(
                        rank,
                        fontSize = 14.sp,
                        color = DyllaBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$totalXp XP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaOnSurface
                    )
                    Text(
                        "$totalSessions sessions",
                        fontSize = 13.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { xpInLevel.toFloat() / xpToNext.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = DyllaBlue,
                trackColor = DyllaBackgroundSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "$xpInLevel / $xpToNext XP to Level ${level + 1}",
                fontSize = 12.sp,
                color = DyllaOnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun ReadinessBanner(level: Int, xpInLevel: Int, xpToNext: Int) {
    val ready = level >= 3
    val bgColor = if (ready) DyllaGreen.copy(alpha = 0.15f) else DyllaOrange.copy(alpha = 0.15f)
    val textColor = if (ready) DyllaGreen else DyllaOrange
    val progressPercent = if (ready) 100 else ((xpInLevel.toFloat() / xpToNext) * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ready) "✅" else "💪", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (ready) "Ready for Live Calls" else "Keep Training",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
            if (!ready) {
                Text(
                    "$progressPercent%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun StreakBanner(currentStreak: Int, bestStreak: Int, objectionsHandled: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaOrange.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "$currentStreak day streak",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaOrange
                    )
                    Text(
                        "Best: $bestStreak",
                        fontSize = 12.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✅", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$objectionsHandled objections handled",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurface
                )
            }
        }
    }
}

@Composable
private fun DifficultyGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(Difficulty.entries.toList()) { difficulty ->
            DifficultyCard(difficulty)
        }
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    difficulty.icon,
                    contentDescription = null,
                    tint = difficulty.color,
                    modifier = Modifier.size(28.dp)
                )
                Box(
                    modifier = Modifier
                        .background(
                            difficulty.color.copy(alpha = 0.12f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${difficulty.xpMultiplier}x XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = difficulty.color
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                difficulty.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurface
            )
            Text(
                "${difficulty.scenarioCount} scenarios",
                fontSize = 12.sp,
                color = DyllaOnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun RecentSessionRow(session: RecentSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(DyllaBackgroundSecondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        session.industry,
                        fontSize = 11.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "+${session.xpEarned} XP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurfaceSecondary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            gradeColor(session.grade).copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.grade,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor(session.grade)
                    )
                }
            }
        }
    }
}

// ── Scenario Picker ──────────────────────────────────────────────────────────

@Composable
private fun ScenarioPickerView(
    scenarios: List<TrainingScenario>,
    onClose: () -> Unit,
    onSelectScenario: (TrainingScenario) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DyllaBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DyllaSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Choose Scenario",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DyllaOnSurface
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = DyllaOnSurfaceSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Random scenario card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .clickable { onSelectScenario(scenarios.random()) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DyllaBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Random Scenario",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Grouped by difficulty
            Difficulty.entries.forEach { difficulty ->
                val grouped = scenarios.filter { it.difficulty == difficulty }
                if (grouped.isNotEmpty()) {
                    item {
                        Text(
                            difficulty.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DyllaOnSurface,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(grouped) { scenario ->
                        ScenarioRow(scenario) { onSelectScenario(scenario) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioRow(scenario: TrainingScenario, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    scenario.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DyllaOnSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            scenario.difficulty.color.copy(alpha = 0.12f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        scenario.difficulty.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scenario.difficulty.color
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "${scenario.merchantName}  •  ${scenario.industry}",
                fontSize = 13.sp,
                color = DyllaOnSurfaceSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                scenario.painPoint,
                fontSize = 13.sp,
                color = DyllaOnSurface.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "${scenario.objections.size} objections",
                fontSize = 12.sp,
                color = DyllaOnSurfaceSecondary
            )
        }
    }
}

// ── Roleplay Session ─────────────────────────────────────────────────────────

@Composable
private fun RoleplaySessionView(
    scenario: TrainingScenario,
    onEnd: (TrainingSessionScore) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var messageCount by remember { mutableIntStateOf(0) }
    var showCoachPopup by remember { mutableStateOf(false) }
    var showTypingIndicator by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "sys1",
                role = ChatRole.SYSTEM,
                content = "Scenario: ${scenario.title} — ${scenario.difficulty.label}"
            ),
            ChatMessage(
                id = "m1",
                role = ChatRole.MERCHANT,
                content = "Hello? Yeah, who's this?"
            ),
            ChatMessage(
                id = "coach1",
                role = ChatRole.COACH,
                content = "Start with a confident intro. Use their name and reference their business."
            )
        )
    }

    val voiceScores = remember {
        listOf(
            VoiceMetric("Energy", 7.5f),
            VoiceMetric("Reaction", 8.2f),
            VoiceMetric("Tonality", 6.8f),
            VoiceMetric("Fillers", 9.0f)
        )
    }

    val coachSuggestion = remember {
        CoachSuggestion(
            objectionType = scenario.objections.firstOrNull() ?: "General",
            responses = listOf(
                "I completely understand — most of our clients felt the same way before seeing the numbers.",
                "That's fair. What if I could show you the exact terms in under 2 minutes?",
                "No pressure at all. Let me just ask one quick question to see if this even makes sense for you."
            )
        )
    }

    fun buildFullSessionScore(): TrainingSessionScore = TrainingSessionScore(
        scenario = scenario,
        grade = "A",
        gradeLabel = "Excellent",
        xpEarned = (130 * scenario.difficulty.xpMultiplier).toInt(),
        closedCount = scenario.objections.size - 1,
        totalObjections = scenario.objections.size,
        messageCount = messages.size,
        durationSeconds = 247,
        strengths = listOf(
            "Strong opening with personalized hook",
            "Effective use of social proof",
            "Maintained composure under pressure"
        ),
        improvements = listOf(
            "Could ask more discovery questions early",
            "Try mirroring the merchant's language"
        ),
        bestLine = "\"I hear you — most business owners in ${scenario.industry.lowercase()} feel the same way. That's exactly why our program is built different.\"",
        coachTip = "When a merchant says '${scenario.objections.firstOrNull() ?: "no"}', pause for 2 seconds before responding. The silence shows confidence.",
        voiceMetrics = listOf(
            VoiceMetric("Inflection", 7.8f),
            VoiceMetric("Reaction", 8.5f),
            VoiceMetric("Tonality", 7.2f),
            VoiceMetric("Fillers", 8.8f),
            VoiceMetric("Uptalk", 6.5f),
            VoiceMetric("Energy", 8.0f)
        )
    )

    fun buildEarlyEndScore(): TrainingSessionScore = TrainingSessionScore(
        scenario = scenario,
        grade = "B+",
        gradeLabel = "Good",
        xpEarned = (85 * scenario.difficulty.xpMultiplier).toInt(),
        closedCount = 1,
        totalObjections = scenario.objections.size,
        messageCount = messages.size,
        durationSeconds = 124,
        strengths = listOf(
            "Good initial rapport building",
            "Handled first objection well"
        ),
        improvements = listOf(
            "Ended session early — push through resistance",
            "Need more closing techniques",
            "Work on transition from discovery to pitch"
        ),
        bestLine = "\"I understand the hesitation — let's look at this from a pure ROI perspective.\"",
        coachTip = "Ending early leaves money on the table. In real calls, the deal often closes in the last 2 minutes.",
        voiceMetrics = listOf(
            VoiceMetric("Inflection", 6.5f),
            VoiceMetric("Reaction", 7.0f),
            VoiceMetric("Tonality", 6.8f),
            VoiceMetric("Fillers", 7.5f),
            VoiceMetric("Uptalk", 5.8f),
            VoiceMetric("Energy", 7.2f)
        )
    )

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return

        messages.add(
            ChatMessage(
                id = "u${messages.size}",
                role = ChatRole.USER,
                content = text
            )
        )
        inputText = ""
        messageCount++
        showTypingIndicator = true

        // End session after enough exchanges
        if (messageCount >= 6) {
            showTypingIndicator = false
            onEnd(buildFullSessionScore())
            return
        }

        // Simulated merchant replies
        val merchantReplies = listOf(
            "Look, I've heard this pitch before. What makes you different?",
            "We're doing fine right now. Why would I take on more debt?",
            "My accountant told me to stay away from these things.",
            "How much is this gonna cost me? Give me real numbers.",
            "I already got denied by two companies. What's the point?",
            "Alright, you've got 30 seconds. Make it count."
        )
        messages.add(
            ChatMessage(
                id = "mr${messages.size}",
                role = ChatRole.MERCHANT,
                content = merchantReplies[messageCount.coerceAtMost(merchantReplies.size - 1)]
            )
        )
        showTypingIndicator = false

        // Show coach popup mid-conversation
        if (messageCount == 3) {
            showCoachPopup = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DyllaBackground)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DyllaSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                scenario.merchantName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurface
            )
            Button(
                onClick = { onEnd(buildEarlyEndScore()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DyllaRed.copy(alpha = 0.12f),
                    contentColor = DyllaRed
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("End", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        // Info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DyllaSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoPill("${scenario.industry} • \$45K/mo")
            InfoPill("${messages.count { it.role == ChatRole.USER || it.role == ChatRole.MERCHANT }} msgs")
            Box(
                modifier = Modifier
                    .background(
                        scenario.difficulty.color.copy(alpha = 0.12f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    scenario.difficulty.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scenario.difficulty.color
                )
            }
        }

        // Voice Coach HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DyllaBackgroundSecondary)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            voiceScores.forEach { metric ->
                VoiceHudPill(metric)
            }
        }

        // Chat area
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }

                if (showTypingIndicator) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Coach popup overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showCoachPopup,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CoachPopup(
                    suggestion = coachSuggestion,
                    onDismiss = { showCoachPopup = false },
                    onSelectResponse = { response ->
                        inputText = response
                        showCoachPopup = false
                    }
                )
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DyllaSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { /* mic action */ },
                modifier = Modifier
                    .size(42.dp)
                    .background(DyllaBackgroundSecondary, CircleShape)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = DyllaOnSurfaceSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Handle the objection...",
                        fontSize = 14.sp,
                        color = DyllaOnSurfaceSecondary
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DyllaBlue,
                    unfocusedBorderColor = Color(0xFFD1D1D6),
                    focusedContainerColor = DyllaBackgroundSecondary,
                    unfocusedContainerColor = DyllaBackgroundSecondary
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            IconButton(
                onClick = { sendMessage() },
                modifier = Modifier
                    .size(42.dp)
                    .background(DyllaBlue, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .background(DyllaBackgroundSecondary, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = DyllaOnSurfaceSecondary)
    }
}

@Composable
private fun VoiceHudPill(metric: VoiceMetric) {
    val color = when {
        metric.score >= 8f -> DyllaGreen
        metric.score >= 6f -> DyllaOrange
        else -> DyllaRed
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(metric.label, fontSize = 11.sp, color = DyllaOnSurfaceSecondary)
            Text(
                "${"%.1f".format(metric.score)}/10",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    when (message.role) {
        ChatRole.USER -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            DyllaBlue,
                            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        message.content,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        ChatRole.MERCHANT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            DyllaBackgroundSecondary,
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        message.content,
                        color = DyllaOnSurface,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        ChatRole.SYSTEM -> {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.content,
                    fontSize = 12.sp,
                    color = DyllaOnSurfaceSecondary,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        ChatRole.COACH -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = DyllaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        message.content,
                        fontSize = 13.sp,
                        color = DyllaOnSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 = animateDotOffset(infiniteTransition, 0)
    val dot2 = animateDotOffset(infiniteTransition, 150)
    val dot3 = animateDotOffset(infiniteTransition, 300)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    DyllaBackgroundSecondary,
                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1, dot2, dot3).forEach { offset ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = offset.dp)
                            .background(DyllaOnSurfaceSecondary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun animateDotOffset(
    infiniteTransition: InfiniteTransition,
    delayMs: Int
): Float {
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at delayMs
                -6f at delayMs + 200
                0f at delayMs + 400
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_$delayMs"
    )
    return offset
}

@Composable
private fun CoachPopup(
    suggestion: CoachSuggestion,
    onDismiss: () -> Unit,
    onSelectResponse: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DyllaSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = DyllaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Objection Detected: ${suggestion.objectionType}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaOnSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DyllaOnSurfaceSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            suggestion.responses.forEach { response ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectResponse(response) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DyllaBlue.copy(alpha = 0.06f)
                    )
                ) {
                    Text(
                        response,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = DyllaOnSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ── Score Card ───────────────────────────────────────────────────────────────

@Composable
private fun ScoreCardView(
    score: TrainingSessionScore,
    onRetry: () -> Unit,
    onBackToTraining: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DyllaBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Giant grade letter
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    score.grade,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor(score.grade)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    score.scenario.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DyllaOnSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(DyllaBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "+${score.xpEarned} XP Earned",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DyllaBlue
                    )
                }
            }
        }

        // Stat pills row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill("Closed ${score.closedCount}/${score.totalObjections}")
                StatPill("${score.messageCount} Messages")
                StatPill(formatDuration(score.durationSeconds))
            }
        }

        // Strengths
        item {
            ScoreSectionHeader("Strengths", DyllaGreen)
        }
        items(score.strengths) { strength ->
            FeedbackBullet(strength, DyllaGreen)
        }

        // Areas to Improve
        item {
            ScoreSectionHeader("Areas to Improve", DyllaOrange)
        }
        items(score.improvements) { improvement ->
            FeedbackBullet(improvement, DyllaOrange)
        }

        // Best Line
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaBlue.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Best Line",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DyllaBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        score.bestLine,
                        fontSize = 14.sp,
                        color = DyllaOnSurface,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Coach Tip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = DyllaOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Coach Tip",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DyllaOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        score.coachTip,
                        fontSize = 14.sp,
                        color = DyllaOnSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Voice Analysis header
        item {
            Text(
                "Voice Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DyllaOnSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Voice metrics card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DyllaSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    score.voiceMetrics.forEach { metric ->
                        VoiceMetricBar(metric)
                    }
                }
            }
        }

        // Action buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DyllaBlue),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Retry Scenario",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onBackToTraining,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DyllaBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Back to Training",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .background(DyllaBackgroundSecondary, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DyllaOnSurface
        )
    }
}

@Composable
private fun ScoreSectionHeader(title: String, color: Color) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Composable
private fun FeedbackBullet(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 6.dp)
                .background(color, CircleShape)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = DyllaOnSurface,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun VoiceMetricBar(metric: VoiceMetric) {
    val fraction = (metric.score / metric.maxScore).coerceIn(0f, 1f)
    val color = when {
        metric.score >= 8f -> DyllaGreen
        metric.score >= 6f -> DyllaOrange
        else -> DyllaRed
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                metric.label,
                fontSize = 13.sp,
                color = DyllaOnSurface
            )
            Text(
                "${"%.1f".format(metric.score)}/${metric.maxScore.toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DyllaBackgroundSecondary
        )
    }
}
