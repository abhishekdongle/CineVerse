package com.cineverse.feature.videoplayer.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cineverse.core.player.analytics.ProgressReporter
import com.cineverse.core.player.model.AudioTrackOption
import com.cineverse.core.player.model.ContentType
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlaybackStatus
import com.cineverse.core.player.model.PlayerError
import com.cineverse.core.player.model.SubtitleTrackOption
import com.cineverse.core.player.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest {

    private val controller = RecordingPlayerController(isCastAvailable = true)
    private val reporter = RecordingProgressReporter()
    private val store = ViewModelStore()

    private val vod = PlaybackRequest(id = "bbb", title = "Big Buck Bunny", subtitle = "Blender", streamUrl = "https://x/bbb.m3u8", type = ContentType.VOD)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    /**
     * runTest that clears the ViewModel before finishing. runTest runs every task left on the
     * shared test clock, so an uncancelled viewModelScope loop (progress reporting) never ends.
     */
    private fun playerTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            store.clear()
        }
    }

    private fun createViewModel(): VideoPlayerViewModel {
        val factory = viewModelFactory { initializer { VideoPlayerViewModel({ controller }, setOf(reporter)) } }
        return ViewModelProvider.create(store, factory)[VideoPlayerViewModel::class]
    }

    private fun playing(positionMs: Long = 0L) {
        controller.update {
            it.copy(status = PlaybackStatus.Ready, isPlaying = true, playWhenReady = true, positionMs = positionMs, durationMs = 600_000L, isSeekable = true)
        }
    }

    @Test
    fun `load starts the request and shows its title`() = playerTest {
        val viewModel = createViewModel()

        viewModel.onAction(VideoPlayerAction.Load(vod))

        assertEquals(listOf("load:bbb"), controller.commands)
        assertEquals("Big Buck Bunny", viewModel.uiState.value.title)
        assertEquals("Blender", viewModel.uiState.value.subtitle)
        assertTrue(viewModel.uiState.value.showCastButton)
    }

    @Test
    fun `maps tracks into settings options with Auto and Off entries`() = playerTest {
        val viewModel = createViewModel()
        controller.update {
            it.copy(
                videoQualities = listOf(VideoQuality("v1", 1280, 720, 3_000_000), VideoQuality("v2", 1920, 1080, 6_000_000)),
                currentVideoHeight = 720,
                audioTracks = listOf(AudioTrackOption("a1", "en", null, 2), AudioTrackOption("a2", "en", null, 6)),
                selectedAudioTrackId = "a2",
                subtitleTracks = listOf(SubtitleTrackOption("s1", "fr", null, isForced = false, isClosedCaption = false)),
                selectedSubtitleTrackId = "s1",
                playbackSpeed = 1.5f,
            )
        }

        val settings = viewModel.uiState.value.settings
        assertEquals(listOf(null, 720, 1080), settings.qualities.map { it.height })
        assertTrue(settings.qualities.first().isSelected)
        assertEquals(720, settings.autoQualityHeight)
        assertEquals("a2", settings.selectedAudio?.id)
        assertEquals(2, settings.audioTracks.map { it.label }.distinct().size)
        assertEquals(listOf(null, "s1"), settings.subtitleTracks.map { it.id })
        assertEquals("s1", settings.selectedSubtitle?.id)
        assertEquals(1.5f, settings.speeds.single { it.isSelected }.speed)
        assertTrue(settings.canChangeQuality && settings.canChangeAudio && settings.canChangeSubtitles)
    }

    @Test
    fun `settings selections go to the player and return to the main page`() = playerTest {
        val viewModel = createViewModel()
        viewModel.onAction(VideoPlayerAction.SettingsOpened)
        viewModel.onAction(VideoPlayerAction.SettingsPageOpened(SettingsPage.Quality))
        assertEquals(SettingsPage.Quality, viewModel.uiState.value.settings.page)

        viewModel.onAction(VideoPlayerAction.QualitySelected("v2"))
        viewModel.onAction(VideoPlayerAction.AudioSelected("a1"))
        viewModel.onAction(VideoPlayerAction.SubtitleSelected(null))
        viewModel.onAction(VideoPlayerAction.SpeedSelected(2f))

        assertEquals(listOf("quality:v2", "audio:a1", "subtitle:null", "speed:2.0"), controller.commands)
        assertEquals(SettingsPage.Main, viewModel.uiState.value.settings.page)
        assertTrue(viewModel.uiState.value.settings.isVisible)
    }

    @Test
    fun `settings back closes a sub-page first, then the panel`() = playerTest {
        val viewModel = createViewModel()
        viewModel.onAction(VideoPlayerAction.SettingsOpened)
        viewModel.onAction(VideoPlayerAction.SettingsPageOpened(SettingsPage.Audio))

        viewModel.onAction(VideoPlayerAction.SettingsBackPressed)
        assertEquals(SettingsPage.Main, viewModel.uiState.value.settings.page)
        assertTrue(viewModel.uiState.value.settings.isVisible)

        viewModel.onAction(VideoPlayerAction.SettingsBackPressed)
        assertFalse(viewModel.uiState.value.settings.isVisible)
    }

    @Test
    fun `settings are hidden during ads and in picture-in-picture`() = playerTest {
        val viewModel = createViewModel()
        viewModel.onAction(VideoPlayerAction.SettingsOpened)

        controller.update { it.copy(isPlayingAd = true) }
        assertFalse(viewModel.uiState.value.settings.isVisible)

        controller.update { it.copy(isPlayingAd = false) }
        viewModel.onAction(VideoPlayerAction.PictureInPictureChanged(true))
        assertFalse(viewModel.uiState.value.settings.isVisible)
        assertTrue(viewModel.uiState.value.isInPictureInPicture)
    }

    @Test
    fun `controls auto-hide after the delay while playing`() = playerTest {
        val viewModel = createViewModel()
        playing()
        assertTrue(viewModel.uiState.value.controlsVisible)

        advanceTimeBy(VideoPlayerViewModel.DEFAULT_AUTO_HIDE_DELAY_MS - 1)
        assertTrue(viewModel.uiState.value.controlsVisible)
        advanceTimeBy(2)
        assertFalse(viewModel.uiState.value.controlsVisible)

        viewModel.onAction(VideoPlayerAction.VideoTapped)
        assertTrue(viewModel.uiState.value.controlsVisible)
    }

    @Test
    fun `interaction restarts the auto-hide timer`() = playerTest {
        val viewModel = createViewModel()
        playing()
        advanceTimeBy(3_000)

        viewModel.onAction(VideoPlayerAction.SeekForwardClicked(10_000))
        advanceTimeBy(3_000)

        assertTrue(viewModel.uiState.value.controlsVisible)
        assertEquals(listOf("forward:10000"), controller.commands)
    }

    @Test
    fun `controls stay visible while paused, scrubbing, in settings or with TalkBack`() = playerTest {
        val viewModel = createViewModel()
        advanceTimeBy(10_000)
        assertTrue("paused", viewModel.uiState.value.controlsVisible)

        playing()
        viewModel.onAction(VideoPlayerAction.Scrubbing(42_000))
        advanceTimeBy(10_000)
        assertTrue("scrubbing", viewModel.uiState.value.controlsVisible)
        assertEquals(42_000, viewModel.uiState.value.displayPositionMs)
        viewModel.onAction(VideoPlayerAction.Scrubbing(null))

        viewModel.onAction(VideoPlayerAction.SettingsOpened)
        advanceTimeBy(10_000)
        assertTrue("settings", viewModel.uiState.value.controlsVisible)
        viewModel.onAction(VideoPlayerAction.SettingsDismissed)

        viewModel.onAction(VideoPlayerAction.AccessibilityChanged(touchExplorationEnabled = true, controlsTimeoutMs = 4_000))
        advanceTimeBy(10_000)
        assertTrue("talkback", viewModel.uiState.value.controlsVisible)
    }

    @Test
    fun `phones go fullscreen in landscape, large screens only on request`() = playerTest {
        val viewModel = createViewModel()

        viewModel.onAction(VideoPlayerAction.WindowChanged(isLargeScreen = false, isLandscape = true))
        assertTrue(viewModel.uiState.value.isFullscreen)
        assertTrue(viewModel.uiState.value.settings.asSidePanel)

        viewModel.onAction(VideoPlayerAction.WindowChanged(isLargeScreen = true, isLandscape = true))
        assertFalse(viewModel.uiState.value.isFullscreen)

        viewModel.onAction(VideoPlayerAction.FullscreenToggled)
        assertTrue(viewModel.uiState.value.isFullscreen)
        viewModel.onAction(VideoPlayerAction.FullscreenToggled)
        assertFalse(viewModel.uiState.value.isFullscreen)
    }

    @Test
    fun `double tap seeks and accumulates feedback on the same side`() = playerTest {
        val viewModel = createViewModel()
        playing()

        viewModel.onAction(VideoPlayerAction.DoubleTapped(forward = true, incrementMs = 10_000))
        viewModel.onAction(VideoPlayerAction.DoubleTapped(forward = true, incrementMs = 10_000))
        assertEquals(SeekFeedback(forward = true, seconds = 20), viewModel.uiState.value.seekFeedback)

        viewModel.onAction(VideoPlayerAction.DoubleTapped(forward = false, incrementMs = 10_000))
        assertEquals(SeekFeedback(forward = false, seconds = 10), viewModel.uiState.value.seekFeedback)
        assertEquals(listOf("forward:10000", "forward:10000", "back:10000"), controller.commands)

        advanceTimeBy(VideoPlayerViewModel.SEEK_FEEDBACK_MS + 1)
        assertNull(viewModel.uiState.value.seekFeedback)
    }

    @Test
    fun `double tap is ignored when the stream cannot seek`() = playerTest {
        val viewModel = createViewModel()
        controller.update { it.copy(isLive = true, isSeekable = true, durationMs = 30_000) }

        viewModel.onAction(VideoPlayerAction.DoubleTapped(forward = true, incrementMs = 10_000))

        assertTrue(controller.commands.isEmpty())
        assertNull(viewModel.uiState.value.seekFeedback)
    }

    @Test
    fun `live state shows distance behind the live edge, including while scrubbing`() = playerTest {
        val viewModel = createViewModel()
        controller.update {
            it.copy(isLive = true, isSeekable = true, durationMs = 300_000, positionMs = 200_000, liveBehindMs = 60_000)
        }
        assertTrue(viewModel.uiState.value.canSeek)
        assertEquals(60_000, viewModel.uiState.value.liveBehindMs)

        viewModel.onAction(VideoPlayerAction.Scrubbing(150_000))
        assertEquals(110_000, viewModel.uiState.value.liveBehindMs)

        viewModel.onAction(VideoPlayerAction.GoToLiveClicked)
        assertEquals(listOf("live"), controller.commands)
    }

    @Test
    fun `errors expose a message and retry goes to the player`() = playerTest {
        val viewModel = createViewModel()
        controller.update { it.copy(status = PlaybackStatus.Error, error = PlayerError(2001, "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", null)) }

        assertEquals("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", viewModel.uiState.value.error?.message)
        viewModel.onAction(VideoPlayerAction.RetryClicked)
        assertEquals(listOf("retry"), controller.commands)
    }

    @Test
    fun `backgrounding pauses unless casting`() = playerTest {
        val viewModel = createViewModel()
        viewModel.onAction(VideoPlayerAction.AppBackgrounded)

        controller.update { it.copy(isCasting = true, castDeviceName = "Living room") }
        viewModel.onAction(VideoPlayerAction.AppBackgrounded)

        assertEquals(listOf("pause"), controller.commands)
        assertFalse(viewModel.uiState.value.canEnterPictureInPicture)
        assertEquals("Living room", viewModel.uiState.value.castDeviceName)
    }

    @Test
    fun `reports progress periodically, on pause and when the screen closes`() = playerTest {
        val viewModel = createViewModel()
        viewModel.onAction(VideoPlayerAction.Load(vod))
        playing(positionMs = 1_000)

        advanceTimeAndUpdatePosition(15_000)
        assertEquals(listOf(Report(16_000, isFinal = false)), reporter.reports)

        controller.update { it.copy(isPlaying = false, playWhenReady = false) }
        assertEquals(Report(16_000, isFinal = true), reporter.reports.last())
        advanceTimeBy(60_000)
        assertEquals(2, reporter.reports.size)

        store.clear()
        assertEquals(Report(16_000, isFinal = true), reporter.reports.last())
        assertTrue(controller.released)
    }

    @Test
    fun `does not report progress for live streams or ads`() = playerTest {
        createViewModel().onAction(VideoPlayerAction.Load(vod.copy(type = ContentType.LIVE)))
        controller.update { it.copy(isLive = true, isPlaying = true, status = PlaybackStatus.Ready, durationMs = 600_000) }
        advanceTimeBy(30_000)
        controller.update { it.copy(isLive = false, isPlayingAd = true) }
        advanceTimeBy(30_000)

        assertTrue(reporter.reports.isEmpty())
    }

    private fun TestScope.advanceTimeAndUpdatePosition(ms: Long) {
        controller.update { it.copy(positionMs = it.positionMs + ms) }
        advanceTimeBy(ms)
        runCurrent()
    }

    private data class Report(val positionMs: Long, val isFinal: Boolean)

    private class RecordingProgressReporter : ProgressReporter {
        val reports = mutableListOf<Report>()
        override fun reportProgress(request: PlaybackRequest, positionMs: Long, durationMs: Long, isFinal: Boolean) {
            reports += Report(positionMs, isFinal)
        }
    }
}
