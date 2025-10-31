# Monitor v2 - Detailed Instructions

This document provides step-by-step instructions for setting up, building, and using the Monitor v2 application.

## Table of Contents

1. [Initial Setup](#initial-setup)
2. [Building the Application](#building-the-application)
3. [Installing on a Device](#installing-on-a-device)
4. [Configuring Monitoring Targets](#configuring-monitoring-targets)
5. [Using the Application](#using-the-application)
6. [Troubleshooting](#troubleshooting)

---

## Initial Setup

### Step 1: Install Required Software

1. **Android Studio**
   - Download from: https://developer.android.com/studio
   - Install following the wizard
   - During installation, ensure "Android SDK" and "Android SDK Platform" are selected

2. **Java Development Kit (JDK) 11**
   - Download from: https://adoptium.net/ or Oracle's website
   - Install and configure JAVA_HOME environment variable:
     - Windows: Add to System Environment Variables
     - Mac/Linux: Add to `~/.bashrc` or `~/.zshrc`

3. **Android SDK Components**
   - Open Android Studio
   - Go to `Tools > SDK Manager`
   - Install:
     - Android SDK Platform 36 (or latest)
     - Android SDK Platform-Tools
     - Android SDK Build-Tools

### Step 2: Prepare the Project

1. **Extract/Clone the Project**
   ```
   If downloaded as ZIP:
   - Extract to a location (e.g., C:\Users\YourName\AndroidStudioProjects\Monitor)
   
   If using Git:
   - git clone <repository-url>
   - cd Monitor
   ```

2. **Verify Project Structure**
   Ensure these files/folders exist:
   - `app/build.gradle.kts`
   - `gradle/libs.versions.toml`
   - `build.gradle.kts`
   - `settings.gradle.kts`
   - `app/src/main/java/com/monitor/`

### Step 3: Open in Android Studio

1. Launch Android Studio
2. Select **"Open"** (or **"File > Open"**)
3. Navigate to the Monitor project folder
4. Click **"OK"**
5. Wait for Gradle sync to complete (may take a few minutes on first open)

### Step 4: Configure SDK Path (if needed)

If you see SDK path errors:

1. Locate `local.properties` in the project root
2. If it doesn't exist, create it
3. Add your SDK path:
   ```
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```
   (Adjust path for your system - Windows uses double backslashes)

---

## Building the Application

### Build from Android Studio

1. **Make Project**
   - Menu: `Build > Make Project`
   - Shortcut: `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (Mac)
   - Wait for "BUILD SUCCESSFUL" message

2. **Check for Errors**
   - Review the "Build" tab at the bottom
   - Fix any compilation errors before proceeding

### Build from Command Line

**Windows:**
```cmd
gradlew.bat assembleDebug
```

**Mac/Linux:**
```bash
./gradlew assembleDebug
```

**Output Location:**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

1. Edit `app/build.gradle.kts` to configure signing (if needed)
2. Run:
   ```bash
   ./gradlew assembleRelease
   ```
3. Output: `app/build/outputs/apk/release/app-release.apk`

---

## Installing on a Device

### Option 1: USB Connected Device

1. **Enable Developer Options on Device**
   - Go to `Settings > About Phone`
   - Tap "Build Number" 7 times
   - Return to `Settings`
   - Find "Developer Options"

2. **Enable USB Debugging**
   - Open "Developer Options"
   - Enable "USB Debugging"
   - Accept USB debugging prompt on device

3. **Connect Device**
   - Connect device via USB
   - Accept "Allow USB Debugging?" prompt on device

4. **Run from Android Studio**
   - Select your device from the device dropdown
   - Click the green "Run" button
   - App will install and launch automatically

### Option 2: Android Emulator

1. **Create AVD (Android Virtual Device)**
   - Open Android Studio
   - Go to `Tools > Device Manager`
   - Click "Create Device"
   - Select a device (e.g., Pixel 5)
   - Select system image (API 24 or higher)
   - Click "Finish"

2. **Start Emulator**
   - In Device Manager, click the play button next to your AVD
   - Wait for emulator to boot

3. **Run Application**
   - Select emulator from device dropdown
   - Click "Run" button

### Option 3: Install APK Directly

1. **Transfer APK to Device**
   - Copy `app-debug.apk` to device storage
   - Or use ADB: `adb install app-debug.apk`

2. **Install on Device**
   - On device, open file manager
   - Navigate to APK location
   - Tap APK file
   - Allow installation from unknown sources if prompted
   - Tap "Install"

---

## Configuring Monitoring Targets

### Accessing Settings

1. Launch the Monitor app
2. Open navigation drawer (hamburger menu) or go to Settings tab
3. Tap "Settings"

### Adding URLs to Monitor

1. In Settings, select **"URLs"** tab
2. Tap the "+" button (or "Add" button)
3. Enter full URL (must include `http://` or `https://`)
   - Example: `https://example.com`
4. Tap "Save" or "OK"
5. URL will appear in the list

### Adding DNS Hosts to Monitor

1. In Settings, select **"DNS Hosts"** tab
2. Tap the "+" button
3. Enter hostname (without `http://` or `https://`)
   - Example: `example.com`
4. Tap "Save"
5. Hostname will appear in the list

### Adding CRL URLs to Monitor

1. In Settings, select **"CRL URLs"** tab
2. Tap the "+" button
3. Enter CRL URL (typically `http://...crl` or `https://...crl`)
   - Example: `http://crl.example.com/path/to/file.crl`
4. Tap "Save"
5. CRL URL will appear in the list

### Removing Targets

1. Navigate to the appropriate tab (URLs, DNS Hosts, or CRL URLs)
2. Find the item in the list
3. Tap delete/remove button (usually trash icon or swipe)
4. Confirm deletion

### Editing Targets

1. Tap on an item in the list
2. Modify the URL/hostname
3. Save changes

---

## Using the Application

### Main Monitor Screen

1. **Viewing Status**
   - Open app (Monitor screen is default)
   - See all configured targets grouped by type
   - Green indicator = Online/Working
   - Red indicator = Offline/Failed

2. **Understanding Status Display**
   - **URL Status**: Shows if HTTP/HTTPS endpoint is reachable
   - **DNS Status**: Shows if hostname resolves correctly
   - **CRL Status**: Shows if CRL is downloadable and valid
   - **Certificate Info**: For HTTPS URLs, shows expiry dates

3. **Manual Refresh**
   - Pull down on the screen (swipe down gesture)
   - All targets will be checked again
   - Wait for refresh spinner to complete

4. **Viewing Details**
   - Tap any item in the monitor list
   - See detailed information:
     - Full error messages
     - Timestamps
     - Certificate validity dates
     - Additional status information

### Logs Screen

1. Navigate to "Logs" from navigation drawer or bottom navigation
2. View monitoring activity history
3. See timestamps and status changes

### Settings Screen

1. Configure what to monitor (see [Configuring Monitoring Targets](#configuring-monitoring-targets) above)
2. Settings are automatically saved
3. Changes take effect immediately

---

## Troubleshooting

### Build Issues

**Problem: Gradle sync fails**
- **Solution 1**: Check internet connection
- **Solution 2**: Verify `local.properties` has correct SDK path
- **Solution 3**: In Android Studio: `File > Invalidate Caches / Restart`

**Problem: "Could not find or load main class"**
- **Solution**: Ensure JDK 11 is installed and JAVA_HOME is set correctly

**Problem: "SDK location not found"**
- **Solution**: Create `local.properties` with: `sdk.dir=YOUR_SDK_PATH`

**Problem: Dependency errors**
- **Solution**: 
  1. `File > Sync Project with Gradle Files`
  2. `Build > Clean Project`
  3. `Build > Rebuild Project`

### Runtime Issues

**Problem: App crashes on launch**
- **Solution 1**: Check logcat in Android Studio for error messages
- **Solution 2**: Ensure device has Android 7.0 (API 24) or higher
- **Solution 3**: Clear app data: `Settings > Apps > Monitor > Clear Data`

**Problem: Network checks always fail**
- **Solution 1**: Verify device has internet connection
- **Solution 2**: Check if targets are reachable from device browser
- **Solution 3**: Verify URLs are correctly formatted (include `http://` or `https://`)

**Problem: DNS checks fail**
- **Solution 1**: Verify hostnames are correct (no `http://`)
- **Solution 2**: Test DNS resolution from device using network tools

**Problem: CRL verification fails**
- **Solution 1**: Verify CRL URLs are accessible
- **Solution 2**: Check if CRL format is valid
- **Solution 3**: Some CRLs may require HTTP (not HTTPS)

**Problem: Settings not saving**
- **Solution 1**: Ensure app has storage permissions
- **Solution 2**: Clear app data and reconfigure
- **Solution 3**: Check SharedPreferences access in logcat

### Device Connection Issues

**Problem: Device not detected**
- **Solution 1**: Enable USB debugging on device
- **Solution 2**: Install device drivers (for Windows)
- **Solution 3**: Run `adb devices` to verify connection
- **Solution 4**: Try different USB cable/port

**Problem: Emulator is slow**
- **Solution 1**: Increase RAM allocation in AVD settings
- **Solution 2**: Enable hardware acceleration (Intel HAXM or AMD Hyper-V)
- **Solution 3**: Use a device profile with lower resolution

### Certificate Warnings

**Problem: "Certificate expiry warning" shown**
- **Explanation**: This is informational - certificates expiring within 30 days trigger warnings
- **Action**: Review certificate expiration dates for affected URLs

**Problem: "SSL context setup failed"**
- **Explanation**: Some HTTPS sites may have certificate issues
- **Action**: Check if site is accessible from browser; may need to update certificate chain

---

## Advanced Usage

### Command Line Installation

```bash
# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Reinstall (uninstall first)
adb uninstall com.monitor
adb install app/build/outputs/apk/debug/app-debug.apk

# View app logs
adb logcat | grep Monitor
```

### Exporting Configuration

Configuration is stored in SharedPreferences. To backup:
```bash
adb backup -f backup.ab com.monitor
```

### Testing Specific Targets

1. Use Settings to add test URLs/hosts
2. Monitor screen will show results
3. Check Logs screen for detailed information

---

## Quick Reference

### Default Monitoring Targets

The app includes these default targets for initial testing:

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

### Keyboard Shortcuts (Android Studio)

- `Ctrl+F9` / `Cmd+F9`: Build project
- `Shift+F10` / `Ctrl+R`: Run app
- `Ctrl+Shift+F10` / `Ctrl+R`: Run selected configuration
- `F9`: Debug app

### Common Gradle Commands

```bash
./gradlew clean              # Clean build artifacts
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew installDebug       # Build and install on connected device
./gradlew test               # Run unit tests
```

---

## Getting Help

If you encounter issues not covered here:

1. Check the main README.md for general information
2. Review Android Studio logcat for error messages
3. Verify all prerequisites are installed correctly
4. Ensure project files are complete and not corrupted

---

**Last Updated**: Version 1.0

