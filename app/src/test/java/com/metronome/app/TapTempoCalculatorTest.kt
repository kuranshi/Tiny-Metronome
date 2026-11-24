package com.metronome.app

import com.metronome.app.engine.TapTempoCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoCalculatorTest {

    @Test
    fun calculatesAccurateBpmAndResetsAfterTimeout() {
        val tap = TapTempoCalculator()

        // First tap yields null
        assertNull(tap.recordTap(1000L))
        // 500ms interval -> 120 BPM
        assertEquals(120, tap.recordTap(1500L))
        assertEquals(120, tap.recordTap(2000L))

        // Wait 3000ms (> 2200ms timeout) -> resets session
        assertNull(tap.recordTap(5000L))
        // 750ms interval -> 80 BPM
        assertEquals(80, tap.recordTap(5750L))
        assertEquals(80, tap.recordTap(6500L))
    }

    @Test
    fun clampsOutOfRangeTempos() {
        val tap = TapTempoCalculator()
        assertNull(tap.recordTap(0L))
        // 50ms interval -> raw 1200 BPM, clamped to 300
        assertEquals(300, tap.recordTap(50L))
    }
}
