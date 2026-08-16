package com.example.tyson

import android.content.Context
import java.util.regex.Pattern

data class ParsedIntent(val name: String, val data: String)

object Nlu {
    fun parse(text: String, ctx: Context): ParsedIntent {
        val lower = text.lowercase().trim()

        val openAppPattern = Pattern.compile("^(open|launch|start) (.+)$")
        val callPattern = Pattern.compile("^(call|dial) (.+)$")
        val whatsappPattern = Pattern.compile("^(?:send (?:a )?whatsapp (?:message )?(?:to )?|whatsapp )(.+?)(?: saying |: | - | message | says )?(.*)$")
        val smsPattern = Pattern.compile("^(?:send (?:a )?message to |message )(.+?)(?: saying |: |, | - )?(.*)$")

        openAppPattern.matcher(lower).let { m ->
            if (m.matches()) return ParsedIntent("open_app_by_name", m.group(2).trim())
        }
        callPattern.matcher(lower).let { m ->
            if (m.matches()) return ParsedIntent("call_contact_by_name", m.group(2).trim())
        }
        whatsappPattern.matcher(lower).let { m ->
            if (m.matches()) {
                val target = m.group(1)?.trim() ?: ""
                val msg = m.group(2)?.trim() ?: ""
                return ParsedIntent("whatsapp_contact", "$target|$msg")
            }
        }
        smsPattern.matcher(lower).let { m ->
            if (m.matches()) {
                val tgt = m.group(1)?.trim() ?: ""
                val msg = m.group(2)?.trim() ?: ""
                return ParsedIntent("sms_contact", "$tgt|$msg")
            }
        }

        if (lower.contains("time")) return ParsedIntent("say_time", "")
        return ParsedIntent("maybe_open_app", lower.replace("^open\\s+".toRegex(), "").trim())
    }
}
