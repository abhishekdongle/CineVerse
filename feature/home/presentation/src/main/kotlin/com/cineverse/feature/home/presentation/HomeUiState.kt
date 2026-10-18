package com.cineverse.feature.home.presentation

import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.domain.model.Rail

/** Platform-neutral home state. Mobile and TV render it differently. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val featured: MediaItem?,
        val rails: List<Rail>,
    ) : HomeUiState

    data class Error(val message: String?) : HomeUiState
}
