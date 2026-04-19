<h1 align="center">Ondy - Notifications On-Demand</h1>
<p align="center">
    <img src="app/assets/logo.png" width="120" height="120" style="border-radius: 50%; background-color: black;">
</p>
<p align="center">Ondy intercepts notifications from your selected apps and delivers them at scheduled times. Stay focused without missing important alerts.</p>

## Features

- **App Selection** - Choose which apps to block notifications from
- **Scheduled Summaries** - Set multiple notification times (e.g., 9am, 12pm, 2pm, 8pm)
- **Notification List** - View all blocked notifications in one place
- **Quick Actions** - Dismiss individual notifications, or clear all at once
- **Privacy First** - All data stored locally on your device, no internet required
- **No Tracking** - No analytics, no collection of any kind

## Screenshots

| Welcome | Notifications Permissions | Privacy |
|--------|--------------------------|---------|
| [![Welcome.jpg](https://iili.io/BUbK1Lu.md.jpg)](https://freeimage.host/i/BUbK1Lu) | [![Notification_Permissions.jpg](https://iili.io/BUbT19a.md.jpg)](https://freeimage.host/i/BUbT19a) | [![Privacy.jpg](https://iili.io/BgU4ghG.md.jpg)](https://freeimage.host/i/BgU4ghG) |

| App Selection | Schedule | Notifications |
|--------------|----------|---------------|
| [![App_Selection.jpg](https://iili.io/BUbnfDB.md.jpg)](https://freeimage.host/i/BUbnfDB) | [![Schedule.jpg](https://iili.io/BUbo0hB.md.jpg)](https://freeimage.host/i/BUbo0hB) | [![Notifications.jpg](https://iili.io/BgUUe29.md.jpg)](https://freeimage.host/i/BgUUe29) |

## Installation

### Download APK

Download the latest release from the [Releases](https://github.com/dhanush777x/ondy/releases) page.

### Building from Source

```bash
# Clone the repository
git clone https://github.com/dhanush777x/ondy.git
cd ondy

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room
- **DI**: Hilt
- **Scheduling**: AlarmManager
- **Preferences**: DataStore

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see [LICENSE](LICENSE) for details.

## Support

If you encounter any issues or have suggestions, please [open an issue](https://github.com/dhanush777x/ondy/issues) on GitHub.

