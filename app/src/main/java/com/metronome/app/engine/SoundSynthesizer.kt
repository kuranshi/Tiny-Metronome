package com.metronome.app.engine

import com.metronome.app.model.AccentLevel
import com.metronome.app.model.SoundKit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Holds pre-synthesized PCM float waveforms [-1.0, 1.0] for one sound kit across all AccentLevels.
 * Pre-computing these off the audio thread guarantees zero allocation during real-time playback.
 */
data class SynthesizedSoundBank(
    val accent: FloatArray,
    val medium: FloatArray,
    val normal: FloatArray,
    val subdivision: FloatArray
) {
    fun waveformFor(accentLevel: AccentLevel, isSubdivision: Boolean): FloatArray {
        if (accentLevel == AccentLevel.MUTE) return EMPTY
        if (isSubdivision) {
            return when (accentLevel) {
                AccentLevel.ACCENT -> accent
                AccentLevel.MEDIUM -> medium
                AccentLevel.NORMAL -> subdivision
                AccentLevel.MUTE -> EMPTY
            }
        }
        return when (accentLevel) {
            AccentLevel.ACCENT -> accent
            AccentLevel.MEDIUM -> medium
            AccentLevel.NORMAL -> normal
            AccentLevel.MUTE -> EMPTY
        }
    }

    companion object {
        val EMPTY = FloatArray(0)
    }
}

/**
 * Pure-Kotlin procedural audio synthesizer producing crisp, sample-aligned click waveforms
 * with zero-latency sharp attack transients (attack peak within first 0.4ms) so rhythmic onset
 * timing is unmistakable to the human ear.
 */
object SoundSynthesizer {

    fun buildBank(
        kit: SoundKit,
        sampleRate: Int,
        pitchMultiplier: Double = 1.0
    ): SynthesizedSoundBank {
        return SynthesizedSoundBank(
            accent = synthesize(kit, sampleRate, gain = 0.96, pitchScale = 1.38 * pitchMultiplier, durationMs = 42.0),
            medium = synthesize(kit, sampleRate, gain = 0.74, pitchScale = 1.14 * pitchMultiplier, durationMs = 36.0),
            normal = synthesize(kit, sampleRate, gain = 0.54, pitchScale = 0.92 * pitchMultiplier, durationMs = 30.0),
            subdivision = synthesize(kit, sampleRate, gain = 0.36, pitchScale = 0.78 * pitchMultiplier, durationMs = 22.0)
        )
    }

    private fun synthesize(
        kit: SoundKit,
        sampleRate: Int,
        gain: Double,
        pitchScale: Double,
        durationMs: Double
    ): FloatArray {
        val sampleCount = ((durationMs / 1000.0) * sampleRate).roundToInt().coerceAtLeast(64)
        val out = FloatArray(sampleCount)
        // Very fast attack (0.25ms) so onset starts precisely at sample 0 without DC click pop
        val attackSamples = (sampleRate * 0.00025).roundToInt().coerceAtLeast(4)
        val releaseTail = (sampleRate * 0.002).roundToInt().coerceAtLeast(16)

        // Deterministic PRNG for noise bursts so waveforms are 100% reproducible across runs/tests
        var lfsr = 0x5A3C19E7L

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate.toDouble()

            // Deterministic white noise in [-1, 1]
            lfsr = (lfsr * 1664525L + 1013904223L) and 0xFFFFFFFFL
            val noise = (lfsr.toDouble() / 0x7FFFFFFFL.toDouble()) - 1.0

            val raw = when (kit) {
                SoundKit.WOODBLOCK -> {
                    val f1 = 760.0 * pitchScale
                    val f2 = f1 * 2.71 // hollow wooden overtone
                    val body = sin(2.0 * PI * f1 * t) * exp(-t * 95.0) +
                        0.42 * sin(2.0 * PI * f2 * t) * exp(-t * 165.0)
                    val knock = noise * exp(-t * 680.0) * 0.35
                    body + knock
                }

                SoundKit.MECHANICAL -> {
                    val f1 = 1050.0 * pitchScale
                    val f2 = 2480.0 * pitchScale
                    val tick = sin(2.0 * PI * f1 * t) * exp(-t * 145.0) * 0.55 +
                        sin(2.0 * PI * f2 * t) * exp(-t * 230.0) * 0.45 +
                        noise * exp(-t * 900.0) * 0.65
                    tick
                }

                SoundKit.DIGITAL -> {
                    val f1 = 1000.0 * pitchScale
                    val f2 = f1 * 2.0
                    (sin(2.0 * PI * f1 * t) * 0.85 + sin(2.0 * PI * f2 * t) * 0.15) * exp(-t * 110.0)
                }

                SoundKit.RIMSHOT -> {
                    val f1 = 1650.0 * pitchScale
                    val f2 = 3120.0 * pitchScale
                    val rim = sin(2.0 * PI * f1 * t) * exp(-t * 120.0) * 0.48 +
                        sin(2.0 * PI * f2 * t) * exp(-t * 180.0) * 0.34 +
                        noise * exp(-t * 360.0) * 0.58
                    rim
                }
            }

            // Fast linear attack & smooth cosine tail fade
            var env = 1.0
            if (i < attackSamples) {
                env *= (i + 1).toDouble() / attackSamples.toDouble()
            }
            val remaining = sampleCount - 1 - i
            if (remaining < releaseTail) {
                env *= remaining.toDouble() / releaseTail.toDouble()
            }

            out[i] = (raw * env * gain).toFloat()
        }

        // Peak-normalize to target gain so loudness ordering (ACCENT > MEDIUM > NORMAL > SUB) is exact
        var peak = 0f
        for (v in out) {
            val a = kotlin.math.abs(v)
            if (a > peak) peak = a
        }
        if (peak > 1e-6f) {
            val scale = gain.toFloat() / peak
            for (i in out.indices) {
                out[i] = (out[i] * scale).coerceIn(-0.98f, 0.98f)
            }
        }
        return out
    }
}
