# 05 — Comprehensive Screen & Component Inventory

## 1. Overview
The target application declares **66 distinct activities** in its manifest, comprising main interface containers, dialog sheets, widget configuration flows, skin theme entry points, licensing gates, and third-party provider integration endpoints.

---

## 2. Primary Interface Containers

| Component Name | Description / Visual Role | Exported | Launch Mode | Verification |
| :--- | :--- | :--- | :--- | :--- |
| `StartupActivity` | Initial splash, permission onboarding dispatcher, and cold-start router | `true` | `standard` | `VERIFIED_STATIC` |
| `MainActivity` | Primary application window hosting the main player screen, library browser, and bottom control bar | `true` | `singleTask` | `VERIFIED_STATIC` |
| `PlayerUIActivity` | Fullscreen player view with album artwork, waveform seekbar, and transport controls | `true` | `standard` | `VERIFIED_STATIC` |
| `ListActivity` | Library list/grid view (All Songs, Albums, Artists, Genres, Playlists, Folders) | `true` | `standard` | `VERIFIED_STATIC` |
| `PlayListActivity` | Playlist viewing, track reordering, and playlist metadata management | `true` | `standard` | `VERIFIED_STATIC` |
| `EqActivity` | Multi-band and parametric equalizer, tone controls, reverb, and preset selector | `true` | `standard` | `VERIFIED_STATIC` |
| `USBMainActivity` | Dedicated launch activity triggered when a USB DAC / audio interface is connected | `true` | `singleTask` | `VERIFIED_STATIC` |

---

## 3. Settings & Configuration Hierarchy

| Component Name | Description / Visual Role | Exported | Launch Mode | Verification |
| :--- | :--- | :--- | :--- | :--- |
| `preference.SettingsActivity` | Root settings dashboard with category search and navigation | `true` | `standard` | `VERIFIED_STATIC` |
| `SettingsActivity` | Alias / entry point for general application preferences | `true` | `standard` | `VERIFIED_STATIC` |
| `preference.SettingsActivityManageSpace` | Android System Storage management endpoint (Clear Cache, Reset DB) | `true` | `standard` | `VERIFIED_STATIC` |
| `preference.SettingsLicenseActivity` | License status, purchase recovery, and registration screen | `false` | `standard` | `VERIFIED_STATIC` |

---

## 4. Modal Dialog & Auxiliary Activities

| Component Name | Purpose / UI Description | Theme / Style | Verification |
| :--- | :--- | :--- | :--- |
| `dialogs.WelcomeActivity` | First-run onboarding wizard and basic permission request flow | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.MusicFoldersActivity` | Storage folder picker and SAF directory selection manager | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.InfoActivity` | Audio technical details sheet (codec, sample rate, bit depth, bitrate) | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.AudioInfoActivity` | Extended audio stream and DSP pipeline diagnostics sheet | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.EditTagActivity` | Audio metadata tag editor (Title, Artist, Album, Year, Genre, Track #) | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.LyricsActivity` | Synced and unsynced lyrics viewer, offset adjuster, and external provider launcher | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.SelectAlbumArtActivity` | Album art chooser, cache inspector, and online art downloader | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.EqPresetsActivity` | Equalizer presets manager, search, and AutoEq browser | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.AddEqPresetActivity` | Dialog to save, name, and bind a new custom EQ preset | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.AddReverbPresetActivity` | Dialog to save and name a custom reverb acoustics preset | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.EqPresetAssignActivity` | Assigns EQ presets to specific output devices (BT MAC, USB, Wired) or tracks | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.PlaylistSelectActivity` | "Add to Playlist" destination selector dialog | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.PlaylistShortcutActivity` | Homescreen shortcut creator for selected playlist | Standard | `VERIFIED_STATIC` |
| `dialogs.PlaylistSortActivity` | Sorting criteria selector for playlist tracks | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.PlaylistsExportActivity` | M3U/M3U8 playlist export directory and file selector | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.ListOptsActivity` | View options sheet (List vs Grid, Zoom level, Compact mode, Collation) | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.AudioBufferActivity` | Audio buffer size and output latency fine-tuning dialog | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.EditNameActivity` | Generic text entry dialog to rename playlists or presets | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.ChangelogActivity` | What's new in this build release notes sheet | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.AutoFindDialogActivity` | Automated missing file search dialog | Modal Dialog | `VERIFIED_STATIC` |
| `dialogs.GenericDialogActivity` | System confirmation and alert dialog host | Modal Dialog | `VERIFIED_STATIC` |
| `widget.app.AlertDialogActivity` | Custom-styled alert dialog wrapper | Modal Dialog | `VERIFIED_STATIC` |

---

## 5. Homescreen Widget Configuration Activities

| Component Name | Target Widget Dimension | Exported | Verification |
| :--- | :--- | :--- | :--- |
| `widgetpackcommon.Widget4x1Configure` | 4x1 Standard Player Widget | `true` | `VERIFIED_STATIC` |
| `widgetpackcommon.Widget4x2Configure` | 4x2 Player + Track Info Widget | `true` | `VERIFIED_STATIC` |
| `widgetpackcommon.Widget2x2Configure` | 2x2 Compact Artwork Widget | `true` | `VERIFIED_STATIC` |
| `widgetpackcommon.Widget4x4Configure` | 4x4 Full Featured Player Widget | `true` | `VERIFIED_STATIC` |
| `widgetpackcommon.Widget4x2ConfigureCover` | 4x2 Large Cover Art Widget | `true` | `VERIFIED_STATIC` |
| `widgetpackcommon.Widget4x4ConfigureCover` | 4x4 Full Artwork Showcase Widget | `true` | `VERIFIED_STATIC` |

---

## 6. Theme & Dynamic Launcher Icon Aliases (15 Activities)

The application declares 15 distinct activity aliases to support dynamic themed app icons and theme switching without restarting the process:

1. `com.maxmpz.audioplayer.rounded_play_inverted_mu`
2. `com.maxmpz.audioplayer.rounded_inverted_mu`
3. `com.maxmpz.audioplayer.rounded_play_mu`
4. `com.maxmpz.audioplayer.rounded_mu`
5. `com.maxmpz.audioplayer.standard_mu`
6. `com.maxmpz.audioplayer.white_play_adaptive`
7. `com.maxmpz.audioplayer.black_play_adaptive`
8. `com.maxmpz.audioplayer.classic`
9. `com.maxmpz.audioplayer.white_round`
10. `com.maxmpz.audioplayer.black_round`
11. `com.maxmpz.audioplayer.zap_plain_adaptive`
12. `com.maxmpz.audioplayer.zap_round`
13. `com.maxmpz.audioplayer.zap_adaptive`
14. `com.maxmpz.audioplayer.round`
15. `com.maxmpz.audioplayer.standard_adaptive`

---

## 7. Licensing & Payment Integration Screens

| Component Name | Role / Integration | Verification |
| :--- | :--- | :--- |
| `dialogs.ExpiredActivity` | Trial expired modal blocking playback and prompting license purchase | `VERIFIED_STATIC` |
| `dialogs.APMUnlockDialogActivity` | Web purchase license key entry and email validation dialog | `VERIFIED_STATIC` |
| `dialogs.HuaweiMigrationActivity` | Migration flow for Huawei AppGallery purchases | `VERIFIED_STATIC` |
| `RstrActivity` | RuStore payment scheme redirect handler (`rstrpoweramp://`) | `VERIFIED_STATIC` |
| `ru.rustore.sdk.pay.internal.presentation.ui.PayActivity` | RuStore payment checkout sheet | `VERIFIED_STATIC` |
| `com.android.billingclient.api.ProxyBillingActivity` | Google Play Billing in-app purchase flow | `VERIFIED_STATIC` |
| `com.android.billingclient.api.ProxyBillingActivityV2` | Google Play Billing V2 flow | `VERIFIED_STATIC` |

---

## 8. Integration & Helper Endpoints

| Component Name | Role / Protocol | Intent Filter | Verification |
| :--- | :--- | :--- | :--- |
| `VoiceSearchHelperActivity` | Handles Google Assistant / Voice Search commands | `android.media.action.MEDIA_PLAY_FROM_SEARCH` | `VERIFIED_STATIC` |
| `PowerampAPIActivity` | Receives external third-party control intents | `com.maxmpz.audioplayer.API_COMMAND` | `VERIFIED_STATIC` |
| `MediaRouteChooserActivity` | Google Cast device selection sheet | MediaRouter Intent | `VERIFIED_STATIC` |
| `MediaRouteControllerActivity` | Active Google Cast volume and stream control sheet | MediaRouter Intent | `VERIFIED_STATIC` |
