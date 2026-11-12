# MIORA NEXT — M7 Test Architecture & Acceptance Verification Plan
## Non-Regression Guarantees, Provider Mocking Suites & Exit Criteria

- **Document Version:** 1.0.0
- **Gate:** Pre-Implementation Test Gate (Milestone M7)
- **Target Branch:** `docs/miora-next-audit-roadmap`

---

## 1. Non-Regression Verification of Existing Foundation

Before any M7 code is written or merged, the existing core player features and test suites must be continuously protected against regressions.

### 1.1 Existing Feature Non-Regression Matrix

| Subsystem / Feature | Key Classes / Components | Regression Risk | Automated Verification Method |
| :--- | :--- | :---: | :--- |
| **Local Audio Playback** | `Media3AudioEngine.kt`, `PlaybackManager.kt` | **HIGH** | `Media3AudioEngineTest.kt`, `PlaybackManagerTest.kt` |
| **MediaStore Scanner & Folders** | `MediaStoreMusicRepository.kt`, `MediaScanner.kt` | **LOW** | `LocalScannerRegressionTest.kt` |
| **Playlists & SQLite Storage** | `PlaylistManager.kt`, `LocalDatabaseHelper.kt` | **LOW** | `PlaylistPersistenceTest.kt`, Schema v3 tests |
| **Equalizer & DSP AudioFX** | `EqualizerController.kt`, `DspSettingsStore.kt` | **MEDIUM** | `EqualizerSessionBindingTest.kt` |
| **Synchronized Lyrics (LRCLIB)**| `LrcLibLyricsClient.kt`, `LyricsParser.kt` | **LOW** | `PhaseM5LyricsAndPlaybackTest.kt` |
| **System Notification & Auto** | `PlaybackService.kt`, `DefaultMediaNotificationProvider` | **MEDIUM** | `ForegroundServiceStartupRegressionTest.kt` |
| **Home Screen App Widget** | `MioraAppWidgetProvider.kt` | **LOW** | `AppWidgetStateSyncTest.kt` |
| **OAuth 2.0 PKCE & Keystore** | `OAuthManager.kt`, `KeystoreCryptoManager.kt` | **LOW** | `OAuthPKCETest.kt`, `KeystoreCryptoManagerTest.kt` |
| **Full Unit Test Inventory** | 197 existing unit tests across 26 test suites | **HIGH** | Full headless test pass (`testDebugUnitTest`) |

---

## 2. Milestone M7 Specific Test Suite Specification

M7 introduces ~40 new automated unit and integration tests across 5 specialized test suites:

### Suite 1: `ProviderContractAndRegistryTest.kt` (~10 tests)
- **Objective:** Verify that `OnlineMediaProvider` interface implementations register correctly, negotiate capabilities accurately, and handle unknown provider queries without crashing.
- **Key Test Cases:**
  1. `testProviderRegistrationAndCapabilityQuery()`: Verifies that providers declare correct sets of `ProviderCapability`.
  2. `testUnsupportedCapabilityThrowsGracefulException()`: Ensures calling `resolveStream()` on a metadata-only provider returns `Result.failure(UnsupportedOperationException)` rather than throwing unhandled exceptions.
  3. `testProviderRegistryFallbackSelection()`: Validates that when Provider A fails, the registry evaluates Provider B.

### Suite 2: `StreamResolverChainTest.kt` (~10 tests)
- **Objective:** Test stream URL resolution, header injection, caching, and expiry logic.
- **Key Test Cases:**
  1. `testDirectLocalUriPassThrough()`: Asserts that `content://` and `file://` URIs are returned instantly without invoking network resolvers.
  2. `testMioraStreamUriResolutionSuccess()`: Resolves a simulated `miora-stream://subsonic/track-123` to a valid HTTP stream URL with auth headers.
  3. `testProactiveExpiryRefresh()`: Asserts that when a stream URL has `expiryTimestampMs <= System.currentTimeMillis() + 60_000`, the resolver triggers proactive URL renewal before playback starts.
  4. `testExpiredStreamHandlingOnLoop()`: Tests seamless URL renewal when a user loops an online stream whose URL expired during playback.

### Suite 3: `PlayableMediaIdAndItemTest.kt` (~8 tests)
- **Objective:** Verify polymorphic conversion and serialization between `PlayableMediaId`, `PlayableItem`, and AndroidX `MediaItem`.
- **Key Test Cases:**
  1. `testLocalMediaIdFormattingAndParsing()`: `PlayableMediaId.parse("local:12345") == PlayableMediaId.Local(12345L)`.
  2. `testOnlineMediaIdFormattingAndParsing()`: `PlayableMediaId.parse("online:subsonic:abc-987") == PlayableMediaId.Online("subsonic", "abc-987")`.
  3. `testMediaItemConversionIntegrity()`: Asserts that `title`, `artist`, `album`, `artworkUri`, and `mediaId` map identically into Media3 `MediaMetadata`.

### Suite 4: `Media3ResolvingDataSourceIntegrationTest.kt` (~6 tests)
- **Objective:** Test ExoPlayer's `ResolvingDataSource` pipeline with a local OkHttp `MockWebServer`.
- **Key Test Cases:**
  1. `testResolvingDataSourceStreamsHttpBytes()`: Feeds ExoPlayer a `miora-stream://mock/audio.mp3` URI, resolves it to `MockWebServer` streaming simulated audio bytes, and verifies ExoPlayer reaches `Player.STATE_READY`.
  2. `testResolvingDataSourceHttp403Translation()`: Simulates an HTTP 403 Forbidden response; verifies ExoPlayer emits `PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED` and `PlaybackManager` transitions to error state.
  3. `testResolvingDataSourceNetworkTimeout()`: Verifies socket timeout handling.

### Suite 5: `QueueTransitionAndFocusTest.kt` (~6 tests)
- **Objective:** Verify seamless queue transitions and audio focus behavior across mixed media types.
- **Key Test Cases:**
  1. `testQueueTransitionFromLocalToOnline()`: Simulates ExoPlayer advancing from a local MediaStore track to an online stream; asserts `isPlaying = true`, duration updates, and audio session ID remains stable.
  2. `testAudioBecomingNoisyPausesOnlineStream()`: Simulates `ACTION_AUDIO_BECOMING_NOISY` broadcast; asserts online playback immediately pauses.
  3. `testTransientAudioFocusDucking()`: Simulates `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`; asserts volume is lowered to 0.2 without pausing or aborting network buffer.

---

## 3. Strict Acceptance Criteria & M7 Exit Gates

Milestone M7 will be certified complete if and only if all of the following exit gates are fully satisfied:

1. **Non-Regression Gate:**
   - All 197 existing unit tests continue to pass with zero failures (`BUILD SUCCESSFUL`).
   - Local audio playback from device storage remains 100% operational with no network checks.
2. **Architecture & Contract Gate:**
   - The playback engine (`Media3AudioEngine.kt`, `PlaybackService.kt`) interacts exclusively with standard Media3 `ResolvingDataSource` and the `OnlineMediaProvider` contract.
   - Zero hardcoded vendor-specific APIs in the core audio service.
3. **In-App Streaming Gate:**
   - Online audio tracks resolved through an authorized provider stream continuously in-app via ExoPlayer without launching external apps.
   - Background audio playback functions with the device screen off.
   - Lockscreen controls, notification actions, and home screen widgets update dynamically with online track metadata and playback state.
4. **Resilience & Expiry Gate:**
   - Network interruptions trigger clear, typed UI error states rather than application crashes.
   - Expired stream URLs are refreshed proactively before audio buffer starvation.
5. **Security & Legal Compliance Gate:**
   - Zero embedded BotGuard VM scripts (`po_token.html`), QuickJS native binaries (`libquickjs.so`), or AST cipher deciphering code.
   - Zero session cookie sniffing (`SAPISID`).
   - All authentication relies on OAuth 2.0 PKCE and Android Keystore AES-256-GCM.
6. **Test Coverage Gate:**
   - Total automated unit and integration tests expand from 197 to **≥ 235 tests**, all passing 100%.
