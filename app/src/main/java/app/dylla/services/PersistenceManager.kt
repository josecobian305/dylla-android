package app.dylla.services

import android.content.Context
import android.content.SharedPreferences
import app.dylla.DyllaApp
import app.dylla.models.CallList
import app.dylla.models.Company
import app.dylla.models.FundingStage
import app.dylla.models.TrainingStats
import app.dylla.models.UserProfile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Singleton persistence layer using SharedPreferences + Gson.
 * Serializes all local data (lists, stages, companies, stats, profile)
 * so the app works fully offline.
 */
object PersistenceManager {

    private const val PREFS_NAME = "dylla_persistence"
    private const val KEY_LISTS = "dylla_lists"
    private const val KEY_STAGES = "dylla_stages_v3"
    private const val KEY_COMPANIES = "dylla_companies"
    private const val KEY_TRAINING_STATS = "dylla_training_stats"
    private const val KEY_USER_PROFILE = "dylla_user_profile"
    private const val KEY_ACTIVE_LIST_ID = "dylla_active_list_id"

    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create()
    }

    private val prefs: SharedPreferences by lazy {
        DyllaApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Lists ──────────────────────────────────────────────────────

    fun saveLists(lists: List<CallList>) {
        prefs.edit().putString(KEY_LISTS, gson.toJson(lists)).apply()
    }

    fun loadLists(): List<CallList> {
        val json = prefs.getString(KEY_LISTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CallList>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Stages ─────────────────────────────────────────────────────

    fun saveStages(stages: List<FundingStage>) {
        prefs.edit().putString(KEY_STAGES, gson.toJson(stages)).apply()
    }

    fun loadStages(): List<FundingStage> {
        val json = prefs.getString(KEY_STAGES, null) ?: return FundingStage.defaults
        return try {
            val type = object : TypeToken<List<FundingStage>>() {}.type
            val cached: List<FundingStage> = gson.fromJson(json, type) ?: return FundingStage.defaults
            mergeStages(cached)
        } catch (e: Exception) {
            FundingStage.defaults
        }
    }

    /**
     * Merge persisted stages with defaults so newly added default stages
     * are never lost after an app update.
     */
    private fun mergeStages(cached: List<FundingStage>): List<FundingStage> {
        if (cached.isEmpty()) return FundingStage.defaults
        val existingNames = cached.map { it.name }.toSet()
        val missing = FundingStage.defaults.filter { it.name !in existingNames }
        return if (missing.isEmpty()) cached else cached + missing
    }

    // ── Companies ──────────────────────────────────────────────────

    fun saveCompanies(companies: List<Company>) {
        prefs.edit().putString(KEY_COMPANIES, gson.toJson(companies)).apply()
    }

    fun loadCompanies(): List<Company> {
        val json = prefs.getString(KEY_COMPANIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Company>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Training Stats ─────────────────────────────────────────────

    fun saveTrainingStats(stats: TrainingStats) {
        prefs.edit().putString(KEY_TRAINING_STATS, gson.toJson(stats)).apply()
    }

    fun loadTrainingStats(): TrainingStats {
        val json = prefs.getString(KEY_TRAINING_STATS, null) ?: return TrainingStats()
        return try {
            gson.fromJson(json, TrainingStats::class.java) ?: TrainingStats()
        } catch (e: Exception) {
            TrainingStats()
        }
    }

    // ── User Profile ───────────────────────────────────────────────

    fun saveUserProfile(profile: UserProfile) {
        prefs.edit().putString(KEY_USER_PROFILE, gson.toJson(profile)).apply()
    }

    fun loadUserProfile(): UserProfile {
        val json = prefs.getString(KEY_USER_PROFILE, null) ?: return UserProfile()
        return try {
            gson.fromJson(json, UserProfile::class.java) ?: UserProfile()
        } catch (e: Exception) {
            UserProfile()
        }
    }

    // ── Active List ID ─────────────────────────────────────────────

    fun saveActiveListId(id: String?) {
        prefs.edit().apply {
            if (id != null) putString(KEY_ACTIVE_LIST_ID, id)
            else remove(KEY_ACTIVE_LIST_ID)
        }.apply()
    }

    fun loadActiveListId(): String? {
        return prefs.getString(KEY_ACTIVE_LIST_ID, null)
    }

    // ── Clear All ──────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
