package app.dylla.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.DyllaApp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.UUID

enum class TeamRole(val label: String) {
    OWNER("Owner"),
    MANAGER("Manager"),
    AGENT("Agent"),
    TRAINEE("Trainee")
}

data class TeamMember(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var email: String = "",
    var role: TeamRole = TeamRole.AGENT,
    var joinedAt: Date = Date(),
    var stats: TrainingStats = TrainingStats(),
    var isActive: Boolean = true
)

data class TeamSession(
    val id: String = UUID.randomUUID().toString(),
    val memberId: String = "",
    val memberName: String = "",
    val date: Date = Date(),
    val scenarioTitle: String = "",
    val industry: String = "",
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val grade: String = "",
    val xpEarned: Int = 0,
    val objectionsClosed: Int = 0,
    val objectionCount: Int = 0,
    val tonalityScore: Float = 0f,
    val avgReactionTime: Float = 0f,
    val fillerCount: Int = 0,
    val uptalkCount: Int = 0,
    val inflectionRating: Float = 0f,
    val callDuration: Long = 0L
)

class TeamManager private constructor() {

    companion object {
        val shared = TeamManager()

        private const val PREFS_NAME = "dylla_team"
        private const val KEY_MEMBERS = "dylla_team_members"
        private const val KEY_SESSIONS = "dylla_team_sessions"
        private const val KEY_OWNER = "dylla_team_owner"
    }

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    private val prefs by lazy {
        DyllaApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var members by mutableStateOf(loadMembers())
        private set

    var sessions by mutableStateOf(loadSessions())
        private set

    var isTeamOwner by mutableStateOf(prefs.getBoolean(KEY_OWNER, true))
        private set

    val currentMemberId: String
        get() {
            val existing = members.firstOrNull()
            if (existing != null) return existing.id
            val member = TeamMember(
                name = "Me",
                role = TeamRole.OWNER,
                isActive = true
            )
            members = listOf(member) + members
            saveMembers()
            return member.id
        }

    val leaderboard: List<TeamMember>
        get() = members.filter { it.isActive }.sortedByDescending { it.stats.totalXP }

    fun logSession(
        memberName: String,
        score: SessionScoreResult,
        scenario: RoleplayScenario,
        voiceSummary: Map<String, Any>? = null,
        duration: Long = 0L
    ) {
        val member = members.firstOrNull { it.name == memberName } ?: return
        val xp = score.xp * scenario.difficulty.xpMultiplier

        val session = TeamSession(
            memberId = member.id,
            memberName = memberName,
            date = Date(),
            scenarioTitle = scenario.title,
            industry = scenario.industry,
            difficulty = scenario.difficulty,
            grade = score.grade,
            xpEarned = xp,
            objectionsClosed = score.objectionsClosed,
            objectionCount = scenario.objections.size,
            tonalityScore = (voiceSummary?.get("tonalityScore") as? Number)?.toFloat() ?: 0f,
            avgReactionTime = (voiceSummary?.get("avgReactionTime") as? Number)?.toFloat() ?: 0f,
            fillerCount = (voiceSummary?.get("fillerCount") as? Number)?.toInt() ?: 0,
            uptalkCount = (voiceSummary?.get("uptalkCount") as? Number)?.toInt() ?: 0,
            inflectionRating = (voiceSummary?.get("inflectionRating") as? Number)?.toFloat() ?: 0f,
            callDuration = duration
        )

        sessions = listOf(session) + sessions
        saveSessions()

        member.stats.totalSessions++
        member.stats.totalXP += xp
        member.stats.objectionsHandled += score.objectionsClosed
        members = members.toList()
        saveMembers()
    }

    fun sessionsFor(member: TeamMember): List<TeamSession> {
        return sessions
            .filter { it.memberId == member.id }
            .sortedByDescending { it.date }
    }

    fun addMember(name: String, email: String, role: TeamRole) {
        val member = TeamMember(
            name = name,
            email = email,
            role = role,
            isActive = true
        )
        members = members + member
        saveMembers()
    }

    fun removeMember(id: String) {
        members = members.filter { it.id != id }
        saveMembers()
    }

    fun setTeamOwner(isOwner: Boolean) {
        isTeamOwner = isOwner
        prefs.edit().putBoolean(KEY_OWNER, isOwner).apply()
    }

    private fun saveMembers() {
        prefs.edit().putString(KEY_MEMBERS, gson.toJson(members)).apply()
    }

    private fun loadMembers(): List<TeamMember> {
        val json = prefs.getString(KEY_MEMBERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TeamMember>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSessions() {
        prefs.edit().putString(KEY_SESSIONS, gson.toJson(sessions)).apply()
    }

    private fun loadSessions(): List<TeamSession> {
        val json = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TeamSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
