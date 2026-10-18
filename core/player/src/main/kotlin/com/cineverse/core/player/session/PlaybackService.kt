package com.cineverse.core.player.session

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Exposes the active [MediaSession] to the system, which gives the media notification,
 * lock-screen controls, headset buttons and Assistant/Wear/Auto control. The session itself
 * belongs to [com.cineverse.core.player.CineVersePlayer]; this service only publishes it.
 */
class PlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        ActiveSessionHolder.session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // The user swiped the app away: stop playback instead of continuing in the background.
        pauseAllPlayersAndStopSelf()
    }
}

/** The session of the player currently on screen. Only one CineVersePlayer exists at a time. */
internal object ActiveSessionHolder {
    @Volatile
    var session: MediaSession? = null
}
