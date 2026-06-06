<div align="center">

# ⚡ Vision Agent OS
### سابقاً: Jarvis-android

**همه‌چیز در یک نگاه — همراهی که با تو زندگی می‌کند**  
*Everything in one glance — a companion that lives with you*

[![License: Source-Available (Proprietary)](https://img.shields.io/badge/License-Source--Available%20(Proprietary)-red.svg?style=for-the-badge)](LICENSE)
[![Status](https://img.shields.io/badge/status-active%20development-magenta?style=for-the-badge)](ROADMAP.md)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Activation](https://img.shields.io/badge/Activation-via%20kiancdn%20bot-0088cc?style=for-the-badge&logo=telegram)](docs/ACTIVATION.md)
[![Contribute](https://img.shields.io/badge/Contributors-Welcome%20(CLA)-cyan?style=for-the-badge)](CONTRIBUTING.md)

</div>

---

> ⚠️ **این پروژه اوپن‌سورس نیست.** کد برای شفافیت و توسعه‌ی مشارکتی *قابل‌مشاهده* (source-available) است، اما استفاده‌ی تجاری، بازانتشار و ساخت اثر مشتق **ممنوع** است. متن کامل: [LICENSE](LICENSE).
>
> **This is NOT open source.** The code is *source-available* for transparency and collaboration only. Commercial use, redistribution, and derivative works are prohibited — see [LICENSE](LICENSE).

---

## ویژن چیست؟ / What is Vision?

**Vision** یک **Agent OS** کامل برای اندروید است — نه فقط یک لانچر.  
گوشی شما را به یک **CyberDeck شخصی زنده** تبدیل می‌کند.

**Vision** turns your Android phone into a **living, breathing personal CyberDeck** —
a cyberpunk HUD launcher backed by a multi-provider AI brain, a real agent engine,
and a distributed mesh of your own devices.

```
Android Phone  →  [ Vision ]  →  Living CyberDeck
                    │
          ┌─────────┼─────────┐
        HUD AI    Agent    Mesh
      Cyberpunk  Engine  Network
```

> **در یک جمله:** Vision = Iron Man HUD + Claude Code + Server Manager + Automation — همه روی گوشی

---

## 🔑 فعال‌سازی / Activation

Vision یک محصول **فعال‌سازی‌محور** است. برای استفاده از اپ، کاربر یک **توکن فعال‌سازی** از ربات رسمی دریافت می‌کند:

```
کاربر  →  ربات تلگرام «kiancdn»  →  دریافت توکن  →  وارد کردن در اپ  →  فعال‌سازی
User   →  «kiancdn» Telegram bot →  receive token →  enter in app    →  activated
```

- 🤖 ربات فعال‌سازی: **[@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)** (kiancdn)
- 📄 جریان کامل، امنیت توکن و معماری: **[docs/ACTIVATION.md](docs/ACTIVATION.md)**

> توکن‌ها شخصی، قابل‌ابطال و دارای محدودیت نرخ هستند. اشتراک‌گذاری یا دور زدن فعال‌سازی نقض لایسنس است.

---

## ✨ ویژگی‌های کلیدی / Core Features

| حوزه | ویژگی‌ها |
|------|-----------|
| 🎨 **HUD هولوگرافیک** | Arc Reactor، Glassmorphism (Haze 2.0)، Glitch Engine، AGSL Shaders، Audio-Reactive، تم‌های Night City / Iron Vision / Neon Hacker |
| 🤖 **هوش مصنوعی** | Multi-Provider (OpenAI، Anthropic، Gemini، DeepSeek، Groq، OpenRouter، Ollama، MLC) + Token Pool با Failover و Budget Control |
| 🎙️ **Voice Engine** | Wake Word «هی ویژن»، Vosk STT فارسی/انگلیسی، Piper TTS، Voice HUD |
| ⚙️ **Agent Engine** | ReAct، Multi-Agent (Planner/Coder/Researcher/Writer/Device)، Tool Calling sandboxed |
| 📦 **Management** | File · App · Server (SSH/Docker/VPS) · Message · Coding Workspace |
| 🌐 **Mesh Network** | کنترل دستگاه‌های اطراف به‌عنوان Nodes/Drones |
| 🔒 **Security** | Permission Sandbox، Action Approval، Secret Vault (Keystore + بیومتریک)، Audit Log |

جزئیات کامل: [ROADMAP.md](ROADMAP.md) · [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 🏗️ معماری / Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Vision Agent OS                        │
├──────────────┬──────────────┬─────────────┬────────────────┤
│  HUD Layer   │  AI Core     │ Agent Engine│  Mesh Network  │
│  Cyberpunk   │  Multi-API   │  Multi-Agent│  Distributed   │
├──────────────┴──────────────┴─────────────┴────────────────┤
│   Activation & Licensing  (kiancdn token service)          │
├─────────────────────────────────────────────────────────────┤
│   Management Skills · Security & Privacy Layer              │
└─────────────────────────────────────────────────────────────┘
```

**Tech Stack**
```
Android   : Kotlin + Jetpack Compose + Haze 2.0 + AGSL
Backend   : Python FastAPI + LiteLLM + WebSocket
Activation: kiancdn Telegram bot → token service (HMAC-signed)
Voice     : Vosk (STT) + Piper (TTS)
Security  : Android Keystore + EncryptedDataStore
```

---

## 🚀 شروع / Getting Started

این مخزن **سورس‌کد** است، نه محل دانلود APK عمومی. برای استفاده‌ی واقعی:

1. به ربات **[@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)** بروید و توکن فعال‌سازی بگیرید.
2. بیلد رسمی Vision را از طریق همان ربات/کانال دریافت کنید.
3. اپ را باز کنید → توکن را وارد کنید → فعال‌سازی.

> **توسعه‌دهندگان:** برای build از سورس و راه‌اندازی Brain Server، [CONTRIBUTING.md](CONTRIBUTING.md) و [docs/SETUP.md](docs/SETUP.md) را ببینید. ساخت از سورس فقط برای مشارکت تحت [CLA](CLA.md) مجاز است.

---

## 🤝 دعوت به همکاری / Call for Contributors

ما به‌دنبال توسعه‌دهندگان جدی هستیم. مشارکت تحت **[CLA](CLA.md)** انجام می‌شود (کد source-available می‌ماند، نه اوپن‌سورس).

| حوزه | مهارت | اولویت |
|------|-------|--------|
| 🎨 HUD/UI | Kotlin + Compose + AGSL | 🔴 فوری |
| 🤖 AI Integration | Python + LiteLLM | 🔴 فوری |
| 🔑 Activation/Backend | FastAPI + Telegram Bot | 🔴 فوری |
| 🔒 Security | Android Security + Crypto | 🟠 زیاد |
| 🎙️ Voice | Vosk + Piper | 🟠 زیاد |

👉 راهنمای کامل: **[CONTRIBUTING.md](CONTRIBUTING.md)** · تماس: [@Kian_irani_t](https://t.me/Kian_irani_t)

---

## 📚 مستندات / Documentation

| فایل | محتوا |
|------|--------|
| [ROADMAP.md](ROADMAP.md) | نقشه راه فازی |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | معماری فنی |
| [docs/ACTIVATION.md](docs/ACTIVATION.md) | جریان فعال‌سازی و سرویس توکن kiancdn |
| [docs/SETUP.md](docs/SETUP.md) | راه‌اندازی توسعه |
| [CONTRIBUTING.md](CONTRIBUTING.md) · [CLA.md](CLA.md) | همکاری |
| [SECURITY.md](SECURITY.md) | گزارش آسیب‌پذیری |
| [LICENSE](LICENSE) | لایسنس انحصاری source-available |

---

## 📄 لایسنس / License

**Vision Agent OS Source-Available License (VAOS-SAL) v1.0** — © 2026 Kian Irani.  
کد قابل‌مشاهده است؛ استفاده‌ی تجاری/بازانتشار/اثر مشتق ممنوع. متن کامل: [LICENSE](LICENSE).

---

<div align="center">

**Vision** — نه فقط یک اپلیکیشن. **آینده‌ی تعامل انسان و هوش مصنوعی روی موبایل.**

*Made with ❤️ and Neon — by Kian Irani & contributors*

</div>
