# MIORA — Final Data Consistency Report: Single Source of Truth

**Date:** 2026-11-11  
**Project:** MIORA — Music in Our Real Audio  
**Target Identity:** `com.fhf.miora`  
**Target Release:** `v0.1.0` (versionCode: `3`)  
**Status:** `IMPLEMENTED & VERIFIED`

---

## 1. Context & Identified Inconsistencies

During testing on physical Android devices, discrepancies were identified in track and playlist counters across different views:
1. **Mock Data Injection in Production:** The user profile and scanner sections displayed `"Local library • 20 sample tracks"` and `"Sample library — 20 tracks indexed"`, even when no local audio existed on the device.
2. **Playlist List vs. Detail Count Divergence:** A playlist row in the library list displayed `"9 tracks"`, but opening the playlist details showed `"3 tracks • 13:33"`.
3. **Hardcoded UI Count References:** Disconnected components were calculating track counts independently or reading stale cached values.

---

## 2. Root Cause & Architectural Fixes

### 2.1 Decoupling Production Repository from Mock Fallback
In [`MediaStoreMusicRepository.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/data/local/MediaStoreMusicRepository.kt):
- **Previous behavior:** The repository constructor defaulted `fallbackRepository` to `MockMusicRepository(seedSampleData = true)`. If storage permission was pending or the device had zero music files, the repository silently served 20 sample tracks.
- **Fixed behavior:** The default fallback repository was changed to `null`:
  ```kotlin
  class MediaStoreMusicRepository(
      private val context: Context,
      private val playlistStore: PlaylistStore,
      private val fallbackRepository: MusicRepository? = null
  ) : MusicRepository
  ```
- **Result:** An empty device accurately reports 0 tracks, showing the designed clean empty states. Sample mock data is strictly restricted to test harnesses.

### 2.2 Deriving Counts from Valid Track Membership
In [`MediaStoreMusicRepository.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/data/local/MediaStoreMusicRepository.kt):
- **Issue:** Playlist objects stored lists of track IDs. If a track had been deleted from storage, the raw ID list still counted it in the playlist row summary, but the detail screen omitted it because the track could not be resolved.
- **Fix:** Both `playlists()` and `playlistById()` now resolve and validate tracks against the indexed library:
  ```kotlin
  val validTrackIds = playlist.trackIds.filter { trackById(it) != null }
  ```
  Furthermore, `PlaylistScreens.kt` calculates the track count directly using `libraryViewModel.tracksForPlaylist(playlist).size`.

### 2.3 Reactive Track Count in Settings & Scanner
In [`SettingsScreen.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/ui/SettingsScreen.kt):
- **Profile Summary:** Uses `libraryViewModel.allTracks.size` dynamically:
  ```kotlin
  val trackCount = libraryViewModel.allTracks.size
  val librarySubtitle = if (trackCount == 0) {
      "Local library • No tracks found"
  } else {
      "Local library • $trackCount ${if (trackCount == 1) "track" else "tracks"}"
  }
  ```
- **Scan Section:** Displays `"Scan for music"` with dynamic feedback from the actual repository refresh flow.

---

## 3. Data Flow Architecture

```
MediaStore (Platform Provider)
       │
       ▼
MediaStoreMusicRepository (Single Source of Truth)
       │
       ▼
LibraryViewModel (Observable StateFlow / StateList)
       ├──> Main Library Screen (Total: N tracks)
       ├──> Settings Profile (Local library • N tracks)
       ├──> Music Scanner (N tracks indexed)
       └──> Playlist Views (List & Detail both display exactly M tracks)
```

---

## 4. Regression Verification

The fix is verified by [`FinalPolishRegressionTest.kt`](file:///workspace/poweramp-clone/app/src/test/java/com/fahim/powerampclone/FinalPolishRegressionTest.kt):
- `testEmptyRepositoryProducesZeroTracksAndNoMocks`: Confirms empty repository returns `emptyList()` without sample track injection.
- `testPlaylistTrackCountConsistencySingleSourceOfTruth`: Confirms that playlist row count and playlist detail track count strictly match.
- `testInvalidTrackIdsPrunedFromPlaylistCount`: Confirms that deleted track IDs are excluded from counts across all UI surfaces.
