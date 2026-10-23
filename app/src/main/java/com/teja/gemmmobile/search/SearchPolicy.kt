package com.teja.gemmmobile.search

/** Pure policies shared by retrieval and tests; no model invocation required. */
object SearchPolicy {
    private val followUp = Regex(
        "\\b(it|its|they|them|those|these|that|which one|cheaper|better|compare them|what about)\\b|" +
            "వాటిలో|దాని|అది|ఇవి|^(and|also|mari)\\b|\\b(adhi|idi|vatilo)\\b",
        RegexOption.IGNORE_CASE
    )

    fun isFollowUp(query: String): Boolean = followUp.containsMatchIn(query)

    fun resolveQuery(raw: String, previousQuestion: String = "", previousAnswer: String = ""): String {
        val query = raw.trim()
        if (previousQuestion.isBlank() || !isFollowUp(query)) return query.take(800)
        // Keep the current question intact and include entities mentioned in the last answer.
        return "${previousQuestion.take(240)} ${previousAnswer.take(240)} $query".takeLast(800)
    }

    fun cacheTtlMillis(query: String, isProduct: Boolean): Long {
        val q = query.lowercase()
        if (listOf("refresh", "right now", "live score", "stock price", "crypto price").any { it in q }) return 0
        if (isProduct || listOf("today", "latest", "news", "weather", "price", "eeroju", "ippudu", "ఈరోజు").any { it in q }) {
            return 5 * 60 * 1000L
        }
        return 60 * 60 * 1000L
    }

    /** Select relevant paragraphs rather than always taking a page's introduction. */
    fun selectPassages(text: String, query: String, maxChars: Int): String {
        if (maxChars <= 0) return ""
        val terms = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 }.toSet()
        val chunks = text.split(Regex("\\n\\s*\\n"))
            .flatMap { it.chunked(700) }.filter { it.isNotBlank() }
        val selected = chunks.withIndex().sortedByDescending { (_, chunk) ->
            val words = chunk.lowercase().split(Regex("[^\\p{L}\\p{N}]+" )).toSet()
            terms.count { it in words }
        }
        val output = StringBuilder()
        for ((_, chunk) in selected) {
            val remaining = maxChars - output.length
            if (remaining <= 0) break
            output.append(chunk.take(remaining))
            if (output.length + 2 <= maxChars) output.append("\n\n")
        }
        return output.toString().trim()
    }
}
