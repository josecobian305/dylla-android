package app.dylla.services

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.DyllaApp
// PersistenceManager is in the same package
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TwilioVoiceManager {
    var isReady by mutableStateOf(false)
        private set
    var isConnected by mutableStateOf(false)
        private set
    var isRinging by mutableStateOf(false)
        private set
    var isMuted by mutableStateOf(false)
        private set
    var isSpeaker by mutableStateOf(false)
        private set

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onFailed: ((String) -> Unit)? = null
    var onRinging: (() -> Unit)? = null

    private val serverBaseURL: String
        get() = PersistenceManager.loadUserProfile()?.serverBaseURL ?: ""

    suspend fun fetchToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val config = TwilioConfig.load()
                val body = JSONObject().apply {
                    put("twilioSid", config.twilioSid)
                    put("twilioToken", config.twilioToken)
                }

                val url = URL("$serverBaseURL/api/twilio-token")
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

                val json = response?.let { JSONObject(it) }
                val token = json?.optString("token")
                if (!token.isNullOrEmpty()) {
                    isReady = true
                    true
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun connect(prospectPhone: String, twilioNumber: String, prospectName: String) {
        onFailed?.invoke("Use bridge call mode")
    }

    fun disconnect() {
        isConnected = false
        isRinging = false
        isMuted = false
        isSpeaker = false
        setSpeakerOutput(false)
        onDisconnected?.invoke()
    }

    fun toggleMute() {
        isMuted = !isMuted
    }

    fun toggleSpeaker() {
        isSpeaker = !isSpeaker
        setSpeakerOutput(isSpeaker)
    }

    private fun setSpeakerOutput(enabled: Boolean) {
        try {
            val audioManager = DyllaApp.instance.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = enabled
        } catch (_: Exception) {
        }
    }
}
