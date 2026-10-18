package com.cineverse.feature.home.presentation

import com.cineverse.core.analytics.AnalyticsEvent
import com.cineverse.core.analytics.AnalyticsTracker
import com.cineverse.core.domain.model.HomeFeed
import com.cineverse.core.domain.repository.CatalogRepository
import com.cineverse.core.domain.usecase.GetHomeFeedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val analytics = RecordingAnalytics()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `emits success and tracks the screen view`() = runTest {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(repository { HomeFeed(null, emptyList()) }), analytics)

        assertEquals(HomeUiState.Success(featured = null, rails = emptyList()), viewModel.uiState.value)
        assertEquals(AnalyticsEvent.Types.SCREEN_VIEW, analytics.events.single().name)
    }

    @Test
    fun `emits error when the feed fails`() = runTest {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(repository { error("offline") }), analytics)

        assertTrue(viewModel.uiState.value is HomeUiState.Error)
    }

    private fun repository(load: () -> HomeFeed) = object : CatalogRepository {
        override suspend fun getHomeFeed(): HomeFeed = load()
    }

    private class RecordingAnalytics : AnalyticsTracker {
        val events = mutableListOf<AnalyticsEvent>()
        override fun track(event: AnalyticsEvent) {
            events += event
        }
    }
}
