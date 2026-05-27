package com.otohana.recorder

import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer

/**
 * AudioMixer — mixes internal (AudioPlaybackCapture) + microphone audio.
 *
 * Usage:
 *   val mixer = AudioMixer(mediaProjection, muxer, audioTrackIndex)
 *   mixer.start()
 *   // ... recording ...
 *   mixer.stop()
 */
@RequiresApi(Build.VERSION_CODES.Q)
class AudioMixer(
    private val mediaProjection: MediaProjection,
    private val muxer: MediaMuxer,
    private val trackIndex: Int
) {
    companion object {
        private const val TAG        = "OtoHanaAudioMixer"
        private const val SAMPLE_RATE = 44_100
        private const val CHANNELS    = AudioFormat.CHANNEL_IN_STEREO
        private const val ENCODING    = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING) * 4
    }

    @Volatile private var running = false
    private var internalRecorder: AudioRecord? = null
    private var micRecorder: AudioRecord? = null
    private var mixThread: Thread? = null

    fun start() {
        running = true

        // Internal audio (system playback)
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        internalRecorder = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(captureConfig)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNELS)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build()

        // Microphone
        micRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNELS, ENCODING, BUFFER_SIZE
        )

        internalRecorder!!.startRecording()
        micRecorder!!.startRecording()

        mixThread = Thread {
            val inBuf  = ByteArray(BUFFER_SIZE)
            val micBuf = ByteArray(BUFFER_SIZE)
            val outBuf = ByteArray(BUFFER_SIZE)
            val info   = MediaCodec.BufferInfo()
            var presentationUs = 0L

            while (running) {
                val inRead  = internalRecorder!!.read(inBuf,  0, BUFFER_SIZE)
                val micRead = micRecorder!!.read(micBuf, 0, BUFFER_SIZE)
                val count   = minOf(inRead, micRead).coerceAtLeast(0)

                // Simple additive mix with clamp
                for (i in 0 until count step 2) {
                    val inSample  = (inBuf[i].toInt()  or (inBuf[i + 1].toInt() shl 8)).toShort().toInt()
                    val micSample = (micBuf[i].toInt() or (micBuf[i + 1].toInt() shl 8)).toShort().toInt()
                    val mixed     = (inSample + micSample).coerceIn(-32768, 32767).toShort()
                    outBuf[i]     = (mixed.toInt() and 0xFF).toByte()
                    outBuf[i + 1] = ((mixed.toInt() shr 8) and 0xFF).toByte()
                }

                if (count > 0) {
                    info.offset        = 0
                    info.size          = count
                    info.presentationTimeUs = presentationUs
                    info.flags         = 0
                    muxer.writeSampleData(trackIndex, ByteBuffer.wrap(outBuf, 0, count), info)
                    presentationUs += (count.toLong() * 1_000_000L) / (SAMPLE_RATE * 2 * 2)
                }
            }
        }.also {
            it.name = "OtoHana-AudioMix"
            it.start()
        }

        Log.i(TAG, "AudioMixer started")
    }

    fun stop() {
        running = false
        mixThread?.interrupt()
        mixThread?.join(2000)
        internalRecorder?.stop()
        internalRecorder?.release()
        micRecorder?.stop()
        micRecorder?.release()
        Log.i(TAG, "AudioMixer stopped")
    }
}
