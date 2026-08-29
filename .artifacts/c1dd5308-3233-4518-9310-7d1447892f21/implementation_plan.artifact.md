# Fix Build Errors and Code Bugs

This plan addresses build issues (specifically `UnstableApi` and `registerReceiver` flags) and resolves code bugs/warnings identified by the project analyzer to ensure successful APK generation.

## Proposed Changes

### [app]

#### [MODIFY] [MainActivity.java](file:///home/jandergy/AndroidStudioProjects/MyJanDergyMusic/app/src/main/java/com/jandergy/myjandergymusic/MainActivity.java)
- Replace `registerReceiver` with `ContextCompat.registerReceiver` to properly handle export flags on Android 14+ (API 34+).
- Remove redundant `findViewById(R.id.player_controls)` calls and consolidate into the `playerControls` member variable.
- Remove unused imports and variables (`PERMISSION_REQUEST_CODES`, `Animator`).

#### [MODIFY] [SettingsActivity.java](file:///home/jandergy/AndroidStudioProjects/MyJanDergyMusic/app/src/main/java/com/jandergy/myjandergymusic/SettingsActivity.java)
- Add `@OptIn(markerClass = UnstableApi.class)` to methods calling unstable Media3 APIs to resolve compilation errors.
- Migrate the deprecated `onBackPressed()` method to use `OnBackPressedDispatcher` for modern back gesture support while preserving `supportFinishAfterTransition()`.
- Fix redundant `btnRepeat` initialization and other minor code cleanup.

#### [MODIFY] [PlaybackService.java](file:///home/jandergy/AndroidStudioProjects/MyJanDergyMusic/app/src/main/java/com/jandergy/myjandergymusic/PlaybackService.java)
- Add `@OptIn(markerClass = UnstableApi.class)` to `onCreate()` and `onPlaybackStateChanged` to resolve `UnstableApi` propagation issues.

#### [MODIFY] [build.gradle.kts](file:///home/jandergy/AndroidStudioProjects/MyJanDergyMusic/app/build.gradle.kts)
- (Optional) Adjust `compileSdk` to `35` if `36` causes build environment issues, but prioritize code fixes first.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to verify the project compiles and generates a debug APK.
- Run `gradle assembleRelease` to verify release build compatibility.

### Manual Verification
- Verify the app launches correctly.
- Test the back gesture in the Settings screen to ensure `supportFinishAfterTransition()` still works.
- Verify that favorite toggles still sync correctly between activities (testing the `BroadcastReceiver`).
