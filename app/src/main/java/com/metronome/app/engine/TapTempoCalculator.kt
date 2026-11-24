package com.metronome.app.engine

import com.metronome.app.model.MetronomeConfig
import kotlin.math.roundToInt

/**
 * Moving-window Tap Tempo estimator.
 * Automatically resets after [resetTimeoutMs] of inactivity so new tapping sessions start cleanly.
 */
class TapTempoCalculator(
    private val maxSamples: Int = 6,
    private val resetTimeoutMs: Long = 2_200L
) {
    private val timestamps = ArrayDeque<Long>()

    fun recordTap(nowMs: Long): Int? {
        val last = timestamps.lastOrNull()
        if (last != null && (nowMs - last) > resetTimeoutMs) {
            timestamps.clear()
        }
        if (last != null && nowMs <= last) {
            return null
        }

        timestamps.addLast(nowMs)
        while (timestamps.size > maxSamples + 1) {
            timestamps.removeFirst()
        }

        if (timestamps.size < 2) return null

        val totalSpanMs = (timestamps.last() - timestamps.first()).toDouble()
        val intervals = (timestamps.size - 1).toDouble()
        val avgIntervalMs = totalSpanMs / intervals
        if (avgIntervalMs <= 0.0) return null

        val rawBpm = (60_000.0 / avgIntervalMs).roundToInt()
        return rawBpm.coerceIn(MetronomeConfig.MIN_BPM, MetronomeConfig.MAX_BPM)
    }

    fun reset() {
        timestamps.clear()
    }
}
