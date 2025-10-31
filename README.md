# Monitor v2

A comprehensive Android network monitoring application that checks the availability and status of URLs, DNS hosts, and Certificate Revocation Lists (CRLs). The app provides real-time monitoring with certificate expiry warnings and detailed status information.

## Features

### 🎯 Core Monitoring
- **URL Monitoring**: Check HTTP/HTTPS endpoint availability
- **DNS Resolution**: Verify DNS hostname resolution
- **CRL Verification**: Validate Certificate Revocation List availability and integrity
- **Certificate Tracking**: Monitor SSL certificate validity and expiry dates
- **Expiry Warnings**: Alert when certificates are expiring within 30 days

### 📱 User Interface
- **Material Design**: Modern, responsive UI with Material Components
- **Navigation Drawer**: Easy access to all sections
- **Pull-to-Refresh**: Manually trigger monitoring checks
- **Grouped Display**: Organized view of monitoring results
- **Detail Views**: Detailed information for each monitored item
- **Settings Management**: Configure URLs, DNS hosts, and CRL URLs to monitor

### 📊 Monitoring Capabilities
- Real-time status updates
- Automatic grouping by status (URL, DNS, CRL)
- Timestamp tracking for last test time
- Error message display for failed checks
- Certificate validity date ranges

## Requirements

- **Android SDK**: Minimum SDK 24 (Android 7.0), Target SDK 36
- **Java Version**: 11
- **Kotlin**: 2.0.21
- **Android Gradle Plugin**: 8.13.0
- **Android Studio**: Latest stable version recommended
- **Internet Permission**: Required for network monitoring

## Project Structure

```
Monitor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/monitor/
│   │   │   │   ├── MainActivity.kt              # Main activity with navigation
│   │   │   │   ├── data/
│   │   │   │   │   └── MonitorStatus.kt         # Data models
│   │   │   │   ├── ui/
│   │   │   │   │   ├── monitor/                 # Main monitoring screen
│   │   │   │   │   ├── settings/                # Configuration screen
│   │   │   │   │   ├── logs/                    # Logs viewer
│   │   │   │   │   ├── url/                     # URL detail view
│   │   │   │   │   ├── dns/                     # DNS detail view
│   │   │   │   │   └── crl/                     # CRL detail view
│   │   │   │   └── util/
│   │   │   │       ├── NetworkMonitor.kt        # URL monitoring logic
│   │   │   │       ├── DnsResolver.kt           # DNS resolution logic
│   │   │   │       ├── CRLVerifier.kt           # CRL verification logic
│   │   │   │       └── ConfigurationManager.kt  # Settings persistence
│   │   │   └── res/                             # Resources (layouts, strings, etc.)
│   │   └── test/                                # Unit tests
│   └── build.gradle.kts                        # App-level build configuration
├── gradle/
│   └── libs.versions.toml                      # Dependency version catalog
├── build.gradle.kts                            # Project-level build configuration
└── settings.gradle.kts                         # Gradle settings

```

## Setup Instructions

### Prerequisites

1. **Install Android Studio**
   - Download from [developer.android.com/studio](https://developer.android.com/studio)
   - Ensure Android SDK is installed (API level 24+)

2. **Install Java 11 JDK**
   - Required for compilation
   - Configure JAVA_HOME environment variable

### Installation Steps

1. **Clone or Download the Project**
   ```bash
   git clone <repository-url>
   cd Monitor
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the Monitor directory
   - Click "OK"

3. **Sync Gradle Dependencies**
   - Android Studio should automatically sync Gradle
   - If not, click "Sync Now" or go to `File > Sync Project with Gradle Files`
   - Wait for dependencies to download

4. **Configure Local Properties** (if needed)
   - Ensure `local.properties` contains your Android SDK path:
   ```properties
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```

5. **Build the Project**
   - Go to `Build > Make Project` or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)
   - Wait for build to complete

6. **Run on Device/Emulator**
   - Connect an Android device with USB debugging enabled, or
   - Create/start an Android Virtual Device (AVD)
   - Click the "Run" button or press `Shift+F10` (Windows/Linux) / `Ctrl+R` (Mac)

## Configuration

### Default Monitoring Targets

The app comes with default monitoring targets configured:

**URLs:**
- `https://pivi.xcloud.authentx.com/portal/index.html`
- `https://piv.xcloud.authentx.com/portal/index.html`

**DNS Hosts:**
- `piv.xcloud.authentx.com`
- `pivi.xcloud.authentx.com`
- `ocsp.xca.xpki.com`
- `crl.xca.xpki.com`
- `aia.xca.xpki.com`

**CRL URLs:**
- `http://crl.xca.xpki.com/CRLs/XTec_PIVI_CA1.crl`
- `http://66.165.167.225/CRLs/XTec_PIVI_CA1.crl`
- `http://152.186.38.46/CRLs/XTec_PIVI_CA1.crl`

### Adding Custom Targets

1. Open the app on your device
2. Navigate to **Settings** from the navigation drawer or bottom navigation
3. Select the appropriate tab (URLs, DNS Hosts, or CRL URLs)
4. Add or remove targets as needed
5. Configuration is automatically saved to SharedPreferences

## Usage

### Monitoring Screen

1. **Main Monitor View**
   - Shows all configured monitoring targets grouped by type
   - Displays status (up/down) for each target
   - Shows error messages for failed checks
   - Displays certificate expiry information

2. **Manual Refresh**
   - Pull down on the monitoring screen to trigger a new check
   - All targets are checked sequentially

3. **View Details**
   - Tap any monitored item to view detailed information
   - Includes full error messages, timestamps, and certificate details

### Settings Screen

- **Manage URLs**: Add, edit, or remove URLs to monitor
- **Manage DNS Hosts**: Configure DNS hostnames to check
- **Manage CRL URLs**: Set Certificate Revocation List URLs to verify

### Logs Screen

- View monitoring activity logs
- Track check history and timestamps

## Building the APK

### Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

**Note:** Release builds use debug signing by default. For production, configure proper signing in `app/build.gradle.kts`.

## Dependencies

### Core Android Libraries
- **AndroidX Core KTX**: 1.10.1
- **AndroidX AppCompat**: 1.6.1
- **Material Design**: 1.10.0
- **AndroidX RecyclerView**: 1.3.0
- **AndroidX ConstraintLayout**: 2.1.4
- **AndroidX SwipeRefreshLayout**: 1.1.0

### Architecture Components
- **AndroidX Lifecycle (ViewModels)**: 2.6.1
- **AndroidX Navigation**: 2.6.0

### Additional Libraries
- **ViewPager2**: 1.0.0
- **CardView**: 1.0.0

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture:

- **Model**: Data classes (`MonitorStatus`, `MonitorItem`)
- **View**: Fragments with ViewBinding (`MonitorFragment`, `SettingsFragment`, etc.)
- **ViewModel**: Business logic and data management (`MonitorViewModel`, `SettingsViewModel`)

### Key Components

- **NetworkMonitor**: Handles HTTP/HTTPS URL checks and SSL certificate validation
- **DnsResolver**: Performs DNS hostname resolution checks
- **CRLVerifier**: Downloads and validates Certificate Revocation Lists
- **ConfigurationManager**: Manages persistent configuration using SharedPreferences

## Permissions

- **INTERNET**: Required for network monitoring operations

## Security Notes

- The app uses permissive SSL verification for monitoring purposes (accepts self-signed certificates)
- Uses cleartext traffic (`android:usesCleartextTraffic="true"`) for HTTP CRL checks
- Certificate expiry warnings are shown for certificates expiring within 30 days

## Troubleshooting

### Build Issues

1. **Gradle Sync Failed**
   - Check internet connection
   - Verify Java 11 is installed and JAVA_HOME is set
   - Try: `File > Invalidate Caches / Restart`

2. **Dependencies Not Found**
   - Check `gradle/libs.versions.toml` for correct versions
   - Ensure Android SDK is properly installed
   - Try: `Build > Clean Project`, then `Build > Rebuild Project`

### Runtime Issues

1. **Network Checks Fail**
   - Verify internet connectivity on device
   - Check firewall settings
   - Ensure targets are reachable

2. **Configuration Not Saving**
   - Clear app data and restart
   - Check SharedPreferences storage permissions

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and single-purpose

## Version Information

- **Version Code**: 1
- **Version Name**: 1.0
- **Package Name**: `com.monitor`

## License

[Specify your license here]

## Contributing

[Add contribution guidelines if applicable]

## Support

[Add support/contact information if applicable]

---

**Note**: This application is designed for monitoring network endpoints and certificates. Ensure you have permission to monitor the configured targets before use.

