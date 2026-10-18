package com.cineverse.core.domain.usecase

import com.cineverse.core.domain.model.HomeFeed
import com.cineverse.core.domain.repository.CatalogRepository
import com.cineverse.core.domain.util.suspendRunCatching
import javax.inject.Inject

class GetHomeFeedUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository,
) {
    /** Loads the home feed, dropping empty rails so neither platform renders an empty row. */
    suspend operator fun invoke(): Result<HomeFeed> = suspendRunCatching {
        val feed = catalogRepository.getHomeFeed()
        feed.copy(rails = feed.rails.filter { it.items.isNotEmpty() })
    }
}
