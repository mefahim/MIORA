# Phase 4 — Metadata & Artwork Pipeline Report

## 1. Synchronized Lyrics Architecture (Milestone 4.3)

`LrcParser` provides a high-performance, specification-compliant parser and serializer for synchronized `.lrc` lyrics files.

```mermaid
flowchart LR
    AudioFile[Audio File: /Music/Song.flac] --> FindLrc[Find Sibling .lrc File]
    FindLrc -->|Found: /Music/Song.lrc| ReadFile[Read UTF-8 Text Content]
    ReadFile --> Parser[LrcParser.parseLrc]
    Parser --> MatchTags[Filter Header & Offset Tags]
    MatchTags --> ExtractTimestamps[Parse [mm:ss.xx] & [mm:ss.xxx]]
    ExtractTimestamps --> ChronoSort[Sort Chronologically by startMs]
    ChronoSort --> DomainModel[List<LyricLine>]
    DomainModel --> Storage[Store in SQLite tracks.lyrics_raw]
```

### Features:
1. **Centisecond & Millisecond Precision**: Supports standard `[mm:ss.xx]` (hundredths) and high-precision `[mm:ss.xxx]` timestamps.
2. **Multi-Timestamp Repeating Lines**: Handles chorus structures like `[00:10.00][00:30.00] Chorus line` into discrete synchronized lyric lines.
3. **Global Playback Offsets**: Correctly applies `[offset:+500]` adjustments to every timestamp.
4. **Bi-directional Serialization**: Converts between `List<LyricLine>` and formatted LRC text (`formatLrc` and `parseLrc`).
5. **Companion Sibling Discovery**: `LrcParser.findSiblingLrcFile` searches for adjacent `.lrc` files on storage.

---

## 2. Audio Format & Codec Resolution

The metadata extractor categorizes and resolves high-resolution audio formats directly from file headers, file extensions, and MIME types:

| Format Codec | Supported Extensions | MIME Types |
| :--- | :--- | :--- |
| **FLAC** | `.flac` | `audio/flac`, `audio/x-flac` |
| **MP3** | `.mp3` | `audio/mpeg`, `audio/mp3` |
| **M4A / AAC** | `.m4a`, `.aac`, `.mp4` | `audio/mp4`, `audio/aac` |
| **OGG / VORBIS** | `.ogg` | `audio/ogg`, `application/ogg` |
| **OPUS** | `.opus` | `audio/opus`, `audio/ogg` |
| **WAV** | `.wav` | `audio/wav`, `audio/x-wav` |
| **DSD** | `.dsd`, `.dsf`, `.dff` | `audio/dsd`, `audio/x-dsf` |
| **APE** | `.ape` | `audio/x-ape` |
| **ALAC** | `.alac` | `audio/alac` |

---

## 3. Artwork Pipeline & In-Memory LRU Cache

`ArtworkResolver` manages cover art loading, resizing, and caching:

1. **LRU In-Memory Cache**: `LruBitmapCache(maxEntries = 120)` provides instant, O(1) in-memory bitmap recall.
2. **Priority Chain**:
   - `LruBitmapCache` in-memory hit.
   - Embedded track picture via `MediaMetadataRetriever.embeddedPicture`.
   - ContentResolver `loadThumbnail` for Android 10+ (API 29+).
   - MediaStore `albumart` URI stream decoder.
   - Deterministic HSL seed color fallback (`seedColorForString`) for immediate flicker-free rendering.
3. **Cache Invalidation**:
   - `invalidate(trackId: Long)` cleans cached entries when a track is updated or rescanned.
   - `invalidateAlbum(albumId: Long)` clears album art when artwork changes.
