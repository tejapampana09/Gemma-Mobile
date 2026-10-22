package com.teja.gemmmobile.skills

import com.teja.gemmmobile.tools.GemmaTool

/**
 * Definition of an official Gemma Skill matching Google AI Edge Gallery Skills architecture.
 */
data class GemmaSkill(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val keywords: List<String>,
    val systemDirective: String,
    val tools: List<GemmaTool> = emptyList()
) {
    fun matches(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()
        return keywords.any { lower.contains(it) }
    }
}
