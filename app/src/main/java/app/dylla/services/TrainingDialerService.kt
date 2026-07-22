package app.dylla.services

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.models.Contact
import app.dylla.models.VoiceDrop
import app.dylla.persistence.PersistenceManager
import app.dylla.services.TwilioConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TrainingDialerService {
    var status by mutableStateOf(PowerCallStatus.IDLE)
        private set
    var callSid by mutableStateOf<String?>(null)
        private set
    var elapsed by mutableIntStateOf(0)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    var onCallEnded: ((String) -> Unit)? = null

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
        isRecording = true
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
                    put("record", true)
                    put("trainingMode", true)
                }

                val result = postJSON("$serverBaseURL/api/power-dial", body)
                val sid = result?.optString("callSid")
                if (sid.isNullOrEmpty()) {
                    lastError = result?.optString("error") ?: "No call SID returned"
                    status = PowerCallStatus.IDLE
                    isRecording = false
                    stopTimer()
                    return@withContext false
                }

                callSid = sid
                startPolling(sid)
                true
            } catch (e: Exception) {
                lastError = e.message
                status = PowerCallStatus.IDLE
                isRecording = false
                stopTimer()
                false
            }
        }
    }

    fun sendVoiceDrop(toPhone: String, contactName: String, drop: VoiceDrop, dropManager: VoiceDropManager) {
        scope.launch {
            try {
                val config = TwilioConfig.load()
                val audioFile = File(drop.filePath)
                if (!audioFile.exists()) return@launch

                val audioBytes = audioFile.readBytes()
                val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

                val body = JSONObject().apply {
                    put("to", toPhone)
                    put("from", config.twilioNumber)
                    put("twilioSid", config.twilioSid)
                    put("twilioToken", config.twilioToken)
                    put("contactName", contactName)
                    put("dropName", drop.name)
                    put("audioData", audioBase64)
                }

                postJSON("$serverBaseURL/api/dialer-voicedrop", body)
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        stopTimer()
        val sid = callSid
        status = PowerCallStatus.IDLE
        isRecording = false
        sid?.let { onCallEnded?.invoke(it) }
    }

    fun reset() {
        pollingJob?.cancel()
        pollingJob = null
        stopTimer()
        callSid = null
        elapsed = 0
        lastError = null
        status = PowerCallStatus.IDLE
        isRecording = false
    }

    private fun startPolling(sid: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            val ringStartTime = System.currentTimeMillis()
            while (isActive) {
                delay(1500)
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
                            }
                        }
                        "completed", "busy", "no-answer", "canceled", "failed" -> {
                            status = PowerCallStatus.IDLE
                            isRecording = false
                            stopTimer()
                            withContext(Dispatchers.Main) { onCallEnded?.invoke(sid) }
                            cancel()
                        }
                    }

                    if (status == PowerCallStatus.RINGING &&
                        System.currentTimeMillis() - ringStartTime > 30_000
                    ) {
                        status = PowerCallStatus.SKIPPING
                        delay(500)
                        status = PowerCallStatus.IDLE
                        isRecording = false
                        stopTimer()
                        withContext(Dispatchers.Main) { onCallEnded?.invoke(sid) }
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
