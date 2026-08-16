package com.example.tyson

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppResolver {
    fun findPackageForAppName(ctx: Context, spokenName: String): String? {
        val pm = ctx.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val query = spokenName.lowercase().trim()
        val tokens = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val labelMatches = mutableListOf<Pair<String, String>>()

        for (app in apps) {
            val label = (app.loadLabel(pm)?.toString() ?: "").lowercase()
            if (label.isBlank()) continue
            val matchesAll = tokens.all { label.contains(it) }
            if (matchesAll) labelMatches.add(Pair(label, app.packageName))
        }
        if (labelMatches.isNotEmpty()) return labelMatches.first().second

        val anyMatches = apps.mapNotNull { app ->
            val label = (app.loadLabel(pm)?.toString() ?: "").lowercase()
            if (tokens.any { label.contains(it) }) Pair(label, app.packageName) else null
        }
        if (anyMatches.isNotEmpty()) return anyMatches.first().second

        return apps.firstOrNull { it.packageName.lowercase().contains(query) }?.packageName
    }
}
