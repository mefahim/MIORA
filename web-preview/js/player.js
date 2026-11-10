/* Simulated playback — port of player/PlayerViewModel.kt.
   A 500 ms ticker advances position only while playing; no audio is produced. */
"use strict";
const Player = {
  queue: [...TRACKS],
  currentIndex: 0,
  isPlaying: false,
  positionMs: 0,
  shuffle: false,
  repeat: "off", // off | all | one
  favorites: new Set(TRACKS.filter((t) => t.favorite).map((t) => t.id)),
  timer: null,
  listeners: [],
  get currentTrack() { return this.queue[this.currentIndex] || null; },
  get upNext() { return this.queue.slice(this.currentIndex + 1); },
  onChange(fn) { this.listeners.push(fn); },
  emit() { this.listeners.forEach((fn) => fn()); ThemeStore.setArtworkSeed(this.currentTrack ? this.currentTrack.seed : null); },
  playTrack(track, queue) {
    if (queue) this.queue = [...queue];
    const idx = this.queue.findIndex((t) => t.id === track.id);
    this.currentIndex = idx >= 0 ? idx : 0;
    this.positionMs = 0; this.isPlaying = true;
    this.startTicker(); this.emit();
  },
  togglePlayPause() {
    this.isPlaying = !this.isPlaying;
    if (this.isPlaying) this.startTicker(); else this.stopTicker();
    this.emit();
  },
  next() {
    if (!this.queue.length) return;
    this.currentIndex = this.shuffle
      ? this.queue.map((_, i) => i).filter((i) => i !== this.currentIndex)[Math.floor(Math.random() * (this.queue.length - 1))]
      : (this.currentIndex + 1) % this.queue.length;
    this.positionMs = 0; this.isPlaying = true; this.startTicker(); this.emit();
  },
  previous() {
    if (!this.queue.length) return;
    if (this.positionMs > 3000) { this.seekTo(0); return; }
    this.currentIndex = this.currentIndex === 0 ? this.queue.length - 1 : this.currentIndex - 1;
    this.positionMs = 0; this.isPlaying = true; this.startTicker(); this.emit();
  },
  seekTo(ms) {
    const t = this.currentTrack; if (!t) return;
    this.positionMs = Math.max(0, Math.min(ms, t.durationMs)); this.emit();
  },
  toggleShuffle() { this.shuffle = !this.shuffle; this.emit(); },
  cycleRepeat() {
    this.repeat = this.repeat === "off" ? "all" : this.repeat === "all" ? "one" : "off";
    this.emit();
  },
  toggleFavorite(id) {
    if (this.favorites.has(id)) this.favorites.delete(id); else this.favorites.add(id);
    this.emit();
  },
  moveQueueItem(from, to) {
    if (from < 0 || to < 0 || from >= this.queue.length || to >= this.queue.length || from === to) return;
    const item = this.queue.splice(from, 1)[0];
    this.queue.splice(to, 0, item);
    if (from === this.currentIndex) this.currentIndex = to;
    else if (from < this.currentIndex && to >= this.currentIndex) this.currentIndex -= 1;
    else if (from > this.currentIndex && to <= this.currentIndex) this.currentIndex += 1;
    this.emit();
  },
  removeQueueItem(index) {
    if (index < 0 || index >= this.queue.length || this.queue.length <= 1) return;
    this.queue.splice(index, 1);
    if (index < this.currentIndex) this.currentIndex -= 1;
    else if (index === this.currentIndex) { this.currentIndex = Math.min(this.currentIndex, this.queue.length - 1); this.positionMs = 0; }
    this.emit();
  },
  clearQueue() {
    const cur = this.currentTrack;
    this.queue = cur ? [cur] : []; this.currentIndex = 0; this.emit();
  },
  shuffleQueue() {
    const cur = this.currentTrack; if (!cur) return;
    const rest = this.queue.filter((t) => t.id !== cur.id);
    for (let i = rest.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [rest[i], rest[j]] = [rest[j], rest[i]]; }
    this.queue = [cur, ...rest]; this.currentIndex = 0; this.emit();
  },
  startTicker() {
    this.stopTicker();
    this.timer = setInterval(() => {
      if (!this.isPlaying) return;
      const t = this.currentTrack; if (!t) return;
      const next = this.positionMs + 500;
      if (next >= t.durationMs) {
        if (this.repeat === "one") this.positionMs = 0;
        else if (this.repeat === "all") this.next();
        else if (this.currentIndex >= this.queue.length - 1) { this.isPlaying = false; this.positionMs = t.durationMs; this.stopTicker(); }
        else this.next();
      } else this.positionMs = next;
      this.emit();
    }, 500);
  },
  stopTicker() { if (this.timer) { clearInterval(this.timer); this.timer = null; } },
};
