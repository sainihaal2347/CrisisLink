CrisisLink
📱 Overview

CrisisLink is an Android application designed for secure, instant messaging during emergencies without relying on internet connectivity. It enables communication between devices using Bluetooth, ensuring critical messages and SOS alerts can be sent and received in situations where mobile networks are unavailable or unreliable. Ideal for outdoor adventures, disaster relief, and field teams.

🚀 Features
Offline Messaging: Direct device-to-device communication over Bluetooth.

Emergency SOS Button: Instantly sends a timestamped emergency alert.

Live Connection Status: Displays the connected device’s name for clear visibility.

Chat History: Persistent local storage of sent and received messages using Room Database.

Device Scanning & Connection: Scan for paired Bluetooth devices and establish connections easily.

Push Notifications: Real-time notifications on new incoming messages.

Persistent Background Service: Foreground service keeps Bluetooth connection alive, compatible with Android 12+.

Modern Material3 UI: Clean, responsive interface with auto-scroll chat and distinct emergency highlights.

🛠 Tech Stack
Kotlin – Primary language for Android development.

Jetpack Compose – For building a reactive and modern UI.

Room Database – Local data persistence for chat history.

Bluetooth API – Peer-to-peer communication.

Foreground Service & Binder – To maintain connection in background.

Material Design 3 – Contemporary UI theming.

📁 Project Structure
text
app/
 ├── bluetooth/                  # Bluetooth connection threads
 ├── database/                   # Room database entities and DAOs
 ├── ui/                        # Compose UI components and theming
 ├── MainActivity.kt             # Activity managing UI and service binding
 ├── BluetoothService.kt        # Foreground service handling Bluetooth communication
 └── AndroidManifest.xml         # App manifest with permissions and components
🔒 Permissions
The app requests the following critical permissions, aligned with Android 12+ requirements:

BLUETOOTH_CONNECT and BLUETOOTH_SCAN

FOREGROUND_SERVICE_CONNECTED_DEVICE

POST_NOTIFICATIONS

Location permissions (for Bluetooth scanning on older Android versions)

