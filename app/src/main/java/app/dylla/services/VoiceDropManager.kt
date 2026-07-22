package app.dylla.services

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dylla.DyllaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import android.util.Base64

data class VoiceDrop(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filename: String,
    val stageID: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("filename", filename)
        put("stageID", stageID ?: JSONObject.NULL)
        put("isDefault", isDefault)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject) = VoiceDrop(
            id = json.getString("id"),
            name = json.getString("name"),
            filename = json.getString("filename"),
            stageID = if (json.isNull("stageID")) null else json.getString("stageID"),
            isDefault = json.getBoolean("isDefault"),
            createdAt = json.getLong("createdAt")
        )
    }
}

class VoiceDropManager {

    var isRecording by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var playingDropID by mutableStateOf<String?>(null)
        private set
    var recordings by mutableStateOf<List<VoiceDrop>>(emptyList())
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingName: String? = null
    private var currentRecordingStageID: String? = null
    private var currentRecordingFilename: String? = null

    private val context get() = DyllaApp.instance
    private val prefs get() = context.getSharedPreferences("dylla_voice_drops", 0)
    private val serverBaseURL: String
        get() = prefs.getString("server_base_url", "http://34.21.11.104:8400") ?: "http://34.21.11.104:8400"

    private val voiceDropsDir: File
        get() = File(context.filesDir, "VoiceDrops").also { if (!it.exists()) it.mkdirs() }

    init {
        loadRecordings()
    }

    val defaultDrop: VoiceDrop?
        get() = recordings.firstOrNull { it.isDefault }

    fun dropForStage(stageID: String?): VoiceDrop? =
        recordings.firstOrNull { it.stageID == stageID } ?: defaultDrop

    fun startRecording(name: String, stageID: String?) {
        if (isRecording) return
        val filename = "voicedrop_${System.currentTimeMillis()}.m4a"
        val file = File(voiceDropsDir, filename)

        currentRecordingName = name
        currentRecordingStageID = stageID
        currentRecordingFilename = filename

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        isRecording = true
    }

    fun stopRecording() {
        if (!isRecording) return
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false

        val name = currentRecordingName ?: return
        val filename = currentRecordingFilename ?: return

        val isFirst = recordings.isEmpty()
        val drop = VoiceDrop(
            name = name,
            filename = filename,
            stageID = currentRecordingStageID,
            isDefault = isFirst
        )
        recordings = recordings + drop
        saveRecordings()

        currentRecordingName = null
        currentRecordingStageID = null
        currentRecordingFilename = null
    }

    fun delete(drop: VoiceDrop) {
        File(voiceDropsDir, drop.filename).delete()
        val updated = recordings.filter { it.id != drop.id }
        recordings = if (drop.isDefault && updated.isNotEmpty()) {
            listOf(updated.first().copy(isDefault = true)) + updated.drop(1)
        } else {
            updated
        }
        saveRecordings()
    }

    fun setDefault(drop: VoiceDrop) {
        recordings = recordings.map { it.copy(isDefault = it.id == drop.id) }
        saveRecordings()
    }

    fun fileUrl(drop: VoiceDrop): File = File(voiceDropsDir, drop.filename)

    fun play(drop: VoiceDrop) {
        stopPlayback()
        val file = File(voiceDropsDir, drop.filename)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                isPlaying = false
                playingDropID = null
            }
            prepare()
            start()
        }
        isPlaying = true
        playingDropID = drop.id
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        playingDropID = null
    }

    fun rename(drop: VoiceDrop, newName: String) {
        recordings = recordings.map {
            if (it.id == drop.id) it.copy(name = newName) else it
        }
        saveRecordings()
    }

    suspend fun sendVoiceDrop(drop: VoiceDrop, toPhone: String, contactName: String) = withContext(Dispatchers.IO) {
        val file = File(voiceDropsDir, drop.filename)
        if (!file.exists()) return@withContext

        val audioBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val body = JSONObject().apply {
            put("audio", audioBase64)
            put("phone", toPhone)
            put("contactName", contactName)
            put("filename", drop.filename)
        }

        val url = URL("$serverBaseURL/api/dialer-voicedrop")
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(body.toString().toByteArray()) }
            val code = responseCode
            disconnect()
            if (code !in 200..299) throw Exception("Voice drop send failed: $code")
        }
    }

    suspend fun uploadDefaultDrop() = withContext(Dispatchers.IO) {
        val drop = defaultDrop ?: return@withContext
        val file = File(voiceDropsDir, drop.filename)
        if (!file.exists()) return@withContext

        val audioBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val body = JSONObject().apply {
            put("audio", audioBase64)
            put("filename", drop.filename)
            put("name", drop.name)
        }

        val url = URL("$serverBaseURL/api/upload-voicedrop")
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(body.toString().toByteArray()) }
            val code = responseCode
            disconnect()
            if (code !in 200..299) throw Exception("Voice drop upload failed: $code")
        }
    }

    private fun saveRecordings() {
        val arr = JSONArray()
        recordings.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("voice_drops_v1", arr.toString()).apply()
    }

    private fun loadRecordings() {
        val raw = prefs.getString("voice_drops_v1", null) ?: return
        val arr = JSONArray(raw)
        val list = mutableListOf<VoiceDrop>()
        for (i in 0 until arr.length()) {
            list.add(VoiceDrop.fromJson(arr.getJSONObject(i)))
        }
        recordings = list
    }
}
