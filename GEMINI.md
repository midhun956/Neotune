# Neotune: Music Streaming Application

Neotune is a comprehensive music streaming system consisting of a modern Android application and a specialized Python backend. It leverages YouTube Music's vast library to provide a seamless streaming experience.

## Project Overview

### Architecture
- **Android App:** Built with Jetpack Compose, following the MVVM (Model-View-ViewModel) architecture. It handles UI, playback state, and user library management.
- **Python Backend:** A FastAPI server that acts as a proxy for YouTube Music. It uses `ytmusicapi` for metadata/search and `yt-dlp` for extracting high-quality audio streams.

### Core Technologies
- **Frontend (Android):** Kotlin, Jetpack Compose, Media3 ExoPlayer, Ktor (HTTP client), Coil (Image loading), Kotlinx Serialization.
- **Backend (Python):** FastAPI, Uvicorn, yt-dlp, ytmusicapi, requests, cachetools.

## Building and Running

### Android Application
The app is a standard Android Studio project using Gradle (Kotlin DSL).

- **Build APK:** `./gradlew assembleDebug`
- **Run on Device/Emulator:** `./gradlew installDebug`
- **Configuration:** Update `app/src/main/java/com/example/neotune/Config.kt` to point to the correct `BACKEND_BASE_URL` (usually your local machine's IP).

### Python Backend
The backend requires Python 3.x and FFmpeg (for `yt-dlp` audio processing).

- **Installation:**
  ```bash
  cd backend
  pip install -r requirements.txt
  ```
- **Execution:**
  ```bash
  python run.py
  ```
- **Main Entry Point:** `backend/main.py` (FastAPI app)
- **Port:** Defaults to `8000`.

## Development Conventions

### Android (Frontend)
- **UI:** Exclusively built with **Jetpack Compose**.
- **State Management:** Uses `SearchViewModel` to manage search results, playback queue, and user library (likes/playlists).
- **Networking:** All API calls are encapsulated in `YouTubeMusicApi.kt` using Ktor.
- **Playback:** Handled by a shared `ExoPlayer` instance managed within the `SearchViewModel`.

### Python (Backend)
- **Performance:** Implements a **Priority-Based Task Management System** (`PriorityThreadPool`) to ensure playback requests (CRITICAL) take precedence over search or prefetching.
- **Caching:** Audio URLs are cached using `TTLCache` (2-hour TTL) to reduce redundant `yt-dlp` extraction calls.
- **Streaming:** Supports **HTTP Range requests**, enabling efficient seeking in the Android player.
- **Task Priorities:**
  - `CRITICAL`: Playback and seeking.
  - `HIGH`: Search requests.
  - `MEDIUM`: Recommendations and trending.
  - `LOW`: Background prefetching.

## Key Files
- `app/src/main/java/com/example/neotune/Config.kt`: Global app configuration (API URLs, timeouts).
- `app/src/main/java/com/example/neotune/YouTubeMusicApi.kt`: Ktor-based client for the backend API.
- `app/src/main/java/com/example/neotune/SearchViewModel.kt`: Central state manager for the app.
- `backend/main.py`: API endpoint definitions and core logic.
- `backend/run.py`: Launcher script that updates dependencies and starts the server.
