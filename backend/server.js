// backend/server.js
// OtoHana Recorder Backend — deploy to Render
// Handles: config delivery, usage analytics, watermark defaults

const express = require('express');
const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;

// ── In-memory store (swap for a DB like Render's Postgres for production) ──
const recordings = [];

// ── Routes ────────────────────────────────────────────────────────────────────

// Health check (Render pings this)
app.get('/', (req, res) => {
    res.json({ status: 'ok', app: 'OtoHana Recorder Backend', version: '1.0.0' });
});

// App config — Android app fetches this on launch
app.get('/api/config', (req, res) => {
    res.json({
        latestVersion:     '1.0.0',
        forceUpdate:        false,
        defaultWatermark:  'OtoHana',
        logoWatermarkLock:  true,     // logo watermark cannot be disabled by user
        allowedBitrates:   [2, 4, 8, 16],  // Mbps
        maxResolution:     { width: 1920, height: 1080 },
        maxFrameRate:       60,
        internalAudioNote: 'Requires Android 10+',
        supportEmail:      'support@otohana.app'
    });
});

// Log a completed recording (called by app on stop)
app.post('/api/recordings/log', (req, res) => {
    const { deviceId, durationSeconds, bitrateKbps, audioMode, hasWatermark } = req.body;

    if (!deviceId || !durationSeconds) {
        return res.status(400).json({ error: 'deviceId and durationSeconds required' });
    }

    const entry = {
        id:              recordings.length + 1,
        deviceId,
        durationSeconds,
        bitrateKbps:    bitrateKbps  || null,
        audioMode:      audioMode    || 'internal',
        hasWatermark:   hasWatermark ?? true,
        timestamp:      new Date().toISOString()
    };

    recordings.push(entry);
    console.log('[OtoHana] Recording logged:', entry);

    res.status(201).json({ success: true, id: entry.id });
});

// Stats endpoint (admin use)
app.get('/api/stats', (req, res) => {
    const total        = recordings.length;
    const totalMinutes = recordings.reduce((s, r) => s + r.durationSeconds, 0) / 60;
    res.json({ totalRecordings: total, totalMinutes: Math.round(totalMinutes) });
});

// ── Start ─────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
    console.log(`🌸 OtoHana Backend running on port ${PORT}`);
});
