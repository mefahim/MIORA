#!/usr/bin/env python3
"""
Phase 1 Automated QA & Baseline Certification Script
Repository: https://github.com/mefahim/poweramp-clone
Usage: python3 scripts/validate_phase1.py
"""

import os
import sys
import hashlib
import subprocess
import re

def main():
    print("===================================================================")
    print("     PHASE 1 REPRODUCIBLE AUTOMATED QA VALIDATION SUITE           ")
    print("===================================================================")

    results = {}
    failures = 0

    # 1. APK SHA-256
    apk_path = "poweramp-music-player-build-1035-uni.apk"
    if not os.path.exists(apk_path):
        results["1_apk_sha256"] = ("FAIL", "APK file missing at ./poweramp-music-player-build-1035-uni.apk")
        failures += 1
    else:
        h = hashlib.sha256()
        with open(apk_path, "rb") as f:
            while chunk := f.read(65536):
                h.update(chunk)
        digest = h.hexdigest()
        expected_digest = "3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f"
        if digest == expected_digest:
            results["1_apk_sha256"] = ("PASS", f"SHA-256 matches baseline: {digest}")
        else:
            results["1_apk_sha256"] = ("FAIL", f"Digest mismatch: got {digest}, expected {expected_digest}")
            failures += 1

    # 2. APK File Size
    if not os.path.exists(apk_path):
        results["2_apk_size"] = ("FAIL", "APK file missing")
        failures += 1
    else:
        size = os.path.getsize(apk_path)
        expected_size = 24481502
        if size == expected_size:
            results["2_apk_size"] = ("PASS", f"Size matches baseline: {size} bytes (~23.35 MB)")
        else:
            results["2_apk_size"] = ("FAIL", f"Size mismatch: got {size}, expected {expected_size}")
            failures += 1

    # 3. Canonical Feature Counts (13 / 55 / 1 / 1 / 0 = 70)
    f04_path = "research/04-FEATURE-INVENTORY.md"
    f14_path = "research/14-EVIDENCE-MATRIX.md"
    if not os.path.exists(f04_path) or not os.path.exists(f14_path):
        results["3_feature_counts"] = ("FAIL", "Feature inventory or evidence matrix missing")
        failures += 1
    else:
        with open(f04_path, "r", encoding="utf-8") as f:
            t04 = f.read()
        with open(f14_path, "r", encoding="utf-8") as f:
            t14 = f.read()

        c04 = (
            len(re.findall(r"- \*\*Access Classification\*\*:\s*`STATIC_IMPLEMENTATION_CONFIRMED`", t04)),
            len(re.findall(r"- \*\*Access Classification\*\*:\s*`IMPLEMENTED_BUT_RUNTIME_UNVERIFIED`", t04)),
            len(re.findall(r"- \*\*Access Classification\*\*:\s*`PARTIALLY_IMPLEMENTED`", t04)),
            len(re.findall(r"- \*\*Access Classification\*\*:\s*`INFERRED`", t04)),
            len(re.findall(r"- \*\*Access Classification\*\*:\s*`RUNTIME_VERIFIED`", t04)),
        )
        c14 = (
            len(re.findall(r"\| `STATIC_IMPLEMENTATION_CONFIRMED` \|", t14)),
            len(re.findall(r"\| `IMPLEMENTED_BUT_RUNTIME_UNVERIFIED` \|", t14)),
            len(re.findall(r"\| `PARTIALLY_IMPLEMENTED` \|", t14)),
            len(re.findall(r"\| `INFERRED` \|", t14)),
            len(re.findall(r"\| `RUNTIME_VERIFIED` \|", t14)),
        )
        expected_counts = (13, 55, 1, 1, 0)
        if c04 == expected_counts and c14 == expected_counts:
            results["3_feature_counts"] = ("PASS", f"Counts exact: Static=13, Unverified=55, Partial=1, Inferred=1, Runtime=0; Total=70")
        else:
            results["3_feature_counts"] = ("FAIL", f"04={c04}, 14={c14}, expected={expected_counts}")
            failures += 1

    # 4. Screen Inventory Counts (8 / 22 / 6 / 8 / 5 / 17 = 66)
    f05_path = "research/05-SCREEN-INVENTORY.md"
    if not os.path.exists(f05_path):
        results["4_screen_counts"] = ("FAIL", "Screen inventory missing")
        failures += 1
    else:
        with open(f05_path, "r", encoding="utf-8") as f:
            t05 = f.read()
        sc_checks = [
            ("USER_FACING_SCREEN", r"USER_FACING_SCREEN:\s*8"),
            ("DIALOG_OR_SHEET", r"DIALOG_OR_SHEET:\s*22"),
            ("WIDGET_CONFIGURATION", r"WIDGET_CONFIGURATION:\s*6"),
            ("PAYMENT_OR_LICENSING", r"PAYMENT_OR_LICENSING:\s*8"),
            ("THIRD_PARTY_INTEGRATION", r"THIRD_PARTY_INTEGRATION:\s*5"),
            ("INTERNAL_ACTIVITY", r"INTERNAL_ACTIVITY.*:\s*17"),
        ]
        sc_ok = all(re.search(pat, t05) for _, pat in sc_checks)
        if sc_ok and "Total Declared Activity Components: 66" in t05:
            results["4_screen_counts"] = ("PASS", "Exact screen breakdown: 8 / 22 / 6 / 8 / 5 / 17 = 66")
        else:
            results["4_screen_counts"] = ("FAIL", "Screen inventory mismatch")
            failures += 1

    # 5. Internal Markdown Links
    md_files = sorted([os.path.join("research", f) for f in os.listdir("research") if f.endswith(".md")] + ["README.md", "PHASE-1-FINAL-REPORT.md"])
    broken_links = []
    total_rel = 0
    unique_targets = set()
    for md in md_files:
        dir_path = os.path.dirname(md)
        with open(md, "r", encoding="utf-8") as f:
            content = f.read()
        for text, target in re.findall(r"\[([^\]]+)\]\(([^)]+)\)", content):
            if target.startswith(("#", "http://", "https://", "mailto:", "file://")):
                continue
            target_path = target.split("#")[0]
            if not target_path:
                continue
            total_rel += 1
            resolved = os.path.normpath(os.path.join(dir_path, target_path))
            unique_targets.add(resolved)
            if not os.path.exists(resolved):
                broken_links.append((md, text, target, resolved))
    if len(broken_links) == 0:
        results["5_markdown_links"] = ("PASS", f"Scanned {len(md_files)} files; {total_rel} relative links; {len(unique_targets)} unique targets; 0 broken")
    else:
        results["5_markdown_links"] = ("FAIL", f"Broken links found: {broken_links}")
        failures += 1

    # 6. Commit Provenance
    try:
        branch = subprocess.check_output(["git", "rev-parse", "--abbrev-ref", "HEAD"]).decode().strip()
        head = subprocess.check_output(["git", "rev-parse", "HEAD"]).decode().strip()
        merge_base = subprocess.check_output(["git", "merge-base", "1e0420395f73f13ebeb86e78cbcea09bc6b796e9", "HEAD"]).decode().strip()
        if branch == "main" and merge_base.startswith("1e04203"):
            results["6_commit_provenance"] = ("PASS", f"Branch={branch}, HEAD={head[:7]}, Baseline 1e04203 in history")
        else:
            results["6_commit_provenance"] = ("FAIL", f"Branch={branch}, HEAD={head}, Base={merge_base}")
            failures += 1
    except Exception as e:
        results["6_commit_provenance"] = ("FAIL", str(e))
        failures += 1

    # 7. Misleading Clipping Claims
    clipping_patterns = [
        r"prevents internal clipping",
        r"prevents clipping",
        r"without clipping",
        r"eliminates clipping",
        r"guaranteed clipping",
    ]
    found_clipping = []
    for md in md_files:
        with open(md, "r", encoding="utf-8") as f:
            lines = f.readlines()
        for idx, line in enumerate(lines):
            # Ignore historical logs and explicit qualifications explaining that float PCM does NOT prevent clipping
            if "18-PHASE-1.2.2-FINAL-QA-REPORT.md" in md and "Before Correction" in line:
                continue
            if "19-PHASE-1.2.2.1-FINAL-CERTIFICATION.md" in md:
                continue
            if "does NOT by itself guarantee clipping prevention" in line or "does not guarantee clipping elimination" in line:
                continue
            for pat in clipping_patterns:
                if re.search(pat, line, re.IGNORECASE):
                    found_clipping.append((md, idx + 1, line.strip()))
    if len(found_clipping) == 0:
        results["7_clipping_claims"] = ("PASS", "Zero unqualified clipping elimination claims")
    else:
        results["7_clipping_claims"] = ("FAIL", f"Unqualified clipping claims: {found_clipping}")
        failures += 1

    # 8. Unsupported CPU Benchmark Percentages
    cpu_patterns = [
        r"<3–5%",
        r"<3-5%",
        r"consumes negligible CPU \(<3",
        r"Under 3% CPU utilization during 32-bit float DSP processing on ARM64\.",
    ]
    found_cpu = []
    for md in md_files:
        with open(md, "r", encoding="utf-8") as f:
            lines = f.readlines()
        for idx, line in enumerate(lines):
            if "19-PHASE-1.2.2.1-FINAL-CERTIFICATION.md" in md:
                continue
            for pat in cpu_patterns:
                if re.search(pat, line):
                    found_cpu.append((md, idx + 1, line.strip()))
    if len(found_cpu) == 0:
        results["8_cpu_claims"] = ("PASS", "Zero unmeasured CPU percentage benchmark claims")
    else:
        results["8_cpu_claims"] = ("FAIL", f"Unmeasured CPU claims: {found_cpu}")
        failures += 1

    # 9. Production Source / Build Isolation
    prod_code = []
    for root, dirs, files in os.walk("."):
        if ".git" in root or "evidence" in root:
            continue
        for f in files:
            if f.endswith((".kt", ".java", ".gradle", ".gradle.kts", ".c", ".cpp", ".h")):
                prod_code.append(os.path.join(root, f))
    if len(prod_code) == 0:
        results["9_production_code_isolation"] = ("PASS", "0 production source/build files found outside evidence/")
    else:
        results["9_production_code_isolation"] = ("FAIL", f"Found code files: {prod_code}")
        failures += 1

    # 10. Security & DRM Integrity Boundary
    security_violations = []
    for md in md_files:
        with open(md, "r", encoding="utf-8") as f:
            lines = f.readlines()
        for idx, line in enumerate(lines):
            if re.search(r"\bsu 0\b|adb shell su|date -s\b|sharedUserId.*bypass", line):
                security_violations.append((md, idx + 1, line.strip()))
    if len(security_violations) == 0:
        results["10_security_integrity"] = ("PASS", "0 root execution, clock tampering, or DRM bypass instructions")
    else:
        results["10_security_integrity"] = ("FAIL", f"Security violations: {security_violations}")
        failures += 1

    # 11. APK Tracked and Unchanged
    try:
        apk_git_status = subprocess.check_output(["git", "status", "--porcelain", "poweramp-music-player-build-1035-uni.apk"]).decode().strip()
        if not apk_git_status:
            results["11_apk_unmodified"] = ("PASS", "poweramp-music-player-build-1035-uni.apk is clean and unmodified")
        else:
            results["11_apk_unmodified"] = ("FAIL", f"APK modified in git: {apk_git_status}")
            failures += 1
    except Exception as e:
        results["11_apk_unmodified"] = ("FAIL", str(e))
        failures += 1

    # 12. Phase 2 Boundary
    phase2_dirs = []
    for root, dirs, files in os.walk("."):
        if ".git" in root or "evidence" in root:
            continue
        for d in dirs:
            if d in ("compose", "ui", "layout", "viewmodel"):
                phase2_dirs.append(os.path.join(root, d))
    if len(phase2_dirs) == 0:
        results["12_phase2_boundary"] = ("PASS", "Phase 2 not started; 0 UI/Compose/layout directories")
    else:
        results["12_phase2_boundary"] = ("FAIL", f"Found Phase 2 directories: {phase2_dirs}")
        failures += 1

    print()
    for key, (status, detail) in sorted(results.items()):
        print(f"[{status}] {key}: {detail}")

    print()
    if failures == 0:
        print("OVERALL RESULT: ALL 12 CHECKS PASSED (PASS)")
        sys.exit(0)
    else:
        print(f"OVERALL RESULT: {failures} CHECKS FAILED (FAIL)")
        sys.exit(1)

if __name__ == "__main__":
    main()
