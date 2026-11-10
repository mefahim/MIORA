# Poweramp Clone — Interactive Web Preview

> **Phase 2.1 premium redesign** — see `DESIGN-SYSTEM.md` and `REDESIGN-AUDIT.md`.
> v1 (plain Material UI) is preserved in git at `0d549b3`.

A fully interactive web prototype of the Phase 2 Android UI (Jetpack Compose),
recreated with **HTML5 + CSS3 + vanilla JavaScript only** — no frameworks.

> **Source of truth:** the Android implementation in `../app/src/main/java/com/fahim/powerampclone/`
> (commit `fb8f8b0` on GitHub). The Android project is untouched — this preview lives
> entirely under `web-preview/`.

## Run it

```bash
cd web-preview
python3 -m http.server 8321
# open http://127.0.0.1:8321/
```

Or open `index.html` directly in a browser (no build step, no dependencies).

## What is inside

| Screen | Status |
|---|---|
| Home (greeting, Recently Played, Browse by Category, All Songs) | ✅ |
| Songs (sort, long-press multi-select) | ✅ |
| Albums (grid) / Album Details | ✅ |
| Artists / Artist Details | ✅ |
| Playlists / Playlist Details | ✅ |
| Folders / Genres (grouped track lists) | ✅ |
| Now Playing (artwork, seek, transport, waveform, Lyrics/Audio Info/Queue) | ✅ |
| Mini Player (expands to Now Playing) | ✅ |
| Queue (reorder, remove, clear-confirm, shuffle) | ✅ |
| Search (grouped results + history) | ✅ |
| Lyrics (synced highlight for Sky Dreams; empty state otherwise) | ✅ |
| Audio Info (format, bitrate, sample rate, bit depth) | ✅ |
| Equalizer (10-band sliders + presets, **UI-only**) | ✅ |
| Settings (theme, accent, dynamic color, library, player, about) | ✅ |

## Interactions

Play / Pause, Next / Previous, seek slider, favourite toggle, shuffle,
repeat OFF → ALL → ONE, queue reorder / remove / clear / shuffle, search with
grouped results and history, sorting, multi-selection, album/artist/playlist
navigation, theme Light / Dark / System, accent selection, dynamic colour, and
**artwork-derived accents** — the interface re-tints when the track changes.

Playback is **simulated** (500 ms ticker, same as the Android `PlayerViewModel`);
there is no audio engine, DSP, or real audio file in this phase.

## Design system

- Material You + Apple-inspired premium music experience
- CSS custom properties for every colour token (`css/tokens.css`)
- Dynamic light / dark / system themes
- Fallback palette: deep ocean teal (`#0f766e`), never permanent purple
- Artwork is an original CSS gradient per album seed colour (same seeds as
  `data/MockData.kt`) — no Poweramp artwork or assets are used

## Viewport switcher

The toolbar above the phone frame switches **Mobile (390px) / Tablet (700px) /
Desktop (1180px)**. The phone frame is centred on desktop; no horizontal
overflow at any size. Touch targets are ≥ 44px.

## Structure

```
web-preview/
  index.html            App shell (phone frame, bottom nav, mini-player, modal root)
  css/  reset.css tokens.css layout.css components.css screens.css responsive.css
  js/   data.js icons.js artwork.js theme.js player.js navigation.js library.js search.js queue.js settings.js app.js
  assets/               (screenshots / artwork placeholders)
```

## QA results

- **Hosted preview (Muse artifact, `poweramp-clone-web-preview`)**: live-probe
  verified Home (greeting / Recently Played / categories / All Songs), mini-player
  Play→Pause, Now Playing (artwork, seek, transport, waveform), Next advancing the
  queue with the accent re-tinting to the new album seed, Repeat, queue remove,
  bottom-nav navigation, grouped Search results with history, Settings theme/accent
  rows, and the toolbar Dark toggle switching the whole UI. Zero console errors in
  every probe frame; page-level mobile overflow is 0. Two bugs were found and fixed
  during that QA: a broken screen-render binding and squeezed queue titles.
- **Repo copy (`web-preview/`)**: the same two bugs existed here and were fixed
  (missing `settings.js` script tag in `index.html`; `Screens.render` called the
  screen method without its receiver). After the fix, a Node DOM-stub smoke test
  rendered all 15 screens (Home, Songs, Albums, Artists, Playlists, Folders,
  Genres, Album/Artist/Playlist Details, Search, Queue, Settings, Lyrics, Audio
  Info, Equalizer, Now Playing) and exercised play/pause, next/previous, seek,
  favourite, shuffle, repeat cycling, queue move/remove/shuffle/clear, search,
  theme and accent switching — zero failures.
- **Not individually driven yet**: Lyrics, Audio Info, Equalizer, Folders, Genres,
  Artists/Playlists lists in a real browser (they share the verified render path),
  and the Desktop viewport frame at full width (checked at 1440px in the audit
  capture). In the probe browser, timers are throttled, so the Now Playing title
  refresh lags a click until the next render tick — behaviour is correct in a
  normal browser where the 500 ms ticker runs.

## Known limitations

- Build verification of the Android app (`./gradlew assembleDebug`) is still
  pending on a developer machine — Gradle cannot run in the original sandbox.
- Equalizer sliders are UI-only (no DSP in Phase 2).
- One script file (`research/patches/rea-patch-manager.sh`) lost its executable
  bit in the GitHub snapshot commit; content is identical.
- No real audio playback, no MediaStore scanning, no cast support.
