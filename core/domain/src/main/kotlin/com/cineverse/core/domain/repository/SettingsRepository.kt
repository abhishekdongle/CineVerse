package com.cineverse.core.domain.repository

import com.cineverse.core.domain.model.PlaybackSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val playbackSettings: Flow<PlaybackSettings>

    suspend fun setAutoplayNextEpisode(enabled: Boolean)

    suspend fun setDataSaver(enabled: Boolean)
}
