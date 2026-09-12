package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GroundingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiResponse(
    val text: String,
    val isGrounded: Boolean,
    val searchQueries: List<String>,
    val sources: List<GroundingSource>
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val MYRA_SYSTEM_INSTRUCTION = """
You are MYRA, a highly advanced Personal AI Assistant powered by the Gemini Live API Framework.
You have direct authority to execute autonomous tool calls, write code, and fetch real-time global knowledge.
Your interaction style is natural, human-like, converting fluently between Hindi and English (Hinglish).

Response Delivery Protocol:
1. Keep responses optimized for ultra-fast, high-density audio playback.
2. Group news and live search results into rapid, clear modules:
   - "ओके बॉस, रिसर्च कर ली है मैंने..."
   - "आज की बड़ी खबरों में..."
   - "बिजनेस और टेक न्यूज़ में..."
   - "स्पोर्ट्स में..."
3. Avoid text blocks. Use brief, punchy sentences that sound lively and natural when spoken aloud.
4. Google Search Grounding: You have an active 'Google Search' tool. Seamlessly merge live data into spoken-friendly format. NEVER announce tool execution (do NOT say "Calling API now" or "Searching Google").
5. Advanced Logical Processing: Solve multi-step problems (math, coding scripts, network DNS adjustments) systematically. Walk through solutions step-by-step ONLY when asked, otherwise deliver the end action or code cleanly and instantly.
"""

    suspend fun generateContent(
        prompt: String,
        apiKeyOverride: String? = null,
        modelName: String = "gemini-2.5-flash",
        enableSearchGrounding: Boolean = true,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<GeminiResponse> = withContext(Dispatchers.IO) {
        val resolvedKey = apiKeyOverride?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
            ?: ""

        if (resolvedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is not configured. Please add your API key in Settings or the Secrets panel.")
            )
        }

        try {
            // First attempt with requested configuration (including Google Search if enabled)
            val result = executeRequest(
                prompt = prompt,
                apiKey = resolvedKey,
                modelName = modelName,
                withSearch = enableSearchGrounding,
                history = conversationHistory
            )
            Result.success(result)
        } catch (e: Exception) {
            Log.w(TAG, "First attempt failed with search=$enableSearchGrounding: ${e.message}")
            if (enableSearchGrounding) {
                // Retry without search tools in case of grounding error or quota issue
                try {
                    val fallback = executeRequest(
                        prompt = prompt,
                        apiKey = resolvedKey,
                        modelName = modelName,
                        withSearch = false,
                        history = conversationHistory
                    )
                    Result.success(fallback)
                } catch (fallbackError: Exception) {
                    Result.failure(fallbackError)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    private fun executeRequest(
        prompt: String,
        apiKey: String,
        modelName: String,
        withSearch: Boolean,
        history: List<Pair<String, String>>
    ): GeminiResponse {
        val rootJson = JSONObject()

        // Contents
        val contentsArray = JSONArray()

        // Append recent history if any (up to last 6 turns)
        history.takeLast(6).forEach { (role, msgText) ->
            val historyContent = JSONObject()
            historyContent.put("role", if (role == "user") "user" else "model")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", msgText)
            parts.put(part)
            historyContent.put("parts", parts)
            contentsArray.put(historyContent)
        }

        // Current prompt
        val currentContent = JSONObject()
        currentContent.put("role", "user")
        val currentParts = JSONArray()
        val currentPart = JSONObject()
        currentPart.put("text", prompt)
        currentParts.put(currentPart)
        currentContent.put("parts", currentParts)
        contentsArray.put(currentContent)

        rootJson.put("contents", contentsArray)

        // System Instruction
        val systemInstruction = JSONObject()
        val sysParts = JSONArray()
        val sysPart = JSONObject()
        sysPart.put("text", MYRA_SYSTEM_INSTRUCTION)
        sysParts.put(sysPart)
        systemInstruction.put("parts", sysParts)
        rootJson.put("systemInstruction", systemInstruction)

        // Tools (Google Search Grounding)
        if (withSearch) {
            val toolsArray = JSONArray()
            val searchTool = JSONObject()
            searchTool.put("googleSearch", JSONObject())
            toolsArray.put(searchTool)
            rootJson.put("tools", toolsArray)
        }

        val url = "$BASE_URL$modelName:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = rootJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini")

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errorJson = JSONObject(responseBody).optJSONObject("error")
                errorJson?.optString("message") ?: "HTTP ${response.code}: $responseBody"
            } catch (_: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            throw IllegalStateException(errorMsg)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
            ?: throw IllegalStateException("No candidate returned by Gemini")

        val contentObj = firstCandidate.optJSONObject("content")
        val partsArr = contentObj?.optJSONArray("parts")

        val textBuilder = StringBuilder()
        if (partsArr != null) {
            for (i in 0 until partsArr.length()) {
                val partObj = partsArr.optJSONObject(i)
                val text = partObj?.optString("text")
                if (!text.isNullOrBlank()) {
                    textBuilder.append(text)
                }
            }
        }

        val fullText = textBuilder.toString().ifBlank { "कोई उत्तर प्राप्त नहीं हुआ।" }

        // Grounding metadata
        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
        val searchQueries = mutableListOf<String>()
        val sources = mutableListOf<GroundingSource>()
        var isGrounded = false

        if (groundingMetadata != null) {
            val webQueries = groundingMetadata.optJSONArray("webSearchQueries")
            if (webQueries != null) {
                for (i in 0 until webQueries.length()) {
                    searchQueries.add(webQueries.optString(i))
                }
            }

            val chunks = groundingMetadata.optJSONArray("groundingChunks")
            if (chunks != null) {
                for (i in 0 until chunks.length()) {
                    val chunkObj = chunks.optJSONObject(i)
                    val web = chunkObj?.optJSONObject("web")
                    if (web != null) {
                        val title = web.optString("title", "वेब स्रोत")
                        val uri = web.optString("uri", "")
                        if (uri.isNotBlank()) {
                            sources.add(GroundingSource(title = title, url = uri))
                        }
                    }
                }
            }

            if (searchQueries.isNotEmpty() || sources.isNotEmpty()) {
                isGrounded = true
            }
        }

        return GeminiResponse(
            text = fullText,
            isGrounded = isGrounded,
            searchQueries = searchQueries,
            sources = sources.distinctBy { it.url }
        )
    }
}
