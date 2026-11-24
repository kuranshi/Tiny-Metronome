package com.metronome.app

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metronome.app.ui.MetronomeScreen
import com.metronome.app.ui.MetronomeViewModel
import com.metronome.app.ui.theme.MetronomeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MetronomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 16 (API 36) enforces Edge-to-Edge; configure dark transparent system bars cleanly
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            MetronomeTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.isPlaying, state.config.keepScreenOn) {
                    if (state.isPlaying && state.config.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                MetronomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Gracefully pause when leaving the app so audio doesn't unexpectedly play forever
        if (!isChangingConfigurations) {
            viewModel.stopPlayback()
        }
    }
}
