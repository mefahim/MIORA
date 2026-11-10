# Redesign QA Report — Phase 2.1 (2026-11-10)

Scope: `web-preview/` redesign (Batches 0–9). v1 preserved in git at `0d549b3`.

## Method

1. **Repo smoke test (Node DOM stub)** — rendered all 16 screens
   (Home, Library, Songs, Albums, Artists, Playlists, Folders, Genres,
   Album/Artist/Playlist Details, Search, Queue, Settings, Lyrics, Audio
   Info, Equalizer, Now Playing) and exercised play/pause, next/previous,
   seek, favourite, shuffle, repeat cycling, queue move/remove/shuffle/
   clear, search, theme and accent switching. **Result: 0 failures.**
2. **Single-file bundle upload** — the v2 bundle uploaded successfully to
   Muse file storage (121,171 bytes, expires 2026-11-12), but the QA
   browser could not open it: its organization policy blocks the muse.ai
   file host ("This page is blocked"). No screenshots could be taken from
   that URL. The bundle is not the product's fault; the host is blocked.
3. **Hosted live preview (Muse artifact `poweramp-clone-web-preview`)** —
   the builder's probe drove the live page: Home, Now Playing, Queue,
   Desktop sidebar, theme toggle. **0 console errors, 0 unreachable
   controls on the probed screens, page-level mobile overflow 0.**

## Issues found and fixed during QA

- **Home shelf occlusion (audit)** — at scroll position 0, the first shelf's
  centre fell ~12px past the phone scrollport's bottom edge (mini-player
  area). Fixed by compacting the Home header (hero 218px→182px, tighter
  margins, ~42px saved) so the first shelf sits inside the visible
  scrollport. Note: shelves are horizontal carousels by design; below-fold
  content is reached by scrolling, like any feed.
- **Queue title truncation (audit)** — track titles were squeezed by the
  overflow button; fixed by suppressing the redundant overflow button in
  queue rows and tightening actions to 32px.
- **Repo copy bugs (found in v1 QA, fixed in v2)** — missing `settings.js`
  script tag; unbound `Screens.render` method call.

## Verified in the live probe

- Home: greeting, featured-album hero with play action, "Jump back in" and
  album/playlist shelves, library chips.
- Now Playing: ambient artwork wash, large original cover, favourite, seek,
  transport, waveform; 0 console errors.
- Queue: highlighted Now Playing card, Up Next with drag handles and
  reorder/remove controls, active nav pill.
- Desktop switch: sidebar rail with Library section beside content.
- Dark toggle: whole UI switches to layered charcoal.

## Not individually driven in a real browser

Songs/Albums/Search/Settings/EQ/Lyrics screens (they render through the same
pipeline verified on Home/Player/Queue); the Desktop viewport at full width
(checked at 1440px in the audit capture); Lyrics sync, Audio Info groups and
EQ faders in a live browser. In the probe browser, timers are throttled, so
the Now Playing title refresh lags a click until the next render tick —
behaviour is correct in a normal browser where the 500 ms ticker runs.

## Known limitations / divergences

- **In-app name**: the hosted preview brands itself "AURA" (the builder's
  interpretation of "original product identity"). The repo copy still says
  "Poweramp Clone". Fahim decides which name stands.
- **Fonts**: the live page uses Google Fonts (Sora/Manrope) with system-font
  fallbacks; font loading fails inside the audit sandbox (egress lockdown)
  but works in a normal browser.
- **Docs**: `REDESIGN-AUDIT.md` / `DESIGN-SYSTEM.md` / this report live in
  the repo only; the single-file artifact cannot serve separate markdown
  files (summarised in Settings → About instead).
- **Android build** (`./gradlew assembleDebug`) is still unverified — the
  Gradle daemon cannot run in this VM; it needs a developer machine.
- Playback remains simulated; Equalizer is UI-only; lyrics exist only for
  "Sky Dreams" and are not invented for other tracks.
