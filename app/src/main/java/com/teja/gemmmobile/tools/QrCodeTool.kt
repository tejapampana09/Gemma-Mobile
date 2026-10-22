package com.teja.gemmmobile.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% on-device offline QR code generator tool.
 *
 * Encodes URLs, Wi-Fi configs, UPI payment IDs, or text into high-resolution QR codes.
 */
class QrCodeTool(private val context: Context) : GemmaTool {

    override val name: String = "generate_qr_code"

    override val description: String =
        "Generates an on-device QR code image from any text, link, URL, UPI payment ID, or Wi-Fi configuration."

    override val parametersJsonSchema: String = """
        {
          "type": "object",
          "properties": {
            "text": {
              "type": "string",
              "description": "The text, link, or data to encode into the QR code."
            },
            "title": {
              "type": "string",
              "description": "Short label or title for the QR code."
            }
          },
          "required": ["text"]
        }
    """.trimIndent()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val text = arguments["text"]?.toString()?.trim()
            ?: return@withContext ToolResult.failure("Missing 'text' parameter")
        val title = arguments["title"]?.toString()?.trim() ?: "QR Code"

        try {
            val qrDir = File(context.cacheDir, "qr_codes").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val qrFile = File(qrDir, "qr_$timestamp.png")

            val size = 512
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            FileOutputStream(qrFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val resultMarkdown = buildString {
                appendLine("### 🏁 $title")
                appendLine()
                appendLine("QR Code has been generated successfully on-device!")
                appendLine()
                appendLine("• **Content:** `$text`")
                appendLine()
                appendLine("![QR Code](file://${qrFile.absolutePath})")
            }

            ToolResult.success(
                content = resultMarkdown,
                data = qrFile.absolutePath
            )
        } catch (e: Exception) {
            ToolResult.failure("Failed to generate QR code: ${e.message}")
        }
    }

    companion object {
        fun isQrCodeRequest(prompt: String): Boolean {
            val p = prompt.lowercase().trim()
            val keywords = listOf("qr code", "qrcode", "qr cheyyi", "qr generate", "qr maker", "qr code create")
            return keywords.any { p.contains(it) }
        }
    }
}
