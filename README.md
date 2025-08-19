# CrisisLink

A secure Android application for instant messaging during emergencies without internet connectivity. Enables device-to-device communication via Bluetooth for critical situations.

## 🚀 Key Features

- **Offline Messaging**: Direct device-to-device communication over Bluetooth
- **Emergency SOS Button**: Instantly sends timestamped emergency alerts
- **Live Connection Status**: Real-time display of connected device information
- **Chat History**: Persistent local storage using Room Database
- **Device Scanning & Connection**: Easy pairing and connection management
- **Push Notifications**: Real-time alerts for incoming messages
- **Background Service**: Maintains Bluetooth connection (Android 12+ compatible)
- **Modern UI**: Clean Material Design 3 interface with emergency highlights

## 🛠 Tech Stack

- **Kotlin** - Primary development language
- **Jetpack Compose** - Modern reactive UI framework
- **Room Database** - Local data persistence
- **Bluetooth API** - Peer-to-peer communication
- **Foreground Service & Binder** - Background connection management
- **Material Design 3** - Contemporary UI theming

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21+
- Bluetooth-enabled Android device

### Installation
1. Clone the repository
   ```bash
   git clone https://github.com/sainihaal2347/CrisisLink.git
   ```
2. Open project in Android Studio
3. Sync Gradle files
4. Enable Bluetooth permissions on your device
5. Build and run the application

## 📁 Project Structure

```
app/
├── bluetooth/                  # Bluetooth connection threads
├── database/                   # Room database entities and DAOs
├── ui/                        # Compose UI components and theming
├── MainActivity.kt             # Activity managing UI and service binding
├── BluetoothService.kt        # Foreground service handling Bluetooth communication
└── AndroidManifest.xml         # App manifest with permissions and components
```

## 📖 Usage

1. **Initial Setup**: Grant Bluetooth and location permissions
2. **Device Pairing**: Use the scan feature to discover nearby devices
3. **Start Messaging**: Connect to a paired device and begin secure communication
4. **Emergency Mode**: Use the SOS button for instant emergency alerts
5. **Background Operation**: App maintains connection even when minimized


