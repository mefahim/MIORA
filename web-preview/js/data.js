/* Sample library — verbatim port of data/MockData.kt (same 20 tracks, albums,
   artists, playlists, genres, folders). Artwork seeds are the same hex values. */
"use strict";
const SEEDS = {
  blue: "#3b82f6", amber: "#f59e0b", violet: "#8b5cf6", cyan: "#06b6d4",
  red: "#ef4444", indigo: "#6366f1", green: "#10b981",
};
const TRACKS = [
  { id: 1, title: "Sky Dreams", artist: "Aurora Lane", album: "Dreamscape", albumId: 1, artistId: 1, durationMs: 232000, trackNumber: 1, year: 2024, genre: "Ambient", folder: "/Music/Aurora Lane/Dreamscape", favorite: true, seed: SEEDS.blue, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16,
    lyrics: [[0, "Walking where the morning breaks"], [8000, "Golden light across the hills"], [16000, "We are made of sky dreams"], [24000, "Drifting on a quiet wind"]] },
  { id: 2, title: "Better Tomorrow", artist: "The Horizon", album: "Better Tomorrow", albumId: 2, artistId: 2, durationMs: 198000, trackNumber: 1, year: 2024, genre: "Pop", folder: "/Music/The Horizon/Better Tomorrow", favorite: false, seed: SEEDS.amber, format: "MP3", bitrateKbps: 320, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 3, title: "Midnight Lights", artist: "Nova Echo", album: "Midnight Lights", albumId: 3, artistId: 3, durationMs: 214000, trackNumber: 1, year: 2023, genre: "Electronic", folder: "/Music/Nova Echo/Midnight Lights", favorite: true, seed: SEEDS.violet, format: "FLAC", bitrateKbps: 1024, sampleRateHz: 48000, bitDepth: 24, lyrics: [] },
  { id: 4, title: "Ocean Breeze", artist: "Blue Horizon", album: "Ocean Breeze", albumId: 4, artistId: 4, durationMs: 187000, trackNumber: 1, year: 2024, genre: "Chill", folder: "/Music/Blue Horizon/Ocean Breeze", favorite: false, seed: SEEDS.cyan, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 5, title: "Feel the Beat", artist: "Pulse Drive", album: "Feel the Beat", albumId: 5, artistId: 5, durationMs: 176000, trackNumber: 1, year: 2023, genre: "Dance", folder: "/Music/Pulse Drive/Feel the Beat", favorite: false, seed: SEEDS.red, format: "MP3", bitrateKbps: 320, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 6, title: "City Lights", artist: "Urban Theory", album: "City Lights", albumId: 6, artistId: 6, durationMs: 203000, trackNumber: 1, year: 2024, genre: "Electronic", folder: "/Music/Urban Theory/City Lights", favorite: false, seed: SEEDS.indigo, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 7, title: "Infinite Moment", artist: "Dream State", album: "Infinite Moment", albumId: 7, artistId: 7, durationMs: 241000, trackNumber: 1, year: 2023, genre: "Ambient", folder: "/Music/Dream State/Infinite Moment", favorite: true, seed: SEEDS.green, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 8, title: "Golden Hour", artist: "Aurora Lane", album: "Dreamscape", albumId: 1, artistId: 1, durationMs: 209000, trackNumber: 2, year: 2024, genre: "Ambient", folder: "/Music/Aurora Lane/Dreamscape", favorite: false, seed: SEEDS.blue, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 9, title: "Northern Sky", artist: "Aurora Lane", album: "Dreamscape", albumId: 1, artistId: 1, durationMs: 226000, trackNumber: 3, year: 2024, genre: "Ambient", folder: "/Music/Aurora Lane/Dreamscape", favorite: false, seed: SEEDS.blue, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 10, title: "Rising Sun", artist: "The Horizon", album: "Better Tomorrow", albumId: 2, artistId: 2, durationMs: 192000, trackNumber: 2, year: 2024, genre: "Pop", folder: "/Music/The Horizon/Better Tomorrow", favorite: false, seed: SEEDS.amber, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 11, title: "Neon Streets", artist: "Nova Echo", album: "Midnight Lights", albumId: 3, artistId: 3, durationMs: 205000, trackNumber: 2, year: 2023, genre: "Electronic", folder: "/Music/Nova Echo/Midnight Lights", favorite: false, seed: SEEDS.violet, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 12, title: "Tide Song", artist: "Blue Horizon", album: "Ocean Breeze", albumId: 4, artistId: 4, durationMs: 174000, trackNumber: 2, year: 2024, genre: "Chill", folder: "/Music/Blue Horizon/Ocean Breeze", favorite: false, seed: SEEDS.cyan, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 13, title: "Night Drive", artist: "Pulse Drive", album: "Feel the Beat", albumId: 5, artistId: 5, durationMs: 189000, trackNumber: 2, year: 2023, genre: "Dance", folder: "/Music/Pulse Drive/Feel the Beat", favorite: true, seed: SEEDS.red, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 14, title: "Metro Lines", artist: "Urban Theory", album: "City Lights", albumId: 6, artistId: 6, durationMs: 211000, trackNumber: 2, year: 2024, genre: "Electronic", folder: "/Music/Urban Theory/City Lights", favorite: false, seed: SEEDS.indigo, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 15, title: "Slow Orbit", artist: "Dream State", album: "Infinite Moment", albumId: 7, artistId: 7, durationMs: 238000, trackNumber: 2, year: 2023, genre: "Ambient", folder: "/Music/Dream State/Infinite Moment", favorite: false, seed: SEEDS.green, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 16, title: "Paper Planes", artist: "The Horizon", album: "Better Tomorrow", albumId: 2, artistId: 2, durationMs: 183000, trackNumber: 3, year: 2024, genre: "Pop", folder: "/Music/The Horizon/Better Tomorrow", favorite: false, seed: SEEDS.amber, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 17, title: "Starlight Run", artist: "Nova Echo", album: "Midnight Lights", albumId: 3, artistId: 3, durationMs: 197000, trackNumber: 3, year: 2023, genre: "Electronic", folder: "/Music/Nova Echo/Midnight Lights", favorite: false, seed: SEEDS.violet, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 18, title: "Salt Air", artist: "Blue Horizon", album: "Ocean Breeze", albumId: 4, artistId: 4, durationMs: 169000, trackNumber: 3, year: 2024, genre: "Chill", folder: "/Music/Blue Horizon/Ocean Breeze", favorite: false, seed: SEEDS.cyan, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 19, title: "Afterglow", artist: "Aurora Lane", album: "Dreamscape", albumId: 1, artistId: 1, durationMs: 219000, trackNumber: 4, year: 2024, genre: "Ambient", folder: "/Music/Aurora Lane/Dreamscape", favorite: false, seed: SEEDS.blue, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
  { id: 20, title: "Uptown Static", artist: "Urban Theory", album: "City Lights", albumId: 6, artistId: 6, durationMs: 194000, trackNumber: 3, year: 2024, genre: "Electronic", folder: "/Music/Urban Theory/City Lights", favorite: false, seed: SEEDS.indigo, format: "FLAC", bitrateKbps: 912, sampleRateHz: 44100, bitDepth: 16, lyrics: [] },
];
const ALBUMS = [
  { id: 1, title: "Dreamscape", artist: "Aurora Lane", artistId: 1, year: 2024, seed: SEEDS.blue, trackIds: [1, 8, 9, 19] },
  { id: 2, title: "Better Tomorrow", artist: "The Horizon", artistId: 2, year: 2024, seed: SEEDS.amber, trackIds: [2, 10, 16] },
  { id: 3, title: "Midnight Lights", artist: "Nova Echo", artistId: 3, year: 2023, seed: SEEDS.violet, trackIds: [3, 11, 17] },
  { id: 4, title: "Ocean Breeze", artist: "Blue Horizon", artistId: 4, year: 2024, seed: SEEDS.cyan, trackIds: [4, 12, 18] },
  { id: 5, title: "Feel the Beat", artist: "Pulse Drive", artistId: 5, year: 2023, seed: SEEDS.red, trackIds: [5, 13] },
  { id: 6, title: "City Lights", artist: "Urban Theory", artistId: 6, year: 2024, seed: SEEDS.indigo, trackIds: [6, 14, 20] },
  { id: 7, title: "Infinite Moment", artist: "Dream State", artistId: 7, year: 2023, seed: SEEDS.green, trackIds: [7, 15] },
];
const ARTISTS = [
  { id: 1, name: "Aurora Lane", seed: SEEDS.blue, albumIds: [1], trackIds: [1, 8, 9, 19] },
  { id: 2, name: "The Horizon", seed: SEEDS.amber, albumIds: [2], trackIds: [2, 10, 16] },
  { id: 3, name: "Nova Echo", seed: SEEDS.violet, albumIds: [3], trackIds: [3, 11, 17] },
  { id: 4, name: "Blue Horizon", seed: SEEDS.cyan, albumIds: [4], trackIds: [4, 12, 18] },
  { id: 5, name: "Pulse Drive", seed: SEEDS.red, albumIds: [5], trackIds: [5, 13] },
  { id: 6, name: "Urban Theory", seed: SEEDS.indigo, albumIds: [6], trackIds: [6, 14, 20] },
  { id: 7, name: "Dream State", seed: SEEDS.green, albumIds: [7], trackIds: [7, 15] },
];
const PLAYLISTS = [
  { id: 1, name: "Chill Vibes", seed: SEEDS.cyan, trackIds: [1, 4, 8, 12, 15, 18, 19], smart: false },
  { id: 2, name: "Focus Flow", seed: SEEDS.green, trackIds: [1, 7, 8, 9, 15, 19], smart: false },
  { id: 3, name: "Dreamscape", seed: SEEDS.blue, trackIds: [1, 3, 7, 8, 9, 11, 19], smart: false },
  { id: 4, name: "Recently Played", seed: SEEDS.amber, trackIds: [1, 2, 3, 4, 5, 6], smart: true },
  { id: 5, name: "Favourites", seed: SEEDS.red, trackIds: [1, 3, 7, 13], smart: true },
];
const GENRES = [
  { name: "Ambient", trackIds: [1, 7, 8, 9, 15, 19] },
  { name: "Pop", trackIds: [2, 10, 16] },
  { name: "Electronic", trackIds: [3, 6, 11, 14, 17, 20] },
  { name: "Chill", trackIds: [4, 12, 18] },
  { name: "Dance", trackIds: [5, 13] },
];
const FOLDERS = [
  { path: "/Music/Aurora Lane/Dreamscape", trackIds: [1, 8, 9, 19] },
  { path: "/Music/The Horizon/Better Tomorrow", trackIds: [2, 10, 16] },
  { path: "/Music/Nova Echo/Midnight Lights", trackIds: [3, 11, 17] },
  { path: "/Music/Blue Horizon/Ocean Breeze", trackIds: [4, 12, 18] },
  { path: "/Music/Pulse Drive/Feel the Beat", trackIds: [5, 13] },
  { path: "/Music/Urban Theory/City Lights", trackIds: [6, 14, 20] },
  { path: "/Music/Dream State/Infinite Moment", trackIds: [7, 15] },
];
const RECENTLY_PLAYED_IDS = [1, 2, 3, 4, 5, 6];
const trackById = (id) => TRACKS.find((t) => t.id === id);
const albumById = (id) => ALBUMS.find((a) => a.id === id);
const artistById = (id) => ARTISTS.find((a) => a.id === id);
const playlistById = (id) => PLAYLISTS.find((p) => p.id === id);
const tracksByIds = (ids) => ids.map(trackById).filter(Boolean);
function searchLibrary(q) {
  const query = q.trim().toLowerCase();
  if (!query) return { tracks: [], albums: [], artists: [], playlists: [] };
  return {
    tracks: TRACKS.filter((t) => (t.title + " " + t.artist + " " + t.album).toLowerCase().includes(query)),
    albums: ALBUMS.filter((a) => (a.title + " " + a.artist).toLowerCase().includes(query)),
    artists: ARTISTS.filter((a) => a.name.toLowerCase().includes(query)),
    playlists: PLAYLISTS.filter((p) => p.name.toLowerCase().includes(query)),
  };
}
function formatDuration(ms) {
  const s = Math.floor(ms / 1000);
  return Math.floor(s / 60) + ":" + String(s % 60).padStart(2, "0");
}
