package com.cineverse.core.player

import com.cineverse.core.player.model.AudioTrackOption
import com.cineverse.core.player.model.SubtitleTrackOption
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TrackLabelsTest {

    @Test
    fun `labels audio by language and separates duplicates by channel layout`() {
        val names = TrackLabels.audioNames(
            listOf(
                AudioTrackOption("a", "en", "stream_5", 2),
                AudioTrackOption("b", "de", "stream_4", 2),
                AudioTrackOption("c", "en", "stream_6", 6),
            ),
            Locale.ENGLISH,
        )
        assertEquals(mapOf("a" to "English (Stereo)", "b" to "German", "c" to "English (5.1)"), names)
    }

    @Test
    fun `falls back to manifest label, then numbering`() {
        val names = TrackLabels.audioNames(
            listOf(
                AudioTrackOption("a", "en", "Main", 2),
                AudioTrackOption("b", "en", "Commentary", 2),
                AudioTrackOption("c", null, null, 2),
            ),
            Locale.ENGLISH,
        )
        assertEquals(mapOf("a" to "English (Main)", "b" to "English (Commentary)", "c" to "Track 3"), names)
    }

    @Test
    fun `marks forced and closed caption subtitles`() {
        val names = TrackLabels.subtitleNames(
            listOf(
                SubtitleTrackOption("a", "fr", null, isForced = false, isClosedCaption = false),
                SubtitleTrackOption("b", "fr", null, isForced = true, isClosedCaption = false),
                SubtitleTrackOption("c", "en", null, isForced = false, isClosedCaption = true),
            ),
            Locale.ENGLISH,
        )
        assertEquals(mapOf("a" to "French", "b" to "French (forced)", "c" to "English CC"), names)
    }
}
