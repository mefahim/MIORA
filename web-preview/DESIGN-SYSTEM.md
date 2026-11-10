# Design System — Premium Redesign (Phase 2.1)

Product identity: **Poweramp Clone** — an original premium music product.
Visual language: Apple Music editorial warmth + Material You adaptive colour,
built as original design (no copied screens, artwork, or assets).

## 1. Colour architecture

Colour is derived, never hard-coded per screen. One engine (`js/theme.js`)
turns an artwork seed into a coordinated palette; CSS variables carry it.

**Neutral surfaces (theme-owned, artwork-independent):**

| Token | Light | Dark |
|---|---|---|
| `--bg` | `#faf9f6` warm off-white | `#121313` deep charcoal |
| `--surface` | `#ffffff` | `#1a1c1c` |
| `--surface-2` (elevated) | `#ffffff` + shadow | `#232626` |
| `--surface-3` (subtle fill) | `#f1efe9` | `#262929` |
| `--text` | `#171816` | `#f2f1ec` |
| `--text-2` (secondary) | `#5c5f5a` | `#a3a6a0` |
| `--text-3` (muted) | `#8a8d86` | `#6f7370` |
| `--outline` | `#e3e0d8` | `#2e3232` |

Dark is layered charcoal, never pure black; light is warm off-white, never a
dark theme inverted. Library screens stay calm and neutral — accent is used
selectively (active nav, play button, current track, links).

**Artwork-derived palette (from the current track's seed):**

- `--accent` — seed adjusted for contrast (light: L≈38%, dark: L≈68%).
- `--accent-2` — hue +40° (secondary), `--accent-3` — hue −40° (tertiary).
- `--accent-soft` — accent at 12% over surface (chips, selected rows).
- `--player-bg` — ambient gradient (seed at low lightness, dark) / warm tint
  (light) behind Now Playing only.
- Palette changes transition smoothly (`transition` on the consuming
  properties, 500 ms); no flashes, no muddy oversaturation (seed lightness
  is clamped, saturation capped at 72%).

**Fallback palette:** deep ocean teal `#0f766e` (same as Phase 2) when no
artwork seed is available or dynamic colour is off. Fixed accent choices
(Ocean / Forest / Ember / Rose / Graphite) replace the artwork seed.

## 2. Typography

System stack (no webfont dependency, portable to Compose later):
`-apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`.

| Role | Size / Weight / Tracking |
|---|---|
| Display (hero titles, Now Playing) | 28–32px / 800 / −0.03em |
| Screen title | 24px / 750 / −0.02em |
| Section heading | 17px / 700 / −0.01em |
| Track title | 16px / 600 / 0 |
| Artist / metadata | 14px / 400 / 0, `--text-2` |
| Label / caption | 12px / 600 / +0.04em uppercase |
| Playback numbers | 13px / 500, `font-variant-numeric: tabular-nums` |

Rules: album/track names get visual priority; avoid excessive bold; line
height 1.25 for titles, 1.4 for body.

## 3. Shape system (restrained)

- Artwork: 16px large, 12px thumb, 50% artist portrait.
- Buttons: pill (primary/tonal), 12px (small controls).
- Inputs: 14px. Dialogs/sheets: 24px. Bottom nav: none (flat bar).
- Do not round everything: rows, dividers and list surfaces stay square.

## 4. Spacing & layout

- Page gutter: 20px mobile, 28px desktop. Section gap: 28px. Grid gap: 16px.
- Component spacing scale: 4 / 8 / 12 / 16 / 20 / 28 / 40.
- Bottom nav height 64px + safe-area; mini-player 68px; desktop sidebar 248px.
- Touch targets ≥ 44px; icon buttons 44–48px.

## 5. Elevation

- `--shadow-sm`: 0 1px 2px rgba(0,0,0,.06); `--shadow-md`: 0 8px 24px rgba(0,0,0,.12);
  `--shadow-lg`: 0 24px 64px rgba(0,0,0,.28).
- Dark mode prefers tonal separation + 1px outline over heavy shadows.

## 6. Icon system (`js/icons.js`)

One self-authored stroke set: 24×24 viewBox, 1.75px stroke, round caps/joins,
`currentColor`. Filled variants (play, pause, skip, heart-filled) use
`fill="currentColor"`. No emoji in functional UI. States: inactive
`--text-2`, active `--accent`, disabled 40% opacity. Optical size 20px in
rows, 24px in nav, 28px in transport.

## 7. Artwork system (`js/artwork.js`)

Original abstract SVG covers, one distinct motif per album, derived from the
album's own seed colour (arcs, sunrise, neon grid, waves, EQ bars, skyline,
orbits). Square aspect, `preserveAspectRatio="xMidYMid slice"`, subtle depth
(shadow) and refined corners. Playlists/artists reuse the motif of their
first album. Fallback motif (duotone + circle) for anything without a seed.
No Poweramp assets, no stock photos, no repeated gradient.

## 8. Components

One implementation each, with default / hover / pressed / selected /
disabled / focus states: app header, section header, icon button, primary &
tonal & outline buttons, track row, album card, artist card, playlist card,
search field, filter chip, playback slider, mini-player, queue item, bottom
nav, sidebar rail, context menu, dialog, bottom sheet, empty state, loading
state. States are defined in `css/components.css`; screens compose them.

## 9. Motion

- Durations: 160 ms (press/feedback), 240 ms (screen/component), 360 ms
  (player/palette). Easing: `cubic-bezier(.22,1,.36,1)`.
- Screen transition: fade + 8px rise (no slide-across). Artwork crossfades on
  track change. Palette transitions 500 ms. Favourite pops 160 ms.
- `prefers-reduced-motion`: all transitions/animations off.

## 10. Responsive

- Mobile (primary): bottom nav, single column, hero artwork first.
- Tablet: 3-column album grid, wider gutters.
- Desktop: left sidebar navigation (248px), content max-width 1080px,
  4–5 column album grid, two-column Now Playing (artwork left, controls
  right), queue/search in wider columns. Never a stretched phone layout.
