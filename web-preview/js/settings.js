/* Settings / Lyrics / Audio Info / Equalizer — Phase 2.1 redesign. */
"use strict";
const SettingsState = {
  autoAddPlaylists: localStorage.getItem("pc-auto-playlists") !== "false",
  crossfade: localStorage.getItem("pc-crossfade") || "Off",
  playbackMode: localStorage.getItem("pc-playback-mode") || "Normal",
  eqGains: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  eqPreset: "Flat",
};
const EQ_PRESETS = {
  Flat: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  Rock: [4, 3, 1, 0, -1, 0, 1, 3, 4, 5],
  Pop: [-1, 0, 2, 3, 3, 2, 0, -1, -1, 0],
  Jazz: [3, 2, 1, 0, -1, 0, 1, 2, 3, 3],
  Classical: [4, 3, 2, 0, -1, -1, 0, 2, 3, 4],
};
const EQ_FREQS = ["31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"];
const themeLabel = () => ({ system: "System default", light: "Light", dark: "Dark" })[ThemeStore.mode];

const SettingsScreens = {
  render() {
    const el = document.getElementById("screen-settings");
    const root = document.documentElement;
    const cs = getComputedStyle(root);
    const sw = (label, val, fg) => `<span class="sw" style="background:${val};color:${fg || "#fff"}">${label}</span>`;
    el.innerHTML = `
      ${Screens.topbar("Settings", { back: true })}
      <div class="settings-profile">
        <div class="avatar">${icon("user", 28)}</div>
        <div><div class="name">Music Lover</div><div class="sub">Local library • 20 sample tracks</div></div>
      </div>
      <div class="settings-group">Appearance</div>
      ${this.row("palette", "Theme", themeLabel() + " — follows your system when set to System", "theme")}
      ${this.row("palette", "Accent colour", ACCENT_LABELS[ThemeStore.accent], "accent")}
      ${this.toggleRow("Dynamic colour", "Let the current album's artwork tint the player and accents", ThemeStore.dynamicColor, "dynamic")}
      <div class="palette-preview" aria-label="Live palette preview">
        ${sw("Accent", cs.getPropertyValue("--accent").trim())}
        ${sw("2nd", cs.getPropertyValue("--accent-2").trim())}
        ${sw("3rd", cs.getPropertyValue("--accent-3").trim())}
        ${sw("Player", cs.getPropertyValue("--player-bg-1").trim())}
      </div>
      <div class="settings-group">Library</div>
      ${this.toggleRow("Auto-add playlists", "Add new folders as playlists automatically", SettingsState.autoAddPlaylists, "autoplaylists")}
      ${this.row("folder", "Scan for music", "Sample library — 20 tracks indexed", "scan")}
      <div class="settings-group">Player</div>
      ${this.row("play", "Playback mode", SettingsState.playbackMode, "playback")}
      ${this.row("audio", "Crossfade", SettingsState.crossfade + " — preview only, no audio engine yet", "crossfade")}
      ${this.row("sliders", "Equalizer", "Preview only — sliders do not process audio in Phase 2", "equalizer")}
      <div class="settings-group">About</div>
      ${this.row("info", "Poweramp Clone", "Version 1.0.0 • Phase 2.1 premium redesign", "about")}
      <p class="settings-note">Clean-room UI. Simulated playback only — no audio decoding, DSP or streaming in this phase. Dynamic colour tints the player from the current album's artwork; turn it off to keep a fixed accent.</p>`;
    bindTopbar(el, {});
    el.querySelectorAll("[data-setting]").forEach((b) => b.addEventListener("click", () => SettingsScreens.openSetting(b.dataset.setting)));
    el.querySelectorAll("[data-toggle]").forEach((t) => t.addEventListener("change", () => {
      const key = t.dataset.toggle;
      if (key === "dynamic") ThemeStore.setDynamic(t.checked);
      if (key === "autoplaylists") { SettingsState.autoAddPlaylists = t.checked; localStorage.setItem("pc-auto-playlists", String(t.checked)); }
      SettingsScreens.render();
    }));
  },
  row(ic, title, sub, key) {
    return `<button class="settings-row" data-setting="${key}"><span class="ic">${icon(ic, 21)}</span><span class="meta"><span class="title" style="display:block">${escapeHtml(title)}</span><span class="sub" style="display:block">${escapeHtml(sub)}</span></span><span class="chev">${icon("chevronRight", 19)}</span></button>`;
  },
  toggleRow(title, sub, checked, key) {
    return `<div class="settings-row" style="cursor:default"><span class="meta"><span class="title" style="display:block">${escapeHtml(title)}</span><span class="sub" style="display:block">${escapeHtml(sub)}</span></span>
      <label class="toggle"><input type="checkbox" data-toggle="${key}" ${checked ? "checked" : ""} aria-label="${escapeHtml(title)}"><span class="track"></span></label></div>`;
  },
  openSetting(key) {
    if (key === "theme") choiceDialog("Theme", [["System default", "system"], ["Light", "light"], ["Dark", "dark"]], ThemeStore.mode, (v) => { ThemeStore.setMode(v); SettingsScreens.render(); });
    if (key === "accent") choiceDialog("Accent colour", Object.entries(ACCENT_LABELS).map(([v, l]) => [l, v]), ThemeStore.accent, (v) => { ThemeStore.setAccent(v); SettingsScreens.render(); });
    if (key === "playback") choiceDialog("Playback mode", [["Normal", "Normal"], ["Gapless (preview)", "Gapless"], ["Crossfade (preview)", "Crossfade"]], SettingsState.playbackMode, (v) => { SettingsState.playbackMode = v; localStorage.setItem("pc-playback-mode", v); SettingsScreens.render(); });
    if (key === "crossfade") choiceDialog("Crossfade", [["Off", "Off"], ["2 seconds", "2s"], ["5 seconds", "5s"], ["10 seconds", "10s"]], SettingsState.crossfade, (v) => { SettingsState.crossfade = v; localStorage.setItem("pc-crossfade", v); SettingsScreens.render(); });
    if (key === "equalizer") Nav.go("equalizer");
    if (key === "scan") showSnackbar("Sample library already indexed — 20 tracks.");
    if (key === "about") showSnackbar("Poweramp Clone 1.0.0 — Phase 2.1 premium redesign.");
  },
  lyrics() {
    const el = document.getElementById("screen-lyrics");
    const t = Player.currentTrack;
    if (!t) { el.innerHTML = `${Screens.topbar("Lyrics", { back: true })}<div class="empty"><div class="empty-icon">${icon("lyrics", 34)}</div><h3>No track playing</h3><p>Play a song to see its lyrics.</p><button class="btn btn-primary" data-browse>Browse songs</button></div>`; bindTopbar(el, {}); el.querySelector("[data-browse]").addEventListener("click", () => Nav.go("songs")); return; }
    el.innerHTML = `${Screens.topbar("Lyrics", { back: true })}
      <div style="display:flex;align-items:center;gap:12px;padding:8px var(--gutter)">${trackArtwork(t, 48, "rounded-8")}<div><div style="font-size:17px;font-weight:700">${escapeHtml(t.title)}</div><div style="color:var(--text-2);font-size:14px">${escapeHtml(t.artist)}</div></div></div>
      ${t.lyrics.length ? `<div class="lyrics-list">${t.lyrics.map(([ms, line], i) => {
        const next = t.lyrics[i + 1] ? t.lyrics[i + 1][0] : Infinity;
        const active = Player.positionMs >= ms && Player.positionMs < next;
        return `<div class="line ${active ? "active" : ""}">${escapeHtml(line)}</div>`;
      }).join("")}</div>` : `<div class="empty"><div class="empty-icon">${icon("lyrics", 34)}</div><h3>No synced lyrics</h3><p>This sample track has no embedded lyrics yet. Lyrics are shown only when the track actually carries them — none are invented here.</p></div>`}`;
    bindTopbar(el, {});
  },
  audioInfo() {
    const el = document.getElementById("screen-audioinfo");
    const t = Player.currentTrack;
    if (!t) { el.innerHTML = `${Screens.topbar("Audio Info", { back: true })}<div class="empty"><div class="empty-icon">${icon("info", 34)}</div><h3>No track playing</h3><p>Play a song to see its audio information.</p><button class="btn btn-primary" data-browse>Browse songs</button></div>`; bindTopbar(el, {}); el.querySelector("[data-browse]").addEventListener("click", () => Nav.go("songs")); return; }
    const group = (title, rows) => `<div class="kv-group"><h3>${title}</h3>${rows.map(([k, v]) => `<div class="kv"><span class="k">${escapeHtml(k)}</span><span class="v">${escapeHtml(v)}</span></div>`).join("")}</div>`;
    el.innerHTML = `${Screens.topbar("Audio Info", { back: true })}
      ${group("Track", [["Title", t.title], ["Artist", t.artist], ["Album", t.album], ["Genre", t.genre], ["Year", String(t.year)]])}
      ${group("Audio", [["Format", t.format], ["Bitrate", t.bitrateKbps + " kbps"], ["Sample rate", t.sampleRateHz + " Hz"], ["Bit depth", t.bitDepth + "-bit"], ["Duration", formatDuration(t.durationMs)]])}
      ${group("File", [["Path", t.folder]])}
      <p class="settings-note">Sample metadata for the demo library — values are illustrative, not measured from a real file.</p>`;
    bindTopbar(el, {});
  },
  equalizer() {
    const el = document.getElementById("screen-equalizer");
    el.innerHTML = `${Screens.topbar("Equalizer", { back: true })}
      <div style="padding:8px var(--gutter)"><h2 style="font-size:19px;font-weight:750">10-band equalizer</h2>
      <p style="color:var(--text-2);font-size:14px;margin-top:4px;line-height:1.5">Preview only — these faders do not process audio in Phase 2. The DSP engine arrives in a later phase.</p></div>
      <div class="eq-presets">${Object.keys(EQ_PRESETS).map((name) => `<button class="chip ${SettingsState.eqPreset === name ? "selected" : ""}" data-preset="${name}">${name}</button>`).join("")}<button class="chip" data-reset>${icon("reset", 15)} Reset</button></div>
      <div class="eq-bands">${SettingsState.eqGains.map((g, i) => `
        <div class="eq-band"><span class="db">${g >= 0 ? "+" : ""}${g}</span>
        <input type="range" class="vertical" min="-12" max="12" step="1" value="${g}" data-band="${i}" aria-label="${EQ_FREQS[i]} Hz band">
        <span class="freq">${EQ_FREQS[i]}</span></div>`).join("")}</div>
      <p class="settings-note">Preset: <strong>${escapeHtml(SettingsState.eqPreset)}</strong> — drag a fader to switch to Custom.</p>`;
    bindTopbar(el, {});
    el.querySelectorAll("[data-preset]").forEach((b) => b.addEventListener("click", () => {
      SettingsState.eqPreset = b.dataset.preset;
      SettingsState.eqGains = [...EQ_PRESETS[b.dataset.preset]];
      SettingsScreens.equalizer();
    }));
    el.querySelector("[data-reset]").addEventListener("click", () => {
      SettingsState.eqPreset = "Flat";
      SettingsState.eqGains = [...EQ_PRESETS.Flat];
      SettingsScreens.equalizer();
    });
    el.querySelectorAll("[data-band]").forEach((s) => s.addEventListener("input", () => {
      const v = Number(s.value);
      SettingsState.eqGains[Number(s.dataset.band)] = v;
      SettingsState.eqPreset = "Custom";
      s.closest(".eq-band").querySelector(".db").textContent = (v >= 0 ? "+" : "") + v;
      el.querySelectorAll("[data-preset]").forEach((c) => c.classList.remove("selected"));
    }));
  },
};
