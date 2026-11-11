#!/usr/bin/env node

/**
 * MIORA Landing Page Verification & QA Test Suite
 * Validates file structure, HTML semantics, accessibility, links, CSS, and JS hydration.
 */

const fs = require('fs');
const path = require('path');

const DOCS_DIR = path.resolve(__dirname, '../docs');
const INDEX_HTML_PATH = path.join(DOCS_DIR, 'index.html');
const CSS_PATH = path.join(DOCS_DIR, 'css/main.css');
const JS_MAIN_PATH = path.join(DOCS_DIR, 'js/main.js');
const JS_CONFIG_PATH = path.join(DOCS_DIR, 'js/release-config.js');

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

function assert(condition, testName) {
  totalTests++;
  if (condition) {
    passedTests++;
    console.log(`  ✓ ${testName}`);
  } else {
    failedTests++;
    console.error(`  ✗ FAIL: ${testName}`);
  }
}

console.log('====================================================');
console.log('🧪 MIORA Landing Page QA Test Suite');
console.log('====================================================\n');

// 1. File Structure & Core Assets
console.log('1. Checking File Structure & Asset Integrity...');
assert(fs.existsSync(INDEX_HTML_PATH), 'docs/index.html exists');
assert(fs.existsSync(CSS_PATH), 'docs/css/main.css exists');
assert(fs.existsSync(JS_MAIN_PATH), 'docs/js/main.js exists');
assert(fs.existsSync(JS_CONFIG_PATH), 'docs/js/release-config.js exists');
assert(fs.existsSync(path.join(DOCS_DIR, '.nojekyll')), 'docs/.nojekyll exists');
assert(fs.existsSync(path.join(DOCS_DIR, 'robots.txt')), 'docs/robots.txt exists');
assert(fs.existsSync(path.join(DOCS_DIR, 'sitemap.xml')), 'docs/sitemap.xml exists');

const requiredAssets = [
  'assets/miora-logo.svg',
  'assets/favicon.svg',
  'assets/artwork-horizon.svg',
  'assets/artwork-drift.svg',
  'assets/artwork-midnight.svg',
  'assets/og-image.svg'
];

requiredAssets.forEach(asset => {
  const assetPath = path.join(DOCS_DIR, asset);
  assert(fs.existsSync(assetPath) && fs.statSync(assetPath).size > 100, `Asset ${asset} exists and has valid size`);
});

// 2. Release Configuration Validation
console.log('\n2. Validating Release Configuration...');
const releaseConfig = require(JS_CONFIG_PATH);
assert(releaseConfig.version === 'v0.1.0', `Version is v0.1.0 (got ${releaseConfig.version})`);
assert(releaseConfig.versionCode === 3, `VersionCode is 3 (got ${releaseConfig.versionCode})`);
assert(releaseConfig.sha256 === '2fe958326f49537760c47437bbaadfc3627a14fb879ae4ea52d14ddcdaa47381', 'SHA-256 matches verified APK hash');
assert(releaseConfig.apkSize === '20.51 MB', `APK size matches verified binary (got ${releaseConfig.apkSize})`);
assert(releaseConfig.downloadUrl.includes('v0.1.0/MIORA-v0.1.0.apk'), 'Direct download URL is well-formed');
assert(releaseConfig.repoUrl === 'https://github.com/mefahim/MIORA', 'Repository URL points to https://github.com/mefahim/MIORA');

// 3. HTML Content, Metadata & Accessibility
console.log('\n3. Validating HTML Content & Metadata...');
const html = fs.readFileSync(INDEX_HTML_PATH, 'utf8');

assert(html.includes('<!DOCTYPE html>'), 'Valid DOCTYPE declaration');
assert(html.includes('<html lang="en">'), 'html element has lang="en"');
assert(html.includes('<title>MIORA — Your Music. Your Moments.</title>'), 'Correct page title');
assert(html.includes('name="viewport" content="width=device-width, initial-scale=1.0"'), 'Responsive viewport tag');
assert(html.includes('name="description"'), 'Meta description present');
assert(html.includes('property="og:title"'), 'Open Graph title present');
assert(html.includes('property="og:image"'), 'Open Graph image present');
assert(html.includes('name="twitter:card"'), 'Twitter card tag present');
assert(html.includes('href="https://mefahim.github.io/MIORA/"'), 'Canonical URL points to https://mefahim.github.io/MIORA/');

// 4. Accessibility & Semantic Structure
console.log('\n4. Validating Accessibility & Landmarks...');
assert(html.includes('class="skip-link"'), 'Skip navigation link present');
assert(html.includes('role="banner"'), 'Header has role="banner"');
assert(html.includes('role="contentinfo"'), 'Footer has role="contentinfo"');
assert(html.includes('<main id="main-content">'), 'Main landmark has id="main-content"');

// Check all img tags have alt attribute
const imgTags = html.match(/<img[^>]+>/g) || [];
let allImgHaveAlt = imgTags.length > 0;
imgTags.forEach(img => {
  if (!img.includes('alt="')) allImgHaveAlt = false;
});
assert(allImgHaveAlt, `All ${imgTags.length} images have explicit alt text`);

// 5. Internal Anchor Links Validation
console.log('\n5. Validating Anchor Targets & Internal Links...');
const internalAnchorMatches = html.match(/href="#([^"]+)"/g) || [];
const anchorTargets = [...new Set(internalAnchorMatches.map(m => m.replace(/href="#|"/g, '')))];

anchorTargets.forEach(target => {
  if (target === '') return;
  const hasTargetId = html.includes(`id="${target}"`);
  assert(hasTargetId, `Internal anchor #${target} has matching element id="${target}"`);
});

// 6. CSS Design System & Responsive Rules
console.log('\n6. Validating CSS Stylesheet & Breakpoints...');
const css = fs.readFileSync(CSS_PATH, 'utf8');

assert(css.includes('--accent: #009587;'), 'Signature Teal accent defined');
assert(css.includes('--bg-canvas: #090C11;'), 'Dark canvas background token defined');
assert(css.includes('--text-primary: #F5F4F0;'), 'Warm off-white text token defined');
assert(css.includes('@media (max-width: 1024px)'), 'Tablet/Medium desktop breakpoint defined');
assert(css.includes('@media (max-width: 768px)'), 'Standard mobile breakpoint defined');
assert(css.includes('@media (max-width: 480px)'), 'Small mobile breakpoint defined');
assert(css.includes('@media (prefers-reduced-motion: reduce)'), 'prefers-reduced-motion accessibility query defined');
assert(css.includes(':focus-visible'), 'focus-visible outline defined');

// 7. Clean-Room & Brand Verification
console.log('\n7. Clean-Room Brand Verification...');
assert(!html.toLowerCase().includes('poweramp logo'), 'No Poweramp logo references');
assert(html.includes('independent clean-room project'), 'Footer includes independent clean-room statement');
assert(html.includes('Not affiliated with, endorsed by, or associated with Poweramp'), 'Explicit non-affiliation disclosure present');

// 8. FAQ Content Completeness
console.log('\n8. Checking FAQ Accordion...');
const faqItems = html.match(/class="faq-item"/g) || [];
assert(faqItems.length === 6, `FAQ contains exactly 6 verified questions (found ${faqItems.length})`);
assert(html.includes('aria-expanded="false"'), 'FAQ items initialize with aria-expanded="false"');
assert(html.includes('aria-controls="faq-a1"'), 'FAQ question 1 controls panel a1');

// 9. Stable Release & Download Link Integrity
console.log('\n9. Validating Stable Release & Link Integrity...');
assert(html.includes('Official Stable Release'), 'Hero and Download badges display "Official Stable Release"');
assert(!html.includes('Public Beta'), 'Zero occurrences of "Public Beta" in landing page');
assert(!html.includes('Download Beta'), 'Zero occurrences of "Download Beta" in landing page');
assert(releaseConfig.downloadUrl === 'https://github.com/mefahim/MIORA/releases/download/v0.1.0/MIORA-v0.1.0.apk', 'Direct APK download URL is correct');
assert(releaseConfig.checksumUrl === 'https://github.com/mefahim/MIORA/releases/download/v0.1.0/MIORA-v0.1.0.apk.sha256', 'Checksum download URL is correct');
assert(releaseConfig.releaseNotesUrl === 'https://github.com/mefahim/MIORA/releases/tag/v0.1.0', 'Release notes URL is correct');
assert(releaseConfig.packageId === 'com.fhf.miora', 'Package identity is com.fhf.miora');
assert(releaseConfig.targetAndroid === 'Android 14 (API 34)', 'Target Android SDK is 34');

// 10. Verified Feature Presentations & Installation Guide
console.log('\n10. Validating Verified Feature Presentations & Installation Guide...');
assert(html.includes('Pure Local Library'), 'Pure Local Library feature card present');
assert(html.includes('Media3 Audio Engine'), 'Media3 Audio Engine feature card present');
assert(html.includes('10-Band Hardware EQ'), '10-Band Hardware EQ feature card present');
assert(html.includes('Synchronized Lyrics'), 'Synchronized Lyrics feature card present');
assert(html.includes('Dynamic Palette Theming'), 'Dynamic Palette Theming feature card present');
assert(html.includes('Persistent SQLite Playlists'), 'Persistent SQLite Playlists feature card present');
assert(html.includes('Widget &amp; Android Auto'), 'Widget & Android Auto feature card present');
assert(html.includes('Privacy &amp; In-App Updates'), 'Privacy & In-App Updates feature card present');
assert(html.includes('MIORA v0.1.0 Milestone Achieved'), 'Release spotlight banner present');
assert(html.includes('Quick Installation Guide'), 'Quick Installation Guide section present');

console.log('\n====================================================');
console.log(`Results: ${passedTests}/${totalTests} Passed (${failedTests} Failures)`);
console.log('====================================================\n');

if (failedTests > 0) {
  process.exit(1);
} else {
  console.log('🎉 ALL QA TESTS PASSED SUCCESSFULLY!');
  process.exit(0);
}
