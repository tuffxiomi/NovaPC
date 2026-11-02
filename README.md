# NovaPC — Android project

NovaPC is an Android Studio starter project for a Windows-game library and runtime front end.

## Current status

**This is not yet a Windows emulator.** It currently provides:

- Import an individual `.exe` through Android's Storage Access Framework.
- Add a game folder through Android's folder picker.
- Save library entries and read access permissions where the document provider supports persistent grants.
- Keep separate Performance, Balanced, and Compatibility labels for each entry.
- Save preferred resolution and frame-rate target values.
- Display device ABI, Android version, and memory information.
- Share a selected `.exe` to another app when that app declares support for receiving it.

The project does **not** bundle Wine, Box86/Box64, a Linux root filesystem, graphics drivers, DXVK, VKD3D, or a Windows game runtime. The saved profile/resolution/FPS values are preferences only; they do not change actual game rendering yet. Folder entries are stored, but the app does not currently scan the folder for executables or launch them.

## Open and build

1. Extract the ZIP.
2. Open the `NovaPC_Android_Project` folder in Android Studio.
3. Install Android SDK Platform 35 and use JDK 17 with Gradle 8.9.
4. Sync the project and run the `app` configuration on an Android device or emulator.

You can also build from a terminal where Gradle 8.9 and Android SDK Platform 35 are installed:

```bash
gradle --no-daemon :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow is included at `.github/workflows/build-android.yml`. Push the project to a GitHub repository and run the workflow to build and download the debug APK as an artifact.

## What is still needed for actual PC-game execution

A real runtime integration is a separate, substantial engineering task. A typical ARM64 Android Windows-game stack involves:

- Wine for Windows API compatibility.
- Box64 and, for older 32-bit software, Box86 for x86/x86-64 translation.
- A Linux userspace/root filesystem and process/runtime management.
- A device-compatible graphics path such as Mesa/Vulkan, plus Direct3D translation components such as DXVK or VKD3D where supported.
- Audio, input, controller mapping, windowing, filesystem mapping, per-game launch configuration, and device-specific testing.

The runtime must be built for supported Android ABIs and GPU drivers, packaged with all required notices/licenses, and tested on real devices. There is no universal Android interface that lets this app control every separately installed Windows runtime. NovaPC currently has no such adapter.

## Compatibility and performance

Do not expect every `.exe` or PC game to work. Some programs are incompatible with Wine, require Windows-only drivers/services, rely on anti-cheat, use unsupported graphics features, or need their complete installation directory and redistributables. Performance depends on CPU, GPU, Vulkan driver, memory, cooling, game settings, and runtime configuration. Even established Android Windows runtimes document device requirements and variable results.

Use only game files you are entitled to use and only run files from sources you trust. Importing a file does not verify that it is safe.

## Project details

- App ID: `com.novapc.launcher`
- Language: Kotlin
- UI: Android platform Views (no third-party UI library)
- Minimum Android version: Android 8.0 (API 26)
- Compile/target SDK: 35
- Version: 0.1.0

## License

The original NovaPC starter code is provided under the MIT License. Any future third-party runtime components must retain their own licenses and notices; do not treat this project's license as applying to them.
