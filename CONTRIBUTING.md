# 🤝 مشارکت در Vision Agent OS / Contributing

از علاقه‌ات به Vision ممنونیم! ما فعالانه به‌دنبال **توسعه‌دهندگان جدی** هستیم.

> ⚠️ **مهم:** Vision **اوپن‌سورس نیست**. کد source-available است و مشارکت تحت **[CLA](CLA.md)** انجام می‌شود. با ارسال هر Pull Request، شرایط CLA را می‌پذیرید: حق استفاده/توزیع/relicense مشارکت شما به دارنده‌ی حق‌نشر واگذار می‌شود و کد ممکن است در بیلدهای انحصاری استفاده شود.

---

## 🎯 حوزه‌های مورد نیاز / Where we need help

> **مهم‌ترین نیازِ فعلی:** بازسازیِ لانچر (LR/NEO) سریع شیپ می‌شود اما سرورِ بیلد **امولاتور ندارد**؛ پس خیلی از UI با برچسبِ *«needs on-device confirmation»* است. **QAِ روی دستگاهِ واقعی و پولیشِ Compose اولویتِ شماره‌ی ۱ است.** نقطه‌ی شروع: [`plu/PLAN.md`](plu/PLAN.md) — نزدیک‌ترین تسکِ باز را بردار، build سبز کن، PR بزن.

| حوزه | مهارت | اولویت |
|------|-------|--------|
| 📱 Launcher UI + QAِ دستگاه | Kotlin + Jetpack Compose (تستِ بیلدِ واقعی روی دستگاه) | 🔴 فوری |
| 🧠 Local AI engine | Kotlin/NDK + llama.cpp / MediaPipe LLM (inference روی دستگاه) | 🔴 فوری |
| 🤖 Agentic core | Kotlin (اتوماسیونِ Accessibility، tool-calling، scheduler) | 🔴 فوری |
| 🧠 Brain Core | Python + FastAPI + LangGraph | 🟠 زیاد |
| 🎙️ Voice/Language | Android TTS/STT + تأییدِ Edge/Azure neural، Vosk/Whisper | 🟠 زیاد |
| 🔑 Activation/Backend | FastAPI + Telegram Bot (kiancdn) | 🟠 زیاد |
| 🔒 Security | Android Keystore + Crypto | 🟠 زیاد |
| ⚡ VISN / Mesh | Kotlin + Python (LZ4/zstd، mesh model exchange) | 🟡 متوسط |

---

## 🔧 جریان مشارکت / Workflow

1. **اول هماهنگ کن:** قبل از کار، یک Issue باز کن یا با [@Kian_irani_t](https://t.me/Kian_irani_t) هماهنگ کن (برای جلوگیری از کار موازی).
2. **[CLA](CLA.md) را بپذیر:** در اولین PR، تأیید کن که CLA را خوانده و می‌پذیری.
3. Fork → branch (`feat/...` یا `fix/...`) → کد → PR به `main`.
4. کد را تمیز، تست‌شده و مطابق سبک پروژه نگه دار.
5. **اسرار را هرگز commit نکن** (توکن، کلید، `.env`). از `.env.example` استفاده کن.

> Fork فقط برای ارسال PR مجاز است؛ هرگونه بازانتشار دیگر طبق [LICENSE](LICENSE) ممنوع است.

---

## 🐛 گزارش باگ / Bug reports

از [قالب Issue](.github/ISSUE_TEMPLATE/bug_report.yml) استفاده کن: انتظار vs واقعیت، مراحل بازتولید، نسخه، لاگ (بدون اسرار).

## 🔒 آسیب‌پذیری امنیتی

**Issue عمومی باز نکن.** طبق [SECURITY.md](SECURITY.md) به‌صورت خصوصی گزارش بده.

---

## 📞 تماس / Contact
- 💬 Telegram: [@Kian_irani_t](https://t.me/Kian_irani_t)
- 🤖 کانال/ربات: [@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)
- 📧 Email: kian.irani.gh@gmail.com

> با هم بسازیم — اما با قواعد روشن. 🌀
