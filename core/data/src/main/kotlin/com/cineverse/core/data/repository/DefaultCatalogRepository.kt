package com.cineverse.core.data.repository

import com.cineverse.core.common.dispatchers.CineVerseDispatchers
import com.cineverse.core.common.dispatchers.Dispatcher
import com.cineverse.core.data.mapper.toDomain
import com.cineverse.core.domain.model.HomeFeed
import com.cineverse.core.domain.repository.CatalogRepository
import com.cineverse.core.network.api.CatalogApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultCatalogRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    @Dispatcher(CineVerseDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CatalogRepository {

    override suspend fun getHomeFeed(): HomeFeed = withContext(ioDispatcher) {
        catalogApi.getHomeFeed().toDomain()
    }
}
