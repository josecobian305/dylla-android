package app.dylla.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dylla.models.Difficulty
import app.dylla.models.RoleplayMessage
import app.dylla.models.RoleplayScenario
import app.dylla.models.SessionRecord
import app.dylla.models.SessionScoreResult
import app.dylla.models.TrainingStats
import app.dylla.services.PersistenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for the AI Roleplay training screen.
 *
 * Fully local simulation -- merchant responses are generated from scenario
 * objections and keyword matching. No API calls required.
 * Scoring is computed locally based on message count, objection handling, etc.
 */
class RoleplayViewModel : ViewModel() {

    // ── Session state ──────────────────────────────────────────────

    var currentScenario by mutableStateOf<RoleplayScenario?>(null)
        private set

    var messages by mutableStateOf<List<RoleplayMessage>>(emptyList())
        private set

    var sessionActive by mutableStateOf(false)
        private set

    var sessionComplete by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isScoring by mutableStateOf(false)
        private set

    var inputText by mutableStateOf("")

    var messageCount by mutableIntStateOf(0)
        private set

    var elapsedTime by mutableLongStateOf(0L)
        private set

    var scoreResult by mutableStateOf<SessionScoreResult?>(null)
        private set

    var stats by mutableStateOf(TrainingStats())
        private set

    // ── Coach popup state ──────────────────────────────────────────

    var showCoachPopup by mutableStateOf(false)
        private set

    var detectedObjection by mutableStateOf<String?>(null)
        private set

    var recommendedResponses by mutableStateOf<List<String>>(emptyList())
        private set

    // ── Internal tracking ──────────────────────────────────────────

    private var interest = 50
    private var objectionsHandled = 0
    private var questionsAsked = 0
    private var rapportMoments = 0
    private var sessionStartTime = 0L
    private var timerJob: Job? = null
    private var lastProspectMessage = ""

    // ── Init ───────────────────────────────────────────────────────

    init {
        stats = PersistenceManager.loadTrainingStats()
    }

    // ── Session lifecycle ──────────────────────────────────────────

    /**
     * Start a new roleplay session with the given scenario.
     */
    fun startSession(scenario: RoleplayScenario) {
        currentScenario = scenario
        messages = emptyList()
        messageCount = 0
        elapsedTime = 0L
        interest = 50
        objectionsHandled = 0
        questionsAsked = 0
        rapportMoments = 0
        sessionActive = true
        sessionComplete = false
        isLoading = false
        isScoring = false
        scoreResult = null
        showCoachPopup = false
        detectedObjection = null
        recommendedResponses = emptyList()
        inputText = ""
        lastProspectMessage = ""

        sessionStartTime = System.currentTimeMillis()
        startTimer()

        // Generate opening line from prospect
        val opening = generateOpening(scenario)
        addProspectMessage(opening)
    }

    /**
     * Send the user's message and generate a prospect response.
     */
    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || !sessionActive || isLoading) return

        inputText = ""
        isLoading = true
        showCoachPopup = false

        // Add user message
        val userMsg = RoleplayMessage(role = ROLE_USER, content = text)
        messages = messages + userMsg
        messageCount = messages.size

        // Analyze user message for quality signals
        analyzeUserMessage(text)

        // Generate prospect response with a slight delay
        viewModelScope.launch {
            delay(800L + (Math.random() * 1200L).toLong())

            val scenario = currentScenario ?: return@launch
            val response = generateProspectResponse(text, scenario)
            addProspectMessage(response)

            // Check for coach tips based on prospect response
            detectObjectionAndSuggest(response)

            isLoading = false
        }
    }

    /**
     * End the session and compute scores.
     */
    fun endSession() {
        if (!sessionActive || messages.size < 4) return

        sessionActive = false
        sessionComplete = true
        isScoring = true
        timerJob?.cancel()

        viewModelScope.launch {
            delay(1500L) // Brief scoring animation delay

            val scenario = currentScenario ?: return@launch
            val result = computeScore(scenario)
            scoreResult = result

            // Update training stats
            stats.totalSessions++
            stats.objectionsHandled += objectionsHandled

            val xp = result.xp
            stats.totalXP += xp

            // Streak tracking
            val now = Date()
            val oneDayMs = 86_400_000L
            val lastSession = stats.sessionHistory.lastOrNull()
            if (lastSession != null && now.time - lastSession.date.time < oneDayMs * 2) {
                stats.currentStreak++
            } else {
                stats.currentStreak = 1
            }
            if (stats.currentStreak > stats.longestStreak) {
                stats.longestStreak = stats.currentStreak
            }

            // Record session
            stats.sessionHistory.add(
                SessionRecord(
                    scenarioTitle = scenario.title,
                    industry = scenario.industry,
                    difficulty = scenario.difficulty,
                    grade = result.grade,
                    xpEarned = xp,
                    date = now
                )
            )

            PersistenceManager.saveTrainingStats(stats)
            isScoring = false
        }
    }

    /**
     * Use a recommended response from the coach popup.
     */
    fun useRecommendedResponse(response: String) {
        inputText = response
        showCoachPopup = false
    }

    // ── Opening generation ─────────────────────────────────────────

    private fun generateOpening(scenario: RoleplayScenario): String {
        val name = scenario.merchantName
        val openings = when (scenario.difficulty) {
            Difficulty.BEGINNER -> listOf(
                "Hello?",
                "Yeah, who's this?",
                "This is $name.",
                "Yeah, go ahead."
            )
            Difficulty.INTERMEDIATE -> listOf(
                "Yeah, who's this?",
                "I'm kinda busy right now.",
                "Make it quick, I got customers.",
                "Who gave you this number?"
            )
            Difficulty.ADVANCED -> listOf(
                "I'm busy, make it quick.",
                "Not interested.",
                "Who is this? I'm in the middle of something.",
                "Look, I get five of these calls a day."
            )
            Difficulty.EXPERT -> listOf(
                "Not interested.",
                "Yeah, no thanks. Bye.",
                "How'd you even get this number?",
                "I said no already. Stop calling."
            )
        }
        return openings.random()
    }

    // ── Prospect response generation ───────────────────────────────

    private fun generateProspectResponse(userMessage: String, scenario: RoleplayScenario): String {
        val msg = userMessage.lowercase()
        val turnCount = messages.count { it.role == ROLE_USER }

        // Interest adjustments
        if (msg.length > 200) interest -= 5
        if (msg.contains("?")) {
            questionsAsked++
            interest += 3
        }
        if (msg.contains(scenario.merchantName.lowercase().split(" ").last())) {
            rapportMoments++
            interest += 5
        }

        // Check for objection handling keywords
        val handlesObjection = OBJECTION_HANDLERS.any { msg.contains(it) }
        if (handlesObjection && OBJECTION_KEYWORDS.any { lastProspectMessage.lowercase().contains(it) }) {
            objectionsHandled++
            interest += 8
        }

        // Generate response based on interest level and difficulty
        val response = when {
            // Early resistance for hard/expert
            (scenario.difficulty == Difficulty.ADVANCED || scenario.difficulty == Difficulty.EXPERT)
                && turnCount <= 2 -> generateResistantResponse()

            // Objection phase
            interest < 40 || (scenario.difficulty >= Difficulty.INTERMEDIATE && turnCount <= 3) ->
                generateObjection(scenario)

            // Warming up
            interest in 40..65 -> generateCuriousResponse()

            // Interested
            interest > 65 -> generateInterestedResponse(scenario)

            else -> generateNeutralResponse()
        }

        interest = interest.coerceIn(5, 95)
        return response
    }

    private fun generateResistantResponse(): String {
        val responses = listOf(
            "Not interested.",
            "Nah, I'm good.",
            "I said no already.",
            "Look, I gotta go.",
            "We're not looking for that right now.",
            "Yeah I've heard this before.",
            "I don't take cold calls.",
            "How'd you even get my number?"
        )
        interest -= 10
        return responses.random()
    }

    private fun generateObjection(scenario: RoleplayScenario): String {
        // Use scenario-specific objections first (unused ones)
        if (scenario.objections.isNotEmpty()) {
            val unused = scenario.objections.filter { obj ->
                messages.none { it.role == ROLE_PROSPECT && it.content.contains(obj, ignoreCase = true) }
            }
            if (unused.isNotEmpty()) {
                interest -= 5
                return unused.random()
            }
        }

        // Generic fallback objections
        val generic = listOf(
            "I need to think about it.",
            "Can you send me something in writing?",
            "I need to talk to my partner first.",
            "What's this gonna cost me?",
            "We already have something in place.",
            "How is this different from everyone else?"
        )
        interest -= 3
        return generic.random()
    }

    private fun generateCuriousResponse(): String {
        val responses = listOf(
            "Hmm, okay. Tell me more about that.",
            "I mean... how does that actually work?",
            "And what's the catch?",
            "How long does something like that take?",
            "What would I need to do on my end?",
            "I've heard about stuff like this. What makes you different?",
            "Alright, I'm listening. But make it quick.",
            "Okay, so what would the first step be?"
        )
        interest += 3
        return responses.random()
    }

    private fun generateInterestedResponse(scenario: RoleplayScenario): String {
        val responses = listOf(
            "Alright, that actually sounds interesting. What do I need to get started?",
            "Okay, send me the details. What's your email?",
            "When can we set up a time to go over this?",
            "How quickly could we get this moving?",
            "My partner was just talking about this. Can you send me some info?",
            "Alright, you got my attention. What's the next step?"
        )
        interest += 5
        return responses.random()
    }

    private fun generateNeutralResponse(): String {
        val responses = listOf("Uh huh.", "Okay.", "Yeah, I hear you.", "Go on.", "Right.")
        return responses.random()
    }

    // ── User message analysis ──────────────────────────────────────

    private fun analyzeUserMessage(text: String) {
        val msg = text.lowercase()
        if (msg.length > 300) interest -= 3
        if (msg.contains("?")) interest += 2
        if (EMPATHY_KEYWORDS.any { msg.contains(it) }) {
            rapportMoments++
            interest += 4
        }
    }

    // ── Coach / objection detection ────────────────────────────────

    private fun detectObjectionAndSuggest(prospectMessage: String) {
        val msg = prospectMessage.lowercase()
        for ((keyword, suggestions) in OBJECTION_COACHING) {
            if (msg.contains(keyword)) {
                detectedObjection = keyword.replaceFirstChar { it.uppercase() }
                recommendedResponses = suggestions
                showCoachPopup = true
                return
            }
        }
        showCoachPopup = false
        detectedObjection = null
        recommendedResponses = emptyList()
    }

    private fun addProspectMessage(content: String) {
        val msg = RoleplayMessage(role = ROLE_PROSPECT, content = content)
        messages = messages + msg
        messageCount = messages.size
        lastProspectMessage = content
    }

    // ── Scoring ────────────────────────────────────────────────────

    private fun computeScore(scenario: RoleplayScenario): SessionScoreResult {
        val userMessages = messages.filter { it.role == ROLE_USER }
        val totalTurns = userMessages.size

        // Average message length
        val avgLength = if (userMessages.isNotEmpty()) {
            userMessages.map { it.content.length }.average().toFloat()
        } else 0f

        // Dimension scores
        val discoveryScore = (questionsAsked.coerceAtMost(5) * 20f).coerceIn(10f, 100f)

        val pitchScore = when {
            avgLength < 20 -> 30f; avgLength < 50 -> 55f
            avgLength < 120 -> 85f; avgLength < 200 -> 70f
            else -> 45f
        }

        val objectionScore = when {
            objectionsHandled >= 4 -> 95f; objectionsHandled >= 3 -> 85f
            objectionsHandled >= 2 -> 70f; objectionsHandled >= 1 -> 55f
            else -> 25f
        }

        val closingKeywords = listOf("next step", "get started", "set up", "schedule", "appointment", "send")
        val closingAttempts = userMessages.count { msg ->
            closingKeywords.any { msg.content.lowercase().contains(it) }
        }
        val closingScore = when {
            interest > 70 && closingAttempts >= 1 -> 90f
            interest > 50 && closingAttempts >= 1 -> 75f
            closingAttempts >= 1 -> 60f; interest > 60 -> 50f
            else -> 30f
        }

        val tonalityScore = when {
            avgLength < 30 -> 60f; avgLength < 80 -> 90f
            avgLength < 150 -> 70f; else -> 40f
        }

        val paceScore = when {
            totalTurns < 3 -> 30f; totalTurns in 3..5 -> 65f
            totalTurns in 6..12 -> 90f; totalTurns in 13..18 -> 75f
            else -> 55f
        }

        val rapportScore = when {
            rapportMoments >= 4 -> 95f; rapportMoments >= 3 -> 80f
            rapportMoments >= 2 -> 65f; rapportMoments >= 1 -> 50f
            else -> 25f
        }

        val allScores = listOf(
            discoveryScore, pitchScore, objectionScore,
            closingScore, tonalityScore, paceScore, rapportScore
        )
        val overall = allScores.average().toFloat()

        // Grade
        val grade = when {
            overall >= 90 -> "A"
            overall >= 80 -> "B+"
            overall >= 70 -> "B"
            overall >= 60 -> "C+"
            overall >= 50 -> "C"
            overall >= 40 -> "D"
            else -> "F"
        }

        // XP calculation based on difficulty multiplier
        val baseXP = (overall * 1.5f).toInt()
        val xp = baseXP * scenario.difficulty.xpMultiplier

        // Identify strengths and improvements
        val dimensionNames = listOf(
            "Discovery", "Pitch", "Objection Handling",
            "Closing", "Tonality", "Pace", "Rapport"
        )
        val scored = dimensionNames.zip(allScores).sortedByDescending { it.second }

        val strengths = scored.take(3).map { (dim, score) ->
            "$dim (${score.toInt()}/100): ${strengthNote(dim)}"
        }

        val improvements = scored.takeLast(2).map { (dim, score) ->
            "$dim (${score.toInt()}/100): ${improvementNote(dim, score)}"
        }

        // Best line from user
        val bestLine = userMessages
            .filter { it.content.contains("?") || EMPATHY_KEYWORDS.any { kw -> it.content.lowercase().contains(kw) } }
            .maxByOrNull { msg ->
                var score = 0
                if (msg.content.contains("?")) score += 3
                if (EMPATHY_KEYWORDS.any { msg.content.lowercase().contains(it) }) score += 5
                if (msg.content.length in 30..150) score += 2
                score
            }
            ?.content
            ?.take(120) ?: userMessages.firstOrNull()?.content?.take(120) ?: ""

        val tip = when {
            objectionScore < 50 -> "Practice acknowledging objections before redirecting. Try: 'I hear you on that. A lot of our clients felt the same way before...'"
            discoveryScore < 50 -> "Ask more open-ended questions early. The prospect should be talking more than you in the first 2 minutes."
            closingScore < 50 -> "Always ask for the next step before hanging up. Even 'Can I send you an email?' counts."
            rapportScore < 50 -> "Use the prospect's name and reference their specific business or industry."
            else -> "Strong call. Push yourself with a harder difficulty to keep growing."
        }

        return SessionScoreResult(
            grade = grade,
            strengths = strengths,
            improvements = improvements,
            bestLine = bestLine,
            tip = tip,
            objectionsClosed = objectionsHandled,
            xp = xp
        )
    }

    private fun strengthNote(dimension: String): String = when (dimension) {
        "Discovery" -> "Good questions that uncovered the prospect's needs."
        "Pitch" -> "Clear, concise value proposition."
        "Objection Handling" -> "Handled pushback with confidence."
        "Closing" -> "Strong close with clear next steps."
        "Tonality" -> "Conversational and natural tone."
        "Pace" -> "Good conversation flow and timing."
        "Rapport" -> "Built genuine connection with the prospect."
        else -> "Solid performance."
    }

    private fun improvementNote(dimension: String, score: Float): String = when (dimension) {
        "Discovery" -> "Ask more open-ended questions before pitching."
        "Pitch" -> if (score < 50) "Messages were too brief or too verbose." else "Lead with the benefit, not the feature."
        "Objection Handling" -> "Acknowledge the concern first, then redirect."
        "Closing" -> "Always ask for a clear next step."
        "Tonality" -> "Write like you talk -- shorter, more natural."
        "Pace" -> if (score < 50) "Too short. Build more rapport first." else "Don't let the call drag."
        "Rapport" -> "Use their name and reference their business."
        else -> "Focus on this area next session."
    }

    // ── Timer ──────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && sessionActive) {
                delay(1000L)
                elapsedTime = (System.currentTimeMillis() - sessionStartTime) / 1000
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // ── Constants ──────────────────────────────────────────────────

    companion object {
        /** Role strings matching the existing RoleplayMessage.role field. */
        const val ROLE_USER = "user"
        const val ROLE_PROSPECT = "prospect"

        private val OBJECTION_KEYWORDS = listOf(
            "not interested", "too expensive", "already have", "think about it",
            "talk to my partner", "no thanks", "busy", "don't need",
            "got burned", "can't afford", "competitor", "cheaper"
        )

        private val OBJECTION_HANDLERS = listOf(
            "understand", "hear you", "get that", "makes sense",
            "totally", "fair", "respect that", "appreciate",
            "lot of", "similar", "other clients", "actually"
        )

        private val EMPATHY_KEYWORDS = listOf(
            "understand", "hear you", "get that", "respect",
            "appreciate", "makes sense", "fair enough", "i hear"
        )

        private val OBJECTION_COACHING = mapOf(
            "not interested" to listOf(
                "I totally get it -- you probably get a ton of these calls. Quick question though: if I could show you how to increase your cash flow in the next 48 hours, would that be worth 60 seconds?",
                "No worries at all. Out of curiosity, is it the timing that's off, or you're just not in the market right now?",
                "Heard. A lot of our best clients said the exact same thing. What changed their mind was seeing how fast it works. Can I send you a quick overview?"
            ),
            "think about it" to listOf(
                "Absolutely, take your time. What specifically do you want to think over? I might be able to answer it right now.",
                "For sure. Is it more about the timing or the terms? Sometimes I can adjust things to make it work.",
                "Of course. Would it help if I sent you a quick one-page breakdown so you have something concrete to look at?"
            ),
            "too expensive" to listOf(
                "I hear you on cost. What if I showed you how most of our clients actually make more money than the cost of capital? It's a net positive.",
                "That's a fair concern. What are you comparing it to? I want to make sure we're looking at apples to apples.",
                "Totally understand. Let me ask -- what would it mean for your business if you had an extra 50K in working capital next week?"
            ),
            "already have" to listOf(
                "That's great that you have something in place. How's it working for you? Most of the businesses I talk to are looking for better terms or more flexibility.",
                "No problem. A lot of our clients actually use us alongside their existing setup. We're not looking to replace anything -- just give you more options.",
                "Got it. When does your current arrangement come up for renewal? I'd love to at least give you a comparison."
            ),
            "partner" to listOf(
                "Totally understand. Would it help if I put together a quick summary they can review? That way when you talk, you have all the details.",
                "Of course. Is your partner involved in the day-to-day finances? I can set up a quick three-way call if that's easier.",
                "Makes sense. What's the best way to loop them in? I can send over an email with all the specifics."
            ),
            "busy" to listOf(
                "I respect your time. Can I call you back at a better time? This literally takes 60 seconds.",
                "No problem -- when's a better time? Morning or afternoon work better for you?",
                "Got it. I'll be super quick -- 30 seconds. If it's not a fit, I'll never call again. Fair?"
            ),
            "how much" to listOf(
                "Great question. It really depends on your business profile -- revenue, time in business, that kind of thing. Can I ask you two quick questions to give you an accurate number?",
                "Pricing varies based on your financials, but I can tell you most of our clients see rates starting around 1.1. The real question is: how much capital could you put to work?",
                "I'd rather give you a real number than a guess. If you can tell me your monthly revenue and how long you've been in business, I can give you an exact range in about 30 seconds."
            )
        )
    }
}
