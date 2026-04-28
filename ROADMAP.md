# 🗺️ JARVIS — نقشه راه کامل

> آخرین بروزرسانی: April 2026 | نسخه: 4.0.0

---

## 📊 وضعیت کلی

| فاز | عنوان | وضعیت | پیشرفت |
|---|---|---|---|
| 0 | Brain Server | ✅ تکمیل | 100% |
| 1 | Node Agent + Mesh | ✅ تکمیل | 100% |
| 2 | Android Launcher | 🔄 در حال توسعه | 40% |
| 3 | فشرده‌سازی + بهینه‌سازی | 🔲 بعدی | 0% |
| 4 | Voice Engine | 🔲 برنامه‌ریزی | 0% |
| 5 | Skills | 🔲 برنامه‌ریزی | 0% |
| 6 | UI پیشرفته HUD | 🔲 برنامه‌ریزی | 0% |
| 7 | VPS ذخیره‌سازی | 🔲 آینده | 0% |

---

## ✅ فاز ۰ — Brain Server (تکمیل)

> VPS مرکزی — Ubuntu 24.04 — FastAPI

- [x] FastAPI با WebSocket Node Registry
- [x] Task Router با async Future (نتیجه برمی‌گردونه)
- [x] Groq AI (LLaMA 3.3 70B) با Key Rotation
- [x] سرویس دائمی systemd
- [x] Web Dashboard
- [x] CORS برای Android
- [x] `/agent.py` و `/install` endpoint
- [ ] HTTPS / SSL
- [ ] LiteLLM Gateway
- [ ] PostgreSQL + Redis

---

## ✅ فاز ۱ — Node Agent + Mesh (تکمیل)

- [x] Auto-detection سیستم‌عامل و منابع (CPU/RAM/Disk)
- [x] WebSocket دائم با auto-reconnect
- [x] Heartbeat هر ۱۵ ثانیه + متریک لحظه‌ای
- [x] اجرای Shell command از Brain
- [x] systemd service با journal logging
- [x] Node ترکیه: `trvps1` — آنلاین ✅
- [x] Node هلند: `vps-holland` — آنلاین ✅
- [x] `install.sh` خودکار (curl | bash)
- [ ] Windows (PowerShell)
- [ ] Termux / Android

---

## 🔄 فاز ۲ — Android Launcher (40%)

**انجام شده:**
- [x] HomeActivity با `category.HOME`
- [x] App Drawer با آیکون‌های اپ
- [x] Chat با JARVIS
- [x] اتصال به Brain API
- [x] تم HUD آبی/سبز
- [x] GitHub Actions → APK خودکار

**باقیمانده:**
- [ ] UI ارتقا: `chrisbanes/haze` + `StarkDroid/ShadowGlow`
- [ ] `lokile/animated-border` — دایره هدف‌گیری
- [ ] `compose-audiowaveform` — موج صدا
- [ ] Quick Settings (WiFi/BT/Flashlight)
- [ ] Mesh Panel (وضعیت Node‌ها از گوشی)
- [ ] Remote Terminal

---

## 🔲 فاز ۳ — فشرده‌سازی Mesh

**هدف:** کاهش ۷۵٪ ترافیک بین Node‌ها

```
JSON (100KB) → MsgPack (60KB) → Zstd (25KB)
نتیجه: ۷۵٪ کاهش + ۳x سرعت
```

- [ ] MsgPack در Node Agent‌ها
- [ ] Zstd middleware در Brain
- [ ] Load balancing (انتخاب Node براساس CPU/RAM)
- [ ] Task queue
- [ ] Retry خودکار

---

## 🔲 فاز ۴ — Voice Engine

- [ ] Wake Word — "Hey JARVIS"
- [ ] Vosk STT آفلاین (پشتیبانی فارسی)
- [ ] Google STT (fallback آنلاین)
- [ ] TTS با صدای JARVIS
- [ ] Intent Detection (Rule-based + LLM)
- [ ] مکالمه چندمرحله‌ای
- [ ] حافظه مکالمه (RAG)

---

## 🔲 فاز ۵ — Skills

**Local (بدون Brain):**
- [ ] WiFi / Bluetooth / Flashlight
- [ ] تماس و SMS
- [ ] آلارم و تایمر
- [ ] باز کردن اپ‌ها

**Cloud (از Brain):**
- [ ] مکالمه AI
- [ ] آب‌وهوا
- [ ] ترجمه
- [ ] یادداشت و یادآوری
- [ ] اجرای دستور روی Node‌ها با صدا

---

## 🔲 فاز ۶ — UI پیشرفته HUD

| کتابخانه | کاربرد | لایسنس |
|---|---|---|
| `chrisbanes/haze` | Glass blur پنل‌ها | Apache-2.0 |
| `StarkDroid/ShadowGlow` | Neon glow + تپش | Apache-2.0 |
| `lokile/animated-border` | دایره هدف‌گیری | Apache-2.0 |
| `TheChance101/AAY-chart` | Radar + تله‌متری | Apache-2.0 |
| `patrykandpatrick/vico` | چارت CPU/RAM | Apache-2.0 |
| `compose-audiowaveform` | موج صدای میک | Apache-2.0 |
| `zeeshanali-k/Typist` | متن تایپ‌شونده | MIT |
| `airbnb/lottie-compose` | AI orb animation | Apache-2.0 |

**فونت:** Orbitron + Share Tech Mono + Rajdhani

---

## 🔲 فاز ۷ — VPS ذخیره‌سازی

```
VPS مرکزی  = Brain (پردازش + کنترل)
VPS هلند   = Node (محاسبه)
VPS جدید   = Storage (Qdrant + MinIO + Backup)
```

---

## 💰 هزینه ماهانه

| منبع | هزینه |
|---|---|
| VPS‌ها | موجود |
| Groq API | رایگان |
| GitHub Actions | رایگان |
| **جمع** | **~$0** |
