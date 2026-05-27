package com.otohana.recorder

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etWatermarkText: EditText
    private lateinit var switchWatermark: Switch
    private lateinit var spinnerBitrate: Spinner
    private lateinit var spinnerAudio: Spinner
    private lateinit var btnSave: Button
    private lateinit var tvAppVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }
        prefs = getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE)
        bindViews()
        setupSpinners()
        loadSettings()
        btnSave.setOnClickListener { saveSettings() }
    }

    private fun bindViews() {
        etWatermarkText = findViewById(R.id.etSettingsWatermarkText)
        switchWatermark  = findViewById(R.id.switchSettingsWatermark)
        spinnerBitrate   = findViewById(R.id.spinnerSettingsBitrate)
        spinnerAudio     = findViewById(R.id.spinnerSettingsAudio)
        btnSave          = findViewById(R.id.btnSettingsSave)
        tvAppVersion     = findViewById(R.id.tvSettingsVersion)
    }

    private fun setupSpinners() {
        ArrayAdapter(this, android.R.layout.simple_spinner_item, Constants.BitrateOption.labels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinnerBitrate.adapter = it }
        ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("Internal Audio Only", "Internal + Microphone"))
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinnerAudio.adapter = it }
    }

    private fun loadSettings() {
        etWatermarkText.setText(prefs.getString(Constants.PREF_WATERMARK_TEXT, Constants.DEFAULT_WATERMARK_TEXT))
        switchWatermark.isChecked = prefs.getBoolean(Constants.PREF_WATERMARK_ENABLED, true)
        spinnerBitrate.setSelection(prefs.getInt(Constants.PREF_BITRATE_INDEX, Constants.DEFAULT_BITRATE))
        spinnerAudio.setSelection(prefs.getInt(Constants.PREF_AUDIO_MODE, Constants.AUDIO_INTERNAL))
        tvAppVersion.text = "OtoHana Recorder v1.0.0"
    }

    private fun saveSettings() {
        prefs.edit()
            .putString(Constants.PREF_WATERMARK_TEXT, etWatermarkText.text.toString().trim().ifEmpty { Constants.DEFAULT_WATERMARK_TEXT })
            .putBoolean(Constants.PREF_WATERMARK_ENABLED, switchWatermark.isChecked)
            .putInt(Constants.PREF_BITRATE_INDEX, spinnerBitrate.selectedItemPosition)
            .putInt(Constants.PREF_AUDIO_MODE, spinnerAudio.selectedItemPosition)
            .apply()
        Toast.makeText(this, "Settings saved ✓", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
