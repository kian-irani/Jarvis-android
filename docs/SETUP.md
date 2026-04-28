# 🚀 JARVIS — راهنمای نصب

---

## پیش‌نیازها

- VPS با Ubuntu 22.04 یا 24.04
- Python 3.10+
- دسترسی root
- اتصال اینترنت

---

## نصب Brain Server

### ۱. اتصال SSH به VPS مرکزی

```bash
ssh root@YOUR_VPS_IP
```

### ۲. نصب یک‌دستوره

اسکریپت نصب را دریافت و اجرا کنید:

```bash
# اسکریپت را دانلود و اجرا کنید
bash <(curl -s YOUR_INSTALL_URL)
```

### ۳. تأیید نصب

```bash
jarvis status
curl http://YOUR_VPS_IP:8000/health
```

---

## اضافه کردن Node جدید

روی هر Linux (VPS، سرور، یا Termux):

```bash
curl -s http://YOUR_BRAIN_IP:8000/install | bash
```

Node به صورت خودکار:
- سیستم‌عامل و منابع را تشخیص می‌دهد
- به Brain وصل می‌شود
- سرویس دائمی systemd نصب می‌کند

---

## JARVIS CLI (روی Brain)

```bash
jarvis status                    # وضعیت همه سرویس‌ها
jarvis logs brain [N]            # لاگ Brain
jarvis logs node [N]             # لاگ Node
jarvis restart                   # ری‌استارت همه
jarvis chat "پیام"               # چت با AI
jarvis run <node-name> "cmd"     # اجرا روی Node
jarvis nodes                     # لیست Node‌ها
```

---

## ساختار فایل‌ها

### Brain VPS
```
/opt/jarvis/
├── brain/main.py          ← Brain API
├── node-agent/agent.py    ← Node template
├── node-agent/install.sh  ← Auto-install
├── dashboard/index.html   ← Web UI
└── venv/                  ← Python env

/usr/local/bin/jarvis      ← CLI
```

### Node VPS
```
/opt/jarvis-node/
├── agent.py               ← Node Agent
└── venv/

/etc/jarvis-node-id        ← Node ID
```

---

## Android App

۱. APK را از [Releases](https://github.com/KIAN-IRANI/Jarvis-android/releases) دانلود کنید
۲. نصب کنید
۳. آدرس Brain را در تنظیمات وارد کنید

---

## عیب‌یابی

```bash
# سرویس کار نمی‌کند
journalctl -u jarvis-brain -n 50 --no-pager
journalctl -u jarvis-node -n 50 --no-pager

# Node وصل نمی‌شود
curl http://YOUR_BRAIN_IP:8000/health
curl http://YOUR_BRAIN_IP:8000/nodes

# ری‌استارت
jarvis restart
```

---

## نکات امنیتی

- آدرس‌های IP و کلیدهای API را در کد ثابت نکنید
- از متغیرهای محیطی استفاده کنید
- فایروال VPS را پیکربندی کنید
- HTTPS را فعال کنید (مرحله بعدی)
