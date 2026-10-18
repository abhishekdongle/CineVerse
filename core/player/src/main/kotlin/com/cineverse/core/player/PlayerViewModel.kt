package com.cineverse.core.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Owns one [CineVersePlayer] per player screen, so rotation, resizing, theme changes and PiP
 * never recreate the player or lose the position. Not UI code; any player UI module can use it.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    playerFactory: CineVersePlayer.Factory,
) : ViewModel() {

    val controller: PlayerController = playerFactory.create()

    override fun onCleared() {
        (controller as CineVersePlayer).release()
    }
}
