# Eunio Health App

A comprehensive women's health and fertility tracking application built with Kotlin Multiplatform.

## Project Structure

```
EunioHealthApp/
├── shared/                          # Shared Kotlin Multiplatform module
│   ├── src/
│   │   ├── commonMain/kotlin/com/eunio/healthapp/
│   │   │   ├── domain/              # Domain layer (business logic)
│   │   │   │   ├── model/           # Domain models
│   │   │   │   ├── repository/      # Repository interfaces
│   │   │   │   ├── error/           # Error handling
│   │   │   │   └── util/            # Domain utilities
│   │   │   ├── data/                # Data layer
│   │   │   │   └── local/           # Local database
│   │   │   ├── presentation/        # Presentation layer
│   │   │   │   ├── viewmodel/       # ViewModels
│   │   │   │   └── state/           # UI state classes
│   │   │   └── di/                  # Dependency injection
│   │   ├── androidMain/kotlin/      # Android-specific implementations
│   │   ├── iosMain/kotlin/          # iOS-specific implementations
│   │   └── commonMain/sqldelight/   # SQLDelight database schemas
├── androidApp/                      # Android application module
└── iosApp/                          # iOS application module
```

## Technologies Used

- **Kotlin Multiplatform**: Cross-platform development
- **Compose Multiplatform**: UI framework
- **Firebase**: Authentication, Firestore, Cloud Functions
- **SQLDelight**: Local database
- **Ktor**: HTTP client
- **Koin**: Dependency injection
- **Kotlinx Serialization**: JSON serialization
- **Kotlinx DateTime**: Date/time handling
- **Kotlinx Coroutines**: Asynchronous programming

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Xcode 15.4+ (for iOS development)
- JDK 11 or later
- Kotlin 1.9.21

### Quick Start

See [docs/QUICK-START.md](docs/QUICK-START.md) for detailed setup instructions.

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Run the Android app using the `androidApp` configuration
5. For iOS, open `iosApp/iosApp.xcodeproj` in Xcode

For detailed build and test instructions, see [docs/BUILD-AND-TEST.md](docs/BUILD-AND-TEST.md).

## Architecture

The app follows Clean Architecture principles with:

- **Domain Layer**: Contains business logic, entities, and repository interfaces
- **Data Layer**: Implements repositories, handles local and remote data sources
- **Presentation Layer**: Contains ViewModels and UI state management

## Features

- User authentication and onboarding
- Daily health logging (symptoms, mood, fertility indicators)
- Smart calendar with cycle tracking
- Advanced fertility tracking with BBT charting
- AI-powered insights and pattern recognition
- Health reports and data export
- Cross-platform synchronization
- Offline-first architecture

## Security & Privacy

- HIPAA-compliant data handling
- End-to-end encryption
- Secure Firebase authentication
- Local data encryption

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for full privacy policy.

## Documentation

- [Quick Start Guide](docs/QUICK-START.md)
- [Build and Test Guide](docs/BUILD-AND-TEST.md)
- [iOS Firebase Sync](docs/IOS-FIREBASE-SYNC.md)
- [Android Logging](docs/ANDROID-LOGGING.md)
- [Cross-Platform Sync](docs/CROSS-PLATFORM-SYNC-GUIDE.md)
- [Network and Retry Logic](docs/NETWORK-AND-RETRY.md)

## Scripts

Utility scripts are in the `scripts/` directory. See [scripts/README.md](scripts/README.md) for details.

## Project Organization

- `docs/` - Current documentation
- `scripts/` - Utility scripts for testing and deployment
- `archive/` - Historical documentation and completed work