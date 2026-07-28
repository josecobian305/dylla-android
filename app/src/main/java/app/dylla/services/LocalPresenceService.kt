package app.dylla.services

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.DyllaApp
// PersistenceManager is in the same package
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class SpoofNumber(
    val id: String = UUID.randomUUID().toString(),
    val number: String,
    val areaCode: String,
    val callCount: Int = 0,
    val lastUsed: Long? = null,
    val purchasedAt: Long = System.currentTimeMillis()
) {
    val formatted: String
        get() {
            val digits = number.replace(Regex("[^0-9]"), "")
            val raw = if (digits.startsWith("1") && digits.length == 11) digits.substring(1) else digits
            return if (raw.length == 10) "(${raw.substring(0, 3)}) ${raw.substring(3, 6)}-${raw.substring(6)}"
            else number
        }
}

class LocalPresenceService {
    var numbers by mutableStateOf<List<SpoofNumber>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isBuying by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // CNAM state
    var cnam by mutableStateOf("")
    var savedCnam by mutableStateOf("")
    var cnamSaving by mutableStateOf(false)

    // SHAKEN/STIR Trust Hub state
    var trustStatus by mutableStateOf<String?>(null)
    var trustProfileSid by mutableStateOf<String?>(null)
    var trustFailureReason by mutableStateOf<String?>(null)
    var trustNumbersAssigned by mutableStateOf<Int?>(null)
    var trustLoading by mutableStateOf(false)

    private val serverBaseURL: String
        get() = PersistenceManager.loadUserProfile()?.serverBaseURL ?: ""

    companion object {
        fun areaCode(phone: String): String {
            val digits = phone.replace(Regex("[^0-9]"), "")
            return when {
                digits.startsWith("1") && digits.length >= 4 -> digits.substring(1, 4)
                digits.length >= 3 -> digits.substring(0, 3)
                else -> ""
            }
        }
    }

    fun matchedNumber(phone: String): SpoofNumber? {
        val target = areaCode(phone)
        return numbers.find { it.areaCode == target }
    }

    suspend fun loadNumbers(uid: String) {
        isLoading = true
        error = null
        try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
            }
            val result = postJSON("$serverBaseURL/api/spoof/list-numbers", body)
            val arr = result?.optJSONArray("numbers")
            if (arr != null) {
                val list = mutableListOf<SpoofNumber>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SpoofNumber(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            number = obj.optString("number", ""),
                            areaCode = obj.optString("areaCode", ""),
                            callCount = obj.optInt("callCount", 0),
                            lastUsed = if (obj.has("lastUsed")) obj.optLong("lastUsed") else null,
                            purchasedAt = obj.optLong("purchasedAt", System.currentTimeMillis())
                        )
                    )
                }
                numbers = list
            } else {
                error = result?.optString("error") ?: "Failed to load numbers"
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    suspend fun getOrBuyNumber(uid: String, prospectPhone: String): SpoofNumber? {
        isBuying = true
        error = null
        try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("prospectPhone", prospectPhone)
                put("areaCode", areaCode(prospectPhone))
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
            }
            val result = postJSON("$serverBaseURL/api/spoof/get-number", body)
            val numObj = result?.optJSONObject("number") ?: return null
            val spoof = SpoofNumber(
                id = numObj.optString("id", UUID.randomUUID().toString()),
                number = numObj.optString("number", ""),
                areaCode = numObj.optString("areaCode", ""),
                callCount = numObj.optInt("callCount", 0),
                lastUsed = if (numObj.has("lastUsed")) numObj.optLong("lastUsed") else null,
                purchasedAt = numObj.optLong("purchasedAt", System.currentTimeMillis())
            )
            if (numbers.none { it.id == spoof.id }) {
                numbers = numbers + spoof
            }
            return spoof
        } catch (e: Exception) {
            error = e.message
            return null
        } finally {
            isBuying = false
        }
    }

    suspend fun bridgeCall(
        uid: String,
        spoofNumber: String,
        prospectPhone: String,
        prospectName: String
    ): String? {
        error = null
        return try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("spoofNumber", spoofNumber)
                put("prospectPhone", prospectPhone)
                put("prospectName", prospectName)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("userPhone", config.userPhone)
            }
            val result = postJSON("$serverBaseURL/api/spoof/bridge-call", body)
            result?.optString("callSid")?.ifEmpty { null }
        } catch (e: Exception) {
            error = e.message
            null
        }
    }

    suspend fun releaseNumber(uid: String, numberId: String) {
        error = null
        try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("numberId", numberId)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
            }
            postJSON("$serverBaseURL/api/spoof/release-number", body)
            numbers = numbers.filter { it.id != numberId }
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun loadSavedSettings(context: Context) {
        val prefs = context.getSharedPreferences("dylla_prefs", Context.MODE_PRIVATE)
        savedCnam = prefs.getString("dylla_cnam", "") ?: ""
        cnam = savedCnam
        trustStatus = prefs.getString("dylla_trust_status", null)
        trustProfileSid = prefs.getString("dylla_trust_profileSid", null)
        trustFailureReason = prefs.getString("dylla_trust_failureReason", null)
        val stored = prefs.getInt("dylla_trust_numbersAssigned", -1)
        trustNumbersAssigned = if (stored >= 0) stored else null
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences("dylla_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("dylla_cnam", savedCnam)
            trustStatus?.let { putString("dylla_trust_status", it) } ?: remove("dylla_trust_status")
            trustProfileSid?.let { putString("dylla_trust_profileSid", it) } ?: remove("dylla_trust_profileSid")
            trustFailureReason?.let { putString("dylla_trust_failureReason", it) } ?: remove("dylla_trust_failureReason")
            trustNumbersAssigned?.let { putInt("dylla_trust_numbersAssigned", it) } ?: remove("dylla_trust_numbersAssigned")
            apply()
        }
    }

    suspend fun saveCnam(uid: String, cnam: String): Boolean {
        cnamSaving = true
        error = null
        return try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("cnam", cnam)
            }
            val result = postJSON("$serverBaseURL/api/spoof/update-cnam", body)
            if (result?.optBoolean("ok", false) == true) {
                savedCnam = cnam
                val ctx = DyllaApp.instance
                saveSettings(ctx)
                true
            } else {
                error = result?.optString("error") ?: "Failed to update CNAM"
                false
            }
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            cnamSaving = false
        }
    }

    suspend fun registerTrust(
        uid: String,
        businessName: String,
        businessType: String,
        ein: String,
        street: String,
        city: String,
        state: String,
        zip: String,
        phone: String,
        email: String,
        website: String
    ): Boolean {
        trustLoading = true
        error = null
        return try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("businessName", businessName)
                put("businessType", businessType)
                put("ein", ein)
                put("street", street)
                put("city", city)
                put("state", state)
                put("zip", zip)
                put("phone", phone)
                put("email", email)
                put("website", website)
            }
            val result = postJSON("$serverBaseURL/api/trust/register", body)
            val status = result?.optString("status", "")
            if (!status.isNullOrEmpty() && status != "null") {
                trustStatus = status
                trustProfileSid = result?.optString("profileSid", null)
                trustFailureReason = null
                val ctx = DyllaApp.instance
                saveSettings(ctx)
                true
            } else {
                error = result?.optString("error") ?: "Registration failed"
                false
            }
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            trustLoading = false
        }
    }

    suspend fun checkTrustStatus(uid: String): Boolean {
        trustLoading = true
        error = null
        return try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("profileSid", trustProfileSid ?: "")
            }
            val result = postJSON("$serverBaseURL/api/trust/status", body)
            val status = result?.optString("status", "")
            if (!status.isNullOrEmpty() && status != "null") {
                trustStatus = status
                trustProfileSid = result?.optString("profileSid", trustProfileSid)
                trustFailureReason = result?.optString("failureReason", null)
                    ?.takeIf { it.isNotEmpty() && it != "null" }
                val ctx = DyllaApp.instance
                saveSettings(ctx)
                true
            } else {
                error = result?.optString("error") ?: "Failed to check status"
                false
            }
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            trustLoading = false
        }
    }

    suspend fun assignNumbers(uid: String): Int? {
        trustLoading = true
        error = null
        return try {
            val config = TwilioConfig.load()
            val body = JSONObject().apply {
                put("uid", uid)
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("profileSid", trustProfileSid ?: "")
            }
            val result = postJSON("$serverBaseURL/api/trust/assign-numbers", body)
            val assigned = result?.optInt("assigned", -1) ?: -1
            if (assigned >= 0) {
                trustNumbersAssigned = assigned
                val ctx = DyllaApp.instance
                saveSettings(ctx)
                assigned
            } else {
                error = result?.optString("error") ?: "Failed to assign numbers"
                null
            }
        } catch (e: Exception) {
            error = e.message
            null
        } finally {
            trustLoading = false
        }
    }

    private suspend fun postJSON(urlString: String, body: JSONObject): JSONObject? =
        withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.bufferedReader()?.readText()
            conn.disconnect()

            response?.let { JSONObject(it) }
        }
}
