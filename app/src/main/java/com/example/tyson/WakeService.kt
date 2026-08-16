package com.example.tyson

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class WakeService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var running = false
    private var audioRecord: AudioRecord? = null
    private var model: Model? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        scope.launch { prepareModelAndRun() }
        TtsHelper.init(applicationContext)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "tyson_wake_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(NotificationChannel(channelId, "Tyson", NotificationManager.IMPORTANCE_LOW))
        }
        val n = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tyson (offline)")
            .setContentText("Listening for 'Hey Tyson'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1, n)
    }

    private suspend fun prepareModelAndRun() {
        // Model folder on device
        val modelPath = File("/sdcard/vosk-model-small-en-us-0.15")
        if (!modelPath.exists()) {
            TtsHelper.speak(applicationContext, "Vosk model not found. Please place model at /sdcard/vosk-model-small-en-us-0.15")
            return
        }
        model = Model(modelPath.absolutePath)
        startWakeLoop()
    }

    private fun startWakeLoop() {
        running = true
        scope.launch {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
            audioRecord?.startRecording()

            val wakeGrammar = "[\"hey tyson\"]"
            val wakeRecognizer = Recognizer(model, sampleRate.toFloat(), wakeGrammar)
            val buffer = ByteArray(4096)
            while (running && audioRecord != null) {
                val read = audioRecord!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (wakeRecognizer.acceptWaveForm(buffer, read)) {
                        val res = wakeRecognizer.result
                        if (res.contains("hey tyson", ignoreCase = true)) {
                            onWakeDetected()
                        }
                    } else {
                        val partial = wakeRecognizer.partialResult
                        if (partial.contains("hey tyson", ignoreCase = true)) {
                            onWakeDetected()
                        }
                    }
                }
            }
            wakeRecognizer.close()
        }
    }

    private fun onWakeDetected() {
        scope.launch {
            // brief TTS ack
            TtsHelper.speak(applicationContext, "Yes?")
            // collect a short command (3s)
            val sampleRate = 16000
            val cmdRecognizer = Recognizer(model, sampleRate.toFloat())
            val cmdBuffer = ByteArray(4096)
            val start = System.currentTimeMillis()
            val timeout = 3000L
            while (System.currentTimeMillis() - start < timeout) {
                val read = audioRecord?.read(cmdBuffer, 0, cmdBuffer.size) ?: 0
                if (read > 0) {
                    cmdRecognizer.acceptWaveForm(cmdBuffer, read)
                }
            }
            val finalJson = cmdRecognizer.finalResult
            cmdRecognizer.close()
            val text = extractTextFromVoskResult(finalJson)
            if (text.isNotBlank()) {
                handleCommand(text)
            } else {
                TtsHelper.speak(applicationContext, "I didn't catch that")
            }
        }
    }

    private fun extractTextFromVoskResult(json: String): String {
        val regex = "\"text\"\s*:\s*\"([^\"]*)\"".toRegex()
        val m = regex.find(json)
        return m?.groups?.get(1)?.value ?: ""
    }

    private fun handleCommand(text: String) {
        val intent = Nlu.parse(text, applicationContext)
        val b = Intent("com.example.tyson.COMMAND")
        b.putExtra("intent_name", intent.name)
        b.putExtra("intent_data", intent.data)
        sendBroadcast(b)
    }

    override fun onDestroy() {
        running = false
        audioRecord?.stop()
        audioRecord?.release()
        model?.close()
        TtsHelper.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
