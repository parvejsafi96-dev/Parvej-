package com.example.tyson

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsHelper {
    private var tts: TextToSpeech? = null
    private var initialized = false

    fun init(ctx: Context) {
        if (initialized) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
        initialized = true
    }

    fun speak(ctx: Context, text: String) {
        if (!initialized) init(ctx)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tyson-tts")
    }

    fun shutdown() {
        tts?.shutdown()
        initialized = false
    }
}
