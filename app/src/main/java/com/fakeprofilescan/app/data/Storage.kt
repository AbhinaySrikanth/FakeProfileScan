package com.fakeprofilescan.app.data

import android.content.Context
import com.fakeprofilescan.app.BuildConfig
import org.json.JSONArray
import java.io.File
import java.time.Instant

data class Settings(
    val apiUrl: String = BuildConfig.DEFAULT_API_URL,
    val storeHandle: Boolean = false,
    val retentionDays: Int = 30, // 0 = keep until deleted
)

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = Settings(
        apiUrl = p.getString("apiUrl", null) ?: BuildConfig.DEFAULT_API_URL,
        storeHandle = p.getBoolean("storeHandle", false),
        retentionDays = p.getInt("retentionDays", 30),
    )

    fun save(s: Settings) = p.edit().putString("apiUrl", s.apiUrl).putBoolean("storeHandle", s.storeHandle)
        .putInt("retentionDays", s.retentionDays).apply()
}

/** Assessment history in app-private storage (excluded from backup). Small list, so one JSON file is enough. */
class HistoryStore(dir: File) {
    private val file = File(dir, "history.json")

    @Synchronized
    fun load(retentionDays: Int, now: Instant = Instant.now()): List<Assessment> {
        val all = try {
            JSONArray(file.readText()).let { a -> List(a.length()) { Assessment.fromJson(a.getJSONObject(it)) } }
        } catch (e: Exception) {
            emptyList()
        }
        if (retentionDays <= 0) return all
        val cutoff = now.minusSeconds(retentionDays * 86_400L)
        val kept = all.filter { runCatching { Instant.parse(it.assessedAt) }.getOrDefault(now).isAfter(cutoff) }
        if (kept.size != all.size) write(kept)
        return kept
    }

    @Synchronized
    fun write(list: List<Assessment>) {
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(JSONArray(list.map { it.toJson() }).toString())
        tmp.renameTo(file)
    }

    @Synchronized
    fun clear() { file.delete() }
}
