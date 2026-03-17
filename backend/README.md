# Neotune Backend

This is the Python backend for the Neotune Android music app. It provides a FastAPI server that uses yt-dlp to extract audio streams from YouTube Music.

## Setup

1. **Install Python dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

2. **Run the server:**
   ```bash
   python run.py
   ```

   This will:
   - Update yt-dlp to the latest version
   - Start the FastAPI server on port 8000

## API Endpoints

- `GET /search?query=<search_term>&limit=<number>` - Search for songs
- `GET /yt_audio?video_id=<youtube_video_id>` - Get audio stream URL
- `GET /recommended?video_id=<youtube_video_id>&limit=<number>` - Get recommended songs
- `GET /trending?limit=<number>` - Get trending songs
- `GET /featured?limit=<number>` - Get featured playlists

## Configuration

The server runs on `http://localhost:8000` by default. To access it from your Android device:

1. Find your computer's IP address on your local network
2. Update the `BACKEND_BASE_URL` in `app/src/main/java/com/example/neotune/Config.kt` to use your computer's IP address
3. Make sure your Android device and computer are on the same network

## Features

- **Audio streaming** with proper HTTP Range support for seeking
- **Caching** of audio URLs to improve performance
- **Background prefetching** of related songs
- **Priority-based task scheduling** for optimal performance
- **Automatic yt-dlp updates** to stay current with YouTube changes

## Troubleshooting

- If you get connection errors, make sure the backend server is running
- Check that your computer's firewall allows connections on port 8000
- Verify that both devices are on the same network
- Try using `ipconfig` (Windows) or `ifconfig` (Mac/Linux) to find your computer's IP address

