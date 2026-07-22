package app.dylla.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
