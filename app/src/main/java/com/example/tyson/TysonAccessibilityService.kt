package com.example.tyson

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.util.Log

class TysonAccessibilityService : AccessibilityService() {
    private val cmdReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.example.tyson.COMMAND" -> {
                    val name = intent.getStringExtra("intent_name") ?: return
                    val data = intent.getStringExtra("intent_data") ?: ""
                    handleIntent(name, data)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val f = IntentFilter().apply { addAction("com.example.tyson.COMMAND") }
        registerReceiver(cmdReceiver, f)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { unregisterReceiver(cmdReceiver); super.onDestroy() }

    private fun handleIntent(name: String, data: String) {
        when (name) {
            "open_app_by_name", "maybe_open_app" -> {
                val pkg = AppResolver.findPackageForAppName(applicationContext, data)
                if (pkg != null) {
                    openApp(pkg); TtsHelper.speak(applicationContext, "Opening $data")
                } else TtsHelper.speak(applicationContext, "I couldn't find $data")
            }
            "call_contact_by_name" -> {
                val number = ContactHelper.findBestNumber(applicationContext, data)
                if (number != null) {
                    callNumber(number); TtsHelper.speak(applicationContext, "Calling $data")
                } else TtsHelper.speak(applicationContext, "I couldn't find $data in contacts")
            }
            "whatsapp_contact" -> {
                val parts = data.split("|")
                val target = parts.getOrNull(0) ?: ""
                val message = parts.getOrNull(1) ?: ""
                val number = ContactHelper.findBestNumber(applicationContext, target)
                if (number != null) {
                    WhatsAppHelper.openChatWithMessage(applicationContext, number, if (message.isBlank()) " " else message)
                    TtsHelper.speak(applicationContext, "Opening WhatsApp to $target")
                } else {
                    val raw = target.replace("[^+0-9]".toRegex(), "")
                    if (raw.isNotBlank()) {
                        WhatsAppHelper.openChatWithMessage(applicationContext, raw, if (message.isBlank()) " " else message)
                        TtsHelper.speak(applicationContext, "Opening WhatsApp")
                    } else TtsHelper.speak(applicationContext, "I couldn't find $target in contacts")
                }
            }
            "sms_contact" -> {
                val parts = data.split("|")
                val target = parts.getOrNull(0) ?: ""
                val message = parts.getOrNull(1) ?: ""
                val number = ContactHelper.findBestNumber(applicationContext, target)
                if (number != null) {
                    sendSms(number, message); TtsHelper.speak(applicationContext, "Opening messaging to $target")
                } else TtsHelper.speak(applicationContext, "I couldn't find $target in contacts")
            }
            "say_time" -> {
                val t = java.time.LocalTime.now().withSecond(0).withNano(0).toString()
                TtsHelper.speak(applicationContext, "The time is $t")
            }
            "volume_up" -> adjustVolume(AudioManager.ADJUST_RAISE)
            "volume_down" -> adjustVolume(AudioManager.ADJUST_LOWER)
            else -> TtsHelper.speak(applicationContext, "Sorry, I can't do that yet.")
        }
    }

    private fun openApp(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            TtsHelper.speak(applicationContext, "Failed to open app")
        }
    }

    private fun adjustVolume(direction: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }

    private fun callNumber(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: SecurityException) {
            TtsHelper.speak(applicationContext, "I need call permission to do that")
        }
    }

    private fun sendSms(number: String, body: String) {
        val smsIntent = Intent(Intent.ACTION_SENDTO)
        smsIntent.data = Uri.parse("smsto:$number")
        smsIntent.putExtra("sms_body", body)
        smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(smsIntent)
    }
}
