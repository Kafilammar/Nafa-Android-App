<div align="center">

# Nafa | نفع

### Share more. Buy less. Build a stronger neighborhood.

**An Arabic-first Android application for sharing, lending, and borrowing tools within local communities.**

[Features](#features) · [Tech Stack](#tech-stack) · [Getting Started](#getting-started) · [Project Structure](#project-structure)

</div>

---

## Overview

**Nafa (نفع)** is a community-focused Android app designed to help neighbors lend and borrow tools instead of buying items they only need occasionally. Users can list their tools, explore tools available in their neighborhood, send borrowing requests, communicate with owners, and share feedback after a borrowing experience.

The application is built primarily in **Java** with the **Android SDK**, **Firebase Authentication**, and **Cloud Firestore**. It features an **Arabic user interface** with right-to-left (RTL) support.

## Features

| Feature | Description |
| --- | --- |
| 🔐 Accounts | Sign up, log in, email verification, and password recovery with Firebase Authentication. |
| 🧰 Tool listings | Add, edit, view, and manage tools, descriptions, availability, and images. |
| 🔎 Discovery | Browse available tools, search listings, and sort/filter results. |
| 🤝 Borrowing workflow | Request a tool for a selected duration; owners can accept or reject incoming requests, and borrowers can track their requests. |
| 💬 In-app chat | Conversations and messages between users using Cloud Firestore. |
| ❤️ Favorites | Save tools to a personal favorites list. |
| 🔔 Notifications | View notifications and updates related to activity in the app. |
| ⭐ Ratings and reviews | Share feedback after borrowing interactions. |
| 👤 User profiles | View and update profile details and manage account settings. |
| 🚩 Reports and support | Report issues, submit support requests, and review their status. |
| 🛡️ Administration | In-app administration screens for managing users, tools, requests, reports, reviews, and support. |

> **Note:** These features describe functionality present in the source code. A running backend, suitable Firestore rules, and Firebase configuration are required for full operation.

## Tech Stack

- **Language:** Java (Java 11 source/target compatibility)
- **Platform:** Android (minimum SDK 24; compile/target SDK 37 in the project configuration)
- **UI:** Native Android views, AndroidX AppCompat, Material Components
- **Authentication:** Firebase Authentication
- **Database and messaging data:** Cloud Firestore
- **Build system:** Gradle with Kotlin DSL (`.gradle.kts`) and version catalog
- **Gradle wrapper:** 9.6.0 (as configured in this repository)

## Getting Started

### Requirements

- Android Studio with compatible Android SDK/build tools and JDK
- An Android device or emulator running Android 7.0 (API 24) or later
- A Firebase project with Android app registration
- An internet connection for Firebase and Gradle dependencies

### 1. Clone the repository

```bash
git clone https://github.com/Kafilammar/Nafa-Android-App.git
cd Nafa-Android-App
```

Alternatively, download the repository as a ZIP and extract it.

### 2. Connect your Firebase project

The repository intentionally **does not include** `app/google-services.json`.

1. Open the [Firebase Console](https://console.firebase.google.com/) and create or select a Firebase project.
2. Register an Android app with the package name **`com.example.nafa`** (matching `applicationId` in `app/build.gradle.kts`).
3. Download your Android app configuration file **`google-services.json`**.
4. Save it locally at **`app/google-services.json`**.
5. Enable **Authentication → Sign-in method → Email/Password**.
6. Create a **Cloud Firestore** database and configure its security rules and any indexes your queries require.

**Important:** The Firebase file is omitted from the GitHub-ready package to avoid sharing project-specific configuration. It must be provided locally before the Android build can be completed. A Firebase client configuration file is not, by itself, an authorization mechanism; protect data through Firestore security rules and Firebase Authentication.

### 3. Open and run

1. Open the repository's root directory in **Android Studio**.
2. Allow Gradle to sync. Install any SDK packages requested by Android Studio.
3. Select an emulator or connect an Android device.
4. Click **Run** to build and launch the `app` module.

If Gradle reports a missing Google Services configuration, confirm that `app/google-services.json` exists and belongs to the Android package `com.example.nafa`.

### Optional command-line build

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew assembleDebug
```

The commands require a configured Android SDK and the local Firebase configuration described above.

## Project Structure

```text
Nafa-Android-App/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/nafa/   # Activities, app logic, Firestore interactions
│   │   │   ├── res/                     # UI resources and app icons
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/
│   │   └── test/
│   ├── build.gradle.kts
│   └── google-services.json             # Local only; intentionally excluded
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

## Firestore Data Collections

The application refers to these Firestore collections in its source code:

`users`, `public_profiles`, `neighborhoods`, `tools`, `tool_images`, `borrow_requests`, `conversations`, `notifications`, `reviews`, `reports`, `support_requests`, `app_config`, and `broadcasts`.

Chat messages are stored in a `messages` subcollection under conversation documents. Collection contents and security rules are **not** included in this repository.

## Repository and Security Notes

This GitHub-ready copy excludes local IDE settings, generated build outputs, machine-specific SDK paths, and `google-services.json`. The `.gitignore` also excludes these files and common signing credentials.

- Never commit passwords, private keys, keystores, service account JSON files, or local environment secrets.
- Do not use open/public Firestore rules for real users; apply least-privilege rules and validate user roles on the backend/rules side.
- The app includes an administration interface, but **client-side visibility checks must not substitute for server-side authorization**.
- Test with a Firebase project whose data you are authorized to use.

## Screenshots

Screenshots can be added later to a `screenshots/` directory and linked here to show the home screen, tool listings, request flow, chat, and account pages.

## Development Status

This repository contains the Android application source code. Backend setup and security rules are environment-specific, and no automated end-to-end verification is claimed here.

## License

No open-source license is included in this repository. Unless a license is added by the project owner, others should not assume permission to redistribute or reuse the code.

---

<div align="center">

**Nafa (نفع) — tools that bring neighbors together.**

</div>
