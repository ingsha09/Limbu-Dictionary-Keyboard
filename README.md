# Limbu Dictionary & Keyboard

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Limbu Dictionary & Keyboard Logo" width="120" height="120"/>
</p>

<p align="center">
  <b>Offline Limbu dictionary with an integrated native Sirijanga script keyboard.</b>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT"/></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"/></a>
  <a href="https://github.com"><img src="https://img.shields.io/badge/Language-Java%20%7C%20Kotlin-orange.svg" alt="Language"/></a>
</p>

---

## 📱 App Screenshots

<p align="center">
  <img src="docs/screenshots/screenshot_dictionary.png" width="220" alt="Trilingual Dictionary Search"/>
  &nbsp;&nbsp;
  <img src="docs/screenshots/screenshot_keyboard.png" width="220" alt="Native Sirijanga Keyboard"/>
  &nbsp;&nbsp;
  <img src="docs/screenshots/screenshot_bookmarks.png" width="220" alt="Offline Bookmarks"/>
  &nbsp;&nbsp;
  <img src="docs/screenshots/screenshot_about.png" width="220" alt="Verified Business Info"/>
</p>

---

## 📖 About the Project

**Limbu Dictionary & Keyboard** is an open-source Android application dedicated to preserving, promoting, and modernizing the **Limbu (Yakthung) language** and **Sirijanga script**.

Designed as a lightweight, fast, and feature-rich utility, it functions completely offline as a trilingual dictionary and includes an integrated native **Sirijanga IME (Input Method Editor) Keyboard** allowing users to type Sirijanga script across any Android application.

---

## ✨ Features

### 📚 Trilingual Dictionary

* **Multi-Language Search**: Search across **8,800+ words** in Limbu (Sirijanga script), English, and Nepali.
* **Exact & Partial Match Engine**: Dynamic search filtering by Limbu script, Romanized phonetic transliteration, or translated meanings.
* **Alphabetical Indexing**: Quick alphabet bar (`ᤀ` to `ᤜ`) for smooth scrolling and section filtering.
* **One-Tap Sharing**: Share detailed word definitions, script spellings, and translations directly to social media or messaging apps.
* **Offline Bookmarking**: Save favorite entries locally for quick vocabulary review.

### ⌨️ Native Sirijanga IME Keyboard

* **System-Wide Input**: Type native Sirijanga script anywhere on your device—SMS, WhatsApp, Notes, and browser search bars.
* **Smart Auto-Suggestions**: Real-time text prediction and word completion engine powered by a local dictionary helper (`LimbuDictionaryHelper`).
* **Clipboard Manager**: Access recently copied snippets and paste them directly from the keyboard toolbar.
* **Emoji Integration**: Built-in emoji picker embedded in the keyboard layout.
* **Haptics & Audio**: Customizable vibration strength and sound feedback on keypress.
* **Dynamic Themes**: Clean layout support with auto-adapting Light and Dark UI options.

### 🔄 Offline-First Architecture

* **Instant Offline Access**: Pre-packaged with local dictionary assets (`data.json`) for zero-latency searches without an active internet connection.
* **Silent Remote Sync**: Uses background thread execution to pull updated vocabulary datasets and keyboard dictionaries from remote API endpoints automatically when connected to the internet.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Java / Kotlin
* **UI & Layouts**: Native Android Framework, Custom XML, Inset Handling (Android 15+ Target SDK 36 compliant)
* **Data Parsing**: Gson
* **Architecture**: Android IME Service, Custom InputMethodService, BaseAdapters, Async Executor Service

---

## 🚀 Getting Started

### Prerequisites

* Android Studio or CodeAssist (Android IDE)
* Android SDK (API 21 / Android 5.0 Lollipop or higher)
* Target SDK: API 36 (Android 15+)

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ingsha09/limbu-dictionary-keyboard.git
   cd limbu-dictionary-keyboard
   ```

2. **Open in IDE:** Open the project folder in Android Studio or CodeAssist.

3. **Build APK/AAB:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Enable Keyboard

After installation, enable the Limbu keyboard:
- Go to **Settings → System → Languages & Input → Virtual Keyboard → Manage Keyboards**
- Toggle on **Limbu Keyboard**
- Switch to the Limbu keyboard from your current keyboard selector

---

## 🤝 Contributing

Contributions make the open-source community an amazing place to learn, inspire, and create. Any contributions you make toward language preservation and feature improvements are greatly appreciated.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 🏢 Organization & Support

Developed and maintained by **Ingsha Hang Subba** under **Kiso Labs** (Registered Micro-Enterprise, Govt. of India).

* **API Repository**: [limbu-dictionary-api](https://github.com/ingsha09/limbu-dictionary-api)
* **Support / Contribute**: [Support via Razorpay](https://razorpay.me/@ingshahangsubba)
* **Contact**: ingshalimbu09@gmail.com

---

## 🙏 Acknowledgements

* Special thanks to the Limbu (Yakthung) community for their cultural heritage.
* Open-source libraries and tools that made this project possible.
* All contributors and users who help preserve the Limbu language.

---

⭐ **If you find this project useful, please consider giving it a star on GitHub!**
