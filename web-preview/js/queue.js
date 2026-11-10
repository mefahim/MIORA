/* Queue screen — Phase 2.1 redesign with pointer-based drag reorder. */
"use strict";
const QueueScreens = {
  render() {
    const el = document.getElementById("screen-queue");
    const cur = Player.currentTrack;
    if (!cur) { el.innerHTML = `${Screens.topbar("Queue", { back: true })}<div class="empty"><div class="empty-icon">${icon("queue", 34)}</div><h3>Queue is empty</h3><p>Play a song and it will appear here.</p><button class="btn btn-primary" data-browse>Browse songs</button></div>`; bindTopbar(el, {}); el.querySelector("[data-browse]").addEventListener("click", () => Nav.go("songs")); return; }
    const upNext = Player.upNext;
    el.innerHTML = `
      ${Screens.topbar("Queue", { back: true })}
      <p class="queue-count">${Player.queue.length} tracks in queue</p>
      <div class="now-label">Now playing</div>
      <div style="padding:2px 8px">${trackRowHTML(cur, {})}</div>
      <div class="section-head"><h2>Up next</h2><span class="sub">Drag the handle to reorder</span></div>
      ${upNext.length ? `<div class="list" id="queue-list">` + upNext.map((t, offset) => {
        const qi = Player.currentIndex + 1 + offset;
        return `<div class="queue-row" data-qi="${qi}">
          <div style="flex:1;min-width:0">${trackRowHTML(t, { number: offset + 1 })}</div>
          <button class="qbtn" data-up="${qi}" ${qi <= Player.currentIndex + 1 ? "disabled" : ""} aria-label="Move ${escapeHtml(t.title)} up in queue">${icon("arrowUp", 18)}</button>
          <button class="qbtn" data-down="${qi}" ${qi >= Player.queue.length - 1 ? "disabled" : ""} aria-label="Move ${escapeHtml(t.title)} down in queue">${icon("arrowDown", 18)}</button>
          <button class="qbtn drag-handle" data-handle="${qi}" aria-label="Drag to reorder ${escapeHtml(t.title)}">${icon("grip", 18)}</button>
          <button class="qbtn" data-remove="${qi}" aria-label="Remove ${escapeHtml(t.title)} from queue">${icon("x", 18)}</button>
        </div>`;
      }).join("") + `</div>` : `<div class="empty"><div class="empty-icon">${icon("queue", 34)}</div><h3>Nothing up next</h3><p>The queue ends after the current track. Add songs from any list to keep the music going.</p></div>`}
      <div class="queue-actions">
        <button class="btn btn-outline" data-clear>${icon("trash", 17)} Clear queue</button>
        <button class="btn btn-tonal" data-shuffle>${icon("shuffle", 17)} Shuffle</button>
      </div>`;
    bindTopbar(el, {});
    el.querySelectorAll("[data-track]").forEach((row) => {
      row.addEventListener("click", (e) => { if (e.target.closest("[data-more]")) return; Player.playTrack(trackById(Number(row.dataset.track))); });
    });
    bindTrackRows(el);
    el.querySelectorAll("[data-up]").forEach((b) => b.addEventListener("click", () => Player.moveQueueItem(Number(b.dataset.up), Number(b.dataset.up) - 1)));
    el.querySelectorAll("[data-down]").forEach((b) => b.addEventListener("click", () => Player.moveQueueItem(Number(b.dataset.down), Number(b.dataset.down) + 1)));
    el.querySelectorAll("[data-remove]").forEach((b) => b.addEventListener("click", () => Player.removeQueueItem(Number(b.dataset.remove))));
    el.querySelector("[data-clear]").addEventListener("click", () => confirmDialog(
      "Clear queue?", "This removes all upcoming tracks. The current track keeps playing.", "Clear",
      () => { Player.clearQueue(); showSnackbar("Queue cleared."); },
    ));
    el.querySelector("[data-shuffle]").addEventListener("click", () => { Player.shuffleQueue(); showSnackbar("Queue shuffled."); });
    this.enableDrag(el);
  },
  enableDrag(el) {
    const list = el.querySelector("#queue-list");
    if (!list) return;
    let dragRow = null, dragIndex = -1, startY = 0;
    list.querySelectorAll("[data-handle]").forEach((handle) => {
      handle.addEventListener("pointerdown", (e) => {
        dragRow = handle.closest(".queue-row");
        dragIndex = Number(dragRow.dataset.qi);
        startY = e.clientY;
        dragRow.classList.add("dragging");
        handle.setPointerCapture(e.pointerId);
        const move = (ev) => {
          if (!dragRow) return;
          const dy = ev.clientY - startY;
          dragRow.style.transform = `translateY(${dy}px)`;
          const rows = [...list.querySelectorAll(".queue-row")].filter((r) => r !== dragRow);
          rows.forEach((r) => r.classList.remove("drag-over"));
          const under = document.elementFromPoint(ev.clientX, ev.clientY);
          const target = under ? under.closest(".queue-row") : null;
          if (target && target !== dragRow) target.classList.add("drag-over");
        };
        const up = (ev) => {
          handle.removeEventListener("pointermove", move);
          handle.removeEventListener("pointerup", up);
          if (!dragRow) return;
          const under = document.elementFromPoint(ev.clientX, ev.clientY);
          const target = under ? under.closest(".queue-row") : null;
          dragRow.classList.remove("dragging");
          dragRow.style.transform = "";
          list.querySelectorAll(".queue-row").forEach((r) => r.classList.remove("drag-over"));
          if (target && target !== dragRow) {
            const to = Number(target.dataset.qi);
            Player.moveQueueItem(dragIndex, to);
            showSnackbar("Queue reordered.");
          }
          dragRow = null;
        };
        handle.addEventListener("pointermove", move);
        handle.addEventListener("pointerup", up);
      });
    });
  },
};
