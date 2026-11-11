/**
 * MIORA Release Configuration
 * Single source of truth for public release metadata and download links.
 */
const MIORA_CONFIG = {
  productName: "MIORA",
  fullMeaning: "Music in Our Real Audio",
  tagline: "Your music. Your moments.",
  version: "v0.1.0",
  versionCode: 3,
  releaseDate: "2026-11-11",
  apkFileName: "MIORA-v0.1.0.apk",
  apkSize: "20.51 MB",
  apkSizeBytes: 21508520,
  sha256: "2fe958326f49537760c47437bbaadfc3627a14fb879ae4ea52d14ddcdaa47381",
  minAndroid: "Android 8.0+ (API 26)",
  targetAndroid: "Android 15 (API 35)",
  downloadUrl: "https://github.com/mefahim/MIORA/releases/download/v0.1.0/MIORA-v0.1.0.apk",
  checksumUrl: "https://github.com/mefahim/MIORA/releases/download/v0.1.0/MIORA-v0.1.0.apk.sha256",
  releasesUrl: "https://github.com/mefahim/MIORA/releases",
  releaseNotesUrl: "https://github.com/mefahim/MIORA/releases/tag/v0.1.0",
  repoUrl: "https://github.com/mefahim/MIORA",
  issuesUrl: "https://github.com/mefahim/MIORA/issues",
  licenseUrl: "https://github.com/mefahim/MIORA/blob/main/LICENSE",
  changelogUrl: "https://github.com/mefahim/MIORA/blob/main/CHANGELOG.md",
  architectureUrl: "https://github.com/mefahim/MIORA/blob/main/docs/ARCHITECTURE.md",
  securityUrl: "https://github.com/mefahim/MIORA/blob/main/docs/SECURITY.md"
};

// Freeze object to prevent accidental runtime mutation
Object.freeze(MIORA_CONFIG);

if (typeof window !== "undefined") {
  window.MIORA_CONFIG = MIORA_CONFIG;
}
if (typeof module !== "undefined" && module.exports) {
  module.exports = MIORA_CONFIG;
}
