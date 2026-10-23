package com.teja.gemmmobile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Lightweight motion: animate state changes, never reanimate the full streaming answer. */
@Composable
fun WebSearchActivity(status: String) {
    val theme = LocalChatTheme.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = theme.surface,
        border = BorderStroke(1.dp, theme.border),
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(180))
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = theme.accent, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(100)) },
                    label = "webSearchStage"
                ) { label ->
                    Text(label, color = Color(0xFFECECEC), style = MaterialTheme.typography.bodyMedium)
                }
                Text("Finding evidence for your answer", color = Color(0xFF9B9B9B),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun ChatWelcome(onPromptSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalChatTheme.current
    val suggestions = listOf(
        "Search the web" to "Search the web for the latest developments in on-device AI. Include sources.",
        "Compare options" to "Compare Android phones under ₹25,000 available in India. Check current prices and cite sources.",
        "Learn something" to "Explain how neural networks learn, using a simple example.",
        "Write together" to "Help me write a clear introduction for my project. Ask me what the project is about."
    )
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val columns = if (maxWidth >= 480.dp) 2 else 1
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("✦", color = theme.accent, fontSize = 40.sp)
            Text("What can we explore?", color = Color(0xFFECECEC), fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold)
            Text("Think, search, and create with Gemma", color = Color(0xFF9B9B9B),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            suggestions.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (title, prompt) ->
                        Surface(
                            onClick = { onPromptSelected(prompt) },
                            shape = RoundedCornerShape(20.dp),
                            color = theme.surface,
                            border = BorderStroke(1.dp, theme.border),
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                        ) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(title, color = Color(0xFFECECEC), modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("↗", color = theme.accent)
                            }
                        }
                    }
                }
            }
            Text("Answers run on your device. Web search sends queries online.",
                color = Color(0xFF9B9B9B), style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp))
        }
    }
}
