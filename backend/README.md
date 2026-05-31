# 🎵 Neotune Backend API Server

This is the consolidated Python backend for the **Neotune Android Music Player**. It delivers a robust FastAPI server that uses `yt-dlp` to extract high-fidelity audio streams from YouTube Music in real-time.

---

## 🚀 One-Command Unified Launcher

The backend is managed entirely by a single unified script: [`run.py`](file:///d:/Coding/Projects/Neotune/backend/run.py). 

When you execute it, it automatically performs the following tasks:
1. **Network Discovery**: Identifies your computer's local network IP address and generates a custom configuration guide for your Android app on-screen.
2. **Extractor Updates**: Automatically checks and upgrades `yt-dlp` to the latest version to bypass YouTube algorithm changes.
3. **Port Conflict Cleanup**: Scans port `8000` for existing or zombie python processes from previous runs and offers to terminate them automatically.
4. **Starts FastAPI Server**: Launches the API server on all interfaces (`0.0.0.0`) so it is accessible from your mobile device.

---

## 🛠️ Getting Started

### 1. Install Dependencies
Ensure you have Python 3.8+ installed, then install the package requirements:
```bash
pip install -r requirements.txt
```

### 2. Launch the Server
Simply run the launcher script from the `backend/` directory:
```bash
python run.py
```

---

## 📱 Android App Connection

To connect the Neotune Android app on your phone to this backend:

1. Check the local network IP detected by the launcher on startup (e.g., `192.168.1.15`).
2. Open the **Neotune app** on your phone.
3. Navigate to **Settings** -> tap **Connection**, enter your Server IP and port (e.g. `http://192.168.1.15:8000`), and tap **Check Connection**!

> [!NOTE]
> Make sure both your computer and your Android device are connected to the **same network** (e.g. the same home Wi-Fi).

---

## 📡 API Endpoints

- **`GET /search`**: Search for songs, albums, and playlists on YouTube Music.
  - Query parameters: `query` (search term), `limit` (default: 20)
- **`GET /yt_audio`**: Stream audio bytes for a YouTube video. Supports HTTP Range requests for seamless seeking/scrubbing.
  - Query parameters: `video_id` (YouTube Video ID)
- **`GET /recommended`**: Retrieve context-aware song recommendations.
  - Query parameters: `video_id` (seed track ID), `limit` (default: 20)
- **`GET /trending`**: Get current top trending tracks.
  - Query parameters: `limit` (default: 20)
- **`GET /featured`**: Fetch featured music playlists.
  - Query parameters: `limit` (default: 5)
- **`GET /ping`**: Server connection diagnostic ping endpoint.

---

## 📂 Folder Layout

```
backend/
├── main.py             # FastAPI Server application core logic & routing
├── run.py              # Unified Launcher script (IP discovery, yt-dlp check, server startup)
├── requirements.txt    # Python library requirements list
└── README.md           # Documentation (this file)
```

---

## 🔧 Troubleshooting

* **Firewall Blocks**: If your phone cannot connect to your PC, verify that your computer's firewall allows inbound TCP traffic on port `8000`.
* **Network Mismatch**: Double-check that your phone is not on a mobile data connection or a guest Wi-Fi network that prevents communication with local LAN clients.
