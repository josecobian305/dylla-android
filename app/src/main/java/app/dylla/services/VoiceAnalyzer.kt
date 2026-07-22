package app.dylla.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.log10
import kotlin.math.sqrt

enum class EnergyLevel { QUIET, LOW, GOOD, STRONG, LOUD }

data class VoiceAnalysisSummary(
    val avgReactionTime: Double,
    val avgVolume: Float,
    val avgPitch: Float,
    val pitchVariance: Float,
    val fillerCount: Int,
    val uptalkCount: Int,
    val tonalityScore: Int,
    val energyConsistency: Float
) {
    val inflectionRating: String get() = when {
        pitchVariance < 10f -> "Monotone"
        pitchVariance < 25f -> "Flat"
        pitchVariance < 50f -> "Good"
        else -> "Excellent"
    }

    val reactionRating: String get() = when {
        avgReactionTime < 0.3 -> "Too Fast"
        avgReactionTime < 0.8 -> "Sharp"
        avgReactionTime < 2.0 -> "Good"
        avgReactionTime < 3.5 -> "Slow"
        else -> "Dead Air"
    }
}

class VoiceAnalyzer {

    var currentVolume by mutableFloatStateOf(0f)
        private set
    var averagePitch by mutableFloatStateOf(0f)
        private set
    var energyLevel by mutableStateOf(EnergyLevel.QUIET)
        private set
    var fillerCount by mutableIntStateOf(0)
        private set
    var uptalkCount by mutableIntStateOf(0)
        private set
    var reactionTime by mutableDoubleStateOf(0.0)
        private set
    var liveFeedback by mutableStateOf("")
        private set
    var tonalityScore by mutableIntStateOf(100)
        private set

    private val volumeHistory = mutableListOf<Float>()
    private val pitchHistory = mutableListOf<Float>()
    private val reactionTimes = mutableListOf<Double>()
    private var merchantFinishTime: Long? = null
    private val lastPitchValues = mutableListOf<Float>()

    private val fillerWords = listOf("um", "uh", "like", "you know", "so", "actually", "basically", "right")

    fun processAudioBuffer(buffer: ShortArray, sampleRate: Int) {
        val floats = FloatArray(buffer.size) { buffer[it].toFloat() }

        val sumSquares = floats.fold(0.0) { acc, v -> acc + v * v }
        val rms = sqrt(sumSquares / floats.size).toFloat()
        val dB = if (rms > 0) 20f * log10(rms / 32768f) else -100f

        currentVolume = dB
        volumeHistory.add(dB)

        energyLevel = when {
            dB < -50f -> EnergyLevel.QUIET
            dB < -35f -> EnergyLevel.LOW
            dB < -20f -> EnergyLevel.GOOD
            dB < -10f -> EnergyLevel.STRONG
            else -> EnergyLevel.LOUD
        }

        val pitch = estimatePitch(floats, sampleRate)
        if (pitch > 0f) {
            pitchHistory.add(pitch)
            lastPitchValues.add(pitch)
            if (lastPitchValues.size > 10) lastPitchValues.removeAt(0)
            averagePitch = pitchHistory.average().toFloat()
        }
    }

    private fun estimatePitch(samples: FloatArray, sampleRate: Int): Float {
        val minLag = sampleRate / 400
        val maxLag = sampleRate / 80
        if (samples.size < maxLag) return 0f

        var bestLag = 0
        var bestCorrelation = 0f

        for (lag in minLag..minOf(maxLag, samples.size - 1)) {
            var correlation = 0f
            val limit = samples.size - lag
            for (i in 0 until limit) {
                correlation += samples[i] * samples[i + lag]
            }
            correlation /= limit
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }

        return if (bestLag > 0 && bestCorrelation > 0) sampleRate.toFloat() / bestLag else 0f
    }

    fun onMerchantFinished() {
        merchantFinishTime = System.currentTimeMillis()
    }

    fun onUserStarted() {
        val finish = merchantFinishTime ?: return
        val elapsed = (System.currentTimeMillis() - finish) / 1000.0
        reactionTime = elapsed
        reactionTimes.add(elapsed)

        when {
            elapsed > 3.0 -> {
                tonalityScore = (tonalityScore - 5).coerceAtLeast(0)
                liveFeedback = "Slow reaction — pick up the pace"
            }
            elapsed < 0.3 -> {
                tonalityScore = (tonalityScore - 4).coerceAtLeast(0)
                liveFeedback = "Too fast — let the merchant finish"
            }
            elapsed < 0.8 -> liveFeedback = "Sharp response!"
            else -> liveFeedback = "Good timing"
        }
        merchantFinishTime = null
    }

    fun onUserFinished(transcript: String) {
        val lower = transcript.lowercase()

        var fillers = 0
        for (filler in fillerWords) {
            var startIndex = 0
            while (true) {
                val idx = lower.indexOf(filler, startIndex)
                if (idx < 0) break
                val before = idx == 0 || !lower[idx - 1].isLetter()
                val after = idx + filler.length >= lower.length || !lower[idx + filler.length].isLetter()
                if (before && after) fillers++
                startIndex = idx + filler.length
            }
        }
        fillerCount += fillers
        tonalityScore = (tonalityScore - fillers * 3).coerceAtLeast(0)

        if (lastPitchValues.size >= 3) {
            val tail = lastPitchValues.takeLast(3)
            val rise = (tail.last() - tail.first()) / tail.first()
            if (rise > 0.15f) {
                uptalkCount++
                tonalityScore = (tonalityScore - 5).coerceAtLeast(0)
                liveFeedback = "Uptalk detected — end with confidence"
            }
        }

        val variance = pitchVariance()
        if (variance < 10f) {
            tonalityScore = (tonalityScore - 10).coerceAtLeast(0)
            liveFeedback = "Monotone — vary your pitch"
        }
    }

    fun reset() {
        currentVolume = 0f
        averagePitch = 0f
        energyLevel = EnergyLevel.QUIET
        fillerCount = 0
        uptalkCount = 0
        reactionTime = 0.0
        liveFeedback = ""
        tonalityScore = 100
        volumeHistory.clear()
        pitchHistory.clear()
        reactionTimes.clear()
        merchantFinishTime = null
        lastPitchValues.clear()
    }

    private fun pitchVariance(): Float {
        if (pitchHistory.size < 2) return 0f
        val mean = pitchHistory.average().toFloat()
        val variance = pitchHistory.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance.toDouble()).toFloat()
    }

    val summary: VoiceAnalysisSummary
        get() {
            val avgRT = if (reactionTimes.isNotEmpty()) reactionTimes.average() else 0.0
            val avgVol = if (volumeHistory.isNotEmpty()) volumeHistory.average().toFloat() else 0f
            val avgP = if (pitchHistory.isNotEmpty()) pitchHistory.average().toFloat() else 0f
            val pv = pitchVariance()

            val energyMean = if (volumeHistory.isNotEmpty()) volumeHistory.average().toFloat() else 0f
            val energyStdDev = if (volumeHistory.size > 1) {
                sqrt(volumeHistory.map { (it - energyMean) * (it - energyMean) }.average()).toFloat()
            } else 0f
            val energyConsistency = (1f - (energyStdDev / 50f).coerceIn(0f, 1f))

            return VoiceAnalysisSummary(
                avgReactionTime = avgRT,
                avgVolume = avgVol,
                avgPitch = avgP,
                pitchVariance = pv,
                fillerCount = fillerCount,
                uptalkCount = uptalkCount,
                tonalityScore = tonalityScore,
                energyConsistency = energyConsistency
            )
        }
}
