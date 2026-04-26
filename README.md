<div align="center">

<img src="docs/assets/jarvis-logo.png" width="120" alt="JARVIS Logo"/>

# J.A.R.V.I.S
### Just A Rather Very Intelligent System

**یک دستیار هوشمند توزیع‌شده برای اندروید**  
*A Distributed AI Assistant & Android Launcher*

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)]()
[![Backend](https://img.shields.io/badge/Backend-Python%20%7C%20FastAPI-orange.svg)]()
[![Status](https://img.shields.io/badge/Status-In%20Development-yellow.svg)]()
[![GitHub](https://img.shields.io/badge/GitHub-Kian--irani-black?logo=github)](https://github.com/Kian-irani)

[🇮🇷 فارسی](#-معرفی) • [🇬🇧 English](#-introduction) • [🗺️ Roadmap](ROADMAP.md) • [📊 Projects](https://github.com/Kian-irani/jarvis-android/projects)

</div>

---

## 🇮🇷 معرفی

JARVIS یک سیستم دستیار هوشمند شخصی است که:

- **جایگزین هوم‌اسکرین اندروید** می‌شود
- از طریق **صدا** همه کارها را انجام می‌دهد
- به یک **شبکه توزیع‌شده از دستگاه‌ها** متصل است
- هر دستگاه جدید با **یک دستور ترمینال** اضافه می‌شود
- همه دستگاه‌ها از **گوشی** قابل کنترل هستند

### ✨ قابلیت‌ها

- 🎙️ دستیار صوتی با Wake Word فارسی
- 📱 لانچر کامل اندروید با تم HUD
- 🌐 شبکه Mesh از دستگاه‌ها (گوشی، کامپیوتر، VPS)
- 🤖 پشتیبانی از چندین مدل AI (Gemini، Groq، Ollama)
- 📂 دسترسی به فایل‌های همه دستگاه‌ها از یک اپ
- 💻 ترمینال روی همه دستگاه‌ها از گوشی
- 📊 مانیتور لحظه‌ای همه سیستم‌ها
- 🔌 WiFi، بلوتوث، تماس، آلارم و کنترل کامل گوشی

---

## 🇬🇧 Introduction

JARVIS is a personal distributed AI assistant that:

- **Replaces the Android home screen**
- Controls everything via **voice commands**
- Connects to a **distributed mesh network** of devices
- Any new device joins with **one terminal command**
- All devices are accessible **from your phone**

### ✨ Features

- 🎙️ Voice assistant with custom Wake Word
- 📱 Full Android launcher with HUD theme
- 🌐 Device Mesh Network (phones, PC, VPS)
- 🤖 Multi-model AI support (Gemini, Groq, Ollama)
- 📂 Access files across all devices from one app
- 💻 Remote terminal on any device from your phone
- 📊 Real-time system monitor for all nodes
- 🔌 Full phone control (WiFi, BT, Calls, Alarms)

---

## 🏗️ Architecture / معماری

```
┌─────────────────────────────────────────────────────┐
│                  JARVIS MESH NETWORK                │
│                                                     │
│  📱 Android (Launcher)  ◄──►  🖥️ VPS (Brain)       │
│                                    │                │
│              ┌─────────────────────┤                │
│              ▼                     ▼                │
│        💻 Computer            🖥️ VPS 2              │
│        [Node]                 [Node]                │
│              ▼                                      │
│        📱 Phone 2                                   │
│        [Node]                                       │
└─────────────────────────────────────────────────────┘

One command to join / یک دستور برای اتصال:
  curl -s https://your-vps/join.sh | bash
```

---

## 🗺️ Roadmap / نقشه راه

### فاز ۰ — زیرساخت / Phase 0 — Infrastructure
- [ ] Brain Server روی VPS (FastAPI + PostgreSQL + Redis)
- [ ] LiteLLM Gateway (Gemini + Groq + Ollama)
- [ ] n8n Workflow Automation
- [ ] Node Registry (WebSocket)

### فاز ۱ — Node Agent / Phase 1 — Node Agent
- [ ] Auto-detection اسکریپت (Linux/Windows/Android)
- [ ] شناسایی خودکار منابع (CPU/GPU/RAM/Disk)
- [ ] اتصال دائم WebSocket به Brain
- [ ] Task execution (shell, files, AI)
- [ ] نصب به عنوان سرویس سیستم

### فاز ۲ — اپ اندروید / Phase 2 — Android App
- [ ] Home Launcher (`category.HOME`)
- [ ] App Drawer
- [ ] Quick Settings Panel
- [ ] Mesh Panel (وضعیت همه Node‌ها)
- [ ] File Browser (همه دستگاه‌ها)
- [ ] Remote Terminal

### فاز ۳ — صدا / Phase 3 — Voice Engine
- [ ] Wake Word Detection
- [ ] Vosk STT (آفلاین / Offline)
- [ ] Text-to-Speech (JARVIS-style)
- [ ] Intent Detection

### فاز ۴ — Skills
- [ ] کنترل سیستم (WiFi/BT/Flashlight)
- [ ] تماس و پیامک
- [ ] آلارم و تایمر
- [ ] مکالمه با AI
- [ ] آب‌وهوا و اخبار

### فاز ۵ — رابط کاربری / Phase 5 — UI
- [ ] تم HUD / Iron Man
- [ ] انیمیشن‌های Compose
- [ ] داشبورد مانیتور
- [ ] Widget Support

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Android App | Kotlin + Jetpack Compose |
| Brain Server | Python + FastAPI |
| AI Gateway | LiteLLM |
| Orchestration | n8n |
| Database | PostgreSQL + Redis |
| Vector DB | Qdrant (RAG) |
| STT Offline | Vosk |
| AI Models | Gemini / Groq / Ollama |
| Node Comm | WebSocket (WSS) |
| Containerization | Docker Compose |

---

## 🚀 Quick Start / شروع سریع

### ۱. راه‌اندازی Brain روی VPS

```bash
git clone https://github.com/Kian-irani/jarvis-android
cd jarvis-android/brain
cp .env.example .env
# کلیدهای API را در .env وارد کنید
docker compose up -d
```

### ۲. اضافه کردن هر دستگاه جدید

```bash
# Linux / VPS
  curl -s https://your-vps-ip/join.sh | bash

# Windows (PowerShell)
  iwr https://your-vps-ip/join.ps1 | iex

# Android (Termux)
  curl -s https://your-vps-ip/join-android.sh | bash
```

### ۳. نصب اپ اندروید

```
APK را از Releases دانلود کنید
به عنوان Home App انتخاب کنید
کلید Brain را وارد کنید
```

---

## 📁 Project Structure / ساختار پروژه

```
jarvis-android/
├── android/          ← اپ اندروید (Kotlin)
├── brain/            ← Brain Server (Python)
│   ├── api/          ← FastAPI routes
│   ├── router/       ← Task Router
│   ├── registry/     ← Node Registry
│   └── docker-compose.yml
├── node-agent/       ← اسکریپت Node
│   ├── install.sh    ← Linux installer
│   ├── install.ps1   ← Windows installer
│   └── agent.py      ← Node Agent
└── docs/             ← مستندات
```

---

## 🤝 Contributing / مشارکت

Pull request و Issue خوش‌آمد است.  
PRs and Issues are welcome.

---

## 📄 License

MIT License — آزاد برای استفاده شخصی و تجاری
