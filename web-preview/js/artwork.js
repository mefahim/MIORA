/* Original abstract cover art — one distinct SVG motif per album, derived
   from the album's own seed colour. No Poweramp assets, no stock photos. */
"use strict";
let __artUid = 0;
function shadeHex(hex, amt) {
  const n = parseInt(hex.slice(1), 16);
  const r = Math.max(0, Math.min(255, (n >> 16) + amt));
  const g = Math.max(0, Math.min(255, ((n >> 8) & 255) + amt));
  const b = Math.max(0, Math.min(255, (n & 255) + amt));
  return "#" + ((r << 16) | (g << 8) | b).toString(16).padStart(6, "0");
}
function mixHex(a, b, t) {
  const na = parseInt(a.slice(1), 16), nb = parseInt(b.slice(1), 16);
  const r = Math.round(((na >> 16) + ((nb >> 16) - (na >> 16)) * t));
  const g = Math.round((((na >> 8) & 255) + (((nb >> 8) & 255) - ((na >> 8) & 255)) * t));
  const bl = Math.round(((na & 255) + ((nb & 255) - (na & 255)) * t));
  return "#" + ((r << 16) | (g << 8) | bl).toString(16).padStart(6, "0");
}
const MOTIFS = {
  // 1 Dreamscape — concentric arcs over a horizon
  arcs(seed) {
    const light = mixHex(seed, "#ffffff", 0.55), dark = shadeHex(seed, -60);
    let rings = "";
    for (let i = 0; i < 5; i++) rings += `<circle cx="100" cy="128" r="${26 + i * 15}" fill="none" stroke="${i % 2 ? light : "#ffffff"}" stroke-opacity="${0.85 - i * 0.13}" stroke-width="5"/>`;
    return `<rect width="200" height="200" fill="${dark}"/><rect y="128" width="200" height="72" fill="${seed}" opacity="0.35"/>${rings}<circle cx="100" cy="128" r="10" fill="${light}"/>`;
  },
  // 2 Better Tomorrow — sunrise with rays
  sunrise(seed) {
    const light = mixHex(seed, "#fff7d6", 0.5), dark = shadeHex(seed, -55);
    let rays = "";
    for (let i = 0; i < 7; i++) { const a = (-150 + i * 25) * Math.PI / 180; const x2 = 100 + Math.cos(a) * 86, y2 = 118 + Math.sin(a) * 86; rays += `<line x1="100" y1="118" x2="${x2.toFixed(0)}" y2="${y2.toFixed(0)}" stroke="${light}" stroke-width="4" stroke-linecap="round" opacity="0.8"/>`; }
    let bars = "";
    for (let i = 0; i < 4; i++) bars += `<rect x="24" y="${138 + i * 12}" width="152" height="5" rx="2.5" fill="#ffffff" opacity="${0.5 - i * 0.1}"/>`;
    return `<rect width="200" height="200" fill="${dark}"/>${rays}<circle cx="100" cy="118" r="34" fill="${light}"/>${bars}`;
  },
  // 3 Midnight Lights — neon perspective grid
  neon(seed) {
    const light = mixHex(seed, "#ffffff", 0.45), dark = shadeHex(seed, -70);
    let grid = "";
    for (let i = 0; i <= 6; i++) grid += `<line x1="${20 + i * 27}" y1="200" x2="${100 + (i - 3) * 16}" y2="96" stroke="${seed}" stroke-width="2" opacity="0.7"/>`;
    for (let i = 0; i < 4; i++) grid += `<line x1="0" y1="${118 + i * 24}" x2="200" y2="${118 + i * 24}" stroke="${seed}" stroke-width="2" opacity="${0.55 - i * 0.1}"/>`;
    const dots = [[52, 66], [100, 44], [148, 66], [76, 88], [124, 88]].map(([x, y], i) => `<circle cx="${x}" cy="${y}" r="${i === 1 ? 7 : 5}" fill="${light}"/>`).join("");
    return `<rect width="200" height="200" fill="${dark}"/>${grid}<circle cx="100" cy="96" r="26" fill="none" stroke="${light}" stroke-width="3"/>${dots}`;
  },
  // 4 Ocean Breeze — layered waves
  waves(seed) {
    const dark = shadeHex(seed, -60);
    const w = (y, amp, op, fill) => `<path d="M0 ${y} Q 25 ${y - amp} 50 ${y} T 100 ${y} T 150 ${y} T 200 ${y} V 200 H 0 Z" fill="${fill}" opacity="${op}"/>`;
    return `<rect width="200" height="200" fill="${dark}"/>${w(84, 22, 0.55, mixHex(seed, "#ffffff", 0.35))}${w(118, 26, 0.7, seed)}${w(152, 20, 0.9, mixHex(seed, "#ffffff", 0.15))}<circle cx="152" cy="48" r="18" fill="#ffffff" opacity="0.85"/>`;
  },
  // 5 Feel the Beat — equalizer bars
  eq(seed) {
    const dark = shadeHex(seed, -62);
    const heights = [52, 96, 68, 128, 84, 112, 58, 92];
    const bars = heights.map((h, i) => `<rect x="${18 + i * 21}" y="${168 - h}" width="13" height="${h}" rx="6.5" fill="${i % 2 ? mixHex(seed, "#ffffff", 0.35) : seed}"/>`).join("");
    return `<rect width="200" height="200" fill="${dark}"/>${bars}<rect x="18" y="172" width="164" height="4" rx="2" fill="#ffffff" opacity="0.35"/>`;
  },
  // 6 City Lights — skyline with lit windows
  skyline(seed) {
    const dark = shadeHex(seed, -66), light = mixHex(seed, "#ffffff", 0.5);
    const buildings = [[14, 96, 34, 104], [54, 62, 30, 138], [90, 84, 36, 116], [132, 56, 30, 144], [168, 92, 22, 108]];
    let rects = buildings.map(([x, y, w, h]) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${seed}"/>`).join("");
    let wins = "";
    buildings.forEach(([x, y, w, h]) => { for (let r = 0; r < 4; r++) for (let c = 0; c < 2; c++) { if ((r + c + x) % 3 === 0) wins += `<rect x="${x + 6 + c * 12}" y="${y + 10 + r * 16}" width="5" height="7" fill="${light}" opacity="0.9"/>`; } });
    return `<rect width="200" height="200" fill="${dark}"/><circle cx="158" cy="42" r="16" fill="${light}"/>${rects}${wins}`;
  },
  // 7 Infinite Moment — orbit rings
  orbits(seed) {
    const dark = shadeHex(seed, -60), light = mixHex(seed, "#ffffff", 0.5);
    return `<rect width="200" height="200" fill="${dark}"/><circle cx="100" cy="100" r="30" fill="${seed}"/><ellipse cx="100" cy="100" rx="74" ry="26" fill="none" stroke="${light}" stroke-width="3" transform="rotate(-24 100 100)"/><ellipse cx="100" cy="100" rx="56" ry="56" fill="none" stroke="${light}" stroke-width="2" stroke-dasharray="3 9" opacity="0.7"/><circle cx="162" cy="66" r="7" fill="${light}"/><circle cx="52" cy="140" r="5" fill="#ffffff" opacity="0.8"/>`;
  },
  // fallback — duotone + circle
  fallback(seed) {
    const dark = shadeHex(seed, -55);
    return `<rect width="200" height="200" fill="${dark}"/><path d="M0 200 200 0v200z" fill="${seed}" opacity="0.55"/><circle cx="100" cy="100" r="46" fill="none" stroke="#ffffff" stroke-width="4" opacity="0.85"/><circle cx="100" cy="100" r="14" fill="#ffffff" opacity="0.9"/>`;
  },
};
const ALBUM_MOTIF = { 1: "arcs", 2: "sunrise", 3: "neon", 4: "waves", 5: "eq", 6: "skyline", 7: "orbits" };
function coverSVG(seed, motif, label) {
  const id = "cv" + (++__artUid);
  const inner = (MOTIFS[motif] || MOTIFS.fallback)(seed);
  return `<svg viewBox="0 0 200 200" role="img" aria-label="Album artwork for ${escapeHtml(label || "music")}" preserveAspectRatio="xMidYMid slice">${inner}</svg>`;
}
function artworkHTML(seed, motifKey, title, size, radiusClass) {
  return `<span class="artwork ${radiusClass || "rounded-12"}" style="width:${size}px;height:${size}px;min-width:${size}px" data-motif="${motifKey}">${coverSVG(seed, motifKey, title)}</span>`;
}
function albumArtwork(album, size, radiusClass) {
  return artworkHTML(album.seed, ALBUM_MOTIF[album.id] || "fallback", album.title, size, radiusClass);
}
function trackArtwork(track, size, radiusClass) {
  return artworkHTML(track.seed, ALBUM_MOTIF[track.albumId] || "fallback", track.title, size, radiusClass);
}
