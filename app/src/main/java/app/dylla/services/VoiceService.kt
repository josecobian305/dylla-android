package app.dylla.services

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class VoiceService private constructor() {

    companion object {
        val shared = VoiceService()
        private const val ELEVENLABS_VOICE_ID = "iP95p4xoKVk53GoZ742B"
        private const val ELEVENLABS_URL = "https://api.elevenlabs.io/v1/text-to-speech/$ELEVENLABS_VOICE_ID"
        private const val ELEVENLABS_API_KEY = "ELEVENLABS_API_KEY_PLACEHOLDER"
        private const val SAMPLE_RATE = 44100
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    var isSpeaking by mutableStateOf(false)
        private set
    var isListening by mutableStateOf(false)
        private set
    var transcript by mutableStateOf("")
        private set
    var voiceEnabled by mutableStateOf(true)

    val analyzer = VoiceAnalyzer()

    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var audioRecordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun speak(text: String) {
        if (!voiceEnabled) return
        scope.launch {
            isSpeaking = true
            try {
                val audioBytes = fetchTTSAudio(text)
                playAudioBytes(audioBytes)
            } catch (_: Exception) {
                isSpeaking = false
            }
        }
    }

    private suspend fun fetchTTSAudio(text: String): ByteArray = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_multilingual_v2")
            put("voice_settings", JSONObject().apply {
                put("stability", 0.5)
                put("similarity_boost", 0.75)
                put("style", 0.4)
                put("use_speaker_boost", true)
            })
        }

        val url = URL(ELEVENLABS_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("xi-api-key", ELEVENLABS_API_KEY)
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw Exception("ElevenLabs TTS failed: ${conn.responseCode}")
        }

        val bytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        bytes
    }

    private suspend fun playAudioBytes(bytes: ByteArray) {
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("tts_", ".mp3").also { file ->
                FileOutputStream(file).use { it.write(bytes) }
            }
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            setOnCompletionListener {
                isSpeaking = false
                tempFile.delete()
            }
            prepare()
            start()
        }
    }

    fun stopSpeaking() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isSpeaking = false
    }

    fun requestPermissions(activity: Activity): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
        return granted
    }

    fun startListening(context: Context) {
        if (isListening) return
        transcript = ""
        isListening = true

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isListening = false
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        transcript = matches[0]
                    }
                    isListening = false
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        transcript = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            startListening(intent)
        }

        startAudioRecordForAnalysis(context)
    }

    private fun startAudioRecordForAnalysis(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).apply { startRecording() }

        audioRecordJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize / 2)
            while (isListening) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0) {
                    val chunk = buffer.copyOf(read)
                    analyzer.processAudioBuffer(chunk, SAMPLE_RATE)
                }
            }
        }
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null

        audioRecordJob?.cancel()
        audioRecordJob = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }
}
