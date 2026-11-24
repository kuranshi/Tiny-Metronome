package com.metronome.app

import com.metronome.app.engine.SoundSynthesizer
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.SoundKit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoundSynthesizerTest {

    @Test
    fun allSoundKitsHaveStrictLoudnessOrderingAndNoClipping() {
        val sampleRate = 48_000
        for (kit in SoundKit.entries) {
            val bank = SoundSynthesizer.buildBank(kit, sampleRate)

            val accentWave = bank.waveformFor(AccentLevel.ACCENT, isSubdivision = false)
            val mediumWave = bank.waveformFor(AccentLevel.MEDIUM, isSubdivision = false)
            val normalWave = bank.waveformFor(AccentLevel.NORMAL, isSubdivision = false)
            val subWave = bank.waveformFor(AccentLevel.NORMAL, isSubdivision = true)
            val muteWave = bank.waveformFor(AccentLevel.MUTE, isSubdivision = false)

            assertTrue("${kit.name} accent empty", accentWave.isNotEmpty())
            assertTrue("${kit.name} medium empty", mediumWave.isNotEmpty())
            assertTrue("${kit.name} normal empty", normalWave.isNotEmpty())
            assertTrue("${kit.name} sub empty", subWave.isNotEmpty())
            assertEquals("${kit.name} mute must be empty", 0, muteWave.size)

            val pAccent = peakOf(accentWave)
            val pMedium = peakOf(mediumWave)
            val pNormal = peakOf(normalWave)
            val pSub = peakOf(subWave)

            assertTrue("${kit.name} peak clipped: $pAccent", pAccent <= 1.0f)
            assertTrue("${kit.name} expected accent > medium", pAccent > pMedium)
            assertTrue("${kit.name} expected medium > normal", pMedium > pNormal)
            assertTrue("${kit.name} expected normal > sub", pNormal > pSub)
        }
    }

    private fun peakOf(samples: FloatArray): Float {
        var max = 0f
        for (s in samples) {
            val a = abs(s)
            if (a > max) max = a
        }
        return max
    }
}
