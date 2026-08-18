# SMS Forwarder (Firebase)

Forwards incoming SMS messages to a Firebase Realtime Database. 

## Features
- Reads incoming SMS and forwards them to a Firebase Realtime Database (`messages/incoming`).
- Monitors a Firebase Realtime Database path (`messages/outgoing`) for outgoing SMS commands and sends them from the device.
- Uses Firebase Presence to monitor device status.

## Setup
1. Create a Firebase project and enable Realtime Database.
2. Download the `google-services.json` file from Firebase and place it in the `app` folder.
3. Build the app using Android Studio or Gradle and install it on your device.
4. Open the app and grant the required permissions (SMS, Notification).
