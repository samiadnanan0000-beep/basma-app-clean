package com.example.util

import android.util.Log
import com.example.BuildConfig

object GeminiConfig {
    /**
     * Safely reads the GEMINI_API_KEY from BuildConfig without throwing any exceptions.
     * Returns empty string if not configured or if it's a placeholder.
     */
    val apiKey: String
        get() = try {
            val key = try {
                BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            } catch (_: Throwable) {
                null
            }
            if (key.isNullOrBlank() || key.contains("PLACEHOLDER", ignoreCase = true)) {
                ""
            } else {
                key
            }
        } catch (e: Throwable) {
            Log.w("GeminiConfig", "Safeguard reading Gemini API key: ${e.message}")
            ""
        }

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
}
