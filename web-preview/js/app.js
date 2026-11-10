/* App boot — Phase 2.1: wires bottom nav + sidebar, viewport switcher,
   theme toolbar, brand/icon injection and the player change subscription. */
"use strict";
const NAV_ICONS = { home: "home", library: "library", search: "search", queue: "queue", settings: "settings" };
document.addEventListener("DOMContentLoaded", () => {
  // Inject icons into the shell (nav items, sidebar, brand marks).
  document.querySelectorAll(".nav-item").forEach((b) => { const ic = b.querySelector(".nav-icon"); if (ic) ic.innerHTML = icon(NAV_ICONS[b.dataset.nav] || "home", 23); });
  document.querySelectorAll(".side-item").forEach((b) => { const ic = b.querySelector(".ic"); if (ic) ic.innerHTML = icon(NAV_ICONS[b.dataset.nav] || "home", 21); });
  const logo = document.getElementById("toolbar-logo");
  if (logo) logo.innerHTML = icon("audio", 17);
  const mark = document.getElementById("brand-mark");
  if (mark) mark.innerHTML = icon("audio", 20);
  ThemeStore.apply();
  ThemeStore.setArtworkSeed(Player.currentTrack ? Player.currentTrack.seed : null);
  document.querySelectorAll("[data-nav]").forEach((b) => b.addEventListener("click", () => Nav.go(b.dataset.nav)));
  document.querySelectorAll("[data-viewport]").forEach((b) => b.addEventListener("click", () => {
    document.querySelectorAll("[data-viewport]").forEach((x) => { x.classList.toggle("active", x === b); x.setAttribute("aria-pressed", String(x === b)); });
    const phone = document.getElementById("phone");
    phone.classList.remove("viewport-mobile", "viewport-tablet", "viewport-desktop");
    phone.classList.add("viewport-" + b.dataset.viewport);
    Screens.render(Nav.current());
  }));
  document.getElementById("toolbar-theme").addEventListener("click", () => { ThemeStore.toggleDark(); if (Nav.current() === "settings") Screens.render("settings"); });
  let tick = 0;
  Player.onChange(() => {
    const now = Date.now();
    if (Nav.current() === "player" || Nav.current() === "queue" || Nav.current() === "lyrics") {
      if (now - tick > 450) { tick = now; Screens.render(Nav.current()); }
    } else {
      renderMiniPlayer();
      if (Nav.current() === "home" || Nav.current() === "songs") Screens.render(Nav.current());
    }
  });
  Nav.render();
});
