package com.cineverse.core.domain.repository

import com.cineverse.core.domain.model.HomeFeed

interface CatalogRepository {
    suspend fun getHomeFeed(): HomeFeed
}
