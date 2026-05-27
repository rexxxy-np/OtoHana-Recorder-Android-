package com.otohana.recorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // UI references (matched to activity_main.xml ids)
    private lateinit var btnRecord: Button
    private lateinit var tvStatus: TextView
    private lateinit var spinnerBitrate: Spinner
    private lateinit var spinnerAudio: Spinner
    private lateinit var etWatermarkText: EditText
    private lateinit var switchWatermark: Switch
    private lateinit var tvTimer: TextView

    // Tracks whether we are currently recording
    private var isRecording = false

    // ── Permission launcher ───────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            launchMediaProjection()
        } else {
            Toast.makeText(this, "Permissions required to record audio", Toast.LENGTH_LONG).show()
        }
    }

    // ── Media projection launcher ─────────────────────────────────────────────
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            saveSettings()
            startRecordingService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        bindViews()
        setupSpinners()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        btnRecord       = findViewById(R.id.btnRecord)
        tvStatus        = findViewById(R.id.tvStatus)
        spinnerBitrate  = findViewById(R.id.spinnerBitrate)
        spinnerAudio    = findViewById(R.id.spinnerAudio)
        etWatermarkText = findViewById(R.id.etWatermarkText)
        switchWatermark = findViewById(R.id.switchWatermark)
        tvTimer         = findViewById(R.id.tvTimer)
    }

    private fun setupSpinners() {
        // Bitrate
        val bitrateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Constants.BitrateOption.labels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerBitrate.adapter = bitrateAdapter

        // Audio mode
        val audioLabels = arrayOf("Internal Audio Only", "Internal + Microphone")
        val audioAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            audioLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerAudio.adapter = audioAdapter
    }

    private fun loadSettings() {
        spinnerBitrate.setSelection(prefs.getInt(Constants.PREF_BITRATE_INDEX, Constants.DEFAULT_BITRATE))
        spinnerAudio.setSelection(prefs.getInt(Constants.PREF_AUDIO_MODE, Constants.AUDIO_INTERNAL))
        etWatermarkText.setText(prefs.getString(Constants.PREF_WATERMARK_TEXT, Constants.DEFAULT_WATERMARK_TEXT))
        switchWatermark.isChecked = prefs.getBoolean(Constants.PREF_WATERMARK_ENABLED, true)
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt(Constants.PREF_BITRATE_INDEX, spinnerBitrate.selectedItemPosition)
            .putInt(Constants.PREF_AUDIO_MODE, spinnerAudio.selectedItemPosition)
            .putString(Constants.PREF_WATERMARK_TEXT, etWatermarkText.text.toString())
            .putBoolean(Constants.PREF_WATERMARK_ENABLED, switchWatermark.isChecked)
            .apply()
    }

    private fun setupListeners() {
        btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else checkPermissionsAndRecord()
        }
    }

    // ── Permission & projection flow ──────────────────────────────────────────
    private fun checkPermissionsAndRecord() {
        val needed = mutableListOf<String>()
        if (spinnerAudio.selectedItemPosition == Constants.AUDIO_INTERNAL_EXTERNAL) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            launchMediaProjection()
        }
    }

    private fun launchMediaProjection() {
        projectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    // ── Service communication ─────────────────────────────────────────────────
    private fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, data)
            putExtra(RecordingService.EXTRA_BITRATE,
                Constants.BitrateOption.values[spinnerBitrate.selectedItemPosition])
            putExtra(RecordingService.EXTRA_AUDIO_MODE, spinnerAudio.selectedItemPosition)
            putExtra(RecordingService.EXTRA_WATERMARK_TEXT, etWatermarkText.text.toString())
            putExtra(RecordingService.EXTRA_WATERMARK_ENABLED, switchWatermark.isChecked)
        }
        ContextCompat.startForegroundService(this, intent)

        isRecording = true
        btnRecord.text = "⏹ Stop Recording"
        btnRecord.setBackgroundColor(getColor(R.color.stop_red))
        tvStatus.text = "Recording…"
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        startService(intent)

        isRecording = false
        btnRecord.text = "⏺ Start Recording"
        btnRecord.setBackgroundColor(getColor(R.color.brand_pink))
        tvStatus.text = "Ready"
        tvTimer.text = "00:00:00"
    }
}
