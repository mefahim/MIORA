/* Theme system — Phase 2.1 palette engine (see DESIGN-SYSTEM.md).
   An artwork seed is turned into a coordinated palette via HSL math:
   accent (+40/-40 hues for secondary/tertiary), clamped lightness for
   contrast, capped saturation so palettes never go muddy or oversaturated.
   Library surfaces stay neutral; the player gets the ambient palette. */
"use strict";
const ACCENT_SEEDS = {
  dynamic: "#0f766e", ocean: "#0f766e", forest: "#2e7d32",
  ember: "#c2410c", rose: "#be123c", graphite: "#64748b",
};
const ACCENT_LABELS = {
  dynamic: "Dynamic (Material You)", ocean: "Ocean", forest: "Forest",
  ember: "Ember", rose: "Rose", graphite: "Graphite",
};
function hexToHsl(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = (n >> 16) / 255, g = ((n >> 8) & 255) / 255, b = (n & 255) / 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h = 0, s = 0; const l = (max + min) / 2;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) * 60;
    else if (max === g) h = ((b - r) / d + 2) * 60;
    else h = ((r - g) / d + 4) * 60;
  }
  return [h, s * 100, l * 100];
}
function hslToHex(h, s, l) {
  h = ((h % 360) + 360) % 360; s = Math.max(0, Math.min(100, s)) / 100; l = Math.max(0, Math.min(100, l)) / 100;
  const c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs(((h / 60) % 2) - 1)), m = l - c / 2;
  let r = 0, g = 0, b = 0;
  if (h < 60) [r, g, b] = [c, x, 0]; else if (h < 120) [r, g, b] = [x, c, 0];
  else if (h < 180) [r, g, b] = [0, c, x]; else if (h < 240) [r, g, b] = [0, x, c];
  else if (h < 300) [r, g, b] = [x, 0, c]; else [r, g, b] = [c, 0, x];
  const to = (v) => Math.round((v + m) * 255).toString(16).padStart(2, "0");
  return "#" + to(r) + to(g) + to(b);
}
function luminance(hex) {
  const n = parseInt(hex.slice(1), 16);
  const f = (c) => { c /= 255; return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); };
  return 0.2126 * f(n >> 16) + 0.7152 * f((n >> 8) & 255) + 0.0722 * f(n & 255);
}
function derivePalette(seed, dark) {
  const [h, s, l] = hexToHsl(seed);
  const sat = Math.min(s, 72);
  const accent = hslToHex(h, sat, dark ? 68 : 38);
  const accent2 = hslToHex(h + 40, Math.min(sat, 60), dark ? 62 : 42);
  const accent3 = hslToHex(h - 40, Math.min(sat, 60), dark ? 62 : 42);
  const soft = dark ? "rgba(255,255,255,0.08)" : hexToHexWithAlpha(accent, 0.12);
  return {
    accent, accent2, accent3, soft,
    onAccent: luminance(accent) > 0.45 ? "#141414" : "#ffffff",
    playerBg1: hslToHex(h, Math.min(sat, 55), dark ? 16 : 88),
    playerBg2: hslToHex(h, Math.min(sat, 60), dark ? 8 : 76),
  };
}
function hexToHexWithAlpha(hex, a) {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${n >> 16}, ${(n >> 8) & 255}, ${n & 255}, ${a})`;
}
const ThemeStore = {
  mode: localStorage.getItem("pc-theme-mode") || "system",
  dynamicColor: localStorage.getItem("pc-dynamic-color") !== "false",
  accent: localStorage.getItem("pc-accent") || "dynamic",
  artworkSeed: null,
  isDark() {
    return this.mode === "dark" || (this.mode === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
  },
  apply() {
    const root = document.documentElement;
    const dark = this.isDark();
    root.setAttribute("data-theme", dark ? "dark" : "light");
    root.setAttribute("data-accent", this.accent);
    const seed = this.dynamicColor && this.artworkSeed
      ? this.artworkSeed
      : ACCENT_SEEDS[this.accent] || ACCENT_SEEDS.dynamic;
    const p = derivePalette(seed, dark);
    root.style.setProperty("--accent", p.accent);
    root.style.setProperty("--accent-2", p.accent2);
    root.style.setProperty("--accent-3", p.accent3);
    root.style.setProperty("--accent-soft", p.soft);
    root.style.setProperty("--on-accent", p.onAccent);
    root.style.setProperty("--player-bg-1", p.playerBg1);
    root.style.setProperty("--player-bg-2", p.playerBg2);
    root.style.setProperty("--color-primary", p.accent);
    const btn = document.getElementById("toolbar-theme");
    if (btn) btn.textContent = dark ? "Light" : "Dark";
    localStorage.setItem("pc-theme-mode", this.mode);
    localStorage.setItem("pc-dynamic-color", String(this.dynamicColor));
    localStorage.setItem("pc-accent", this.accent);
  },
  setMode(mode) { this.mode = mode; this.apply(); },
  toggleDark() { this.mode = this.isDark() ? "light" : "dark"; this.apply(); },
  setDynamic(on) { this.dynamicColor = on; this.apply(); },
  setAccent(choice) { this.accent = choice; this.apply(); },
  setArtworkSeed(seed) { this.artworkSeed = seed; this.apply(); },
};
window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
  if (ThemeStore.mode === "system") ThemeStore.apply();
});
