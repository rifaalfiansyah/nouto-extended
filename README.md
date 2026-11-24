<div align="center">

# Nouto Extended

**A fast, offline-first Android notes app built with Kotlin and Material 3 Expressive.**

[![Version](https://img.shields.io/badge/version-v1.0.1-6750A4?style=flat-square)](https://github.com/rifaalfiansyah/nouto-extended)
[![Min SDK](https://img.shields.io/badge/Android-9.0%2B%20(API%2028)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/pie)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-6750A4?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

An enhanced fork of [Nouto](https://github.com/HotarunIchijou/Nouto), with expressive motion, rich text editing, automatic saving, note insights, and privacy features.

</div>

---

## Features

### Write and organize

- Format notes with bold, italic, underline, strikethrough, bulleted and numbered lists, undo, and redo.
- Notes save automatically as you type and when you leave the editor.
- See the last-edited time and interactive word and character counts.
- Pin important notes, search their text, and switch between linear and staggered grid layouts.
- Deleted notes move to Trash, where they can be restored or permanently removed. Undo accidental deletions from the Snackbar.
- Keep recent search suggestions and remove individual suggestions.

### Privacy and personalization

- Lock the app with a four-digit PIN and optionally use biometric unlock. Configure automatic locking after inactivity.
- Choose Light, Dark, System Default, or Pure Black themes. Dynamic colors can match your wallpaper on Android 12 and later.
- Adjust editor behavior, including view-only mode for existing notes, keyboard display, initial focus, and title visibility.
- Use optional haptic feedback for PIN input and selected interactions.

### Local data and portability

- Notes are stored locally in a Room database. The app does not require an account or make network requests.
- Back up and restore the notes database locally on your device.
- Import and export individual notes as JSON files.

---

## Tech stack

- Kotlin with Java 17 compatibility
- Material Components, ViewBinding, and AndroidX
- MVVM with ViewModel, LiveData, and Navigation Component
- Room with KSP, Kotlin Coroutines, and SharedPreferences
- [Android-RTEditor](https://github.com/1gravity/Android-RTEditor) for rich text editing
- [Android-Room-Database-Backup](https://github.com/rafi0101/Android-Room-Database-Backup) for database backup and restore

## Build from source

Requirements: Android SDK for API 37 and a Java 17 compatible JDK. The Gradle wrapper downloads the configured Gradle version when needed.

```bash
git clone https://github.com/rifaalfiansyah/nouto-extended.git
cd nouto-extended
./gradlew assembleDebug
```

To install the debug build on a connected Android device running API 28 or later:

```bash
./gradlew installDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

## Credits and license

- **Original project:** [Nouto](https://github.com/HotarunIchijou/Nouto) by [HotarunIchijou (Kaoru Ichijou)](https://github.com/HotarunIchijou)
- **License:** [MIT](LICENSE)
