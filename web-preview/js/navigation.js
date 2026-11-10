/* Navigation — port of navigation/NavGraph.kt + MainActivity bottom nav.
   Bottom destinations: home / search / queue / settings. Others push onto a stack. */
"use strict";
const BOTTOM_SCREENS = ["home", "library", "search", "queue"];
const Nav = {
  stack: ["home"],
  detail: null, // { kind: 'album'|'artist'|'playlist', id }
  current() { return this.stack[this.stack.length - 1]; },
  go(screen, opts = {}) {
    if (opts.detail) this.detail = opts.detail;
    if (BOTTOM_SCREENS.includes(screen)) this.stack = [screen];
    else if (this.current() !== screen) this.stack.push(screen);
    this.render();
  },
  back() {
    if (this.stack.length > 1) { this.stack.pop(); this.render(); }
  },
  show(screen) {
    document.querySelectorAll(".screen").forEach((s) => s.classList.toggle("active", s.dataset.screen === screen));
    const bottom = BOTTOM_SCREENS.includes(screen) ? screen : null;
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.nav === bottom));
    document.querySelectorAll(".side-item[data-nav]").forEach((b) => b.classList.toggle("active", b.dataset.nav === (BOTTOM_SCREENS.includes(screen) ? screen : (screen === "settings" ? "settings" : bottom))));
    document.getElementById("miniplayer").style.display = screen === "player" ? "none" : "";
    document.getElementById("bottomnav").style.display = screen === "player" ? "none" : "";
    const el = document.querySelector(`.screen[data-screen="${screen}"]`);
    if (el) el.scrollTop = 0;
  },
  render() { const s = this.current(); Screens.render(s); this.show(s); },
};
function showSnackbar(msg) {
  const bar = document.getElementById("snackbar");
  bar.textContent = msg; bar.classList.add("show");
  clearTimeout(showSnackbar._t);
  showSnackbar._t = setTimeout(() => bar.classList.remove("show"), 2600);
}
function openModal(html) {
  const root = document.getElementById("modal-root");
  root.innerHTML = `<div class="modal-backdrop" role="dialog" aria-modal="true"><div class="modal">${html}</div></div>`;
  root.querySelector(".modal-backdrop").addEventListener("click", (e) => {
    if (e.target.classList.contains("modal-backdrop")) closeModal();
  });
}
function closeModal() { document.getElementById("modal-root").innerHTML = ""; }
function choiceDialog(title, options, selected, onSelect) {
  openModal(`<h3>${escapeHtml(title)}</h3>` + options.map(([label, value]) =>
    `<button class="choice ${value === selected ? "selected" : ""}" data-value="${escapeHtml(String(value))}"><span class="dot"></span>${escapeHtml(label)}</button>`
  ).join("") + `<div class="modal-actions"><button data-close>Done</button></div>`);
  document.querySelectorAll("#modal-root .choice").forEach((b) => b.addEventListener("click", () => {
    onSelect(b.dataset.value); closeModal();
  }));
  document.querySelector("#modal-root [data-close]").addEventListener("click", closeModal);
}
function confirmDialog(title, text, confirmLabel, onConfirm) {
  openModal(`<h3>${escapeHtml(title)}</h3><p style="color:var(--color-on-surface-variant);font-size:15px">${escapeHtml(text)}</p>
    <div class="modal-actions"><button data-close>Cancel</button><button data-confirm>${escapeHtml(confirmLabel)}</button></div>`);
  document.querySelector("#modal-root [data-close]").addEventListener("click", closeModal);
  document.querySelector("#modal-root [data-confirm]").addEventListener("click", () => { closeModal(); onConfirm(); });
}
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
