package app.dylla.services

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.DyllaApp
import app.dylla.models.Contact
// PersistenceManager is in the same package
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class TwilioConfig(
    val twilioSid: String = "",
    val twilioToken: String = "",
    val twilioNumber: String = "",
    val twilioNumbers: List<String> = emptyList(),
    val userPhone: String = "",
    val linesPerBatch: Int = 1
) {
    val isConfigured get() = twilioSid.isNotEmpty() && twilioToken.isNotEmpty() && twilioNumber.isNotEmpty()
    val allNumbers get() = if (twilioNumbers.isEmpty()) listOfNotNull(twilioNumber.ifEmpty { null }) else twilioNumbers

    companion object {
        private const val PREFS_KEY = "dylla_twilio_config_v2"

        fun load(): TwilioConfig {
            val prefs = DyllaApp.instance.getSharedPreferences("dylla_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString(PREFS_KEY, null) ?: return TwilioConfig()
            return try {
                val obj = JSONObject(json)
                val numbersArray = obj.optJSONArray("twilioNumbers")
                val numbers = mutableListOf<String>()
                if (numbersArray != null) {
                    for (i in 0 until numbersArray.length()) {
                        numbers.add(numbersArray.getString(i))
                    }
                }
                TwilioConfig(
                    twilioSid = obj.optString("twilioSid", ""),
                    twilioToken = obj.optString("twilioToken", ""),
                    twilioNumber = obj.optString("twilioNumber", ""),
                    twilioNumbers = numbers,
                    userPhone = obj.optString("userPhone", ""),
                    linesPerBatch = obj.optInt("linesPerBatch", 1)
                )
            } catch (_: Exception) {
                TwilioConfig()
            }
        }

        fun save(config: TwilioConfig) {
            val obj = JSONObject().apply {
                put("twilioSid", config.twilioSid)
                put("twilioToken", config.twilioToken)
                put("twilioNumber", config.twilioNumber)
                put("twilioNumbers", JSONArray(config.twilioNumbers))
                put("userPhone", config.userPhone)
                put("linesPerBatch", config.linesPerBatch)
            }
            DyllaApp.instance.getSharedPreferences("dylla_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_KEY, obj.toString())
                .apply()
        }
    }
}

enum class PowerCallStatus { IDLE, RINGING, CONNECTED, SKIPPING }

class PowerDialerService {
    var status by mutableStateOf(PowerCallStatus.IDLE)
        private set
    var callSid by mutableStateOf<String?>(null)
        private set
    var elapsed by mutableIntStateOf(0)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    var onSkipped: (() -> Unit)? = null
    var onAnswered: (() -> Unit)? = null

    val activeSids = mutableListOf<String>()

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val serverBaseURL: String
        get() = PersistenceManager.loadUserProfile()?.serverBaseURL ?: ""

    suspend fun placeCall(contact: Contact, uid: String): Boolean {
        reset()
        val config = TwilioConfig.load()
        if (!config.isConfigured) {
            lastError = "Twilio not configured"
            return false
        }

        status = PowerCallStatus.RINGING
        startTimer()

        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("to", contact.phone)
                    put("from", config.twilioNumber)
                    put("twilioSid", config.twilioSid)
                    put("twilioToken", config.twilioToken)
                    put("userPhone", config.userPhone)
                    put("uid", uid)
                }

                val result = postJSON("$serverBaseURL/api/power-dial", body)
                val sid = result?.optString("callSid")
                if (sid.isNullOrEmpty()) {
                    lastError = result?.optString("error") ?: "No call SID returned"
                    status = PowerCallStatus.IDLE
                    stopTimer()
                    return@withContext false
                }

                callSid = sid
                activeSids.add(sid)
                startPolling(sid)
                true
            } catch (e: Exception) {
                lastError = e.message
                status = PowerCallStatus.IDLE
                stopTimer()
                false
            }
        }
    }

    suspend fun placeMultiCall(contacts: List<Contact>, uid: String): Boolean {
        reset()
        val config = TwilioConfig.load()
        if (!config.isConfigured) {
            lastError = "Twilio not configured"
            return false
        }

        status = PowerCallStatus.RINGING
        startTimer()

        return withContext(Dispatchers.IO) {
            try {
                val contactsArray = JSONArray()
                contacts.forEach { c ->
                    contactsArray.put(JSONObject().apply {
                        put("phone", c.phone)
                        put("name", c.name)
                        put("id", c.id)
                    })
                }

                val body = JSONObject().apply {
                    put("batchId", UUID.randomUUID().toString())
                    put("contacts", contactsArray)
                    put("from", config.twilioNumber)
                    put("twilioSid", config.twilioSid)
                    put("twilioToken", config.twilioToken)
                    put("userPhone", config.userPhone)
                    put("uid", uid)
                    put("linesPerBatch", config.linesPerBatch)
                    put("twilioNumbers", JSONArray(config.allNumbers))
                }

                val result = postJSON("$serverBaseURL/api/power-dial-multi", body)
                val sidsArray = result?.optJSONArray("callSids")
                if (sidsArray == null || sidsArray.length() == 0) {
                    lastError = result?.optString("error") ?: "No call SIDs returned"
                    status = PowerCallStatus.IDLE
                    stopTimer()
                    return@withContext false
                }

                for (i in 0 until sidsArray.length()) {
                    val sid = sidsArray.getString(i)
                    activeSids.add(sid)
                }
                callSid = activeSids.firstOrNull()
                callSid?.let { startPolling(it) }
                true
            } catch (e: Exception) {
                lastError = e.message
                status = PowerCallStatus.IDLE
                stopTimer()
                false
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        stopTimer()
        status = PowerCallStatus.IDLE
    }

    fun reset() {
        stop()
        callSid = null
        elapsed = 0
        lastError = null
        activeSids.clear()
    }

    private fun startPolling(sid: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            var ringStartTime = System.currentTimeMillis()
            while (isActive) {
                delay(1000)
                try {
                    val url = URL("$serverBaseURL/api/call-status/$sid")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    val json = JSONObject(response)
                    val callStatus = json.optString("status", "")

                    when (callStatus) {
                        "in-progress", "answered" -> {
                            if (status != PowerCallStatus.CONNECTED) {
                                status = PowerCallStatus.CONNECTED
                                withContext(Dispatchers.Main) { onAnswered?.invoke() }
                            }
                        }
                        "completed", "busy", "no-answer", "canceled", "failed" -> {
                            status = PowerCallStatus.IDLE
                            stopTimer()
                            cancel()
                        }
                    }

                    if (status == PowerCallStatus.RINGING &&
                        System.currentTimeMillis() - ringStartTime > 20_000
                    ) {
                        status = PowerCallStatus.SKIPPING
                        withContext(Dispatchers.Main) { onSkipped?.invoke() }
                        delay(500)
                        status = PowerCallStatus.IDLE
                        stopTimer()
                        cancel()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        elapsed = 0
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (status == PowerCallStatus.RINGING || status == PowerCallStatus.CONNECTED) {
                    elapsed++
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun postJSON(urlString: String, body: JSONObject): JSONObject? {
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

        return response?.let { JSONObject(it) }
    }
}
