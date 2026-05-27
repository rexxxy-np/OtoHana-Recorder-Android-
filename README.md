# 🌸 OtoHana Recorder

A professional Android screen recorder with internal/external audio support, custom watermarks, 1080p recording, and configurable bitrates.

## Features
- 📹 1080p screen recording (1920×1080)
- 🎙️ Internal audio only OR Internal + External (mic) audio
- 💧 Custom text watermark (editable) + OtoHana logo watermark (fixed)
- ⚡ Multiple bitrate options (2, 4, 8, 16 Mbps)
- ☁️ Backend on Render for license/config management
- 🎨 Beautiful cherry blossom-themed UI

## Project Structure
```
OtoHanaRecorder/
├── android/          ← Android Studio project
├── backend/          ← Node.js backend (deploy to Render)
└── docs/             ← Setup guides
```

## Quick Start

### Android App
1. Open `android/` in Android Studio (or use GitHub Codespaces on mobile)
2. Update `BASE_URL` in `Constants.kt` to your Render backend URL
3. Build & install APK

### Backend (Render)
1. Push repo to GitHub
2. Create a new Web Service on Render, point to `backend/`
3. Set environment variables (see `backend/.env.example`)

## Requirements
- Android 10+ (API 29+) — required for internal audio capture
- Render free tier works fine for backend
