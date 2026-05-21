<div align="center">

# 🤖 JARVIS

### Distributed AI assistant for Android — Iron Man-style HUD launcher

**🇮🇷 [نسخه فارسی پایین صفحه](#معرفی-فارسی)**

[![Build APK](https://github.com/KIAN-IRANI/Jarvis-android/actions/workflows/build.yml/badge.svg)](https://github.com/KIAN-IRANI/Jarvis-android/actions)
[![Version](https://img.shields.io/badge/version-4.0.0-blue?style=for-the-badge)](https://github.com/KIAN-IRANI/Jarvis-android/releases)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![AI](https://img.shields.io/badge/AI-LLaMA%203.3%2070B-purple?style=for-the-badge)](https://groq.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)
[![Donate TRC20](https://img.shields.io/badge/Donate-TRON%20TRC20-EF0027?style=for-the-badge&logo=tron)](DONATE.md)

</div>

---

## 🎯 What is this?

**JARVIS** is a distributed AI assistant ecosystem with three components:

- 📱 **Android Launcher** — Iron Man HUD-style home screen
- 🧠 **Brain Server** — FastAPI backend with LLM integration
- 🔗 **Node Mesh** — every device on your network is a Node

The AI brain runs **LLaMA 3.3 70B via Groq** (free tier), and any device on your network can talk to it through the Node Agent. Your Android phone becomes a voice-activated assistant that knows about your home, your servers, your schedule.

---

## 🏗️ Architecture

```
📱 JARVIS Android (Kotlin)
    ↓  HTTPS + WebSocket
🧠 Brain Server (Python/FastAPI on VPS)
    ↓  Groq API
🤖 LLaMA 3.3 70B (free)

   ↑
🔗 Node Agents (Python)
   └── Home server, NAS, IoT devices, etc.
```

---

## ✨ Features

### Android Launcher
- 🎨 **HUD overlay** — Iron Man-inspired widget showing system stats, calendar, weather, AI suggestions
- 🎤 **Voice activation** — "Hey JARVIS"
- 📊 **Quick info cards** — battery, network, time, smart home
- 🎯 **App launcher** with AI-suggested apps based on context

### Brain Server
- 🧠 **LLM integration** — LLaMA 3.3 70B / Mixtral / Gemma via Groq
- 💬 **Multi-turn conversations** with session memory
- 🔌 **Plugin system** — extend with custom commands
- 📡 **WebSocket** for real-time push to Android
- 🔐 **Per-user auth** with token-based access

### Node Agents
- 🐍 **Lightweight Python** (~30MB RAM)
- 🔗 **Auto-discovery** on local network
- 📊 **System metrics** reported to Brain
- 🎛️ **Remote command** execution (with security restrictions)

---

## 📦 Components

| Folder | Description |
|--------|-------------|
| [`android/`](android/) | Kotlin Android launcher (Jetpack Compose) |
| [`app/`](app/) | Legacy Android module |
| [`brain/`](brain/) | FastAPI brain server (deploy on VPS) |
| [`node-agent/`](node-agent/) | Python agent for non-Android devices |
| [`dashboard.html`](dashboard.html) | Web dashboard for brain monitoring |
| [`docker-compose.yml`](docker-compose.yml) | One-shot deploy for brain + dashboard |

---

## 🚀 Quick Start

### Brain Server (VPS or local)

```bash
git clone https://github.com/KIAN-IRANI/Jarvis-android.git
cd Jarvis-android
cp .env.example .env
# Edit .env with your Groq API key and admin token
docker-compose up -d
```

Brain will be available at `http://localhost:8000`.

### Android App

Download latest APK from [Releases](https://github.com/KIAN-IRANI/Jarvis-android/releases) → install → configure Brain URL.

### Node Agent (other devices)

```bash
pip install -r node-agent/requirements.txt
python node-agent/agent.py --brain http://your-brain:8000
```

---

## 🗺️ Roadmap

See [ROADMAP.md](ROADMAP.md) for detailed plans. Highlights:

- [ ] Local LLM fallback (Ollama support)
- [ ] Voice synthesis (offline TTS)
- [ ] Custom wake-word training
- [ ] Smart home integrations (Home Assistant, Tasmota)
- [ ] Multi-user with role-based access

---

## 🛠️ Tech Stack

- **Android:** Kotlin + Jetpack Compose
- **Brain:** Python 3.11+ + FastAPI + WebSocket
- **AI:** Groq API (LLaMA 3.3 70B default, configurable)
- **Storage:** SQLite (brain memory)
- **Deploy:** Docker + docker-compose
- **Build:** Gradle 8 (Android), uv (Python)

---

## 🤝 Contributing

PRs welcome! Especially:
- 🌍 Translations
- 🎨 UI/HUD widgets
- 🔌 New plugin integrations
- 📖 Documentation improvements

See [CONTRIBUTING.md](CONTRIBUTING.md) (coming soon).

---

## 💝 Support this project

If you find this project useful, please consider supporting via **Tron TRC20**:

```
TEVuoZ7574341zbc8pc5jrrBrgqGGMys5q
```

(Accepts USDT, USDC, TRX. See [DONATE.md](DONATE.md) for full details.)

---

## 🆘 Support & Community

- 💬 Telegram: [@Kian_irani_t](https://t.me/Kian_irani_t)
- 📢 Channel: [@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)

---

## 📄 License

MIT — see [LICENSE](LICENSE)

---

<div align="center">

## معرفی فارسی

</div>

### 🤖 JARVIS — دستیار هوشمند توزیع‌شده

یه سیستم اکوسیستم AI با ۳ بخش:

- 📱 **لانچر اندروید** — رابط کاربری HUD سبک آیرون مَن
- 🧠 **Brain Server** — سرور FastAPI با هوش مصنوعی
- 🔗 **Node Mesh** — هر دستگاه شبکه‌ت یه Node

موتور AI با **LLaMA 3.3 70B از Groq** (رایگان) کار می‌کنه. گوشی اندرویدت می‌شه یه دستیار صوتی که از خونه، سرورها، و برنامه‌ت خبر داره.

### 🏗️ معماری

```
📱 JARVIS Android (Kotlin)
    ↓  HTTPS + WebSocket
🧠 Brain Server (Python/FastAPI روی VPS)
    ↓  Groq API
🤖 LLaMA 3.3 70B (رایگان)
```

### ✨ ویژگی‌ها

- 🎨 **HUD overlay** سبک آیرون من — آمار سیستم، تقویم، آب‌وهوا
- 🎤 **فعال‌سازی صوتی** — «هی جارویس»
- 🧠 **LLaMA 3.3 70B** از Groq (رایگان)
- 🔗 **Node Mesh** — همه دستگاه‌های شبکه قابل کنترل
- 🐍 **Node Agent سبک** (~۳۰ مگ RAM)

### 🚀 شروع سریع

```bash
git clone https://github.com/KIAN-IRANI/Jarvis-android.git
cd Jarvis-android
cp .env.example .env
# .env رو با کلید Groq و توکن admin ویرایش کن
docker-compose up -d
```

اپلیکیشن اندرویدش رو از [Releases](https://github.com/KIAN-IRANI/Jarvis-android/releases) دانلود کن.

### 💬 ارتباط

- 💬 پشتیبانی: [@Kian_irani_t](https://t.me/Kian_irani_t)
- 📢 کانال: [@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)

### 💝 حمایت

ترون TRC20 (USDT/USDC/TRX):
```
TEVuoZ7574341zbc8pc5jrrBrgqGGMys5q
```

جزئیات: [DONATE.md](DONATE.md)

---

<div align="center">

⭐ **Star if you like the idea!**

</div>
