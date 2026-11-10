/**
 * MIORA Release Configuration
 * Single source of truth for public release metadata and download links.
 */
const MIORA_CONFIG = {
  productName: "MIORA",
  fullMeaning: "Music in Our Real Audio",
  tagline: "Your music. Your moments.",
  version: "v0.1.0-beta.1",
  versionCode: 2,
  releaseDate: "2026-11-10",
  apkFileName: "MIORA-v0.1.0-beta.1.apk",
  apkSize: "17.07 MB",
  apkSizeBytes: 17903746,
  sha256: "d125c381713f5b802ec2452733d8426ce90d8b2a8a7e0dd7ba69f923e28eed90",
  minAndroid: "Android 8.0+ (API 26)",
  targetAndroid: "Android 15 (API 35)",
  downloadUrl: "https://github.com/mefahim/MIORA/releases/download/v0.1.0-beta.1/MIORA-v0.1.0-beta.1.apk",
  checksumUrl: "https://github.com/mefahim/MIORA/releases/download/v0.1.0-beta.1/MIORA-v0.1.0-beta.1.apk.sha256",
  releasesUrl: "https://github.com/mefahim/MIORA/releases",
  releaseNotesUrl: "https://github.com/mefahim/MIORA/releases/tag/v0.1.0-beta.1",
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
