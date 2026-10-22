package com.teja.gemmmobile.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teja.gemmmobile.ai.BackendType
import com.teja.gemmmobile.ai.EngineState
import com.teja.gemmmobile.ai.GemmaConfig
import com.teja.gemmmobile.ai.GemmaEngine
import com.teja.gemmmobile.ai.GemmaRepository
import com.teja.gemmmobile.model.ModelInstallState
import com.teja.gemmmobile.model.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.teja.gemmmobile.memory.MemoryItem
import com.teja.gemmmobile.memory.MemoryManager
import com.teja.gemmmobile.memory.AutonomousBrainMemoryHelper
import com.teja.gemmmobile.memory.MemoryBrainAction
import com.teja.gemmmobile.search.SearchResult
import com.teja.gemmmobile.search.SearchImage
import com.teja.gemmmobile.search.SearchConfig
import com.teja.gemmmobile.search.WebSearchClient
import com.teja.gemmmobile.audio.TtsManager
import com.teja.gemmmobile.ocr.ExtractedDocument
import com.teja.gemmmobile.storage.ChatSession
import com.teja.gemmmobile.storage.ChatStorage
import android.graphics.Bitmap
import java.util.UUID

private const val TAG = "GemmaMobile"

enum class MessageRole {
    USER,
    ASSISTANT
}

enum class ResponseFeedback {
    POSITIVE,
    NEGATIVE
}

@androidx.compose.runtime.Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val thoughtText: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val searchImages: List<SearchImage> = emptyList(),
    val isStreaming: Boolean = false,
    val isThinking: Boolean = false,
    val isSearchingWeb: Boolean = false,
    val isImageAnalysis: Boolean = false,
    @Transient val imageBitmap: Bitmap? = null,
    val imagePath: String? = null,
    val isExecutingTool: Boolean = false,
    val toolExecutionStatus: String? = null,
    val citedPages: List<Int> = emptyList(),
    val sourceFileName: String? = null,
    val feedback: ResponseFeedback? = null,
    val isExplicitVisualRequest: Boolean = false,
    val generatedPdfPath: String? = null,
    val generatedPdfTitle: String? = null,
    val generatedPdfPageCount: Int = 0,
    val isPdfDocumentRequest: Boolean = false,
    val isCutOff: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val modelManager = ModelManager(application.applicationContext)
    private val chatStorage = ChatStorage(application.applicationContext)
    private val searchManager = com.teja.gemmmobile.search.SearchManager()
    val memoryManager = MemoryManager(application.applicationContext)
    val skillRegistry = com.teja.gemmmobile.skills.SkillRegistry(application.applicationContext)
    val toolRegistry = com.teja.gemmmobile.tools.ToolRegistry().apply {
        register(com.teja.gemmmobile.tools.WebSearchTool(searchManager))
        register(com.teja.gemmmobile.tools.ImageSearchTool(searchManager))
        register(com.teja.gemmmobile.tools.MemoryTool(memoryManager))
        register(com.teja.gemmmobile.tools.OcrTool(application.applicationContext))
        register(skillRegistry.deviceIntentsTool)
        register(skillRegistry.qrCodeTool)
        register(skillRegistry.wikipediaTool)
    }
    val contextManager = com.teja.gemmmobile.context.ContextManager()
    private val engine: GemmaEngine? get() = GemmaRepository.getEngine()

    val installState: StateFlow<ModelInstallState> = modelManager.installState

    private val _engineState = MutableStateFlow<EngineState>(GemmaRepository.engineState.value)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String>(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val memories: StateFlow<List<MemoryItem>> = memoryManager.memories

    private val _memoryUpdatedEvent = MutableStateFlow<String?>(null)
    val memoryUpdatedEvent: StateFlow<String?> = _memoryUpdatedEvent.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isWebSearchEnabled = MutableStateFlow(false)
    val isWebSearchEnabled: StateFlow<Boolean> = _isWebSearchEnabled.asStateFlow()

    private val _selectedTheme = MutableStateFlow(
        AppTheme.fromName(
            getApplication<Application>()
                .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getString("app_theme", AppTheme.CHATGPT_DARK.name)
        )
    )
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _selectedTheme.value = theme
        getApplication<Application>()
            .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("app_theme", theme.name)
            .apply()
    }

    private val ttsManager = TtsManager(application)
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val currentlySpeakingId: StateFlow<String?> = ttsManager.currentMessageId

    private val _attachedDocument = MutableStateFlow<ExtractedDocument?>(null)
    val attachedDocument: StateFlow<ExtractedDocument?> = _attachedDocument.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<ExtractedDocument>>(emptyList())
    val recentFiles: StateFlow<List<ExtractedDocument>> = _recentFiles.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val generationExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "[$TAG] Uncaught exception in applicationScope coroutine: ${throwable.localizedMessage}", throwable)
        _errorMessage.value = throwable.localizedMessage ?: "An unexpected error occurred"
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + generationExceptionHandler)
    private var activeGenerationJob: Job? = null
    val activeGeneratingSessionId = MutableStateFlow<String?>(null)



    fun sendMessageWithText(text: String) {
        _inputText.value = text
        sendMessage()
    }

    private val _hfToken = MutableStateFlow(modelManager.getSavedHfToken())
    val hfToken: StateFlow<String> = _hfToken.asStateFlow()

    private val _config = MutableStateFlow(modelManager.loadConfig())
    val config: StateFlow<GemmaConfig> = _config.asStateFlow()

    private var activeDownloadJob: Job? = null

    init {
        checkModel()
        loadPersistedSessions()
        // Mirror shared engine state across entire app
        viewModelScope.launch {
            GemmaRepository.engineState.collect { state ->
                _engineState.value = state
                if (state is EngineState.Error) {
                    _errorMessage.value = state.message
                } else if (state is EngineState.Ready) {
                    _errorMessage.value = null
                }
            }
        }
        // Auto-start engine only when model becomes available in local storage
        viewModelScope.launch {
            installState.collect { state ->
                if (state is ModelInstallState.Installed && !GemmaRepository.isReady()) {
                    initializeEngine()
                }
            }
        }
    }

    private fun loadPersistedSessions() {
        viewModelScope.launch {
            val loaded = chatStorage.loadSessions()
            _sessions.value = loaded
            if (loaded.isNotEmpty()) {
                val latest = loaded.first()
                _currentSessionId.value = latest.id
                _messages.value = latest.messages
            }
            withContext(Dispatchers.IO) {
                com.teja.gemmmobile.storage.StorageManagerHelper.cleanOrphanImages(getApplication(), loaded)
                refreshStorageUsage()
            }
        }
    }

    fun updateSessionMessages(
        sessionId: String,
        transform: (List<ChatMessage>) -> List<ChatMessage>
    ) {
        // 1. Compute updated messages from the most current source
        val baseMessages = if (_currentSessionId.value == sessionId && _messages.value.isNotEmpty()) {
            _messages.value
        } else {
            _sessions.value.find { it.id == sessionId }?.messages ?: emptyList()
        }
        val updatedMessages = transform(baseMessages)

        // 2. Update the displayed messages immediately — this is the source of truth for UI
        if (_currentSessionId.value == sessionId) {
            _messages.value = updatedMessages
        }

        // 3. Update the sessions list (find/create session, update messages, keep order stable)
        val sessionList = _sessions.value.toMutableList()
        val index = sessionList.indexOfFirst { it.id == sessionId }
        val oldSession = if (index != -1) sessionList[index] else {
            ChatSession(id = sessionId, title = "New Chat", messages = emptyList())
        }

        val title = if (oldSession.title != "New Chat") oldSession.title else {
            val firstUser = updatedMessages.firstOrNull { it.role == MessageRole.USER }?.text?.trim()
            if (!firstUser.isNullOrBlank()) firstUser.take(35) else "New Chat"
        }

        val updatedSession = oldSession.copy(
            title = title,
            updatedAt = System.currentTimeMillis(),
            messages = updatedMessages
        )

        if (index != -1) {
            sessionList[index] = updatedSession
        } else {
            sessionList.add(0, updatedSession)
        }
        // Only sort sessions in the sidebar — do not trigger a _messages reset here
        _sessions.value = sessionList.sortedByDescending { it.updatedAt }
    }

    fun persistChat() {
        val msgs = _messages.value
        if (msgs.isEmpty()) return
        val currId = _currentSessionId.value
        val existingSession = _sessions.value.find { it.id == currId }
        val title = if (existingSession != null && existingSession.title != "New Chat") {
            existingSession.title
        } else {
            val firstUser = msgs.firstOrNull { it.role == MessageRole.USER }?.text?.trim()
            if (!firstUser.isNullOrBlank()) {
                firstUser.take(35)
            } else "New Chat"
        }

        val updatedSession = ChatSession(
            id = currId,
            title = title,
            createdAt = existingSession?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messages = msgs
        )

        val updatedList = _sessions.value.toMutableList()
        val idx = updatedList.indexOfFirst { it.id == currId }
        if (idx != -1) {
            updatedList[idx] = updatedSession
        } else {
            updatedList.add(0, updatedSession)
        }
        val sorted = updatedList.sortedByDescending { it.updatedAt }
        _sessions.value = sorted

        viewModelScope.launch {
            chatStorage.saveSession(updatedSession)
        }
    }

    fun onHfTokenChanged(token: String) {
        _hfToken.value = token
        modelManager.saveHfToken(token)
    }

    fun updateConfig(newConfig: GemmaConfig) {
        val oldBackend = _config.value.preferredBackend
        _config.value = newConfig
        modelManager.saveConfig(newConfig)
        if (oldBackend != newConfig.preferredBackend) {
            initializeEngine()
        } else {
            viewModelScope.launch {
                GemmaRepository.updateConfig(newConfig)
            }
        }
    }

    fun resetConfig() {
        updateConfig(GemmaConfig.DEFAULT)
    }

    fun checkModel() {
        viewModelScope.launch {
            modelManager.checkModelAvailability()
        }
    }

    fun downloadModel() {
        activeDownloadJob?.cancel()
        activeDownloadJob = viewModelScope.launch {
            val result = modelManager.downloadModelFromHf(_hfToken.value)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to download model"
            }
        }
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        checkModel()
    }

    fun deleteModel() {
        viewModelScope.launch {
            GemmaRepository.close()
            modelManager.deleteModel()
        }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            val result = modelManager.importModelFromUri(uri)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to import model"
            }
        }
    }

    fun initializeEngine() {
        val currentInstallState = installState.value
        val modelFile = (currentInstallState as? ModelInstallState.Installed)?.modelFile
            ?: modelManager.defaultModelFile

        if (!modelFile.exists()) {
            _errorMessage.value = "Gemma model is not installed."
            return
        }

        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir.absolutePath
            val result = GemmaRepository.reinitializeEngine(modelFile, _config.value, cacheDir)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Model loading failure"
                _errorMessage.value = err
            } else {
                _errorMessage.value = null
            }
        }
    }

    fun addMemory(fact: String) {
        viewModelScope.launch {
            memoryManager.addMemory(fact)
        }
    }

    fun removeMemory(id: String) {
        viewModelScope.launch {
            memoryManager.removeMemory(id)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryManager.clearAll()
        }
    }

    fun clearMemoryUpdatedEvent() {
        _memoryUpdatedEvent.value = null
    }

    fun toggleWebSearch() {
        _isWebSearchEnabled.value = !_isWebSearchEnabled.value
    }

    fun stopGeneration() {
        val genSessionId = activeGeneratingSessionId.value ?: _currentSessionId.value
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        activeGeneratingSessionId.value = null
        _isGenerating.value = false

        // Force reset engine state back to Ready immediately so the badge changes
        GemmaRepository.resetToReady()
        val backend = (_engineState.value as? EngineState.Generating)?.backend ?: BackendType.CPU
        _engineState.value = EngineState.Ready(backend)

        updateSessionMessages(genSessionId) { msgs ->
            msgs.map { msg ->
                if (msg.isStreaming || msg.isThinking || msg.isSearchingWeb) {
                    msg.copy(isStreaming = false, isThinking = false, isSearchingWeb = false)
                } else msg
            }
        }
        val targetSession = _sessions.value.find { it.id == genSessionId }
        if (targetSession != null) {
            applicationScope.launch { chatStorage.saveSession(targetSession) }
        }
    }

    fun isPersonQuery(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        if (isProductQuery(p) || isFlowchartRequest(p) || isContinueRequest(p)) return false

        // 1. Direct biographical intent keywords in English and Telugu
        val personKeywords = listOf(
            "who is ", "who was ", "who are ", "who were ",
            "tell me about ", "details of ", "profile of ", "bio of ",
            "biography of ", "background of ", "career of ",
            "evaru", "evaroo", "gurinchi cheppu", "gurinchi telupu",
            "gurinchi vivarinchu", "evaro cheppu", "evaro telupu",
            "linkedin", "profile", "founder of", "ceo of", "cto of",
            "director of", "president of", "minister of", "author of",
            "actor", "actress", "politician", "scientist", "cricketer", "player"
        )
        if (personKeywords.any { p.contains(it) }) return true

        // 2. Name patterns: 2-4 capitalized words in raw prompt
        val words = prompt.trim().split(Regex("""\s+"""))
        if (words.size in 2..4 && words.all { it.isNotEmpty() && it[0].isUpperCase() }) {
            val nonPersonWords = setOf("How", "Why", "What", "When", "Where", "Which", "Can", "Could", "Would", "Should", "Is", "Are", "The", "Best", "Top", "Android", "Google", "Apple", "Microsoft", "Amazon", "Samsung")
            if (words.none { it in nonPersonWords }) {
                return true
            }
        }
        return false
    }

    fun isProductQuery(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        val shoppingPhrases = listOf(
            "buy online", "buy in india", "shopping", "where to buy",
            "price in india", "under 10000", "under 15000", "under 20000",
            "under 25000", "under 30000", "under 40000", "under 50000"
        )
        return shoppingPhrases.any { p.contains(it) }
    }

    private fun resolveSearchQuery(raw: String, history: List<ChatMessage>): String {
        var q = raw.trim()
        val removePrefixes = listOf(
            "can you please search the web for ", "can you please search web for ",
            "can you search the web for ", "can you search web for ",
            "please search the web for ", "please search web for ",
            "search the web for ", "search the web and ", "search the web ",
            "search web for ", "search web and ", "search web ",
            "search for ", "search ", "browse for ", "browse ",
            "google for ", "google ", "look up for ", "look up ",
            "web search chesi ", "net lo chusi "
        )
        for (p in removePrefixes) {
            if (q.startsWith(p, ignoreCase = true)) {
                q = q.substring(p.length).trim()
                break
            }
        }
        return q.ifBlank { raw.trim() }
    }

    /**
     * Smart fallback normalizer for offline/fast queries:
     * Translates common Telugu/Tanglish conversational intent words to English keywords
     * and strips conversational filler words like 'cheppu', 'undhi', 'bro', 'please'.
     */
    private fun fallbackNormalizeQuery(raw: String): String {
        var q = resolveSearchQuery(raw, emptyList())

        val replacements = listOf(
            Regex("""\beeroju\b""", RegexOption.IGNORE_CASE) to "today",
            Regex("""\bippudu\b""", RegexOption.IGNORE_CASE) to "current",
            Regex("""\bvarthalu\b""", RegexOption.IGNORE_CASE) to "news",
            Regex("""\bsamacharam\b""", RegexOption.IGNORE_CASE) to "news update",
            Regex("""\bdharalu\b""", RegexOption.IGNORE_CASE) to "prices",
            Regex("""\bdhara\b""", RegexOption.IGNORE_CASE) to "price",
            Regex("""\bentha\b""", RegexOption.IGNORE_CASE) to "price",
            Regex("""\bhyd\b""", RegexOption.IGNORE_CASE) to "hyderabad",
            Regex("""\bvizag\b""", RegexOption.IGNORE_CASE) to "visakhapatnam",
            Regex("""\bvja\b""", RegexOption.IGNORE_CASE) to "vijayawada",
            Regex("""\bgurinchi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bcheppu\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bcheppandi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bchey\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bcheyyi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bchudu\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bchupinchu\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bundhi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bunna\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bunnayi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bkavali\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bivvu\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bivvandi\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bbro\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bplease\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\btell me\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bcan you\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bwhat is\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bwhat are\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\btelugu lo\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bnet lo\b""", RegexOption.IGNORE_CASE) to "",
            Regex("""\bonline lo\b""", RegexOption.IGNORE_CASE) to ""
        )

        for ((regex, replacement) in replacements) {
            q = regex.replace(q, replacement)
        }

        // Clean extra punctuation and normalize whitespace
        q = q.replace(Regex("""[?.,!\"':;()]+"""), " ").trim()
        q = q.replace(Regex("""\s+"""), " ").trim()

        return q.ifBlank { raw.trim() }
    }

    /**
     * Uses Gemma SLM on-device to formulate 2 to 5 high-yield search engine keywords
     * from conversational or multilingual/Telugu user requests before hitting DuckDuckGo/Web.
     * Features a strict 2.5s timeout with automatic fast fallback.
     */
    private suspend fun formulateAiSearchQuery(prompt: String, eng: GemmaEngine): String {
        val words = prompt.trim().split(Regex("""\s+"""))
        val isTelugu = prompt.any { it.code in 0x0C00..0x0C7F }
        val isConversational = words.size > 4 || isTelugu || prompt.contains("?") ||
                listOf("tell", "what", "how", "cheppu", "bro", "eeroju", "undhi", "lo", "who", "which").any { prompt.lowercase().contains(it) }

        if (!isConversational && words.size in 1..4) {
            return prompt.trim()
        }

        return try {
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                val cleanUserPrompt = prompt.take(160).replace("\"", "")
                val systemPrompt = "You are an AI Search Query Formulator. Given a user question, output ONLY 2 to 5 concise English search keywords optimized for search engines. Translate regional languages to English. Output NO quotes, NO explanation, NO conversational filler. Only keywords."
                val userPrompt = "User request: \"$cleanUserPrompt\"\nKeywords:"

                val chunks = StringBuilder()
                eng.sendMessage(
                    prompt = userPrompt,
                    systemInstruction = systemPrompt,
                    enableThinkingOverride = false,
                    dynamicMaxOutputOverride = 16
                ).collect { chunk ->
                    if (chunk.text.isNotBlank()) {
                        chunks.append(chunk.text)
                    }
                }

                val result = chunks.toString().trim()
                    .lines().firstOrNull { it.isNotBlank() } ?: ""
                var clean = result
                    .replace(Regex("""^["'`*-]+"""), "")
                    .replace(Regex("""["'`]+$"""), "")
                    .replace(Regex("""^(keywords|query|search query|search for):\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (clean.isNotBlank() && clean.length > 2 && !clean.contains("cannot", ignoreCase = true) && !clean.contains("as an ai", ignoreCase = true)) {
                    Log.i(TAG, "[$TAG] AI formulated search query: '$clean' from prompt: '$prompt'")
                    clean
                } else {
                    fallbackNormalizeQuery(prompt)
                }
            } ?: fallbackNormalizeQuery(prompt)
        } catch (t: Throwable) {
            Log.w(TAG, "[$TAG] AI query formulation failed or timed out, using fallback", t)
            fallbackNormalizeQuery(prompt)
        }
    }

    fun isRefusalToBrowse(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("don't have the capability to browse") ||
               lower.contains("do not have the capability to browse") ||
               lower.contains("cannot browse the web") ||
               lower.contains("can't browse the web") ||
               lower.contains("don't have access to real-time") ||
               lower.contains("do not have access to real-time") ||
               lower.contains("cannot provide real-time") ||
               lower.contains("can't provide real-time") ||
               lower.contains("data i was trained on") ||
               lower.contains("cannot browse live") ||
               lower.contains("can't browse live") ||
               lower.contains("web browse cheyalenu") ||
               lower.contains("internet access ledu") ||
               lower.contains("live search cheyalenu") ||
               lower.contains("real-time samacharam ledu")
    }

    fun isRefusalToSearchImages(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("cannot display images") ||
               lower.contains("can't display images") ||
               lower.contains("cannot show images") ||
               lower.contains("can't show images") ||
               lower.contains("cannot generate images") ||
               lower.contains("can't generate images") ||
               lower.contains("text-based ai") ||
               lower.contains("text-based model") ||
               lower.contains("cannot search images") ||
               lower.contains("can't search images") ||
               lower.contains("cannot view photos") ||
               lower.contains("cannot view images") ||
               lower.contains("unable to show photos") ||
               lower.contains("unable to show images") ||
               lower.contains("bommalu chupinchalenu") ||
               lower.contains("photos chupinchalenu") ||
               lower.contains("image chupinchalenu")
    }

    private fun extractSearchQuery(raw: String): String {
        return searchManager.sanitizeQuery(raw)
    }

    private fun shouldAutoSearch(prompt: String): Boolean {
        val p = prompt.lowercase().trim()

        // Documents, PDFs, and uploaded files must NEVER trigger web search
        if (p.contains("pdf") || p.contains("document") || p.contains("this file") || p.contains("my file") || p.contains("my notes") || p.contains("in the notes")) {
            return false
        }

        // 1. Explicit search command from user
        if (p.startsWith("search ") || p.startsWith("web search ") || p.startsWith("browse ") ||
            p.contains("search web") || p.contains("search the web") || p.contains("search for ") ||
            p.contains("google ") || p.contains("look up ") || p.contains("find out ") ||
            p.contains("product links") || p.contains("buy links") || p.contains("give links") ||
            p.contains("give product links") || p.contains("search chesi") || p.contains("net lo chusi") ||
            p.contains("net lo vethiki") || p.contains("online lo chusi") || p.contains("web research") ||
            p.contains("research chey") || p.contains("fact check") || p.contains("links ivvu")) {
            return true
        }

        // 2. Real-time / factual questions needing web evidence
        val realTimeKeywords = listOf(
            "latest", "today", "breaking news", "current price", "stock price", "crypto price",
            "live score", "match score", "weather", "forecast", "live update", "election",
            "release date", "who is ", "who was ", "when is ", "where is ", "how much is ",
            "cutoff", "results", "winner", "prime minister", "president", "ceo of",
            "varthalu", "eeroju", "ippudu",
            // Product & Recommendation queries
            "suggest", "recommend", "best phone", "best mobile", "best laptop", "best sunscreen",
            "phone under", "mobile under", "laptop under", "under 10000", "under 15000", "under 20000",
            "under 25000", "under 30000", "under 40000", "under 50000", "under 500", "under 1000",
            "budget phone", "budget mobile", "budget sunscreen", "which phone", "which mobile",
            "which laptop", "manchi phone", "manchi mobile"
        )
        return realTimeKeywords.any { keyword -> p.contains(keyword) }
    }

    fun shouldQueryImages(prompt: String): Boolean {
        val p = prompt.lowercase().trim()

        // Documents, PDFs, and uploaded files must NEVER trigger image search
        if (p.contains("pdf") || p.contains("document") || p.contains("this file") || p.contains("my file") || p.contains("my notes")) {
            return false
        }

        // Whole-word matching only — prevents "topics" matching "pics" or "pic"!
        val words = p.split(Regex("""[^a-zA-Z0-9]+""")).filter { it.isNotBlank() }.toSet()
        val singleWordVisual = setOf(
            "image", "images", "photo", "photos", "picture", "pictures",
            "pic", "pics", "diagram", "diagrams", "figure", "figures",
            "chart", "charts", "illustration", "illustrations", "wallpaper", "wallpapers",
            "bomma", "bommalu"
        )
        if (words.any { it in singleWordVisual }) return true

        val visualPhrases = listOf(
            "look like", "how does it look", "visualize", "show me", "chupinchu", "chudu", "photos of", "images of", "diagram of"
        )
        return visualPhrases.any { p.contains(it) }
    }

    fun isContinueRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim().trimEnd('.', '!', '?', ',', ' ', ';')
        val exactMatches = setOf(
            "continue", "cont", "continue generating", "go on", "keep going",
            "more", "tell me more", "inka", "inka cheppu", "inka rayi",
            "next", "tarvatha", "continue chey", "continue cheyyi",
            "inka emundi", "malli cheppu", "proceed", "carry on", "continue please",
            "continue response", "continue from where you left off",
            "continue chey bro", "continue bro", "inka cheppu bro"
        )
        if (p in exactMatches) return true
        if (p.startsWith("continue") || p.startsWith("inka ") || p.startsWith("go on")) return true
        val words = p.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size <= 3 && (words.contains("continue") || words.contains("inka") || words.contains("tarvatha"))) return true
        return false
    }

    /**
     * Detects if an assistant response was abruptly truncated / cut off mid-thought due to token budget exhaustion.
     */
    fun isTruncatedOrCutoff(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        val text = rawText.trim()
        if (text.length < 350) return false // Short direct answers are not cutoffs

        // 1. Ending with hanging hyphen or dash (e.g. "within-", "hyper-", "multi-")
        if (text.endsWith("-") || text.endsWith("—") || text.endsWith("–")) return true

        // 2. Ending with non-terminal punctuation (comma, colon, semicolon, open brackets, slashes)
        if (text.endsWith(",") || text.endsWith(":") || text.endsWith(";") ||
            text.endsWith("(") || text.endsWith("[") || text.endsWith("{") ||
            text.endsWith("/") || text.endsWith("\\") || text.endsWith("&")) return true

        // 3. Ending in an incomplete list item or heading (e.g. "• Metric: ", "### ")
        val lastLine = text.lines().lastOrNull()?.trim() ?: ""
        if (lastLine.matches(Regex("""^(?:[-*•]|\d+\.)\s*([A-Za-z0-9_]+:)?\s*$""")) ||
            (lastLine.startsWith("#") && !lastLine.contains("\n"))) return true

        // 4. Odd number of markdown code fences (unclosed code block)
        val fenceCount = Regex("""```""").findAll(text).count()
        if (fenceCount % 2 != 0) return true

        // 5. Ending without terminal sentence punctuation
        val lastChar = text.last()
        val terminalPunctuation = setOf('.', '!', '?', '"', '\'', '`', '।', '’', '”', '…', ')', ']', '}')
        if (lastChar !in terminalPunctuation) {
            return true
        }

        return false
    }

    /**
     * Seamlessly stitches a continuation chunk onto previous text, eliminating duplicate overlapping tokens
     * and preserving hyphenated words and whitespace.
     */
    fun stitchContinuation(previousText: String, continuation: String): String {
        val prev = previousText.trimEnd()
        var cont = continuation.trimStart()

        // Strip conversational filler prefixes emitted by the model
        val fillerPrefixes = listOf(
            "Continuing from where we left off:",
            "Continuing from where I left off:",
            "Continuing:",
            "Sure, continuing:",
            "Sure!",
            "Certainly!",
            "Here is the rest:",
            "Here is the continuation:",
            "Continuing the study notes:"
        )
        for (filler in fillerPrefixes) {
            if (cont.startsWith(filler, ignoreCase = true)) {
                cont = cont.substring(filler.length).trimStart()
            }
        }

        // Check overlap of up to 40 characters between end of prev and start of cont
        val maxOverlap = minOf(40, prev.length, cont.length)
        for (overlapLen in maxOverlap downTo 3) {
            val prevSuffix = prev.takeLast(overlapLen)
            if (cont.startsWith(prevSuffix, ignoreCase = true)) {
                cont = cont.substring(overlapLen).trimStart()
                break
            }
        }

        // If prev ends with a hyphen (like "within-") and cont starts with "cluster"
        if (prev.endsWith("-") && !cont.startsWith(" ") && !cont.startsWith("\n")) {
            return prev + cont
        }

        // If prev doesn't end with whitespace or newline and cont doesn't start with whitespace/newline
        if (!prev.endsWith(" ") && !prev.endsWith("\n") && !cont.startsWith(" ") && !cont.startsWith("\n")) {
            return "$prev $cont"
        }

        return prev + cont
    }

    fun isFlowchartRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        val keywords = listOf(
            "flowchart", "flow chart", "workflow", "process flow", "architecture flow",
            "mermaid", "flow chats", "flowchat", "flow chart lo", "diagram lo", "flowchart lo",
            "explain with flowchart", "flowchart tho"
        )
        return keywords.any { p.contains(it) }
    }

    fun isPdfRequest(prompt: String): Boolean {
        val p = prompt.lowercase().trim()
        val containsPdf = p.contains("pdf") ||
                          p.contains("p.d.f") ||
                          p.contains("పిడిఎఫ్") ||
                          p.contains("పీడీఎఫ్") ||
                          p.contains("పీడిఎఫ్")
        if (!containsPdf) return false

        // Informational questions about PDF technology should remain conversational
        val informationalQueries = listOf(
            "what is pdf", "what is a pdf", "pdf full form", "full form of pdf",
            "who invented pdf", "who created pdf", "how does pdf work", "define pdf",
            "pdf meaning", "pdf ante enti", "pdf ante emiti"
        )
        if (informationalQueries.any { p.contains(it) }) return false

        return true
    }

    fun extractPdfTopic(prompt: String): String {
        var topic = prompt.trim()
        val removePatterns = listOf(
            Regex("""(?i)\b(?:please\s+)?(?:make|create|generate|give|export|format|write|prepare)\b"""),
            Regex("""(?i)\b(?:a\s+)?(?:pdf|p\.d\.f|document|file)\b"""),
            Regex("""(?i)\b(?:cheyyi|chey|kavali|ivvu|pettu|rasi|cheymante|lo|ga|ante|ey|chesi|ivvali|cheyali)\b"""),
            Regex("""[పిపీ]డిఎఫ్""")
        )
        for (pat in removePatterns) {
            topic = pat.replace(topic, " ")
        }
        topic = topic.replace(Regex("""\s+"""), " ").trim()
        return topic.ifBlank { prompt.trim() }
    }

    fun stripPdfDisclaimers(text: String): String {
        var cleaned = text.trim()
        val disclaimerPatterns = listOf(
            // "As an AI, I cannot create PDF files directly..."
            Regex("""^(?:As an AI|I am an AI|As a language model)[^\n]*?(?:cannot|can't|unable to)[^\n]*?(?:pdf|file|document|download)[^\n]*?\n+""", RegexOption.IGNORE_CASE),
            // "I cannot create or export PDF files..."
            Regex("""^I (?:cannot|can't|am unable to|do not have the ability to) (?:directly )?(?:create|generate|export|download|make|save|produce) (?:a |any )?(?:pdf|file|document)[^\n]*?\n+""", RegexOption.IGNORE_CASE),
            // "Sure! Here is the content formatted for a PDF:"
            Regex("""^(?:Sure!?|Certainly!?|Here is|Below is|Here are)[^\n]*?(?:notes|content|guide|summary|document)[^\n]*?(?:for (?:your |a )?pdf|to (?:save|export|convert) (?:as|into) (?:a )?pdf)[^\n]*?:?\n+""", RegexOption.IGNORE_CASE),
            // Conversational opener lines
            Regex("""^(?:Sure!?|Certainly!?|Of course!?|Here you go!?)\s*\n+""", RegexOption.IGNORE_CASE)
        )
        for (pattern in disclaimerPatterns) {
            cleaned = pattern.replace(cleaned, "").trim()
        }
        // Strip trailing chat closing fluffs
        cleaned = cleaned.replace(Regex("""\n+(?:Hope this helps!?|Let me know if you need (?:more|any other) (?:notes|help|topics)!?|Feel free to ask[^\n]*)\s*$""", RegexOption.IGNORE_CASE), "").trim()
        return cleaned
    }

    fun stripToolCallJson(text: String): String {
        // Strip markdown code fences containing JSON tool calls
        val fenceRegex = Regex("""```(?:json)?\s*\{\s*["'](?:name|tool|function|type|tool_call)["'][\s\S]*?\}\s*```""", RegexOption.IGNORE_CASE)
        var cleaned = fenceRegex.replace(text, "").trim()
        // Strip raw standalone JSON tool call objects
        val rawJsonRegex = Regex("""\{\s*["'](?:name|tool|function|type|tool_call)["'][\s\S]*?\}""", RegexOption.IGNORE_CASE)
        cleaned = rawJsonRegex.replace(cleaned, "").trim()
        return cleaned
    }

    fun stripAssumedToolText(text: String): String {
        var cleaned = stripToolCallJson(text)
        // Strip "(Assuming the search tool has run and returned relevant information...)"
        cleaned = cleaned.replace(Regex("""\([Aa]ssuming the (?:search )?tool has run[^\)]*\):?"""), "")
        cleaned = cleaned.replace(Regex("""Please wait while I search[^\n]*"""), "")
        cleaned = cleaned.replace(Regex("""Since I cannot directly display images here, I will use the search tool[^\n]*"""), "")
        cleaned = cleaned.replace(Regex("""I will search the web for[^\n]*"""), "")
        return cleaned.trim()
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun attachDocument(document: ExtractedDocument) {
        _attachedDocument.value = document
        // Strip heavy raw image bytes from recentFiles cache to avoid heap memory bloat
        val cachedDoc = if (document.imageBytes != null && document.imageBytes.size > 200_000) {
            document.copy(imageBytes = null)
        } else document
        _recentFiles.value = (listOf(cachedDoc) + _recentFiles.value.filter { it.fileName != document.fileName }).take(10)
    }

    fun clearAttachedDocument() {
        _attachedDocument.value = null
    }

    fun setResponseFeedback(messageId: String, feedback: ResponseFeedback) {
        val sessionId = _currentSessionId.value
        updateSessionMessages(sessionId) { messages ->
            messages.map { message ->
                if (message.id == messageId) {
                    message.copy(feedback = if (message.feedback == feedback) null else feedback)
                } else message
            }
        }
        persistChat()
    }

    fun toggleSpeak(messageId: String, text: String) {
        ttsManager.speak(messageId, text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun getCurrentSession(): ChatSession? {
        return _sessions.value.find { it.id == _currentSessionId.value }
    }

    fun sendMessage() {
        val rawInput = _inputText.value.trim()
        val doc = _attachedDocument.value

        if (rawInput.isEmpty() && doc == null) return
        if (_isGenerating.value) return

        val eng = engine
        if (eng == null || _engineState.value !is EngineState.Ready) {
            _errorMessage.value = "Please wait for Gemma 4 to finish loading before sending."
            return
        }

        // Do not discard a draft or attachment until the model can actually accept it.
        // Keeping the input intact lets the user retry after startup instead of having to
        // recreate a long message or reattach a file.
        _inputText.value = ""
        _attachedDocument.value = null
        ttsManager.stop()

        // Autonomous Brain & Memory Detection (ChatGPT-style auto memory extraction & forget)
        val memoryAction = AutonomousBrainMemoryHelper.evaluateUserUtterance(rawInput, memoryManager.memories.value)
        if (memoryAction != null) {
            when (memoryAction) {
                is MemoryBrainAction.Add -> {
                    viewModelScope.launch {
                        memoryManager.addMemory(memoryAction.fact)
                        _memoryUpdatedEvent.value = "Memory updated: ${memoryAction.fact}"
                    }
                }
                is MemoryBrainAction.Forget -> {
                    viewModelScope.launch {
                        val matching = memoryManager.memories.value.find {
                            it.fact.contains(memoryAction.keyword, ignoreCase = true)
                        }
                        if (matching != null) {
                            memoryManager.removeMemory(matching.id)
                            _memoryUpdatedEvent.value = "Memory updated: Forgot '${matching.fact}'"
                        }
                    }
                }
            }
        }

        val imageBytesToSend = if (doc != null && doc.isImage) doc.imageBytes else null

        val isImage = doc != null && doc.isImage
        val promptResult = if (doc != null && !isImage) {
            com.teja.gemmmobile.ocr.DocumentIntelligenceEngine.buildDocumentPrompt(doc, rawInput)
        } else null

        val promptForGemma = when {
            isImage -> {
                if (rawInput.isBlank()) {
                    "What is in this image? Describe clearly."
                } else {
                    rawInput.trim()
                }
            }
            promptResult != null -> promptResult.promptForModel
            else -> rawInput
        }

        val userBubbleText = when {
            isImage -> rawInput.trim() // Clean: NO forced predefined text in user chat bubble!
            doc != null -> {
                val pageInfo = if (doc.pageCount > 1) " (${doc.pageCount} Pages)" else ""
                val cleanUserPrompt = if (rawInput.isNotBlank()) rawInput.trim() else "📄 Summarize document"
                "📄 **[${doc.fileName}]**$pageInfo\n\n$cleanUserPrompt"
            }
            else -> rawInput
        }

        val isContinue = isContinueRequest(rawInput) && !isImage
        val isDocument = doc != null && !doc.isImage
        val isPdfReq = isPdfRequest(rawInput) && !isImage && !isDocument
        val isVisualRequest = !isContinue && !isPdfReq && shouldQueryImages(rawInput) && !isImage && !isDocument
        val useWebSearch = !isContinue && !isPdfReq && (_isWebSearchEnabled.value || (shouldAutoSearch(rawInput) && !isDocument) || isVisualRequest) && !isImage && !isDocument

        // Direct device action execution (alarms, whatsapp, calendar, timers)
        val directDeviceIntentArgs = if (!isImage && doc == null) {
            com.teja.gemmmobile.tools.DeviceIntentsTool.parseIntentArguments(rawInput)
        } else null

        if (directDeviceIntentArgs != null) {
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.USER,
                text = rawInput
            )
            viewModelScope.launch {
                val execResult = skillRegistry.deviceIntentsTool.execute(directDeviceIntentArgs)
                val assistantMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    text = execResult.content,
                    isStreaming = false
                )
                updateSessionMessages(_currentSessionId.value) { it + userMsg + assistantMsg }
            }
            return
        }

        val userMessageId = UUID.randomUUID().toString()
        val savedImagePath = if (isImage && (doc.imageBytes != null || doc.previewBitmap != null)) {
            com.teja.gemmmobile.storage.StorageManagerHelper.compressAndSaveImageSync(
                context = getApplication(),
                previewBitmap = doc.previewBitmap,
                rawBytes = doc.imageBytes,
                messageId = userMessageId
            )
        } else null

        val userMessage = ChatMessage(
            id = userMessageId,
            role = MessageRole.USER,
            text = userBubbleText,
            imageBitmap = if (isImage) doc.previewBitmap else null,
            imagePath = savedImagePath,
            isImageAnalysis = isImage
        )
        val assistantMessageId = UUID.randomUUID().toString()
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            text = "",
            thoughtText = "",
            isStreaming = true,
            isThinking = _config.value.enableThinking && !useWebSearch && !isPdfReq,
            isSearchingWeb = useWebSearch,
            isImageAnalysis = isImage,
            isPdfDocumentRequest = isPdfReq,
            toolExecutionStatus = if (isPdfReq) "Compiling A4 PDF Document..." else if (useWebSearch) "AI formulating search query..." else null,
            citedPages = promptResult?.citedPages ?: emptyList(),
            sourceFileName = if (isDocument) doc.fileName else null
        )

        val currSessionId = _currentSessionId.value
        // If a generation is running for a different session, stop it so the model can process the new request
        if (activeGeneratingSessionId.value != null && activeGeneratingSessionId.value != currSessionId) {
            stopGeneration()
        }

        updateSessionMessages(currSessionId) { msgs ->
            msgs + userMessage + initialAssistantMessage
        }

        startGeneration(
            targetSessionId = currSessionId,
            prompt = promptForGemma,
            assistantMessageId = assistantMessageId,
            eng = eng,
            useWebSearch = useWebSearch,
            allowFallback = !useWebSearch && !isDocument,
            imageBytes = imageBytesToSend,
            isVisual = isVisualRequest,
            isDocument = isDocument
        )
    }



    private fun startGeneration(
        targetSessionId: String,
        prompt: String,
        assistantMessageId: String,
        eng: GemmaEngine,
        useWebSearch: Boolean,
        allowFallback: Boolean,
        imageBytes: ByteArray? = null,
        isVisual: Boolean = false,
        isDocument: Boolean = false
    ) {
        activeGenerationJob?.cancel()
        activeGeneratingSessionId.value = targetSessionId
        _isGenerating.value = (_currentSessionId.value == targetSessionId)

        activeGenerationJob = applicationScope.launch {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GemmaMobile:ResponseGeneration")
            try {
                wakeLock?.acquire(5 * 60 * 1000L) // 5 minutes max wake lock
            } catch (e: Exception) {
                Log.w(TAG, "[$TAG] Could not acquire WakeLock", e)
            }

            try {
                var searchResults: List<SearchResult> = emptyList()
                var searchImages: List<SearchImage> = emptyList()
                var searchContext = ""

                val sessionSnapshot = _sessions.value.find { it.id == targetSessionId }
                val targetMsgs = sessionSnapshot?.messages ?: _messages.value
                val hasDocInSession = isDocument || targetMsgs.any { it.text.contains("📄 **[") || it.isImageAnalysis }

                val cleanQuery = resolveSearchQuery(prompt, targetMsgs)
                val isProductRequest = isProductQuery(prompt) || isProductQuery(cleanQuery)

                val isVisualQuery = isVisual && imageBytes == null && !isDocument && !hasDocInSession
                val lowerPrompt = prompt.lowercase()
                val isExplicitPhotoRequest = lowerPrompt.contains("photo") ||
                        lowerPrompt.contains("image") ||
                        lowerPrompt.contains("picture") ||
                        lowerPrompt.contains("pic") ||
                        lowerPrompt.contains("diagram") ||
                        lowerPrompt.contains("chupinchu") ||
                        lowerPrompt.contains("chudu") ||
                        lowerPrompt.contains("bomma") ||
                        lowerPrompt.contains("wallpaper")
                val effectiveUseWebSearch = useWebSearch && !isDocument && !hasDocInSession

                if (effectiveUseWebSearch || isVisualQuery) {
                    if (isVisualQuery) {
                        val visualSearchQuery = searchManager.cleanVisualQuery(prompt)

                        // 1. Direct Visual Media Execution
                        updateSessionMessages(targetSessionId) { msgs ->
                            msgs.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(
                                    isSearchingWeb = true,
                                    isExecutingTool = true,
                                    toolExecutionStatus = "Searching images..."
                                ) else msg
                            }
                        }
                        try {
                            searchImages = kotlinx.coroutines.withTimeoutOrNull(8000L) {
                                searchManager.searchImages(visualSearchQuery, maxImages = 6)
                            } ?: emptyList()
                        } catch (e: Exception) {
                            Log.w(TAG, "[$TAG] Visual search error", e)
                        }

                        // Also fetch concise factual page snippets so model can explain the diagram/topic
                        try {
                            val enrichedResults = kotlinx.coroutines.withTimeoutOrNull(8000L) {
                                searchManager.searchAndRead(visualSearchQuery, maxResults = 3)
                            }
                            if (!enrichedResults.isNullOrEmpty()) {
                                searchResults = enrichedResults.map { it.toSearchResult() }
                                searchContext = searchManager.formatGemmaWebContext(enrichedResults)
                            }
                        } catch (_: Exception) {}
                    } else {
                        // 1. AI Formulates High-Yield Search Keywords First
                        updateSessionMessages(targetSessionId) { msgs ->
                            msgs.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(
                                    isSearchingWeb = true,
                                    isExecutingTool = true,
                                    toolExecutionStatus = "AI formulating search query..."
                                ) else msg
                            }
                        }

                        val searchTargetQuery = formulateAiSearchQuery(cleanQuery, eng)

                        // 2. Parallel Web Search + Product Image Execution with AI-formulated keywords
                        updateSessionMessages(targetSessionId) { msgs ->
                            msgs.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(
                                    toolExecutionStatus = if (isProductRequest) "Searching products: $searchTargetQuery" else "Searching web: \"$searchTargetQuery\"..."
                                ) else msg
                            }
                        }
                        try {
                            coroutineScope {
                                val searchJob = async {
                                    try {
                                        kotlinx.coroutines.withTimeoutOrNull(10000L) {
                                            searchManager.searchAndReadWithStatus(searchTargetQuery, maxResults = SearchConfig.DEFAULT_MAX_SEARCH_RESULTS, isProduct = isProductRequest)
                                        }
                                    } catch (t: Throwable) {
                                        Log.w(TAG, "[$TAG] Web search failed", t)
                                        null
                                    }
                                }

                                val shouldSearchImages = isExplicitPhotoRequest || isProductRequest

                                val imageJob = if (shouldSearchImages) {
                                    async {
                                        try {
                                            val baseVisualQ = searchManager.cleanVisualQuery(searchTargetQuery)
                                            val visualQ = if (isProductRequest && !baseVisualQ.lowercase().contains("product")) {
                                                "$baseVisualQ product packshot"
                                            } else {
                                                baseVisualQ
                                            }
                                            kotlinx.coroutines.withTimeoutOrNull(7000L) {
                                                searchManager.searchImages(visualQ, maxImages = 6)
                                            } ?: emptyList()
                                        } catch (_: Exception) {
                                            emptyList()
                                        }
                                    }
                                } else null

                                val searchResp = searchJob.await()
                                if (searchResp != null && searchResp.results.isNotEmpty()) {
                                    searchResults = searchResp.results.map { it.toSearchResult() }
                                    searchContext = searchManager.formatGemmaWebContext(searchResp.results)
                                    if (shouldSearchImages && searchResp.images.isNotEmpty()) {
                                        searchImages = searchResp.images
                                    }
                                }
                                if (shouldSearchImages && imageJob != null) {
                                    val fetchedImages = imageJob.await()
                                    if (searchImages.isEmpty()) {
                                        searchImages = fetchedImages
                                    } else if (fetchedImages.isNotEmpty()) {
                                        val seen = searchImages.map { it.imageUrl }.toMutableSet()
                                        val combined = searchImages.toMutableList()
                                        for (img in fetchedImages) {
                                            if (seen.add(img.imageUrl) && combined.size < 8) {
                                                combined.add(img)
                                            }
                                        }
                                        searchImages = combined
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            Log.w(TAG, "[$TAG] Web & product search failed", t)
                        }
                    }

                    updateSessionMessages(targetSessionId) { msgs ->
                        msgs.map { msg ->
                            if (msg.id == assistantMessageId) msg.copy(
                                searchResults = searchResults,
                                searchImages = searchImages,
                                isSearchingWeb = false,
                                isExecutingTool = false,
                                toolExecutionStatus = null,
                                isExplicitVisualRequest = isExplicitPhotoRequest || isVisualQuery
                            ) else msg
                        }
                    }
                }

                // Retrieve persistent memories and history using ContextManager
                val memoryContext = memoryManager.getFormattedMemoryPrompt()

                val baseSystemPrompt = _config.value.systemPrompt.ifBlank { GemmaConfig.DEFAULT_SYSTEM_PROMPT }
                val allowTools = !isDocument && !hasDocInSession && imageBytes == null && searchContext.isBlank()

                val lastAssistant = targetMsgs.lastOrNull { it.role == MessageRole.ASSISTANT && it.id != assistantMessageId && it.text.isNotBlank() }
                val isContinue = isContinueRequest(prompt)
                val isPdf = (isPdfRequest(prompt) || (isContinue && lastAssistant?.isPdfDocumentRequest == true)) && !isDocument && imageBytes == null
                val matchedSkill = if (isPdf) skillRegistry.skills.firstOrNull { it.id == "pdf-maker" } else skillRegistry.findMatchingSkill(prompt)
                val effectiveSystemPrompt = when {
                    imageBytes != null -> "You are a helpful assistant. Directly and accurately describe what is in the image in plain language."
                    isContinue -> {
                        "$baseSystemPrompt\n\n## CONTINUATION PROTOCOL\n" +
                        "The user asked to continue the previous explanation. Pick up immediately with the next point without any conversational filler, greetings, or repetition. Deliver the remaining content and wrap up with a natural conclusion."
                    }
                    isPdf -> {
                        "$baseSystemPrompt\n\n## AUTONOMOUS PUBLICATION-GRADE A4 DOCUMENT ENGINE\n" +
                        "The user requested complete study notes or document to be compiled directly into a PDF.\n" +
                        "CRITICAL ZERO-REFUSAL MANDATES:\n" +
                        "1. You are an autonomous document compiler engine. The application natively compiles your exact response directly into an on-device A4 PDF document.\n" +
                        "2. NEVER say 'I cannot create PDF files', 'As an AI...', or 'Here is the text to save as a PDF'. Output the structured document directly.\n" +
                        "3. Start IMMEDIATELY on line 1 with the main document title: '# <Title>' (e.g. '# Machine Learning: Comprehensive Guide').\n" +
                        "4. Organize into comprehensive sections ('## Section Title') and subsections ('### Sub-topic').\n" +
                        "5. Provide exhaustive, deep coverage: formal definitions, high-yield bullet points, core equations/formulas, comparison markdown tables, and summary takeaways.\n" +
                        "6. Absolutely NO conversational greetings, chatter, or sign-offs. Start directly at line 1 with '# <Title>'."
                    }
                    searchContext.isNotBlank() -> {
                        "$baseSystemPrompt\n\n" +
                        "Verified live web search results are provided above, numbered [1], [2], [3], etc.\n" +
                        "Answer the user's question clearly, accurately, and naturally based ONLY on the provided search results.\n" +
                        "When citing sources, use standard bracket numbers matching the search results (e.g. [1] or [2]).\n" +
                        "CRITICAL: NEVER fabricate, invent, or hallucinate URLs or links. Only state verified facts from the search results.\n" +
                        "Respond in the same language as the user's message (English or Telugu)."
                    }
                    matchedSkill != null -> {
                        "$baseSystemPrompt\n\n${matchedSkill.systemDirective}\n\n${toolRegistry.getToolsDocumentation()}\nWhen actions, alarms, QR codes, or external data are required, YOU MUST invoke a tool using JSON."
                    }
                    isFlowchartRequest(prompt) -> {
                        "$baseSystemPrompt\n\n## FLOWCHART & PROCESS DIAGRAM INSTRUCTION\n" +
                        "The user requested an explanation with a flowchart.\n" +
                        "1. Output a valid, clean Mermaid diagram code block:\n" +
                        "```mermaid\n" +
                        "graph TD\n" +
                        "    A[Start Step] --> B{Decision / Check?}\n" +
                        "    B -->|Yes| C[Process Action]\n" +
                        "    B -->|No| D[Alternative Action]\n" +
                        "```\n" +
                        "2. Below the diagram, explain each step thoroughly in natural, well-structured prose like ChatGPT."
                    }
                    allowTools -> {
                        "$baseSystemPrompt\n\n${toolRegistry.getToolsDocumentation()}\n" +
                        "When external facts, live web search, or products/links are needed, YOU MUST call a tool using JSON. Never say you cannot browse or provide links."
                    }
                    else -> baseSystemPrompt
                }

                val effectivePrompt = if (imageBytes != null) {
                    prompt
                } else {
                    val promptToUse = when {
                        isVisualQuery -> {
                            val visualSubject = searchManager.cleanVisualQuery(prompt).replace(Regex("""\s+diagram""", RegexOption.IGNORE_CASE), "").trim()
                            "Please provide a comprehensive explanation of the architecture, key components, and workflow of $visualSubject in detail. Structure your answer with clear headings and bullet points."
                        }
                        isPdf -> {
                            val cleanTopic = extractPdfTopic(prompt)
                            "Generate a comprehensive, publication-grade study document on: $cleanTopic.\n" +
                            "Output pure markdown starting with '# $cleanTopic: Complete Study Notes' on line 1, organized with clear section headings (##), detailed explanations, bullet points, comparison tables, and key takeaways."
                        }
                        isContinue -> {
                            val lastText = lastAssistant?.text?.trim() ?: ""
                            val tailSnippet = if (lastText.length > 300) lastText.takeLast(300) else lastText
                            if (tailSnippet.isNotBlank()) {
                                "## CONTINUATION DIRECTIVE\n" +
                                "The user requested to continue the previous response that ended at:\n" +
                                "\"\"\"\n...$tailSnippet\n\"\"\"\n\n" +
                                "INSTRUCTIONS:\n" +
                                "1. Pick up IMMEDIATELY from that exact point. Continue seamlessly with the very next word or sentence.\n" +
                                "2. DO NOT output conversational filler like 'Sure!', 'Certainly!', 'Continuing from where we left off...', or repeat any text already stated.\n" +
                                "3. Continue in the same language and tone, and finish with a natural conclusion."
                            } else {
                                "Please continue the previous explanation thoroughly and conclude."
                            }
                        }
                        else -> prompt
                    }
                    val historyTurns = targetMsgs.filter { it.id != assistantMessageId }.dropLast(1)
                    val allowToolsInPrompt = !isDocument && !hasDocInSession && searchContext.isBlank() && !isPdf
                    contextManager.buildPrompt(
                        systemPrompt = "", // Handled natively in ConversationConfig
                        toolsDocumentation = if (allowToolsInPrompt) toolRegistry.getToolsDocumentation() else "",
                        memoryContext = memoryContext,
                        conversationHistory = historyTurns,
                        currentPrompt = promptToUse,
                        searchContext = searchContext
                    )
                }

                val responseBuilder = StringBuilder()
                val thoughtBuilder = StringBuilder()
                var lastUiUpdateTime = 0L
                val UI_THROTTLE_MS = 100L // 10 updates/sec: smooth fluid streaming while freeing CPU cycles

                var nativeToolCallRequest: com.teja.gemmmobile.tools.ToolCallRequest? = null

                eng.sendMessage(
                    prompt = effectivePrompt,
                    systemInstruction = effectiveSystemPrompt,
                    imageBytes = imageBytes,
                    enableThinkingOverride = if (effectiveUseWebSearch || searchContext.isNotBlank() || isPdf) false else null,
                    dynamicMaxOutputOverride = when {
                        effectiveUseWebSearch || searchContext.isNotBlank() -> com.teja.gemmmobile.search.SearchConfig.MAX_WEB_OUTPUT_TOKENS
                        isPdf -> 3072
                        else -> 2048
                    }
                )
                    .catch { error ->
                        Log.e(TAG, "[$TAG] Stream error", error)
                        _errorMessage.value = "Generation failure: ${error.localizedMessage ?: "Unknown error"}"
                        updateSessionMessages(targetSessionId) { msgs ->
                            msgs.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(
                                        text = if (responseBuilder.isEmpty() && thoughtBuilder.isEmpty()) "Error: Failed to generate response." else responseBuilder.toString(),
                                        thoughtText = thoughtBuilder.toString(),
                                        isStreaming = false,
                                        isThinking = false,
                                        isSearchingWeb = false
                                    )
                                } else msg
                            }
                        }
                    }
                    .collect { chunk ->
                        if (chunk.toolCalls.isNotEmpty() && nativeToolCallRequest == null) {
                            val firstTc = chunk.toolCalls.first()
                            nativeToolCallRequest = com.teja.gemmmobile.tools.ToolCallRequest(
                                name = firstTc.name,
                                arguments = firstTc.arguments
                            )
                            Log.d(TAG, "[$TAG] Captured native LiteRT-LM tool call: ${firstTc.name}")
                        }

                        if (chunk.thought.isNotEmpty()) {
                            thoughtBuilder.append(chunk.thought)
                        }

                        val chunkText = chunk.text
                        if (chunkText.isNotEmpty()) {
                            val cleanChunk = chunkText.replace("<thought>", "").replace("</thought>", "")
                            if (cleanChunk.isNotEmpty()) {
                                responseBuilder.append(cleanChunk)
                            }
                        }

                        val now = System.currentTimeMillis()
                        // Throttle UI recompositions to avoid pegging the Main thread on every token
                        if (now - lastUiUpdateTime >= UI_THROTTLE_MS) {
                            val rawStreamingText = responseBuilder.toString()
                            val currentText = if (isPdf) stripPdfDisclaimers(rawStreamingText) else rawStreamingText
                            val currentThought = thoughtBuilder.toString()
                            val stillThinking = currentText.isEmpty() && currentThought.isNotEmpty()

                            if (_currentSessionId.value == targetSessionId) {
                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == assistantMessageId) {
                                        msg.copy(
                                            text = currentText,
                                            thoughtText = currentThought,
                                            isStreaming = true,
                                            isThinking = stillThinking,
                                            isSearchingWeb = false
                                        )
                                    } else msg
                                }
                            }
                        }
                    }

                val rawFinalText = responseBuilder.toString()
                val finalThought = thoughtBuilder.toString()

                // Bounded autonomous on-device tool calling loop (up to MAX_TOOL_STEPS)
                val MAX_TOOL_STEPS = com.teja.gemmmobile.search.SearchConfig.MAX_TOOL_STEPS
                var currentToolStep = 0
                var currentGenerationText = responseBuilder.toString()
                val accumulatedToolResults = StringBuilder()
                val calledToolsHistory = mutableListOf<com.teja.gemmmobile.tools.ToolCallRequest>()
                var currentSearchImages = emptyList<SearchImage>()

                val allowToolCalls = !isDocument && !hasDocInSession && imageBytes == null
                while (allowToolCalls && currentToolStep < MAX_TOOL_STEPS) {
                    var detectedToolCall = nativeToolCallRequest ?: toolRegistry.parseToolCall(currentGenerationText)
                    nativeToolCallRequest = null // consume once

                    // Autonomous Agent Loop Fallback: If model refused or query asked for visuals and no tool has run yet
                    if (detectedToolCall == null && currentToolStep == 0) {
                        if (isRefusalToBrowse(currentGenerationText) || isRefusalToSearchImages(currentGenerationText)) {
                            val fallbackQuery = resolveSearchQuery(prompt, targetMsgs)
                            val toolName = if (shouldQueryImages(prompt) || isVisual) "image_search" else "web_search"
                            Log.i(TAG, "[$TAG] Agent loop intercepted model refusal! Autonomous execution of $toolName for '$fallbackQuery'")
                            detectedToolCall = com.teja.gemmmobile.tools.ToolCallRequest(
                                name = toolName,
                                arguments = mapOf("query" to fallbackQuery)
                            )
                        } else if ((shouldQueryImages(prompt) || isVisual) && searchImages.isEmpty() && currentSearchImages.isEmpty()) {
                            val visualQ = searchManager.cleanVisualQuery(prompt)
                            if (visualQ.isNotBlank()) {
                                Log.i(TAG, "[$TAG] Agent loop triggered image_search for visual request '$visualQ'")
                                detectedToolCall = com.teja.gemmmobile.tools.ToolCallRequest(
                                    name = "image_search",
                                    arguments = mapOf("query" to visualQ)
                                )
                            }
                        }
                    }

                    if (detectedToolCall == null) break

                    // Duplicate tool-call and infinite loop protection
                    if (toolRegistry.isDuplicateOrLoop(calledToolsHistory, detectedToolCall)) {
                        Log.w(TAG, "[$TAG] Loop/duplicate detected for tool '${detectedToolCall.name}'. Breaking tool loop.")
                        break
                    }
                    calledToolsHistory.add(detectedToolCall)

                    // Re-route to image_search if the query asks for images, photos, or diagrams
                    val toolArgs = detectedToolCall.arguments
                    val toolQuery = (toolArgs["query"] as? String) ?: prompt
                    if (detectedToolCall.name == "web_search" && (shouldQueryImages(toolQuery) || shouldQueryImages(prompt))) {
                        Log.i(TAG, "[$TAG] Re-routing visual tool call to 'image_search'")
                        detectedToolCall = com.teja.gemmmobile.tools.ToolCallRequest("image_search", toolArgs)
                    }

                    currentToolStep++
                    Log.d(TAG, "[$TAG] Step $currentToolStep: Tool call detected: ${detectedToolCall.name} with ${detectedToolCall.arguments}")

                    val preToolText = stripAssumedToolText(currentGenerationText)
                    updateSessionMessages(targetSessionId) { msgs ->
                        msgs.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    text = if (isRefusalToBrowse(currentGenerationText)) "" else preToolText,
                                    isExecutingTool = true,
                                    toolExecutionStatus = if (detectedToolCall.name == "image_search") "Searching images..." else "Using ${detectedToolCall.name}...",
                                    isStreaming = false
                                )
                            } else msg
                        }
                    }

                    val toolResult = toolRegistry.execute(detectedToolCall.name, detectedToolCall.arguments)
                    accumulatedToolResults.appendLine("[Tool: ${detectedToolCall.name}]\n${toolResult.content}\n")

                    if (detectedToolCall.name == "web_search" && toolResult.data is List<*>) {
                        val newSearchResults = toolResult.data.filterIsInstance<SearchResult>()
                        if (newSearchResults.isNotEmpty()) {
                            searchResults = newSearchResults
                        }
                    } else if (detectedToolCall.name == "image_search" && toolResult.data is List<*>) {
                        val newImages = toolResult.data.filterIsInstance<SearchImage>()
                        if (newImages.isNotEmpty()) {
                            currentSearchImages = newImages
                        }
                    }

                    // Feed tool result back to the model as evidence (NOT in system prompt to prevent prompt injection)
                    val isImageTool = detectedToolCall.name == "image_search"
                    val nextTurnSubject = if (isImageTool) {
                        (detectedToolCall.arguments["query"] as? String)?.let { searchManager.cleanVisualQuery(it) } ?: cleanQuery
                    } else {
                        cleanQuery
                    }

                    val nextTurnSystemPrompt = "$baseSystemPrompt\n\n" +
                        "Live web results are provided below as factual evidence, numbered [1], [2], etc. Answer the user's question clearly, accurately, and directly based on this information. " +
                        "When citing facts, reference the source numbers in brackets (e.g. [1] or [2]). NEVER fabricate URLs or links. Do not call any further tools."

                    val nextTurnPrompt = contextManager.buildPrompt(
                        systemPrompt = nextTurnSystemPrompt,
                        toolsDocumentation = if (isImageTool) "" else if (currentToolStep < MAX_TOOL_STEPS) toolRegistry.getToolsDocumentation() else "",
                        memoryContext = memoryContext,
                        conversationHistory = targetMsgs.filter { it.id != assistantMessageId },
                        currentPrompt = if (isImageTool) "Explain the architecture, components, and workflow of $nextTurnSubject in detail based on the images above." else prompt,
                        toolResultsContext = accumulatedToolResults.toString().take(4000)
                    )

                    val nextTurnBuilder = StringBuilder()
                    updateSessionMessages(targetSessionId) { msgs ->
                        msgs.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(
                                    searchResults = searchResults,
                                    searchImages = if (currentSearchImages.isNotEmpty()) currentSearchImages else if (searchImages.isNotEmpty()) searchImages else msg.searchImages,
                                    isExecutingTool = false,
                                    toolExecutionStatus = null,
                                    isStreaming = true,
                                    isExplicitVisualRequest = isExplicitPhotoRequest || isVisualQuery
                                )
                            } else msg
                        }
                    }

                    try {
                        var toolUiUpdate = 0L
                        eng.sendMessage(
                            prompt = nextTurnPrompt,
                            systemInstruction = effectiveSystemPrompt,
                            dynamicMaxOutputOverride = com.teja.gemmmobile.search.SearchConfig.MAX_WEB_OUTPUT_TOKENS
                        ).collect { chunk ->
                            if (chunk.thought.isNotEmpty()) thoughtBuilder.append(chunk.thought)
                            val clean = chunk.text.replace("<thought>", "").replace("</thought>", "")
                            if (clean.isNotEmpty()) {
                                nextTurnBuilder.append(clean)
                                val now = System.currentTimeMillis()
                                if (now - toolUiUpdate >= UI_THROTTLE_MS) {
                                    toolUiUpdate = now
                                    if (_currentSessionId.value == targetSessionId) {
                                        _messages.value = _messages.value.map { msg ->
                                            if (msg.id == assistantMessageId) {
                                                msg.copy(text = stripAssumedToolText(nextTurnBuilder.toString()), isStreaming = true)
                                            } else msg
                                        }
                                    }
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "[$TAG] Follow-up tool synthesis error at step $currentToolStep", t)
                    }

                    currentGenerationText = nextTurnBuilder.toString()
                    if (currentGenerationText.isNotBlank()) {
                        responseBuilder.clear()
                        responseBuilder.append(currentGenerationText)
                    } else if (responseBuilder.isEmpty() && toolResult.content.isNotBlank()) {
                        responseBuilder.append(toolResult.content)
                    }
                }

                // Autonomous auto-continuation loop for PDF document requests
                if (isPdf && isTruncatedOrCutoff(responseBuilder.toString())) {
                    var autoContinuePass = 0
                    val maxAutoContinuePasses = 2
                    while (autoContinuePass < maxAutoContinuePasses && isTruncatedOrCutoff(responseBuilder.toString())) {
                        autoContinuePass++
                        Log.i(TAG, "[$TAG] Autonomous auto-continue for PDF triggered (pass $autoContinuePass)")
                        val currentStitched = responseBuilder.toString().trim()
                        val tailSnippet = currentStitched.takeLast(250).trim()

                        updateSessionMessages(targetSessionId) { msgs ->
                            msgs.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(
                                        toolExecutionStatus = "Completing notes (continuing section $autoContinuePass)...",
                                        isStreaming = true
                                    )
                                } else msg
                            }
                        }

                        val contPrompt = contextManager.buildPrompt(
                            systemPrompt = effectiveSystemPrompt,
                            conversationHistory = emptyList(),
                            currentPrompt = "## CONTINUATION DIRECTIVE\n" +
                                "You were generating a comprehensive study document, but generation reached token capacity and cut off at:\n" +
                                "\"\"\"\n...$tailSnippet\n\"\"\"\n\n" +
                                "CRITICAL INSTRUCTIONS:\n" +
                                "1. Pick up IMMEDIATELY from that exact point. Continue seamlessly with the very next word or sentence.\n" +
                                "2. DO NOT output conversational filler ('Sure!', 'Continuing...'). DO NOT repeat any prior text.\n" +
                                "3. Finish all remaining topics, comparisons, and key takeaways thoroughly."
                        )

                        val contBuilder = StringBuilder()
                        var contUiUpdate = 0L
                        try {
                            eng.sendMessage(
                                prompt = contPrompt,
                                systemInstruction = effectiveSystemPrompt,
                                dynamicMaxOutputOverride = 2500
                            ).collect { chunk ->
                                val clean = chunk.text.replace("<thought>", "").replace("</thought>", "")
                                if (clean.isNotEmpty()) {
                                    contBuilder.append(clean)
                                    val now = System.currentTimeMillis()
                                    if (now - contUiUpdate >= UI_THROTTLE_MS) {
                                        contUiUpdate = now
                                        val stitchedPreview = stitchContinuation(currentStitched, contBuilder.toString())
                                        val displayPreview = stripPdfDisclaimers(stitchedPreview)
                                        if (_currentSessionId.value == targetSessionId) {
                                            _messages.value = _messages.value.map { msg ->
                                                if (msg.id == assistantMessageId) {
                                                    msg.copy(
                                                        text = displayPreview,
                                                        isStreaming = true,
                                                        toolExecutionStatus = "Completing notes (continuing section $autoContinuePass)..."
                                                    )
                                                } else msg
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            Log.w(TAG, "[$TAG] PDF auto-continuation pass $autoContinuePass error", t)
                            break
                        }

                        val contText = contBuilder.toString()
                        if (contText.isNotBlank()) {
                            val newlyStitched = stitchContinuation(currentStitched, contText)
                            responseBuilder.clear()
                            responseBuilder.append(newlyStitched)
                        } else {
                            break
                        }
                    }
                }

                val finalText = responseBuilder.toString()

                // Finalize assistant message and update session history once
                val cleanedFinal = stripAssumedToolText(finalText)
                val cleanedRaw = stripAssumedToolText(rawFinalText)
                val resolvedText = when {
                    cleanedFinal.isNotBlank() -> cleanedFinal
                    cleanedRaw.isNotBlank() -> cleanedRaw
                    finalThought.isNotBlank() -> finalThought
                    searchResults.isNotEmpty() -> {
                        // Fallback synthesis directly from search results if model silently stopped or choked on prompt
                        buildString {
                            appendLine("Based on search results, here is an overview of **$cleanQuery**:\n")
                            searchResults.take(3).forEach { res ->
                                val cleanTitle = res.title.substringBefore("-").substringBefore("|").trim()
                                appendLine("• **$cleanTitle**: ${res.snippet.trim()}")
                            }
                            appendLine()
                            appendLine("Please let me know if you would like more details!")
                        }.trim()
                    }
                    currentSearchImages.isNotEmpty() || searchImages.isNotEmpty() -> {
                        val visualSubject = searchManager.cleanVisualQuery(prompt).replace(Regex("""\s+diagram""", RegexOption.IGNORE_CASE), "").trim()
                        "Here are the diagrams and visual figures for **$visualSubject**."
                    }
                    else -> "I couldn't generate a response. Please try again with a shorter prompt."
                }

                val sanitizedResolved = if (isPdf) stripPdfDisclaimers(resolvedText) else resolvedText
                val finalTextToDisplay = sanitizedResolved.ifBlank { resolvedText }
                var autoPdfPath: String? = null
                var autoPdfTitle: String? = null
                var autoPdfPages = 0

                if (isPdf && finalTextToDisplay.isNotBlank()) {
                    try {
                        val markdownForPdf = if (isContinue && lastAssistant?.text?.isNotBlank() == true) {
                            stitchContinuation(lastAssistant.text, finalTextToDisplay)
                        } else {
                            finalTextToDisplay
                        }
                        val pdfResult = com.teja.gemmmobile.pdf.PdfExporter.generatePdf(
                            context = getApplication(),
                            markdownText = markdownForPdf,
                            title = lastAssistant?.generatedPdfTitle
                        )
                        autoPdfPath = pdfResult.file.absolutePath
                        autoPdfTitle = pdfResult.title
                        autoPdfPages = pdfResult.pageCount
                        Log.i(TAG, "[$TAG] Auto PDF generated successfully: $autoPdfPath with $autoPdfPages pages")
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "📄 PDF Ready! ($autoPdfPages Pages) Tap 'Open PDF' to view", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[$TAG] Auto PDF generation error", e)
                    }
                }

                val cutoff = !isPdf && isTruncatedOrCutoff(finalTextToDisplay)
                updateSessionMessages(targetSessionId) { msgs ->
                    msgs.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                text = finalTextToDisplay,
                                thoughtText = if (finalTextToDisplay == finalThought) "" else finalThought,
                                searchResults = searchResults,
                                searchImages = if (currentSearchImages.isNotEmpty()) currentSearchImages else if (searchImages.isNotEmpty()) searchImages else msg.searchImages,
                                isStreaming = false,
                                isThinking = false,
                                isSearchingWeb = false,
                                isExecutingTool = false,
                                toolExecutionStatus = null,
                                isPdfDocumentRequest = isPdf,
                                isExplicitVisualRequest = isExplicitPhotoRequest || isVisualQuery,
                                isCutOff = cutoff,
                                generatedPdfPath = autoPdfPath,
                                generatedPdfTitle = autoPdfTitle,
                                generatedPdfPageCount = autoPdfPages
                            )
                        } else msg
                    }
                }

                // Persist completed session to storage
                val completedSession = _sessions.value.find { it.id == targetSessionId }
                if (completedSession != null) {
                    chatStorage.saveSession(completedSession)
                }

            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "[$TAG] Response generation cancelled")
                throw cancellation
            } catch (t: Throwable) {
                Log.e(TAG, "[$TAG] Error during generation", t)
                val errorText = when {
                    t.message?.contains("Context budget") == true -> "Context limit reached. Please start a new chat."
                    t.message?.contains("Out of memory") == true -> "Device memory is low. Please close background apps and try again."
                    else -> "Unable to complete response: ${t.localizedMessage ?: "Unknown error"}"
                }
                updateSessionMessages(targetSessionId) { msgs ->
                    msgs.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(
                                text = errorText,
                                isStreaming = false,
                                isThinking = false,
                                isSearchingWeb = false,
                                isExecutingTool = false,
                                toolExecutionStatus = null
                            )
                        } else msg
                    }
                }
            } finally {
                if (activeGeneratingSessionId.value == targetSessionId) {
                    activeGeneratingSessionId.value = null
                    if (_currentSessionId.value == targetSessionId) {
                        _isGenerating.value = false
                    }
                }
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun createNewChat() {
        persistChat()

        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _messages.value = emptyList()
        _inputText.value = ""
        _isGenerating.value = false
        // Notice: Do NOT cancel activeGenerationJob! Previous chat keeps generating in background
    }

    fun refreshSessions(targetSessionId: String? = null) {
        viewModelScope.launch {
            val loaded = chatStorage.loadSessions()
            _sessions.value = loaded
            if (targetSessionId != null) {
                val target = loaded.find { it.id == targetSessionId }
                if (target != null) {
                    _currentSessionId.value = target.id
                    _messages.value = target.messages
                    _isGenerating.value = (activeGeneratingSessionId.value == target.id)
                    return@launch
                }
            }
            if (_sessions.value.none { it.id == _currentSessionId.value } && loaded.isNotEmpty()) {
                val latest = loaded.first()
                _currentSessionId.value = latest.id
                _messages.value = latest.messages
            }
        }
    }

    fun selectSession(sessionId: String) {
        persistChat()
        val inMemory = _sessions.value.find { it.id == sessionId }
        if (inMemory != null) {
            _currentSessionId.value = inMemory.id
            _messages.value = inMemory.messages
            _inputText.value = ""
            _isGenerating.value = (activeGeneratingSessionId.value == inMemory.id)
        } else {
            refreshSessions(targetSessionId = sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        if (activeGeneratingSessionId.value == sessionId) {
            stopGeneration()
        }
        val sessionToDelete = _sessions.value.find { it.id == sessionId }
        if (sessionToDelete != null) {
            viewModelScope.launch(Dispatchers.IO) {
                com.teja.gemmmobile.storage.StorageManagerHelper.deleteSessionImages(sessionToDelete.messages)
                refreshStorageUsage()
            }
        }
        viewModelScope.launch {
            val updated = chatStorage.deleteSession(sessionId)
            _sessions.value = updated
            if (_currentSessionId.value == sessionId) {
                if (updated.isNotEmpty()) {
                    val next = updated.first()
                    _currentSessionId.value = next.id
                    _messages.value = next.messages
                    _isGenerating.value = (activeGeneratingSessionId.value == next.id)
                } else {
                    val newId = UUID.randomUUID().toString()
                    _currentSessionId.value = newId
                    _messages.value = emptyList()
                    _isGenerating.value = false
                }
            }
        }
    }

    fun deleteCurrentChat() {
        deleteSession(_currentSessionId.value)
    }

    fun clearAllChats() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
        _messages.value = emptyList()
        _sessions.value = emptyList()
        val newId = UUID.randomUUID().toString()
        _currentSessionId.value = newId
        viewModelScope.launch {
            chatStorage.clearAllSessions()
            withContext(Dispatchers.IO) {
                com.teja.gemmmobile.storage.StorageManagerHelper.deleteAllImages(getApplication())
                refreshStorageUsage()
            }
            engine?.clearConversation()
        }
    }

    private val _storageUsageText = MutableStateFlow<String>("0 KB")
    val storageUsageText: StateFlow<String> = _storageUsageText.asStateFlow()

    fun refreshStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val imgSize = com.teja.gemmmobile.storage.StorageManagerHelper.getChatImagesSizeBytes(getApplication())
            val cacheSize = com.teja.gemmmobile.storage.StorageManagerHelper.getCacheSizeBytes(getApplication())
            val total = imgSize + cacheSize
            _storageUsageText.value = com.teja.gemmmobile.storage.StorageManagerHelper.formatBytes(total)
        }
    }

    fun clearCacheAndTempFiles(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = com.teja.gemmmobile.storage.StorageManagerHelper.clearCache(getApplication(), _sessions.value)
            val freedStr = com.teja.gemmmobile.storage.StorageManagerHelper.formatBytes(freed)
            refreshStorageUsage()
            withContext(Dispatchers.Main) {
                onComplete(freedStr)
            }
        }
    }

    fun clearChat() {
        deleteCurrentChat()
    }

    fun renameCurrentChat(newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updated = chatStorage.renameSession(_currentSessionId.value, newTitle)
            _sessions.value = updated
        }
    }

    fun regenerateLastResponse() {
        val msgs = _messages.value
        val lastUserIndex = msgs.indexOfLast { it.role == MessageRole.USER }
        if (lastUserIndex == -1) return
        val userMsg = msgs[lastUserIndex]

        activeGenerationJob?.cancel()
        _isGenerating.value = false

        // Keep all messages up to the last user message
        val prunedMessages = msgs.subList(0, lastUserIndex + 1)
        val eng = engine ?: return

        val newAssistantId = UUID.randomUUID().toString()
        val newAssistantMessage = ChatMessage(
            id = newAssistantId,
            role = MessageRole.ASSISTANT,
            text = "",
            isStreaming = true,
            isThinking = _config.value.enableThinking,
            isSearchingWeb = _isWebSearchEnabled.value,
            isImageAnalysis = userMsg.isImageAnalysis
        )

        val currSessionId = _currentSessionId.value
        updateSessionMessages(currSessionId) { prunedMessages + newAssistantMessage }
        _isGenerating.value = true

        val imageBytesToSend = if (userMsg.isImageAnalysis && userMsg.imagePath != null) {
            try { java.io.File(userMsg.imagePath).readBytes() } catch (_: Exception) { null }
        } else null

        val prompt = if (userMsg.text.isNotBlank()) userMsg.text else "What is in this image? Describe clearly."

        val useSearch = (_isWebSearchEnabled.value || shouldAutoSearch(prompt)) && imageBytesToSend == null

        val isVisual = shouldQueryImages(prompt) && imageBytesToSend == null
        startGeneration(
            targetSessionId = currSessionId,
            prompt = prompt,
            assistantMessageId = newAssistantId,
            eng = eng,
            useWebSearch = useSearch,
            allowFallback = !useSearch,
            imageBytes = imageBytesToSend,
            isVisual = isVisual,
            isDocument = false
        )
    }

    fun editAndResendMessage(messageId: String, newText: String) {
        val msgs = _messages.value
        val targetIndex = msgs.indexOfFirst { it.id == messageId }
        if (targetIndex == -1) return
        val originalMsg = msgs[targetIndex]

        activeGenerationJob?.cancel()
        _isGenerating.value = false

        // Keep messages strictly before the target message
        val prefixMessages = msgs.subList(0, targetIndex)
        val updatedUserMsg = originalMsg.copy(text = newText)

        val eng = engine ?: return
        val newAssistantId = UUID.randomUUID().toString()
        val imageBytesToSend = if (updatedUserMsg.isImageAnalysis && updatedUserMsg.imagePath != null) {
            try { java.io.File(updatedUserMsg.imagePath).readBytes() } catch (_: Exception) { null }
        } else null
        val useSearch = (_isWebSearchEnabled.value || shouldAutoSearch(newText)) && imageBytesToSend == null

        val newAssistantMessage = ChatMessage(
            id = newAssistantId,
            role = MessageRole.ASSISTANT,
            text = "",
            isStreaming = true,
            isThinking = _config.value.enableThinking,
            isSearchingWeb = useSearch,
            isImageAnalysis = updatedUserMsg.isImageAnalysis
        )

        val currSessionId = _currentSessionId.value
        updateSessionMessages(currSessionId) { prefixMessages + updatedUserMsg + newAssistantMessage }
        _isGenerating.value = true

        val isVisual = shouldQueryImages(newText) && imageBytesToSend == null
        startGeneration(
            targetSessionId = currSessionId,
            prompt = newText,
            assistantMessageId = newAssistantId,
            eng = eng,
            useWebSearch = useSearch,
            allowFallback = !useSearch,
            imageBytes = imageBytesToSend,
            isVisual = isVisual,
            isDocument = false
        )
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    /** Retries the last turn when possible, or restarts the model if it is unavailable. */
    fun retryLastResponse() {
        if (_isGenerating.value) return
        if (engine == null || _engineState.value !is EngineState.Ready) {
            initializeEngine()
            return
        }
        dismissError()
        regenerateLastResponse()
    }

    /**
     * Seamlessly continues generating from where the assistant message cut off, stitching the continuation
     * directly into the existing message bubble and updating attached PDF documents.
     */
    fun continueGeneration(messageId: String) {
        val eng = engine ?: run {
            _errorMessage.value = "Engine not loaded"
            return
        }
        if (_isGenerating.value) return

        val targetSessionId = _currentSessionId.value
        val sessionSnapshot = _sessions.value.find { it.id == targetSessionId } ?: return
        val targetMsg = sessionSnapshot.messages.find { it.id == messageId } ?: return
        if (targetMsg.role != MessageRole.ASSISTANT || targetMsg.text.isBlank()) return

        val originalText = targetMsg.text.trim()
        val tailSnippet = originalText.takeLast(250).trim()
        val isPdf = targetMsg.isPdfDocumentRequest || targetMsg.generatedPdfPath != null

        _isGenerating.value = true
        activeGeneratingSessionId.value = targetSessionId

        updateSessionMessages(targetSessionId) { msgs ->
            msgs.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        isStreaming = true,
                        isCutOff = false,
                        toolExecutionStatus = if (isPdf) "Continuing & compiling PDF..." else "Continuing generation..."
                    )
                } else msg
            }
        }

        activeGenerationJob = applicationScope.launch {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GemmaMobile:ContinueGen")
            try { wakeLock?.acquire(5 * 60 * 1000L) } catch (_: Exception) {}

            try {
                val continuePrompt = "## CONTINUATION DIRECTIVE\n" +
                    "The previous response cut off at:\n" +
                    "\"\"\"\n...$tailSnippet\n\"\"\"\n\n" +
                    "CRITICAL INSTRUCTIONS:\n" +
                    "1. Pick up IMMEDIATELY from that exact point. Continue seamlessly with the very next word or sentence.\n" +
                    "2. DO NOT output conversational filler ('Sure!', 'Continuing...', etc.) and DO NOT repeat any prior text.\n" +
                    "3. Finish the explanation thoroughly and conclude naturally."

                val effectivePrompt = contextManager.buildPrompt(
                    systemPrompt = GemmaConfig.DEFAULT_SYSTEM_PROMPT,
                    conversationHistory = emptyList(),
                    currentPrompt = continuePrompt
                )

                val continuationBuilder = StringBuilder()
                var lastContUpdate = 0L
                val UI_THROTTLE_MS = 100L

                eng.sendMessage(
                    prompt = effectivePrompt,
                    dynamicMaxOutputOverride = if (isPdf) 3072 else 2048
                ).collect { chunk ->
                    val clean = chunk.text.replace("<thought>", "").replace("</thought>", "")
                    if (clean.isNotEmpty()) {
                        continuationBuilder.append(clean)
                        val now = System.currentTimeMillis()
                        if (now - lastContUpdate >= UI_THROTTLE_MS) {
                            lastContUpdate = now
                            val stitchedPreview = stitchContinuation(originalText, continuationBuilder.toString())
                            val displayPreview = if (isPdf) stripPdfDisclaimers(stitchedPreview) else stitchedPreview
                            if (_currentSessionId.value == targetSessionId) {
                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == messageId) {
                                        msg.copy(text = displayPreview, isStreaming = true)
                                    } else msg
                                }
                            }
                        }
                    }
                }

                val stitchedFinal = stitchContinuation(originalText, continuationBuilder.toString())
                val resolvedFinal = if (isPdf) stripPdfDisclaimers(stitchedFinal) else stitchedFinal
                val stillCutOff = isTruncatedOrCutoff(resolvedFinal)

                var updatedPdfPath = targetMsg.generatedPdfPath
                var updatedPdfTitle = targetMsg.generatedPdfTitle
                var updatedPdfPages = targetMsg.generatedPdfPageCount

                if (isPdf && resolvedFinal.isNotBlank()) {
                    try {
                        val pdfResult = com.teja.gemmmobile.pdf.PdfExporter.generatePdf(
                            context = getApplication(),
                            markdownText = resolvedFinal,
                            title = updatedPdfTitle
                        )
                        updatedPdfPath = pdfResult.file.absolutePath
                        updatedPdfTitle = pdfResult.title
                        updatedPdfPages = pdfResult.pageCount
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "📄 PDF Updated with complete notes!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[$TAG] Error regenerating PDF after continuation", e)
                    }
                }

                updateSessionMessages(targetSessionId) { msgs ->
                    msgs.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(
                                text = resolvedFinal,
                                isStreaming = false,
                                isCutOff = stillCutOff,
                                toolExecutionStatus = null,
                                generatedPdfPath = updatedPdfPath,
                                generatedPdfTitle = updatedPdfTitle,
                                generatedPdfPageCount = updatedPdfPages
                            )
                        } else msg
                    }
                }

                _sessions.value.find { it.id == targetSessionId }?.let { chatStorage.saveSession(it) }
            } catch (t: Throwable) {
                Log.e(TAG, "[$TAG] Error during continueGeneration", t)
                updateSessionMessages(targetSessionId) { msgs ->
                    msgs.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(isStreaming = false, toolExecutionStatus = null)
                        } else msg
                    }
                }
            } finally {
                _isGenerating.value = false
                activeGeneratingSessionId.value = null
                try { wakeLock?.release() } catch (_: Exception) {}
            }
        }
    }

    fun generatePdfForMessage(messageId: String, text: String, title: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanText = stripPdfDisclaimers(text)
                val result = com.teja.gemmmobile.pdf.PdfExporter.generatePdf(
                    context = getApplication(),
                    markdownText = cleanText.ifBlank { text },
                    title = title
                )
                updateSessionMessages(_currentSessionId.value) { msgs ->
                    msgs.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(
                                text = cleanText.ifBlank { msg.text },
                                generatedPdfPath = result.file.absolutePath,
                                generatedPdfTitle = result.title,
                                generatedPdfPageCount = result.pageCount,
                                isPdfDocumentRequest = true
                            )
                        } else msg
                    }
                }
                withContext(Dispatchers.Main) {
                    com.teja.gemmmobile.pdf.PdfExporter.openPdf(getApplication(), result.file)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$TAG] Error generating PDF for message $messageId", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeGenerationJob?.cancel()
        ttsManager.release()
    }
}
