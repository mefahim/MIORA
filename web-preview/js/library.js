/* Screen renderers — Phase 2.1 premium redesign.
   Ports of ui/HomeScreen.kt, ui/LibraryScreens.kt, ui/PlayerScreen.kt
   with the new design system (icons.js + artwork.js). */
"use strict";
const LibraryState = { sort: "title", selecting: false, selected: new Set() };
function greeting() {
  const h = new Date().getHours();
  if (h >= 5 && h < 12) return "Good morning";
  if (h >= 12 && h < 17) return "Good afternoon";
  if (h >= 17 && h < 22) return "Good evening";
  return "Good night";
}
function trackRowHTML(track, opts = {}) {
  const playing = Player.currentTrack && Player.currentTrack.id === track.id && Player.isPlaying;
  const fav = Player.favorites.has(track.id);
  const selected = LibraryState.selected.has(track.id);
  const num = opts.number != null ? `<span class="num">${opts.number}</span>` : "";
  return `<div class="track-row ${playing ? "playing" : ""} ${selected ? "selected" : ""}" data-track="${track.id}" role="button" tabindex="0" aria-label="${escapeHtml(track.title)} by ${escapeHtml(track.artist)}">
    ${num}${trackArtwork(track, 48, "rounded-8")}
    <div class="meta"><div class="title">${escapeHtml(track.title)}</div><div class="sub">${escapeHtml(track.artist)} • ${escapeHtml(track.album)}</div></div>
    ${fav ? `<span class="fav" aria-label="Favourite">${icon("heartFilled", 17)}</span>` : ""}
    <span class="dur">${formatDuration(track.durationMs)}</span>
    <button class="more" data-more="${track.id}" aria-label="More options for ${escapeHtml(track.title)}">${icon("more", 18)}</button>
  </div>`;
}
function bindTrackRows(root) {
  root.querySelectorAll("[data-track]").forEach((el) => {
    const id = Number(el.dataset.track);
    const track = trackById(id);
    el.addEventListener("click", (e) => {
      if (e.target.closest("[data-more]")) return;
      if (LibraryState.selecting) { toggleSelect(id); return; }
      Player.playTrack(track); Nav.go("player");
    });
    el.addEventListener("keydown", (e) => { if (e.key === "Enter") { Player.playTrack(track); Nav.go("player"); } });
    let pressTimer = null;
    el.addEventListener("pointerdown", () => { pressTimer = setTimeout(() => toggleSelect(id), 550); });
    ["pointerup", "pointerleave"].forEach((ev) => el.addEventListener(ev, () => clearTimeout(pressTimer)));
  });
  root.querySelectorAll("[data-more]").forEach((btn) => btn.addEventListener("click", (e) => {
    e.stopPropagation();
    const track = trackById(Number(btn.dataset.more));
    openModal(`<h3>${escapeHtml(track.title)}</h3>
      <button class="choice" data-act="play">${icon("play", 18)} Play</button>
      <button class="choice" data-act="next">${icon("skipNext", 18)} Play next</button>
      <button class="choice" data-act="fav">${icon(Player.favorites.has(track.id) ? "heartFilled" : "heart", 18)} ${Player.favorites.has(track.id) ? "Remove from favourites" : "Add to favourites"}</button>
      <div class="modal-actions"><button data-close>Close</button></div>`);
    document.querySelector("#modal-root [data-close]").addEventListener("click", closeModal);
    document.querySelectorAll("#modal-root [data-act]").forEach((b) => b.addEventListener("click", () => {
      const act = b.dataset.act; closeModal();
      if (act === "play") { Player.playTrack(track); Nav.go("player"); }
      if (act === "fav") Player.toggleFavorite(track.id);
      if (act === "next") { Player.queue.splice(Player.currentIndex + 1, 0, track); Player.emit(); showSnackbar("Added to queue: " + track.title); }
    }));
  }));
}
function toggleSelect(id) {
  if (LibraryState.selected.has(id)) LibraryState.selected.delete(id); else LibraryState.selected.add(id);
  LibraryState.selecting = LibraryState.selected.size > 0;
  Screens.render(Nav.current());
}
function sortedTracks() {
  const list = [...TRACKS];
  switch (LibraryState.sort) {
    case "title": return list.sort((a, b) => a.title.localeCompare(b.title));
    case "artist": return list.sort((a, b) => a.artist.localeCompare(b.artist));
    case "duration": return list.sort((a, b) => b.durationMs - a.durationMs);
    case "recent": return list.sort((a, b) => b.id - a.id);
    case "track": return list.sort((a, b) => a.trackNumber - b.trackNumber);
    default: return list;
  }
}
const SORT_LABELS = { title: "Title", artist: "Artist", duration: "Duration", recent: "Recently Added", track: "Track Number" };

const Screens = {
  render(screen) {
    const map = {
      home: this.home, library: this.library, songs: this.songs, albums: this.albums,
      artists: this.artists, playlists: this.playlists, folders: this.folders, genres: this.genres,
      detail: this.detail, search: () => SearchScreens.render(), queue: () => QueueScreens.render(),
      settings: () => SettingsScreens.render(), lyrics: () => SettingsScreens.lyrics(),
      audioinfo: () => SettingsScreens.audioInfo(), equalizer: () => SettingsScreens.equalizer(),
      player: this.player,
    };
    (map[screen] || this.home).call(this);
    renderMiniPlayer();
  },
  topbar(title, opts = {}) {
    const back = opts.back ? `<button class="back-btn" data-back aria-label="Back">${icon("chevronLeft", 22)}</button>` : "";
    const actions = (opts.actions || []).map((a) => `<button class="icon-btn" data-action="${a.id}" aria-label="${escapeHtml(a.label)}">${icon(a.icon, 21)}</button>`).join("");
    return `<div class="topbar">${back}<h1>${escapeHtml(title)}</h1>${actions}</div>`;
  },
  home() {
    const el = document.getElementById("screen-home");
    const featured = ALBUMS[0];
    const recent = tracksByIds(RECENTLY_PLAYED_IDS);
    el.innerHTML = `
      <div class="topbar"><h1 style="font-size:19px">Poweramp Clone</h1>
        <button class="icon-btn" data-action="search" aria-label="Search">${icon("search", 21)}</button>
        <button class="icon-btn" data-action="settings" aria-label="Settings">${icon("settings", 21)}</button></div>
      <div class="greeting"><h2>${greeting()},<br>music lover.</h2><p>Your library, beautifully in tune.</p></div>
      <div class="home-hero" data-album="${featured.id}" role="button" tabindex="0" aria-label="Featured album ${escapeHtml(featured.title)}">
        <div class="hero-art">${coverSVG(featured.seed, ALBUM_MOTIF[featured.id], featured.title)}</div>
        <div class="hero-scrim"></div>
        <div class="hero-text"><div class="hero-kicker">Featured album</div><h2>${escapeHtml(featured.title)}</h2><p>${escapeHtml(featured.artist)} • ${featured.year} • ${featured.trackIds.length} tracks</p></div>
        <span class="hero-play">${icon("play", 24)}</span>
      </div>
      <div class="section-head"><h2>Recently played</h2><button data-goto="songs">See all</button></div>
      <div class="hscroll">${recent.slice(0, 6).map((t) => `
        <button class="shelf-item" data-track="${t.id}">
          ${trackArtwork(t, 132, "rounded-16")}
          <div class="t">${escapeHtml(t.title)}</div><div class="s">${escapeHtml(t.artist)}</div>
        </button>`).join("")}</div>
      <div class="section-head"><h2>Browse your library</h2></div>
      <div class="cat-strip">
        <button class="cat-pill" data-goto="songs"><span class="ic">${icon("library", 18)}</span>Songs</button>
        <button class="cat-pill" data-goto="albums"><span class="ic">${icon("disc", 18)}</span>Albums</button>
        <button class="cat-pill" data-goto="artists"><span class="ic">${icon("user", 18)}</span>Artists</button>
        <button class="cat-pill" data-goto="playlists"><span class="ic">${icon("queue", 18)}</span>Playlists</button>
        <button class="cat-pill" data-goto="folders"><span class="ic">${icon("folder", 18)}</span>Folders</button>
        <button class="cat-pill" data-goto="genres"><span class="ic">${icon("audio", 18)}</span>Genres</button>
      </div>
      <div class="section-head"><h2>Curated playlists</h2><button data-goto="playlists">See all</button></div>
      <div class="hscroll">${PLAYLISTS.slice(0, 3).map((p) => `
        <button class="shelf-item" data-playlist="${p.id}">
          ${artworkHTML(p.seed, ALBUM_MOTIF[PLAYLISTS[0].id] && "fallback", p.name, 132, "rounded-16")}
          <div class="t">${escapeHtml(p.name)}</div><div class="s">${p.trackIds.length} tracks${p.smart ? " • Smart" : ""}</div>
        </button>`).join("")}</div>
      <div class="section-head"><h2>Continue listening</h2><button data-goto="songs">See all</button></div>
      <div class="list">${sortedTracks().slice(0, 6).map((t) => trackRowHTML(t)).join("")}</div>`;
    bindTopbar(el, { search: () => Nav.go("search"), settings: () => Nav.go("settings") });
    el.querySelectorAll("[data-goto]").forEach((b) => b.addEventListener("click", () => Nav.go(b.dataset.goto)));
    el.querySelectorAll("[data-playlist]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "playlist", id: Number(b.dataset.playlist) } })));
    el.querySelector("[data-album]").addEventListener("click", () => Nav.go("detail", { detail: { kind: "album", id: featured.id } }));
    bindTrackRows(el);
  },
  library() {
    const el = document.getElementById("screen-library");
    const cats = [
      ["Songs", "library", "songs", `${TRACKS.length} tracks`],
      ["Albums", "disc", "albums", `${ALBUMS.length} albums`],
      ["Artists", "user", "artists", `${ARTISTS.length} artists`],
      ["Playlists", "queue", "playlists", `${PLAYLISTS.length} playlists`],
      ["Folders", "folder", "folders", `${FOLDERS.length} folders`],
      ["Genres", "audio", "genres", `${GENRES.length} genres`],
    ];
    el.innerHTML = `
      ${this.topbar("Library", { actions: [{ id: "settings", icon: "settings", label: "Settings" }] })}
      <p class="count-line">${TRACKS.length} songs • ${ALBUMS.length} albums • ${ARTISTS.length} artists</p>
      <div class="seg-nav">${cats.map(([label, , route]) => `<button class="chip" data-goto="${route}">${label}</button>`).join("")}</div>
      <div class="list">${cats.map(([label, ic, route, count]) => `
        <button class="row-item" data-goto="${route}">
          <span style="width:48px;height:48px;border-radius:12px;background:var(--accent-soft);color:var(--accent);display:grid;place-items:center">${icon(ic, 22)}</span>
          <div class="meta"><div class="title">${label}</div><div class="sub">${count}</div></div>
          <span class="chev">${icon("chevronRight", 20)}</span>
        </button>`).join("")}</div>`;
    bindTopbar(el, { settings: () => Nav.go("settings") });
    el.querySelectorAll("[data-goto]").forEach((b) => b.addEventListener("click", () => Nav.go(b.dataset.goto)));
  },
  songs() {
    const el = document.getElementById("screen-songs");
    const sel = LibraryState.selecting;
    el.innerHTML = `
      ${this.topbar(sel ? LibraryState.selected.size + " selected" : "Songs", { back: true, actions: [{ id: "sort", icon: "filter", label: "Sort songs" }, { id: "select", icon: "check", label: "Select tracks" }] })}
      <p class="count-line">${TRACKS.length} songs • sorted by ${SORT_LABELS[LibraryState.sort].toLowerCase()}</p>
      ${sel ? `<div style="display:flex;gap:8px;padding:8px var(--gutter)"><button class="btn btn-tonal" data-act="fav">Add to favourites</button><button class="btn btn-outline" data-act="clear">Clear selection</button></div>` : ""}
      <div class="list">${sortedTracks().map((t) => trackRowHTML(t)).join("")}</div>`;
    bindTopbar(el, {
      sort: () => choiceDialog("Sort by", Object.entries(SORT_LABELS).map(([v, l]) => [l, v]), LibraryState.sort, (v) => { LibraryState.sort = v; Screens.render("songs"); }),
      select: () => { LibraryState.selecting = !LibraryState.selecting; if (!LibraryState.selecting) LibraryState.selected.clear(); Screens.render("songs"); },
    });
    const favBtn = el.querySelector("[data-act='fav']");
    if (favBtn) favBtn.addEventListener("click", () => { LibraryState.selected.forEach((id) => Player.favorites.add(id)); LibraryState.selected.clear(); LibraryState.selecting = false; showSnackbar("Added to favourites."); Screens.render("songs"); });
    const clearBtn = el.querySelector("[data-act='clear']");
    if (clearBtn) clearBtn.addEventListener("click", () => { LibraryState.selected.clear(); LibraryState.selecting = false; Screens.render("songs"); });
    bindTrackRows(el);
  },
  albums() {
    const el = document.getElementById("screen-albums");
    el.innerHTML = `${this.topbar("Albums", { back: true, actions: [{ id: "sort", icon: "filter", label: "Sort albums" }] })}
      <p class="count-line">${ALBUMS.length} albums</p>
      <div class="album-grid">${ALBUMS.map((a) => `
        <button class="album-item" data-album="${a.id}">
          ${albumArtwork(a, 160, "rounded-16")}
          <div class="t">${escapeHtml(a.title)}</div><div class="s">${escapeHtml(a.artist)} • ${a.year}</div>
        </button>`).join("")}</div>`;
    bindTopbar(el, { sort: () => showSnackbar("Albums are sorted by title in this preview.") });
    el.querySelectorAll("[data-album]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "album", id: Number(b.dataset.album) } })));
  },
  artists() {
    const el = document.getElementById("screen-artists");
    el.innerHTML = `${this.topbar("Artists", { back: true })}
      <p class="count-line">${ARTISTS.length} artists</p>
      <div class="list">${ARTISTS.map((a) => `
        <button class="row-item" data-artist="${a.id}">
          <span class="artwork rounded-full" style="width:56px;height:56px;min-width:56px">${coverSVG(a.seed, ALBUM_MOTIF[a.albumIds[0]] || "fallback", a.name)}</span>
          <div class="meta"><div class="title">${escapeHtml(a.name)}</div><div class="sub">${a.albumIds.length} album${a.albumIds.length > 1 ? "s" : ""} • ${a.trackIds.length} tracks</div></div>
          <span class="chev">${icon("chevronRight", 20)}</span>
        </button>`).join("")}</div>`;
    bindTopbar(el, {});
    el.querySelectorAll("[data-artist]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "artist", id: Number(b.dataset.artist) } })));
  },
  playlists() {
    const el = document.getElementById("screen-playlists");
    el.innerHTML = `${this.topbar("Playlists", { back: true })}
      <p class="count-line">${PLAYLISTS.length} playlists</p>
      <div class="list">${PLAYLISTS.map((p) => `
        <button class="row-item" data-playlist="${p.id}">
          ${artworkHTML(p.seed, "fallback", p.name, 56, "rounded-12")}
          <div class="meta"><div class="title">${escapeHtml(p.name)}</div><div class="sub">${p.trackIds.length} tracks${p.smart ? " • Smart playlist" : ""}</div></div>
          <span class="chev">${icon("chevronRight", 20)}</span>
        </button>`).join("")}</div>`;
    bindTopbar(el, {});
    el.querySelectorAll("[data-playlist]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "playlist", id: Number(b.dataset.playlist) } })));
  },
  folders() {
    const el = document.getElementById("screen-folders");
    el.innerHTML = `${this.topbar("Folders", { back: true })}
      <p class="count-line">Music / ${FOLDERS.length} folders</p>
      ${FOLDERS.map((f) => {
        const name = f.path.split("/").filter(Boolean).pop();
        return `<div class="section-head"><h2 style="font-size:16px">${icon("folder", 18)} ${escapeHtml(name)}</h2><span class="sub">${f.trackIds.length} tracks</span></div>
        <div class="list">${tracksByIds(f.trackIds).map((t) => trackRowHTML(t)).join("")}</div>`;
      }).join("")}`;
    bindTopbar(el, {}); bindTrackRows(el);
  },
  genres() {
    const el = document.getElementById("screen-genres");
    el.innerHTML = `${this.topbar("Genres", { back: true })}
      <p class="count-line">${GENRES.length} genres</p>
      ${GENRES.map((g) => `
        <div style="padding:16px var(--gutter) 0">
          <div style="display:flex;align-items:baseline;gap:10px"><h3 style="font-size:19px;font-weight:750">${escapeHtml(g.name)}</h3><span style="color:var(--text-2);font-size:13px">${g.trackIds.length} tracks</span></div>
          <div style="margin-top:10px">${tracksByIds(g.trackIds).map((t) => trackRowHTML(t)).join("")}</div>
        </div>`).join("")}`;
    bindTopbar(el, {}); bindTrackRows(el);
  },
  detail() {
    const el = document.getElementById("screen-detail");
    const d = Nav.detail;
    let title = "", sub = "", seed = "#888888", motif = "fallback", count = "", tracks = [];
    if (d.kind === "album") { const a = albumById(d.id); title = a.title; sub = a.artist; seed = a.seed; motif = ALBUM_MOTIF[a.id]; count = `${a.trackIds.length} tracks • ${a.year}`; tracks = tracksByIds(a.trackIds); }
    if (d.kind === "artist") { const a = artistById(d.id); title = a.name; sub = "Artist"; seed = a.seed; motif = ALBUM_MOTIF[a.albumIds[0]]; count = `${a.albumIds.length} album${a.albumIds.length > 1 ? "s" : ""} • ${a.trackIds.length} tracks`; tracks = tracksByIds(a.trackIds); }
    if (d.kind === "playlist") { const p = playlistById(d.id); title = p.name; sub = p.smart ? "Smart playlist" : "Playlist"; seed = p.seed; motif = "fallback"; count = `${p.trackIds.length} tracks`; tracks = tracksByIds(p.trackIds); }
    el.innerHTML = `${this.topbar("", { back: true, actions: [{ id: "more", icon: "more", label: "More options" }] })}
      <div class="detail-hero"><div class="hero-glow"></div>
        <div class="hero-inner">
          ${artworkHTML(seed, motif, title, 208, "rounded-20")}
          <h2>${escapeHtml(title)}</h2><p>${escapeHtml(sub)}</p><p>${escapeHtml(count)}</p>
          <div class="detail-actions">
            <button class="btn btn-primary" data-playall>${icon("play", 18)} Play</button>
            <button class="btn btn-tonal" data-shuffleplay>${icon("shuffle", 18)} Shuffle</button>
          </div>
        </div>
      </div>
      <div class="list">${tracks.map((t, i) => trackRowHTML(t, { number: i + 1 })).join("")}</div>`;
    bindTopbar(el, { more: () => showSnackbar("More options — rename and editing are not part of this preview.") });
    el.querySelector("[data-playall]").addEventListener("click", () => { if (tracks.length) { Player.playTrack(tracks[0], tracks); Nav.go("player"); } });
    el.querySelector("[data-shuffleplay]").addEventListener("click", () => { if (tracks.length) { const s = [...tracks].sort(() => Math.random() - 0.5); Player.playTrack(s[0], s); Nav.go("player"); } });
    bindTrackRows(el);
  },
  player() {
    const el = document.getElementById("screen-player");
    const t = Player.currentTrack;
    if (!t) { el.innerHTML = `<div class="empty"><div class="empty-icon">${icon("library", 34)}</div><h3>Nothing playing</h3><p>Pick a song from your library to start simulated playback.</p><button class="btn btn-primary" data-browse>Browse songs</button></div>`;
      el.querySelector("[data-browse]").addEventListener("click", () => Nav.go("songs")); return; }
    const progress = t.durationMs ? Player.positionMs / t.durationMs : 0;
    const fav = Player.favorites.has(t.id);
    const repeatIcon = Player.repeat === "one" ? "repeatOne" : "repeat";
    const peaks = waveformPeaks(t.seed);
    el.innerHTML = `
      <div class="topbar"><button class="back-btn" data-back aria-label="Minimise player">${icon("chevronDown", 24)}</button><h1></h1>
        <button class="icon-btn" data-action="cast" aria-label="Cast (not available in this preview)">${icon("volume", 21)}</button>
        <button class="icon-btn" data-action="more" aria-label="More options">${icon("more", 21)}</button></div>
      <div class="player-wrap">
        <div class="player-art">${coverSVG(t.seed, ALBUM_MOTIF[t.albumId] || "fallback", t.title)}</div>
        <div class="player-side" style="width:100%">
          <div class="player-title-row"><div class="meta"><h2>${escapeHtml(t.title)}</h2><p>${escapeHtml(t.artist)} • ${escapeHtml(t.album)}</p></div>
            <button class="icon-btn fav-btn ${fav ? "active" : ""}" data-action="fav" aria-label="${fav ? "Remove from favourites" : "Add to favourites"}">${icon(fav ? "heartFilled" : "heart", 24)}</button></div>
          <div class="seek-row">
            <input type="range" class="seek" min="0" max="1000" value="${Math.round(progress * 1000)}" data-seek aria-label="Seek">
            <div class="seek-times"><span>${formatDuration(Player.positionMs)}</span><span>-${formatDuration(t.durationMs - Player.positionMs)}</span></div>
          </div>
          <div class="transport">
            <button class="icon-btn ${Player.shuffle ? "active" : ""}" data-action="shuffle" aria-label="Shuffle ${Player.shuffle ? "on" : "off"}">${icon("shuffle", 24)}</button>
            <button class="skip-btn" data-action="prev" aria-label="Previous track">${icon("skipBack", 32)}</button>
            <button class="play-btn" data-action="playpause" aria-label="${Player.isPlaying ? "Pause (simulated playback)" : "Play (simulated playback)"}">${icon(Player.isPlaying ? "pause" : "play", 30)}</button>
            <button class="skip-btn" data-action="next" aria-label="Next track">${icon("skipNext", 32)}</button>
            <button class="icon-btn ${Player.repeat !== "off" ? "active" : ""}" data-action="repeat" aria-label="Repeat ${Player.repeat}">${icon(repeatIcon, 24)}</button>
          </div>
          <div class="waveform" aria-hidden="true">${peaks.map((p, i) => `<span class="bar ${i / peaks.length <= progress ? "played" : ""}" style="height:${Math.round(p * 100)}%"></span>`).join("")}</div>
          <div class="player-links">
            <button class="btn btn-outline" data-action="lyrics">${icon("lyrics", 17)} Lyrics</button>
            <button class="btn btn-outline" data-action="audioinfo">${icon("info", 17)} Audio Info</button>
            <button class="btn btn-outline" data-action="queue">${icon("queue", 17)} Queue</button>
          </div>
          <p class="player-note">Simulated playback — no audio engine in this preview.</p>
        </div>
      </div>`;
    bindTopbar(el, {});
    el.querySelector("[data-seek]").addEventListener("input", (e) => Player.seekTo((Number(e.target.value) / 1000) * t.durationMs));
    el.querySelectorAll("[data-action]").forEach((b) => b.addEventListener("click", () => {
      const a = b.dataset.action;
      if (a === "playpause") Player.togglePlayPause();
      if (a === "next") Player.next();
      if (a === "prev") Player.previous();
      if (a === "shuffle") Player.toggleShuffle();
      if (a === "repeat") Player.cycleRepeat();
      if (a === "fav") { Player.toggleFavorite(t.id); b.classList.add("pop"); setTimeout(() => b.classList.remove("pop"), 160); }
      if (a === "lyrics") Nav.go("lyrics");
      if (a === "audioinfo") Nav.go("audioinfo");
      if (a === "queue") Nav.go("queue");
      if (a === "cast") showSnackbar("Cast is not available in this preview.");
      if (a === "more") showSnackbar("More options — coming with the real library.");
    }));
  },
};
function waveformPeaks(seed) {
  const base = parseInt(seed.slice(1), 16) % 10;
  return Array.from({ length: 48 }, (_, i) => {
    const v = Math.abs(Math.sin(i * 0.7 + base));
    return 0.25 + 0.65 * v * (0.6 + 0.4 * Math.abs(Math.sin(i * 0.31)));
  });
}
function bindTopbar(el, actions) {
  const back = el.querySelector("[data-back]");
  if (back) back.addEventListener("click", () => Nav.back());
  el.querySelectorAll("[data-action]").forEach((b) => {
    const fn = actions[b.dataset.action];
    if (fn) b.addEventListener("click", fn);
  });
}
function renderMiniPlayer() {
  const box = document.getElementById("miniplayer");
  const t = Player.currentTrack;
  if (!t) { box.innerHTML = ""; return; }
  const progress = t.durationMs ? Player.positionMs / t.durationMs : 0;
  box.innerHTML = `<div class="miniplayer-inner">
    <button class="tap" data-expand aria-label="Open Now Playing">
      ${trackArtwork(t, 48, "rounded-8")}
      <span style="flex:1;min-width:0"><span class="title" style="display:block">${escapeHtml(t.title)}</span><span class="sub" style="display:block">${escapeHtml(t.artist)}</span></span>
    </button>
    <button class="icon-btn" data-miniplay aria-label="${Player.isPlaying ? "Pause" : "Play"}">${icon(Player.isPlaying ? "pause" : "play", 22)}</button>
    <button class="icon-btn" data-mininext aria-label="Next track">${icon("skipNext", 22)}</button>
    <div class="miniplayer-progress"><i style="width:${(progress * 100).toFixed(1)}%"></i></div>
  </div>`;
  box.querySelector("[data-expand]").addEventListener("click", () => Nav.go("player"));
  box.querySelector("[data-miniplay]").addEventListener("click", () => Player.togglePlayPause());
  box.querySelector("[data-mininext]").addEventListener("click", () => Player.next());
}
