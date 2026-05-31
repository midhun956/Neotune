package com.example.neotune

object Config {
    // Backend server configuration
    var BACKEND_BASE_URL = ""
    
    // Timeout settings (in milliseconds)
    const val REQUEST_TIMEOUT = 30000L
    const val CONNECT_TIMEOUT = 15000L
    const val SOCKET_TIMEOUT = 30000L
    
    // Retry settings
    const val DEFAULT_MAX_RETRIES = 3
    
    // Search settings
    const val DEFAULT_SEARCH_LIMIT = 20
    const val DEFAULT_RECOMMENDED_LIMIT = 20
    const val DEFAULT_TRENDING_LIMIT = 20
}

