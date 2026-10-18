package com.cineverse.core.network.api

import com.cineverse.core.network.model.HomeFeedDto
import retrofit2.http.GET

interface CatalogApi {
    @GET("v1/home")
    suspend fun getHomeFeed(): HomeFeedDto
}
