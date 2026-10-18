# CineVerse

Android OTT app (VOD, AVOD, live HLS) for phones, tablets and Android TV / Fire TV, built from one
codebase with Clean Architecture modules, Hilt, Compose and Jetpack Media3 (no third-party player SDKs).

## Modules

```
app/
  mobile                 Phone/tablet app: MainActivity, navigation, Material 3 theme
  tv                     Android TV / Fire TV app: Leanback launcher, D-pad UI
core/
  domain                 Pure Kotlin: models, repository interfaces, use cases
  data                   Repository implementations (network + datastore)
  network                Retrofit/OkHttp; debug builds serve a mock catalog
  datastore              Preferences DataStore
  common                 Dispatchers, extensions, constants
  analytics              AnalyticsTracker interface + Logcat implementation
  notifications          FCM, notification channels
  player                 Player engine (no UI): Media3 ExoPlayer, IMA ads, Cast, MediaSession
  player-ui-mobile       Compose player UI for phones, tablets and foldables
feature/home/
  presentation           HomeViewModel shared by mobile and TV
  ui                     Home screens (mobile and TV)
build-logic/             Convention plugins and module dependency rules
```

`build-logic` enforces the module rules at configuration time: features never depend on each
other or on `:core:data`/`:core:network`/`:core:datastore`; `:core:domain` stays pure Kotlin;
nothing depends on `:app:*`. A violation fails the build with an explanation.

## Player

### `:core:player` (engine, no UI)

- `PlayerController`: the API every player UI uses.
  - `state: StateFlow<PlayerState>`: playing, position, duration, buffered position, live/DVR,
    ad playback, video qualities, audio and subtitle tracks (with selection), speed, video size,
    casting, errors.
  - Commands: `load`, `play`, `pause`, `seekTo`, `seekForward`, `seekBack`, `setQuality`,
    `setAudioTrack`, `setSubtitleTrack`, `setSpeed`, `goToLive`, `retry`.
- `CineVersePlayer`: the Media3 implementation.
  - ExoPlayer with HLS and DASH, audio focus, pause on "becoming noisy" and a network wake lock.
  - IMA client-side ads (`media3-exoplayer-ima`) whenever `PlaybackRequest.adTagUrl` is set.
  - `CastPlayer` wrapping the local player when Cast is available. Media3 1.11 moves the media
    item and position between the phone and the Cast device in both directions.
  - A `MediaSession` published by `PlaybackService`: notification, lock screen, headset buttons.
- `PlayerViewModel`: owns one controller per screen, so configuration changes, fullscreen and
  PiP never recreate the player.
- `TrackLabels`: human-readable track names ("English (5.1)", "French (forced)"), shared by all UIs.
- Hooks, bound through Hilt sets (`@Binds @IntoSet`; both sets are empty by default):
  - `PlaybackAnalyticsListener`: start, buffering, quality change, error, progress every 15 s.
  - `ProgressReporter`: watch progress for your backend.

### `:core:player-ui-mobile`

Fully custom Compose controls; Media3's `PlayerView` controller is not used.

- Inline and fullscreen modes; immersive fullscreen; rotation that respects the system
  rotation lock.
- Window size classes: on tablets and unfolded foldables, landscape stays inline and settings
  open as a side panel. On phones, settings are a bottom sheet in portrait and a side panel in
  landscape.
- Seek bar with buffered progress; double-tap left/right to seek; LIVE badge, DVR seeking and
  "Go to live".
- Settings: quality, audio, subtitles and speed. Each applies to the running player without
  restarting playback.
- Picture-in-Picture: auto-enter on Android 12+, manual enter on older versions, aspect ratio
  from the video, play/pause remote action. All controls are hidden in PiP.
- Accessibility: TalkBack labels and state descriptions, 48 dp touch targets, no auto-hide while
  TalkBack is on, and subtitles styled with the system caption settings.

### Calling `PlayerScreen`

```kotlin
PlayerScreen(
    request = PlaybackRequest(
        id = "tears-of-steel",
        title = "Tears of Steel",
        streamUrl = "https://…/master.m3u8",
        type = ContentType.VOD,
        adTagUrl = null,                 // VAST/VMAP URL for AVOD
    ),
    config = PlayerUiConfig(
        seekIncrementMs = 15_000,
        controls = PlayerControlsConfig(showCast = false),
        slots = PlayerSlots(
            overlay = { if (state.positionMs in 5_000..30_000) SkipIntroButton { controller.seekTo(30_000) } },
        ),
    ),
    onBack = { navController.popBackStack() },
)
```

Requirements for the host activity:

- It is a `ComponentActivity` annotated with `@AndroidEntryPoint`.
- PiP needs `android:supportsPictureInPicture="true"` on the activity.
- The activity must handle `orientation|screenSize|smallestScreenSize|screenLayout` config
  changes, so rotation and PiP don't recreate the screen.
- Call `CastSupport.initialize(this)` in `Application.onCreate()`. It is a safe no-op without
  Google Play services.

Pass your own `PlayerController` to the second `PlayerScreen` overload if you don't want the
Hilt `PlayerViewModel`.

### Adding `:core:player-ui-tv`

1. Create the module with the `cineverse.android.library.compose` plugin, depending on
   `projects.core.player` and `androidx.tv:tv-material`.
2. Get the controller from `hiltViewModel<PlayerViewModel>().controller`, call
   `controller.load(request)`, and render `controller.player` with `ContentFrame` from
   `media3-ui-compose`.
3. Build D-pad controls from `controller.state` (focusable TV Material buttons, a seek bar that
   handles left/right keys). Reuse `TrackLabels` for the settings lists. Host
   `controller.adViewContainer` for IMA ads.
4. Replace `app/tv`'s interim `TvPlayerScreen` (Media3 `PlayerView`) with the new screen.

`:core:player` needs no changes; the TV app already uses it today.

## Sample content

Debug builds read the catalog from `MockCatalogInterceptor`, which uses public test streams:

| Rail | Title | What it exercises |
|---|---|---|
| Movies (VOD) | Angel One | 5 audio languages, 4 subtitle languages, 5 qualities |
| | Tears of Steel | English and Italian audio |
| | Big Buck Bunny | Up to 1080p |
| Free with ads (AVOD) | Big Buck Bunny (with ads) | IMA VMAP: pre-roll, mid-roll at 0:15, post-roll |
| | BipBop (with ads) | IMA VAST pre-roll, 4 subtitle languages |
| | CineVerse Live | Live HLS with a DVR window |
| Clips | BipBop 4:3, BipBop fMP4 | Short-form, closed captions |

## Known limitations

- **Cast handoff is untested on hardware.** Media3 1.11's `CastPlayer` builder handles the
  transfer, but no Cast device was available to verify it.
- **No ads while casting.** The Default Media Receiver doesn't run IMA. Use a custom receiver
  with server-side ad insertion if you need ads on Cast.
- **PiP rendering on the arm64 16 KB page-size emulator.** `SurfaceView` frames came out
  corrupted inside PiP there, while `TextureView` rendered correctly. `SurfaceView` stays the
  default because secure (Widevine L1) output requires it and it uses less power.
  `PlayerUiConfig(useTextureView = true)` is available if a device shows the problem.
- **Push on Fire TV.** FCM needs Google Play services; Fire TV needs Amazon Device Messaging.
- **Not implemented yet:** thumbnail seek previews (`thumbnailsVttUrl`) and DRM license
  renewal. `DrmConfig` is wired into the media item, but no DRM content has been tested.
