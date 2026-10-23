package com.teja.gemmmobile.search

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import com.google.gson.JsonParser

private const val TAG = "SearchManager"

/**
 * Distinct failure and success states for web search.
 */
enum class SearchStatus {
    SUCCESS,
    NO_RESULTS,
    NETWORK_ERROR,
    TIMEOUT,
    PROVIDER_ERROR,
    FETCH_ERROR
}

/**
 * Provenance of search content.
 */
enum class ContentSourceType {
    SEARCH_SNIPPET,
    FETCHED_WEBPAGE,
    IMAGE_SEARCH
}

/**
 * Structured search response with explicit status code and debug message.
 */
data class SearchResponse(
    val status: SearchStatus,
    val results: List<EnrichedSearchResult> = emptyList(),
    val errorMessage: String? = null,
    val images: List<SearchImage> = emptyList()
)

/**
 * Enriched search result containing title, source URL, search snippet,
 * extracted public page content, and provenance metadata.
 */
data class EnrichedSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val pageContent: String = "",
    val fetchSucceeded: Boolean = false,
    val imageUrl: String? = null,
    val sourceType: ContentSourceType = if (fetchSucceeded) ContentSourceType.FETCHED_WEBPAGE else ContentSourceType.SEARCH_SNIPPET
) {
    /**
     * Converts to lightweight UI SearchResult for chat cards and profile avatars.
     */
    fun toSearchResult(): SearchResult = SearchResult(
        title = title,
        url = url,
        snippet = snippet,
        imageUrl = imageUrl
    )
}

/**
 * Central orchestrator for on-device web search.
 * Uses Serper.dev (1,900+ queries) and SerpApi (230+ queries) as primary providers
 * for 100% genuine products, live INR prices, packshot images, and direct store URLs.
 * Seamlessly falls back to on-device zero-cost DuckDuckGo Lite & Wikipedia if API quotas run out.
 * Includes bounded, freshness-aware caching to conserve API credits.
 */
open class SearchManager(
    private val webSearchClient: WebSearchClient = WebSearchClient(),
    private val webPageFetcher: WebPageFetcher = WebPageFetcher(),
    private val serpApiClient: SerpApiClient = SerpApiClient(),
    private val serperClient: SerperClient = SerperClient()
) {

    companion object {
        private val TRACKING_PARAMS = setOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "ref", "ved", "usqp", "source", "srsltid"
        )
        private const val MAX_CACHE_ENTRIES = 64
    }

    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, SearchResponse>>()

    /**
     * Sanitizes user input queries by stripping filler conversational commands.
     */
    fun sanitizeQuery(raw: String): String = webSearchClient.sanitizeQuery(raw)
    fun isGenericQuery(query: String): Boolean = webSearchClient.isGenericQuery(query)

    /**
     * Normalizes a URL by stripping tracking parameters, fragments, and standardizing host case.
     */
    fun normalizeUrl(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl.trim())
            val scheme = (uri.scheme ?: "https").lowercase()
            val host = (uri.host ?: "").lowercase()
            val rawPath = uri.path ?: ""
            val cleanPath = if (rawPath.length > 1 && rawPath.endsWith("/")) rawPath.removeSuffix("/") else rawPath

            // Strip tracking query parameters
            val cleanQuery = uri.query?.split("&")
                ?.filter { param ->
                    val key = param.substringBefore("=").lowercase()
                    key !in TRACKING_PARAMS
                }
                ?.joinToString("&")
                ?.ifBlank { null }

            val portPart = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val queryPart = if (!cleanQuery.isNullOrBlank()) "?$cleanQuery" else ""

            "$scheme://$host$portPart$cleanPath$queryPart"
        } catch (_: Exception) {
            rawUrl.trim().removeSuffix("/")
        }
    }

    /**
     * Extracts normalized domain host from a URL string for domain diversity checking.
     */
    fun extractDomain(url: String): String {
        return try {
            URI(url).host?.lowercase()?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Deduplicates and balances search results to maximize domain diversity.
     */
    fun deduplicateAndRank(results: List<SearchResult>, maxResults: Int): List<SearchResult> {
        val seenUrls = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()
        val domainCounts = mutableMapOf<String, Int>()

        // Check if a verified direct LinkedIn profile exists
        val hasDirectLinkedInProfile = results.any { it.url.lowercase().contains("linkedin.com/in/") }

        val primaryCandidates = mutableListOf<SearchResult>()
        val overflowCandidates = mutableListOf<SearchResult>()

        for (res in results) {
            val normUrl = normalizeUrl(res.url)
            val normTitle = res.title.trim().lowercase()
            val lowerUrl = normUrl.lowercase()

            if (normUrl.isBlank() || !webPageFetcher.isSafeUrl(normUrl)) continue
            if (normTitle.isBlank()) continue

            // If a direct LinkedIn profile exists, drop LinkedIn posts or directories
            if (hasDirectLinkedInProfile && (lowerUrl.contains("linkedin.com/posts/") || lowerUrl.contains("linkedin.com/pub/dir/") || lowerUrl.contains("linkedin.com/pulse/"))) {
                continue
            }

            // Deduplicate by URL and exact title
            if (!seenUrls.add(normUrl)) continue
            if (!seenTitles.add(normTitle)) continue

            val domain = extractDomain(normUrl)
            val isLinkedInProfile = lowerUrl.contains("linkedin.com/in/")
            val maxAllowedForDomain = if (isLinkedInProfile) 3 else 2
            val count = domainCounts.getOrDefault(domain, 0)

            // Favor domain diversity: allow max 2 results from the same domain (or 3 for distinct LinkedIn profiles) in primary selection
            if (domain.isBlank() || count < maxAllowedForDomain) {
                domainCounts[domain] = count + 1
                primaryCandidates.add(res.copy(url = normUrl))
            } else {
                overflowCandidates.add(res.copy(url = normUrl))
            }

            if (primaryCandidates.size >= maxResults) break
        }

        return (primaryCandidates + overflowCandidates).take(maxResults)
    }

    /**
     * Orchestrates web search with explicit status reporting.
     */
    open suspend fun searchAndReadWithStatus(
        query: String,
        maxResults: Int = SearchConfig.DEFAULT_MAX_SEARCH_RESULTS,
        isProduct: Boolean = false,
        onProgress: suspend (String) -> Unit = {}
    ): SearchResponse = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        if (cleanQuery.isBlank()) {
            return@withContext SearchResponse(SearchStatus.NO_RESULTS, emptyList(), "Empty query")
        }

        Log.d(TAG, "[$TAG] Executing searchAndRead for query: \"$cleanQuery\" (isProduct=$isProduct)")

        // 0. Cache according to query freshness and requested result count
        val cacheKey = "$isProduct:$maxResults:${cleanQuery.lowercase()}"
        val cacheTtl = SearchPolicy.cacheTtlMillis(cleanQuery, isProduct)
        val cached = searchCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first in 0 until cacheTtl) {
            Log.d(TAG, "[$TAG] Returning cached search response for: \"$cleanQuery\"")
            return@withContext cached.second
        }

        onProgress("Searching the web")
        // 1. Primary Engine: Serper.dev (Google Search & Google Shopping in India)
        var searchResultCandidate: SerpApiResult? = null
        if (serperClient.isAvailable()) {
            try {
                searchResultCandidate = serperClient.search(cleanQuery, maxResults = 10, isProduct = isProduct)
                if (searchResultCandidate != null && searchResultCandidate.results.isNotEmpty()) {
                    Log.d(TAG, "[$TAG] Serper.dev returned ${searchResultCandidate.results.size} results and ${searchResultCandidate.images.size} images")
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "[$TAG] Serper.dev search failed: ${t.localizedMessage}")
            }
        }

        // 2. Secondary Engine: SerpApi (Google Search & Shopping)
        if ((searchResultCandidate == null || searchResultCandidate.results.isEmpty()) && serpApiClient.isAvailable()) {
            try {
                searchResultCandidate = serpApiClient.search(cleanQuery, maxResults = 10, isProduct = isProduct)
                if (searchResultCandidate != null && searchResultCandidate.results.isNotEmpty()) {
                    Log.d(TAG, "[$TAG] SerpApi returned ${searchResultCandidate.results.size} results and ${searchResultCandidate.images.size} images")
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "[$TAG] SerpApi search failed: ${t.localizedMessage}")
            }
        }

        // 3. Fallback: On-device WebSearchClient (DuckDuckGo Lite + Bing + Wikipedia)
        val rawResults: List<SearchResult>
        val rawImages: List<SearchImage>
        if (searchResultCandidate != null && searchResultCandidate.results.isNotEmpty()) {
            rawResults = searchResultCandidate.results
            rawImages = searchResultCandidate.images
        } else {
            try {
                rawResults = webSearchClient.search(cleanQuery, maxResults = 12)
                rawImages = emptyList()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "[$TAG] Web search failed: ${t.localizedMessage}")
                val isTimeout = t is java.net.SocketTimeoutException
                val status = if (isTimeout) SearchStatus.TIMEOUT else SearchStatus.PROVIDER_ERROR
                return@withContext SearchResponse(status, emptyList(), t.localizedMessage ?: "Provider error")
            }
        }

        if (rawResults.isEmpty()) {
            return@withContext SearchResponse(SearchStatus.NO_RESULTS, emptyList())
        }

        // 4. Rank and deduplicate to ensure relevance & domain diversity
        val rankedCandidates = webSearchClient.rankByRelevance(rawResults, cleanQuery, maxResults = 12, isProduct = isProduct)
        val rankedResults = deduplicateAndRank(rankedCandidates, maxResults)
        if (rankedResults.isEmpty()) {
            return@withContext SearchResponse(SearchStatus.NO_RESULTS, emptyList())
        }

        // 5. Concurrently fetch top public pages with bounded semaphore concurrency
        // 5. Concurrently fetch top public pages with bounded semaphore concurrency
        val semaphore = Semaphore(SearchConfig.MAX_CONCURRENT_FETCHES)

        // Identify non-auth-walled public pages eligible for content scraping
        val eligibleIndicesToFetch = mutableSetOf<Int>()
        if (!isProduct) {
            for ((idx, res) in rankedResults.withIndex()) {
                val domain = extractDomain(res.url)
                val isAuthWalledDomain = domain.contains("linkedin") || domain.contains("facebook") ||
                    domain.contains("instagram") || domain.contains("twitter") || domain.contains("x.com") ||
                    domain.contains("youtube") || domain.contains("reddit") || domain.contains("tiktok")
                if (!isAuthWalledDomain && webPageFetcher.isSafeUrl(res.url)) {
                    eligibleIndicesToFetch.add(idx)
                    if (eligibleIndicesToFetch.size >= SearchConfig.MAX_PAGES_TO_FETCH) break
                }
            }
        }

        onProgress("Reading ${eligibleIndicesToFetch.size} pages")
        val enrichedList = coroutineScope {
            val deferredEnriched = rankedResults.mapIndexed { idx, res ->
                async {
                    var fetchedContent = ""
                    var fetchSuccess = false
                    var fetchedImage: String? = rawImages.firstOrNull { it.title == res.title }?.imageUrl

                    // Scrape public page content if eligible
                    if (idx in eligibleIndicesToFetch) {
                        try {
                            semaphore.withPermit {
                                val fetched = withTimeoutOrNull(SearchConfig.PER_PAGE_TIMEOUT_MS) {
                                    webPageFetcher.fetchPage(res.url)
                                }

                                if (fetched != null && fetched.success && fetched.content.isNotBlank() && !isAuthOrLoginText(fetched.content)) {
                                    fetchedContent = SearchPolicy.selectPassages(fetched.content, cleanQuery, 2400)
                                    fetchSuccess = true
                                    if (fetchedImage == null) fetchedImage = fetched.imageUrl
                                }
                            }
                        } catch (t: Throwable) {
                            if (t is CancellationException) throw t
                            Log.w(TAG, "[$TAG] Error fetching ${res.url}: ${t.localizedMessage}")
                        }
                    }

                    EnrichedSearchResult(
                        title = res.title,
                        url = res.url,
                        snippet = res.snippet,
                        pageContent = fetchedContent,
                        fetchSucceeded = fetchSuccess,
                        imageUrl = fetchedImage,
                        sourceType = if (fetchSuccess) ContentSourceType.FETCHED_WEBPAGE else ContentSourceType.SEARCH_SNIPPET
                    )
                }
            }

            deferredEnriched.awaitAll()
        }

        onProgress("Preparing answer from ${enrichedList.size} sources")
        val response = SearchResponse(
            status = SearchStatus.SUCCESS,
            results = enrichedList,
            images = rawImages
        )
        if (cacheTtl > 0) {
            synchronized(searchCache) {
                if (searchCache.size >= MAX_CACHE_ENTRIES) {
                    searchCache.entries.minByOrNull { it.value.first }?.let { searchCache.remove(it.key) }
                }
                searchCache[cacheKey] = Pair(System.currentTimeMillis(), response)
            }
        }
        return@withContext response
    }

    /**
     * Backward-compatible convenience wrapper returning list of results.
     */
    open suspend fun searchAndRead(
        query: String,
        maxResults: Int = SearchConfig.DEFAULT_MAX_SEARCH_RESULTS,
        isProduct: Boolean = false
    ): List<EnrichedSearchResult> {
        return searchAndReadWithStatus(query, maxResults, isProduct).results
    }

    /**
     * Cleans query specifically for visual image search engines by stripping filler/intent words,
     * while guaranteeing keywords like 'diagram' remain for architectural and technical searches.
     */
    fun cleanVisualQuery(raw: String): String {
        val q = sanitizeQuery(raw).trim()
        val wordsToStrip = setOf(
            "images", "image", "photos", "photo", "pictures", "picture", "pics", "pic",
            "wallpapers", "wallpaper", "diagrams", "diagram", "show me", "chupinchu",
            "chudu", "bomma", "bommalu", "hd", "4k", "of", "search", "find", "get", "show",
            "chey", "kavali", "chudali", "for", "please", "bro"
        )
        val tokens = q.split(Regex("""\s+""")).filter { it.lowercase() !in wordsToStrip }
        var cleaned = tokens.joinToString(" ").trim()
        if (cleaned.isBlank()) cleaned = q

        val lowerRaw = raw.lowercase()
        if ((lowerRaw.contains("architecture") || lowerRaw.contains("diagram") || lowerRaw.contains("flowchart") || lowerRaw.contains("workflow")) &&
            !cleaned.lowercase().contains("diagram")) {
            cleaned = "$cleaned diagram"
        }
        return cleaned
    }

    /**
     * Extracts and retrieves high-signal search images for inline chat display (ChatGPT style).
     * Strictly distinct from web search: focuses exclusively on visual diagrams, photos, and figures.
     */
    open suspend fun searchImages(
        query: String,
        enrichedPages: List<EnrichedSearchResult> = emptyList(),
        maxImages: Int = 8
    ): List<SearchImage> = withContext(Dispatchers.IO) {
        val clean = cleanVisualQuery(query)
        if (clean.isBlank()) return@withContext emptyList()

        val images = mutableListOf<SearchImage>()
        val seenUrls = mutableSetOf<String>()

        // 1. Primary zero-cost engine: High-resolution Bing Images Async API
        val bingImages = fetchBingImages(clean, maxImages)
        for (img in bingImages) {
            if (seenUrls.add(img.imageUrl)) {
                images.add(img)
            }
        }

        // 2. Fallback: DuckDuckGo Images if Bing returned fewer than desired
        if (images.size < maxImages) {
            val ddgImages = fetchDuckDuckGoImages(clean, maxImages - images.size)
            for (ddgImg in ddgImages) {
                if (seenUrls.add(ddgImg.imageUrl)) {
                    images.add(ddgImg)
                }
            }
        }

        // 3. Fallback: High-fidelity OpenGraph images from verified public webpages if engines returned empty
        if (images.isEmpty() && enrichedPages.isNotEmpty()) {
            for (page in enrichedPages) {
                val img = page.imageUrl
                if (!img.isNullOrBlank() && seenUrls.add(img) && webPageFetcher.isSafeUrl(img)) {
                    images.add(
                        SearchImage(
                            title = page.title,
                            imageUrl = img,
                            sourceUrl = page.url,
                            sourceDomain = extractDomain(page.url)
                        )
                    )
                }
                if (images.size >= maxImages) break
            }
        }

        images.take(maxImages)
    }

    fun fetchBingImages(query: String, limit: Int): List<SearchImage> {
        if (query.isBlank() || limit <= 0) return emptyList()
        var conn: HttpURLConnection? = null
        return try {
            val endpoint = URL("https://www.bing.com/images/async?q=" + URLEncoder.encode(query, "UTF-8") + "&first=0&count=25&mmasync=1")
            conn = (endpoint.openConnection() as HttpURLConnection).apply {
                connectTimeout = SearchConfig.IMAGE_CONNECT_TIMEOUT_MS
                readTimeout = SearchConfig.IMAGE_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (conn.responseCode != 200) {
                return emptyList()
            }

            val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseBingImages(html, limit, query)
        } catch (e: Exception) {
            Log.w(TAG, "[$TAG] Bing image search failed for '$query', falling back to DDG", e)
            emptyList()
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    fun parseBingImages(html: String, limit: Int, defaultTitle: String): List<SearchImage> {
        val iuscRegex = Regex("""class=["']iusc["'][^>]*m=["']([^"']+)["']""")
        val matches = iuscRegex.findAll(html).toList()
        val list = mutableListOf<SearchImage>()
        val seenUrls = mutableSetOf<String>()

        for (m in matches) {
            if (list.size >= limit) break
            val rawM = m.groupValues[1]
            try {
                val unescaped = rawM
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                val json = JsonParser.parseString(unescaped).asJsonObject
                val imgUrl = json.get("murl")?.asString?.trim() ?: continue
                val thumbUrl = json.get("turl")?.asString?.trim() ?: ""
                val srcUrl = json.get("purl")?.asString?.trim() ?: imgUrl
                val title = json.get("t")?.asString?.trim()
                    ?: json.get("desc")?.asString?.trim()
                    ?: defaultTitle

                if (imgUrl.isNotBlank() && imgUrl.startsWith("http") && seenUrls.add(imgUrl) && webPageFetcher.isSafeUrl(imgUrl)) {
                    list.add(
                        SearchImage(
                            title = title,
                            imageUrl = imgUrl,
                            sourceUrl = srcUrl,
                            sourceDomain = extractDomain(srcUrl),
                            thumbnailUrl = thumbUrl
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore malformed card
            }
        }
        return list
    }

    private fun fetchDuckDuckGoImages(query: String, limit: Int): List<SearchImage> {
        if (query.isBlank() || limit <= 0) return emptyList()
        var conn: HttpURLConnection? = null
        var apiConn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            // Step 1: obtain vqd token
            val tokenUrl = "https://duckduckgo.com/?q=$encodedQuery"
            conn = (URL(tokenUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = SearchConfig.IMAGE_CONNECT_TIMEOUT_MS
                readTimeout = SearchConfig.IMAGE_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }

            val vqdMatch = Regex("""vqd=["']?([0-9-]+)["']?""").find(html)
                ?: Regex("""vqd=([0-9-]+)""").find(html)
            val vqd = vqdMatch?.groupValues?.get(1) ?: return emptyList()

            // Step 2: Query image API endpoint
            val imgApiUrl = "https://duckduckgo.com/i.js?l=us-en&o=json&q=$encodedQuery&vqd=$vqd&f=,,,"
            apiConn = (URL(imgApiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = SearchConfig.IMAGE_CONNECT_TIMEOUT_MS
                readTimeout = SearchConfig.IMAGE_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Accept", "application/json")
            }
            if (apiConn.responseCode != 200) {
                return emptyList()
            }
            val jsonText = apiConn.inputStream.bufferedReader().use { it.readText() }

            val jsonObject = JsonParser.parseString(jsonText).asJsonObject
            val resultsArray = jsonObject.getAsJsonArray("results") ?: return emptyList()

            val ddgList = mutableListOf<SearchImage>()
            for (elem in resultsArray) {
                if (ddgList.size >= limit) break
                val obj = elem.asJsonObject
                val imgUrl = obj.get("image")?.asString ?: continue
                val thumbUrl = obj.get("thumbnail")?.asString?.trim() ?: ""
                val title = obj.get("title")?.asString ?: query
                val srcUrl = obj.get("url")?.asString ?: imgUrl
                if (webPageFetcher.isSafeUrl(imgUrl)) {
                    ddgList.add(
                        SearchImage(
                            title = title,
                            imageUrl = imgUrl,
                            sourceUrl = srcUrl,
                            sourceDomain = extractDomain(srcUrl),
                            thumbnailUrl = thumbUrl
                        )
                    )
                }
            }
            return ddgList
        } catch (_: Exception) {
            return emptyList()
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
            try { apiConn?.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * Formats enriched search results into a clean, injection-safe context block for Gemma's prompt.
     * Strictly delimits external webpage text with <WEB_SEARCH_RESULTS> to neutralize prompt injection.
     */
    fun formatGemmaWebContext(
        results: List<EnrichedSearchResult>,
        maxTotalChars: Int = SearchConfig.MAX_WEB_CONTEXT_CHARS
    ): String {
        if (results.isEmpty()) return ""

        val header = "## LIVE WEB RESULTS (UNTRUSTED EXTERNAL DATA)\n<WEB_SOURCE_UNTRUSTED_DATA>\n"
        val footer = "</WEB_SOURCE_UNTRUSTED_DATA>"
        if (maxTotalChars < header.length + footer.length) return ""
        val sources = results.take(SearchConfig.DEFAULT_MAX_SEARCH_RESULTS)
        val perSource = (maxTotalChars - header.length - footer.length) / sources.size
        val sb = StringBuilder(header)
        fun clean(value: String) = value.replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
            .replace("<", "&lt;").replace(">", "&gt;").trim()
        for ((idx, res) in sources.withIndex()) {
            val metadata = "[${idx + 1}] Title: ${clean(res.title).take(120)}\n" +
                "Source URL: ${clean(res.url).take(300)}\n" +
                "Evidence: ${if (res.fetchSucceeded) "fetched webpage" else "search snippet only"}\n"
            val label = if (res.fetchSucceeded) "Webpage Text: " else "Snippet: "
            val remaining = perSource - metadata.length - label.length - 2
            if (remaining < 0) break
            val evidence = if (res.fetchSucceeded && res.pageContent.isNotBlank()) res.pageContent else res.snippet
            sb.append(metadata).append(label).append(clean(evidence).take(remaining)).append("\n\n")
        }
        return sb.append(footer).toString()
    }

    private fun isAuthOrLoginText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("sign in to") || lower.contains("login to") ||
               lower.contains("join linkedin") || lower.contains("enable javascript") ||
               lower.contains("verify you are human") || lower.contains("cloudflare")
    }
}
