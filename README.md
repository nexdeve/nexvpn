<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:1e3a5f,100:3b82f6&height=160&section=header&text=NexVPN&fontSize=52&fontColor=ffffff&animation=fadeIn&fontAlignY=42&desc=Android%20OpenVPN%20Library&descAlignY=62&descColor=93c5fd" />

[![Maven Central](https://img.shields.io/maven-central/v/ai.nextech/nexvpn?style=for-the-badge&color=3b82f6)](https://central.sonatype.com/artifact/ai.nextech/nexvpn)
[![License](https://img.shields.io/badge/License-Apache_2.0-6366f1?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Android-21+-10b981?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Author](https://img.shields.io/badge/By-NexDeve-076AF4?style=for-the-badge)](https://nexdeve.com)

**A robust, modern OpenVPN client library for Android 14+**
Built on [ics-openvpn](https://github.com/schwabe/ics-openvpn) · Made by [nexdeve.com](https://nexdeve.com)

[Installation](#-installation) · [Setup](#-setup) · [Usage](#-usage) · [API](#-api-reference) · [Flutter Plugin](https://github.com/nexdeve/nexvpn_flutter) · [Python](https://github.com/nexdeve/nexvpn_python)

</div>

---

## ✨ Features

- 🚀 **Simple API** — connect in just a few lines
- 📡 **Live Stats** — real-time speed & data usage
- 🔔 **Foreground Service** — survives background kill
- 🔑 **Flexible Auth** — username/password or certificate
- 📁 **Profile Loading** — from assets or raw String
- 🤖 **Android 14+ Ready** — fully compliant with API 34

---

## 📦 Installation

```gradle
dependencies {
    implementation 'ai.nextech:nexvpn:1.0.0'
}
```

---

## ⚙️ Setup

```gradle
android {
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 💡 Usage

### Java

```java
NexVpn nexVpn = new NexVpn(this);

nexVpn.attachFromAsset("server.ovpn", "username", "password");

nexVpn.setVpnListener(new NexVpn.VpnListener() {
    @Override public void onVpnConnected() { }
    @Override public void onVpnStopped()   { }
    @Override public void onStatusUpdate(String status) { }
    @Override public void onError(String errorMessage)  { }
    @Override public void onSpeedUpdate(long dlBytes, long ulBytes,
                                        long dlSpeed, long ulSpeed) {
        String speed = NexVpn.formatSpeed(dlSpeed);  // "1.5 MB/s"
        String usage = NexVpn.formatBytes(dlBytes);  // "256 MB"
    }
});

if (nexVpn.hasVpnPermission()) nexVpn.startVpn();
else nexVpn.requestVpnPermission();

// Cleanup
nexVpn.release();
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
    override fun onSpeedUpdate(dlBytes: Long, ulBytes: Long,
                               dlSpeed: Long, ulSpeed: Long) { }
})
if (nexVpn.hasVpnPermission()) nexVpn.startVpn()
```

---

## 📖 API Reference

| Method | Description |
|--------|-------------|
| `attachFromAsset(file, user, pass)` | Load `.ovpn` from `assets/` |
| `attachFromString(config, user, pass)` | Load `.ovpn` from raw String |
| `startVpn()` | Start VPN tunnel |
| `stopVpn()` | Stop VPN tunnel |
| `isConnected()` | Is VPN connected? |
| `getCurrentState()` | `VpnState` enum |
| `hasVpnPermission()` | VPN permission granted? |
| `NexVpn.formatSpeed(bps)` | `→ "1.5 MB/s"` |
| `NexVpn.formatBytes(b)` | `→ "256 MB"` |
| `release()` | Free resources in `onDestroy` |

---

## 🌐 NexVPN Ecosystem

| Platform | Repo | Install |
|----------|------|---------|
| 🤖 Android (this) | [nexvpn](https://github.com/nexdeve/nexvpn) | `implementation 'ai.nextech:nexvpn:1.0.0'` |
| 💙 Flutter | [nexvpn_flutter](https://github.com/nexdeve/nexvpn_flutter) | `nexvpn_flutter: ^1.0.0` |
| 🐍 Python | [nexvpn_python](https://github.com/nexdeve/nexvpn_python) | `pip install nexvpn` |

---

## 🤝 Credits

- [ics-openvpn](https://github.com/schwabe/ics-openvpn) by Arne Schwabe

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:3b82f6,100:6366f1&height=80&section=footer" />

Made with ❤️ by [**NexDeve**](https://nexdeve.com) · [nexdeve.com](https://nexdeve.com) · [Telegram](https://t.me/+c34_uTIBJEpkZGM9)

</div>
