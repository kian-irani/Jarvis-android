# 🗺️ JARVIS — Roadmap / نقشه راه

> 📊 [مشاهده در GitHub Projects](https://github.com/Kian-irani/jarvis-android/projects)  
> 📊 [View on GitHub Projects](https://github.com/Kian-irani/jarvis-android/projects)

---

## وضعیت کلی / Overall Status

| فاز / Phase | عنوان | وضعیت |
|---|---|---|
| 0 | Brain Server | 🔲 برنامه‌ریزی شده |
| 1 | Node Agent | 🔲 برنامه‌ریزی شده |
| 2 | Android Launcher | 🔲 برنامه‌ریزی شده |
| 3 | Voice Engine | 🔲 برنامه‌ریزی شده |
| 4 | Skills | 🔲 برنامه‌ریزی شده |
| 5 | UI / HUD | 🔲 برنامه‌ریزی شده |

> 🔲 برنامه‌ریزی شده | 🔄 در حال توسعه | ✅ تکمیل شده

---

## فاز ۰ — Brain Server (زیرساخت)
> *Phase 0 — Brain Server (Infrastructure)*

مغز اصلی سیستم روی VPS — همه Node‌ها به اینجا وصل می‌شن.  
*Main brain on VPS — all nodes connect here.*

- [ ] راه‌اندازی Docker Compose روی VPS
- [ ] FastAPI — API اصلی سیستم
- [ ] Node Registry — ثبت و مدیریت دستگاه‌ها (WebSocket)
- [ ] Task Router — مسیریابی هوشمند task‌ها به Node‌ها
- [ ] LiteLLM Gateway — یک endpoint برای همه مدل‌های AI
- [ ] پشتیبانی Gemini API (رایگان)
- [ ] پشتیبانی Groq API (رایگان، سریع)
- [ ] پشتیبانی Ollama (محلی، خصوصی)
- [ ] n8n Workflow Automation
- [ ] PostgreSQL — حافظه بلندمدت
- [ ] Redis — Cache و صف task
- [ ] Qdrant — Vector DB برای RAG
- [ ] HTTPS + احراز هویت Node‌ها

---

## فاز ۱ — Node Agent (اتصال دستگاه‌ها)
> *Phase 1 — Node Agent (Device Connector)*

یک اسکریپت — هر دستگاهی را به شبکه اضافه می‌کند.  
*One script — adds any device to the network.*

- [ ] اسکریپت Linux/VPS (`install.sh`)
- [ ] اسکریپت Windows (`install.ps1`)
- [ ] اسکریپت Android/Termux (`install-android.sh`)
- [ ] شناسایی خودکار سیستم‌عامل
- [ ] شناسایی خودکار منابع (CPU/GPU/RAM/Disk)
- [ ] شناسایی خودکار قابلیت‌ها (Ollama/Docker/GPU)
- [ ] تعیین نوع Node (compute / storage / ai / edge)
- [ ] اتصال دائم WebSocket به Brain
- [ ] Heartbeat هر ۱۰ ثانیه
- [ ] نصب به عنوان سرویس سیستم (auto-start)
- [ ] اجرای دستور Shell از Brain
- [ ] دسترسی فایل (read/write/list)
- [ ] اجرای Ollama inference محلی
- [ ] ارسال متریک لحظه‌ای

---

## فاز ۲ — Android Launcher (اپ اصلی)
> *Phase 2 — Android Launcher (Main App)*

جایگزین هوم‌اسکرین اندروید — مرکز کنترل JARVIS.  
*Replaces Android home screen — JARVIS control center.*

- [ ] `HomeActivity` با `category.HOME`
- [ ] App Drawer با جستجو
- [ ] Quick Settings Panel (WiFi/BT/Flashlight/Volume)
- [ ] اتصال به Brain Server
- [ ] **Mesh Panel** — وضعیت همه Node‌ها
- [ ] **File Browser** — فایل‌های همه دستگاه‌ها
- [ ] **Remote Terminal** — ترمینال روی هر Node از گوشی
- [ ] **System Monitor** — CPU/RAM/Disk همه Node‌ها
- [ ] AccessibilityService برای کنترل صفحه
- [ ] NotificationListener
- [ ] Offline Mode (بدون Brain هم کار می‌کنه)
- [ ] Push Notification از Brain

---

## فاز ۳ — Voice Engine (موتور صوتی)
> *Phase 3 — Voice Engine*

همه چیز با صدا کنترل می‌شود.  
*Everything controlled by voice.*

- [ ] Wake Word Detection — "Hey JARVIS"
- [ ] Vosk STT آفلاین (پشتیبانی فارسی)
- [ ] Google STT آنلاین (fallback)
- [ ] Text-to-Speech با صدای JARVIS
- [ ] Intent Detection (Rule-based برای دستورات ساده)
- [ ] Intent Detection (LLM برای دستورات پیچیده)
- [ ] مکالمه چندمرحله‌ای
- [ ] حافظه مکالمه (RAG)

---

## فاز ۴ — Skills (مهارت‌ها)
> *Phase 4 — Skills*

هر Skill یک قابلیت مستقل.  
*Each skill is an independent capability.*

**Local Skills (بدون Brain):**
- [ ] WiFi on/off
- [ ] Bluetooth on/off
- [ ] Flashlight
- [ ] Volume control
- [ ] تماس تلفنی
- [ ] ارسال SMS
- [ ] آلارم و تایمر
- [ ] باز کردن اپ‌ها

**Cloud Skills (از طریق Brain):**
- [ ] مکالمه با AI
- [ ] آب‌وهوا
- [ ] اخبار
- [ ] ترجمه
- [ ] یادداشت و یادآوری
- [ ] قیمت ارز (مناسب کانال فارکس 😉)
- [ ] جستجوی وب
- [ ] کنترل Node‌های دیگه با صدا

---

## فاز ۵ — UI / HUD Theme (رابط کاربری)
> *Phase 5 — UI / HUD Theme*

طراحی بصری به سبک Iron Man.  
*Iron Man-style visual design.*

- [ ] تم HUD با رنگ‌های آبی/نارنجی
- [ ] انیمیشن‌های Jetpack Compose
- [ ] داشبورد مانیتور همه Node‌ها
- [ ] Widget Support
- [ ] صفحه قفل سفارشی
- [ ] حالت Always-On Display

---

## 🔮 آینده / Future Ideas

- پشتیبانی Raspberry Pi به عنوان Node خانگی
- اتصال به دستگاه‌های IoT (لامپ هوشمند و...)
- مدل AI اختصاصی fine-tune شده برای JARVIS
- اپ iOS (همراه)
- پنل وب برای مدیریت Mesh

---

## 📌 چطور مشارکت کنیم / How to Contribute

1. یک Issue باز کنید
2. Fork → Branch → Pull Request
3. یا پیشنهاد Feature بدید

---

*آخرین بروزرسانی / Last updated: 2026*
