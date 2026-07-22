package app.dylla.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dylla.ui.theme.*
import app.dylla.service.VoiceAnalyzer
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

data class GameObstacle(
    val x: Float,
    val gapTop: Float,
    val gapHeight: Float,
    var passed: Boolean = false
)

private val objections = listOf(
    "What’s your rate?",
    "I already have a lender",
    "Send me an email",
    "I’m not interested",
    "How did you get my number?",
    "We’re doing fine",
    "I need to talk to my partner",
    "That sounds too expensive",
    "I’ve heard bad things about MCAs",
    "Can you call back later?"
)

@Composable
fun VoiceGameScreen(
    analyzer: VoiceAnalyzer,
    merchantSpeaking: Boolean = false
) {
    var planeY by remember { mutableFloatStateOf(0.5f) }
    var obstacles by remember { mutableStateOf(listOf<GameObstacle>()) }
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var inZone by remember { mutableStateOf(false) }
    var coachMessage by remember { mutableStateOf("") }
    var showCoachMessage by remember { mutableStateOf(false) }
    var liveObjection by remember { mutableStateOf<String?>(null) }

    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)

            val volume = analyzer.normalizedVolume
            val pitch = analyzer.normalizedPitch
            val target = volume * 0.4f + pitch * 0.6f
            planeY += (target - planeY) * 0.15f
            planeY = planeY.coerceIn(0f, 1f)

            scrollOffset += 1.5f

            val currentObstacles = obstacles.toMutableList()

            if (currentObstacles.isEmpty() || (currentObstacles.last().x - scrollOffset) < 120f) {
                currentObstacles.add(
                    GameObstacle(
                        x = scrollOffset + 300f,
                        gapTop = Random.nextFloat() * 0.3f + 0.15f,
                        gapHeight = Random.nextFloat() * 0.2f + 0.25f
                    )
                )
            }

            currentObstacles.removeAll { (it.x - scrollOffset) < -50f }

            val isInZone = planeY in 0.3f..0.7f
            if (isInZone && !merchantSpeaking) {
                if (!inZone) {
                    combo++
                }
                score += max(1, combo / 5)
            } else {
                if (inZone) {
                    combo = 0
                }
            }
            inZone = isInZone

            currentObstacles.forEachIndexed { index, obstacle ->
                if (!obstacle.passed && (obstacle.x - scrollOffset) < 0f) {
                    currentObstacles[index] = obstacle.copy(passed = true)
                }
            }

            obstacles = currentObstacles
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)

            val message = when {
                analyzer.reactionTime > 3f -> "Respond faster!"
                analyzer.reactionTime < 0.4f -> "Slow down a bit"
                analyzer.energyLevel == VoiceAnalyzer.EnergyLevel.QUIET -> "Project your voice!"
                analyzer.energyLevel == VoiceAnalyzer.EnergyLevel.LOUD -> "Ease up a bit"
                analyzer.fillerCount > 3 -> "Drop the fillers!"
                analyzer.uptalkCount > 1 -> "End with authority"
                Random.nextFloat() < 0.3f -> {
                    liveObjection = objections[Random.nextInt(objections.size)]
                    delay(4000)
                    liveObjection = null
                    null
                }
                else -> null
            }

            if (message != null) {
                coachMessage = message
                showCoachMessage = true
                delay(2500)
                showCoachMessage = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF4FC3F7)
                    )
                )
            )

            drawRect(
                color = DyllaGreen.copy(alpha = 0.12f),
                topLeft = Offset(0f, h * 0.3f),
                size = Size(w, h * 0.4f)
            )

            obstacles.forEach { obstacle ->
                val ox = obstacle.x - scrollOffset
                if (ox in -30f..w + 30f) {
                    val obstacleWidth = 28f
                    val topHeight = obstacle.gapTop * h
                    val gapBottom = (obstacle.gapTop + obstacle.gapHeight) * h

                    drawRect(
                        color = DyllaRed,
                        topLeft = Offset(ox, 0f),
                        size = Size(obstacleWidth, topHeight)
                    )

                    drawRect(
                        color = DyllaGreen.copy(alpha = 0.3f),
                        topLeft = Offset(ox, topHeight),
                        size = Size(obstacleWidth, gapBottom - topHeight)
                    )

                    drawRect(
                        color = DyllaRed,
                        topLeft = Offset(ox, gapBottom),
                        size = Size(obstacleWidth, h - gapBottom)
                    )
                }
            }

            val planeX = 50f
            val planePosY = planeY * h
            val rotation = (planeY - 0.5f) * -30f

            rotate(rotation, pivot = Offset(planeX, planePosY)) {
                val planeText = "✈️"
                val layoutResult = textMeasurer.measure(
                    planeText,
                    style = TextStyle(fontSize = 22.sp)
                )
                drawText(
                    layoutResult,
                    topLeft = Offset(
                        planeX - layoutResult.size.width / 2f,
                        planePosY - layoutResult.size.height / 2f
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (combo > 1) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "🔥 $combo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "$score",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LevelBar("VOL", analyzer.normalizedVolume, DyllaGreen, Modifier.weight(1f))
            LevelBar("PITCH", analyzer.normalizedPitch, DyllaBlue, Modifier.weight(1f))
            LevelBar("TONE", analyzer.normalizedTone, DyllaPurple, Modifier.weight(1f))
        }

        AnimatedVisibility(
            visible = showCoachMessage,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DyllaBlue.copy(alpha = 0.9f)
            ) {
                Text(
                    text = coachMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (liveObjection != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = liveObjection ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DyllaOrange,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}
