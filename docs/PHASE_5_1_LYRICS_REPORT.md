# Phase 5.1 — Synced Lyrics Viewer & Offset Adjuster Report

**Status:** Completed & Verified  
**Milestone:** 5.1  
**Target Release:** v0.1.0-beta.2  
**Date:** 2026-11-11  

---

## 1. Executive Summary

Milestone 5.1 implements a comprehensive synchronized lyrics system connecting real `.lrc` companion files to the UI, playback engine, and player lifecycle:
- **Asynchronous Companion `.lrc` Discovery:** Looks for sibling `.lrc` files on the device filesystem and queries Android's `ContentResolver` without blocking the main playback thread or UI thread.
- **Dynamic Playback Position Synchronization:** Tracks ExoPlayer / Media3 current playback position, taking user-configured millisecond offsets into account.
- **Non-Intrusive Animated Auto-Scrolling:** Smoothly animates the list view to keep the active lyric line centered, while detecting manual user touch gestures to avoid fighting the user. Auto-scroll automatically resumes after 4 seconds of inactivity or upon pressing the "Resume Sync" action.
- **Interactive Line Seeking:** Tapping any lyric line immediately seeks audio playback to that timestamp.
- **Persistent Millisecond Offset Adjuster:** Users can fine-tune lyrics synchronization with quick `±50ms` and `±500ms` controls. Offsets are persistently stored per track via `LyricOffsetStore` across application restarts.
- **Resilient Fallback:** When a track has no companion lyrics, a clean and informative placeholder guides the user on naming and placing `.lrc` companion files without fabricating fake lyric data.

---

## 2. Source Code Changes & New Architecture

| Component / File | Purpose |
| :--- | :--- |
| [`LyricOffsetStore.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/data/lyrics/LyricOffsetStore.kt) | Interface and production `SharedPreferencesLyricOffsetStore` + `InMemoryLyricOffsetStore` for persisting per-track synchronization offsets in milliseconds. |
| [`LrcParser.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/data/lyrics/LrcParser.kt) | Extended with `resolveLyricsForTrack()` handling file schemes, ContentResolver data lookup, and folder hierarchy searches. |
| [`PlayerViewModel.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/player/PlayerViewModel.kt) | Added `lyricOffsetMs`, `currentTrackLyrics`, asynchronous companion loader, and `adjustLyricOffset()` / `resetLyricOffset()` functions. |
| [`PlaybackManager.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/playback/PlaybackManager.kt) | Integrated `LyricOffsetStore` ensuring seamless access in service context. |
| [`DialogScreens.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/ui/DialogScreens.kt) | Upgraded `LyricsScreen` with active line highlight, animated auto-scroll, manual scroll detection, interactive tap-to-seek, and the offset control bar. |
| [`SyncedLyricsAndOffsetTest.kt`](file:///workspace/poweramp-clone/app/src/test/java/com/fahim/powerampclone/SyncedLyricsAndOffsetTest.kt) | 6 comprehensive unit tests covering offset persistence, asynchronous resolution, manual adjustments, line calculations, and error handling. |

---

## 3. Verification & Test Metrics

- **Unit Tests Passed:** 121 tests across 16 test suites (100% success rate, 0 failures, 0 skipped).
- **Execution Time:** Clean execution under Gradle `--offline`.
