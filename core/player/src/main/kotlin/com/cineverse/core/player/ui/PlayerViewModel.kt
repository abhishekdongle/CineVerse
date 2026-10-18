package com.cineverse.core.player.ui

import androidx.lifecycle.ViewModel
import com.cineverse.core.player.CineVersePlayer
import com.cineverse.core.player.PlayableMedia
import com.cineverse.core.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Owns the player so playback survives rotation and other configuration changes. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    playerFactory: CineVersePlayer.Factory,
) : ViewModel() {

    val player: CineVersePlayer = playerFactory.create()
    val state: StateFlow<PlaybackState> = player.state

    fun load(media: PlayableMedia) = player.load(media)

    override fun onCleared() {
        player.release()
    }
}
