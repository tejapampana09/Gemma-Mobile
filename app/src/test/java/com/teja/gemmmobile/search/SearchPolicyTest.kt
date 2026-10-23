package com.teja.gemmmobile.search

import org.junit.Assert.*
import org.junit.Test

class SearchPolicyTest {
    @Test fun followUpPreservesEntitiesAndCurrentConstraint() {
        val query = SearchPolicy.resolveQuery("Which one is cheaper in India today?",
            "Compare Pixel and Galaxy phones", "Pixel 9 and Galaxy S25 are the options.")
        assertTrue(query.contains("Pixel 9"))
        assertTrue(query.contains("Galaxy S25"))
        assertTrue(query.endsWith("Which one is cheaper in India today?"))
    }

    @Test fun standaloneQuestionDoesNotInheritUnrelatedContext() {
        val question = "Weather in Hyderabad today"
        assertEquals(question, SearchPolicy.resolveQuery(question, "Compare phones", "Pixel 9"))
        assertEquals("Cats and dogs", SearchPolicy.resolveQuery("Cats and dogs", "Compare phones", "Pixel 9"))
    }

    @Test fun volatileQueriesBypassCacheAndProductsExpireSooner() {
        assertEquals(0L, SearchPolicy.cacheTtlMillis("live score India", false))
        assertEquals(0L, SearchPolicy.cacheTtlMillis("refresh laptop prices", true))
        assertTrue(SearchPolicy.cacheTtlMillis("laptop", true) < SearchPolicy.cacheTtlMillis("binary search", false))
    }

    @Test fun relevantPassageBeatsLongIntroductionWithinBudget() {
        val text = "Welcome to our website. ".repeat(100) + "\n\n" +
            "The battery lasts 18 hours during video playback."
        val passage = SearchPolicy.selectPassages(text, "battery video playback", 100)
        assertTrue(passage.startsWith("The battery"))
        assertTrue(passage.length <= 100)
        assertEquals("", SearchPolicy.selectPassages(text, "battery", 0))
    }
}
