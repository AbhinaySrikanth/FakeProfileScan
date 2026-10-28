package com.fakeprofilescan.app.data

import org.json.JSONArray
import org.json.JSONObject

enum class Band { LOW, REVIEW, ELEVATED;
    companion object { fun parse(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: REVIEW }
}

data class Signal(val name: String, val raises: Boolean, val weight: Double)

/** Server verdict plus the user's own context. Raw profile data is never stored here. */
data class Assessment(
    val id: String,
    val score: Double,
    val band: Band,
    val lowConfidence: Boolean,
    val signals: List<Signal>,
    val unusualInputs: List<String>,
    val modelVersion: String,
    val assessedAt: String,
    val handle: String? = null,
    val note: String = "",
    val modelBand: Band = band,
    val escalation: String? = null,
    val flags: List<String> = emptyList(),
    val decision: String = "",
) {
    fun toJson() = JSONObject()
        .put("id", id).put("risk_score", score).put("risk_band", band.name.lowercase())
        .put("confidence", if (lowConfidence) "low" else "normal")
        .put("signals", JSONArray(signals.map {
            JSONObject().put("name", it.name).put("effect", if (it.raises) "raises" else "lowers").put("weight", it.weight)
        }))
        .put("unusual_inputs", JSONArray(unusualInputs))
        .put("model_version", modelVersion).put("assessed_at", assessedAt)
        .put("handle", handle ?: JSONObject.NULL).put("note", note)
        .put("model_band", modelBand.name.lowercase()).put("escalation", escalation ?: JSONObject.NULL)
        .put("reported_flags", JSONArray(flags.map { JSONObject().put("label", it) })).put("decision", decision)

    companion object {
        /** Parses the API response (and our own stored copy, which uses the same shape). */
        fun fromJson(j: JSONObject, id: String = j.optString("id").ifEmpty { j.getString("request_id") }) = Assessment(
            id = id,
            score = j.getDouble("risk_score"),
            band = Band.parse(j.getString("risk_band")),
            lowConfidence = j.optString("confidence") == "low",
            signals = j.getJSONArray("signals").let { a ->
                List(a.length()) { i ->
                    a.getJSONObject(i).let { Signal(it.getString("name"), it.getString("effect") == "raises", it.getDouble("weight")) }
                }
            },
            unusualInputs = j.optJSONArray("unusual_inputs")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
            modelVersion = j.getString("model_version"),
            assessedAt = j.getString("assessed_at"),
            handle = if (j.isNull("handle")) null else j.optString("handle").ifEmpty { null },
            note = j.optString("note"),
            modelBand = Band.parse(j.optString("model_band").ifEmpty { j.getString("risk_band") }),
            escalation = if (j.isNull("escalation")) null else j.optString("escalation").ifEmpty { null },
            flags = j.optJSONArray("reported_flags")?.let { a -> List(a.length()) { a.getJSONObject(it).getString("label") } } ?: emptyList(),
            decision = j.optString("decision"),
        )
    }
}
