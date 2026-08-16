package com.example.tyson

import android.content.Context
import android.provider.ContactsContract

data class ContactMatch(val name: String, val number: String)

object ContactHelper {
    fun findContactsByName(ctx: Context, nameQuery: String, limit: Int = 10): List<ContactMatch> {
        val results = mutableListOf<ContactMatch>()
        val cr = ctx.contentResolver
        val tokens = nameQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        for ((i, token) in tokens.withIndex()) {
            if (i > 0) selectionBuilder.append(" AND ")
            selectionBuilder.append("LOWER(${ContactsContract.Contacts.DISPLAY_NAME}) LIKE ?")
            selectionArgs.add("%$token%")
        }

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val cursor = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selectionBuilder.toString(),
            selectionArgs.toTypedArray(),
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT $limit"
        )
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val phoneFlagIdx = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            while (c.moveToNext()) {
                val id = c.getString(idIdx)
                val display = c.getString(nameIdx) ?: continue
                val hasPhone = c.getInt(phoneFlagIdx)
                if (hasPhone > 0) {
                    val phones = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phones?.use { pcur ->
                        while (pcur.moveToNext()) {
                            val num = pcur.getString(0)
                            if (!num.isNullOrBlank()) results.add(ContactMatch(display, normalizeNumber(num)))
                        }
                    }
                }
            }
        }
        return results.distinctBy { it.number }
    }

    fun findBestNumber(ctx: Context, nameQuery: String): String? {
        val matches = findContactsByName(ctx, nameQuery, limit = 5)
        return matches.firstOrNull()?.number
    }

    private fun normalizeNumber(raw: String): String {
        return raw.replace("[\\s\\-()]+".toRegex(), "")
    }
}
