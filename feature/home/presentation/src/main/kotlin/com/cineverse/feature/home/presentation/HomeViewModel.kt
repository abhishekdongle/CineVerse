package com.cineverse.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineverse.core.analytics.AnalyticsEvent
import com.cineverse.core.analytics.AnalyticsTracker
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.domain.usecase.GetHomeFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared by the mobile and TV home screens. It knows nothing about layouts, focus or touch;
 * the screens report user intent through [onItemSelected] and [retry].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        analytics.track(AnalyticsEvent.screenView(SCREEN_NAME))
        load()
    }

    fun retry() = load()

    fun onItemSelected(item: MediaItem) {
        analytics.track(AnalyticsEvent.contentSelected(item.id, source = SCREEN_NAME))
    }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = getHomeFeed().fold(
                onSuccess = { HomeUiState.Success(featured = it.featured, rails = it.rails) },
                onFailure = { HomeUiState.Error(it.message) },
            )
        }
    }

    private companion object {
        const val SCREEN_NAME = "home"
    }
}
