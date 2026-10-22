package com.teja.gemmmobile.skills

import android.content.Context
import com.teja.gemmmobile.tools.DeviceIntentsTool
import com.teja.gemmmobile.tools.QrCodeTool
import com.teja.gemmmobile.tools.WikipediaTool

/**
 * Registry of all official and custom on-device AI Skills for Gemma Mobile.
 * Built following the Google AI Edge Gallery Skills architecture.
 */
class SkillRegistry(private val context: Context) {

    val deviceIntentsTool = DeviceIntentsTool(context)
    val qrCodeTool = QrCodeTool(context)
    val wikipediaTool = WikipediaTool()

    val skills: List<GemmaSkill> by lazy {
        listOf(
            createDeviceIntentsSkill(),
            createQrCodeSkill(),
            createWikipediaSkill(),
            createQuizMasterSkill(),
            createCodeReviewerSkill(),
            createTeluguGuruSkill(),
            createPdfMakerSkill()
        )
    }

    fun findMatchingSkill(prompt: String): GemmaSkill? {
        val p = prompt.lowercase().trim()
        // Check in order of specificity
        return skills.firstOrNull { it.matches(p) }
    }

    private fun createDeviceIntentsSkill(): GemmaSkill {
        return GemmaSkill(
            id = "device-intents",
            name = "Device Actions & Alarms",
            icon = "⏰",
            description = "Sets alarms, timers, opens apps (WhatsApp, Camera, YouTube), and adds calendar reminders in English and Telugu.",
            keywords = listOf(
                "alarm", "అలారం", "timer", "టైమర్", "whatsapp", "వాట్సాప్",
                "calendar", "క్యాలెండర్", "reminder", "రిమైండర్", "open app",
                "ఓపెన్ చెయ్", "పెట్టు", "సేవ్ చెయ్", "open camera", "open settings"
            ),
            systemDirective = """
                ## SKILL: DEVICE ACTIONS & INTENTS (device-intents)
                You are equipped with the 'device_intents' tool to perform real Android actions.
                1. When the user asks to set an alarm (e.g. "రేపు పొద్దున్న 6 గంటలకి అలారం పెట్టు", "Set alarm for 7 AM"):
                   Call the `device_intents` tool with action "set_alarm", hour, and minutes.
                2. When the user asks to open an app (e.g. "WhatsApp ఓపెన్ చెయ్", "Open Camera", "Open YouTube"):
                   Call the `device_intents` tool with action "open_app" and app_name.
                3. When the user asks to add a calendar reminder (e.g. "Calendar లో రిమైండర్ సేవ్ చెయ్"):
                   Call the `device_intents` tool with action "add_calendar_event" and message.
                4. Confirm the action in the user's language (Telugu or English) clearly and enthusiastically.
            """.trimIndent(),
            tools = listOf(deviceIntentsTool)
        )
    }

    private fun createQrCodeSkill(): GemmaSkill {
        return GemmaSkill(
            id = "qr-code",
            name = "QR Code Generator",
            icon = "🏁",
            description = "Generates 100% on-device offline QR codes for links, Wi-Fi, UPI IDs, or text.",
            keywords = listOf(
                "qr code", "qrcode", "qr cheyyi", "qr generate", "qr maker",
                "create qr", "qr code create", "make qr"
            ),
            systemDirective = """
                ## SKILL: QR CODE GENERATOR (qr-code)
                You are equipped with the 'generate_qr_code' tool.
                1. When the user asks to create or generate a QR code for a link, text, phone number, or UPI ID:
                   Call the `generate_qr_code` tool with the text and optional title.
                2. Explain what is encoded in the QR code clearly to the user.
            """.trimIndent(),
            tools = listOf(qrCodeTool)
        )
    }

    private fun createWikipediaSkill(): GemmaSkill {
        return GemmaSkill(
            id = "query-wikipedia",
            name = "Wikipedia Deep Lookup",
            icon = "📚",
            description = "Retrieves verified encyclopedic definitions, historical facts, and biographies from Wikipedia.",
            keywords = listOf(
                "wikipedia", "wiki lo", "wiki search", "wiki lookup", "look up on wikipedia"
            ),
            systemDirective = """
                ## SKILL: WIKIPEDIA DEEP LOOKUP (query-wikipedia)
                You are equipped with the 'query_wikipedia' tool.
                1. When encyclopedic depth, historical chronology, or verified biographies are requested from Wikipedia:
                   Call `query_wikipedia` with the query string.
                2. Synthesize the returned Wikipedia knowledge into a clear, fascinating explanation.
            """.trimIndent(),
            tools = listOf(wikipediaTool)
        )
    }

    private fun createQuizMasterSkill(): GemmaSkill {
        return GemmaSkill(
            id = "quiz-master",
            name = "Flashcard & Exam Quiz Master",
            icon = "🎯",
            description = "Creates high-yield interactive exam quizzes, flashcards, MCQs with difficulty ratings, hints, and deep explanations.",
            keywords = listOf(
                "quiz", "flashcard", "exam", "test me", "questions on", "mcq",
                "practice test", "upsc", "gate", "neet", "interview questions",
                "test questions", "quiz master"
            ),
            systemDirective = """
                ## SKILL: FLASHCARD & EXAM QUIZ MASTER (quiz-master)
                You are a world-class exam tutor and quiz master.
                When the user asks for a quiz, test, or flashcards on any topic:
                1. Formulate 3 to 5 high-yield, conceptually rigorous Multiple Choice Questions (MCQs) or Flashcards.
                2. For each question:
                   - State the difficulty level (e.g. Easy, Medium, High-Yield Exam Level).
                   - Provide 4 distinct options (A, B, C, D) without obvious giveaways.
                   - Provide a hidden or separated answer section with:
                     * Correct Answer
                     * Deep conceptual explanation of why the correct option is right
                     * Why the common distractor options are incorrect
                     * Key takeaway / exam memory trick (Mnemonics)
                3. Conclude by asking if the user wants another round or deeper drill-down on a specific concept!
            """.trimIndent()
        )
    }

    private fun createCodeReviewerSkill(): GemmaSkill {
        return GemmaSkill(
            id = "code-reviewer",
            name = "Bug Hunter & Code Reviewer",
            icon = "🐛",
            description = "Inspects code for memory leaks, edge cases, security vulnerabilities, and provides optimized, clean refactorings.",
            keywords = listOf(
                "review code", "bug hunter", "find bugs", "code review", "optimize code",
                "debug this", "security review", "memory leak", "fix this code", "code smells"
            ),
            systemDirective = """
                ## SKILL: BUG HUNTER & CODE REVIEWER (code-reviewer)
                You are a Principal Software Engineer and Security Researcher.
                When provided with code or asked to review/hunt bugs:
                1. **Vulnerability & Bug Scan**: Check for null dereferences, off-by-one errors, memory leaks, concurrency race conditions, and injection flaws (SQLi, XSS, insecure deserialization).
                2. **Complexity Analysis**: State current Time Complexity (O(n)) and Space Complexity, and identify performance bottlenecks.
                3. **Structured Review Format**:
                   - 🚨 **Critical Bugs & Edge Cases** (with line references and failure scenarios)
                   - ⚡ **Performance & Optimization Opportunities**
                   - 🛡️ **Security & Reliability Findings**
                   - ✨ **Refactored Production Code** (provide complete, clean, idiomatic code)
            """.trimIndent()
        )
    }

    private fun createTeluguGuruSkill(): GemmaSkill {
        return GemmaSkill(
            id = "telugu-guru",
            name = "Telugu Sahityam / Translator",
            icon = "🪶",
            description = "Masters Telugu literature, Padyalu, Chandassu, Sumathi/Vemana Sathakalu, Bhagavatham, and high-accuracy bilingual translation.",
            keywords = listOf(
                "sahityam", "padyam", "telugu translation", "translate to telugu",
                "telugu lo cheppu", "vemana padyam", "sumathi sathakam", "chandassu",
                "sametha", "jatheeyam", "pratipadartham", "telugu guru", "bhagavatham"
            ),
            systemDirective = """
                ## SKILL: TELUGU SAHITYAM & TRANSLATOR (telugu-guru)
                You are a renowned Telugu Scholar (తెలుగు పండితులు) and master translator.
                1. **Telugu Literature & Padyalu**:
                   - When asked for Padyalu (Vemana, Sumathi, Potana Bhagavatham, Mahabharatham):
                     * Provide the authentic Padyam with proper line breaks.
                     * Provide the **ప్రతిపదార్థం (Pratipadartham)** - word-by-word meaning.
                     * Provide the **తాత్పర్యం / భావం (Bhavam)** - clear, inspirational explanation.
                     * Mention the **ఛందస్సు (Chandassu)** (ఉత్పలమాల, చంపకమాల, మత్తేభం, శార్దూలం, తేటగీతి, ఆటవెలది) where applicable.
                2. **Telugu Idioms & Proverbs**:
                   - Explain **సామెతలు (Samethalu)** and **జాతీయాలు (Jatheeyalu)** with cultural context and modern examples.
                3. **Translation**:
                   - Translate English <-> Telugu naturally, avoiding robotic literal translation, preserving authentic Telugu conversational idioms and grammar.
            """.trimIndent()
        )
    }

    private fun createPdfMakerSkill(): GemmaSkill {
        return GemmaSkill(
            id = "pdf-maker",
            name = "Study Notes & PDF Document Maker",
            icon = "📄",
            description = "Formats pure, high-yield study notes or documents into publication-ready PDF files without chat fluff.",
            keywords = listOf(
                "pdf", "notes pdf", "pdf cheyyi", "cheymante", "pdf format lo",
                "make pdf", "create pdf", "pdf document", "pdf lo ivvu"
            ),
            systemDirective = """
                ## SKILL: STUDY NOTES & PDF DOCUMENT MAKER (pdf-maker)
                The user requested comprehensive, publication-grade study notes or document in PDF format.
                1. Structure your output directly as a clean, complete, high-yield study document.
                2. Start with an informative Document Title (# <Title>).
                3. Organize the subject into clear, logical sections (## Section) and subsections (### Topic).
                4. Include comprehensive explanations, formal definitions, detailed bullet points, equations/formulas, and comparison tables or code snippets where appropriate.
                5. DO NOT output conversational filler like 'Here is your PDF' or 'I cannot create files'. Output the pure, well-structured document directly so the system can compile it into a PDF file.
            """.trimIndent()
        )
    }
}
