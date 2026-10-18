package com.cineverse.core.domain.usecase

import com.cineverse.core.domain.model.PlaybackSettings
import com.cineverse.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaybackSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<PlaybackSettings> = settingsRepository.playbackSettings
}
