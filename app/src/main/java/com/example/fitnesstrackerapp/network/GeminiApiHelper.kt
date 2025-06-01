package com.example.fitnesstrackerapp.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiApiHelper {

    private val client = OkHttpClient()

    suspend fun sendFitnessData(steps: Int, calories: Double, points: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("steps", steps)
                    put("calories", calories)
                    put("points", points)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://api.gemini.example.com/workout-suggestions")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }
                    return@withContext response.body?.string()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}
