# Contributing to MIORA

Thank you for your interest in contributing to **MIORA**! We welcome bug reports, feature discussions, and code contributions that adhere to our architectural standards and strict clean-room engineering principles.

---

## ⚖️ Clean-Room Development Rules

MIORA is an independent, clean-room reconstruction inspired by modern audiophile music players:

1. **Zero Proprietary Code**: Do NOT paste, incorporate, or reference decompiled Smali, Java, or C/C++ code from proprietary applications.
2. **Zero Proprietary Assets**: Do NOT commit copyrighted images, icons, sound effects, or branding assets.
3. **Open Standards Only**: All algorithms, DSP filters, and protocol integrations must be based on public RFCs, standard audio engineering literature, or official Android open-source documentation.

---

## 🛠️ Development Setup

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK recommended)
- Android SDK (API 34, Build Tools 35.0.0)
- Android Studio Ladybug / Koala or CLI Gradle

### Building & Running Tests Locally
```bash
# Run unit tests
./gradlew testDebugUnitTest --no-daemon

# Build debug APK
./gradlew assembleDebug --no-daemon
```

*Note for PocketDev/Sandbox environments*: Use the designated flags:
```bash
gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline assembleDebug
```

---

## 🌿 Git & Pull Request Guidelines

1. **Branching**: Create a feature branch off `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. **Commit Messages**: Follow standard conventional commits:
   - `feat: add parametric equalizer visualizer`
   - `fix: resolve duration sync issue on Android 14`
   - `docs: update release guide`
3. **Tests**: Ensure all existing tests pass and add unit tests for any new business logic:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Pull Requests**: Open a pull request against `main` on [GitHub](https://github.com/mefahim/MIORA). Include a clear description of changes and screenshot/video demonstrations if modifying the UI.
