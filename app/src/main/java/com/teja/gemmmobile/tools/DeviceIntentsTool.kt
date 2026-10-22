package com.teja.gemmmobile.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * On-device Android system intents tool.
 *
 * Executes device actions requested in English or Telugu:
 * - Set Alarm ("రేపు పొద్దున్న 6 గంటలకి అలారం పెట్టు", "Set alarm for 7:30 AM")
 * - Set Timer ("10 నిమిషాలు టైమర్ పెట్టు", "Set a 5 minute timer")
 * - Open Apps ("WhatsApp ఓపెన్ చెయ్", "Open Camera", "Open YouTube")
 * - Calendar Events ("Calendar లో రిమైండర్ సేవ్ చెయ్", "Add doctor appointment tomorrow")
 * - Open Web URL ("Open google.com")
 */
class DeviceIntentsTool(private val context: Context) : GemmaTool {

    override val name: String = "device_intents"

    override val description: String =
        "Executes on-device Android actions: setting alarms, setting timers, opening installed apps like WhatsApp, or adding calendar reminders."

    override val parametersJsonSchema: String = """
        {
          "type": "object",
          "properties": {
            "action": {
              "type": "string",
              "enum": ["set_alarm", "set_timer", "open_app", "add_calendar_event", "open_url"],
              "description": "The specific device action to perform."
            },
            "hour": {
              "type": "integer",
              "description": "Hour of the day (0-23) for alarms."
            },
            "minutes": {
              "type": "integer",
              "description": "Minutes (0-59) for alarms."
            },
            "seconds": {
              "type": "integer",
              "description": "Duration in seconds for timers."
            },
            "message": {
              "type": "string",
              "description": "Label or description for the alarm, timer, or calendar event."
            },
            "app_name": {
              "type": "string",
              "description": "Name of the app to open (e.g. 'whatsapp', 'camera', 'youtube', 'maps', 'settings', 'calculator')."
            },
            "url": {
              "type": "string",
              "description": "URL to open in the browser."
            }
          },
          "required": ["action"]
        }
    """.trimIndent()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult = withContext(Dispatchers.Main) {
        val action = arguments["action"]?.toString()?.lowercase() ?: return@withContext ToolResult.failure("Missing 'action' parameter")
        val message = arguments["message"]?.toString() ?: ""

        try {
            when (action) {
                "set_alarm" -> {
                    val hour = (arguments["hour"] as? Number)?.toInt() ?: 6
                    val minutes = (arguments["minutes"] as? Number)?.toInt() ?: 0
                    setAlarm(hour, minutes, message.ifEmpty { "Gemma Alarm" })
                }
                "set_timer" -> {
                    val seconds = (arguments["seconds"] as? Number)?.toInt() ?: 300
                    setTimer(seconds, message.ifEmpty { "Gemma Timer" })
                }
                "open_app" -> {
                    val appName = arguments["app_name"]?.toString() ?: ""
                    openApp(appName)
                }
                "add_calendar_event" -> {
                    val title = arguments["title"]?.toString() ?: message.ifEmpty { "Event" }
                    val desc = arguments["description"]?.toString() ?: ""
                    addCalendarEvent(title, desc)
                }
                "open_url" -> {
                    val url = arguments["url"]?.toString() ?: ""
                    openUrl(url)
                }
                else -> ToolResult.failure("Unknown device action: $action")
            }
        } catch (e: Exception) {
            Log.e("DeviceIntentsTool", "Error executing device action $action", e)
            ToolResult.failure("Failed to execute $action: ${e.message}")
        }
    }

    private fun setAlarm(hour: Int, minutes: Int, message: String): ToolResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val timeString = String.format("%02d:%02d %s", displayHour, minutes, amPm)

            ToolResult.success(
                content = "⏰ Alarm successfully scheduled for $timeString ($message).\n(రేపు ఉదయం $timeString కి అలారం సెట్ చేయబడింది)."
            )
        } catch (e: Exception) {
            ToolResult.failure("Could not set alarm: ${e.message}")
        }
    }

    private fun setTimer(seconds: Int, message: String): ToolResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = seconds / 60
            ToolResult.success(
                content = "⏱️ Timer set for $minutes minutes ($message).\n($minutes నిమిషాల టైమర్ సెట్ చేయబడింది)."
            )
        } catch (e: Exception) {
            ToolResult.failure("Could not set timer: ${e.message}")
        }
    }

    private fun openApp(appName: String): ToolResult {
        val pm: PackageManager = context.packageManager
        val cleanName = appName.lowercase().trim()

        // Known common package mappings
        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to null, // handled via camera intent
            "settings" to android.provider.Settings.ACTION_SETTINGS,
            "calculator" to null,
            "gmail" to "com.google.android.gm",
            "calendar" to null
        )

        try {
            if (cleanName.contains("camera")) {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult.success("📸 Opened Camera (కెమెరా ఓపెన్ చేయబడింది).")
            }

            if (cleanName.contains("whatsapp")) {
                val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult.success("💬 Opened WhatsApp (వాట్సాప్ ఓపెన్ చేయబడింది).")
                } else {
                    val playStore = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.whatsapp")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(playStore)
                    return ToolResult.success("WhatsApp is not installed. Opened Google Play Store.")
                }
            }

            if (cleanName.contains("settings")) {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult.success("⚙️ Opened Settings (సెట్టింగ్స్ ఓపెన్ చేయబడ్డాయి).")
            }

            val targetPkg = knownPackages[cleanName]
            if (targetPkg != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult.success("📱 Opened $appName.")
                }
            }

            // General search across all installed applications
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val match = installedApps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label == cleanName || label.contains(cleanName)
            }

            if (match != null) {
                val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult.success("📱 Opened ${pm.getApplicationLabel(match)}.")
                }
            }

            return ToolResult.failure("App '$appName' not found on device.")
        } catch (e: Exception) {
            return ToolResult.failure("Could not open $appName: ${e.message}")
        }
    }

    private fun addCalendarEvent(title: String, description: String): ToolResult {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult.success("📅 Calendar reminder opened for '$title'. (క్యాలెండర్ లో రిమైండర్ సేవ్ చేయబడింది).")
        } catch (e: Exception) {
            ToolResult.failure("Could not open calendar: ${e.message}")
        }
    }

    private fun openUrl(url: String): ToolResult {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult.success("🌐 Opened $validUrl in browser.")
        } catch (e: Exception) {
            ToolResult.failure("Could not open browser: ${e.message}")
        }
    }

    companion object {
        /**
         * Quick heuristic matcher to intercept direct Telugu and English commands.
         */
        fun matchesDeviceIntent(prompt: String): Boolean {
            val p = prompt.lowercase().trim()
            val alarmKeywords = listOf("అలారం", "alarm", "పెట్టు", "set alarm", "wake me up")
            val timerKeywords = listOf("టైమర్", "timer", "నిమిషాలు టైమర్")
            val appKeywords = listOf("whatsapp", "వాట్సాప్", "ఓపెన్ చెయ్", "open whatsapp", "open camera", "open youtube", "open settings")
            val calendarKeywords = listOf("రిమైండర్", "reminder", "calendar", "క్యాలెండర్", "ఈవెంట్")

            val isAlarm = alarmKeywords.count { p.contains(it) } >= 2 || (p.contains("alarm") && (p.contains("set") || p.contains("for")))
            val isTimer = timerKeywords.any { p.contains(it) } && (p.contains("set") || p.contains("పెట్టు") || p.contains("timer"))
            val isApp = (p.contains("open") || p.contains("ఓపెన్") || p.contains("open chey")) && (p.contains("whatsapp") || p.contains("వాట్సాప్") || p.contains("camera") || p.contains("youtube") || p.contains("settings"))
            val isCalendar = calendarKeywords.any { p.contains(it) } && (p.contains("save") || p.contains("సేవ్") || p.contains("add") || p.contains("పెట్టు") || p.contains("set"))

            return isAlarm || isTimer || isApp || isCalendar
        }

        fun parseIntentArguments(prompt: String): Map<String, Any?>? {
            val p = prompt.lowercase().trim()

            // 1. Alarm detection
            if (p.contains("alarm") || p.contains("అలారం")) {
                val hourMatch = Regex("""(\d{1,2})\s*(am|pm|గంటలకి|గంటలకు|o'clock|hours)?""", RegexOption.IGNORE_CASE).find(p)
                var hour = hourMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 6
                val isPm = p.contains("pm") || p.contains("సాయంత్రం") || p.contains("రాత్రి") || p.contains("night") || p.contains("evening")
                val isAm = p.contains("am") || p.contains("పొద్దున్న") || p.contains("ఉదయం") || p.contains("morning")
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0

                val minMatch = Regex("""\d{1,2}:(\d{2})""").find(p)
                val minutes = minMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

                return mapOf(
                    "action" to "set_alarm",
                    "hour" to hour,
                    "minutes" to minutes,
                    "message" to "Alarm"
                )
            }

            // 2. WhatsApp or App detection
            if (p.contains("whatsapp") || p.contains("వాట్సాప్")) {
                return mapOf(
                    "action" to "open_app",
                    "app_name" to "whatsapp"
                )
            }
            if (p.contains("camera") || p.contains("కెమెరా")) {
                return mapOf(
                    "action" to "open_app",
                    "app_name" to "camera"
                )
            }
            if (p.contains("youtube") || p.contains("యూట్యూబ్")) {
                return mapOf(
                    "action" to "open_app",
                    "app_name" to "youtube"
                )
            }

            // 3. Calendar reminder detection
            if (p.contains("calendar") || p.contains("క్యాలెండర్") || p.contains("reminder") || p.contains("రిమైండర్")) {
                val cleanTitle = prompt
                    .replace(Regex("""(?i)\b(calendar|లో|reminder|సేవ్|చెయ్|పెట్టు|save|add|to)\b"""), "")
                    .trim()
                    .ifEmpty { "Reminder" }
                return mapOf(
                    "action" to "add_calendar_event",
                    "message" to cleanTitle
                )
            }

            return null
        }
    }
}
