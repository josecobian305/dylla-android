package app.dylla.services

import android.content.Context
import app.dylla.DyllaApp
import app.dylla.models.TwilioConfig
import app.dylla.models.UserProfile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CloudSettingsService {

    private const val BASE_URL =
        "https://firestore.googleapis.com/v1/projects/chc-capital-ai/databases/(default)/documents"

    private val gson = Gson()

    private val prefs by lazy {
        DyllaApp.instance.getSharedPreferences("dylla_cloud_settings", Context.MODE_PRIVATE)
    }

    suspend fun save(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = PersistenceManager.loadUserProfile()
            val twilioJson = gson.toJson(profile.twilio)
            val profileJson = gson.toJson(profile)

            val fields = buildMap {
                put("twilioConfig", mapOf("stringValue" to twilioJson))
                put("userProfile", mapOf("stringValue" to profileJson))
                put("trainingDialerEnabled", mapOf("booleanValue" to prefs.getBoolean("trainingDialerEnabled", false)))
                put("trainingVoiceDropEnabled", mapOf("booleanValue" to prefs.getBoolean("trainingVoiceDropEnabled", false)))
                put("minTrainingSessions", mapOf("integerValue" to prefs.getInt("minTrainingSessions", 0).toString()))
                put("onboardingComplete", mapOf("booleanValue" to prefs.getBoolean("onboardingComplete", false)))
            }

            val body = gson.toJson(mapOf("fields" to fields))
            val url = URL("$BASE_URL/dylla_users/$uid/settings/app")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) Result.success(Unit)
            else Result.failure(Exception("Firestore PATCH failed: $code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun load(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/dylla_users/$uid/settings/app")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Content-Type", "application/json")

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext false
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val doc = gson.fromJson(responseBody, Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val fields = doc["fields"] as? Map<String, Map<String, Any>> ?: return@withContext false

            val twilioJson = fields["twilioConfig"]?.get("stringValue") as? String
            val profileJson = fields["userProfile"]?.get("stringValue") as? String

            if (profileJson != null) {
                val profile = gson.fromJson(profileJson, UserProfile::class.java)
                if (twilioJson != null) {
                    profile.twilio = gson.fromJson(twilioJson, TwilioConfig::class.java)
                }
                PersistenceManager.saveUserProfile(profile)
            }

            val editor = prefs.edit()

            (fields["trainingDialerEnabled"]?.get("booleanValue") as? Boolean)?.let {
                editor.putBoolean("trainingDialerEnabled", it)
            }
            (fields["trainingVoiceDropEnabled"]?.get("booleanValue") as? Boolean)?.let {
                editor.putBoolean("trainingVoiceDropEnabled", it)
            }
            val minSessions = fields["minTrainingSessions"]?.get("integerValue")
            if (minSessions != null) {
                editor.putInt("minTrainingSessions", minSessions.toString().toIntOrNull() ?: 0)
            }
            (fields["onboardingComplete"]?.get("booleanValue") as? Boolean)?.let {
                editor.putBoolean("onboardingComplete", it)
            }

            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}
