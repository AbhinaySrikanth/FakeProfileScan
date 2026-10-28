package com.fakeprofilescan.app.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface ApiResult {
    data class Ok(val assessment: Assessment) : ApiResult
    data class Invalid(val fields: Map<String, String>) : ApiResult
    data object NotConfigured : ApiResult
    data object Offline : ApiResult
    data object Timeout : ApiResult
    data class Server(val message: String) : ApiResult
}

class AssessmentApi(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build(),
) {
    suspend fun assess(baseUrl: String, form: FormState): ApiResult {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return ApiResult.NotConfigured
        val req = try {
            Request.Builder().url("$base/api/v1/assessments")
                .post(form.toRequestJson().toString().toRequestBody("application/json".toMediaType())).build()
        } catch (e: IllegalArgumentException) {
            return ApiResult.NotConfigured
        }
        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(req)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume(if (e is SocketTimeoutException || e.message == "timeout") ApiResult.Timeout else ApiResult.Offline)
                }

                override fun onResponse(call: Call, response: Response) {
                    val r = response.use { parse(it.code, it.body?.string().orEmpty()) }
                    if (cont.isActive) cont.resume(r)
                }
            })
        }
    }

    companion object {
        /** Maps an HTTP status + body to a result; separated for unit testing. */
        fun parse(code: Int, body: String): ApiResult = try {
            val j = JSONObject(body)
            when (code) {
                200 -> ApiResult.Ok(Assessment.fromJson(j))
                422 -> {
                    val f = j.getJSONObject("error").optJSONObject("fields")
                    ApiResult.Invalid(f?.keys()?.asSequence()?.associate { (FormState.serverField[it] ?: it) to f.getString(it) } ?: emptyMap())
                }
                else -> ApiResult.Server(j.optJSONObject("error")?.optString("message") ?: "Server error ($code).")
            }
        } catch (e: Exception) {
            ApiResult.Server("Unexpected response from server ($code).")
        }
    }
}
