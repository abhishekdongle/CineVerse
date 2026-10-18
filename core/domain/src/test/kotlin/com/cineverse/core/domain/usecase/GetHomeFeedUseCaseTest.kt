package com.cineverse.core.domain.usecase

import com.cineverse.core.domain.model.HomeFeed
import com.cineverse.core.domain.model.Rail
import com.cineverse.core.domain.repository.CatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetHomeFeedUseCaseTest {

    @Test
    fun `drops empty rails`() = runTest {
        val feed = HomeFeed(
            featured = null,
            rails = listOf(Rail("empty", "Empty", emptyList())),
        )
        val result = GetHomeFeedUseCase(FakeCatalogRepository { feed })()
        assertEquals(emptyList<Rail>(), result.getOrThrow().rails)
    }

    @Test
    fun `wraps repository errors in a failed result`() = runTest {
        val result = GetHomeFeedUseCase(FakeCatalogRepository { throw IOException("offline") })()
        assertTrue(result.exceptionOrNull() is IOException)
    }

    private class FakeCatalogRepository(private val load: () -> HomeFeed) : CatalogRepository {
        override suspend fun getHomeFeed(): HomeFeed = load()
    }
}
