<div align="center">

# 🤖 JARVIS

**یک دستیار هوشمند توزیع‌شده برای Android**

[![Build APK](https://github.com/KIAN-IRANI/Jarvis-android/actions/workflows/build.yml/badge.svg)](https://github.com/KIAN-IRANI/Jarvis-android/actions)
![Version](https://img.shields.io/badge/version-4.0.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![AI](https://img.shields.io/badge/AI-LLaMA%203.3%2070B-purple)
![License](https://img.shields.io/badge/license-MIT-orange)

</div>

---

## 📖 درباره پروژه

JARVIS یک سیستم دستیار هوشمند توزیع‌شده است که شامل:
- **Android Launcher** — رابط کاربری HUD سبک Iron Man
- **Brain Server** — سرور مرکزی FastAPI با هوش مصنوعی
- **Node Mesh** — شبکه‌ای از دستگاه‌های متصل
- **AI Engine** — LLaMA 3.3 70B از طریق Groq (رایگان)

---

## 🏗️ معماری

```
📱 JARVIS Android
       │ HTTP / WebSocket
       ▼
🖥️  Brain Server (FastAPI)
       │ WebSocket Mesh
  ┌────┴────┐
  ▼         ▼
🖥️ Node 1  🖥️ Node 2  ...  🖥️ Node N
```

هر Node می‌تواند یک VPS، کامپیوتر شخصی، یا دستگاه Android (Termux) باشد.

---

## ✨ قابلیت‌ها

| قابلیت | وضعیت |
|---|---|
| Brain Server (FastAPI + WebSocket) | ✅ |
| Node Mesh Network | ✅ |
| Groq AI (LLaMA 3.3 70B) | ✅ |
| Android Launcher (HUD) | 🔄 |
| Web Dashboard | ✅ |
| Remote Command Execution | ✅ |
| Auto Node Registration | ✅ |
| Voice Engine | 🔲 |
| HUD UI Libraries | 🔲 |

---

## 🚀 نصب سریع

### Brain Server (Linux VPS)

```bash
# دانلود و اجرا
curl -s http://YOUR_BRAIN_IP:8000/install | bash
```

### Node Agent — اضافه کردن دستگاه جدید

```bash
# روی هر Linux
curl -s http://YOUR_BRAIN_IP:8000/install | bash
```

### Android App

آخرین نسخه APK را از [Releases](https://github.com/KIAN-IRANI/Jarvis-android/releases) دانلود کنید.

---

## 📡 API Endpoints

| Endpoint | Method | توضیح |
|---|---|---|
| `/` | GET | اطلاعات Brain |
| `/health` | GET | وضعیت سیستم |
| `/nodes` | GET | لیست Node‌ها |
| `/nodes/{id}/exec` | POST | اجرای دستور روی Node |
| `/chat` | POST | چت با AI |
| `/dashboard` | GET | داشبورد وب |
| `/install` | GET | اسکریپت نصب Node |
| `/agent.py` | GET | فایل Node Agent |

---

## 🛠️ تکنولوژی‌ها

**Backend:**
- Python 3.x + FastAPI
- WebSocket (اتصال دائمی Node‌ها)
- Groq API (LLaMA 3.3 70B)
- psutil (متریک سیستم)
- aiohttp (async HTTP)

**Android:**
- Kotlin + Jetpack Compose
- GitHub Actions (CI/CD → APK)

**Infrastructure:**
- Ubuntu VPS
- systemd services
- Zero cost (Groq free tier)

---

## 📚 مستندات

- [معماری کامل](docs/ARCHITECTURE.md)
- [راهنمای نصب](docs/SETUP.md)
- [نقشه راه](ROADMAP.md)

---

## 📊 وضعیت پروژه

```
فاز ۰ — Brain Server      ████████████ 100% ✅
فاز ۱ — Node Mesh         ████████████ 100% ✅
فاز ۲ — Android Launcher  ████░░░░░░░░  40% 🔄
فاز ۳ — Compression       ░░░░░░░░░░░░   0% 🔲
فاز ۴ — Voice Engine      ░░░░░░░░░░░░   0% 🔲
فاز ۵ — Skills            ░░░░░░░░░░░░   0% 🔲
فاز ۶ — HUD UI            ░░░░░░░░░░░░   0% 🔲
```

---

<div align="center">
ساخته شده با ❤️ — KIAN IRANI
</div>
