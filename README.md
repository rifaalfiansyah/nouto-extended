<div align="center">

# Nouto Extended

**A fast, offline-first Android notes app built with Kotlin & Material 3 Expressive Design.**

[![Version](https://img.shields.io/badge/version-v1.0.1-6750A4?style=flat-square)](https://github.com/rifaalfiansyah/nouto-extended)
[![Min SDK](https://img.shields.io/badge/Android-9.0%2B%20%28API%2028%29-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/pie)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-37-6750A4?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](/LICENSE)

An enhanced fork of [Nouto](https://github.com/HotarunIchijou/Nouto) focused on expressive motion, richer text formatting, auto-save, note insights, and rock-solid stability.

</div>

---

## ✨ Features

### Writing & Formatting
- **Rich Text Floating Toolbar:** Format notes effortlessly with **Undo**, **Redo**, **Bold**, **Italic**, **Underline**, **Strikethrough**, **Bulleted Lists**, and **Numbered Lists**.
- **Auto-Save:** Changes are saved automatically while typing, toggling formatting, pinning, or leaving the editor — no manual Save button required.
- **Note Metadata & Counters:** Inspect last-edited timestamps (`Today`, `Yesterday`, or date) alongside interactive **Word** and **Character** count pills with tap-to-toggle and press-and-hold animations.
- **View-Only & Editor Preferences:** Customize whether existing notes open in view-only mode, whether titles appear on cards/editor, and initial keyboard focus.

### Organization & Search
- **Pinning & Flexible Layouts:** Pin important notes to the top and switch smoothly between **Linear** and **Staggered Grid** layouts.
- **Plain-Text Search & History:** Search notes by rendered plain text (ignoring raw HTML tags) with persistent recent search suggestions.
- **Swipe Actions & Trash:** Swipe notes to soft-delete with instant Snackbar undo, or browse, restore, and permanently delete items in **Trash**.
- **Redesigned Sidebar:** Inverted-active Material 3 navigation sheet displaying live Notes and Trash counts with quick footer access to Settings.

### Personalization & Data
- **Material You Dynamic Colors:** Adapts seamlessly to your wallpaper on Android 12+, including dynamic launcher icon foreground and splash screen theming.
- **Light, Dark & Pure Black Mode:** Supports Light, Dark, System Default, and AMOLED Pure Black themes.
- **Offline-First Storage & Portability:** 100% local Room database with zero network calls, full database backup/restore, and single-note JSON export/import.
- **Predictive Back Gestures:** Smooth shared-axis transitions and Android 14+ predictive back navigation.

---

## 🚀 Enhancements Over Original Nouto

| Area | Improvements in Nouto Extended |
| :--- | :--- |
| **Editor & Workflow** | Added Undo/Redo, Strikethrough, Bulleted & Numbered Lists in a compact grouped floating toolbar; excluded initial text load from the undo stack; replaced manual Save with debounced auto-save. |
| **Metadata & UI** | Added relative edit timestamps on cards and editor, interactive expandable Word/Character counters, redesigned sidebar with footer Settings, and Material You dynamic app icon & splash screen. |
| **Search & History** | Filtered searches against plain text instead of raw HTML markup; migrated search history storage to `JSONArray` (preserving queries with commas, with backward compatibility). |
| **Stability & Safety** | Eliminated unsafe `!!` operators and ViewHolder/layout casts; fixed ViewBinding cleanup in export settings; surfaced clear import error messages; added safe `"note.json"` export filename fallbacks. |
| **Performance** | Moved JSON file I/O to `Dispatchers.IO` coroutines, cached `DateTimeFormatter` and adapter preferences, and reused `StaggeredGridLayoutManager` for fluid list/grid transitions. |

---

## 🛠️ Tech Stack

- **Language:** Kotlin (Java 17 bytecode)
- **UI & Design:** Material Components (`1.14.0` Material 3 Expressive), ViewBinding, CoordinatorLayout, ConstraintLayout, AndroidX Transition
- **Architecture:** MVVM (`ViewModel`, `LiveData`, `Navigation Component` with Safe Args)
- **Persistence:** Room (`2.8.4`) with KSP, Coroutines, SharedPreferences
- **Libraries:**
  - [1gravity/Android-RTEditor](https://github.com/1gravity/Android-RTEditor) — native Android rich-text editor
  - [rafi0101/Android-Room-Database-Backup](https://github.com/rafi0101/Android-Room-Database-Backup) — local Room database backup and restore

---

## 📦 Building from Source

```bash
git clone https://github.com/rifaalfiansyah/nouto-extended.git
cd nouto-extended
./gradlew assembleDebug
```

To install directly on a connected Android device (`API 28+`):

```bash
./gradlew installDebug
```

---

## 🙌 Credits & License

- **Original Project:** [Nouto](https://github.com/HotarunIchijou/Nouto) by [HotarunIchijou (Kaoru Ichijou)](https://github.com/HotarunIchijou)
- **License:** Distributed under the [MIT License](/LICENSE)
