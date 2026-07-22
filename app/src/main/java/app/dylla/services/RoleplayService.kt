package app.dylla.services

import app.dylla.models.RoleplayScenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(
    val role: String,
    val content: String
)

data class SessionScoreResult(
    val objectionsClosed: Int,
    val grade: String,
    val xp: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val bestLine: String,
    val tip: String
)

data class CoachingAnalysis(
    val currentObjection: String,
    val suggestedResponses: List<String>,
    val tip: String
)

data class ResponseEvaluation(
    val rating: String,
    val missedOpportunity: String?,
    val betterResponse: String?
)

data class RoleplayMessage(
    val role: String,
    val content: String
)

object RoleplayService {

    private const val API_URL = "http://34.21.11.104:3456/v1/chat/completions"
    private const val MODEL = "claude-opus-4-6"
    private const val BEARER_TOKEN = "sk-local"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun buildSystemPrompt(scenario: RoleplayScenario): String {
        val objectionsList = scenario.objections.joinToString("\n") { "- $it" }
        return """You are ${scenario.merchantName}, a real merchant who owns a ${scenario.industry} business.
Your business does $${scenario.monthlyRevenue}/month in revenue and has been operating for ${scenario.timeInBusiness}.
Your main pain point: ${scenario.painPoint}

You are skeptical about funding offers but can be persuaded with the right approach.
Stay in character at all times. Be a difficult but realistic merchant.

Your objections (use these naturally throughout the conversation):
$objectionsList

Rules:
- Never break character
- Start skeptical, warm up only if the closer earns it
- Give short, realistic merchant responses (1-3 sentences)
- Push back on vague promises
- If the closer handles an objection well, acknowledge it subtly and move to the next one
- Do NOT reveal you are AI or playing a role"""
    }

    fun sendMessage(history: List<ChatMessage>, callback: (Result<String>) -> Unit) {
        scope.launch {
            try {
                val response = postCompletion(history, 0.85, 200)
                val cleaned = stripCostFooter(response)
                withContext(Dispatchers.Main) { callback(Result.success(cleaned)) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
            }
        }
    }

    fun analyzeObjection(
        history: List<ChatMessage>,
        scenario: RoleplayScenario,
        callback: (Result<CoachingAnalysis>) -> Unit
    ) {
        scope.launch {
            try {
                val systemPrompt = """Analyze this roleplay conversation. The scenario is a ${scenario.industry} merchant named ${scenario.merchantName}.
Identify the current objection being raised and provide coaching.
Respond in JSON only:
{"currentObjection": "...", "suggestedResponses": ["response1", "response2", "response3"], "tip": "..."}"""

                val messages = listOf(ChatMessage("system", systemPrompt)) + history
                val response = postCompletion(messages, 0.3, 500)
                val cleaned = stripCostFooter(response)
                val json = JSONObject(extractJson(cleaned))

                val suggestions = mutableListOf<String>()
                val arr = json.getJSONArray("suggestedResponses")
                for (i in 0 until arr.length()) suggestions.add(arr.getString(i))

                val analysis = CoachingAnalysis(
                    currentObjection = json.getString("currentObjection"),
                    suggestedResponses = suggestions,
                    tip = json.getString("tip")
                )
                withContext(Dispatchers.Main) { callback(Result.success(analysis)) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
            }
        }
    }

    fun evaluateResponse(
        history: List<ChatMessage>,
        scenario: RoleplayScenario,
        callback: (Result<ResponseEvaluation>) -> Unit
    ) {
        scope.launch {
            try {
                val systemPrompt = """Evaluate the closer's last response in this ${scenario.industry} merchant roleplay.
Did they miss any opportunities? Rate their response.
Respond in JSON only:
{"rating": "strong|adequate|weak", "missedOpportunity": "..." or null, "betterResponse": "..." or null}"""

                val messages = listOf(ChatMessage("system", systemPrompt)) + history
                val response = postCompletion(messages, 0.3, 500)
                val cleaned = stripCostFooter(response)
                val json = JSONObject(extractJson(cleaned))

                val eval = ResponseEvaluation(
                    rating = json.getString("rating"),
                    missedOpportunity = if (json.isNull("missedOpportunity")) null else json.getString("missedOpportunity"),
                    betterResponse = if (json.isNull("betterResponse")) null else json.getString("betterResponse")
                )
                withContext(Dispatchers.Main) { callback(Result.success(eval)) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
            }
        }
    }

    fun scoreSession(
        messages: List<RoleplayMessage>,
        scenario: RoleplayScenario,
        callback: (Result<SessionScoreResult>) -> Unit
    ) {
        scope.launch {
            try {
                val transcript = messages.joinToString("\n") { "${it.role}: ${it.content}" }
                val systemPrompt = """Grade this sales roleplay session for a ${scenario.industry} merchant named ${scenario.merchantName}.
Objections they needed to handle: ${scenario.objections.joinToString(", ")}

Transcript:
$transcript

Score the session and respond in JSON only:
{"objectionsClosed": 0, "grade": "A+ to F", "xp": 50-500, "strengths": ["..."], "improvements": ["..."], "bestLine": "...", "tip": "..."}"""

                val chatMessages = listOf(ChatMessage("system", systemPrompt))
                val response = postCompletion(chatMessages, 0.3, 800)
                val cleaned = stripCostFooter(response)
                val json = JSONObject(extractJson(cleaned))

                val strengths = mutableListOf<String>()
                val sArr = json.getJSONArray("strengths")
                for (i in 0 until sArr.length()) strengths.add(sArr.getString(i))

                val improvements = mutableListOf<String>()
                val iArr = json.getJSONArray("improvements")
                for (i in 0 until iArr.length()) improvements.add(iArr.getString(i))

                val result = SessionScoreResult(
                    objectionsClosed = json.getInt("objectionsClosed"),
                    grade = json.getString("grade"),
                    xp = json.getInt("xp"),
                    strengths = strengths,
                    improvements = improvements,
                    bestLine = json.getString("bestLine"),
                    tip = json.getString("tip")
                )
                withContext(Dispatchers.Main) { callback(Result.success(result)) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(Result.failure(e)) }
            }
        }
    }

    private fun postCompletion(messages: List<ChatMessage>, temperature: Double, maxTokens: Int): String {
        val messagesArray = JSONArray()
        for (msg in messages) {
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", messagesArray)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
        }

        val url = URL(API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $BEARER_TOKEN")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (conn.responseCode !in 200..299) {
            val error = conn.errorStream?.use { it.readBytes().decodeToString() } ?: "Unknown error"
            conn.disconnect()
            throw Exception("API error ${conn.responseCode}: $error")
        }

        val responseStr = conn.inputStream.use { it.readBytes().decodeToString() }
        conn.disconnect()

        val json = JSONObject(responseStr)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun stripCostFooter(text: String): String {
        val lines = text.lines()
        val filtered = mutableListOf<String>()
        var hitFooter = false
        for (line in lines) {
            if (!hitFooter && (line.trimStart().startsWith("---") || line.lowercase().contains("estimated cost"))) {
                hitFooter = true
                continue
            }
            if (!hitFooter) filtered.add(line)
        }
        return filtered.joinToString("\n").trimEnd()
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }
}
