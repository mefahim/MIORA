# 04 — Exhaustive Feature Inventory

## Overview
This document catalogs every feature identified in the target application through decompiled bytecode analysis, Android manifest declarations, and decoded resource tables. Each entry provides evidentiary citations and evaluates clean-room feasibility for an independent implementation.

---

## Category A: Library Management

### A.01 — Song Library
- **Description**: Displays the complete catalog of indexed audio tracks on the device.
- **Related Screen**: `ListActivity` / Category: All Songs.
- **Entry Point**: Main Library navigation grid -> "All Songs".
- **Internal Implementation Evidence**: Table `folder_files` in `folders.db` (`p000/ar.java`), query via `RestProvider` (`com.maxmpz.audioplayer.rest.RestProvider`).
- **Runtime Behavior**: Inferred from list adapter queries ordered by song title collation.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: SQLite `folders.db`, `ScanDispatcherService`.
- **Independent Feasibility**: High (Room DB querying `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`).
- **Confidence Level**: High.

### A.02 — Albums View
- **Description**: Displays albums with embedded/cached album art, track counts, and release years.
- **Related Screen**: `ListActivity` / Category: Albums.
- **Entry Point**: Main Library navigation grid -> "Albums".
- **Internal Implementation Evidence**: Table `albums` with columns `album_sort`, `album_year`, and `num_songs` (`ar.java` L180-250, `po0.java`).
- **Runtime Behavior**: Inferred grid/list display with album cover caching.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Album metadata extractor, art cache.
- **Independent Feasibility**: High (`MediaStore.Audio.Albums`).
- **Confidence Level**: High.

### A.03 — Artists & Album Artists
- **Description**: Browsing by performing artist, album artist, and composers with multi-artist split support.
- **Related Screen**: `ListActivity` / Categories: Artists, Album Artists, Composers.
- **Entry Point**: Main Library navigation grid -> "Artists" / "Album Artists".
- **Internal Implementation Evidence**: Tables `artists`, `album_artists`, `composers`, and relational tables `multi_artists`, `multi_album_artists`, `multi_composers` with column `is_unsplit` (`po0.java` B0 migration).
- **Runtime Behavior**: Inferred relational artist-to-album drilldown.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Multi-artist tag parsing logic (e.g. splitting by `/`, `;`, `feat.`).
- **Independent Feasibility**: High (Normalized Room schema with junction tables).
- **Confidence Level**: High.

### A.04 — Genres Hierarchy
- **Description**: Displays music categorized by ID3/Vorbis genre tags with song and album counts.
- **Related Screen**: `ListActivity` / Category: Genres.
- **Entry Point**: Main Library navigation grid -> "Genres".
- **Internal Implementation Evidence**: Table `genres` with index `genres_played_at` (`po0.java`).
- **Runtime Behavior**: Inferred genre grouping.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Audio tag parser.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### A.05 — Folder & File Explorer Hierarchy
- **Description**: Dual folder navigation: flat folder list and full nested directory tree.
- **Related Screen**: `ListActivity` / Categories: Folders, Folder Hierarchy.
- **Entry Point**: Main Library navigation grid -> "Folders" / "Folder Hierarchy".
- **Internal Implementation Evidence**: Table `folders` with `parent_id`, `num_subfolders`, `path COLLATE NOCASE`, `is_cue` (`ar.java` L150-180), class `DirAndSAFScanner` (`com.maxmpz.audioplayer.scanner.folder.DirAndSAFScanner`).
- **Runtime Behavior**: Direct filesystem directory traversal.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Storage Access Framework (`DocumentFile`) and Java `File` APIs.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### A.06 — Global & Scoped Search
- **Description**: Instant search across tracks, albums, artists, genres, playlists, and settings.
- **Related Screen**: `MainActivity` / Search overlay dialog.
- **Entry Point**: Search icon button in player header or library action bar.
- **Internal Implementation Evidence**: Table `search_history` (`term`, `updated_at`), table `pref_search_fts` (`FTS4 unicode61 tokenizer`), string resource `searchable_label` ("Search songs, artists, albums, playlists").
- **Runtime Behavior**: Inferred live incremental filtering on keystrokes.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: SQLite FTS4/FTS5 virtual tables.
- **Independent Feasibility**: High (Room FTS5 implementation).
- **Confidence Level**: High.

### A.07 — Multi-Criteria Sorting & Grouping
- **Description**: Sort by title, artist, album, year, date added, date modified, track number, duration, file path, play count, and random shuffle.
- **Related Screen**: `ListOptsActivity` / `PlaylistSortActivity`.
- **Entry Point**: Overflow menu -> "List Options" / "Sort".
- **Internal Implementation Evidence**: `ListOptsActivity.java`, strings `sort_by_*` (`decoded-resources.json`).
- **Runtime Behavior**: Reordering list cursor queries based on user selection.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: SQL `ORDER BY` parameterization.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### A.08 — Favorites & Play History
- **Description**: Dedicated smart categories for favorited tracks, most played, and recently played tracks.
- **Related Screen**: `ListActivity` / Top-level Library items.
- **Entry Point**: Library -> "Most Played", "Recently Played", "Recently Added", "Top Rated".
- **Internal Implementation Evidence**: Columns `played_times`, `total_played_times`, `updated_at` in `folder_files`, `played_at` index in `genres`, `albums`, `artists` (`ar.java` L560-600).
- **Runtime Behavior**: Inferred tracking of playback completion timestamps.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Playback completion listener updating database columns.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### A.09 — Bookmarks System
- **Description**: Saved audio bookmarks with exact millisecond offsets and custom notes (ideal for podcasts and audiobooks).
- **Related Screen**: `ListActivity` / Category: Bookmarks.
- **Entry Point**: Player track overflow menu -> "Add Bookmark", Library -> "Bookmarks".
- **Internal Implementation Evidence**: Table `bookmarks` (`folder_file_id`, `offset_ms`, `meta`, `sort`, `created_at`) (`po0.java` A0 migration).
- **Runtime Behavior**: Inferred bookmark creation at current playback position.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Database bookmark records + playback seek on tap.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

---

## Category B: Audio Playback

### B.01 — Basic Playback Controls
- **Description**: Play, pause, previous track, next track, fast forward, rewind.
- **Related Screen**: `MainActivity` / `PlayerUIActivity`.
- **Entry Point**: On-screen control bar, notification, widget, headset button.
- **Internal Implementation Evidence**: Methods `native_play`, `native_pause`, `native_seek` in `Pipeline2.java`, `PlayerServiceCore.java`.
- **Runtime Behavior**: Audio playback initiation and state transitions.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Media player playback engine.
- **Independent Feasibility**: High (AndroidX Media3 `Player.play()`, `Player.pause()`).
- **Confidence Level**: High.

### B.02 — Dynamic Queue Management
- **Description**: Independent playback queue separate from current playlist or album context. Allows "Play Next" and "Enqueue".
- **Related Screen**: Queue view sheet / `MainActivity`.
- **Entry Point**: Song item action -> "Enqueue" / "Play Next", Library bottom bar -> "Queue".
- **Internal Implementation Evidence**: Table `queue` (`type`, `folder_file_id`, `library_media_id`, `played`, `sort`) in `folders.db` (`ar.java` L170-190).
- **Runtime Behavior**: Inferred FIFO / manual reorder queue playback.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Database queue table + queue playback scheduler.
- **Independent Feasibility**: High (Media3 custom playlist manager or Room Queue repository).
- **Confidence Level**: High.

### B.03 — Advanced Shuffle & Repeat Modes
- **Description**: Repeat Off, Repeat Song, Repeat Category (Album/Playlist), Advance Category. Shuffle Off, Shuffle All, Shuffle Category, Shuffle Songs then Category.
- **Related Screen**: `MainActivity` player controls.
- **Entry Point**: Shuffle and Repeat toggle buttons on player screen.
- **Internal Implementation Evidence**: Preference keys and strings `shuffle_all`, `shuffle_cats`, `repeat_all`, `repeat_single` (`decoded-resources.json`).
- **Runtime Behavior**: Inferred playlist cursor index manipulation.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Playlist cursor / navigation state machine.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### B.04 — Waveform Seekbar & Fine Scrubbing
- **Description**: Visual audio seekbar displaying the exact amplitude waveform of the track, supporting smooth scrubbing with zoom.
- **Related Screen**: `MainActivity` / `WaveSeekbar`.
- **Entry Point**: Main player screen central seekbar.
- **Internal Implementation Evidence**: Column `wave BLOB` in `folder_files` (`ar.java` L300), class `com.maxmpz.widget.player.WaveSeekbar`.
- **Runtime Behavior**: Renders pre-calculated RMS amplitudes as a dual-color waveform.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: RMS waveform extractor during scanning + custom Canvas rendering.
- **Independent Feasibility**: High (Compose `Canvas` drawing normalized float array).
- **Confidence Level**: High.

### B.05 — Gapless Playback & Crossfade
- **Description**: Seamless track transitions without silence; user-configurable crossfade duration (0 to 10 seconds) on manual or automatic track changes.
- **Related Screen**: `SettingsActivity` -> Audio -> Crossfade / Gapless.
- **Entry Point**: Settings -> Audio Engine.
- **Internal Implementation Evidence**: Native callback `callbackOnDspTrackStarted`, `native_set_end_event_position_ms` in `Pipeline2.java`, strings `pref_crossfade`, `pref_gapless` (`decoded-resources.json`).
- **Runtime Behavior**: Dual-decoder overlap in native C++ core.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: ExoPlayer dual-player crossfade or native Oboe mixer.
- **Independent Feasibility**: Moderate (Requires audio buffer crossfading).
- **Confidence Level**: High.

### B.06 — Playback Speed & Pitch Control
- **Description**: Variable tempo (0.5x to 2.0x) and musical pitch adjustment (-12 to +12 semitones).
- **Related Screen**: Player overflow sheet -> Tempo / Pitch.
- **Entry Point**: Overflow menu -> Speed / Pitch.
- **Internal Implementation Evidence**: Strings `tempo`, `pitch` (`decoded-resources.json`), SoundTouch / Sonic DSP integration in `libpowerampcore.so`.
- **Runtime Behavior**: Inferred DSP time-stretching without pitch alteration (or linked).
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Media3 `PlaybackParameters(speed, pitch)` (backed by Sonic).
- **Independent Feasibility**: High (Built into AndroidX Media3).
- **Confidence Level**: High.

### B.07 — Sleep Timer
- **Description**: Automatically pause playback after specified minutes or at the end of the current track, with gentle volume fade-out.
- **Related Screen**: Sleep Timer Dialog.
- **Entry Point**: Player overflow menu -> "Sleep Timer".
- **Internal Implementation Evidence**: String resources `sleep_timer`, `sleep_timer_fade`, `sleep_timer_end_of_track` (`decoded-resources.json`).
- **Runtime Behavior**: Inferred Android `AlarmManager` / Coroutine timer.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Coroutine timer + audio volume ramp.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### B.08 — Audio Focus & Headset Disconnect Safety
- **Description**: Pauses on headset unplug or Bluetooth disconnect; ducks or pauses on incoming phone calls and navigation instructions.
- **Related Screen**: `SettingsActivity` -> Audio -> Audio Focus.
- **Entry Point**: Settings -> Audio Focus.
- **Internal Implementation Evidence**: `ManifestHeadsetPlugReceiver.java`, `BTReceiver.java`, `AudioManager.OnAudioFocusChangeListener` in `PlayerServiceCore.java`.
- **Runtime Behavior**: Inferred focus transition handling.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `AudioManager` audio focus requests.
- **Independent Feasibility**: High (Handled automatically by Media3).
- **Confidence Level**: High.

### B.09 — Background Service & Notification Controls
- **Description**: Persistent foreground playback service with Android 13+ media style notification, action buttons, progress bar, and album artwork.
- **Related Screen**: System Notification Shade / Lock Screen.
- **Entry Point**: Always active during playback.
- **Internal Implementation Evidence**: `PlayerService.java`, permissions `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` in `AndroidManifest.xml`.
- **Runtime Behavior**: System notification displaying metadata and transport controls.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `MediaStyle` notification.
- **Independent Feasibility**: High (`MediaSessionService` / Media3).
- **Confidence Level**: High.

---

## Category C: Audio Processing & Equalizer

### C.01 — Multi-Band Equalizer (10 to 32 Bands)
- **Description**: Studio-grade equalizer with selectable band counts (10, 12, 15, 16, 24, 32 bands) or fully parametric mode.
- **Related Screen**: `EqActivity.java`.
- **Entry Point**: Main bottom navigation -> Equalizer button.
- **Internal Implementation Evidence**: `Band.java` (type, frequency, q, gain, channels), `Coefs.java` (`native_calc_bands_coefs`), `FrequencyResponseScroller.java`.
- **Runtime Behavior**: Biquad IIR filtering on 64-bit float audio buffer.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Native DSP or Android `AudioEffect.Equalizer`.
- **Independent Feasibility**: High (Kotlin / C++ Biquad filter chain or Android AudioEffect).
- **Confidence Level**: High.

### C.02 — Parametric Equalizer Mode
- **Description**: Add, delete, and configure arbitrary filter types (Peaking, Low/High Shelf, Low/High Pass, Bandpass, Notch) with continuous frequency (20Hz-20kHz) and Q-factor (0.1 to 12.0).
- **Related Screen**: `EqActivity.java` (Parametric toggle).
- **Entry Point**: Equalizer header -> "Parametric" switch.
- **Internal Implementation Evidence**: `eq_presets.parametric` column (`ar.java`), `PeqNativeUtils.java`, `Band.java`.
- **Runtime Behavior**: Dynamic frequency response curve plotting and interactive node dragging.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Custom DSP engine + custom Canvas curve renderer.
- **Independent Feasibility**: High (Robert Bristow-Johnson filter formulas).
- **Confidence Level**: High.

### C.03 — Presets & AutoEq Integration
- **Description**: Bundled audio presets (Rock, Pop, Classical, Flat, etc.), user-created presets, import/export, and AutoEq database search for specific headphone models.
- **Related Screen**: `EqPresetsActivity.java`, `AddEqPresetActivity.java`.
- **Entry Point**: Equalizer screen -> "Presets" button.
- **Internal Implementation Evidence**: Table `eq_presets` in `folders.db`, string resource `selected_auto_eq` ("Selected AutoEq") in `decoded-resources.json`.
- **Runtime Behavior**: Inferred preset JSON parsing and coefficient application.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Preset repository + AutoEq text parser.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### C.04 — Per-Device & Per-Track Preset Assignment
- **Description**: Automatically load specific EQ presets when connecting to Speaker, Wired Headset, Bluetooth (per MAC address), USB DAC, or Chromecast.
- **Related Screen**: `EqPresetAssignActivity.java`.
- **Entry Point**: Preset item overflow -> "Assign".
- **Internal Implementation Evidence**: Columns `bind_to_speaker`, `bind_to_wired`, `bind_to_bt`, `bind_to_usb`, `bind_to_cc`, `bind_to_other` in `eq_presets`, table `eq_preset_songs` (`ar.java` L180-250, L550).
- **Runtime Behavior**: Device connection receiver triggers preset reload.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Bluetooth receiver, Audio device callback, SQLite database.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### C.05 — Direct Volume Control (DVC)
- **Description**: Bypasses Android's internal software mixer, routing digital volume directly to the hardware amplifier for maximum dynamic range and headroom.
- **Related Screen**: `SettingsActivity` -> Audio -> Direct Volume Control.
- **Entry Point**: Equalizer DVC button / Audio settings.
- **Internal Implementation Evidence**: String resources `audio_dvc#dvc_enabled`, `shizuku_absvol`, `dsp_dvc_vol_compensation` (`decoded-resources.json`), Shizuku integration in `ShizukuProvider.java`.
- **Runtime Behavior**: Adjusts preamp and disables OS volume step attenuation.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: AudioTrack float mode, hardware DAC support, optional Shizuku IPC.
- **Independent Feasibility**: Moderate (Software preamp + 32-bit float output; hardware DVC requires vendor-specific direct output flags).
- **Confidence Level**: High.

### C.06 — Tone Controls (Bass & Treble)
- **Description**: Dedicated high-grade rotary knobs for Bass and Treble shelving filters.
- **Related Screen**: `EqActivity.java` / `EqBassToneKnobLayout`.
- **Entry Point**: Equalizer screen -> "Tone" page.
- **Internal Implementation Evidence**: Views `EqBassToneKnobLayout`, `EqTrebleToneKnobLayout`, state bus ID `dsp_bass_gain` (`decoded-resources.json`).
- **Runtime Behavior**: Rotational touch gestures driving shelving biquad filters.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Low-shelf and high-shelf IIR filters.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### C.07 — Spatializer, Reverb & Stereo Balance
- **Description**: Stereo widening effect, room acoustics simulation (Small Room to Large Hall), and Left/Right stereo pan balance.
- **Related Screen**: `EqActivity.java` / Reverb tab.
- **Entry Point**: Equalizer screen -> "Reverb" / "Stereo" tab.
- **Internal Implementation Evidence**: Table `reverb_presets` in `folders.db` (`ar.java`), Freeverb algorithmic reverb engine in `libpowerampcore.so`.
- **Runtime Behavior**: Inferred delay line / comb filter processing.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Freeverb open-source C++ implementation or Android `EnvironmentalReverb`.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

---

## Category D: Lyrics System

### D.01 — Embedded ID3/Vorbis Lyrics
- **Description**: Extracts and displays unsynced text lyrics embedded in ID3v2 (`USLT`), MP4 (`\u00a9lyr`), FLAC/Vorbis (`LYRICS`).
- **Related Screen**: `LyricsActivity.java`.
- **Entry Point**: Player track title tap / overflow menu -> "Lyrics".
- **Internal Implementation Evidence**: Column `has_lyrics_tag` in `folder_files` (`po0.java` F0 migration), `TagReader.java`.
- **Runtime Behavior**: Reads text tag directly from audio file header.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Audio tag parser library.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### D.02 — Synchronized LRC File Support
- **Description**: Automatically pairs `.lrc` subtitle files located in the same directory or `/Lyrics/` subfolder, highlighting lines in real time with auto-scroll.
- **Related Screen**: `LyricsActivity.java` / `LyricsFastTextView`.
- **Entry Point**: Lyrics view during playback.
- **Internal Implementation Evidence**: Table `lrc_files` (`title`, `artist`, `album`, `simple_filename`, `folder_path`, `is_utf8`) in `folders.db` (`po0.java` F0 migration), view `LyricsFastTextView.java`.
- **Runtime Behavior**: Time-stamped LRC line matching against current player position.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: LRC parser (`[mm:ss.xx]`) + Compose scroll state synchronized to player time.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### D.03 — Third-Party Lyrics App Integrations
- **Description**: Fallback deep integration with external installed lyrics applications (Genius, QuickLyric, Musixmatch, Letras) via Android Intents.
- **Related Screen**: `LyricsActivity.java`.
- **Entry Point**: Lyrics screen -> "Search with External App".
- **Internal Implementation Evidence**: Class `p000/th.java` matching packages `com.genius.android`, `com.geecko.QuickLyric.getLyrics`, `com.musixmatch.android.lyrify`, `com.studiosol.player.letras` (`decoded-resources.json`).
- **Runtime Behavior**: Launches external intent with track title, artist, and album extras.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `Intent` packaging.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### D.04 — Lyrics Caching & Database Storage
- **Description**: Stores downloaded and matched lyrics locally to avoid re-querying external sources.
- **Related Screen**: Behind the scenes in `LyricsActivity`.
- **Entry Point**: Automatic upon lyric match.
- **Internal Implementation Evidence**: Table `cached_lyrics` (`created_at`, `updated_at`, `created_by_pak`, `info_line`, `content`) (`po0.java` F0 migration).
- **Runtime Behavior**: Database lookup before initiating network or external queries.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Room Database entity.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

---

## Category E: Playlist System

### E.01 — Playlist Creation & Management
- **Description**: Create, rename, delete, and duplicate playlists.
- **Related Screen**: `PlayListActivity.java`, `EditNameActivity.java`.
- **Entry Point**: Library -> "Playlists" -> New Playlist button.
- **Internal Implementation Evidence**: Table `playlists` in `folders.db` (`ar.java`), `EditNameActivity.java`.
- **Runtime Behavior**: CRUD operations on playlist records.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: SQLite database.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### E.02 — Track Reordering & Playlist Persistence
- **Description**: Drag-and-drop reordering of playlist items with persistent custom order index.
- **Related Screen**: `PlayListActivity.java`.
- **Entry Point**: Inside any playlist -> drag handles.
- **Internal Implementation Evidence**: Table `playlist_entries` (`folder_file_id`, `playlist_id`, `sort`, `played_at`) (`ar.java` L170-190).
- **Runtime Behavior**: Inferred drag-and-drop re-indexing of `sort` column.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Reorderable list UI component + SQL batch update.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### E.03 — M3U / M3U8 Export & Import
- **Description**: Import file-based playlists (`.m3u`, `.m3u8`, `.pls`) from storage and export internal playlists to storage.
- **Related Screen**: `PlaylistsExportActivity.java`.
- **Entry Point**: Playlists overflow menu -> "Export Playlists".
- **Internal Implementation Evidence**: Activity `PlaylistsExportActivity` in `AndroidManifest.xml`, strings `export_playlists`, `select_playlists_will_be_deleted` (`decoded-resources.json`).
- **Runtime Behavior**: Generates UTF-8 encoded text files with relative/absolute paths.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: File I/O + M3U string formatting.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

---

## Category F: Visualizations & Artwork

### F.01 — Milkdrop OpenGL Visualizer
- **Description**: Real-time 2D/3D hardware-accelerated audio visualization supporting classic Winamp/Milkdrop `.milk` preset scripts.
- **Related Screen**: Fullscreen Visualizer / `MilkSurfaceView`.
- **Entry Point**: Player screen visualizer button / long-press album art.
- **Internal Implementation Evidence**: `com.maxmpz.milk.MilkRenderer`, `MilkLoader`, `MilkScanService`, permission `ACCESS_MILK_PRESETS`, table `milk_presets` (`ar.java`, `AndroidManifest.xml`).
- **Runtime Behavior**: OpenGL ES shader rendering reactive to FFT audio spectrum.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Open-source `projectM` library or custom GLSL shaders.
- **Independent Feasibility**: Moderate (Embedded projectM C++ library or Compose shader visualizer).
- **Confidence Level**: High.

### F.02 — Album Artwork Management & Auto-Download
- **Description**: High-resolution cover art display, embedded ID3 APIC extraction, external folder art (`cover.jpg`, `folder.png`), and web search.
- **Related Screen**: `SelectAlbumArtActivity.java`.
- **Entry Point**: Long-press album artwork -> "Select Album Art".
- **Internal Implementation Evidence**: Activity `SelectAlbumArtActivity`, string resources `select_album_art`, `download_album_art` (`decoded-resources.json`).
- **Runtime Behavior**: Asynchronous image resolution hierarchy: Embedded -> Local folder image -> HTTP cache.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Coil image loader + MusicBrainz / Last.fm artwork APIs.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

---

## Category G: UI Customization & Themes

### G.01 — Theme Engine (Light, Dark, Black, Yaps)
- **Description**: Built-in visual themes including Light, Dark, Black (OLED power-saving), and dynamic material accents.
- **Related Screen**: `SettingsActivity` -> Look and Feel -> Skin.
- **Entry Point**: Settings -> Look and Feel.
- **Internal Implementation Evidence**: Activities `white_round`, `black_round`, `zap_plain_adaptive`, `classic` in `AndroidManifest.xml`, skin action `com.maxmpz.audioplayer.SKIN_MAIN`.
- **Runtime Behavior**: Dynamic style attribute switching without full app restart.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Jetpack Compose Theme system with `dynamicDarkColorScheme`.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### G.02 — Homescreen Widgets (Multiple Dimensions)
- **Description**: 4x1, 4x2, 2x2, and 4x4 interactive homescreen widgets with configuration screens, artwork, and playback controls.
- **Related Screen**: Widget configure activities (`Widget4x1Configure`, etc.).
- **Entry Point**: Android Homescreen widget picker.
- **Internal Implementation Evidence**: AppWidgetProviders `Widget4x1Provider`, `Widget4x2Provider`, `Widget2x2Provider`, `Widget4x4Provider`, `Widget4x4ProviderCover` in `AndroidManifest.xml`.
- **Runtime Behavior**: Broadcast receiver updates widget RemoteViews.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `AppWidgetProvider` or Jetpack Glance.
- **Independent Feasibility**: High (Jetpack Glance Compose widgets).
- **Confidence Level**: High.

---

## Category H: Advanced Features

### H.01 — In-App Tag & Metadata Editor
- **Description**: Edit Title, Artist, Album, Year, Track Number, Genre, and Disk Number directly inside audio files.
- **Related Screen**: `EditTagActivity.java`.
- **Entry Point**: Track overflow menu -> "Edit Tags" / "Info/Tags".
- **Internal Implementation Evidence**: `EditTagActivity.java`, `TagWriter.java` (`com.maxmpz.audioplayer.scanner.TagWriter`).
- **Runtime Behavior**: Opens file descriptor via SAF and writes ID3v2/Vorbis tag frames.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: TagLib open-source library or JAudioTagger.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### H.02 — Audio File Inspector (Codec & Stream Technical Details)
- **Description**: Detailed audio stream inspector displaying exact codec, container, bitrate, sample rate, bit depth, channel configuration, and file path.
- **Related Screen**: `InfoActivity.java` / `AudioInfoActivity.java`.
- **Entry Point**: Track overflow menu -> "Info/Tags" -> Technical Details.
- **Internal Implementation Evidence**: `InfoActivity.java`, `AudioInfoActivity.java`, `FFMpegTagReader.java`.
- **Runtime Behavior**: Inferred stream inspection from decoder metadata.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: MediaExtractor / MediaFormat track metadata.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### H.03 — Android Auto Integration
- **Description**: Full automotive head-unit interface displaying curated library, playlists, search, and voice commands.
- **Related Screen**: Android Auto in-vehicle display.
- **Entry Point**: Vehicle USB/wireless projection.
- **Internal Implementation Evidence**: Service `BrowserService` implementing `MediaBrowserServiceCompat`, provider `AAContentProvider` in `AndroidManifest.xml`.
- **Runtime Behavior**: Exposes media hierarchy tree to Android Auto host.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: AndroidX Media3 `MediaSessionService` / `MediaLibraryService`.
- **Independent Feasibility**: High.
- **Confidence Level**: High.

### H.04 — USB DAC OTG Auto-Detection & Exclusive Mode
- **Description**: Automatically detects connected USB audio interfaces and prompts to launch high-res bit-perfect mode.
- **Related Screen**: `USBMainActivity.java`.
- **Entry Point**: USB DAC plug event.
- **Internal Implementation Evidence**: Activity `USBMainActivity`, permission `android.hardware.usb.host`, plugin `UsbxPluginService.java`.
- **Runtime Behavior**: Intent filter captures `android.hardware.usb.action.USB_DEVICE_ATTACHED`.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `UsbManager` and USB host permissions.
- **Independent Feasibility**: Moderate (Native USB audio driver or standard Android USB audio routing).
- **Confidence Level**: High.

### H.05 — Quick Settings Playback Tile
- **Description**: Android system Quick Settings pull-down tile to immediately toggle playback.
- **Related Screen**: System Quick Settings.
- **Entry Point**: Quick Settings shade.
- **Internal Implementation Evidence**: `TileService` with permission `android.permission.BIND_QUICK_SETTINGS_TILE` in `AndroidManifest.xml`.
- **Runtime Behavior**: System triggers `onClick()` on tile to dispatch toggle to `PlayerService`.
- **Access Classification**: `VERIFIED_STATIC`.
- **Dependencies**: Android `TileService` API.
- **Independent Feasibility**: High.
- **Confidence Level**: High.
