package com.teja.gemmmobile.tools

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Wikipedia Deep Lookup tool.
 *
 * Retrieves encyclopedic definitions, historical timelines, and biographies from English and Telugu Wikipedia.
 */
class WikipediaTool : GemmaTool {

    override val name: String = "query_wikipedia"

    override val description: String =
        "Fetches verified encyclopedic summaries, history, biographies, and scientific explanations from Wikipedia."

    override val parametersJsonSchema: String = """
        {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "Topic, person, event, or concept to look up on Wikipedia."
            },
            "language": {
              "type": "string",
              "enum": ["en", "te"],
              "description": "Language edition of Wikipedia ('en' for English, 'te' for Telugu). Default is 'en'."
            }
          },
          "required": ["query"]
        }
    """.trimIndent()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val rawQuery = arguments["query"]?.toString()?.trim()
            ?: return@withContext ToolResult.failure("Missing 'query' parameter")
        val lang = arguments["language"]?.toString()?.lowercase()?.takeIf { it == "te" } ?: "en"

        try {
            // 1. Try direct page summary endpoint
            val encodedQuery = Uri.encode(rawQuery.replace(" ", "_"))
            val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedQuery"
            val summaryJson = fetchHttpString(summaryUrl)

            if (summaryJson != null) {
                val json = JSONObject(summaryJson)
                val type = json.optString("type", "")
                if (type != "disambiguation") {
                    val title = json.optString("title", rawQuery)
                    val description = json.optString("description", "")
                    val extract = json.optString("extract", "")
                    val pageUrl = json.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page", "")

                    if (extract.isNotBlank()) {
                        val formatted = buildString {
                            appendLine("### 📚 $title (Wikipedia)")
                            if (description.isNotBlank()) appendLine("*$description*")
                            appendLine()
                            appendLine(extract)
                            if (!pageUrl.isNullOrBlank()) {
                                appendLine()
                                appendLine("🔗 [Read full article on Wikipedia]($pageUrl)")
                            }
                        }
                        return@withContext ToolResult.success(formatted)
                    }
                }
            }

            // 2. Fallback: Search endpoint
            val searchUrl = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=${Uri.encode(rawQuery)}&format=json&utf8=1"
            val searchJson = fetchHttpString(searchUrl)
            if (searchJson != null) {
                val json = JSONObject(searchJson)
                val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                if (searchArr != null && searchArr.length() > 0) {
                    val firstHit = searchArr.getJSONObject(0)
                    val title = firstHit.optString("title", "")
                    val snippet = firstHit.optString("snippet", "").replace(Regex("<[^>]*>"), "")
                    val pageUrl = "https://$lang.wikipedia.org/wiki/${Uri.encode(title.replace(" ", "_"))}"

                    val formatted = buildString {
                        appendLine("### 📚 $title (Wikipedia Search)")
                        appendLine()
                        appendLine(snippet)
                        appendLine()
                        appendLine("🔗 [Read article on Wikipedia]($pageUrl)")
                    }
                    return@withContext ToolResult.success(formatted)
                }
            }

            ToolResult.failure("No Wikipedia articles found for '$rawQuery'.")
        } catch (e: Exception) {
            Log.e("WikipediaTool", "Error fetching Wikipedia summary", e)
            ToolResult.failure("Failed to fetch Wikipedia data: ${e.message}")
        }
    }

    private fun fetchHttpString(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "GemmaMobile/1.0 (Android on-device AI)")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun isWikipediaRequest(prompt: String): Boolean {
            val p = prompt.lowercase().trim()
            val keywords = listOf("wikipedia", "wiki lo chudu", "wiki gurinchi", "lookup wikipedia", "wiki search")
            return keywords.any { p.contains(it) }
        }
    }
}
