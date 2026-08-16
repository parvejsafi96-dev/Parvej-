package com.example.tyson

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppHelper {
    fun openChatWithMessage(ctx: Context, number: String, message: String) {
        try {
            val norm = number.replace("[^+0-9]".toRegex(), "")
            val numNoPlus = if (norm.startsWith("+")) norm.substring(1) else norm
            val encoded = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$numNoPlus&text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setPackage("com.whatsapp")
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                val encoded = URLEncoder.encode(message, "UTF-8")
                val url = "https://api.whatsapp.com/send?phone=$number&text=$encoded"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            } catch (_: Exception) { }
        }
    }
}
