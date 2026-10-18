# CineVerse

Android OTT app (VOD, AVOD, live HLS) for phones, tablets and Android TV / Fire TV, built from one
codebase with Clean Architecture modules, Hilt, Compose and Jetpack Media3 (no third-party player SDKs).

## Modules

```
app/
  mobile                 Phone/tablet app: MainActivity, navigation graph, Material 3 theme
  tv                     Android TV / Fire TV app: Leanback launcher, D-pad UI
core/
  domain                 Pure Kotlin: models, repository interfaces, use cases
  data                   Repository implementations (network + datastore)
  network                Retrofit/OkHttp; debug builds serve a mock catalog
  datastore              Preferences DataStore
  common                 Dispatchers, extensions, constants
  analytics              AnalyticsTracker interface + Logcat implementation
  notifications          FCM, notification channels
  player                 Playback library (no screens): Media3 ExoPlayer, IMA ads, Cast, MediaSession
feature/home/
  presentation           HomeViewModel shared by mobile and TV
  ui                     Home screens (mobile and TV)
feature/videoplayer/
  presentation           VideoPlayerViewModel, VideoPlayerUiState, VideoPlayerAction (no Compose)
  ui                     VideoPlayerScreen (Compose), TV player screen, navigation route
build-logic/             Convention plugins and module dependency rules
```

### Video player modules

```
:app:mobile / :app:tv
        │  navigation graph only (videoPlayerScreen / tvVideoPlayerScreen)
        ▼
:feature:videoplayer:ui            Compose UI, window work (fullscreen, rotation, immersive, PiP)
        │  api
        ▼
:feature:videoplayer:presentation  ViewModel, UiState, actions — plain unit tests, no Compose
        │  api
        ▼
:core:player                       PlayerController + StateFlow<PlayerState>; no UI, no ViewModels
```

Dependency rules, enforced by `build-logic` at configuration time (a violation fails the build
with an explanation):

- One direction only: `ui → presentation → :core:player`. Presentation never depends on ui.
- `:core:player` depends on no feature module (core modules never do; in practice it depends on
  no project module at all).
- Presentation modules may not apply the Compose compiler or depend on any `androidx.compose`
  artifact.
- Apps depend on a feature's `:ui` module, never directly on its `:presentation` module.
  `:app:mobile` also declares `:core:player`, only for DI setup: `CastSupport.initialize` and binding
  `PlaybackAnalyticsListener`.
- Features never depend on each other or on `:core:data`/`:core:network`/`:core:datastore`;
  `:core:domain` stays pure Kotlin; nothing depends on `:app:*`.

## Player

### `:core:player` (library, no UI)

- `PlayerController`: the API every feature uses.
  - `state: StateFlow<PlayerState>`: playing, position, duration, buffered position, live/DVR,
    ad playback, video qualities, audio and subtitle tracks (with selection), speed, video size,
    casting, errors.
  - Commands: `load`, `play`, `pause`, `seekTo`, `seekForward`, `seekBack`, `setQuality`,
    `setAudioTrack`, `setSubtitleTrack`, `setSpeed`, `goToLive`, `retry`, `release`.
- `PlayerControllerFactory`: inject it (Hilt) to create one controller per screen. The Media3
  implementation behind it is internal:
  - ExoPlayer with HLS and DASH, audio focus, pause on "becoming noisy" and a network wake lock.
  - IMA client-side ads (`media3-exoplayer-ima`) whenever `PlaybackRequest.adTagUrl` is set.
  - `CastPlayer` wrapping the local player when Cast is available. Media3 1.11 moves the media
    item and position between the phone and the Cast device in both directions.
  - A `MediaSession` published by `PlaybackService`: notification, lock screen, headset buttons.
- `PlaybackRequest`: what to play. Used by the feature and by the apps.
- `TrackLabels`: human-readable track names ("English (5.1)", "French (forced)").
- Hooks, bound through Hilt sets (`@Binds @IntoSet`; both sets are empty by default):
  - `PlaybackAnalyticsListener`: start, buffering, quality change, error, progress every 15 s.
    Called by the player.
  - `ProgressReporter`: watch progress for your backend, every 15 s and when playback pauses,
    ends or the screen closes. Called by `VideoPlayerViewModel`.

### `:feature:videoplayer:presentation`

- `VideoPlayerViewModel` owns the screen's `PlayerController`, so rotation, fullscreen, resizing
  and PiP never recreate the player. It exposes `uiState: StateFlow<VideoPlayerUiState>` and
  takes every input through `onAction(VideoPlayerAction)`.
- It handles control visibility and the auto-hide timer, the settings panel (open, page,
  option lists), fullscreen (phones go fullscreen in landscape; large screens only on request),
  PiP flags, double-tap seek feedback, errors and retry, and progress reporting.
- Tested with plain JVM unit tests (`VideoPlayerViewModelTest`) against `RecordingPlayerController`, a test double that records commands.

### `:feature:videoplayer:ui`

Fully custom Compose controls; Media3's `PlayerView` controller is not used on mobile. The UI
only renders `VideoPlayerUiState` and sends actions; the window work that needs an Activity lives
here.

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
- `TvVideoPlayerScreen`: interim TV screen on the same ViewModel, using Media3's D-pad-ready
  `PlayerView`.

### Using the player from an app

```kotlin
NavHost(navController, startDestination = HomeRoute) {
    composable<HomeRoute> {
        HomeMobileScreen(onPlay = { item -> navController.navigateToVideoPlayer(item.toPlaybackRequest()) })
    }
    videoPlayerScreen(onBack = { navController.popBackStack() })   // tvVideoPlayerScreen() on TV
}
```

Or call the screen directly, optionally with a config:

```kotlin
VideoPlayerScreen(
    request = PlaybackRequest(
        id = "tears-of-steel",
        title = "Tears of Steel",
        streamUrl = "https://…/master.m3u8",
        type = ContentType.VOD,
        adTagUrl = null,                 // VAST/VMAP URL for AVOD
    ),
    onBack = { navController.popBackStack() },
    config = PlayerUiConfig(
        seekIncrementMs = 15_000,
        controls = PlayerControlsConfig(showCast = false),
        slots = PlayerSlots(
            overlay = {
                if (uiState.positionMs in 5_000..30_000) {
                    SkipIntroButton { onAction(VideoPlayerAction.SeekTo(30_000)) }
                }
            },
        ),
    ),
)
```

Requirements for the host activity:

- It is a `ComponentActivity` annotated with `@AndroidEntryPoint`.
- PiP needs `android:supportsPictureInPicture="true"` on the activity.
- The activity must handle `orientation|screenSize|smallestScreenSize|screenLayout` config
  changes, so rotation and PiP don't recreate the screen.
- Call `CastSupport.initialize(this)` in `Application.onCreate()`. It is a safe no-op without
  Google Play services.

### Replacing the interim TV player

Add Compose for TV controls in `:feature:videoplayer:ui` (next to `tv/TvVideoPlayerScreen.kt`)
that render `VideoPlayerUiState`: focusable TV Material buttons and a seek bar that handles
left/right keys, sending the same `VideoPlayerAction`s. Host `viewModel.adViewContainer` for IMA
ads. Neither `:core:player` nor the presentation module changes.

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
