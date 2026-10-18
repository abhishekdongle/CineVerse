package com.cineverse.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.Surface
import com.cineverse.tv.navigation.TvNavHost
import com.cineverse.tv.ui.theme.CineVerseTvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CineVerseTvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TvNavHost()
                }
            }
        }
    }
}
