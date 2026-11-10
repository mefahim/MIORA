/* Search screen — Phase 2.1 redesign. */
"use strict";
const SearchState = { query: "", history: ["Sky Dreams", "Aurora Lane", "Midnight"] };
const SearchScreens = {
  render() {
    const el = document.getElementById("screen-search");
    const q = SearchState.query;
    const results = q.trim() ? searchLibrary(q) : null;
    let body = "";
    if (!results) {
      body = SearchState.history.length ? `
        <div class="section-head"><h2 style="font-size:16px">Recent searches</h2><button id="clear-history">Clear</button></div>
        ${SearchState.history.map((term) => `<button class="history-row" data-term="${escapeHtml(term)}"><span class="ic">${icon("clock", 18)}</span> ${escapeHtml(term)}</button>`).join("")}`
        : `<div class="empty"><div class="empty-icon">${icon("search", 34)}</div><h3>Search your library</h3><p>Try a song, album, artist or playlist name — results appear as you type.</p></div>`;
    } else if (!results.tracks.length && !results.albums.length && !results.artists.length && !results.playlists.length) {
      body = `<div class="empty"><div class="empty-icon">${icon("search", 34)}</div><h3>No results for “${escapeHtml(q)}”</h3><p>Nothing in the sample library matches. Check the spelling or try an artist name like Aurora Lane.</p><button class="btn btn-tonal" data-clearq>Clear search</button></div>`;
    } else {
      body = `
        ${results.tracks.length ? `<div class="section-head"><h2>Songs</h2><span class="sub">${results.tracks.length}</span></div><div class="list">${results.tracks.map((t) => trackRowHTML(t)).join("")}</div>` : ""}
        ${results.albums.length ? `<div class="section-head"><h2>Albums</h2></div>${results.albums.map((a) => `
          <button class="row-item" data-album="${a.id}">${albumArtwork(a, 48, "rounded-8")}<span class="meta"><span class="title" style="display:block">${escapeHtml(a.title)}</span><span class="sub" style="display:block">${escapeHtml(a.artist)}</span></span><span class="chev">${icon("chevronRight", 18)}</span></button>`).join("")}` : ""}
        ${results.artists.length ? `<div class="section-head"><h2>Artists</h2></div>${results.artists.map((a) => `
          <button class="row-item" data-artist="${a.id}"><span class="artwork rounded-full" style="width:48px;height:48px;min-width:48px">${coverSVG(a.seed, ALBUM_MOTIF[a.albumIds[0]] || "fallback", a.name)}</span><span class="meta"><span class="title" style="display:block">${escapeHtml(a.name)}</span></span><span class="chev">${icon("chevronRight", 18)}</span></button>`).join("")}` : ""}
        ${results.playlists.length ? `<div class="section-head"><h2>Playlists</h2></div>${results.playlists.map((p) => `
          <button class="row-item" data-playlist="${p.id}">${artworkHTML(p.seed, "fallback", p.name, 48, "rounded-8")}<span class="meta"><span class="title" style="display:block">${escapeHtml(p.name)}</span></span><span class="chev">${icon("chevronRight", 18)}</span></button>`).join("")}` : ""}`;
    }
    el.innerHTML = `
      ${Screens.topbar("Search", { back: true })}
      <div class="search-box"><div class="search-field"><span class="ic">${icon("search", 20)}</span><input id="search-input" type="search" placeholder="Songs, albums, artists…" value="${escapeHtml(q)}" aria-label="Search library" autocomplete="off">${q ? `<button class="icon-btn" id="search-clear" aria-label="Clear search" style="width:36px;height:36px">${icon("x", 18)}</button>` : ""}</div></div>
      ${body}`;
    bindTopbar(el, {});
    const input = el.querySelector("#search-input");
    input.addEventListener("input", () => { SearchState.query = input.value; SearchScreens.render(); const n = el.querySelector("#search-input"); n.focus(); n.setSelectionRange(n.value.length, n.value.length); });
    input.addEventListener("keydown", (e) => { if (e.key === "Enter") SearchScreens.submit(); });
    const clearBtn = el.querySelector("#search-clear");
    if (clearBtn) clearBtn.addEventListener("click", () => { SearchState.query = ""; SearchScreens.render(); });
    const clearQ = el.querySelector("[data-clearq]");
    if (clearQ) clearQ.addEventListener("click", () => { SearchState.query = ""; SearchScreens.render(); });
    const clear = el.querySelector("#clear-history");
    if (clear) clear.addEventListener("click", () => { SearchState.history = []; SearchScreens.render(); });
    el.querySelectorAll("[data-term]").forEach((b) => b.addEventListener("click", () => { SearchState.query = b.dataset.term; SearchScreens.render(); }));
    el.querySelectorAll("[data-album]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "album", id: Number(b.dataset.album) } })));
    el.querySelectorAll("[data-artist]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "artist", id: Number(b.dataset.artist) } })));
    el.querySelectorAll("[data-playlist]").forEach((b) => b.addEventListener("click", () => Nav.go("detail", { detail: { kind: "playlist", id: Number(b.dataset.playlist) } })));
    bindTrackRows(el);
  },
  submit() {
    const q = SearchState.query.trim();
    if (!q) return;
    SearchState.history = [q, ...SearchState.history.filter((h) => h !== q)].slice(0, 8);
    SearchScreens.render();
  },
};
