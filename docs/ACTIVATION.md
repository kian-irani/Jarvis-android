# 🔑 ACTIVATION — Vision Agent OS
### فعال‌سازی توکن‌محور از طریق ربات kiancdn

> **وضعیت:** طراحی (Paper Design) — فاز A نقشه‌راه. این سند جریان، قرارداد API و امنیت را تعریف می‌کند؛ پیاده‌سازی در فاز اجرایی.

---

## چرا فعال‌سازی؟ / Why activation?

Vision **اوپن‌سورس نیست** و برای جلوگیری از سوءاستفاده و توزیع غیرمجاز، اجرای اپ نیازمند **توکن فعال‌سازی معتبر** است که فقط از طریق ربات رسمی صادر می‌شود.

```
کاربر → ربات تلگرام «kiancdn» → توکن → اپ → فعال‌سازی
```

- ربات رسمی: **[@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)** (kiancdn)

---

## جریان کامل / End-to-end flow

```
┌─────────────┐   /start, /token   ┌──────────────────────┐
│ Telegram    │ ─────────────────► │  kiancdn Bot         │
│ User        │ ◄───────────────── │  (Activation Service)│
└─────────────┘   توکن امضاشده     └──────────┬───────────┘
                                              │ issue() + store
                                              ▼
                                   ┌──────────────────────┐
                                   │ Token DB (Postgres)  │
                                   └──────────┬───────────┘
┌─────────────┐   verify(token)              │
│ Android App │ ───────────────────────────► │ /activation/verify
│             │ ◄─────────────────────────── │ valid / invalid
└─────────────┘   فعال + ذخیره در Keystore
```

### مراحل
1. کاربر در تلگرام به ربات `kiancdn` پیام `/start` می‌دهد → احراز هویت با `user_id` تلگرام.
2. کاربر `/token` می‌زند → سرویس یک توکن **امضاشده (HMAC)** صادر و ذخیره می‌کند.
3. ربات توکن را به کاربر می‌دهد (یک‌بار نمایش).
4. کاربر توکن را در صفحه‌ی فعال‌سازی اپ وارد می‌کند.
5. اپ توکن را به `POST /activation/verify` می‌فرستد.
6. در صورت اعتبار → فعال‌سازی + ذخیره‌ی امن در **Android Keystore**.
7. اپ به‌صورت دوره‌ای re-verify می‌کند (با **grace period** برای حالت آفلاین).

---

## قرارداد API / API contract (پیشنهادی)

### `POST /activation/issue`  (داخلی — فقط ربات، با کلید سرویس)
```json
// req
{ "telegram_user_id": 123456789, "plan": "free" }
// res
{ "token": "VAOS.<base64payload>.<hmacsig>", "expires_at": "2026-12-31T00:00:00Z" }
```

### `POST /activation/verify`  (از اپ)
```json
// req
{ "token": "VAOS.<...>.<...>", "device_id": "<hashed>" }
// res 200
{ "status": "active", "expires_at": "...", "features": ["multi_api","voice"] }
// res 403
{ "status": "revoked" | "expired" | "invalid" | "device_mismatch" }
```

---

## ساختار توکن / Token structure
```
VAOS.<payload>.<signature>
payload (base64) = { uid, jti, plan, iat, exp }
signature        = HMAC-SHA256(payload, SERVER_SECRET)
```
- **uid**: شناسه‌ی کاربر تلگرام · **jti**: شناسه‌ی یکتا (برای ابطال) · **exp**: انقضا
- توکن به یک `device_id` هش‌شده گره می‌خورد تا اشتراک‌گذاری سخت شود.

---

## امنیت / Security
- 🔏 امضای HMAC با `SERVER_SECRET` (در env سرور، هرگز در اپ).
- ⏱️ انقضا + چرخش + لیست ابطال (`jti` در DB).
- 🚦 Rate-limit صدور (per telegram user) و verify (per device/IP).
- 🕵️ ضدتقلب: تشخیص یک توکن روی چند device → فلگ/ابطال.
- 📵 grace period آفلاین (مثلاً ۷۲h) تا بدون نت هم کار کند.
- 🔐 ذخیره‌ی توکن در Android Keystore + EncryptedDataStore.

---

## نقشه‌ی پیاده‌سازی / Build plan (فاز A)
- [ ] `activation-service` (FastAPI) کنار brain-server
- [ ] ربات `kiancdn`: `/start`، `/token`، نگاشت user↔token
- [ ] DB توکن‌ها (Postgres) + لیست ابطال
- [ ] اپ: صفحه‌ی فعال‌سازی + Keystore + re-verify دوره‌ای
- [ ] داشبورد مدیریت توکن (صادر/فعال/ابطال) در dashboard.html
- [ ] تست‌های ضدتقلب و rate-limit

> یادداشت: مقادیر دقیق (طول grace period، plan‌ها، فیلدهای feature) در فاز اجرایی نهایی می‌شوند.
