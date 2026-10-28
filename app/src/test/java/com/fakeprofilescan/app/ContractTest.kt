package com.fakeprofilescan.app

import com.fakeprofilescan.app.data.ApiResult
import com.fakeprofilescan.app.data.AssessmentApi
import com.fakeprofilescan.app.data.Band
import com.fakeprofilescan.app.data.FormState
import com.fakeprofilescan.app.data.HistoryStore
import com.fakeprofilescan.app.data.parseCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.Instant

class ContractTest {
    private val ok = FormState(username = "anna_travels", fullName = "Anna Lee", bio = "hi", posts = "10", followers = "100", following = "50")

    @Test fun validFormHasNoErrors() = assertTrue(ok.errors().isEmpty())

    @Test fun emptyNumbersAreRejectedNotDefaulted() {
        val e = ok.copy(posts = "", followers = "x").errors()
        assertEquals(setOf("posts", "followers"), e.keys)
    }

    @Test fun usernameRules() {
        assertTrue("username" in ok.copy(username = "has space").errors())
        assertTrue("username" in ok.copy(username = "").errors())
        assertTrue("following" in ok.copy(following = "100001").errors())
    }

    @Test fun requestSendsBioLengthNotText() {
        val j = ok.copy(bio = "secret bio").toRequestJson()
        assertEquals(10, j.getInt("bio_length"))
        assertTrue(!j.has("bio"))
    }

    private val body = """{"risk_score":0.82,"risk_band":"elevated","confidence":"low","signals":[{"name":"Number of posts","effect":"raises","weight":1.2}],
        "unusual_inputs":[],"model_version":"m-1","assessed_at":"2026-01-01T00:00:00+00:00","request_id":"abc"}"""

    @Test fun parsesSuccess() {
        val r = AssessmentApi.parse(200, body) as ApiResult.Ok
        assertEquals(Band.ELEVATED, r.assessment.band)
        assertTrue(r.assessment.lowConfidence)
        assertEquals("abc", r.assessment.id)
    }

    @Test fun mapsValidationErrorsToFormFields() {
        val r = AssessmentApi.parse(422, """{"error":{"code":"validation_failed","message":"x","fields":{"full_name":"too long","posts":"bad"}}}""") as ApiResult.Invalid
        assertEquals(setOf("fullName", "posts"), r.fields.keys)
    }

    @Test fun serverErrorsAndGarbageAreSafe() {
        assertTrue(AssessmentApi.parse(500, """{"error":{"message":"boom"}}""") is ApiResult.Server)
        assertTrue(AssessmentApi.parse(200, "<html>") is ApiResult.Server)
    }

    @Test fun retentionPrunesExpiredItems() {
        val dir = Files.createTempDirectory("h").toFile()
        val store = HistoryStore(dir)
        val a = (AssessmentApi.parse(200, body) as ApiResult.Ok).assessment
        store.write(listOf(a.copy(id = "old", assessedAt = "2020-01-01T00:00:00Z"), a.copy(id = "new", assessedAt = Instant.now().toString())))
        assertEquals(listOf("new"), store.load(30).map { it.id })
        assertEquals(2, HistoryStore(dir).also { it.write(listOf(a, a.copy(id = "b"))) }.load(0).size)
    }

    @Test fun countsAcceptProfileShorthand() {
        assertEquals(850L, parseCount("850")); assertEquals(12_400L, parseCount("12,400"))
        assertEquals(1_200L, parseCount("1.2K")); assertEquals(3_000_000L, parseCount("3m"))
        assertEquals(null, parseCount("1e5")); assertEquals(null, parseCount("5D")); assertEquals(null, parseCount("-3"))
    }

    @Test fun flagsAreSentToServer() {
        val j = ok.copy(flags = setOf("asks_money"), followers = "1.2K").toRequestJson()
        assertEquals("asks_money", j.getJSONArray("observed_flags").getString(0))
        assertEquals(1200L, j.getLong("followers"))
    }

    @Test fun escalationAndFlagsSurviveStorage() {
        val body = """{"risk_score":0.4,"risk_band":"elevated","model_band":"review","escalation":"Raised","reported_flags":[{"id":"urgency","label":"Pressure"}],
            "confidence":"normal","signals":[],"model_version":"m","assessed_at":"2026-01-01T00:00:00Z","request_id":"x"}"""
        val a = (AssessmentApi.parse(200, body) as ApiResult.Ok).assessment
        val b = com.fakeprofilescan.app.data.Assessment.fromJson(a.copy(decision = "Blocked").toJson())
        assertEquals(Band.REVIEW, b.modelBand); assertEquals(listOf("Pressure"), b.flags); assertEquals("Blocked", b.decision)
    }
}
