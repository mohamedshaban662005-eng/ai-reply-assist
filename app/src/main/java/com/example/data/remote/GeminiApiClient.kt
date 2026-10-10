package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class GeminiMessage(
        val role: String, // "user" or "model"
        val text: String
    )

    data class GenerationResult(
        val success: Boolean,
        val text: String,
        val error: String? = null
    )

    /**
     * Generates a smart response using Gemini API with conversation context and structured instructions.
     */
    suspend fun generateReply(
        apiKey: String,
        model: String = "gemini-3.5-flash",
        conversationHistory: List<GeminiMessage>,
        systemInstruction: String,
        temperature: Float = 0.7f
    ): GenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GenerationResult(
                success = false,
                text = "",
                error = "Gemini API key is not configured. Please enter your API key in Settings."
            )
        }

        try {
            val url = "$BASE_URL$model:generateContent?key=$apiKey"

            // Construct contents array
            val contentsList = conversationHistory.map { msg ->
                mapOf(
                    "role" to msg.role,
                    "parts" to listOf(mapOf("text" to msg.text))
                )
            }

            val requestPayload = mutableMapOf<String, Any>(
                "contents" to contentsList,
                "generationConfig" to mapOf(
                    "temperature" to temperature,
                    "maxOutputTokens" to 300
                )
            )

            if (systemInstruction.isNotBlank()) {
                requestPayload["systemInstruction"] = mapOf(
                    "parts" to listOf(mapOf("text" to systemInstruction))
                )
            }

            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonString = jsonAdapter.toJson(requestPayload)

            val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, "Gemini API error: $errorMsg")
                return@withContext GenerationResult(
                    success = false,
                    text = "",
                    error = errorMsg
                )
            }

            val extractedText = extractCandidateText(responseBody)
            if (extractedText.isNullOrBlank()) {
                return@withContext GenerationResult(
                    success = false,
                    text = "",
                    error = "Received empty response from Gemini model"
                )
            }

            GenerationResult(
                success = true,
                text = extractedText.trim()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            GenerationResult(
                success = false,
                text = "",
                error = e.localizedMessage ?: "Network or API connection error"
            )
        }
    }

    /**
     * Validates API key with a minimal query.
     */
    suspend fun validateApiKey(apiKey: String, model: String = "gemini-3.5-flash"): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Pair(false, "API Key is empty")
            }
            try {
                val testPrompt = listOf(GeminiMessage(role = "user", text = "Ping"))
                val result = generateReply(
                    apiKey = apiKey,
                    model = model,
                    conversationHistory = testPrompt,
                    systemInstruction = "Reply with 'Pong'",
                    temperature = 0.1f
                )
                if (result.success) {
                    Pair(true, "API Key verified successfully!")
                } else {
                    Pair(false, result.error ?: "Verification failed")
                }
            } catch (e: Exception) {
                Pair(false, e.localizedMessage ?: "Connection error")
            }
        }

    private fun extractCandidateText(jsonString: String): String? {
        return try {
            val root = moshi.adapter(Map::class.java).fromJson(jsonString) as? Map<*, *>
            val candidates = root?.get("candidates") as? List<*>
            val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCandidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            firstPart?.get("text") as? String
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON", e)
            null
        }
    }

    private fun parseErrorMessage(jsonString: String): String? {
        return try {
            val root = moshi.adapter(Map::class.java).fromJson(jsonString) as? Map<*, *>
            val error = root?.get("error") as? Map<*, *>
            val message = error?.get("message") as? String
            message
        } catch (e: Exception) {
            null
        }
    }
}
