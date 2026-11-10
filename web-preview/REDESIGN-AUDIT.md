# Redesign Audit — Web Preview v1 (commit 0d549b3)

Audit date: 2026-11-10. Scope: `web-preview/` only. Android project and Phase 1
research are untouched. v1 is preserved in git (`0d549b3`) and remains restorable.

## What v1 got right (keep)

- Complete functional scope: all 18 destinations render; simulated playback,
  queue ops, search, sorting, multi-select, themes and accents all work.
- Data fidelity: the same 20 tracks / 7 albums / 7 artists / 5 playlists as
  `data/MockData.kt`; no invented content.
- No dead controls in the core flows (every button on Home/Player/Queue does
  something real in the prototype).

## Visual problems

1. **No product identity.** The header is the literal words "Poweramp Clone"
   in a system font; there is no brand mark, no display typography, no
   editorial voice. It reads as a template, not a product.
2. **Placeholder artwork everywhere.** Every album is the same CSS gradient
   with a ♫ glyph. A music library whose covers are indistinguishable is not a
   music library — it is a list with coloured squares.
3. **Emoji iconography.** 🏠 🔍 🎵 ⚙️ ♥ ⏮ ⏭ 🔀 🔁 📁 — functional icons are
   emoji with inconsistent size, weight and alignment. This alone caps the
   perceived quality at "hobby project".
4. **Flat, card-heavy composition.** Everything is a rounded rectangle on a
   grey surface: category cards, album grid, settings rows, queue rows. No
   editorial hierarchy, no featured moments, no intentional negative space.
5. **Weak colour system.** One `--color-primary` seed tints the whole UI; the
   "dynamic colour" is a single accent swap, not a coordinated palette
   (background / surface / elevated / text / muted are not derived from
   artwork at all). Light mode is effectively dark mode inverted.
6. **Dark theme is plain black-ish.** `#101414` background with `#191c1c`
   surfaces — no layered charcoal, no tonal separation, no soft dividers.
7. **Typography has one voice.** Every heading is `700` system-ui at a few
   sizes; track titles, section heads and screen titles are barely
   distinguishable. No display scale, no numeric (tabular) playback type.
8. **Now Playing is a stacked column, not an experience.** Artwork, title,
   slider, transport, waveform, buttons — in a single vertical list with
   equal visual weight. No ambient background, no artwork-derived atmosphere,
   no memorable composition.
9. **Mini-player looks like a notification.** A grey rectangle with a square
   thumb; no progress edge, no depth, no reason to tap it.
10. **Desktop is a stretched phone.** The 1180px frame just widens the same
    390px column layout; no sidebar, no multi-column library, no expanded
    player composition.

## UX problems

11. **Navigation is flat.** Bottom nav has 4 destinations but Library (the
    core of the product) is a category grid on Home, not a destination. No
    segmented library navigation; every category is a separate push with no
    sense of place.
12. **Queue reorder is arrows only.** Tiny ↑/↓ buttons are the only reorder
    mechanism — no drag handle, no drop feedback.
13. **Search is a bare input.** Unstyled field, plain history rows; no grouped
    result presentation with distinct artwork sizes per type.
14. **Equalizer is ten identical browser sliders.** No vertical faders, no
    preset identity, no visual relationship to audio.
15. **Settings is a form, not a product.** Plain rows; theme/accent choices
    have no live palette preview; dynamic colour is a toggle with no
    explanation of what it does.
16. **No shared empty/loading/error states.** Empty states are a grey circle
    + two lines; no helpful next action, no visual consistency.
17. **Motion is one fade.** `screenIn` (opacity + 14px slide) is the only
    transition; no artwork transition, no palette transition, no press
    feedback beyond hover, no reduced-motion handling.
18. **Selection mode is undiscoverable.** Long-press (550 ms) is the only
    entry; nothing on screen explains it.

## Screen-specific weaknesses

| Screen | Main weakness |
|---|---|
| Home | Category cards are 6 identical grey boxes; Recently Played is a plain row; no featured editorial moment |
| Songs | Rows are correct but visually flat; sort is a modal list; selection state is a tint only |
| Albums | Grid of identical gradient squares; artwork does not dominate |
| Artists | Settings-style list with round thumbs; no discovery feel |
| Playlists | Same list as Artists; smart playlists differ by a text suffix only |
| Folders | Raw paths as primary content; folder hierarchy has no breadcrumb |
| Genres | Plain grouped lists; no genre identity at all |
| Album/Artist/Playlist Details | Centred hero is fine but generic; no album-derived atmosphere |
| Now Playing | See #8 — the signature screen is the weakest composition |
| Queue | See #12; current track has only a tint to distinguish it |
| Search | See #13 |
| Lyrics | Centred lines, one active colour; no typography expression |
| Audio Info | Flat key-value list; long file path is not handled |
| Equalizer | See #14 |
| Settings | See #15 |

## Technical limitations (v1)

- `Screens.render` had an unbound-method bug (fixed in 0d549b3 follow-up) and
  `settings.js` was missing from `index.html` — both caught by QA, evidence
  that render paths need a smoke test on every change.
- Artwork is a CSS class with a hard-coded gradient; there is no artwork
  component that can take a real image or generated cover.
- Theme application writes one variable; nothing derives a palette.
- No icon system; icons are inline emoji/Unicode scattered through templates.
- No design tokens for elevation, motion, or focus states.

## Redesign decisions

1. **Original abstract cover art** — a deterministic SVG cover generator,
   one distinct motif per album (arcs, sunrise, neon grid, waves, EQ bars,
   skyline, orbits), derived from the album's own seed colour. No Poweramp
   assets, no stock photos, no repeated gradient.
2. **Self-authored stroke icon system** — one 24×24, 1.75px-stroke set,
   consistent optical sizing, active/inactive states, no emoji in functional
   UI. (Lucide-style, original paths.)
3. **Derived palette engine** — artwork seed → coordinated primary/secondary/
   tertiary/background/surface/elevated/text/muted via HSL math; player gets
   the strongest artwork atmosphere, library stays calm; smooth transitions;
   deep-charcoal dark theme and warm off-white light theme as separate designs.
4. **Editorial Home** — featured album hero, horizontal shelves, segmented
   library navigation, curated playlists, continue listening.
5. **Desktop sidebar layout** — left navigation rail, wider content, expanded
   two-column Now Playing; mobile keeps bottom nav.
6. **Queue drag reorder** — pointer-based drag with drop feedback (arrows
   remain as an accessible fallback).
7. **Vertical EQ faders** — custom vertical sliders with preset identity and
   reset.
8. **Motion system** — consistent durations/easing, artwork + palette
   transitions, press feedback, `prefers-reduced-motion` respected.
9. **Shared states** — one empty-state component with explanation + action;
   one dialog/bottom-sheet style; one context-menu style.
10. **Keep everything that works** — same 20 tracks, same simulated player
    logic, same interaction set; only the visual experience is redesigned.
