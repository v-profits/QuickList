# QuickList / Быстрый список

Simple shopping & to-do lists with a home-screen widget. No account, no internet permission, all data stays on the device.
Простые списки покупок и дел с виджетом. Без аккаунта, без интернета, все данные только на устройстве.

## Open in Android Studio
1. Android Studio Koala (2024.1) or newer → **File ▸ Open** → select the `QuickList` folder.
2. Wait for Gradle sync (JDK 17+, Android SDK 34 is installed automatically on request).
3. Run ▸ `app`.

## Features
- Unlimited lists of two kinds: **Shopping** and **To-do**
- **Quick input**: the keyboard opens with the list; add many at once with commas
  - `milk x2`, `2 milk`, `1.5 kg apples`, `milk 2l` → quantity is recognised
  - `call mom!` → high priority (to-do)
  - voice input (microphone button)
  - suggestions from your history; tap to re-add
  - re-adding a completed item restores it instead of duplicating
  - Home screen has a one-field quick add with a list picker
  - Share text from any app to QuickList → added to your last list
  - Launcher shortcuts (long-press the app icon): add to shopping / add a task
- Shopping: quantity, price + total, auto categories (EN/RU keywords), grouping by category
- To-do: priority, due date & time with notification, repeat (daily/weekly/monthly), "Done" button in the notification
- Swipe to delete with Undo, tap to edit, completed section (collapsible)
- Sort, rename, share as text, uncheck all, clear completed
- **Widget** (resizable): choose a list, tick items directly in the widget, "+" opens the list ready for typing
- Light / dark / system theme, dynamic colors (Android 12+), English / Russian (per-app language)
- Backup: export / import JSON (Settings)

## Structure
```
app/src/main/java/com/quicklist/app/
  MainActivity.kt, QuickListApp.kt
  data/      Room entities, DAOs, Repository (incl. backup), Prefs, Category
  util/      QuickParser (smart input), sorting & formatting
  reminders/ AlarmManager scheduler, notification + boot receivers
  widget/    Glance widget, config activity
  ui/        Compose screens, ViewModel, navigation, theme
```
Stack: Kotlin, Jetpack Compose (Material 3), Room (KSP), Glance, AppCompat per-app locales. minSdk 26, targetSdk 34.

## Notes
- Reminders use inexact alarms (no special permission): they fire within a few minutes of the chosen time.
- The widget follows the *system* theme/wallpaper colors (widgets live outside the app process).
- To add a language: create `res/values-xx/strings.xml` and add it to `res/xml/locales_config.xml`.
