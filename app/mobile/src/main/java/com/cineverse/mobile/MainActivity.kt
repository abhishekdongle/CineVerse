package com.cineverse.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cineverse.mobile.navigation.MobileNavHost
import com.cineverse.mobile.ui.theme.CineVerseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineVerseTheme {
                // Each destination handles its own insets: the player draws edge to edge in fullscreen.
                MobileNavHost()
            }
        }
    }
}
