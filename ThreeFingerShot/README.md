# ThreeFingerShot

A simple Android application that provides a system-wide three-finger swipe down gesture to capture screenshots.

## Features
- Detects a three-finger swipe down gesture anywhere on the screen.
- Captures the current screen display.
- Saves screenshots in `Pictures/ThreeFingerShot/`.
- Uses a background foreground service to ensure it continuously runs.

## Setup Instructions

1. **Install the App**: Compile and install the provided Debug APK from GitHub Actions, or clone this repository and build via Android Studio.
2. **Open the App**: Launch "ThreeFingerShot" from your app drawer.
3. **Grant Permissions**:
   - The app will prompt you for necessary permissions, such as Overlay permission (to display the transparent touch detector), Notification permissions (to maintain a Foreground Service), and Storage permissions.
4. **Enable Accessibility Service**:
   - Tap "Open Accessibility Settings" in the app.
   - Scroll to "ThreeFingerShot" and enable the Accessibility Service.
5. **Ready**: The status inside the app will change to "Service Running".

## How to use
Once the service is running, simply use three fingers simultaneously to swipe down from the top area of the screen. Wait a moment, and the screen capture consent dialog (provided by Android system) will show up for the first time. Tap "Start Now". Successive gestures will silently capture the screen and save the screenshot directly.
