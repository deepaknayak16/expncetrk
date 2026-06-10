# Expense Tracker

An Android expense tracking application with SMS parsing and cloud backup capabilities.

## Features

- **SMS Transaction Parsing**: Automatically parse banking SMS messages from major banks
- **Local Backup & Restore**: Export and import your transaction data to/from local storage
- **Google Drive Sync**: Backup and restore your data to/from Google Drive
- **Dashboard**: View spending summaries and recent transactions
- **Transaction Management**: Add, view, and manage all your transactions

## Google Drive Backup & Sync

The app supports backing up your transaction data to Google Drive for safe keeping and cross-device synchronization.

### Setup

1. **Sign In**: Go to Settings → Profile & Settings → Sign In to Google
2. **Backup**: Tap "Backup to Google Drive" to upload your current data
3. **Restore**: Tap "Restore from Google Drive" to download and restore data from the cloud

### Implementation Details

The Google Drive sync feature consists of:

- **GoogleDriveSyncManager** (`core/sync/`): Handles Google Drive API interactions
- **SyncBackupToGoogleDriveUseCase** (`domain/usecase/sync/`): Business logic for uploading backups
- **RestoreBackupFromGoogleDriveUseCase** (`domain/usecase/sync/`): Business logic for restoring backups
- **SettingsViewModel**: Manages UI state and coordinates sync operations

### Required Dependencies

Add these to your `build.gradle.kts`:

```kotlin
// Google Play Services Auth
implementation("com.google.android.gms:play-services-auth:20.7.0")

// Google API Client for Android
implementation("com.google.api-client:google-api-client-android:2.2.0")

// Google Drive API
implementation("com.google.apis:google-api-services-drive:v3-rev20230815-2.0.0")

// Gson for JSON serialization
implementation("com.google.code.gson:gson:2.10.1")
```

### Permissions

Add required permissions to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
```

## Project Structure

```
├── core/           # Core functionality (parsers, SMS, sync)
├── data/           # Data layer (models, repositories, database)
├── di/             # Dependency injection modules
├── domain/         # Business logic (use cases, domain models)
└── ui/             # Compose UI screens and view models
```

## License

See LICENSE file for details.