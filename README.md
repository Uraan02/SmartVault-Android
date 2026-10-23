# SmartVault — Smart Card Vault & Personal Finance Android App

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" height="120" alt="SmartVault Logo"/>
</p>

**SmartVault** is a complete, premium, offline-first native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, **Google ML Kit Text Recognition**, and **AndroidX Biometric**.

It securely stores your personal cards, bank cards, family profiles, business visiting cards, confidential notes, checklists, and optional card-wise payment balance ledgers with bank-grade encryption on your local device.

---

## 🚀 Key Features

### 1. 🔐 Multi-Layer Vault Security & App Lock
- **Master PIN & Password Lock**: Hashed using **PBKDF2 with HMAC-SHA256** and cryptographic salt. No plain-text credentials stored.
- **Biometric & Fingerprint Unlock**: Instant authentication using `AndroidX BiometricPrompt`.
- **Configurable Auto-Lock**: Auto-locks on background or after configurable timeout (Immediate, 30s, 1m, 5m).
- **Screenshot Protection (`FLAG_SECURE`)**: Prevents screenshots, screen recording, and hides vault preview in Android Recent Apps.
- **Emergency Account Recovery**: Secure recovery question & answer verification.
- **1-Tap Privacy Mode**: Masks all card numbers, names, and balances instantly with one tap.

### 2. 💳 Smart Card Management System
- **Categories**:
  - 👤 My Personal Cards
  - 🏦 Bank & Payment Cards (Debit / Credit)
  - 👨‍👩‍👧 Family Members' Cards
  - 💼 Visiting & Business Cards
  - 🪪 Identity & Membership Cards (CNIC, Driving License, Passport)
  - ⚙️ Custom Categories
- **Card Attributes**: Nickname, masked number (`•••• •••• •••• 1234`) with reveal & copy buttons, issuing bank/entity, issue & expiry dates, phone, website, address, notes, custom dynamic fields, and front/back photos.
- **Zero CVV/PIN Storage Policy**: Never stores card CVVs or ATM PINs for maximum security.
- **Visual Styles**: Customizable card color gradients matching physical cards.

### 3. 📷 Smart Camera Card Scanner (On-Device OCR)
- Real-time CameraX scanning viewfinder with card aspect ratio (~1.58) guide overlay.
- Automatic text recognition using **Google ML Kit** running 100% locally and offline.
- Intelligent parser extracts: Card numbers, expiry dates (`MM/YY`), cardholder names, banks, phone numbers, emails, companies, and designations.
- **Review Screen**: Allows user verification and manual correction before saving.
- Gallery image import support for existing card photos.

### 4. 💰 Optional Card-Wise Balance & Payment Tracking
- **OFF by default** and independently toggleable per card.
- **Formula**:
  $$\text{Current Balance} = \text{Opening Balance} + \text{Credits} + \text{Refunds} - \text{Expenses}$$
- Supports PKR (Pakistani Rupee) by default alongside USD, EUR, GBP, AED, SAR, and other currencies.
- Prevents accidental negative balances with warnings.
- *Strictly manual recordkeeping: Never connects to or debits real bank accounts.*

### 5. 📊 Dedicated Expense Manager
- Log expenses and income with category tags (Food, Transport, Fuel, Shopping, Bills, Business, Medical, Education, Custom).
- Automatically deducts from linked card's balance if tracking is enabled.
- Category spending breakdown with visual progress meters and percentage analytics.
- **CSV Export**: 1-click export of transactions to CSV.

### 6. 📝 Notes, Reminders & Checklists
- Quick notes, card-linked notes, contact-linked notes, and checklists with strike-through items.
- Reminder scheduling for card renewal dates and bill payments.
- Pinned and searchable notes.

### 7. 📇 Visiting Card Contact Directory
- Extracts contact details from business cards.
- 1-tap phone call, email compose, website browsing, and map navigation.
- Meeting discussion logs and follow-up tracking.
- **vCard (.vcf) Export**: Share or import into standard phone contacts.

### 8. 🚨 Card Expiry Radar
- Real-time monitor classifying cards into:
  - 🔴 **Expired**
  - 🟠 **Expiring within 30 Days**
  - 🟡 **Expiring within 60 Days**
  - 🔵 **Expiring within 90 Days**

### 9. 🛡️ Encrypted Backup & Restore (.svault)
- Complete database export encrypted with **AES-256-GCM** using user's password.
- Password-verified restoration for easy phone upgrades.

---

## 📲 APK Installation Guide

The signed release APK has been compiled and saved to your **Downloads** folder:
`C:\Users\MC\Downloads\SmartVault.apk`

### To install on your Android device:
1. Transfer `SmartVault.apk` to your phone via USB cable, Google Drive, WhatsApp, or local share.
2. On your Android phone, tap the `SmartVault.apk` file in your **Files** or **Downloads** app.
3. If prompted, enable **"Allow from this source"** for installation of APKs outside Google Play.
4. Tap **Install** and open **SmartVault**.
5. Set your master 4-digit PIN on first launch to initialize your encrypted vault.

---

## 🛠️ Tech Stack
- **Language**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: AndroidX Room 2.6.1 with KSP
- **Camera & Vision**: CameraX 1.3.4 + Google ML Kit Text Recognition
- **Security**: AndroidX Biometric 1.1.0 + Java Cryptography Extension (AES-256-GCM, PBKDF2)
- **Image Loading**: Coil Compose 2.6.0
- **CI/CD**: GitHub Actions automated APK build workflow (`.github/workflows/build-apk.yml`)
