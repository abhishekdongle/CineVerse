package com.cineverse.core.data.di

import com.cineverse.core.data.repository.DefaultCatalogRepository
import com.cineverse.core.data.repository.DefaultSettingsRepository
import com.cineverse.core.domain.repository.CatalogRepository
import com.cineverse.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsCatalogRepository(impl: DefaultCatalogRepository): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindsSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository
}
