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
          "featured_id": "angel-one",
          "rails": [
            {
              "id": "movies",
              "title": "Movies (VOD)",
              "items": [
                {
                  "id": "angel-one",
                  "title": "Angel One",
                  "subtitle": "Multi-language · 5 audio tracks, 4 subtitle languages",
                  "description": "Shaka Player test asset with German, English, Spanish, French and Italian audio plus Greek, English, French and Brazilian Portuguese subtitles.",
                  "poster_url": "https://picsum.photos/seed/angel-one/400/600",
                  "backdrop_url": "https://picsum.photos/seed/angel-one/1280/720",
                  "stream_url": "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
                  "type": "vod",
                  "access": "svod"
                },
                {
                  "id": "tears-of-steel",
                  "title": "Tears of Steel",
                  "subtitle": "2012 · Sci-fi · English and Italian audio",
                  "description": "Sci-fi short film from the Blender Foundation.",
                  "poster_url": "https://picsum.photos/seed/tears-of-steel/400/600",
                  "backdrop_url": "https://picsum.photos/seed/tears-of-steel/1280/720",
                  "stream_url": "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel-multi-lang.ism/.m3u8",
                  "type": "vod",
                  "access": "svod"
                },
                {
                  "id": "big-buck-bunny",
                  "title": "Big Buck Bunny",
                  "subtitle": "2008 · Animation · Up to 1080p",
                  "description": "Animated short film from the Blender Foundation.",
                  "poster_url": "https://picsum.photos/seed/big-buck-bunny/400/600",
                  "backdrop_url": "https://picsum.photos/seed/big-buck-bunny/1280/720",
                  "stream_url": "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                  "type": "vod",
                  "access": "svod"
                }
              ]
            },
            {
              "id": "free",
              "title": "Free with ads (AVOD)",
              "items": [
                {
                  "id": "big-buck-bunny-ads",
                  "title": "Big Buck Bunny (with ads)",
                  "subtitle": "Pre-roll, mid-roll at 0:15 and post-roll",
                  "description": "The same film, monetized with a sample IMA VMAP ad schedule.",
                  "poster_url": "https://picsum.photos/seed/big-buck-bunny-ads/400/600",
                  "backdrop_url": "https://picsum.photos/seed/big-buck-bunny-ads/1280/720",
                  "stream_url": "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                  "type": "vod",
                  "access": "avod",
                  "ad_tag_url": "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
                },
                {
                  "id": "bipbop-ads",
                  "title": "BipBop (with ads)",
                  "subtitle": "Single pre-roll · 4 subtitle languages",
                  "description": "Apple's HLS sample with a sample IMA VAST pre-roll.",
                  "poster_url": "https://picsum.photos/seed/bipbop-ads/400/600",
                  "backdrop_url": "https://picsum.photos/seed/bipbop-ads/1280/720",
                  "stream_url": "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
                  "type": "vod",
                  "access": "avod",
                  "ad_tag_url": "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
                },
                {
                  "id": "cineverse-live",
                  "title": "CineVerse Live",
                  "subtitle": "Live channel with DVR",
                  "description": "24/7 live test channel from Unified Streaming.",
                  "poster_url": "https://picsum.photos/seed/cineverse-live/400/600",
                  "backdrop_url": "https://picsum.photos/seed/cineverse-live/1280/720",
                  "stream_url": "https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8",
                  "type": "live",
                  "access": "avod"
                }
              ]
            },
            {
              "id": "clips",
              "title": "Clips",
              "items": [
                {
                  "id": "bipbop-4x3",
                  "title": "BipBop 4:3",
                  "subtitle": "Clip · Apple HLS sample",
                  "description": "Basic 4:3 adaptive HLS stream.",
                  "poster_url": "https://picsum.photos/seed/bipbop-4x3/400/600",
                  "backdrop_url": "https://picsum.photos/seed/bipbop-4x3/1280/720",
                  "stream_url": "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
                  "type": "clip",
                  "access": "avod"
                },
                {
                  "id": "bipbop-fmp4",
                  "title": "BipBop fMP4",
                  "subtitle": "Clip · Apple advanced HLS sample",
                  "description": "fMP4 segments with closed captions and WebVTT subtitles.",
                  "poster_url": "https://picsum.photos/seed/bipbop-fmp4/400/600",
                  "backdrop_url": "https://picsum.photos/seed/bipbop-fmp4/1280/720",
                  "stream_url": "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                  "type": "clip",
                  "access": "avod"
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
}
