# Audio Studio Pro — Fixed Gradle + UI build

## Build configuration
- Gradle: 8.9 (installed by GitHub Actions workflow)
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- JDK: 17
- compileSdk / targetSdk: 35
- minSdk: 26 (Android 8.0)

## What changed
- Replaced AppCompatActivity with the native Android Activity API, removing the missing AppCompat dependency.
- Added Java/Kotlin 17 compile settings.
- Added a GitHub Actions workflow that installs Gradle 8.9 and builds `:app:assembleDebug`.
- Added scalable vector branding and a more polished dark editor layout. Vector resources scale cleanly on high-density and 4K screens; UI dimensions remain dp-based for proper Android scaling.
- Kept bounded-buffer compressed audio trim and background processing.

## Implemented
- Select/import audio through Android Storage Access Framework.
- Playback of selected audio through MediaPlayer.
- Trim AAC/AMR-compatible tracks by start/end seconds.
- Save to a user-selected `.m4a` destination.
- Verify that the output has non-zero size before showing success.

## Format limitation
The fast trim path supports AAC/AMR audio tracks in compatible containers. It is not a universal MP3/WAV/FLAC/OGG editor. Unsupported formats return a clear error. Output is an MPEG-4 audio container.

## Build in GitHub Actions
Push this project to GitHub or upload it to a repository. The workflow at `.github/workflows/android-build.yml` runs automatically on push and can also be started manually from Actions. The APK is uploaded as an artifact named `Audio-Studio-Pro-debug`.

## Honest status
This is not yet the complete professional editor described in the original brief. Recording, waveform rendering, split/merge, EQ, effects, multitrack mixer, undo/redo, and MP3 export remain unimplemented. No on-device or remote GitHub build has been run from this environment.
