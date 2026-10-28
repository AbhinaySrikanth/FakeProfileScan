package com.fakeprofilescan.app.data

import org.json.JSONObject

/** Editable form state. Validation mirrors the server limits for fast feedback; the server stays authoritative. */
data class FormState(
    val username: String = "",
    val fullName: String = "",
    val hasPicture: Boolean = true,
    val bio: String = "",
    val hasLink: Boolean = false,
    val isPrivate: Boolean = false,
    val posts: String = "",
    val followers: String = "",
    val following: String = "",
    val flags: Set<String> = emptySet(),
) {
    fun errors(): Map<String, String> = buildMap {
        val u = username.trim()
        if (u.isEmpty()) put("username", "Enter the username (without @).")
        else if (u.length > 30) put("username", "Usernames are at most 30 characters.")
        else if (u.any { it.isWhitespace() }) put("username", "Usernames cannot contain spaces.")
        if (fullName.length > 100) put("fullName", "At most 100 characters.")
        if (bio.length > 150) put("bio", "Bios are at most 150 characters.")
        count("posts", posts, 10_000_000)
        count("followers", followers, 1_000_000_000)
        count("following", following, 100_000)
    }

    private fun MutableMap<String, String>.count(key: String, v: String, max: Long) {
        val n = parseCount(v)
        if (v.isBlank()) put(key, "Required. Enter 0 if none.")
        else if (n == null) put(key, "Enter a number like 850, 12,400, 1.2K or 3M.")
        else if (n > max) put(key, "Must be at most $max.")
    }

    /** Only the minimal fields the model needs: the bio text itself is never sent. */
    fun toRequestJson(): JSONObject = JSONObject()
        .put("username", username.trim()).put("full_name", fullName.trim())
        .put("has_profile_picture", hasPicture).put("bio_length", bio.length)
        .put("has_external_url", hasLink).put("is_private", isPrivate)
        .put("posts", parseCount(posts)).put("followers", parseCount(followers))
        .put("following", parseCount(following)).put("observed_flags", org.json.JSONArray(flags.toList()))

    companion object {
        /** Behaviour the user can report. Ids match the server; the server decides what they mean for the band. */
        val FLAG_CATALOG = listOf(
            "asks_money" to "Asked for money, gift cards or crypto",
            "asks_codes" to "Asked for passwords, codes or card details",
            "urgency" to "Pressure to act quickly",
            "off_platform" to "Pushed to move the chat to another app",
            "stolen_photos" to "Photos appear copied from elsewhere",
            "lookalike" to "Username imitates someone or a brand you know",
            "too_good" to "Offers prizes, jobs or investment returns",
        )

        /** Maps server field names to our form keys. */
        val serverField = mapOf("username" to "username", "full_name" to "fullName", "bio_length" to "bio",
            "posts" to "posts", "followers" to "followers", "following" to "following")
    }
}

/** Accepts counts as profiles display them: 850, 12,400, 1.2K, 3M, 1.1B. Returns null if not a valid non-negative count. */
fun parseCount(raw: String): Long? {
    val m = Regex("""(\d+(?:\.\d+)?)([KMB]?)""").matchEntire(raw.trim().replace(",", "").uppercase()) ?: return null
    val mult = when (m.groupValues[2]) { "K" -> 1_000.0; "M" -> 1_000_000.0; "B" -> 1_000_000_000.0; else -> 1.0 }
    return Math.round(m.groupValues[1].toDouble() * mult)
}
