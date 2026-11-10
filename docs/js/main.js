/**
 * MIORA Official Landing Page Client Script
 * Handles configuration hydration, accessible navigation, FAQ accordion, and interactive preview.
 */

document.addEventListener("DOMContentLoaded", () => {
  // 1. Hydrate dynamic release configuration
  hydrateReleaseConfig();

  // 2. Setup Sticky Header scroll listener
  setupStickyHeader();

  // 3. Setup Accessible Mobile Menu
  setupMobileNav();

  // 4. Setup FAQ Accordion
  setupFaqAccordion();

  // 5. Setup Checksum Copy Tool
  setupChecksumCopy();

  // 6. Setup Interactive Mockup Playback Simulation
  setupMockupPlayer();
});

/**
 * Hydrates UI elements using values from release-config.js
 */
function hydrateReleaseConfig() {
  const cfg = window.MIORA_CONFIG || {};

  // Update dynamic year in footer
  document.querySelectorAll("[data-current-year]").forEach(el => {
    el.textContent = new Date().getFullYear();
  });

  if (!cfg.version) return;

  // Hydrate text nodes
  document.querySelectorAll("[data-config-version]").forEach(el => {
    el.textContent = cfg.version;
  });

  document.querySelectorAll("[data-config-size]").forEach(el => {
    el.textContent = cfg.apkSize;
  });

  document.querySelectorAll("[data-config-sha256]").forEach(el => {
    el.textContent = cfg.sha256;
  });

  document.querySelectorAll("[data-config-android]").forEach(el => {
    el.textContent = cfg.minAndroid;
  });

  // Hydrate anchor URLs
  document.querySelectorAll("[data-download-apk]").forEach(el => {
    if (cfg.downloadUrl) el.setAttribute("href", cfg.downloadUrl);
  });

  document.querySelectorAll("[data-release-notes]").forEach(el => {
    if (cfg.releaseNotesUrl) el.setAttribute("href", cfg.releaseNotesUrl);
  });

  document.querySelectorAll("[data-releases-url]").forEach(el => {
    if (cfg.releasesUrl) el.setAttribute("href", cfg.releasesUrl);
  });

  document.querySelectorAll("[data-repo-url]").forEach(el => {
    if (cfg.repoUrl) el.setAttribute("href", cfg.repoUrl);
  });
}

/**
 * Adds backdrop blur and border when page is scrolled
 */
function setupStickyHeader() {
  const header = document.querySelector(".site-header");
  if (!header) return;

  const onScroll = () => {
    if (window.scrollY > 20) {
      header.classList.add("scrolled");
    } else {
      header.classList.remove("scrolled");
    }
  };

  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
}

/**
 * Accessible mobile drawer navigation
 */
function setupMobileNav() {
  const toggleBtn = document.querySelector(".mobile-nav-toggle");
  const drawer = document.querySelector(".mobile-drawer");
  if (!toggleBtn || !drawer) return;

  const toggle = () => {
    const isOpen = drawer.classList.contains("open");
    drawer.classList.toggle("open", !isOpen);
    toggleBtn.setAttribute("aria-expanded", String(!isOpen));
  };

  toggleBtn.addEventListener("click", toggle);

  // Close drawer when any internal link is clicked
  drawer.querySelectorAll("a").forEach(link => {
    link.addEventListener("click", () => {
      drawer.classList.remove("open");
      toggleBtn.setAttribute("aria-expanded", "false");
    });
  });
}

/**
 * Accessible FAQ accordion with smooth height animation
 */
function setupFaqAccordion() {
  const items = document.querySelectorAll(".faq-item");

  items.forEach(item => {
    const btn = item.querySelector(".faq-question-btn");
    const panel = item.querySelector(".faq-answer-panel");
    if (!btn || !panel) return;

    btn.addEventListener("click", () => {
      const isExpanded = btn.getAttribute("aria-expanded") === "true";

      // Close all other items for clean accordion UX
      items.forEach(otherItem => {
        if (otherItem !== item) {
          otherItem.classList.remove("active");
          const otherBtn = otherItem.querySelector(".faq-question-btn");
          const otherPanel = otherItem.querySelector(".faq-answer-panel");
          if (otherBtn) otherBtn.setAttribute("aria-expanded", "false");
          if (otherPanel) otherPanel.style.maxHeight = "0";
        }
      });

      // Toggle current
      if (isExpanded) {
        item.classList.remove("active");
        btn.setAttribute("aria-expanded", "false");
        panel.style.maxHeight = "0";
      } else {
        item.classList.add("active");
        btn.setAttribute("aria-expanded", "true");
        panel.style.maxHeight = panel.scrollHeight + "px";
      }
    });
  });
}

/**
 * Copy SHA-256 Checksum to Clipboard with UI feedback
 */
function setupChecksumCopy() {
  const copyBtn = document.getElementById("copy-checksum-btn");
  const valueEl = document.querySelector(".checksum-value");
  if (!copyBtn || !valueEl) return;

  copyBtn.addEventListener("click", async () => {
    const textToCopy = valueEl.textContent.trim();
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(textToCopy);
      } else {
        const textarea = document.createElement("textarea");
        textarea.value = textToCopy;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
      }

      const originalText = copyBtn.textContent;
      copyBtn.textContent = "Copied!";
      copyBtn.style.color = "var(--accent-light)";
      setTimeout(() => {
        copyBtn.textContent = originalText;
        copyBtn.style.color = "";
      }, 2000);
    } catch (e) {
      console.warn("Unable to copy checksum", e);
    }
  });
}

/**
 * Interactive preview playback on device mockup
 */
function setupMockupPlayer() {
  const playBtn = document.getElementById("mockup-play-btn");
  const playIcon = document.getElementById("mockup-play-icon");
  const pauseIcon = document.getElementById("mockup-pause-icon");
  const elapsedEl = document.getElementById("mockup-elapsed-time");
  const sliderFill = document.querySelector(".device-slider-fill");
  const sliderThumb = document.querySelector(".device-slider-thumb");

  if (!playBtn || !elapsedEl || !sliderFill || !sliderThumb) return;

  let isPlaying = false;
  let currentSeconds = 165; // 02:45
  const totalSeconds = 252; // 04:12
  let timer = null;

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m < 10 ? "0" + m : m}:${s < 10 ? "0" + s : s}`;
  };

  playBtn.addEventListener("click", () => {
    isPlaying = !isPlaying;

    if (isPlaying) {
      if (playIcon) playIcon.style.display = "none";
      if (pauseIcon) pauseIcon.style.display = "block";
      playBtn.setAttribute("aria-label", "Pause playback simulation");

      timer = setInterval(() => {
        if (currentSeconds >= totalSeconds) {
          currentSeconds = 0;
        } else {
          currentSeconds++;
        }
        elapsedEl.textContent = formatTime(currentSeconds);
        const percent = (currentSeconds / totalSeconds) * 100;
        sliderFill.style.width = `${percent}%`;
        sliderThumb.style.left = `${percent}%`;
      }, 1000);
    } else {
      if (playIcon) playIcon.style.display = "block";
      if (pauseIcon) pauseIcon.style.display = "none";
      playBtn.setAttribute("aria-label", "Play playback simulation");
      if (timer) clearInterval(timer);
    }
  });
}
