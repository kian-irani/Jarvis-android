# 🏗️ JARVIS — معماری سیستم

---

## دیاگرام کلی

```
┌─────────────────────────────────────────────────────┐
│                   JARVIS Ecosystem                  │
│                                                     │
│  📱 Android App                                     │
│  ├── HomeActivity (Launcher)                        │
│  ├── App Drawer (HUD Style)                         │
│  └── Chat Interface                                 │
│          │                                          │
│          │ HTTP REST / WebSocket                    │
│          ▼                                          │
│  🖥️  Brain Server (FastAPI)                         │
│  ├── /health        — وضعیت سیستم                  │
│  ├── /nodes         — لیست Node‌ها                  │
│  ├── /nodes/{id}/exec — اجرای دستور                │
│  ├── /chat          — Groq AI                       │
│  ├── /dashboard     — Web UI                        │
│  ├── /agent.py      — دانلود Agent                 │
│  └── /install       — نصب خودکار                   │
│          │                                          │
│          │ WebSocket (دائمی)                        │
│     ┌────┴────┐                                     │
│     ▼         ▼                                     │
│  🖥️ Node 1   🖥️ Node 2  ...  🖥️ Node N             │
│  trvps1    vps-holland       [آینده]                │
└─────────────────────────────────────────────────────┘
```

---

## Brain Server

**فایل:** `/opt/jarvis/brain/main.py`

**پورت:** `8000`

**کتابخانه‌ها:** FastAPI, uvicorn, websockets, psutil, aiohttp

### Endpoints

| Endpoint | Method | عملکرد |
|---|---|---|
| `/` | GET | اطلاعات Brain |
| `/health` | GET | CPU/RAM/Disk + تعداد Node‌ها |
| `/nodes` | GET | لیست کامل Node‌ها + متریک |
| `/nodes/{id}/exec` | POST | اجرای دستور Shell روی Node |
| `/chat` | POST | ارسال پیام به Groq AI |
| `/dashboard` | GET | داشبورد وب |
| `/agent.py` | GET | فایل Node Agent |
| `/install` | GET | اسکریپت نصب خودکار |

### WebSocket — Node Protocol

```
Node → Brain:  {"node_id": "...", "name": "...", "hardware": {...}}
Brain → Node:  {"type": "registered", "node_id": "..."}

// Heartbeat (هر ۱۵ ثانیه)
Brain → Node:  {"type": "ping"}
Node → Brain:  {"type": "heartbeat", "metrics": {"cpu_percent": 5.2, ...}}

// اجرای دستور
Brain → Node:  {"type": "exec", "task_id": "uuid", "command": "ls -la"}
Node → Brain:  {"type": "task_result", "task_id": "uuid", "result": "..."}
```

---

## Node Agent

**فایل:** `/opt/jarvis-node/agent.py`

**کتابخانه‌ها:** websockets, psutil

### متریک‌های ارسالی

```json
{
  "cpu_percent": 12.5,
  "ram_free_gb": 0.56,
  "ram_used_percent": 44.2,
  "disk_free_gb": 24.1
}
```

### قابلیت‌های تشخیص خودکار

- `compute` — همیشه
- `storage` — همیشه
- `shell` — همیشه
- `docker` — اگه Docker نصب باشه
- `python3` — اگه Python3 نصب باشه
- `git` — اگه Git نصب باشه

---

## Android App

**مخزن:** `github.com/KIAN-IRANI/Jarvis-android`

**تکنولوژی:** Kotlin + Jetpack Compose

**Build:** GitHub Actions → APK خودکار

**اتصال:** HTTP REST به Brain API

---

## AI Engine

**مدل:** LLaMA 3.3 70B Versatile

**سرویس:** Groq (رایگان)

**ویژگی‌ها:**
- Key Rotation بین ۳ کلید API
- حافظه مکالمه (۲۰ پیام آخر)
- پاسخ به زبان کاربر

---

## سرویس‌های systemd

```
jarvis-brain.service   — Brain API (port 8000)
jarvis-node.service    — Node Agent (Turkey local)
```

همه لاگ‌ها در `journald`:
```bash
journalctl -u jarvis-brain -f
journalctl -u jarvis-node -f
```
