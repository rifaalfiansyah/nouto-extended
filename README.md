# Nouto Extended

**Nouto Extended** is an enhanced fork of [Nouto](https://github.com/HotarunIchijou/Nouto) — a simple, offline-first Android notes app built in Kotlin with Material 3 Expressive Design. It focuses heavily on smooth transitions, intuitive gestures, and a clean interface that stays out of your way, with additional stability fixes and performance improvements.

## ✨ Key features
* **Material 3 Expressive design:** Sleek, modern UI with smooth shared-axis transitions and responsive layouts
* **Rich text editing:** Format notes with bold, italic, and underline styling using a floating toolbar
* **Pinning & flexible layouts:** Pin important notes to the top and switch between linear and staggered grid views
* **Plain-text search & history:** Search notes by actual plain-text content with recent search history
* **Swipe to delete with undo & Trash:** Delete notes with a swipe, restore instantly via Snackbar, or recover them from Trash
* **Backup, restore, export & import:** Full Room database backup/restore and single-note JSON export/import
* **Dynamic color & Pure Black theme:** Supports Android 12+ Material You dynamic colors, light/dark modes, and AMOLED pure black mode
* **Predictive back gestures:** Full support for Android 14+ predictive back navigation
* **Offline-first:** Everything is stored locally on your device — no account required, no network calls

## 🚀 Enhancements in Nouto Extended
* **Improved null-safety:** Eliminated unsafe `!!` operators in note adapters and export filename generation, adding HTML stripping and a safe `"note.json"` fallback
* **Safer Trash interactions:** Replaced unsafe ViewHolder and layout casts in Trash deletion dialogs with safe casts
* **Clear import error feedback:** Surface descriptive error messages in Snackbars when JSON note imports fail
* **Smarter search & history:** Filtered search results against plain text instead of raw HTML tags, and upgraded search history persistence to `JSONArray` (with backward compatibility) so queries with commas stay intact
* **Async file I/O & UI optimizations:** Moved note import/export file I/O to `Dispatchers.IO` coroutines, removed duplicate soft-delete calls, and cached adapter preferences on attach instead of reading per item bind

## 🙌 Credits
* **Original Project:** [Nouto](https://github.com/HotarunIchijou/Nouto) by [HotarunIchijou (Kaoru Ichijou)](https://github.com/HotarunIchijou), licensed under the [MIT License](/LICENSE)
* **Libraries:**
  * [1gravity/Android-RTEditor](https://github.com/1gravity/Android-RTEditor) — native Android rich text editor
  * [rafi0101/Android-Room-Database-Backup](https://github.com/rafi0101/Android-Room-Database-Backup) — local Room database backup and restore
