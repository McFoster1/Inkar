package com.mcfoster.inkar

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** After the repo is renamed to Inkar, GitHub forwards this old name, so it keeps working. */
const val REPO_SLUG = "McFoster1/Inkar-Native"
const val REPO_URL = "https://github.com/$REPO_SLUG"
const val RELEASES_URL = "$REPO_URL/releases"

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val url: String) : UpdateState
    data object Failed : UpdateState
}

@Suppress("DEPRECATION")
fun currentVersion(ctx: Context): String =
    try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }

private fun parseVersion(text: String): List<Int>? {
    val m = Regex("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").find(text) ?: return null
    return listOf(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].ifEmpty { "0" }.toInt())
}

private fun compareVersions(a: List<Int>, b: List<Int>): Int {
    for (i in 0 until 3) {
        if (a[i] != b[i]) return a[i].compareTo(b[i])
    }
    return 0
}

/** Checks the project's GitHub releases. The only network call in the app; it never sends your writing. */
object Updater {
    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    fun check(ctx: Context) {
        if (state == UpdateState.Checking) return
        state = UpdateState.Checking
        val current = currentVersion(ctx)
        thread {
            val result = try {
                fetch(current)
            } catch (e: Exception) {
                UpdateState.Failed
            }
            onMain { state = result }
        }
    }

    private fun fetch(current: String): UpdateState {
        val conn = URL("https://api.github.com/repos/$REPO_SLUG/releases?per_page=10").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Inkar")
            if (conn.responseCode != 200) return UpdateState.Failed
            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            var bestVersion: List<Int>? = null
            var bestUrl = RELEASES_URL
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
                val v = parseVersion(r.optString("tag_name")) ?: continue
                val top = bestVersion
                if (top == null || compareVersions(v, top) > 0) {
                    bestVersion = v
                    bestUrl = r.optString("html_url").ifBlank { RELEASES_URL }
                }
            }
            val latest = bestVersion ?: return UpdateState.UpToDate
            val cur = parseVersion(current) ?: return UpdateState.Failed
            return if (compareVersions(latest, cur) > 0) UpdateState.Available(latest.joinToString("."), bestUrl) else UpdateState.UpToDate
        } finally {
            conn.disconnect()
        }
    }
}
