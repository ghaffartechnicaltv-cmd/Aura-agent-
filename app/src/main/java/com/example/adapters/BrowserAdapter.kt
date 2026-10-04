package com.example.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.security.PromptInjectionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebSearchResult(
    val title: String,
    val snippet: String,
    val sourceUrl: String
)

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    var url: String,
    var title: String,
    var contentSummary: String
)

object BrowserAdapter {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val activeTabs = mutableListOf<BrowserTab>()
    private var currentTabIndex = 0

    fun openBrowser(context: Context, url: String): Pair<Boolean, String> {
        return try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val newTab = BrowserTab(url = formattedUrl, title = formattedUrl, contentSummary = "Page loaded")
            activeTabs.add(newTab)
            currentTabIndex = activeTabs.size - 1

            Pair(true, "Browser launched with URL: $formattedUrl")
        } catch (e: Exception) {
            Pair(false, "Failed to launch browser: ${e.message}")
        }
    }

    suspend fun performWebSearch(query: String): List<WebSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<WebSearchResult>()

        // 1. Check if weather search
        if (query.lowercase().contains("weather") || query.lowercase().contains("mausam")) {
            val weatherResult = fetchRealWeather(query)
            if (weatherResult != null) {
                results.add(weatherResult)
                return@withContext results
            }
        }

        // 2. Query DuckDuckGo Instant Answer API (Real public search API)
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AURA-Personal-Assistant/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", query)
                val abstractUrl = json.optString("AbstractURL", "https://duckduckgo.com/?q=$encodedQuery")

                if (abstractText.isNotEmpty()) {
                    val (sanitized, _) = PromptInjectionGuard.sanitizeUntrustedContent(abstractText)
                    results.add(
                        WebSearchResult(
                            title = heading,
                            snippet = sanitized,
                            sourceUrl = abstractUrl
                        )
                    )
                }

                // Check Related Topics
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null) {
                    for (i in 0 until minOf(relatedTopics.length(), 3)) {
                        val topic = relatedTopics.optJSONObject(i)
                        val text = topic?.optString("Text", "") ?: ""
                        val firstUrl = topic?.optString("FirstURL", "") ?: ""
                        if (text.isNotEmpty()) {
                            val (sanitized, _) = PromptInjectionGuard.sanitizeUntrustedContent(text)
                            results.add(
                                WebSearchResult(
                                    title = "$heading (Related)",
                                    snippet = sanitized,
                                    sourceUrl = firstUrl
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log and try Wikipedia fallback
        }

        // 3. Query Wikipedia REST API if results still empty
        if (results.isEmpty()) {
            try {
                val cleanTerm = query.replace(Regex("(?i)kholo|search|karo|batao|find|what is|who is"), "").trim()
                val encodedTerm = URLEncoder.encode(cleanTerm, "UTF-8")
                val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTerm"
                val wikiReq = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "AURA-AI-Assistant/1.0")
                    .build()

                val wikiResp = httpClient.newCall(wikiReq).execute()
                if (wikiResp.isSuccessful) {
                    val body = wikiResp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val title = json.optString("title", query)
                    val extract = json.optString("extract", "")
                    val pageUrl = json.optJSONObject("content_urls")
                        ?.optJSONObject("desktop")?.optString("page", "https://en.wikipedia.org") ?: "https://en.wikipedia.org"

                    if (extract.isNotEmpty()) {
                        val (sanitized, _) = PromptInjectionGuard.sanitizeUntrustedContent(extract)
                        results.add(
                            WebSearchResult(
                                title = title,
                                snippet = sanitized,
                                sourceUrl = pageUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Return verified fallback search link
            }
        }

        // Default authoritative link if zero results
        if (results.isEmpty()) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            results.add(
                WebSearchResult(
                    title = "Web Search: $query",
                    snippet = "Live search query prepared for verified sources.",
                    sourceUrl = "https://www.google.com/search?q=$encodedQuery"
                )
            )
        }

        results
    }

    private suspend fun fetchRealWeather(query: String): WebSearchResult? = withContext(Dispatchers.IO) {
        try {
            // Determine coordinates for known cities or capital
            var lat = 33.6844 // Islamabad, Pakistan default
            var lon = 73.0479
            var cityName = "Islamabad, Pakistan"

            val q = query.lowercase()
            if (q.contains("karachi")) {
                lat = 24.8607; lon = 67.0011; cityName = "Karachi, Pakistan"
            } else if (q.contains("lahore")) {
                lat = 31.5204; lon = 74.3587; cityName = "Lahore, Pakistan"
            } else if (q.contains("delhi")) {
                lat = 28.6139; lon = 77.2090; cityName = "New Delhi, India"
            } else if (q.contains("london")) {
                lat = 51.5074; lon = -0.1278; cityName = "London, UK"
            } else if (q.contains("new york")) {
                lat = 40.7128; lon = -74.0060; cityName = "New York, USA"
            }

            val meteoUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val request = Request.Builder().url(meteoUrl).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val current = json.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val wind = current.getDouble("windspeed")
                val code = current.getInt("weathercode")

                val condition = when (code) {
                    0 -> "Clear sky"
                    1, 2, 3 -> "Mainly clear to partly cloudy"
                    45, 48 -> "Foggy"
                    51, 53, 55 -> "Drizzle"
                    61, 63, 65 -> "Rain"
                    71, 73, 75 -> "Snow"
                    80, 81, 82 -> "Rain showers"
                    95 -> "Thunderstorm"
                    else -> "Fair"
                }

                val snippet = "Real-time weather for $cityName: ${temp}°C, $condition. Wind speed: ${wind} km/h."
                return@withContext WebSearchResult(
                    title = "Current Weather: $cityName",
                    snippet = snippet,
                    sourceUrl = "https://open-meteo.com"
                )
            }
        } catch (e: Exception) {
            // Weather service error
        }
        null
    }

    suspend fun readPageContent(url: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AURA-Research-Bot/1.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val rawHtml = response.body?.string() ?: ""
                // Strip scripts and tags
                val textOnly = rawHtml.replace(Regex("<[^>]*>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .take(1500)
                return@withContext PromptInjectionGuard.sanitizeUntrustedContent(textOnly)
            }
        } catch (e: Exception) {
            // Failed
        }
        Pair("Unable to read webpage content directly from $url. Please verify your network connection.", false)
    }

    fun getTabs(): List<BrowserTab> = activeTabs.toList()

    fun closeTab(tabId: String): Boolean {
        return activeTabs.removeAll { it.id == tabId }
    }
}
