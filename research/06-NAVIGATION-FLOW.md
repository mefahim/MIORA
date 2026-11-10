# 06 — Navigation Flow & State Architecture

## 1. Top-Level Application State Machine

The application transitions through three main visual layers:
1. **Cold Start & Onboarding Layer**: `StartupActivity` -> `WelcomeActivity` / Storage Permissions -> `MainActivity`.
2. **Main Application Surface**: Dual-pane / sliding sheet container alternating between **Fullscreen Player Screen** and **Library Browser Hierarchy**.
3. **Dedicated Auxiliary Overlays**: Modal sheets for Equalizer, Lyrics, Metadata Tag Editor, and Settings.

```mermaid
stateDiagram-v2
    [*] --> Startup
    Startup --> PermissionCheck: Cold Start
    PermissionCheck --> WelcomeOnboarding: Permissions Missing
    WelcomeOnboarding --> FolderPicker: Grant SAF Permissions
    FolderPicker --> MainSurface: Library Initialized
    PermissionCheck --> MainSurface: Permissions Already Granted

    state MainSurface {
        [*] --> FullPlayer
        FullPlayer --> LibraryBrowser: Swipe Down / Tap Header Back
        LibraryBrowser --> FullPlayer: Tap Mini-Player / Swipe Up
        FullPlayer --> EqualizerScreen: Tap EQ Icon
        EqualizerScreen --> FullPlayer: Tap Back / Player Icon
        FullPlayer --> LyricsSheet: Tap Track Title / Lyrics Button
        LyricsSheet --> FullPlayer: Dismiss Sheet
        FullPlayer --> TrackInfoDialog: Long-Press Track Title
        TrackInfoDialog --> FullPlayer: Close Dialog
    }

    MainSurface --> SettingsScreen: Tap Settings Icon
    SettingsScreen --> MainSurface: Back Press
```

---

## 2. Main Player & Library Navigation Flow

```mermaid
flowchart TD
    subgraph PlayerScreen["Fullscreen Player Screen (PlayerUIActivity)"]
        Art[Album Artwork Showcase]
        Wave[Interactive Waveform Seekbar]
        Controls[Transport Controls: Prev / Play-Pause / Next]
        Meta[Title, Artist, Album & Stream Info]
        BtnEQ[Equalizer Button]
        BtnLib[Library Button]
        BtnLyr[Lyrics Button]
        BtnMenu[Overflow Menu]
    end

    subgraph LibraryBrowser["Library Browser (ListActivity)"]
        CatGrid[Category Selector Grid]
        AllSongs[All Songs List]
        Albums[Albums Grid / List]
        Artists[Artists Drilldown]
        Genres[Genres List]
        Playlists[Playlists Manager]
        Folders[Folder Tree / Hierarchy]
        QueueView[Playback Queue]
        MiniPlayer[Persistent Bottom Mini-Player]
    end

    BtnLib -->|Tap or Swipe Down| CatGrid
    CatGrid --> AllSongs
    CatGrid --> Albums
    CatGrid --> Artists
    CatGrid --> Genres
    CatGrid --> Playlists
    CatGrid --> Folders
    CatGrid --> QueueView

    Albums -->|Select Album| AlbumSongs[Album Track List]
    Artists -->|Select Artist| ArtistAlbums[Artist Albums List]
    ArtistAlbums --> AlbumSongs
    Folders -->|Select Directory| FolderContents[Folder Track List]

    AlbumSongs -->|Tap Track| FullPlayerAction[Play Track & Expand Player]
    FolderContents -->|Tap Track| FullPlayerAction
    AllSongs -->|Tap Track| FullPlayerAction
    FullPlayerAction --> PlayerScreen
    MiniPlayer -->|Tap or Swipe Up| PlayerScreen
```

---

## 3. Equalizer & DSP Navigation Flow

```mermaid
flowchart LR
    subgraph EqualizerSurface["Equalizer (EqActivity)"]
        EQToggle[Master EQ Toggle]
        ModeToggle[Graphic / Parametric Switch]
        BandSliders[10 to 32 Band Sliders]
        ParametricCanvas[Interactive Frequency Response Canvas]
        ToneTab[Tone Knobs: Bass & Treble]
        ReverbTab[Reverb Acoustics: Room Size, Damping]
        StereoTab[Stereo Widening & Pan Balance]
        DVCToggle[Direct Volume Control Toggle]
        PresetsBtn[Presets Manager Button]
    end

    subgraph PresetsHierarchy["Presets (EqPresetsActivity)"]
        BuiltinPresets[Built-in Presets: Rock, Pop, Flat...]
        UserPresets[User Custom Presets]
        AutoEqSearch[AutoEq Headphone Database Search]
        AssignDialog[Assign to BT / USB / Wired Output]
    end

    BtnEQ --> EqualizerSurface
    PresetsBtn --> PresetsHierarchy
    PresetsHierarchy -->|Save Current| AddPresetDialog[AddEqPresetActivity]
    PresetsHierarchy -->|Long Press Preset| AssignDialog
```

---

## 4. Modal Dialogs & Sheets Hierarchy

All dialogs in the application extend `BaseDialogActivity`, rendering as elevated modal cards with dimming behind the content:

```mermaid
flowchart TD
    PlayerScreen[Fullscreen Player Screen]

    PlayerScreen -->|Overflow -> Info/Tags| InfoSheet[InfoActivity - Audio Technical Details]
    InfoSheet -->|Edit Tags Button| TagEditor[EditTagActivity - Tag Editor]
    
    PlayerScreen -->|Tap Lyrics Button| LyricsSheet[LyricsActivity - Synced Lyrics View]
    LyricsSheet -->|Offset Icon| LyricsOffset[Adjust Timing Offset +/- ms]
    LyricsSheet -->|External Icon| ExtLyrics[Launch Genius / Musixmatch Intent]

    PlayerScreen -->|Long Press Cover Art| ArtSheet[SelectAlbumArtActivity - Cover Art Chooser]
    ArtSheet -->|Web Search| OnlineArt[Download High-Res Cover Art]

    PlayerScreen -->|Overflow -> Sleep Timer| SleepTimer[Sleep Timer Dialog]
    
    PlayerScreen -->|Cast Icon| CastDialog[MediaRouteChooserActivity - Chromecast Device Picker]
```

---

## 5. Touch Gestures & Physical Key Controls

| Gesture / Physical Input | Context | Resulting Action |
| :--- | :--- | :--- |
| **Horizontal Swipe on Cover Art** | Player Screen | Next Track (Swipe Left) / Previous Track (Swipe Right) |
| **Vertical Swipe on Cover Art** | Player Screen | Next Category/Album (Swipe Down) / Previous Category (Swipe Up) |
| **Swipe Down on Player Screen** | Player Screen | Minimize player to bottom mini-player and reveal Library |
| **Swipe Up on Mini-Player** | Library Screen | Expand mini-player into Fullscreen Player Screen |
| **Pinch-to-Zoom** | Library Grid / List | Adjust grid column count (1 to 6 columns) and item zoom |
| **Rotary Drag on Knobs** | Equalizer / Tone | Adjust Bass, Treble, Balance, and Frequency values with haptic clicks |
| **Long-Press Track Item** | Any Track List | Show Context Action Sheet (Enqueue, Play Next, Info, Add to Playlist, Delete) |
| **Headset Media Button Single Click** | Background / Foreground | Play / Pause toggle |
| **Headset Media Button Double Click** | Background / Foreground | Next track |
| **Headset Media Button Triple Click** | Background / Foreground | Previous track |
| **Long-Press Volume Buttons (Screen Off)** | Locked / Screen Off | Skip forward / backward (`SET_VOLUME_KEY_LONG_PRESS_LISTENER`) |
