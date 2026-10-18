package com.cineverse.core.network.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Serves a canned home feed so the apps run end to end before the backend exists.
 * The streams are public HLS test streams. Installed only when BuildConfig.USE_MOCK_API is true.
 */
internal class MockCatalogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath != "/v1/home") return chain.proceed(request)
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(HOME_FEED_JSON.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private companion object {
        val HOME_FEED_JSON = """
        {
          "featured_id": "tears-of-steel",
          "rails": [
            {
              "id": "live",
              "title": "Live now",
              "items": [
                {
                  "id": "cineverse-live",
                  "title": "CineVerse Live",
                  "description": "24/7 live test channel.",
                  "poster_url": "https://picsum.photos/seed/cineverse-live/400/600",
                  "backdrop_url": "https://picsum.photos/seed/cineverse-live/1280/720",
                  "stream_url": "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8",
                  "type": "live",
                  "access": "avod"
                }
              ]
            },
            {
              "id": "trending",
              "title": "Trending",
              "items": [
                {
                  "id": "tears-of-steel",
                  "title": "Tears of Steel",
                  "description": "Sci-fi short film from the Blender Foundation.",
                  "poster_url": "https://picsum.photos/seed/tears-of-steel/400/600",
                  "backdrop_url": "https://picsum.photos/seed/tears-of-steel/1280/720",
                  "stream_url": "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                  "type": "vod",
                  "access": "svod"
                },
                {
                  "id": "big-buck-bunny",
                  "title": "Big Buck Bunny",
                  "description": "Animated short film from the Blender Foundation.",
                  "poster_url": "https://picsum.photos/seed/big-buck-bunny/400/600",
                  "backdrop_url": "https://picsum.photos/seed/big-buck-bunny/1280/720",
                  "stream_url": "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                  "type": "vod",
                  "access": "avod"
                },
                {
                  "id": "bipbop",
                  "title": "BipBop",
                  "description": "Apple's adaptive bitrate HLS sample.",
                  "poster_url": "https://picsum.photos/seed/bipbop/400/600",
                  "backdrop_url": "https://picsum.photos/seed/bipbop/1280/720",
                  "stream_url": "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                  "type": "vod",
                  "access": "avod"
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
}
