package com.teja.gemmmobile.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.widget.Toast
import androidx.core.content.FileProvider
import com.teja.gemmmobile.storage.ChatSession
import com.teja.gemmmobile.ui.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Publication-grade, 100% on-device PDF generation engine for Gemma Mobile.
 *
 * Designed with ChatGPT / Claude aesthetic:
 * - Precise A4 geometry (595 x 842 pt)
 * - Elegant typography hierarchy (H1, H2, H3, Body)
 * - Rich markdown formatting (bold, italic, code spans, links)
 * - Bordered syntax-styled code blocks with language pills
 * - Clean styled tables with header tinting and alternating rows
 * - Accent callout blockquotes
 * - Running headers & footers with "Page X of Y"
 * - Automated multi-page layout splitting
 */
object PdfExporter {

    // A4 Dimensions at 72 DPI (Standard PDF points)
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f

    // Margins
    private const val MARGIN_LEFT = 46f
    private const val MARGIN_RIGHT = 46f
    private const val MARGIN_TOP = 52f
    private const val MARGIN_BOTTOM = 52f

    private const val PRINTABLE_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT // 503 pt
    private const val CONTENT_MAX_Y = PAGE_HEIGHT - MARGIN_BOTTOM - 12f // 778 pt

    // Color Palette (Clean Modern Slate / Anthropic & OpenAI inspired)
    private const val COLOR_TEXT_PRIMARY = 0xFF0F172A.toInt()    // Slate 900
    private const val COLOR_TEXT_SECONDARY = 0xFF334155.toInt()  // Slate 700
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()      // Slate 500
    private const val COLOR_TEXT_LIGHT = 0xFF94A3B8.toInt()      // Slate 400
    private const val COLOR_ACCENT = 0xFF10A37F.toInt()          // Gemma Emerald Accent
    private const val COLOR_DIVIDER = 0xFFE2E8F0.toInt()         // Slate 200
    private const val COLOR_CODE_BG = 0xFFF8FAFC.toInt()         // Slate 50
    private const val COLOR_CODE_BORDER = 0xFFE2E8F0.toInt()     // Slate 200
    private const val COLOR_CODE_TEXT = 0xFF0F172A.toInt()       // Slate 900
    private const val COLOR_QUOTE_BAR = 0xFF3B82F6.toInt()       // Blue 500
    private const val COLOR_QUOTE_BG = 0xFFF8FAFC.toInt()        // Slate 50
    private const val COLOR_TABLE_HEADER_BG = 0xFFF1F5F9.toInt() // Slate 100
    private const val COLOR_TABLE_BORDER = 0xFFCBD5E1.toInt()    // Slate 300
    private const val COLOR_TABLE_ROW_ALT = 0xFFF8FAFC.toInt()   // Slate 50
    private const val COLOR_BADGE_BG = 0xFFECFDF5.toInt()        // Emerald 50
    private const val COLOR_BADGE_TEXT = 0xFF047857.toInt()      // Emerald 700

    data class PdfResult(
        val file: File,
        val title: String,
        val pageCount: Int
    )

    /**
     * Generate a publication-ready PDF document from markdown text and return file, title, and page count.
     */
    suspend fun generatePdf(
        context: Context,
        markdownText: String,
        title: String? = null
    ): PdfResult = withContext(Dispatchers.IO) {
        val docTitle = title?.takeIf { it.isNotBlank() } ?: deriveTitle(markdownText)
        val (file, pageCount) = generatePdfInternal(context, docTitle, markdownText)
        PdfResult(file, docTitle, pageCount)
    }

    /**
     * Export a single message (or arbitrary markdown text) to a clean PDF and trigger share/open.
     */
    suspend fun exportMessage(
        context: Context,
        markdownText: String,
        title: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            generatePdf(context, markdownText, title).file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export an entire ChatSession to a clean PDF and trigger share/open.
     */
    suspend fun exportSession(
        context: Context,
        session: ChatSession
    ): File? = withContext(Dispatchers.IO) {
        try {
            val markdown = buildSessionMarkdown(session)
            val (file, _) = generatePdfInternal(context, session.title, markdown)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export and directly launch the Android Share / View chooser.
     */
    fun exportAndShare(
        context: Context,
        markdownText: String,
        title: String? = null,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Generating clean PDF...", Toast.LENGTH_SHORT).show()
            val result = withContext(Dispatchers.IO) {
                try {
                    generatePdf(context, markdownText, title)
                } catch (_: Exception) { null }
            }
            if (result != null && result.file.exists()) {
                openPdf(context, result.file)
            } else {
                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Export entire session and launch Share chooser.
     */
    fun exportSessionAndShare(
        context: Context,
        session: ChatSession,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            Toast.makeText(context, "Generating session PDF...", Toast.LENGTH_SHORT).show()
            val file = withContext(Dispatchers.IO) {
                exportSession(context, session)
            }
            if (file != null && file.exists()) {
                openOrSharePdf(context, file, session.title)
            } else {
                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Open PDF directly in Android's system PDF viewer.
     */
    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file, file.nameWithoutExtension)
        }
    }

    /**
     * Share PDF file via system share sheet (WhatsApp, Drive, Gmail, etc.).
     */
    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openOrSharePdf(context: Context, file: File, title: String) {
        sharePdf(context, file, title)
    }

    // =========================================================================
    // Core Layout & Rendering Engine
    // =========================================================================

    private fun generatePdfInternal(
        context: Context,
        documentTitle: String,
        markdownText: String
    ): Pair<File, Int> {
        // Prepare output directory
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        cleanupOldExports(exportDir)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedTitle = documentTitle
            .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            .take(30)
            .trim('_')
            .ifEmpty { "document" }

        val outputFile = File(exportDir, "Gemma_${sanitizedTitle}_$timestamp.pdf")

        // Parse markdown into layout blocks
        val blocks = parseMarkdownBlocks(markdownText)

        // Build pages layout model
        val pages = layoutDocument(documentTitle, blocks)

        // Render to Android PdfDocument
        val pdfDocument = PdfDocument()
        val totalPages = max(1, pages.size)

        for ((pageIndex, pageContent) in pages.withIndex()) {
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH.toInt(),
                PAGE_HEIGHT.toInt(),
                pageNumber
            ).create()

            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Crisp white background
            canvas.drawColor(Color.WHITE)

            // Running header (Page 2+)
            if (pageNumber > 1) {
                drawRunningHeader(canvas, documentTitle)
            }

            // Running footer (all pages)
            drawRunningFooter(canvas, pageNumber, totalPages)

            // Render all items for this page
            pageContent.render(canvas)

            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return Pair(outputFile, totalPages)
    }

    private fun cleanupOldExports(exportDir: File) {
        try {
            val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < dayAgo) {
                    file.delete()
                }
            }
        } catch (_: Exception) { }
    }

    private fun deriveTitle(markdownText: String): String {
        val headingLine = markdownText.lines().firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.startsWith("#") || trimmed.startsWith("**")
        } ?: markdownText.lines().firstOrNull { it.isNotBlank() } ?: "Study Notes Document"

        val clean = headingLine
            .replace("#", "")
            .replace("*", "")
            .replace("`", "")
            .trim()
        return if (clean.length > 50) clean.take(47) + "..." else clean.ifEmpty { "Study Notes Document" }
    }

    private fun buildSessionMarkdown(session: ChatSession): String {
        val sb = StringBuilder()
        sb.append("# ${session.title}\n\n")

        for (msg in session.messages) {
            val isUser = msg.role == MessageRole.USER
            val roleName = if (isUser) "Prompt" else "Response"
            val badge = if (isUser) "### 👤 **$roleName**" else "### ✨ **Gemma AI**"
            sb.append("$badge\n\n")

            if (msg.thoughtText.isNotBlank()) {
                sb.append("> **Thinking Process:**\n")
                msg.thoughtText.lines().forEach { line ->
                    sb.append("> $line\n")
                }
                sb.append("\n")
            }

            sb.append(msg.text.trim())
            sb.append("\n\n---\n\n")
        }
        return sb.toString().trimEnd('-', '\n', ' ')
    }

    // =========================================================================
    // Markdown Parser -> Structured Elements
    // =========================================================================

    private sealed class DocBlock {
        data class Heading(val level: Int, val text: String) : DocBlock()
        data class Paragraph(val text: String) : DocBlock()
        data class BulletItem(val text: String, val indentLevel: Int = 0) : DocBlock()
        data class NumberedItem(val number: String, val text: String, val indentLevel: Int = 0) : DocBlock()
        data class CodeBlock(val language: String, val code: String) : DocBlock()
        data class Blockquote(val text: String) : DocBlock()
        data class Table(val headers: List<String>, val rows: List<List<String>>) : DocBlock()
        object HorizontalRule : DocBlock()
    }

    private fun parseMarkdownBlocks(markdown: String): List<DocBlock> {
        val blocks = mutableListOf<DocBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            // Blank line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Code Block: ```lang
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // Skip closing ```
                blocks.add(DocBlock.CodeBlock(language, codeLines.joinToString("\n")))
                continue
            }

            // Horizontal Rule: ---, ***, ___
            if (trimmed.matches(Regex("^([-*_])\\s*\\1\\s*\\1[\\s\\1]*$"))) {
                blocks.add(DocBlock.HorizontalRule)
                i++
                continue
            }

            // Table check: starts with | and contains |
            if (trimmed.startsWith("|") && trimmed.contains("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s\\-:\\|]+$"))) {
                val headers = parseTableRow(trimmed)
                i += 2 // Skip header and separator
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows.add(parseTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(DocBlock.Table(headers, rows))
                continue
            }

            // Heading: #, ##, ###
            if (trimmed.startsWith("#")) {
                val match = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
                if (match != null) {
                    val level = match.groupValues[1].length
                    val text = match.groupValues[2]
                    blocks.add(DocBlock.Heading(level, text))
                    i++
                    continue
                }
            }

            // Blockquote: > text
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(DocBlock.Blockquote(quoteLines.joinToString("\n")))
                continue
            }

            // Bullet List: - item, * item, + item
            val bulletMatch = Regex("^(\\s*)[-*+]\\s+(.*)$").find(line)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1].length / 2
                val text = bulletMatch.groupValues[2]
                blocks.add(DocBlock.BulletItem(text, indent))
                i++
                continue
            }

            // Numbered List: 1. item, 2) item
            val numMatch = Regex("^(\\s*)(\\d+[.)])\\s+(.*)$").find(line)
            if (numMatch != null) {
                val indent = numMatch.groupValues[1].length / 2
                val number = numMatch.groupValues[2]
                val text = numMatch.groupValues[3]
                blocks.add(DocBlock.NumberedItem(number, text, indent))
                i++
                continue
            }

            // Regular paragraph (accumulate until blank line or new block indicator)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val current = lines[i].trim()
                if (current.isEmpty() ||
                    current.startsWith("#") ||
                    current.startsWith("```") ||
                    current.startsWith(">") ||
                    current.startsWith("|") ||
                    current.matches(Regex("^([-*_])\\s*\\1\\s*\\1[\\s\\1]*$")) ||
                    Regex("^(\\s*)[-*+]\\s+").containsMatchIn(lines[i]) ||
                    Regex("^(\\s*)\\d+[.)]\\s+").containsMatchIn(lines[i])
                ) {
                    break
                }
                paragraphLines.add(lines[i].trim())
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(DocBlock.Paragraph(paragraphLines.joinToString(" ")))
            }
        }

        return blocks
    }

    private fun parseTableRow(rowLine: String): List<String> {
        val trimmed = rowLine.removePrefix("|").removeSuffix("|")
        return trimmed.split("|").map { it.trim() }
    }

    // =========================================================================
    // Multi-Page Layout Engine
    // =========================================================================

    private class PageModel {
        val renderCommands = mutableListOf<(Canvas) -> Unit>()
        fun render(canvas: Canvas) {
            renderCommands.forEach { it(canvas) }
        }
    }

    private fun layoutDocument(documentTitle: String, blocks: List<DocBlock>): List<PageModel> {
        val pages = mutableListOf<PageModel>()
        var currentPage = PageModel()
        pages.add(currentPage)

        var currentY = MARGIN_TOP

        fun advancePage(): Float {
            currentPage = PageModel()
            pages.add(currentPage)
            return MARGIN_TOP + 12f // Leave space for running header on page 2+
        }

        // 1. Draw Document Header on First Page
        val headerHeight = measureCoverHeaderHeight(documentTitle)
        drawCoverHeader(currentPage, documentTitle, MARGIN_LEFT, currentY)
        currentY += headerHeight + 16f

        // 2. Iterate through content blocks
        for (block in blocks) {
            when (block) {
                is DocBlock.Heading -> {
                    val (layout, before, after) = createHeadingLayout(block.level, block.text, PRINTABLE_WIDTH)
                    val totalNeeded = layout.height + before + after

                    // Keep-with-next: avoid orphan headings at the bottom of a page
                    if (currentY + totalNeeded + 35f > CONTENT_MAX_Y) {
                        currentY = advancePage()
                    }

                    currentY += before
                    val drawY = currentY
                    currentPage.renderCommands.add { canvas ->
                        canvas.save()
                        canvas.translate(MARGIN_LEFT, drawY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    currentY += layout.height + after
                }

                is DocBlock.Paragraph -> {
                    val layout = createParagraphLayout(block.text, PRINTABLE_WIDTH)
                    val spaceAfter = 8f

                    if (currentY + layout.height <= CONTENT_MAX_Y) {
                        // Fits completely on current page
                        val drawY = currentY
                        currentPage.renderCommands.add { canvas ->
                            canvas.save()
                            canvas.translate(MARGIN_LEFT, drawY)
                            layout.draw(canvas)
                            canvas.restore()
                        }
                        currentY += layout.height + spaceAfter
                    } else {
                        // Needs splitting across pages
                        var remainingText = block.text
                        while (remainingText.isNotBlank()) {
                            val availableH = CONTENT_MAX_Y - currentY
                            if (availableH < 24f) {
                                currentY = advancePage()
                                continue
                            }

                            val fullLayout = createParagraphLayout(remainingText, PRINTABLE_WIDTH)
                            if (currentY + fullLayout.height <= CONTENT_MAX_Y) {
                                val drawY = currentY
                                currentPage.renderCommands.add { canvas ->
                                    canvas.save()
                                    canvas.translate(MARGIN_LEFT, drawY)
                                    fullLayout.draw(canvas)
                                    canvas.restore()
                                }
                                currentY += fullLayout.height + spaceAfter
                                break
                            }

                            // Find line where it fits
                            val lineCount = fullLayout.lineCount
                            var fitLine = -1
                            for (lineIdx in 0 until lineCount) {
                                if (fullLayout.getLineBottom(lineIdx) <= availableH) {
                                    fitLine = lineIdx
                                } else {
                                    break
                                }
                            }

                            if (fitLine <= 0) {
                                // Not even one line fits, move to fresh page
                                currentY = advancePage()
                            } else {
                                val splitOffset = fullLayout.getLineEnd(fitLine)
                                val currentSlice = remainingText.substring(0, splitOffset).trim()
                                remainingText = remainingText.substring(splitOffset).trim()

                                val sliceLayout = createParagraphLayout(currentSlice, PRINTABLE_WIDTH)
                                val drawY = currentY
                                currentPage.renderCommands.add { canvas ->
                                    canvas.save()
                                    canvas.translate(MARGIN_LEFT, drawY)
                                    sliceLayout.draw(canvas)
                                    canvas.restore()
                                }
                                currentY = advancePage()
                            }
                        }
                    }
                }

                is DocBlock.BulletItem -> {
                    val indent = block.indentLevel * 14f
                    val bulletX = MARGIN_LEFT + indent + 4f
                    val textX = MARGIN_LEFT + indent + 16f
                    val textW = PRINTABLE_WIDTH - (indent + 16f)

                    val layout = createParagraphLayout(block.text, textW)
                    val needed = layout.height + 4f

                    if (currentY + needed > CONTENT_MAX_Y) {
                        currentY = advancePage()
                    }

                    val drawY = currentY
                    currentPage.renderCommands.add { canvas ->
                        // Draw bullet point
                        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = COLOR_ACCENT
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(bulletX, drawY + 6.5f, 2.2f, bulletPaint)

                        // Draw text
                        canvas.save()
                        canvas.translate(textX, drawY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    currentY += layout.height + 5f
                }

                is DocBlock.NumberedItem -> {
                    val indent = block.indentLevel * 14f
                    val numX = MARGIN_LEFT + indent + 2f
                    val textX = MARGIN_LEFT + indent + 20f
                    val textW = PRINTABLE_WIDTH - (indent + 20f)

                    val layout = createParagraphLayout(block.text, textW)
                    val needed = layout.height + 4f

                    if (currentY + needed > CONTENT_MAX_Y) {
                        currentY = advancePage()
                    }

                    val drawY = currentY
                    val numText = block.number
                    currentPage.renderCommands.add { canvas ->
                        // Draw number
                        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = COLOR_ACCENT
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textSize = 9.5f
                        }
                        canvas.drawText(numText, numX, drawY + 9.5f, numPaint)

                        // Draw text
                        canvas.save()
                        canvas.translate(textX, drawY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    currentY += layout.height + 5f
                }

                is DocBlock.CodeBlock -> {
                    val codeLines = block.code.lines()
                    val linePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = COLOR_CODE_TEXT
                        typeface = Typeface.MONOSPACE
                        textSize = 8.2f
                    }
                    val lineHeight = 11.5f
                    val pad = 9f
                    val headerH = if (block.language.isNotBlank()) 18f else 6f
                    val totalH = headerH + (codeLines.size * lineHeight) + pad

                    if (currentY + totalH <= CONTENT_MAX_Y) {
                        // Entire codeblock fits
                        val drawY = currentY
                        currentPage.renderCommands.add { canvas ->
                            drawSingleCodeBlockBox(
                                canvas,
                                MARGIN_LEFT,
                                drawY,
                                PRINTABLE_WIDTH,
                                totalH,
                                block.language,
                                codeLines,
                                headerH,
                                linePaint,
                                lineHeight,
                                pad
                            )
                        }
                        currentY += totalH + 12f
                    } else {
                        // Split code block across pages
                        var lineIndex = 0
                        var isFirstChunk = true

                        while (lineIndex < codeLines.size) {
                            val availH = CONTENT_MAX_Y - currentY
                            val currentHeaderH = if (isFirstChunk && block.language.isNotBlank()) 18f else 6f
                            val minNeeded = currentHeaderH + (lineHeight * 2) + pad

                            if (availH < minNeeded) {
                                currentY = advancePage()
                                continue
                            }

                            val linesFit = ((availH - currentHeaderH - pad) / lineHeight).toInt()
                            val chunkLinesCount = max(1, min(linesFit, codeLines.size - lineIndex))
                            val chunkLines = codeLines.subList(lineIndex, lineIndex + chunkLinesCount)
                            val chunkH = currentHeaderH + (chunkLines.size * lineHeight) + pad

                            val drawY = currentY
                            val lang = if (isFirstChunk) block.language else ""
                            val header = currentHeaderH

                            currentPage.renderCommands.add { canvas ->
                                drawSingleCodeBlockBox(
                                    canvas,
                                    MARGIN_LEFT,
                                    drawY,
                                    PRINTABLE_WIDTH,
                                    chunkH,
                                    lang,
                                    chunkLines,
                                    header,
                                    linePaint,
                                    lineHeight,
                                    pad
                                )
                            }

                            currentY += chunkH + 8f
                            lineIndex += chunkLinesCount
                            isFirstChunk = false

                            if (lineIndex < codeLines.size) {
                                currentY = advancePage()
                            }
                        }
                        currentY += 4f
                    }
                }

                is DocBlock.Blockquote -> {
                    val quoteText = block.text
                    val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = COLOR_TEXT_SECONDARY
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        textSize = 9.5f
                    }
                    val quoteWidth = PRINTABLE_WIDTH - 20f
                    val spannable = parseMarkdownSpans(quoteText, Typeface.create(Typeface.DEFAULT, Typeface.ITALIC), COLOR_TEXT_SECONDARY, 9.5f)
                    val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, quotePaint, quoteWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2.5f, 1.25f)
                        .setIncludePad(false)
                        .build()

                    val padV = 7f
                    val totalH = layout.height + (padV * 2)

                    if (currentY + totalH > CONTENT_MAX_Y) {
                        currentY = advancePage()
                    }

                    val drawY = currentY
                    currentPage.renderCommands.add { canvas ->
                        // Background tint
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = COLOR_QUOTE_BG
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(
                            RectF(MARGIN_LEFT, drawY, MARGIN_LEFT + PRINTABLE_WIDTH, drawY + totalH),
                            3f, 3f, bgPaint
                        )

                        // Left vertical accent bar
                        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = COLOR_QUOTE_BAR
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(
                            RectF(MARGIN_LEFT, drawY, MARGIN_LEFT + 3.5f, drawY + totalH),
                            1.5f, 1.5f, barPaint
                        )

                        // Text content
                        canvas.save()
                        canvas.translate(MARGIN_LEFT + 14f, drawY + padV)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    currentY += totalH + 10f
                }

                is DocBlock.Table -> {
                    currentY = layoutTable(
                        block,
                        currentPage,
                        currentY,
                        advancePage = { advancePage() }
                    )
                }

                is DocBlock.HorizontalRule -> {
                    if (currentY + 16f > CONTENT_MAX_Y) {
                        currentY = advancePage()
                    }
                    val drawY = currentY + 8f
                    currentPage.renderCommands.add { canvas ->
                        val hrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = COLOR_DIVIDER
                            strokeWidth = 0.75f
                        }
                        canvas.drawLine(MARGIN_LEFT + 20f, drawY, MARGIN_LEFT + PRINTABLE_WIDTH - 20f, drawY, hrPaint)
                    }
                    currentY += 16f
                }
            }
        }

        return pages
    }

    // =========================================================================
    // Table Layout Engine
    // =========================================================================

    private fun layoutTable(
        table: DocBlock.Table,
        initialPage: PageModel,
        startY: Float,
        advancePage: () -> Float
    ): Float {
        var currentPage = initialPage
        var currentY = startY

        val colCount = max(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 1)
        if (colCount == 0) return currentY

        // Compute proportional column widths based on content lengths
        val colWeights = FloatArray(colCount) { 1f }
        for (c in 0 until colCount) {
            val headerLen = table.headers.getOrNull(c)?.length ?: 0
            val maxRowLen = table.rows.maxOfOrNull { it.getOrNull(c)?.length ?: 0 } ?: 0
            val maxLen = max(headerLen, maxRowLen)
            colWeights[c] = max(12f, min(100f, maxLen.toFloat()))
        }
        val totalWeight = colWeights.sum()
        val colWidths = FloatArray(colCount) { c ->
            (colWeights[c] / totalWeight) * PRINTABLE_WIDTH
        }

        val cellPadH = 6f
        val cellPadV = 5f

        // Cell Layout Helpers
        fun makeCellLayout(text: String, width: Float, isHeader: Boolean): StaticLayout {
            val tf = if (isHeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            val color = if (isHeader) COLOR_TEXT_PRIMARY else COLOR_TEXT_SECONDARY
            val size = if (isHeader) 8.8f else 8.4f

            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = tf
                this.color = color
                this.textSize = size
            }

            val printableCellW = max(10f, width - (cellPadH * 2))
            val spannable = parseMarkdownSpans(text, tf, color, size)

            return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, printableCellW.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.5f, 1.15f)
                .setIncludePad(false)
                .build()
        }

        fun drawTableRow(
            page: PageModel,
            y: Float,
            height: Float,
            cells: List<String>,
            isHeader: Boolean,
            isAlt: Boolean
        ) {
            page.renderCommands.add { canvas ->
                // Row background
                val bgPaint = Paint().apply {
                    color = when {
                        isHeader -> COLOR_TABLE_HEADER_BG
                        isAlt -> COLOR_TABLE_ROW_ALT
                        else -> Color.WHITE
                    }
                    style = Paint.Style.FILL
                }
                canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + PRINTABLE_WIDTH, y + height, bgPaint)

                // Bottom border
                val borderPaint = Paint().apply {
                    color = COLOR_TABLE_BORDER
                    strokeWidth = if (isHeader) 1f else 0.5f
                }
                canvas.drawLine(MARGIN_LEFT, y + height, MARGIN_LEFT + PRINTABLE_WIDTH, y + height, borderPaint)

                // Draw cell contents and vertical borders
                var colX = MARGIN_LEFT
                for (c in 0 until colCount) {
                    val w = colWidths[c]
                    val cellText = cells.getOrNull(c) ?: ""
                    val cellLayout = makeCellLayout(cellText, w, isHeader)

                    canvas.save()
                    canvas.translate(colX + cellPadH, y + cellPadV)
                    cellLayout.draw(canvas)
                    canvas.restore()

                    if (c > 0) {
                        canvas.drawLine(colX, y, colX, y + height, borderPaint)
                    }
                    colX += w
                }

                // Table outer borders
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_LEFT, y + height, borderPaint)
                canvas.drawLine(MARGIN_LEFT + PRINTABLE_WIDTH, y, MARGIN_LEFT + PRINTABLE_WIDTH, y + height, borderPaint)
            }
        }

        // Measure Header Row
        val headerLayouts = table.headers.mapIndexed { c, text -> makeCellLayout(text, colWidths.getOrElse(c) { 40f }, true) }
        val headerHeight = (headerLayouts.maxOfOrNull { it.height } ?: 14) + (cellPadV * 2)

        if (currentY + headerHeight + 30f > CONTENT_MAX_Y) {
            currentY = advancePage()
        }

        // Top table border
        val startTopY = currentY
        currentPage.renderCommands.add { canvas ->
            val p = Paint().apply { color = COLOR_TABLE_BORDER; strokeWidth = 1f }
            canvas.drawLine(MARGIN_LEFT, startTopY, MARGIN_LEFT + PRINTABLE_WIDTH, startTopY, p)
        }

        // Draw Header
        drawTableRow(currentPage, currentY, headerHeight, table.headers, isHeader = true, isAlt = false)
        currentY += headerHeight

        // Data Rows
        for ((rowIndex, rowData) in table.rows.withIndex()) {
            val cellLayouts = rowData.mapIndexed { c, text -> makeCellLayout(text, colWidths.getOrElse(c) { 40f }, false) }
            val rowHeight = (cellLayouts.maxOfOrNull { it.height } ?: 14) + (cellPadV * 2)

            if (currentY + rowHeight > CONTENT_MAX_Y) {
                currentY = advancePage()
                // Redraw top border and table header on new page
                val topBorderY = currentY
                currentPage.renderCommands.add { canvas ->
                    val p = Paint().apply { color = COLOR_TABLE_BORDER; strokeWidth = 1f }
                    canvas.drawLine(MARGIN_LEFT, topBorderY, MARGIN_LEFT + PRINTABLE_WIDTH, topBorderY, p)
                }
                drawTableRow(currentPage, currentY, headerHeight, table.headers, isHeader = true, isAlt = false)
                currentY += headerHeight
            }

            drawTableRow(currentPage, currentY, rowHeight, rowData, isHeader = false, isAlt = rowIndex % 2 == 1)
            currentY += rowHeight
        }

        return currentY + 12f
    }

    // =========================================================================
    // Code Block Drawer Helper
    // =========================================================================

    private fun drawSingleCodeBlockBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        language: String,
        lines: List<String>,
        headerH: Float,
        linePaint: TextPaint,
        lineHeight: Float,
        pad: Float
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Box background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CODE_BG
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)

        // Box border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CODE_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }
        canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

        // Optional language pill in top right
        if (language.isNotBlank()) {
            val langText = language.uppercase()
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_LIGHT
                textSize = 7f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(langText, x + width - 10f, y + 12f, pillPaint)

            // Subtle divider under header
            val divPaint = Paint().apply {
                color = COLOR_CODE_BORDER
                strokeWidth = 0.5f
            }
            canvas.drawLine(x, y + headerH, x + width, y + headerH, divPaint)
        }

        // Draw code text lines
        var lineY = y + headerH + 9.5f
        for (line in lines) {
            canvas.drawText(line, x + pad, lineY, linePaint)
            lineY += lineHeight
        }
    }

    // =========================================================================
    // Cover Header & Running Headers/Footers
    // =========================================================================

    private fun measureCoverHeaderHeight(title: String): Float {
        val titleLayout = createDocumentTitleLayout(title, PRINTABLE_WIDTH)
        return 20f + titleLayout.height + 6f + 14f + 14f // Badge + Title + Subtitle + Line
    }

    private fun drawCoverHeader(page: PageModel, title: String, x: Float, y: Float) {
        val titleLayout = createDocumentTitleLayout(title, PRINTABLE_WIDTH)
        val formattedDate = SimpleDateFormat("MMMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date())

        page.renderCommands.add { canvas ->
            var currentY = y

            // 1. Badge Pill ("GEMMA AI EXPORT")
            val badgeText = "GEMMA 4 ON-DEVICE AI EXPORT"
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BADGE_TEXT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 7.2f
            }
            val badgeWidth = badgePaint.measureText(badgeText) + 14f
            val badgeRect = RectF(x, currentY, x + badgeWidth, currentY + 14f)
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BADGE_BG
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, 3f, 3f, badgeBgPaint)
            canvas.drawText(badgeText, x + 7f, currentY + 10f, badgePaint)

            currentY += 22f

            // 2. Document Title
            canvas.save()
            canvas.translate(x, currentY)
            titleLayout.draw(canvas)
            canvas.restore()

            currentY += titleLayout.height + 6f

            // 3. Metadata Subtitle (Date & Platform)
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("Generated on $formattedDate", x, currentY + 7f, subPaint)

            currentY += 16f

            // 4. Subtle Hairline Divider
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DIVIDER
                strokeWidth = 1f
            }
            canvas.drawLine(x, currentY, x + PRINTABLE_WIDTH, currentY, divPaint)
        }
    }

    private fun drawRunningHeader(canvas: Canvas, title: String) {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_LIGHT
            textSize = 7.5f
            typeface = Typeface.DEFAULT
        }
        val truncated = if (title.length > 55) title.take(52) + "..." else title
        canvas.drawText(truncated, MARGIN_LEFT, 38f, headerPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 0.5f
        }
        canvas.drawLine(MARGIN_LEFT, 44f, MARGIN_LEFT + PRINTABLE_WIDTH, 44f, linePaint)
    }

    private fun drawRunningFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerY = PAGE_HEIGHT - 34f

        // Top separator line
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 0.5f
        }
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, MARGIN_LEFT + PRINTABLE_WIDTH, footerY - 10f, linePaint)

        // Left label: Gemma Mobile
        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_LIGHT
            textSize = 8f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("Gemma Mobile • On-Device AI", MARGIN_LEFT, footerY + 2f, leftPaint)

        // Right label: Page X of Y
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNumber of $totalPages", MARGIN_LEFT + PRINTABLE_WIDTH, footerY + 2f, rightPaint)
    }

    // =========================================================================
    // Typographic Helpers
    // =========================================================================

    private fun createDocumentTitleLayout(text: String, width: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 17f
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.2f)
            .setIncludePad(false)
            .build()
    }

    private data class HeadingMetrics(val layout: StaticLayout, val spaceBefore: Float, val spaceAfter: Float)

    private fun createHeadingLayout(level: Int, text: String, width: Float): HeadingMetrics {
        val (size, color, before, after) = when (level) {
            1 -> Tuple4(15f, COLOR_TEXT_PRIMARY, 16f, 6f)
            2 -> Tuple4(13f, COLOR_TEXT_PRIMARY, 13f, 5f)
            3 -> Tuple4(11.5f, COLOR_TEXT_SECONDARY, 10f, 4f)
            else -> Tuple4(10.5f, COLOR_TEXT_SECONDARY, 8f, 4f)
        }

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.textSize = size
        }

        val spannable = parseMarkdownSpans(text, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), color, size)
        val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.2f)
            .setIncludePad(false)
            .build()

        return HeadingMetrics(layout, before, after)
    }

    private fun createParagraphLayout(text: String, width: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            typeface = Typeface.DEFAULT
            textSize = 9.8f
        }

        val spannable = parseMarkdownSpans(text, Typeface.DEFAULT, COLOR_TEXT_SECONDARY, 9.8f)
        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, max(10, width.toInt()))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2.5f, 1.3f)
            .setIncludePad(false)
            .build()
    }

    /**
     * Parses inline markdown spans (**bold**, *italic*, `code`, [link](url)).
     */
    private fun parseMarkdownSpans(
        text: String,
        baseTypeface: Typeface,
        baseColor: Int,
        baseSize: Float
    ): SpannableStringBuilder {
        val cleanBuilder = StringBuilder()
        val spansToApply = mutableListOf<SpanItem>()

        var i = 0
        while (i < text.length) {
            // Bold: **text**
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val content = text.substring(i + 2, end)
                    val startIdx = cleanBuilder.length
                    cleanBuilder.append(content)
                    spansToApply.add(SpanItem(startIdx, cleanBuilder.length, StyleSpan(Typeface.BOLD)))
                    i = end + 2
                    continue
                }
            }

            // Inline Code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    val content = text.substring(i + 1, end)
                    val startIdx = cleanBuilder.length
                    cleanBuilder.append(content)
                    spansToApply.add(SpanItem(startIdx, cleanBuilder.length, TypefaceSpan("monospace")))
                    spansToApply.add(SpanItem(startIdx, cleanBuilder.length, ForegroundColorSpan(0xFFB91C1C.toInt())))
                    i = end + 1
                    continue
                }
            }

            // Italic: *text* or _text_
            if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] == ' ' || text[i - 1] == '(')) {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1) {
                    val content = text.substring(i + 1, end)
                    val startIdx = cleanBuilder.length
                    cleanBuilder.append(content)
                    spansToApply.add(SpanItem(startIdx, cleanBuilder.length, StyleSpan(Typeface.ITALIC)))
                    i = end + 1
                    continue
                }
            }

            // Link: [text](url)
            if (text[i] == '[') {
                val endBracket = text.indexOf(']', i + 1)
                if (endBracket != -1 && endBracket + 1 < text.length && text[endBracket + 1] == '(') {
                    val endParen = text.indexOf(')', endBracket + 2)
                    if (endParen != -1) {
                        val linkText = text.substring(i + 1, endBracket)
                        val startIdx = cleanBuilder.length
                        cleanBuilder.append(linkText)
                        spansToApply.add(SpanItem(startIdx, cleanBuilder.length, ForegroundColorSpan(0xFF2563EB.toInt())))
                        spansToApply.add(SpanItem(startIdx, cleanBuilder.length, UnderlineSpan()))
                        i = endParen + 1
                        continue
                    }
                }
            }

            cleanBuilder.append(text[i])
            i++
        }

        val spannable = SpannableStringBuilder(cleanBuilder.toString())
        for (item in spansToApply) {
            spannable.setSpan(item.span, item.start, item.end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private data class SpanItem(val start: Int, val end: Int, val span: Any)
    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
