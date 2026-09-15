<div align="center">

<img src="https://img.shields.io/badge/NexVPN-1.0.0-blue?style=for-the-badge&logo=android&logoColor=white" />

# 🔐 NexVPN

**A robust, modern OpenVPN client library for Android**  
Built on the trusted core of [ics-openvpn](https://github.com/schwabe/ics-openvpn) · Optimized for Android 14+ (API 34+)

[![Maven Central](https://img.shields.io/maven-central/v/ai.nextech/nexvpn?style=flat-square&color=brightgreen)](https://central.sonatype.com/artifact/ai.nextech/nexvpn)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%2021+-green?style=flat-square&logo=android)](https://developer.android.com)
[![API](https://img.shields.io/badge/Min%20SDK-21-orange?style=flat-square)](https://developer.android.com)

[Installation](#-installation) · [Setup](#-setup) · [Usage](#-usage) · [API](#-api-reference) · [Flutter Plugin](https://github.com/nexdeve/nexvpn_flutter)

</div>

---

## ✨ Features

- 🚀 **Simple API** — connect in just a few lines of code
- 📡 **Live Stats** — real-time download/upload speed & usage
- 🔔 **Foreground Service** — persistent notification, survives background kill
- 🔑 **Flexible Auth** — username/password or certificate-based
- 📁 **Profile Loading** — from assets folder or raw String
- 🤖 **Android 14+ Ready** — fully compliant with API 34 FGS policies

---

## 📦 Installation

Add to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'ai.nextech:nexvpn:1.0.0'
}
```

---

## ⚙️ Setup

### 1. Packaging Configuration

```gradle
android {
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
```

### 2. Permissions (`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 3. Google Play FGS Declaration

For Android 14+ (API 34+), declare in **Google Play Console → App Content → Foreground Services**:

| Field | Value |
|-------|-------|
| **FGS Type** | `systemExempted` |
| **Use Case** | VpnService to establish and manage an encrypted OpenVPN tunnel |

---

## 💡 Usage

### Java

```java
// 1. Initialize
NexVpn nexVpn = new NexVpn(this);

// 2. Request notification permission (Android 13+)
if (!nexVpn.hasNotificationPermission()) {
    nexVpn.requestNotificationPermission();
}

// 3. Attach profile
nexVpn.attachFromAsset("server.ovpn", "username", "password");
// or: nexVpn.attachFromString(ovpnConfigString, "username", "password");

// 4. Set listener
nexVpn.setVpnListener(new NexVpn.VpnListener() {
    @Override public void onVpnConnected() { /* tunnel established */ }
    @Override public void onVpnStopped()   { /* disconnected */ }
    @Override public void onStatusUpdate(String status) { /* state change */ }
    @Override public void onError(String errorMessage)  { /* error */ }

    @Override
    public void onSpeedUpdate(long dlBytes, long ulBytes, long dlSpeed, long ulSpeed) {
        String speed = NexVpn.formatSpeed(dlSpeed);  // "1.5 MB/s"
        String usage = NexVpn.formatBytes(dlBytes);  // "256 MB"
    }
});

// 5. Connect
if (nexVpn.hasVpnPermission()) {
    nexVpn.startVpn();
} else {
    nexVpn.requestVpnPermission();
}

// 6. Disconnect
nexVpn.stopVpn();

// 7. Cleanup
@Override
protected void onDestroy() {
    super.onDestroy();
    nexVpn.release();
}
```

### Kotlin

```kotlin
val nexVpn = NexVpn(this)

nexVpn.attachFromAsset("server.ovpn", "username", "password")

nexVpn.setVpnListener(object : NexVpn.VpnListener {
    override fun onVpnConnected()              { }
    override fun onVpnStopped()                { }
    override fun onStatusUpdate(status: String){ }
    override fun onError(errorMessage: String) { }
    override fun onSpeedUpdate(dlBytes: Long, ulBytes: Long, dlSpeed: Long, ulSpeed: Long) {
        val speed = NexVpn.formatSpeed(dlSpeed)
        val usage = NexVpn.formatBytes(dlBytes)
    }
})

if (nexVpn.hasVpnPermission()) nexVpn.startVpn()
else nexVpn.requestVpnPermission()
```

---

## 📖 API Reference

### Profile

| Method | Description |
|--------|-------------|
| `attachFromAsset(fileName, user, pass)` | Load `.ovpn` from `assets/` folder |
| `attachFromString(config, user, pass)` | Load `.ovpn` from raw String |

### Control

| Method | Description |
|--------|-------------|
| `startVpn()` | Start VPN connection |
| `stopVpn()` | Stop VPN connection |
| `release()` | Release resources (call in `onDestroy`) |

### State

| Method | Returns | Description |
|--------|---------|-------------|
| `isConnected()` | `boolean` | Is VPN currently connected? |
| `getCurrentState()` | `VpnState` | Current state enum |
| `hasVpnPermission()` | `boolean` | VPN permission granted? |
| `hasNotificationPermission()` | `boolean` | Notification permission granted? |

### Utilities

| Method | Example Output |
|--------|---------------|
| `NexVpn.formatSpeed(bytesPerSec)` | `"1.5 MB/s"` |
| `NexVpn.formatBytes(totalBytes)` | `"256 MB"` |

### VpnListener Events

| Callback | Triggered When |
|----------|---------------|
| `onVpnConnected()` | Tunnel fully established |
| `onVpnStopped()` | VPN disconnected |
| `onStatusUpdate(status)` | Connection state changes |
| `onError(message)` | Connection or auth error |
| `onSpeedUpdate(...)` | Every second with live stats |

---

## 🌐 Also Available

| Platform | Package |
|----------|---------|
| 🤖 Android (this) | `ai.nextech:nexvpn:1.0.0` |
| 💙 Flutter | [nexvpn_flutter](https://github.com/nexdeve/nexvpn_flutter) |
| 🐍 Python | Coming soon |

---

## 🤝 Credits

- **[ics-openvpn](https://github.com/schwabe/ics-openvpn)** by Arne Schwabe — the rock-solid OpenVPN core for Android

---

## 📄 License

```
Copyright 2026 NexTech

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```

<div align="center">
Made with ❤️ by <a href="https://github.com/nexdeve">NexDeve</a>
</div>
