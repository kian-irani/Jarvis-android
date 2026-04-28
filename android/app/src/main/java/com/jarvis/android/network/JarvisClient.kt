package com.jarvis.android.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HealthResponse(val nodes_online: Int, val nodes_total: Int, val cpu: Double, val ram_free_gb: Double)

object JarvisClient {
    private const val BRAIN = "http://212.87.199.62:8000"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getHealth(): HealthResponse = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BRAIN/health").get().build()
        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body!!.string())
        HealthResponse(
            nodes_online = json.optInt("nodes_online", 0),
            nodes_total = json.optInt("nodes_total", 0),
            cpu = json.optDouble("cpu", 0.0),
            ram_free_gb = json.optDouble("ram_free_gb", 0.0)
        )
    }

    suspend fun chat(message: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("message", message).toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$BRAIN/chat").post(body).build()
        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body!!.string())
        json.optString("response", "خطا در دریافت پاسخ")
    }

    suspend fun getNodes(): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$BRAIN/nodes").get().build()
        val resp = client.newCall(req).execute()
        resp.body!!.string()
    }
}
