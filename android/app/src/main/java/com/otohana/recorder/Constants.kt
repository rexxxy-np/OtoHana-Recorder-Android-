package com.otohana.recorder

object Constants {

    // ── Backend ──────────────────────────────────────────────────────────────
    // Replace with your actual Render URL after deployment
    const val BASE_URL = "https://otohana-backend.onrender.com"

    // ── Recording defaults ────────────────────────────────────────────────────
    const val DEFAULT_WIDTH      = 1920
    const val DEFAULT_HEIGHT     = 1080
    const val DEFAULT_FRAME_RATE = 60
    const val DEFAULT_BITRATE    = BitrateOption.BITRATE_8MBPS  // index

    // ── Bitrate options (bits per second) ─────────────────────────────────────
    object BitrateOption {
        const val BITRATE_2MBPS  = 0
        const val BITRATE_4MBPS  = 1
        const val BITRATE_8MBPS  = 2
        const val BITRATE_16MBPS = 3

        val labels = arrayOf("2 Mbps", "4 Mbps", "8 Mbps", "16 Mbps")
        val values = intArrayOf(
            2_000_000,
            4_000_000,
            8_000_000,
            16_000_000
        )
    }

    // ── Audio modes ───────────────────────────────────────────────────────────
    const val AUDIO_INTERNAL          = 0   // Internal audio only
    const val AUDIO_INTERNAL_EXTERNAL = 1   // Internal + Microphone

    // ── Watermark ─────────────────────────────────────────────────────────────
    const val DEFAULT_WATERMARK_TEXT   = "OtoHana"
    const val WATERMARK_TEXT_EDITABLE  = true   // user can change text
    const val WATERMARK_LOGO_EDITABLE  = false  // logo position/visibility locked

    // ── Notification ─────────────────────────────────────────────────────────
    const val NOTIFICATION_CHANNEL_ID   = "otohana_recording"
    const val NOTIFICATION_CHANNEL_NAME = "OtoHana Recording"
    const val NOTIFICATION_ID           = 1001

    // ── SharedPrefs keys ──────────────────────────────────────────────────────
    const val PREF_FILE              = "otohana_prefs"
    const val PREF_WATERMARK_TEXT    = "watermark_text"
    const val PREF_BITRATE_INDEX     = "bitrate_index"
    const val PREF_AUDIO_MODE        = "audio_mode"
    const val PREF_WATERMARK_ENABLED = "watermark_enabled"
}
