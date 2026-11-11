# MIORA — First-Launch Permission Onboarding Report

**Date:** 2026-11-11  
**Project:** MIORA — Music in Our Real Audio  
**Target Identity:** `com.fhf.miora`  
**Target Release:** `v0.1.0` (versionCode: `3`)  
**Status:** `IMPLEMENTED & VERIFIED`

---

## 1. Context & Motivation

Prior to v0.1.0, clean installations launched directly into the main Library screen without an introductory onboarding experience. If audio or notification permissions were ungranted, the user saw empty lists without clear explanation or immediate recourse.

Modern Android guidelines require applications that rely on sensitive capabilities (such as filesystem audio scanning and ongoing notification services) to provide clear context *before* invoking platform permission dialogs.

The **First-Launch Permission Onboarding Flow** was introduced to welcome new users, explain why permissions are needed, request them smoothly, and trigger the first local media scan before entering the main player.

---

## 2. Onboarding Architecture & Flow

The flow is implemented as a 4-step wizard in [`OnboardingScreen.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/ui/OnboardingScreen.kt), managed by a simple state machine:

```
[ Step 1: Welcome ] ──> [ Step 2: Audio Access ] ──> [ Step 3: Notifications ] ──> [ Step 4: Library Ready ] ──> [ Main Library ]
```

### Step 1: Welcome (`WelcomeStep`)
- **Visuals:** MIORA glowing brand icon, tagline *"Your music. Your moments."*
- **Copy:** Reassures users that MIORA is completely offline, privacy-first, and never uploads or tracks listening habits.
- **Action:** Primary button *"Get Started"*.

### Step 2: Audio Library Access (`AudioPermissionStep`)
- **Context:** Explains that MIORA requires read access to local audio files to catalog songs, albums, and artwork.
- **Platform Adaptation:**
  - On **Android 13+ (API 33+)**: Requests `android.permission.READ_MEDIA_AUDIO`.
  - On **Android 8.0–12 (API 26–32)**: Requests `android.permission.READ_EXTERNAL_STORAGE`.
- **Action:** Primary button *"Grant Audio Access"*, utilizing Jetpack Compose `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. A secondary *"Continue"* button enables proceeding if permission was previously granted or skipped.

### Step 3: Playback Notifications (`NotificationPermissionStep`)
- **Context:** Explains that background playback controls, lockscreen media art, and status bar notification controls require notification permission.
- **Platform Adaptation:**
  - On **Android 13+ (API 33+)**: Launches `android.permission.POST_NOTIFICATIONS` request.
  - On **Android 8.0–12**: Automatically detects notifications as inherently enabled and offers direct continuation.
- **Action:** Primary button *"Enable Notifications"* and secondary *"Maybe Later"*.

### Step 4: Initial Indexing & Ready (`ReadyStep`)
- **Action:** Automatically triggers `libraryViewModel.refresh()` to index device audio files via MediaStore.
- **Completion:** Persists `hasCompletedOnboarding = true` to `SettingsStore` and transitions to `Routes.LIBRARY` with `inclusive = true`, preventing back-navigation into onboarding.

---

## 3. DataStore & State Persistence

### [`SettingsStore.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/data/SettingsStore.kt)
```kotlin
private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[KEY_ONBOARDING_COMPLETED] ?: false
}

suspend fun setCompletedOnboarding(completed: Boolean) {
    dataStore.edit { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] = completed
    }
}
```

### [`MainActivity.kt`](file:///workspace/poweramp-clone/app/src/main/java/com/fahim/powerampclone/MainActivity.kt)
```kotlin
val hasCompletedOnboarding by settingsViewModel.hasCompletedOnboarding.collectAsState(initial = true)
val startDestination = if (hasCompletedOnboarding) Routes.LIBRARY else Routes.ONBOARDING

MioraNavGraph(
    navController = navController,
    startDestination = startDestination,
    // ...
)
```

---

## 4. Verification & Testing

1. **Clean Install State:** Verified that on clean launch, `hasCompletedOnboarding` defaults to `false` and routes directly to `Routes.ONBOARDING`.
2. **Backstack Security:** Verified that after reaching `ReadyStep` and tapping *"Start Listening"*, the onboarding route is completely popped from the backstack; pressing the system back button exits the app rather than reopening onboarding.
3. **Subsequent Launches:** Verified that on subsequent app launches, `hasCompletedOnboarding` reads `true` and the app immediately opens `Routes.LIBRARY` with zero onboarding overhead.
