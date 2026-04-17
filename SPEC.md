# Ondy - Specification Document

## 1. Project Overview
- **Project Name**: Ondy
- **Project Type**: Native Android Application (Kotlin)
- **Core Functionality**: An app that intercepts notifications from selected apps, stores them, and displays a summary notification at user-defined scheduled times (e.g., 9am, 12pm, 2pm, 8pm).

## 2. Technology Stack & Choices
- **Language**: Kotlin 1.9.x
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **Architecture**: MVVM with Clean Architecture
- **UI Framework**: Jetpack Compose with Material 3
- **Key Libraries**:
  - Jetpack Compose BOM 2024.02.00
  - Material 3
  - Room Database (for storing notifications)
  - Hilt (Dependency Injection)
  - Kotlin Coroutines + Flow
  - WorkManager (for scheduled notifications)
  - DataStore (preferences)
- **State Management**: StateFlow + ViewModel

## 3. Feature List
1. **NotificationListenerService** - Capture notifications from selected apps (no accessibility needed)
2. **App Selection Screen** - List installed apps, allow multi-select to block notifications
3. **Schedule Time Selection** - Add/remove multiple scheduled times (e.g., 9am, 12pm, 2pm, 8pm)
4. **Notification Storage** - Store blocked notifications in Room database
5. **Summary Notification** - At scheduled time, show "You have X notifications" notification
6. **Notification Detail View** - Tap summary to see list of all stored notifications
7. **Swipe to Clear** - Swipe individual notifications to dismiss
8. **Clear All Button** - Clear all stored notifications at once
9. **Permission Handling** - Request Notification Access permission, request ignoring battery optimization
10. **Light/Dark Theme** - Follow system theme with Material 3 dynamic colors

## 4. UI/UX Design Direction
- **Visual Style**: Material Design 3 with clean, minimalist aesthetic
- **Color Scheme**: Dynamic theming based on system, with purple/violet primary accent
- **Layout**: 
  - Bottom Navigation with 2 tabs: Apps, Settings/Schedule
  - Main screens: App selector list, Schedule time picker, Notifications list
- **Components**: 
  - Cards for app items and notification items
  - Floating Action Button for adding time slots
  - Chips for schedule times
  - Swipe-to-delete on notification items
  - TopAppBar with actions