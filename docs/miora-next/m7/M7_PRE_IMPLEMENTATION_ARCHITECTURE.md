# MIORA NEXT — M7 Pre-Implementation Architecture Specification
## In-App Streaming Engine, Media3 Lifecycle & Unified Playback Integration

- **Document Version:** 1.0.0
- **Gate:** Pre-Implementation Architectural Freeze (Milestone M7)
- **Target Branch:** `docs/miora-next-audit-roadmap`
- **Application ID:** `com.fhf.miora`
- **Code Namespace:** `com.fahim.powerampclone`
- **Media3 Dependency Baseline:** AndroidX Media3 `1.4.1` (Verified from `gradle/libs.versions.toml`)

---

## 1. Roadmap Freeze & Architectural Philosophy

### 1.1 Preserved Milestone Sequence
The milestone structure established in `docs/miora-next/audit/MIORA_NEXT_PHASE_ROADMAP.md` is strictly frozen:
- **M0–M6 (Foundation):** GPL-3.0 relicensing, decoupled domain models, SQLite schema v3, YouTube search & caching, OAuth 2.0 PKCE with Keystore encryption, LRCLIB synced lyrics, and 197 verified unit tests.
- **M7 (Current Gate):** Integrated In-App Online Streaming Engine (Resolving MediaSource, MediaLibraryService lifecycle, stream resolution abstraction).
- **M8 (Next Phase):** Unified Queue, Multi-Tier Offline Cache and Download Engine.
- **M9 (Final Polish):** Audio Experience (Equalizer/Normalization), UI Polish, CI/CD, and v1.0.0 Release.

### 1.2 Core Architectural Principles
1. **Local Mode Primacy:** Local audio playback from MediaStore and device storage is the core foundation of MIORA. Under no circumstances may online streaming additions degrade, block, slow down, or introduce network dependencies into local playback.
2. **Provider Agnosticism:** The playback engine (`ExoPlayer`, `PlaybackService`, `AudioEngine`) must remain completely unaware of specific remote network APIs or streaming protocols. It interacts exclusively with standard AndroidX Media3 data sources and an abstract `StreamResolver` contract.
3. **Graceful Degrade & Transparency:** If an online stream cannot be resolved or authorized, the player reports an explicit typed failure through `StateFlow<PlaybackError>` rather than silently falling back to external applications or mock audio.

---

## 2. Media Playback Subsystem Architecture (AndroidX Media3 1.4.1)

### 2.1 Verification of Dependency Compatibility
Inspection of `gradle/libs.versions.toml` confirms:
```toml
media3 = "1.4.1"
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
androidx-media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
```
**Conclusion:** Media3 `1.4.1` fully supports `MediaLibraryService`, `ExoPlayer`, `ResolvingDataSource`, custom `AudioProcessor`, and `CacheDataSource`. **No dependency bump is required for M7**, avoiding potential ABI incompatibilities or Gradle sync failures.

---

### 2.2 System Component Topology

```
+-----------------------------------------------------------------------------------+
|                            UI Layer (Jetpack Compose)                             |
|    PlayerScreen / MiniPlayer / QueueScreen / Lockscreen / Android Auto UI        |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|               PlaybackManager (StateFlow<PlayerUiState>, Queue, Scope)            |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|         PlaybackService (androidx.media3.session.MediaLibraryService)             |
|   - Holds MediaLibrarySession + ForwardingPlayer bridge                           |
|   - Manages Foreground Service lifecycle (FOREGROUND_SERVICE_MEDIA_PLAYBACK)      |
|   - Dispatches MediaMetadata to Lockscreen, Notification, Auto, & App Widgets     |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        Media3AudioEngine (AudioEngine)                            |
|   - Owns ExoPlayer instance configured with AudioAttributes                       |
|   - ResolvingDataSource.Factory intercepts data requests                          |
|   - VolumeNormalizationAudioProcessor + EqualizerController                      |
+-----------------------------------------------------------------------------------+
                         /                                    \
                        /                                      \
                       v                                        v
+-------------------------------+              +------------------------------------+
|    Local File DataSource      |              |   ResolvingDataSource.Factory      |
|   - content:// and file://    |              |   - URI Scheme: miora-stream://    |
|   - Direct MediaStore / SAF   |              |   - Calls StreamResolverChain      |
|   - Zero Network Overhead     |              |   - Resolves to HTTP/HTTPS Stream  |
+-------------------------------+              +------------------------------------+
                                                                |
                                                                v
                                               +------------------------------------+
                                               |    StreamResolverChain (Abstract)  |
                                               |   - Resolves direct audio URL      |
                                               |   - Injects required headers       |
                                               |   - Checks expiry & refreshes      |
                                               +------------------------------------+
```

---

### 2.3 Polymorphic Media Identity Architecture

To eliminate the friction between local tracks (`Track`, numeric ID `Long`) and online tracks (`OnlineTrack`, string ID `String`), M7 establishes a unified polymorphic representation at the engine level:

```kotlin
package com.fahim.powerampclone.playback.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Stable, globally unique identifier for any playable media in MIORA.
 */
sealed interface PlayableMediaId {
    val rawId: String

    data class Local(val trackId: Long) : PlayableMediaId {
        override val rawId: String = "local:$trackId"
    }

    data class Online(val providerId: String, val trackId: String) : PlayableMediaId {
        override val rawId: String = "online:$providerId:$trackId"
    }

    companion object {
        fun parse(raw: String): PlayableMediaId {
            return when {
                raw.startsWith("local:") -> Local(raw.removePrefix("local:").toLong())
                raw.startsWith("online:") -> {
                    val parts = raw.removePrefix("online:").split(":", limit = 2)
                    Online(providerId = parts[0], trackId = parts.getOrElse(1) { "" })
                }
                else -> raw.toLongOrNull()?.let { Local(it) } ?: Online("unknown", raw)
            }
        }
    }
}

/**
 * Engine-level playable item encompassing both local files and online streams.
 */
data class PlayableItem(
    val mediaId: PlayableMediaId,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val artworkUri: String?,
    val playbackUri: Uri,
    val isOnline: Boolean,
    val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val expiryTimestampMs: Long? = null,
) {
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply {
                artworkUri?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId.rawId)
            .setUri(playbackUri)
            .setMediaMetadata(metadata)
            .apply {
                mimeType?.let { setMimeType(it) }
            }
            .build()
    }
}
```

---

## 3. ResolvingDataSource & Stream Resolution Pipeline

### 3.1 Custom URI Scheme: `miora-stream://`
Instead of resolving remote URLs upfront when constructing the queue (which causes race conditions, expired URLs, and unnecessary network requests for unplayed tracks), online tracks enter the ExoPlayer queue with a deferred URI scheme:
```
miora-stream://{providerId}/{trackId}?quality={quality}
```

### 3.2 ResolvingDataSource Implementation
ExoPlayer's data source factory is configured with `ResolvingDataSource.Factory`:

```kotlin
class MioraDataSourceFactory(
    private val context: Context,
    private val streamResolverChain: StreamResolverChain,
    private val baseHttpDataSourceFactory: DataSource.Factory,
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        val defaultDataSource = DefaultDataSource.Factory(context, baseHttpDataSourceFactory)
            .createDataSource()

        return ResolvingDataSource(defaultDataSource, object : ResolvingDataSource.Resolver {
            override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                val uri = dataSpec.uri
                
                // 1. Pass-through for local content:// and file:// schemes
                if (uri.scheme == "content" || uri.scheme == "file") {
                    return dataSpec
                }

                // 2. Intercept miora-stream:// scheme
                if (uri.scheme == "miora-stream") {
                    val mediaId = PlayableMediaId.parse(dataSpec.key ?: uri.toString())
                    if (mediaId !is PlayableMediaId.Online) {
                        throw PlaybackException(
                            "Invalid media ID for stream resolver: $mediaId",
                            null,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                        )
                    }

                    // Synchronously resolve via cached or active provider
                    val resolvedStream = streamResolverChain.resolve(mediaId)
                        ?: throw PlaybackException(
                            "Failed to resolve audio stream for track ${mediaId.trackId}",
                            null,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        )

                    // Inject resolved URI and necessary authentication / range headers
                    val headersBuilder = dataSpec.httpRequestHeaders.toMutableMap()
                    headersBuilder.putAll(resolvedStream.httpHeaders)

                    return dataSpec.buildUpon()
                        .setUri(Uri.parse(resolvedStream.streamUrl))
                        .setHttpRequestHeaders(headersBuilder)
                        .build()
                }

                return dataSpec
            }
        })
    }
}
```

---

## 4. Lifecycle, Audio Focus & Network Resilience

### 4.1 Audio Focus & Audio Attributes
In `Media3AudioEngine.kt`, ExoPlayer is configured with:
```kotlin
val audioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .build()

player = ExoPlayer.Builder(context)
    .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
    .setHandleAudioBecomingNoisy(true)
    .setWakeMode(C.WAKE_MODE_NETWORK) // Holds partial wake lock + wifi lock for streaming
    .build()
```
- **Transient Focus Loss (e.g. Navigation prompt):** ExoPlayer automatically ducks volume without interrupting streaming buffer.
- **Audio Becoming Noisy (Headphones disconnected):** ExoPlayer automatically pauses playback, updates `PlayerUiState.isPlaying = false`, and notifies UI.
- **Permanent Focus Loss (Incoming phone call):** Pauses playback and releases audio focus cleanly.

### 4.2 Network Resilience & Stream Expiry
1. **Network Callback Monitoring:** A dedicated `NetworkConnectivityObserver` registers with Android `ConnectivityManager`. When connectivity drops:
   - If playing a local track: Playback continues uninterrupted (zero network dependency).
   - If playing an online track: ExoPlayer continues playing cached buffer (~30-60s). If the buffer empties before network re-establishes, the engine transitions to `STATE_BUFFERING` with a 15-second grace timer before emitting a user-facing retry prompt.
2. **Proactive Stream Expiry Renewal:**
   - Online audio stream URLs typically embed an expiration timestamp (e.g. `expire=1731420000`).
   - `StreamResolverChain` stores an `expiryTimestampMs` for each resolved URL.
   - When ExoPlayer prepares the next track or loops, `StreamResolverChain` verifies whether `currentTime + 60_000ms >= expiryTimestampMs`. If expired, it triggers a proactive URL refresh before delivering the stream URL to ExoPlayer.

### 4.3 Background Playback & Android 14+ Foreground Service
- `PlaybackService` is registered as a `foregroundServiceType="mediaPlayback"` in `AndroidManifest.xml`.
- When playback starts, `PlaybackService` transitions to foreground service using `DefaultMediaNotificationProvider` with proper `PendingIntent` and channel setup.
- Complies strictly with Android 14 (API 34) and Android 15 (API 35) requirements by guaranteeing that `startForeground()` is called within the mandatory timeout and never throws `ForegroundServiceStartNotAllowedException`.

---

## 5. Queue Transition Architecture

To allow seamless transitions between local files and online streams:
1. `PlaybackManager` maintains the queue as `List<PlayableItem>`.
2. When transitioning from Track A (Local FLAC, 44.1kHz / 16-bit) to Track B (Online Stream, 48kHz / Opus):
   - ExoPlayer's internal decoder graph dynamically re-configures the audio sink without tearing down the player instance.
   - The notification and `MediaMetadata` update atomically via `ForwardingPlayer.Listener`.
   - `AudioSessionId` remains stable, preventing equalizers or DSP audio effects from resetting between tracks.
