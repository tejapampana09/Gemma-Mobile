package com.teja.gemmmobile.tools

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.teja.gemmmobile.search.SearchConfig
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ToolRegistry"

/**
 * Result of validating tool arguments against its declared schema.
 */
data class ArgumentValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Central registry, schema validator, and invocation dispatcher for all on-device tools.
 */
class ToolRegistry {

    private val tools = ConcurrentHashMap<String, GemmaTool>()
    private val gson = Gson()

    fun register(tool: GemmaTool) {
        tools[tool.name] = tool
        Log.d(TAG, "[$TAG] Registered tool: ${tool.name}")
    }

    fun resolveToolName(name: String): String {
        return when (name.lowercase().trim()) {
            "web_search", "websearch", "search", "search_web", "google_search", "bing_search", "duckduckgo_search" -> "web_search"
            "image_search", "imagesearch", "image", "images", "search_images", "visual_search" -> "image_search"
            "manage_memory", "memory_store", "memory", "store_memory", "remember", "save_memory" -> "manage_memory"
            "ocr", "ocr_tool", "scan_document" -> "ocr"
            else -> name
        }
    }

    fun get(name: String): GemmaTool? {
        val resolvedName = resolveToolName(name)
        return tools[resolvedName]
    }

    fun getAll(): List<GemmaTool> = tools.values.toList()

    /**
     * Generates concise tool documentation for the model to minimize KV cache footprint.
     */
    fun getToolsDocumentation(): String {
        if (tools.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("## Autonomous Tools")
        sb.appendLine("You have real-time access to tools. When the user asks for images, photos, diagrams, or visual examples, YOU MUST call `image_search`.")
        sb.appendLine("When the user asks for current facts, news, or web research, call `web_search`.")
        sb.appendLine("To call a tool, output the tool call in this exact JSON format:")
        sb.appendLine("```json")
        sb.appendLine("{\"name\": \"image_search\", \"arguments\": {\"query\": \"<search keywords>\"}}")
        sb.appendLine("```")
        sb.appendLine("- `image_search`: Search and display web photos, diagrams, architecture visuals, wallpapers, or images (arguments: {\"query\": \"...\"})")
        sb.appendLine("- `web_search`: Search live web for facts, recent events, websites, or articles (arguments: {\"query\": \"...\"})")
        sb.appendLine("- `manage_memory`: Save user personal preferences or facts (arguments: {\"action\": \"save\", \"fact\": \"...\"})")
        sb.appendLine("Never say you cannot browse or cannot show images. Call the appropriate tool immediately.")
        return sb.toString().trim()
    }

    /**
     * Validates arguments against required schema rules before executing the tool.
     */
    fun validateArguments(tool: GemmaTool, arguments: Map<String, Any?>): ArgumentValidationResult {
        return try {
            val schemaElement = JsonParser.parseString(tool.parametersJsonSchema)
            if (!schemaElement.isJsonObject) return ArgumentValidationResult(true)

            val schemaObj = schemaElement.asJsonObject
            val requiredProps = schemaObj.getAsJsonArray("required")

            if (requiredProps != null) {
                for (elem in requiredProps) {
                    val reqField = elem.asString
                    val value = arguments[reqField]
                    if (value == null || (value is String && value.isBlank())) {
                        return ArgumentValidationResult(
                            isValid = false,
                            errorMessage = "Missing required argument '$reqField'"
                        )
                    }
                }
            }

            // String size limits check
            for ((key, value) in arguments) {
                if (value is String && value.length > SearchConfig.MAX_TOOL_ARG_STRING_CHARS) {
                    return ArgumentValidationResult(
                        isValid = false,
                        errorMessage = "Argument '$key' exceeds length limit of ${SearchConfig.MAX_TOOL_ARG_STRING_CHARS} characters"
                    )
                }
            }

            ArgumentValidationResult(isValid = true)
        } catch (e: Exception) {
            // Permissive fallback if schema parsing fails
            ArgumentValidationResult(isValid = true)
        }
    }

    /**
     * Executes a tool by name with arguments, enforcing validation and catching all errors.
     */
    suspend fun execute(name: String, arguments: Map<String, Any?>): ToolResult {
        val resolvedName = resolveToolName(name)
        val tool = tools[resolvedName] ?: return ToolResult.failure("Unknown tool: '$name'")

        // Normalize argument keys
        val normalizedArgs = mutableMapOf<String, Any?>()
        normalizedArgs.putAll(arguments)

        if (resolvedName == "web_search" || resolvedName == "image_search") {
            if (!normalizedArgs.containsKey("query")) {
                val candidateQuery = normalizedArgs["q"] ?: normalizedArgs["search_query"] ?: normalizedArgs["input"] ?: normalizedArgs["keywords"]
                if (candidateQuery != null) {
                    normalizedArgs["query"] = candidateQuery
                }
            }
        } else if (resolvedName == "manage_memory") {
            if (!normalizedArgs.containsKey("action")) {
                normalizedArgs["action"] = "save"
            }
            if (!normalizedArgs.containsKey("fact")) {
                val candidateFact = (normalizedArgs["text"] ?: normalizedArgs["info"] ?: normalizedArgs["value"]) as? String
                    ?: listOfNotNull(normalizedArgs["key"] as? String, normalizedArgs["val"] as? String).joinToString(": ").ifBlank { null }
                if (!candidateFact.isNullOrBlank()) {
                    normalizedArgs["fact"] = candidateFact
                }
            }
        }

        // Enforce argument validation
        val validation = validateArguments(tool, normalizedArgs)
        if (!validation.isValid) {
            val errorMsg = validation.errorMessage ?: "Invalid arguments"
            Log.w(TAG, "[$TAG] Tool '$name' rejected invalid arguments: $errorMsg")
            return ToolResult.failure(errorMsg)
        }

        return try {
            Log.d(TAG, "[$TAG] Executing tool: '$resolvedName' with args: $normalizedArgs")
            tool.execute(normalizedArgs)
        } catch (t: Throwable) {
            Log.e(TAG, "[$TAG] Tool '$name' execution failed", t)
            ToolResult.failure("Error executing $name: ${t.localizedMessage ?: t.message ?: "Unknown error"}")
        }
    }

    /**
     * Robust balanced-brace JSON extractor that parses tool calls even when surrounded by text.
     * Example: "I should search the web.\n{"name":"web_search","arguments":{"query":"latest AI news"}}"
     */
    fun parseToolCall(rawText: String): ToolCallRequest? {
        if (rawText.isBlank()) return null

        // 1. Tag or prefix-style tool calls (LiteRT-LM & Gemma native conventions):
        // e.g. <call:image_search>{"query": "..."}</call> or call:image_search{"query": "..."} or <tool_call:image_search>{...}
        val tagPrefixRegex = Regex("""(?:<call:|<tool_call:|<action:|call:)?(image_search|web_search|manage_memory|ocr)(?:>)?\s*(\{[\s\S]*?\})""", RegexOption.IGNORE_CASE)
        val tagMatch = tagPrefixRegex.find(rawText)
        if (tagMatch != null) {
            val toolName = resolveToolName(tagMatch.groupValues[1])
            val jsonCandidate = tagMatch.groupValues[2]
            try {
                val element = JsonParser.parseString(jsonCandidate)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val args = mutableMapOf<String, Any?>()
                    val argsObj = if (obj.has("arguments") && obj.get("arguments").isJsonObject) {
                        obj.getAsJsonObject("arguments")
                    } else if (obj.has("parameters") && obj.get("parameters").isJsonObject) {
                        obj.getAsJsonObject("parameters")
                    } else {
                        obj
                    }
                    for ((k, v) in argsObj.entrySet()) {
                        args[k] = jsonElementToAny(v)
                    }
                    if (tools.containsKey(toolName)) {
                        return ToolCallRequest(name = toolName, arguments = args)
                    }
                }
            } catch (_: Exception) {}
        }

        // Tag attribute format: e.g. <call:image_search query="..."/>
        val tagAttrRegex = Regex("""<call:(image_search|web_search|manage_memory)\s+query=["']([^"']+)["']\s*/?>""", RegexOption.IGNORE_CASE)
        val tagAttrMatch = tagAttrRegex.find(rawText)
        if (tagAttrMatch != null) {
            val toolName = resolveToolName(tagAttrMatch.groupValues[1])
            val queryVal = tagAttrMatch.groupValues[2].trim()
            if (tools.containsKey(toolName) && queryVal.isNotBlank()) {
                return ToolCallRequest(name = toolName, arguments = mapOf("query" to queryVal))
            }
        }

        val candidates = extractBalancedJsonCandidates(rawText)

        for (candidate in candidates) {
            try {
                val element = JsonParser.parseString(candidate)
                if (!element.isJsonObject) continue

                val obj = element.asJsonObject

                // Support pattern variations:
                // 1. {"name": "...", "arguments": {...}}
                // 2. {"tool": "...", "arguments": {...}}
                // 3. {"type": "tool_call", "name": "...", "arguments": {...}}
                // 4. {"tool_call": {"name": "...", "arguments": {...}}}
                val targetObj = if (obj.has("tool_call") && obj.get("tool_call").isJsonObject) {
                    obj.getAsJsonObject("tool_call")
                } else {
                    obj
                }

                var rawToolName = when {
                    targetObj.has("name") -> targetObj.get("name").asString
                    targetObj.has("tool") -> targetObj.get("tool").asString
                    targetObj.has("function") -> targetObj.get("function").asString
                    else -> null
                }

                // If candidate has arguments but no explicit name, check if preceding text names the tool
                if (rawToolName == null && (targetObj.has("query") || targetObj.has("q"))) {
                    val cIdx = rawText.indexOf(candidate)
                    if (cIdx > 0) {
                        val prefix = rawText.substring(0, cIdx).lowercase()
                        rawToolName = when {
                            prefix.contains("image_search") || prefix.contains("search_image") -> "image_search"
                            prefix.contains("web_search") || prefix.contains("search_web") -> "web_search"
                            else -> null
                        }
                    }
                }

                if (rawToolName == null) continue

                val toolName = resolveToolName(rawToolName)

                // Tool must be registered in this registry
                if (!tools.containsKey(toolName)) continue

                val args = mutableMapOf<String, Any?>()
                val argsElement = when {
                    targetObj.has("arguments") -> targetObj.get("arguments")
                    targetObj.has("parameters") -> targetObj.get("parameters")
                    targetObj.has("args") -> targetObj.get("args")
                    else -> null
                }

                if (argsElement != null && argsElement.isJsonObject) {
                    val argsObj = argsElement.asJsonObject
                    for ((key, value) in argsObj.entrySet()) {
                        args[key] = jsonElementToAny(value)
                    }
                } else if (targetObj.has("query") || targetObj.has("q")) {
                    for ((key, value) in targetObj.entrySet()) {
                        args[key] = jsonElementToAny(value)
                    }
                }

                return ToolCallRequest(name = toolName, arguments = args)
            } catch (_: Exception) {
                // Not a valid JSON object candidate, try next
            }
        }

        // 2. Check function-call style fallback: e.g. web_search(query="...") or web_search("...")
        val fnRegex = Regex("""(?:call:)?(web_search|image_search|manage_memory)\s*\(\s*(?:(?:query|q)=)?\s*["']([^"']+)["']\s*\)""", RegexOption.IGNORE_CASE)
        val fnMatch = fnRegex.find(rawText)
        if (fnMatch != null) {
            val toolName = resolveToolName(fnMatch.groupValues[1])
            val queryVal = fnMatch.groupValues[2].trim()
            if (tools.containsKey(toolName) && queryVal.isNotBlank()) {
                return ToolCallRequest(name = toolName, arguments = mapOf("query" to queryVal))
            }
        }

        return null
    }

    /**
     * Extracts all balanced { ... } substrings from text, ignoring braces inside string quotes.
     */
    fun extractBalancedJsonCandidates(text: String): List<String> {
        val candidates = mutableListOf<String>()

        // Check for markdown code fences first (```json ... ```)
        val fenceRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        for (match in fenceRegex.findAll(text)) {
            candidates.add(match.groupValues[1].trim())
        }

        // Balanced brace scan
        var depth = 0
        var startIndex = -1
        var inString = false
        var isEscaped = false

        for (i in text.indices) {
            val c = text[i]

            if (inString) {
                if (isEscaped) {
                    isEscaped = false
                } else if (c == '\\') {
                    isEscaped = true
                } else if (c == '"') {
                    inString = false
                }
                continue
            }

            if (c == '"') {
                inString = true
                continue
            }

            if (c == '{') {
                if (depth == 0) {
                    startIndex = i
                }
                depth++
            } else if (c == '}') {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        val candidate = text.substring(startIndex, i + 1).trim()
                        if (candidate.length <= 4096 && !candidates.contains(candidate)) {
                            candidates.add(candidate)
                        }
                        startIndex = -1
                    }
                }
            }
        }

        return candidates
    }

    /**
     * Helper to detect duplicate calls or loops in a sequence of tool calls.
     */
    fun isDuplicateOrLoop(
        history: List<ToolCallRequest>,
        candidate: ToolCallRequest
    ): Boolean {
        if (history.isEmpty()) return false

        // 1. Immediate duplicate check: exactly same tool and same arguments as previous
        val last = history.last()
        if (last.name == candidate.name && last.arguments == candidate.arguments) {
            return true
        }

        // 2. Ping-pong loop check: A -> B -> A -> B
        if (history.size >= 3) {
            val prev2 = history[history.size - 2]
            val prev3 = history[history.size - 3]
            if (candidate.name == prev2.name && candidate.arguments == prev2.arguments &&
                last.name == prev3.name && last.arguments == prev3.arguments
            ) {
                return true
            }
        }

        // 3. Repeated occurrences: same tool + args already called twice in cycle
        val count = history.count { it.name == candidate.name && it.arguments == candidate.arguments }
        return count >= 1
    }

    private fun jsonElementToAny(element: JsonElement): Any? {
        return when {
            element.isJsonNull -> null
            element.isJsonPrimitive -> {
                val prim = element.asJsonPrimitive
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isNumber -> prim.asNumber
                    prim.isString -> prim.asString
                    else -> prim.asString
                }
            }
            element.isJsonArray -> {
                element.asJsonArray.map { jsonElementToAny(it) }
            }
            element.isJsonObject -> {
                val map = mutableMapOf<String, Any?>()
                for ((k, v) in element.asJsonObject.entrySet()) {
                    map[k] = jsonElementToAny(v)
                }
                map
            }
            else -> element.toString()
        }
    }
}
