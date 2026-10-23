package com.teja.gemmmobile.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.teja.gemmmobile.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import com.teja.gemmmobile.ocr.DocumentOcrHelper
import com.teja.gemmmobile.ocr.ExtractedDocument
import com.teja.gemmmobile.search.SearchResult
import com.teja.gemmmobile.search.ProfileAvatarLoader
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.style.TextDecoration
import com.teja.gemmmobile.util.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teja.gemmmobile.search.SearchImage
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import com.teja.gemmmobile.storage.ChatSession
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teja.gemmmobile.ai.BackendType
import com.teja.gemmmobile.ai.EngineState
import com.teja.gemmmobile.ai.GemmaConfig
import com.teja.gemmmobile.model.ModelInstallState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val installState by viewModel.installState.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val config by viewModel.config.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val memoryUpdatedEvent by viewModel.memoryUpdatedEvent.collectAsState()
    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsState()
    val currentTheme by viewModel.selectedTheme.collectAsState()

    var showSettingsPage by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var activeUserMenuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showCameraOverlay by remember { mutableStateOf(false) }
    var showAddFilesSheet by remember { mutableStateOf(false) }
    var showAppIntro by remember { mutableStateOf(true) }
    var showModelInfoSheet by remember { mutableStateOf(false) }

    // Let the local model load on its own worker thread while the UI is already
    // usable. This avoids opening to a technical CPU/loading screen.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1050)
        showAppIntro = false
    }

    if (showAppIntro && installState is ModelInstallState.Installed) {
        AppIntroOverlay()
    }

    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    var inputDockHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = isImeVisible || imeBottomDp > 0.dp
    val bottomInsetsDp = maxOf(navBarBottomDp, imeBottomDp)
    val effectiveBottomInset = bottomInsetsDp
    val inputDockHeightDp = with(density) {
        if (inputDockHeightPx > 0) inputDockHeightPx.toDp() else 68.dp
    }

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val activeGeneratingSessionId by viewModel.activeGeneratingSessionId.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentlySpeakingId by viewModel.currentlySpeakingId.collectAsState()
    val attachedDocument by viewModel.attachedDocument.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val storageUsageText by viewModel.storageUsageText.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Contacts permission enabled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(memoryUpdatedEvent) {
        if (memoryUpdatedEvent != null) {
            kotlinx.coroutines.delay(3500L)
            viewModel.clearMemoryUpdatedEvent()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importModel(it) }
    }

    // Voice Input Launcher (Native SpeechRecognizer Intent)
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val current = viewModel.inputText.value
                val combined = if (current.isBlank()) spoken else "$current $spoken"
                viewModel.onInputTextChanged(combined)
            }
        }
    }

    // Document Picker Launcher (PDF & TXT)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                isOcrProcessing = true
                try {
                    val mime = context.contentResolver.getType(it) ?: ""
                    val doc = if (mime == "application/pdf" || it.toString().endsWith(".pdf", ignoreCase = true)) {
                        DocumentOcrHelper.processPdfUri(context, it)
                    } else if (mime.startsWith("image/")) {
                        DocumentOcrHelper.processImageUri(context, it)
                    } else {
                        DocumentOcrHelper.processTextUri(context, it)
                    }
                    viewModel.attachDocument(doc)
                    val info = if (doc.isImage) "" else " (${doc.wordCount} words)"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Attached ${doc.fileName}$info", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isOcrProcessing = false
                }
            }
        }
    }

    // Chat-style file picker: users can select a small batch of documents and ask
    // one question about all of them. The on-device prompt is kept compact below.
    val multiDocumentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isOcrProcessing = true
            try {
                val documents = withContext(Dispatchers.IO) {
                    uris.take(5).map { uri ->
                        val mime = context.contentResolver.getType(uri).orEmpty()
                        when {
                            mime == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true) ->
                                DocumentOcrHelper.processPdfUri(context, uri)
                            mime.startsWith("image/") -> DocumentOcrHelper.processImageUri(context, uri)
                            else -> DocumentOcrHelper.processTextUri(context, uri)
                        }
                    }
                }
                val attachment = combineDocumentsForChat(documents)
                viewModel.attachDocument(attachment)
                val label = if (documents.size == 1) attachment.fileName else "${documents.size} files"
                Toast.makeText(context, "Attached $label", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading files: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isOcrProcessing = false
            }
        }
    }

    // Camera Capture Launcher — pure native multimodal vision
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val name = "Photo_${System.currentTimeMillis() % 10000}.jpg"
                    val doc = DocumentOcrHelper.processImageBitmap(it, name)
                    viewModel.attachDocument(doc)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "📷 Photo attached", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to attach photo: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Image Picker Launcher — pure native multimodal vision
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val doc = DocumentOcrHelper.processImageUri(context, it)
                    viewModel.attachDocument(doc)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "🖼️ Image attached", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read image: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    var isScrollToBottomVisible by remember { mutableStateOf(false) }

    // ChatGPT-style Drag Up at end of page to Start New Chat
    val pullOffsetAnim = remember { Animatable(0f) }
    val pullTriggerThresholdPx = with(density) { 76.dp.toPx() }
    val pullMaxOffsetPx = with(density) { 130.dp.toPx() }
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    LaunchedEffect(messages.isEmpty()) {
        if (messages.isEmpty()) {
            pullOffsetAnim.snapTo(0f)
            hasTriggeredThresholdHaptic = false
        }
    }

    val pullToNewChatConnection = remember(messages.isNotEmpty(), listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (messages.isEmpty()) return Offset.Zero
                // If user was pulling up and now drags finger DOWN (available.y > 0f), consume downward delta to collapse offset
                if (pullOffsetAnim.value > 0f && available.y > 0f && source == NestedScrollSource.UserInput) {
                    val newOffset = (pullOffsetAnim.value - available.y).coerceAtLeast(0f)
                    scope.launch { pullOffsetAnim.snapTo(newOffset) }
                    if (newOffset < pullTriggerThresholdPx) {
                        hasTriggeredThresholdHaptic = false
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // When at the bottom of the list and user drags finger UP (available.y < 0f)
                if (messages.isNotEmpty() && source == NestedScrollSource.UserInput && available.y < 0f) {
                    val isAtBottom = !listState.canScrollForward
                    if (isAtBottom) {
                        val upwardDelta = -available.y
                        // Diminishing resistance as offset grows (smooth rubber-band physics)
                        val resistance = 0.45f * (1f - (pullOffsetAnim.value / (pullMaxOffsetPx * 1.5f)).coerceIn(0f, 0.65f))
                        val newOffset = (pullOffsetAnim.value + upwardDelta * resistance).coerceAtMost(pullMaxOffsetPx)
                        scope.launch { pullOffsetAnim.snapTo(newOffset) }
                        if (newOffset >= pullTriggerThresholdPx && !hasTriggeredThresholdHaptic) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hasTriggeredThresholdHaptic = true
                        } else if (newOffset < pullTriggerThresholdPx) {
                            hasTriggeredThresholdHaptic = false
                        }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (messages.isEmpty()) return Velocity.Zero
                if (pullOffsetAnim.value > 0f) {
                    val triggered = pullOffsetAnim.value >= pullTriggerThresholdPx
                    if (triggered) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        withContext(Dispatchers.Main) {
                            viewModel.createNewChat()
                            Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                        }
                    }
                    hasTriggeredThresholdHaptic = false
                    pullOffsetAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // Floating Scroll Down Button Auto-Hide (ChatGPT style):
    // Appears on scroll interaction, stays for 2 seconds, then smoothly auto-hides.
    // Reappears on new scroll touch, and immediately hides when reaching the bottom.
    LaunchedEffect(listState.isScrollInProgress, canScrollForward) {
        if (!canScrollForward) {
            isScrollToBottomVisible = false
        } else if (listState.isScrollInProgress) {
            isScrollToBottomVisible = true
        } else if (isScrollToBottomVisible) {
            kotlinx.coroutines.delay(2000L)
            isScrollToBottomVisible = false
        }
    }

    // When user scrolls messages, dismiss keyboard if it was open (never opens on scroll!)
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isImeVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Haptic feedback when generation completes
    var prevGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(isGenerating) {
        if (prevGenerating && !isGenerating) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevGenerating = isGenerating
    }

    // When a new message is added, animate to bottom (use lastIndex, not size, to stay in bounds)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // +1 for bottom_anchor spacer item after the messages list
            val target = messages.size // points to follow_up_chips or bottom_anchor
            listState.animateScrollToItem(target)
        }
    }

    // While generating: smooth throttled auto-scroll so we don't spam scrollToItem on every single character
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (true) {
                kotlinx.coroutines.delay(60L) // smooth 60ms frame-aligned throttle
                if (!isGenerating) break
                if (!listState.isScrollInProgress && messages.isNotEmpty()) {
                    val totalItems = listState.layoutInfo.totalItemsCount
                    val layout = listState.layoutInfo
                    val lastVisible = layout.visibleItemsInfo.lastOrNull()
                    val nearBottom = lastVisible != null && lastVisible.index == totalItems - 1 &&
                        lastVisible.offset + lastVisible.size <= layout.viewportEndOffset + 120
                    if (totalItems > 0 && nearBottom) {
                        listState.scrollToItem(messages.size)
                    }
                }
            }
        }
    }

    // User Message Options Dialog (Edit / Copy)
    if (activeUserMenuMessage != null) {
        val userMsg = activeUserMenuMessage!!
        AlertDialog(
            onDismissRequest = { activeUserMenuMessage = null },
            containerColor = Color(0xFF171717),
            title = { Text("Message Options", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onInputTextChanged(userMsg.text)
                                editingMessageId = userMsg.id
                                activeUserMenuMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Edit message", fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val cm = (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("user_msg", userMsg.text))
                                Toast.makeText(context, "Copied prompt to clipboard", Toast.LENGTH_SHORT).show()
                                activeUserMenuMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Copy prompt", fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { activeUserMenuMessage = null }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    val activity = LocalContext.current as? Activity
    BackHandler(enabled = !showSettingsPage && !drawerState.isOpen) {
        activity?.moveTaskToBack(true)
    }

    if (showSettingsPage) {
        LaunchedEffect(Unit) {
            viewModel.refreshStorageUsage()
        }
        SettingsScreen(
            currentConfig = config,
            isWebSearchEnabled = isWebSearchEnabled,
            selectedTheme = currentTheme,
            onSelectTheme = { viewModel.setTheme(it) },
            memories = memories,
            onDeleteMemory = { viewModel.removeMemory(it) },
            onClearAllMemories = { viewModel.clearMemories() },
            storageUsageText = storageUsageText,
            onClearCache = { onComplete ->
                viewModel.clearCacheAndTempFiles(onComplete)
            },
            onToggleWebSearch = { viewModel.toggleWebSearch() },
            onApplyConfig = { newConfig ->
                viewModel.updateConfig(newConfig)
                showSettingsPage = false
            },
            onResetDefaults = { viewModel.resetConfig() },
            onBack = {
                scope.launch { drawerState.snapTo(androidx.compose.material3.DrawerValue.Closed) }
                showSettingsPage = false
            }
        )
        return
    }

    if (showMemoryDialog) {
        MemoryDialog(
            memories = memories,
            onAddMemory = { viewModel.addMemory(it) },
            onDeleteMemory = { viewModel.removeMemory(it) },
            onClearAll = { viewModel.clearMemories() },
            onDismiss = { showMemoryDialog = false }
        )
    }

    if (showAttachmentDialog) {
        AttachmentDialog(
            onDismiss = { showAttachmentDialog = false },
            onTakePhoto = { cameraLauncher.launch(null) },
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onPickDocument = { docPickerLauncher.launch(arrayOf("application/pdf", "text/*")) },
            isWebSearchEnabled = isWebSearchEnabled,
            onToggleWebSearch = { viewModel.toggleWebSearch() }
        )
    }

    if (showModelInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showModelInfoSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E22),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF48484E))
                )
            }
        ) {
            ModelInfoSheetContent(
                engineState = engineState,
                isWebSearchEnabled = isWebSearchEnabled,
                onToggleWebSearch = { viewModel.toggleWebSearch() },
                onOpenSettings = {
                    showModelInfoSheet = false
                    showSettingsPage = true
                },
                onDismiss = { showModelInfoSheet = false }
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Conversation?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this conversation from your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentChat()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Chat Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameCurrentChat(renameText.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color(0xFF171717)
            ) {
                RecentChatsDrawer(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    activeGeneratingSessionId = activeGeneratingSessionId,
                    isWebSearchEnabled = isWebSearchEnabled,
                    onNewChat = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onSelectSession = { id ->
                        viewModel.selectSession(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { id ->
                        viewModel.deleteSession(id)
                    },
                    onShareSession = { session ->
                        ExportHelper.shareSession(context, session)
                    },
                    onClearAll = {
                        viewModel.clearAllChats()
                        scope.launch { drawerState.close() }
                    },
                    onOpenVision = {
                        scope.launch { drawerState.close() }
                        imagePickerLauncher.launch("image/*")
                    },
                    onToggleWebSearch = {
                        viewModel.toggleWebSearch()
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showSettingsPage = true
                    },
                    onOpenMemory = {
                        showMemoryDialog = true
                    },
                    onLaunchVoice = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemma…")
                        }
                        voiceLauncher.launch(intent)
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(currentTheme.background)
        ) {
            // Main content depending on engine & install state
            when {
                installState !is ModelInstallState.Installed -> {
                    ModelManagementSection(
                        installState = installState,
                        onImportClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onRetryCheck = { viewModel.checkModel() },
                        viewModel = viewModel
                    )
                }

                engineState is EngineState.Error -> {
                    val errorState = engineState as EngineState.Error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚠️", fontSize = 44.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Model Loading Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.initializeEngine() }
                                    ) {
                                        Text("Retry")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.deleteModel()
                                            viewModel.checkModel()
                                        }
                                    ) {
                                        Text("Delete & Re-Download")
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Chat Interface - Full-Page Edge-to-Edge with Vignette Transparency
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (messages.isEmpty()) {
                            EmptyChatHero(
                                onPromptSelected = { prompt ->
                                    viewModel.onInputTextChanged(prompt)
                                },
                                onExamineSelected = {
                                    imagePickerLauncher.launch("image/*")
                                },
                                modifier = Modifier
                                    .widthIn(max = 840.dp)
                                    .fillMaxWidth()
                                    .padding(
                                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
                                        bottom = effectiveBottomInset + inputDockHeightDp + 16.dp
                                    )
                            )
                        } else {
                            val onSpeakStable = remember(viewModel) {
                                { id: String, text: String -> viewModel.toggleSpeak(id, text) }
                            }
                            val onShareStable = remember(context) {
                                { text: String -> ExportHelper.shareMessage(context, text) }
                            }
                            val onExportPdfStable = remember(context, scope) {
                                { text: String -> ExportHelper.exportMessageAsPdf(context, text, null, scope) }
                            }
                            val onRegenerateStable = remember(viewModel) {
                                { viewModel.regenerateLastResponse() }
                            }
                            val onLongPressUserMessageStable = remember {
                                { msg: ChatMessage -> activeUserMenuMessage = msg }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .widthIn(max = 840.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .nestedScroll(pullToNewChatConnection)
                                    .graphicsLayer {
                                        translationY = -pullOffsetAnim.value * 0.45f
                                    },
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 58.dp,
                                    bottom = effectiveBottomInset + inputDockHeightDp + 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(messages, key = { it.id }) { message ->
                                    MessageBubble(
                                        message = message,
                                        isLatest = message.id == messages.lastOrNull()?.id,
                                        isSpeaking = isSpeaking && currentlySpeakingId == message.id,
                                        onSpeak = onSpeakStable,
                                        onShare = onShareStable,
                                        onExportPdf = { text -> viewModel.generatePdfForMessage(message.id, text) },
                                        onRegenerate = onRegenerateStable,
                                        onFeedback = { feedback -> viewModel.setResponseFeedback(message.id, feedback) },
                                        onLongPressUserMessage = onLongPressUserMessageStable,
                                        onContinueGenerating = { id -> viewModel.continueGeneration(id) }
                                    )
                                }

                                item(key = "bottom_anchor") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }



            // Top Vignette & Floating Controls Header (Edge-to-Edge ChatGPT style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Top Vignette Gradient: Smooth feathering from status bar into chat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to currentTheme.background.copy(alpha = 0.95f),
                                0.55f to currentTheme.background.copy(alpha = 0.60f),
                                0.85f to currentTheme.background.copy(alpha = 0.20f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Floating Action Bar Row
                // Floating Action Bar Row - ChatGPT 1:1 ConversationTopBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Bare Hamburger Menu Button (ChatGPT MenuIconButton)
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open drawer",
                            tint = Color(0xFFCDCDCD),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center: ChatGPT ConversationTopBarTitleStack (Title + Chevron)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showModelInfoSheet = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Gemma",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Model select",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Right: Bare ChatGPT Action Buttons (New Chat + 3-Dots Menu)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // New Chat Button (ConversationTopBarNewChatButton)
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.createNewChat()
                            },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "New chat",
                                tint = Color(0xFFCDCDCD),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 3-Dots More Options Menu (ConversationTopBarIconButton)
                        var showTopMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color(0xFFCDCDCD),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Dark Solid ChatGPT Dropdown Menu (#212121 surface, #323232 border)
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF212121))
                                    .border(1.dp, Color(0xFF323232), RoundedCornerShape(14.dp))
                            ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSettingsPage = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Memories", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showTopMenu = false
                                    showMemoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isWebSearchEnabled) "Web Search (ON)" else "Web Search (OFF)",
                                        color = if (isWebSearchEnabled) Color(0xFF10A37F) else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (isWebSearchEnabled) Color(0xFF10A37F) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.toggleWebSearch()
                                }
                            )
                            if (messages.isNotEmpty()) {
                                HorizontalDivider(color = Color(0xFF383838), thickness = 0.5.dp)
                                DropdownMenuItem(
                                    text = { Text("Share conversation", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        viewModel.getCurrentSession()?.let { session ->
                                            ExportHelper.shareSession(context, session)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as PDF", color = Color.White) },
                                    leadingIcon = {
                                        Icon(painter = painterResource(id = R.drawable.ic_chatgpt_pdf), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        viewModel.getCurrentSession()?.let { session ->
                                            ExportHelper.exportSessionAsPdf(context, session, scope)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename", color = Color.White) },
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        renameText = viewModel.getCurrentSession()?.title ?: ""
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete conversation", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Error Banner (floating under top bar)
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 54.dp)
            ) {
                errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.retryLastResponse() }) {
                                Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // ChatGPT-style Floating Indicator for Drag-Up New Chat at end of page
            if (messages.isNotEmpty() && pullOffsetAnim.value > 2f) {
                val pullProgress = (pullOffsetAnim.value / pullTriggerThresholdPx).coerceIn(0f, 1f)
                val isTriggered = pullOffsetAnim.value >= pullTriggerThresholdPx
                val pillBottomOffset = with(density) {
                    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    navBottom + 82.dp + (pullOffsetAnim.value * 0.35f).toDp()
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE1E1E22),
                    border = BorderStroke(
                        1.dp,
                        if (isTriggered) Color(0xFF10A37F) else Color(0x33FFFFFF)
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = pillBottomOffset)
                        .graphicsLayer {
                            alpha = (pullProgress * 1.5f).coerceIn(0f, 1f)
                            scaleX = 0.85f + 0.15f * pullProgress
                            scaleY = 0.85f + 0.15f * pullProgress
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isTriggered) Color(0xFF10A37F) else Color(0xFFCCCCCC),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(pullProgress * 180f)
                        )
                        Text(
                            text = if (isTriggered) "Release for new chat" else "Pull up for new chat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isTriggered) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isTriggered) Color.White else Color(0xFFAAAAAA)
                        )
                    }
                }
            }

            // Bottom Vignette & Floating Input Dock
            if (installState is ModelInstallState.Installed && (engineState is EngineState.Ready || engineState is EngineState.Generating)) {
                // Bottom Vignette Gradient: Smooth feathering into chat from bottom edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(inputDockHeightDp + effectiveBottomInset + 16.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.30f to currentTheme.background.copy(alpha = 0.20f),
                                0.60f to currentTheme.background.copy(alpha = 0.55f),
                                0.85f to currentTheme.background.copy(alpha = 0.82f),
                                1.0f to currentTheme.background.copy(alpha = 0.95f)
                            )
                        )
                )

                // 3. Floating Input Dock and Actions (safely padded above system navigation bar & keyboard)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = effectiveBottomInset + 6.dp)
                ) {

                    // Independent Floating ChatGPT style Memory Updated Notification
                    AnimatedVisibility(
                        visible = memoryUpdatedEvent != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                bottom = inputDockHeightDp + if (isScrollToBottomVisible && canScrollForward) 58.dp else 12.dp
                            )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = currentTheme.surface,
                            border = BorderStroke(1.dp, currentTheme.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .liquidBounceClick(scaleDown = 0.94f) { showMemoryDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧠",
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = memoryUpdatedEvent ?: "Memory updated",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFECECEC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss memory notification",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { viewModel.clearMemoryUpdatedEvent() }
                                )
                            }
                        }
                    }

                    // Independent Floating Scroll-to-Bottom Arrow (firmly anchored above input bar with zero overlap)
                    AnimatedVisibility(
                        visible = isScrollToBottomVisible && canScrollForward,
                        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
                        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = inputDockHeightDp + 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentTheme.surface,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, currentTheme.border),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .liquidBounceClick(scaleDown = 0.86f) {
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll to bottom",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Input Dock Column firmly anchored at bottom
                    ChatInputDock(
                        viewModel = viewModel,
                        isGenerating = isGenerating,
                        isWebSearchEnabled = isWebSearchEnabled,
                        engineState = engineState,
                        attachedDocument = attachedDocument,
                        editingMessageId = editingMessageId,
                        showAttachmentMenu = showAttachmentMenu,
                        onCancelEdit = { editingMessageId = null },
                        onToggleAttachmentMenu = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAttachmentMenu = !showAttachmentMenu
                        },
                        onLaunchVoice = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemma…")
                            }
                            voiceLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onGloballyPositioned { coordinates ->
                                if (Math.abs(inputDockHeightPx - coordinates.size.height) > 3) {
                                    inputDockHeightPx = coordinates.size.height
                                }
                            }
                    )
                }
            }
        }
    }

    // Floating Attachment Popup Menu (matching ChatGPT AMOLED pure dark screenshot)
    if (showAttachmentMenu) {
        // Scrim using pointerInput so it NEVER steals focus from the text field
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        showAttachmentMenu = false
                    }
                }
        )
    }

    AnimatedVisibility(
        visible = showAttachmentMenu,
        enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.20f,
                    transformOrigin = TransformOrigin(0.08f, 1.0f)
                ),
        exit = fadeOut(animationSpec = tween(150)) +
               scaleOut(
                   animationSpec = spring(
                       dampingRatio = 0.85f,
                       stiffness = Spring.StiffnessMedium
                   ),
                   targetScale = 0.20f,
                   transformOrigin = TransformOrigin(0.08f, 1.0f)
               )
    ) {
        // Floating Dark Card anchored directly above the + button with windowInsetsPadding so it stays with keyboard!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, bottom = inputDockHeightDp + effectiveBottomInset + 12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = currentTheme.surface,
                border = BorderStroke(1.dp, currentTheme.border),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FloatingMenuItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        onClick = {
                            showAttachmentMenu = false
                            showCameraOverlay = true
                        }
                    )
                    FloatingMenuItem(
                        icon = Icons.Default.Image,
                        label = "Photos",
                        onClick = {
                            showAttachmentMenu = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                    FloatingMenuItem(
                        icon = Icons.Default.AttachFile,
                        label = "Files",
                        onClick = {
                            showAttachmentMenu = false
                            showAddFilesSheet = true
                        }
                    )
                    HorizontalDivider(
                        color = Color(0xFF383838),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    FloatingMenuItemWithToggle(
                        icon = Icons.Default.Language,
                        label = "Web search",
                        isChecked = isWebSearchEnabled,
                        onToggle = {
                            viewModel.toggleWebSearch()
                        }
                    )
                    FloatingMenuItemWithToggle(
                        icon = Icons.Default.Psychology,
                        label = "Think harder",
                        isChecked = config.enableThinking,
                        onToggle = {
                            viewModel.updateConfig(config.copy(enableThinking = !config.enableThinking))
                        }
                    )
                }
            }
        }
    }

    // In-Chat Camera Viewfinder Overlay (matching media_1791398963414.jpg)
    InChatCameraOverlay(
        visible = showCameraOverlay,
        onDismiss = { showCameraOverlay = false },
        onPhotoCaptured = { doc ->
            showCameraOverlay = false
            viewModel.attachDocument(doc)
            Toast.makeText(context, "📷 Photo attached", Toast.LENGTH_SHORT).show()
        }
    )

    // AMOLED Dark "Add files" Modal Page (matching media_1791398949494.jpg)
    AddFilesSheet(
        visible = showAddFilesSheet,
        onDismiss = { showAddFilesSheet = false },
        onUploadFiles = {
            multiDocumentPickerLauncher.launch(arrayOf("application/pdf", "text/plain", "text/*", "image/*"))
        },
        onFileSelected = { doc ->
            viewModel.attachDocument(doc)
            Toast.makeText(context, "Attached ${doc.fileName}", Toast.LENGTH_SHORT).show()
        },
        recentFiles = recentFiles
    )
}
}


@Composable
fun RecentChatsDrawer(
    sessions: List<ChatSession>,
    currentSessionId: String,
    activeGeneratingSessionId: String? = null,
    isWebSearchEnabled: Boolean = false,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onShareSession: (ChatSession) -> Unit,
    onClearAll: () -> Unit,
    onOpenVision: () -> Unit = {},
    onToggleWebSearch: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenMemory: () -> Unit,
    onLaunchVoice: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    var showClearAllConfirm by remember { mutableStateOf(false) }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = Color(0xFF212121),
            title = { Text("Clear All Chats?", fontWeight = FontWeight.SemiBold, color = Color.White) },
            text = { Text("Are you sure you want to delete all saved conversations? This cannot be undone.", color = Color(0xFFB0B0B0)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFF171717))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gemma",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onCloseDrawer, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close drawer",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search chats (ChatGPT capsule)
        var searchQuery by remember { mutableStateOf("") }
        val filteredSessions = remember(sessions, searchQuery) {
            if (searchQuery.isBlank()) sessions
            else sessions.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.messages.any { m -> m.text.contains(searchQuery, ignoreCase = true) }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF212121),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search chats...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick feature shortcuts (matching Screenshot 3)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DrawerShortcutRow(
                icon = Icons.Default.CameraAlt,
                label = "Vision & OCR",
                onClick = onOpenVision
            )
            DrawerShortcutRow(
                icon = Icons.Default.Psychology,
                label = "Memories",
                onClick = { onOpenMemory(); onCloseDrawer() }
            )
            DrawerShortcutRow(
                icon = Icons.Default.Language,
                label = if (isWebSearchEnabled) "Web Search (ON)" else "Web Search",
                tint = if (isWebSearchEnabled) Color(0xFF10A37F) else Color(0xFFB0B0B0),
                onClick = onToggleWebSearch
            )
            DrawerShortcutRow(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = { onOpenSettings(); onCloseDrawer() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Recent chats",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        // Session list
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching chats found" else "No recent chats yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val isSelected = session.id == currentSessionId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF262626) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .liquidBounceClick(scaleDown = 0.98f) { onSelectSession(session.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (session.id == activeGeneratingSessionId) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Writing…",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF10A37F),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onShareSession(session) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share chat",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete chat",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Bottom pinned bar (matching Screenshot 3: Coral Pink Chat button + avatar "TP" + Voice button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coral Pink "+ Chat" pill button
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF43F5E),
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .liquidBounceClick(scaleDown = 0.94f) { onNewChat() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New chat",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Avatar "TP" (Teja Pampana) -> Opens Settings on click (ChatGPT style)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2E2E2E),
                    border = BorderStroke(1.dp, Color(0xFF383838)),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .liquidBounceClick(scaleDown = 0.90f) { onOpenSettings() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "TP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Voice assistant button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF212121),
                    border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .liquidBounceClick(scaleDown = 0.90f) { onLaunchVoice() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Voice assistant",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerShortcutRow(
    icon: ImageVector,
    label: String,
    tint: Color = Color(0xFFB0B0B0),
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .liquidBounceClick(scaleDown = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (tint != Color(0xFFB0B0B0)) tint else Color.White
            )
        }
    }
}

@Composable
private fun AppIntroOverlay() {
    val transition = rememberInfiniteTransition(label = "appIntro")
    val glowScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "introGlowScale"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF102D24),
                    border = BorderStroke(1.dp, Color(0xFF1D8F70)),
                    modifier = Modifier
                        .size(82.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✦", color = Color(0xFF66E1BD), fontSize = 38.sp)
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Gemma",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Private AI, ready when you are",
                    color = Color(0xFF9D9DA5),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun EmptyChatHero(
    onPromptSelected: (String) -> Unit,
    onExamineSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ChatWelcome(onPromptSelected = onPromptSelected, modifier = modifier)
}

@Composable
fun BackendBadge(engineState: EngineState) {
    val (label, bg, fg) = when (engineState) {
        is EngineState.Ready -> {
            when (engineState.backend) {
                BackendType.GPU -> Triple("GPU", Color(0xFF1E3A2F), Color(0xFF10A37F))
                BackendType.CPU_FALLBACK -> Triple("CPU fallback", Color(0xFF3E2D1A), Color(0xFFFFB74D))
                BackendType.CPU -> Triple("CPU", Color(0xFF262629), Color(0xFFA0A0A5))
            }
        }
        is EngineState.Generating -> {
            Triple("Generating...", Color(0xFF1A2A3A), Color(0xFF58A6FF))
        }
        is EngineState.Loading -> {
            Triple("Loading...", Color(0xFF2A1F3D), Color(0xFFB39DDB))
        }
        else -> {
            Triple("Offline", Color(0xFF262629), Color(0xFF8E8E93))
        }
    }

    AnimatedContent(
        targetState = Triple(label, bg, fg),
        transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(180))) },
        label = "backendBadgeTransition"
    ) { (curLabel, curBg, curFg) ->
        Surface(
            color = curBg,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = curLabel,
                color = curFg,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isLatest: Boolean = true,
    isSpeaking: Boolean = false,
    onSpeak: (String, String) -> Unit = { _, _ -> },
    onShare: (String) -> Unit = {},
    onExportPdf: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onFeedback: (ResponseFeedback) -> Unit = {},
    onLongPressUserMessage: (ChatMessage) -> Unit = {},
    onContinueGenerating: (String) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val displayBitmap by androidx.compose.runtime.produceState<Bitmap?>(
        initialValue = message.imageBitmap,
        key1 = message.id,
        key2 = message.imageBitmap,
        key3 = message.imagePath
    ) {
        if (message.imageBitmap != null) {
            value = message.imageBitmap
        } else if (!message.imagePath.isNullOrBlank()) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.teja.gemmmobile.storage.StorageManagerHelper.decodeSampledFromFile(message.imagePath!!, 512)
            }
        } else {
            value = null
        }
    }

    if (isUser) {
        // ── USER bubble: dark charcoal pill, right-aligned, no avatar ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {


            val currentDisplayBitmap = displayBitmap
            BoxWithConstraints {
                val maxBubbleWidth = (maxWidth * 0.85f).coerceIn(240.dp, 560.dp)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF2F2F2F),
                    border = null,
                    modifier = Modifier
                        .widthIn(min = 40.dp, max = maxBubbleWidth)
                        .clip(RoundedCornerShape(18.dp))
                        .combinedClickable(
                            onClick = {
                                onLongPressUserMessage(message)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPressUserMessage(message)
                            }
                        )
                ) {
                Column(
                    modifier = Modifier.padding(
                        if (currentDisplayBitmap != null && message.text.isBlank()) 4.dp
                        else 12.dp
                    )
                ) {
                    if (currentDisplayBitmap != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.graphics.painter.BitmapPainter(currentDisplayBitmap.asImageBitmap()),
                            contentDescription = "User uploaded photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    if (message.text.isNotBlank()) {
                        if (message.text.startsWith("📄 **[")) {
                            val parts = message.text.split("\n\n", limit = 2)
                            val header = parts.getOrNull(0) ?: ""
                            val userPrompt = parts.getOrNull(1) ?: ""

                            // Google AI Edge / NotebookLM Source Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x662A2A38),
                                border = BorderStroke(0.8.dp, Color(0x2EFFFFFF)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Source",
                                        tint = Color(0xFFFFB4AB),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = header.removePrefix("📄 ").replace("**", ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE6E0E9),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (userPrompt.isNotBlank()) {
                                Text(
                                    text = userPrompt,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
} else {
        // ── ASSISTANT: clean ChatGPT style (no avatar header, pure text aligned to left) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {

            val effectiveText = when {
                message.text.isNotEmpty() -> message.text
                !message.isStreaming && message.thoughtText.isNotEmpty() -> message.thoughtText
                else -> ""
            }

            // Thinking card: shown while streaming thoughts OR if thoughtText exists alongside body text
            if (message.thoughtText.isNotEmpty() && (message.text.isNotEmpty() || message.isStreaming)) {
                ThinkingCard(
                    thoughtText = message.thoughtText,
                    isThinking = message.isThinking,
                    isStreaming = message.isStreaming
                )
                if (effectiveText.isNotEmpty() && message.text.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
            }

            // Autonomous tool execution indicator
            if (message.isExecutingTool && !message.isSearchingWeb) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C20),
                    border = BorderStroke(1.dp, Color(0xFF2E2E36)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color(0xFF10A37F),
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = message.toolExecutionStatus ?: "Executing tool...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE5E5E5),
                            fontSize = 11.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            var showSourcesSheet by remember { mutableStateOf(false) }
            var selectedSearchImage by remember { mutableStateOf<SearchImage?>(null) }

            if (selectedSearchImage != null) {
                InAppImagePreviewDialog(
                    image = selectedSearchImage!!,
                    onDismiss = { selectedSearchImage = null }
                )
            }

            // ChatGPT "Searched N sites" collapsible accordion (matching media_1791402016025.jpg)
            if (message.isSearchingWeb) {
                WebSearchActivity(message.toolExecutionStatus ?: "Searching the web")
                Spacer(modifier = Modifier.height(10.dp))
            } else if (message.searchResults.isNotEmpty()) {
                ChatGptSearchedSitesPill(
                    searchResults = message.searchResults,
                    onClick = { showSourcesSheet = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (message.webSearchFailed && isLatest && !message.isStreaming) {
                OutlinedButton(onClick = onRegenerate, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry web search")
                }
                Spacer(Modifier.height(8.dp))
            }

            // ChatGPT / Perplexity Style Images Carousel for visual and image search queries
            if (message.searchImages.isNotEmpty()) {
                SearchImagesCarousel(
                    images = message.searchImages,
                    onImageClick = { selectedSearchImage = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // PDF Compiling Indicator (while generating notes)
            if (message.isStreaming && (message.isPdfDocumentRequest || message.toolExecutionStatus?.contains("PDF", ignoreCase = true) == true)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF241517),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = Color(0xFFEF4444),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Compiling A4 PDF Document...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Prominent Top PDF Document Card (Ready immediately on stream complete)
            if (!message.isStreaming && message.generatedPdfPath != null) {
                GeneratedPdfDocumentCard(
                    title = message.generatedPdfTitle ?: "Document.pdf",
                    pageCount = message.generatedPdfPageCount,
                    filePath = message.generatedPdfPath!!,
                    onOpen = {
                        val file = java.io.File(message.generatedPdfPath!!)
                        if (file.exists()) {
                            com.teja.gemmmobile.pdf.PdfExporter.openPdf(context, file)
                        } else {
                            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        val file = java.io.File(message.generatedPdfPath!!)
                        if (file.exists()) {
                            com.teja.gemmmobile.pdf.PdfExporter.sharePdf(context, file, message.generatedPdfTitle ?: "Document")
                        } else {
                            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Minimal subtle pulsing dot while waiting for first token (ChatGPT style)
            if (effectiveText.isEmpty() && message.isStreaming && message.thoughtText.isEmpty()) {
                val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursorAlpha"
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .size(10.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .background(Color.White, CircleShape)
                )
            } else if (effectiveText.isNotEmpty()) {
                val emailDraft = if (!message.isStreaming && effectiveText.contains("Subject:", ignoreCase = true)) {
                    remember(effectiveText) { extractEmailDraft(effectiveText) }
                } else null
                if (emailDraft != null) {
                    val (preamble, body) = emailDraft
                    if (preamble != null) {
                        MarkdownText(text = preamble, isUser = false, searchResults = message.searchResults)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (!message.isStreaming && effectiveText.contains(":::product", ignoreCase = true)) {
                    val productSegments = remember(effectiveText, message.searchImages, message.searchResults) {
                        parseProductMessageSegments(effectiveText, message.searchImages, message.searchResults)
                    }
                    for (seg in productSegments) {
                        when (seg) {
                            is MessagePart.Text -> {
                                if (seg.markdown.isNotBlank()) {
                                    MarkdownText(text = seg.markdown, isUser = false, searchResults = message.searchResults)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            is MessagePart.Product -> {
                                ProductRecommendationCard(card = seg.card)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                } else {
                    MarkdownText(text = effectiveText, isUser = false, searchResults = message.searchResults)
                }
            }



                // Document Citations Badge (Google AI Edge Gallery / NotebookLM style)
                if (!message.isStreaming && message.citedPages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16161A),
                        border = BorderStroke(1.dp, Color(0xFF28282E))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Sources",
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(13.dp)
                            )
                            val docName = message.sourceFileName?.let { "$it • " } ?: ""
                            val pagesText = message.citedPages.joinToString(", ") { "Page $it" }
                            Text(
                                text = "Sources: $docName$pagesText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC4C7C5)
                            )
                        }
                    }
                }

                // Generated PDF Document Card (Study notes / document attachment card)
                if (!message.isStreaming && message.generatedPdfPath != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GeneratedPdfDocumentCard(
                        title = message.generatedPdfTitle ?: "Document.pdf",
                        pageCount = message.generatedPdfPageCount,
                        filePath = message.generatedPdfPath!!,
                        onOpen = {
                            val file = java.io.File(message.generatedPdfPath!!)
                            if (file.exists()) {
                                com.teja.gemmmobile.pdf.PdfExporter.openPdf(context, file)
                            } else {
                                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            val file = java.io.File(message.generatedPdfPath!!)
                            if (file.exists()) {
                                com.teja.gemmmobile.pdf.PdfExporter.sharePdf(context, file, message.generatedPdfTitle ?: "Document")
                            } else {
                                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Action bar — only after streaming done (matching media_1791402016025.jpg)
                if (!message.isStreaming && effectiveText.isNotEmpty()) {
                    if (message.isCutOff) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF10A37F).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .liquidBounceClick(scaleDown = 0.94f, alphaDown = 0.85f) {
                                    onContinueGenerating(message.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Continue Generating",
                                    tint = Color(0xFF10A37F),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Continue Generating / ఇంకా కొనసాగించు",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF1F5F9)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left icons (Copy, Like, Dislike, Speaker, Share, Regenerate)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Copy
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        clipboardManager.setText(AnnotatedString(effectiveText))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Like
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onFeedback(ResponseFeedback.POSITIVE)
                                        Toast.makeText(context, "Feedback saved", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_thumb_up),
                                    contentDescription = "Good response",
                                    tint = if (message.feedback == ResponseFeedback.POSITIVE) Color(0xFF10A37F) else Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Dislike
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onFeedback(ResponseFeedback.NEGATIVE)
                                        Toast.makeText(context, "Feedback saved", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_thumb_down),
                                    contentDescription = "Bad response",
                                    tint = if (message.feedback == ResponseFeedback.NEGATIVE) Color(0xFFF43F5E) else Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Speak / stop
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onSpeak(message.id, effectiveText)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSpeaking) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chatgpt_stop),
                                        contentDescription = "Stop reading",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(15.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chatgpt_read_aloud),
                                        contentDescription = "Read aloud",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            // Share
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onShare(effectiveText)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_share),
                                    contentDescription = "Share",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Export as PDF
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onExportPdf(effectiveText)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_pdf),
                                    contentDescription = "Export as PDF",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Regenerate
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .liquidBounceClick(scaleDown = 0.85f, alphaDown = 0.75f) {
                                        onRegenerate()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_regenerate),
                                    contentDescription = "Regenerate response",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Right: [in] [in] [S] Sources button (matching media_1791402016025.jpg)
                        if (message.searchResults.isNotEmpty()) {
                            ChatGptSourcesPill(
                                searchResults = message.searchResults,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showSourcesSheet = true
                                }
                            )
                        }
                    }
                }

                if (message.searchResults.isNotEmpty()) {
                    SourcesBottomSheet(
                        visible = showSourcesSheet,
                        searchResults = message.searchResults,
                        onDismiss = { showSourcesSheet = false }
                    )
                }
            }
        }
    }

@Composable
fun GeneratedPdfDocumentCard(
    title: String,
    pageCount: Int,
    filePath: String,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val cleanTitle = if (title.endsWith(".pdf", ignoreCase = true)) title else "$title.pdf"
    val pagesLabel = if (pageCount > 1) "$pageCount Pages" else "1 Page"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpen()
            },
        color = Color(0xFF1E2128),
        border = BorderStroke(1.dp, Color(0xFF2E333D))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF Icon badge with crimson accent box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2D1517)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chatgpt_pdf),
                        contentDescription = "PDF Document",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Metadata
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cleanTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$pagesLabel • Study Notes PDF • Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row: [ Open PDF ] & [ Share ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open Document button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpen()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2E39),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chatgpt_pdf),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF60A5FA)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }

                // Share button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShare()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10A37F).copy(alpha = 0.15f),
                        contentColor = Color(0xFF10A37F)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chatgpt_share),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF10A37F)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// Dynamic, alive ChatGPT-style working indicator (never feels stuck)
@Composable
fun DynamicWorkingIndicator(
    isImage: Boolean = false,
    isWebSearch: Boolean = false
) {
    val statusMessages = remember(isImage, isWebSearch) {
        when {
            isImage -> listOf(
                "Analyzing image…",
                "Examining visual details…",
                "Reading content…",
                "Formulating answer…"
            )
            isWebSearch -> listOf(
                "Searching the web…",
                "Reading live web sources…",
                "Synthesizing answer…"
            )
            else -> listOf(
                "Thinking…",
                "Working on response…",
                "Formulating answer…"
            )
        }
    }

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(statusMessages) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            currentIndex = (currentIndex + 1) % statusMessages.size
        }
    }

    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(13.dp),
            strokeWidth = 1.8.dp,
            color = Color(0xFF10A37F)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusMessages[currentIndex],
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = Color(0xFFAAAAAA),
            fontWeight = FontWeight.Medium
        )
    }
}


/**
 * Detects whether the text contains an email draft and returns (preamble, emailContent).
 */
fun extractEmailDraft(text: String): Pair<String?, String>? {
    val t = text.trim()

    // Pattern 1: Code block containing email content
    val codeBlockMatch = Regex("""```(?:email|markdown|text)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE).find(t)
    if (codeBlockMatch != null) {
        val block = codeBlockMatch.groupValues[1].trim()
        if (block.contains("Subject:", ignoreCase = true) || block.contains("Dear ", ignoreCase = true)) {
            val pre = t.substring(0, codeBlockMatch.range.first).trim()
            return Pair(if (pre.isNotBlank()) pre else null, block)
        }
    }

    // Pattern 2: Raw email with "Subject:"
    val subjectIdx = t.indexOf("Subject:", ignoreCase = true)
    if (subjectIdx != -1) {
        val subjectAndRest = t.substring(subjectIdx).trim()
        val lower = subjectAndRest.lowercase()
        val looksLikeEmail = lower.contains("dear ") || lower.contains("hi ") || 
                             lower.contains("sincerely") || lower.contains("regards") || 
                             lower.contains("respectfully") || lower.contains("thank you") || 
                             lower.contains("thanks") || lower.contains("leave")
        if (looksLikeEmail && subjectAndRest.length > 35) {
            val pre = t.substring(0, subjectIdx).trim()
            return Pair(if (pre.isNotBlank()) pre else null, subjectAndRest)
        }
    }

    // Pattern 3: Email starting with "Dear ..." or "To: ..."
    if (t.startsWith("To:", ignoreCase = true) || t.startsWith("Dear ", ignoreCase = true)) {
        val lower = t.lowercase()
        val looksLikeEmail = lower.contains("sincerely") || lower.contains("regards") || 
                             lower.contains("respectfully") || lower.contains("thank you")
        if (looksLikeEmail && t.length > 50) {
            return Pair(null, t)
        }
    }

    return null
}

@Composable
fun EmailDraftBox(
    draftText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF212121),
        border = BorderStroke(
            1.dp,
            Color(0xFF2E2E2E)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar with Copy button
            Surface(
                color = Color(0xFF282828),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email Draft",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // ChatGPT style Copy Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF383838),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(draftText))
                                Toast.makeText(context, "Email draft copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = Color(0xFF2E2E2E),
                thickness = 0.5.dp
            )

            // Draft Body
            Box(modifier = Modifier.padding(14.dp)) {
                MarkdownText(
                    text = draftText,
                    isUser = false
                )
            }
        }
    }
}

@Composable
fun ThinkingCard(
    thoughtText: String,
    isThinking: Boolean,
    isStreaming: Boolean
) {
    var userToggledState by remember { mutableStateOf<Boolean?>(null) }
    val isExpanded = userToggledState ?: (isThinking && isStreaming)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(
            1.dp,
            Color(0xFF2E2E2E)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        userToggledState = !isExpanded
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = if (isThinking && isStreaming) Color(0xFF10A37F) else Color(0xFFB0B0B0),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isThinking && isStreaming) "Thinking live..." else "Thought process",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isThinking && isStreaming) Color(0xFF10A37F) else Color(0xFFB0B0B0)
                )
                if (isThinking && isStreaming) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF10A37F)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isExpanded) "Collapse" else "Expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(end = 2.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(rotationZ = if (isExpanded) 180f else 0f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(
                        color = Color(0xFF2E2E2E),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = thoughtText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color(0xFFCCCCCC)
                    )
                }
            }
        }
    }
}

fun extractDomain(url: String): String {
    return try {
        val uri = Uri.parse(url)
        var host = uri.host ?: ""
        if (host.startsWith("www.")) {
            host = host.substring(4)
        }
        host
    } catch (_: Exception) {
        ""
    }
}

/**
 * iOS-inspired Liquid Spring Touch Feedback modifier.
 * Provides organic squish-on-press with elastic spring bounce on release,
 * subtle alpha dip, and crisp tactile haptic feedback (no boxy Android ripple).
 */
@Composable
fun Modifier.liquidBounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    alphaDown: Float = 0.88f,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && hapticFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed) Spring.DampingRatioNoBouncy else 0.65f,
            stiffness = if (isPressed) Spring.StiffnessHigh else Spring.StiffnessMediumLow
        ),
        label = "liquidScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) alphaDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidAlpha"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun ChatGptSurfaceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    color: Color = Color.Transparent,
    border: BorderStroke? = null,
    shadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .liquidBounceClick(
                enabled = enabled,
                scaleDown = 0.92f,
                alphaDown = 0.88f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun DomainFaviconBadge(
    url: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp
) {
    val host = remember(url) {
        try { java.net.URI(url).host?.removePrefix("www.")?.lowercase() ?: "" } catch (_: Exception) { "" }
    }
    var faviconBmp by remember(url) { mutableStateOf(com.teja.gemmmobile.search.FaviconLoader.getCached(url)) }

    LaunchedEffect(url) {
        if (faviconBmp == null) {
            try {
                val bmp = com.teja.gemmmobile.search.FaviconLoader.loadFavicon(url)
                if (bmp != null) {
                    faviconBmp = bmp
                }
            } catch (_: Throwable) {}
        }
    }

    val isLinkedIn = host.contains("linkedin")
    val isGitHub = host.contains("github")
    val isWikipedia = host.contains("wikipedia")
    val isTwitter = host.contains("twitter") || host.contains("x.com")
    val isYoutube = host.contains("youtube") || host.contains("youtu.be")
    val isFacebook = host.contains("facebook") || host.contains("fb.com")
    val isInstagram = host.contains("instagram")
    val isSrm = host.contains("srm")

    val bg = when {
        isLinkedIn -> Color(0xFF0A66C2)
        isGitHub -> Color(0xFF24292E)
        isWikipedia -> Color(0xFF636466)
        isTwitter -> Color(0xFF1D9BF0)
        isYoutube -> Color(0xFFFF0000)
        isFacebook -> Color(0xFF1877F2)
        isInstagram -> Color(0xFFE1306C)
        isSrm -> Color(0xFF0F9D58)
        else -> Color(0xFF2A2A2E)
    }

    val label = when {
        isLinkedIn -> "in"
        isGitHub -> "gh"
        isWikipedia -> "W"
        isTwitter -> "𝕏"
        isYoutube -> "▶"
        isFacebook -> "f"
        isInstagram -> "📷"
        isSrm -> "S"
        host.isNotEmpty() -> host.first().uppercase()
        else -> "🌐"
    }

    // Circular favicon badge (exact ChatGPT style)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (faviconBmp != null) Color(0xFF1E1E22) else bg),
        contentAlignment = Alignment.Center
    ) {
        val bmp = faviconBmp
        val imgBitmap = remember(bmp) {
            if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                try {
                    bmp.asImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            } else null
        }
        if (imgBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = imgBitmap,
                contentDescription = host,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Text(
                text = label,
                fontSize = (size.value * 0.52f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

/**
 * Data model for ChatGPT-style product recommendation cards.
 */
@Immutable
data class ProductCardData(
    val title: String,
    val badge: String? = null,
    val price: String? = null,
    val description: String = "",
    val sourceDomain: String = "",
    val url: String = "",
    val imageUrl: String? = null
)

sealed class MessagePart {
    data class Text(val markdown: String) : MessagePart()
    data class Product(val card: ProductCardData) : MessagePart()
}

fun parseProductMessageSegments(
    rawText: String,
    searchImages: List<SearchImage>,
    searchResults: List<SearchResult>
): List<MessagePart> {
    if (!rawText.contains(":::product", ignoreCase = true)) {
        return listOf(MessagePart.Text(rawText))
    }

    val parts = mutableListOf<MessagePart>()
    val productBlockRegex = Regex(":::product\\s*([\\s\\S]*?)(?::::|$)", RegexOption.IGNORE_CASE)

    var lastIndex = 0
    var cardIndex = 0

    for (match in productBlockRegex.findAll(rawText)) {
        val start = match.range.first
        if (start > lastIndex) {
            val preText = rawText.substring(lastIndex, start).trim()
            if (preText.isNotEmpty()) {
                parts.add(MessagePart.Text(preText))
            }
        }

        val blockContent = match.groupValues[1].trim()
        val card = parseProductCardBlock(blockContent, searchImages, searchResults, cardIndex)
        if (card != null) {
            parts.add(MessagePart.Product(card))
            cardIndex++
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < rawText.length) {
        val postText = rawText.substring(lastIndex).trim()
        if (postText.isNotEmpty()) {
            parts.add(MessagePart.Text(postText))
        }
    }

    return if (parts.isEmpty()) listOf(MessagePart.Text(rawText)) else parts
}

fun parseProductCardBlock(
    block: String,
    searchImages: List<SearchImage>,
    searchResults: List<SearchResult>,
    index: Int
): ProductCardData? {
    if (block.isBlank()) return null

    var title = ""
    var badge: String? = null
    var price: String? = null
    val descLines = mutableListOf<String>()
    var source = ""
    var url = ""

    val lines = block.lines()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("title:", ignoreCase = true) -> {
                title = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("badge:", ignoreCase = true) || trimmed.startsWith("pick:", ignoreCase = true) -> {
                val b = trimmed.substringAfter(":").trim()
                if (b.isNotBlank()) badge = b
            }
            trimmed.startsWith("price:", ignoreCase = true) || trimmed.startsWith("specs:", ignoreCase = true) -> {
                val p = trimmed.substringAfter(":").trim()
                if (p.isNotBlank()) price = p
            }
            trimmed.startsWith("source:", ignoreCase = true) || trimmed.startsWith("store:", ignoreCase = true) -> {
                source = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("url:", ignoreCase = true) || trimmed.startsWith("link:", ignoreCase = true) -> {
                url = trimmed.substringAfter(":").trim()
            }
            trimmed.startsWith("description:", ignoreCase = true) || trimmed.startsWith("desc:", ignoreCase = true) -> {
                val d = trimmed.substringAfter(":").trim()
                if (d.isNotBlank()) descLines.add(d)
            }
            trimmed.isNotBlank() && !trimmed.startsWith(":::") -> {
                descLines.add(trimmed)
            }
        }
    }

    if (title.isBlank()) {
        title = lines.firstOrNull { it.isNotBlank() && !it.startsWith(":::") }?.trim() ?: "Recommended Product"
    }

    val resolvedUrl = resolveProductUrl(title, if (url.isNotBlank()) url else findMatchingResultUrl(title, searchResults), searchResults)
    val finalDomain = when {
        resolvedUrl.contains("amazon") -> "amazon.in"
        resolvedUrl.contains("flipkart") -> "flipkart.com"
        resolvedUrl.contains("nykaa") -> "nykaa.com"
        resolvedUrl.contains("myntra") -> "myntra.com"
        resolvedUrl.contains("meesho") -> "meesho.com"
        resolvedUrl.contains("purplle") -> "purplle.com"
        resolvedUrl.contains("reliance") -> "reliancedigital.in"
        resolvedUrl.contains("blinkit") -> "blinkit.com"
        resolvedUrl.contains("ibp=oshop") -> "Google Shopping"
        source.isNotBlank() && !source.contains("nytimes") && !source.contains("goodhousekeeping") && !source.contains("forbes") -> source
        resolvedUrl.isNotBlank() -> extractDomain(resolvedUrl)
        else -> "amazon.in"
    }

    val matchedImage = findMatchingProductImage(title, searchImages, index)
    val effectivePrice = price ?: run {
        val cleanT = title.lowercase()
        val words = cleanT.split(Regex("""[^a-zA-Z0-9]+""")).filter { it.length >= 3 }
        val matchingRes = searchResults.firstOrNull { res ->
            val rt = res.title.lowercase()
            words.count { rt.contains(it) } >= 2
        } ?: searchResults.getOrNull(index)
        matchingRes?.snippet?.let { snip ->
            Regex("""(?:₹|Rs\.?\s*)\s*(\d[\d,]*\b)""").find(snip)?.value
        }
    }

    return ProductCardData(
        title = title.removePrefix("**").removeSuffix("**").trim(),
        badge = badge,
        price = effectivePrice,
        description = descLines.joinToString(" ").trim(),
        sourceDomain = finalDomain,
        url = resolvedUrl,
        imageUrl = matchedImage
    )
}

fun findMatchingProductImage(title: String, images: List<SearchImage>, index: Int): String? {
    if (images.isEmpty()) return null
    val clean = title.lowercase()
    val words = clean.split(Regex("""[^a-zA-Z0-9]+""")).filter { it.length > 2 }
    val match = images.firstOrNull { img ->
        val ititle = img.title.lowercase()
        words.count { ititle.contains(it) } >= 2
    }
    // Never reuse the exact same photo across distinct product cards
    return match?.imageUrl ?: images.getOrNull(index)?.imageUrl
}

fun resolveProductUrl(title: String, rawUrl: String, results: List<SearchResult>): String {
    val shoppingDomains = setOf(
        "amazon.in", "amazon.com", "flipkart.com", "myntra.com", "nykaa.com",
        "beminimalist.co", "thedermaco.com", "aqualogica.in", "croma.com",
        "reliancedigital.in", "tatacliq.com", "apollopharmacy.in", "pharmeasy.in",
        "meesho.com", "jiomart.com", "purplle.com", "1mg.com", "blinkit.com", "bigbasket.com"
    )
    val lower = rawUrl.lowercase()
    if (shoppingDomains.any { lower.contains(it) } || lower.contains("/dp/") || lower.contains("/p/") || lower.contains("/product/") || lower.contains("/prid/") || lower.contains("/pd/") || lower.contains("ibp=oshop") || lower.contains("prds=catalogid")) {
        return rawUrl
    }

    // Match specific product title keywords against real shopping results
    val clean = title.lowercase()
    val words = clean.split(Regex("""[^a-zA-Z0-9]+""")).filter { it.length >= 3 }
    val storeResult = results.firstOrNull { res ->
        val resUrl = res.url.lowercase()
        val resTitle = res.title.lowercase()
        (shoppingDomains.any { resUrl.contains(it) } || resUrl.contains("ibp=oshop") || resUrl.contains("prds=catalogid")) && words.any { resTitle.contains(it) }
    }
    if (storeResult != null) {
        return storeResult.url
    }

    val anyStoreResult = results.firstOrNull { res ->
        val resUrl = res.url.lowercase()
        shoppingDomains.any { resUrl.contains(it) } || resUrl.contains("ibp=oshop") || resUrl.contains("prds=catalogid")
    }
    if (anyStoreResult != null) {
        return anyStoreResult.url
    }

    // Fall back to direct Amazon.in product search query so user can buy the exact item
    val cleanTitle = title.replace(Regex("""^\d+\.\s*"""), "").replace(Regex("""\([^)]*\)"""), "").trim()
    val encoded = try {
        java.net.URLEncoder.encode(cleanTitle, "UTF-8")
    } catch (_: Exception) {
        cleanTitle.replace(" ", "+")
    }
    return "https://www.amazon.in/s?k=$encoded"
}

fun findMatchingResultUrl(title: String, results: List<SearchResult>): String {
    if (results.isEmpty()) return ""
    val clean = title.lowercase()
    val words = clean.split(Regex("""[^a-zA-Z0-9]+""")).filter { it.length >= 3 }
    val match = results.firstOrNull { res ->
        val rtitle = res.title.lowercase()
        words.count { rtitle.contains(it) } >= 2
    }
    return match?.url ?: ""
}

fun extractFallbackProductCards(
    results: List<SearchResult>,
    images: List<SearchImage>
): List<ProductCardData> {
    val shoppingDomains = setOf(
        "amazon.in", "amazon.com", "flipkart.com", "myntra.com", "nykaa.com",
        "beminimalist.co", "thedermaco.com", "aqualogica.in", "croma.com",
        "reliancedigital.in", "tatacliq.com", "apollopharmacy.in", "pharmeasy.in",
        "meesho.com", "jiomart.com", "purplle.com", "1mg.com", "blinkit.com", "bigbasket.com"
    )

    // MUST be a verified shopping store or direct product listing (NEVER blog articles like forbes/nytimes)
    val candidates = results.filter { res ->
        val url = res.url.lowercase()
        val domain = extractDomain(res.url)
        val isArticle = domain.contains("nytimes") || domain.contains("goodhousekeeping") ||
            domain.contains("forbes") || domain.contains("healthline") || domain.contains("wikipedia")
        !isArticle && (shoppingDomains.any { domain.contains(it) } || url.contains("/dp/") || url.contains("/p/") || url.contains("/product/") || url.contains("/prid/") || url.contains("/pd/") || url.contains("ibp=oshop") || url.contains("prds=catalogid"))
    }

    val cards = mutableListOf<ProductCardData>()
    for ((idx, res) in candidates.take(3).withIndex()) {
        val cleanTitle = res.title
            .substringBefore(" - ")
            .substringBefore(" | ")
            .substringBefore(" : ")
            .trim()

        val badge = when (idx) {
            0 -> "Top Recommendation"
            1 -> "Best Value Pick"
            else -> "Alternative Option"
        }

        val domain = extractDomain(res.url)
        val imgUrl = findMatchingProductImage(cleanTitle, images, idx)
        val purchaseUrl = resolveProductUrl(cleanTitle, res.url, results)

        // Try extracting real price from snippet (e.g. ₹499 or Rs 399)
        val priceMatch = Regex("""(?:₹|Rs\.?\s*)\s*(\d[\d,]*\b)""").find(res.snippet)
        val extractedPrice = priceMatch?.value

        val finalDomain = when {
            purchaseUrl.contains("amazon") -> "amazon.in"
            purchaseUrl.contains("flipkart") -> "flipkart.com"
            purchaseUrl.contains("nykaa") -> "nykaa.com"
            purchaseUrl.contains("myntra") -> "myntra.com"
            purchaseUrl.contains("meesho") -> "meesho.com"
            purchaseUrl.contains("purplle") -> "purplle.com"
            purchaseUrl.contains("reliance") -> "reliancedigital.in"
            purchaseUrl.contains("blinkit") -> "blinkit.com"
            purchaseUrl.contains("ibp=oshop") -> {
                val onMatch = Regex("""on\s+([A-Za-z0-9\.\-]+)""").find(res.snippet)?.groupValues?.get(1)
                onMatch ?: "Google Shopping"
            }
            else -> domain
        }

        cards.add(
            ProductCardData(
                title = cleanTitle,
                badge = badge,
                price = extractedPrice,
                description = res.snippet.take(200).trim(),
                sourceDomain = finalDomain,
                url = purchaseUrl,
                imageUrl = imgUrl
            )
        )
    }
    return cards
}

@Composable
fun ProductRecommendationCard(
    card: ProductCardData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141417),
        border = BorderStroke(1.dp, Color(0xFF26262C)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left: White rounded thumbnail holding product packshot
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                modifier = Modifier
                    .width(92.dp)
                    .height(115.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProductImageThumbnail(
                        imageUrl = card.imageUrl,
                        pageUrl = card.url,
                        title = card.title
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Product details column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White
                )

                // Green badge pill (e.g. "Best evidence-led budget pick")
                if (!card.badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F3822), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = card.badge,
                            color = Color(0xFF25D366),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Price line (muted grey)
                if (!card.price.isNullOrBlank()) {
                    Text(
                        text = card.price,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFF9E9E9E)
                    )
                }

                // Description snippet
                if (card.description.isNotBlank()) {
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 18.5.sp
                        ),
                        color = Color(0xFFEDEDED)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Source domain capsule pill
                if (card.sourceDomain.isNotBlank() || card.url.isNotBlank()) {
                    val domain = card.sourceDomain.ifBlank { extractDomain(card.url) }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF222226),
                        border = BorderStroke(0.8.dp, Color(0xFF34343A)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            DomainFaviconBadge(
                                url = card.url,
                                size = 12.dp
                            )
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color(0xFFB0B0B5)
                            )
                        }
                    }
                }

                // Action link (e.g. "Official product & details ↗")
                if (card.url.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val actionLabel = when {
                            card.sourceDomain.contains("amazon", ignoreCase = true) -> "Buy on Amazon ↗"
                            card.sourceDomain.contains("flipkart", ignoreCase = true) -> "Buy on Flipkart ↗"
                            card.sourceDomain.contains("nykaa", ignoreCase = true) -> "Buy on Nykaa ↗"
                            card.sourceDomain.contains("myntra", ignoreCase = true) -> "Buy on Myntra ↗"
                            card.sourceDomain.contains("meesho", ignoreCase = true) -> "Buy on Meesho ↗"
                            card.sourceDomain.contains("purplle", ignoreCase = true) -> "Buy on Purplle ↗"
                            card.sourceDomain.contains("reliance", ignoreCase = true) -> "Buy on Reliance Digital ↗"
                            card.sourceDomain.contains("blinkit", ignoreCase = true) -> "Buy on Blinkit ↗"
                            card.url.contains("ibp=oshop") -> "View & Buy on Google Shopping ↗"
                            card.sourceDomain.isNotBlank() -> "Buy on ${card.sourceDomain.substringBefore(".")} ↗"
                            else -> "Check price & buy ↗"
                        }
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = Color(0xFF10A37F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCardsList(
    cards: List<ProductCardData>,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.forEach { card ->
            ProductRecommendationCard(card = card)
        }
    }
}

@Composable
fun ProductImageThumbnail(
    imageUrl: String?,
    pageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    var thumbBitmap by remember(imageUrl, pageUrl) {
        mutableStateOf<Bitmap?>(
            imageUrl?.let { ProfileAvatarLoader.getCachedImage(it) }
                ?: ProfileAvatarLoader.getCached(pageUrl)
        )
    }

    LaunchedEffect(imageUrl, pageUrl) {
        if (thumbBitmap == null) {
            try {
                withContext(Dispatchers.IO) {
                    val bmp = if (!imageUrl.isNullOrBlank()) {
                        ProfileAvatarLoader.loadImage(imageUrl)
                    } else if (pageUrl.isNotBlank()) {
                        ProfileAvatarLoader.loadAvatar(pageUrl)
                    } else null
                    thumbBitmap = bmp
                }
            } catch (_: Throwable) {}
        }
    }

    val currentBmp = thumbBitmap
    if (currentBmp != null) {
        androidx.compose.foundation.Image(
            bitmap = currentBmp.asImageBitmap(),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            DomainFaviconBadge(
                url = pageUrl.ifBlank { imageUrl ?: "" },
                size = 32.dp
            )
        }
    }
}

/**
 * Data structure for rich interactive profile cards (LinkedIn, GitHub, Portfolio).
 */
data class ProfileCardData(
    val title: String,
    val subtitle: String,
    val url: String,
    val brand: String,
    val iconColor: Color,
    val badgeLetter: String
)

fun extractProfileCards(results: List<SearchResult>): List<ProfileCardData> {
    val cards = mutableListOf<ProfileCardData>()
    for (res in results) {
        val url = res.url.lowercase()
        val title = res.title
        when {
            url.contains("linkedin.com/in/") || url.contains("linkedin.com/posts/") -> {
                val isPost = url.contains("/posts/")
                val cleanName = title.substringBefore("-").substringBefore("|").substringBefore("·").trim()
                cards.add(
                    ProfileCardData(
                        title = if (cleanName.isNotBlank() && cleanName.length < 35) cleanName else "LinkedIn Profile",
                        subtitle = if (isPost) "LinkedIn Activity" else "LinkedIn Profile • in.linkedin.com",
                        url = res.url,
                        brand = "LinkedIn",
                        iconColor = Color(0xFF0A66C2),
                        badgeLetter = "in"
                    )
                )
            }
            url.contains("github.com/") && !url.contains("/issues") && !url.contains("/pull") -> {
                val cleanTitle = title.substringBefore("-").substringBefore("|").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "GitHub Profile" },
                        subtitle = "GitHub • Code & Projects",
                        url = res.url,
                        brand = "GitHub",
                        iconColor = Color(0xFF8B5CF6),
                        badgeLetter = "gh"
                    )
                )
            }
            url.contains("twitter.com/") || url.contains("x.com/") -> {
                val handle = url.substringAfter("twitter.com/").substringAfter("x.com/").substringBefore("/").substringBefore("?").trim()
                val cleanTitle = title.substringBefore("-").substringBefore("|").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "@$handle" },
                        subtitle = "X (Twitter) • Profile",
                        url = res.url,
                        brand = "X",
                        iconColor = Color(0xFF1D9BF0),
                        badgeLetter = "𝕏"
                    )
                )
            }
            url.contains("youtube.com/") || url.contains("youtu.be/") -> {
                val cleanTitle = title.substringBefore("-").substringBefore("|").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "YouTube Channel" },
                        subtitle = "YouTube • Media",
                        url = res.url,
                        brand = "YouTube",
                        iconColor = Color(0xFFFF0000),
                        badgeLetter = "▶"
                    )
                )
            }
            url.contains("huggingface.co/") -> {
                val cleanTitle = title.substringBefore("-").substringBefore("(").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "Hugging Face" },
                        subtitle = "Hugging Face • Models & Profile",
                        url = res.url,
                        brand = "Hugging Face",
                        iconColor = Color(0xFFFFD21E),
                        badgeLetter = "🤗"
                    )
                )
            }
            url.contains("wikipedia.org/wiki/") -> {
                val cleanTitle = title.substringBefore("-").substringBefore("|").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "Wikipedia" },
                        subtitle = "Wikipedia • Encyclopedia",
                        url = res.url,
                        brand = "Wikipedia",
                        iconColor = Color(0xFF636466),
                        badgeLetter = "W"
                    )
                )
            }
            url.contains("imdb.com/") -> {
                val cleanTitle = title.substringBefore("-").substringBefore("|").trim()
                cards.add(
                    ProfileCardData(
                        title = cleanTitle.ifBlank { "IMDb" },
                        subtitle = "IMDb • Movies & Filmography",
                        url = res.url,
                        brand = "IMDb",
                        iconColor = Color(0xFFF5C518),
                        badgeLetter = "★"
                    )
                )
            }
            url.contains("srmist.edu") || url.contains("srmap.edu") || title.contains("SRM University", ignoreCase = true) -> {
                cards.add(
                    ProfileCardData(
                        title = "SRM University-AP",
                        subtitle = "Academic Portal & University",
                        url = res.url,
                        brand = "SRM",
                        iconColor = Color(0xFF0F9D58),
                        badgeLetter = "S"
                    )
                )
            }
        }
    }
    return cards.distinctBy { it.brand }.take(4)
}

@Composable
fun ProfileAvatarBadge(
    url: String,
    brand: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp
) {
    var avatarBmp by remember(url) {
        mutableStateOf(ProfileAvatarLoader.getCached(url))
    }

    LaunchedEffect(url) {
        if (avatarBmp == null) {
            try {
                val bmp = ProfileAvatarLoader.loadAvatar(url)
                if (bmp != null) {
                    avatarBmp = bmp
                }
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val currentBmp = avatarBmp
        if (currentBmp != null) {
            androidx.compose.foundation.Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = brand,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            )
            // Tiny bottom-right corner brand badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF18181B))
                    .border(0.8.dp, Color(0xFF3E3E48), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                DomainFaviconBadge(
                    url = url,
                    size = 10.dp
                )
            }
        } else {
            DomainFaviconBadge(
                url = url,
                size = size
            )
        }
    }
}

@Composable
fun ProfileCardsRow(
    cards: List<ProfileCardData>,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "PROFILES & LINKS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8E8E93),
            letterSpacing = 0.5.sp
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { card ->
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFF1E1E22),
                    border = BorderStroke(1.dp, Color(0xFF323238)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatarBadge(
                            url = card.url,
                            brand = card.brand,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = card.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF9E9EA4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2C2C32),
                            border = BorderStroke(0.8.dp, Color(0xFF44444C))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Open profile",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E2E8)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Open",
                                    tint = Color(0xFF10A37F),
                                    modifier = Modifier
                                        .size(10.dp)
                                        .graphicsLayer { rotationZ = 45f }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGptSearchingWebPill() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlobe")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "globeAlpha"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1B1E),
        border = BorderStroke(1.dp, Color(0xFF2C2C30)),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chatgpt_globe),
                contentDescription = null,
                tint = Color(0xFF10A37F),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { this.alpha = alpha }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Searching the web…",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD1D1D6)
            )
        }
    }
}

@Composable
fun ChatGptSearchedSitesPill(
    searchResults: List<SearchResult>,
    onClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1A1D),
        border = BorderStroke(1.dp, Color(0xFF2B2B30)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (onClick != null) {
                    onClick()
                } else {
                    expanded = !expanded
                }
            }
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(tween(180))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular domain favicons row (overlapping like ChatGPT)
                val topFavicons = searchResults.take(3)
                if (topFavicons.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        topFavicons.forEachIndexed { index, res ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (-3 * index).dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF1A1A1D), CircleShape)
                            ) {
                                DomainFaviconBadge(url = res.url, size = 15.dp)
                            }
                        }
                    }
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chatgpt_globe),
                        contentDescription = null,
                        tint = Color(0xFF10A37F),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Searched ${searchResults.size} sites",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E2E6)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(15.dp)
                )
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFF26262A), thickness = 0.8.dp)
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (res in searchResults) {
                        val host = remember(res.url) { extractDomain(res.url) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DomainFaviconBadge(url = res.url, size = 18.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = host.ifBlank { res.title },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (res.title.isNotBlank()) {
                                    Text(
                                        text = res.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF8E8E93),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Open",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGptSourcesPill(
    searchResults: List<SearchResult>,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .liquidBounceClick(scaleDown = 0.94f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val topSources = searchResults.take(3)
            for (res in topSources) {
                DomainFaviconBadge(url = res.url)
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Sources",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD1D1D6)
            )
        }
    }
}

@Composable
fun InlineSourceCitations(
    searchResults: List<SearchResult>,
    searchImages: List<SearchImage> = emptyList()
) {
    val context = LocalContext.current
    if (searchResults.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(searchResults.take(6)) { result ->
            val domain = remember(result.url) { extractDomain(result.url).ifBlank { "Source" } }
            val cardImageUrl = remember(result.url, result.imageUrl, searchImages) {
                result.imageUrl ?: searchImages.firstOrNull { img ->
                    img.sourceUrl == result.url ||
                    (domain.isNotBlank() && extractDomain(img.sourceUrl) == domain)
                }?.imageUrl
            }
            var cardBitmap by remember(cardImageUrl) {
                mutableStateOf(cardImageUrl?.let { ProfileAvatarLoader.getCachedImage(it) })
            }
            LaunchedEffect(cardImageUrl) {
                if (cardImageUrl != null && cardBitmap == null) {
                    try {
                        withContext(Dispatchers.IO) {
                            val bmp = ProfileAvatarLoader.loadImage(cardImageUrl)
                            cardBitmap = bmp
                        }
                    } catch (_: Throwable) {}
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E22),
                border = BorderStroke(1.dp, Color(0xFF2E2E36)),
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open source", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DomainFaviconBadge(url = result.url, size = 16.dp)
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFE2E2E6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (result.title.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = result.title.trim(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color(0xFF9E9EA4),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (cardBitmap != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.Image(
                            bitmap = cardBitmap!!.asImageBitmap(),
                            contentDescription = result.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF28282E))
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ChatGPT-Style Web Search Images & Carousel
// ==========================================

@Composable
fun SearchImagesCarousel(
    images: List<SearchImage>,
    onImageClick: (SearchImage) -> Unit
) {
    if (images.isEmpty()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(images) { img ->
                SearchImageThumbnail(
                    image = img,
                    onClick = { onImageClick(img) }
                )
            }
        }
    }
}

@Composable
fun SearchImageThumbnail(
    image: SearchImage,
    onClick: () -> Unit
) {
    val effectiveUrl = remember(image.imageUrl, image.thumbnailUrl) {
        image.thumbnailUrl.ifBlank { image.imageUrl }
    }
    var thumbBitmap by remember(effectiveUrl) {
        mutableStateOf<Bitmap?>(ProfileAvatarLoader.getCachedImage(effectiveUrl))
    }
    LaunchedEffect(effectiveUrl) {
        if (thumbBitmap == null) {
            try {
                withContext(Dispatchers.IO) {
                    var bmp = ProfileAvatarLoader.loadImage(effectiveUrl)
                    if (bmp == null && image.imageUrl.isNotBlank() && effectiveUrl != image.imageUrl) {
                        bmp = ProfileAvatarLoader.loadImage(image.imageUrl)
                    }
                    thumbBitmap = bmp
                }
            } catch (_: Throwable) {}
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E22),
        border = BorderStroke(1.dp, Color(0xFF2E2E34)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(230.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .liquidBounceClick(scaleDown = 0.95f, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbBitmap!!.asImageBitmap(),
                    contentDescription = image.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF555558),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom gradient overlay with source domain & icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66000000),
                                Color(0xEE000000)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                val domain = remember(image.sourceDomain, image.sourceUrl) {
                    image.sourceDomain.ifBlank { extractDomain(image.sourceUrl) }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = domain.ifBlank { image.title },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// In-App Image Preview Dialog (ChatGPT Style)
// ==========================================

@Composable
fun InAppImagePreviewDialog(
    image: SearchImage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectivePreviewUrl = remember(image.imageUrl, image.thumbnailUrl) {
        image.imageUrl.ifBlank { image.thumbnailUrl }
    }
    var fullBitmap by remember(effectivePreviewUrl) {
        mutableStateOf<Bitmap?>(ProfileAvatarLoader.getCachedImage(effectivePreviewUrl))
    }
    var isLoading by remember(effectivePreviewUrl) { mutableStateOf(fullBitmap == null) }

    LaunchedEffect(effectivePreviewUrl) {
        if (fullBitmap == null) {
            try {
                withContext(Dispatchers.IO) {
                    var bmp = ProfileAvatarLoader.downloadBitmap(effectivePreviewUrl, maxDimension = 1280)
                    if (bmp == null && image.thumbnailUrl.isNotBlank() && effectivePreviewUrl != image.thumbnailUrl) {
                        bmp = ProfileAvatarLoader.downloadBitmap(image.thumbnailUrl, maxDimension = 1280)
                    }
                    fullBitmap = bmp
                    isLoading = false
                }
            } catch (_: Throwable) {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F11))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF212124), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val domain = remember(image.sourceUrl, image.sourceDomain) {
                    image.sourceDomain.ifBlank { extractDomain(image.sourceUrl) }
                }
                if (domain.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF212124),
                        border = BorderStroke(0.8.dp, Color(0xFF333336))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DomainFaviconBadge(url = image.sourceUrl, size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = domain,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Image Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 90.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (fullBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = fullBitmap!!.asImageBitmap(),
                        contentDescription = image.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading high-resolution image...",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        text = "Unable to load preview",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp
                    )
                }
            }

            // Bottom Caption Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF18181A))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (image.title.isNotBlank()) {
                    Text(
                        text = image.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (image.sourceUrl.isNotBlank()) {
                    Text(
                        text = image.sourceUrl,
                        color = Color(0xFF8E8E93),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// ChatGPT Sources ModalBottomSheet
// Matching media_1791441433580.jpg AMOLED overlay
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesBottomSheet(
    visible: Boolean,
    searchResults: List<SearchResult>,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF212124),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF555558),
                width = 36.dp,
                height = 4.dp
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header: "Sources" with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sources",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val primary = searchResults.firstOrNull()
                if (primary != null) {
                    item {
                        val primaryDomain = remember(primary.url) { extractDomain(primary.url).ifBlank { primary.title } }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2A2A2E),
                            border = BorderStroke(0.8.dp, Color(0xFF38383E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(primary.url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DomainFaviconBadge(url = primary.url, size = 22.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = primaryDomain,
                                        color = Color(0xFF9E9EA4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = primary.title.ifBlank { primaryDomain },
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (primary.snippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = primary.snippet,
                                        color = Color(0xFFB0B0B8),
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                val moreResults = if (searchResults.size > 1) searchResults.drop(1) else emptyList()
                if (moreResults.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            color = Color(0xFF333338),
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "More",
                            color = Color(0xFF8E8E93),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
                        )
                    }

                    items(moreResults) { res ->
                        val domain = remember(res.url) { extractDomain(res.url).ifBlank { res.title } }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242427),
                            border = BorderStroke(0.6.dp, Color(0xFF303036)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                DomainFaviconBadge(url = res.url, size = 20.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = domain,
                                        color = Color(0xFF8E8E93),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = res.title.ifBlank { domain },
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (res.snippet.isNotBlank()) {
                                        Text(
                                            text = res.snippet,
                                            color = Color(0xFF9E9EA4),
                                            fontSize = 11.5.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

private enum class ActionButtonState {
    VOICE,
    SEND,
    STOP
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChatInputDock(
    viewModel: ChatViewModel,
    isGenerating: Boolean,
    isWebSearchEnabled: Boolean,
    engineState: EngineState,
    attachedDocument: ExtractedDocument?,
    editingMessageId: String?,
    showAttachmentMenu: Boolean = false,
    onCancelEdit: () -> Unit,
    onToggleAttachmentMenu: () -> Unit,
    onLaunchVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputText by viewModel.inputText.collectAsState()
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val composerStatus = when (engineState) {
        is EngineState.Loading -> engineState.message
        is EngineState.Uninitialized -> "Model is not ready yet"
        is EngineState.Error -> "Model unavailable — retry from the error message"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Document Quick Action Chips (when document is attached)
        if (attachedDocument != null && !attachedDocument.isImage) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val docChips = listOf(
                    Pair("📑 Summarize document", "Summarize this document"),
                    Pair("📚 Extract all topics", "Extract all topics and sections"),
                    Pair("🎯 Key takeaways", "Key takeaways and findings"),
                    Pair("❓ Ask a question", "")
                )
                items(docChips) { (chipLabel, actionPrompt) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1E24),
                        border = BorderStroke(1.dp, Color(0xFF33333E)),
                        modifier = Modifier.liquidBounceClick(scaleDown = 0.95f) {
                            if (actionPrompt.isNotBlank()) {
                                viewModel.onInputTextChanged(actionPrompt)
                                viewModel.sendMessage()
                            } else {
                                keyboardController?.show()
                            }
                        }
                    ) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6D6E0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Editing message pill (ChatGPT style)
        if (editingMessageId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Editing message",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onCancelEdit()
                                    viewModel.onInputTextChanged("")
                                }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = composerStatus != null) {
            Text(composerStatus.orEmpty(), color = Color(0xFF9B9B9B),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }

        // Floating ChatGPT style Input Bar
        ChatInputBar(
            inputText = inputText,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (editingMessageId != null) {
                    val targetId = editingMessageId
                    onCancelEdit()
                    viewModel.editAndResendMessage(targetId, inputText)
                    viewModel.onInputTextChanged("")
                } else {
                    viewModel.sendMessage()
                    viewModel.onInputTextChanged("")
                }
            },
            onStop = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.stopGeneration()
            },
            isGenerating = isGenerating,
            isWebSearchEnabled = isWebSearchEnabled,
            onToggleWebSearch = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleWebSearch()
            },
            isEnabled = engineState is EngineState.Ready || engineState is EngineState.Generating,
            onVoiceInput = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLaunchVoice()
            },
            onVoiceAssistant = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLaunchVoice()
            },
            onAttach = onToggleAttachmentMenu,
            hasAttachment = attachedDocument != null,
            attachedDocument = attachedDocument,
            onRemoveAttachment = { viewModel.clearAttachedDocument() },
            showAttachmentMenu = showAttachmentMenu
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatInputBar(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    isWebSearchEnabled: Boolean,
    onToggleWebSearch: () -> Unit,
    isEnabled: Boolean,
    onVoiceInput: () -> Unit = {},
    onVoiceAssistant: () -> Unit = {},
    onAttach: () -> Unit = {},
    hasAttachment: Boolean = false,
    attachedDocument: ExtractedDocument? = null,
    onRemoveAttachment: () -> Unit = {},
    showAttachmentMenu: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val density = LocalDensity.current
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = WindowInsets.isImeVisible || imeBottomDp > 0.dp

    var isFocused by remember { mutableStateOf(false) }
    var isMaximized by remember { mutableStateOf(false) }

    var wasKeyboardOpen by remember { mutableStateOf(false) }

    // When the soft keyboard closes, clear focus immediately so the bar shrinks back to unclicked 80% state
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen) {
            wasKeyboardOpen = true
        } else if (wasKeyboardOpen) {
            focusManager.clearFocus()
            isFocused = false
            wasKeyboardOpen = false
        }
    }

    LaunchedEffect(inputText.isEmpty()) {
        if (inputText.isEmpty()) {
            isMaximized = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalWidthPx = constraints.maxWidth

        // Keep the composer stable as the keyboard opens; animate height and controls only.
        val animatedWidthFraction = 1f

        // Threshold width for single-line text field:
        val singleLineTextMaxWidthPx = remember(totalWidthPx, density, isWebSearchEnabled, animatedWidthFraction) {
            with(density) {
                val currentWidthPx = (totalWidthPx * animatedWidthFraction).toInt()
                val reservedDp = 24.dp + 48.dp + 48.dp + 8.dp + (if (isWebSearchEnabled) 75.dp else 0.dp)
                val calculated = currentWidthPx - reservedDp.roundToPx()
                calculated.coerceAtLeast(140.dp.roundToPx())
            }
        }
        val hysteresisPx = remember(density) {
            with(density) { 14.dp.roundToPx() }
        }

        val textMeasurer = rememberTextMeasurer()
        val textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.White,
            lineHeight = 22.sp,
            fontFamily = FontFamily.Default
        )

        var isMultiLineState by remember { mutableStateOf(false) }

        val isMultiLine = remember(inputText, attachedDocument, isMaximized, isWebSearchEnabled, singleLineTextMaxWidthPx) {
            if (isMaximized || isWebSearchEnabled || attachedDocument != null || inputText.contains('\n')) {
                isMultiLineState = true
                true
            } else if (inputText.isEmpty()) {
                isMultiLineState = false
                false
            } else {
                val measuredWidth = textMeasurer.measure(
                    text = AnnotatedString(inputText),
                    style = textStyle
                ).size.width

                if (measuredWidth > singleLineTextMaxWidthPx) {
                    isMultiLineState = true
                    true
                } else if (measuredWidth < (singleLineTextMaxWidthPx - hysteresisPx)) {
                    isMultiLineState = false
                    false
                } else {
                    isMultiLineState
                }
            }
        }

        // Dynamic Action State: VOICE (when idle/empty), SEND (when text/file present), STOP (when generating)
        val actionState = when {
            isGenerating -> ActionButtonState.STOP
            inputText.isNotBlank() || hasAttachment -> ActionButtonState.SEND
            else -> ActionButtonState.VOICE
        }

        // ChatGPT smooth morph: Circular capsule (26.dp) when single-line, rounded card (20.dp) when multi-line (ect0.a = 22.dp)
        val cornerRadius by animateDpAsState(
            targetValue = if (isMultiLine) 20.dp else 26.dp,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "inputCornerRadius"
        )

        val plusRotation by animateFloatAsState(
            targetValue = if (showAttachmentMenu) 45f else 0f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "plusRotation"
        )

        // Solid ChatGPT Dark Bottom Input Bar Container (ytc.C: #FF212121 surface, #FF323232 border)
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = LocalChatTheme.current.surface,
            border = BorderStroke(1.dp, LocalChatTheme.current.border),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth(animatedWidthFraction)
                .padding(vertical = 2.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
        ChatInputBarLayout(
            isMultiLine = isMultiLine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = if (isMultiLine) 6.dp else 4.dp),
            attachmentContent = if (attachedDocument != null) {
                {
                    val doc = attachedDocument
                    if (doc.isImage && doc.previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp, top = 2.dp, bottom = 4.dp)
                                .size(64.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.graphics.painter.BitmapPainter(doc.previewBitmap.asImageBitmap()),
                                contentDescription = "Attached image",
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.BottomStart)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2A2A2A)),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color(0xDD000000),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .clickable { onRemoveAttachment() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isPdf = doc.fileName.endsWith(".pdf", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x662A2A38),
                            border = BorderStroke(0.8.dp, Color(0x2EFFFFFF)),
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isPdf) Color(0xFFFF857D) else Color(0xFF66D9B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 180.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF383840))
                                        .clickable { onRemoveAttachment() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else null,
            webBadgeContent = if (isWebSearchEnabled) {
                {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x2810A37F),
                        border = BorderStroke(1.dp, Color(0x5510A37F)),
                        modifier = Modifier
                            .padding(start = 2.dp, bottom = if (isMultiLine) 4.dp else 0.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .liquidBounceClick(scaleDown = 0.94f) { onToggleWebSearch() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chatgpt_globe),
                                contentDescription = "Search the web",
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFF10A37F)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMultiLine) "Web Search" else "Web",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10A37F)
                            )
                        }
                    }
                }
            } else null,
            textFieldContent = {
                // Unconditionally mounted BasicTextField that NEVER unmounts, preserving focus & cursor 100%
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = if (isMultiLine) 42.dp else 24.dp,
                            max = if (isMaximized) 360.dp else if (isMultiLine) 150.dp else 38.dp
                        )
                        .padding(horizontal = 4.dp, vertical = if (isMultiLine) 6.dp else 2.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onTextChanged,
                        enabled = true,
                        minLines = if (isMultiLine) 2 else 1,
                        maxLines = if (isMaximized) 16 else if (isMultiLine) 6 else 1,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrect = true,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Default
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) {
                                    keyboardController?.show()
                                }
                            },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = when {
                                            isWebSearchEnabled -> "Search the web…"
                                            hasAttachment -> "Ask about this..."
                                            else -> "Ask Gemma…"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 16.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            },
            plusButtonContent = {
                // Bare ChatGPT + icon button (rotates 45° to an 'x' on open)
                ChatGptSurfaceButton(
                    onClick = onAttach,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add attachment",
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = plusRotation },
                        tint = Color(0xFFCDCDCD)
                    )
                }
            },
            singleLineActionsContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dictation mic button (bare icon, visible in VOICE state, smooth exit when typing/generating)
                    AnimatedVisibility(
                        visible = actionState == ActionButtonState.VOICE,
                        enter = fadeIn(animationSpec = tween(160)) + scaleIn(
                            initialScale = 0.6f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                        exit = fadeOut(animationSpec = tween(120)) + scaleOut(
                            targetScale = 0.6f,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + shrinkHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    ) {
                        ChatGptSurfaceButton(
                            onClick = onVoiceInput,
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice dictation",
                                tint = Color(0xFFCDCDCD),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Dynamic Morphing Action Button (Voice Assistant <-> Send <-> Stop) with ChatGPT signature Coral Pink (#FA70AB)
                    AnimatedContent(
                        targetState = actionState,
                        transitionSpec = {
                            (scaleIn(
                                initialScale = 0.5f,
                                animationSpec = spring(
                                    dampingRatio = 0.70f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(
                                animationSpec = tween(160)
                            )).togetherWith(
                                scaleOut(
                                    targetScale = 0.5f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(120)
                                )
                            )
                        },
                        label = "singleLineActionButtonMorph"
                    ) { state ->
                        when (state) {
                            ActionButtonState.STOP -> {
                                // Coral Pink circle with pure white stop square
                                ChatGptSurfaceButton(
                                    onClick = onStop,
                                    shape = CircleShape,
                                    color = Color(0xFFFA70AB),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chatgpt_stop),
                                        contentDescription = "Stop generating",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            ActionButtonState.SEND -> {
                                val canSend = isEnabled && !isGenerating && (inputText.isNotBlank() || hasAttachment)
                                // Coral Pink circle (#FA70AB) with pure white up-arrow
                                ChatGptSurfaceButton(
                                    onClick = onSend,
                                    enabled = canSend,
                                    shape = CircleShape,
                                    color = if (canSend) Color(0xFFFA70AB) else Color(0xFF383838),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chatgpt_send),
                                        contentDescription = "Send",
                                        tint = if (canSend) Color.White else Color(0xFF757575),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            ActionButtonState.VOICE -> {
                                // ChatGPT signature Voice Mode assistant button (Coral Pink circle #FA70AB with white waveform)
                                ChatGptSurfaceButton(
                                    onClick = onVoiceAssistant,
                                    shape = CircleShape,
                                    color = Color(0xFFFA70AB),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chatgpt_voice),
                                        contentDescription = "Voice assistant",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            multiLineActionsContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Expand/Collapse icon button
                    ChatGptSurfaceButton(
                        onClick = { isMaximized = !isMaximized },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMaximized) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = if (isMaximized) "Collapse input" else "Expand input",
                            tint = Color(0xFFB0B0B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Dictation Mic button (bare icon)
                    if (!isGenerating) {
                        ChatGptSurfaceButton(
                            onClick = onVoiceInput,
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = Color(0xFFCDCDCD),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = if (isGenerating) ActionButtonState.STOP else ActionButtonState.SEND,
                        transitionSpec = {
                            (scaleIn(
                                initialScale = 0.5f,
                                animationSpec = spring(
                                    dampingRatio = 0.70f,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeIn(
                                animationSpec = tween(160)
                            )).togetherWith(
                                scaleOut(
                                    targetScale = 0.5f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(120)
                                )
                            )
                        },
                        label = "multiLineActionButtonMorph"
                    ) { state ->
                        if (state == ActionButtonState.STOP) {
                            ChatGptSurfaceButton(
                                onClick = onStop,
                                shape = CircleShape,
                                color = Color(0xFFFA70AB),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_stop),
                                    contentDescription = "Stop",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else {
                            val canSend = isEnabled && (inputText.isNotBlank() || hasAttachment)
                            ChatGptSurfaceButton(
                                onClick = onSend,
                                enabled = canSend,
                                shape = CircleShape,
                                color = if (canSend) Color(0xFFFA70AB) else Color(0xFF383838),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chatgpt_send),
                                    contentDescription = "Send",
                                    tint = if (canSend) Color.White else Color(0xFF757575),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
}


@Composable
private fun ChatInputBarLayout(
    isMultiLine: Boolean,
    modifier: Modifier = Modifier,
    attachmentContent: (@Composable () -> Unit)? = null,
    webBadgeContent: (@Composable () -> Unit)? = null,
    textFieldContent: @Composable () -> Unit,
    plusButtonContent: @Composable () -> Unit,
    singleLineActionsContent: @Composable () -> Unit,
    multiLineActionsContent: @Composable () -> Unit
) {
    Layout(
        content = {
            if (attachmentContent != null) {
                Box(Modifier.layoutId("attachment")) {
                    attachmentContent()
                }
            }
            if (webBadgeContent != null) {
                Box(Modifier.layoutId("webBadge")) {
                    webBadgeContent()
                }
            }
            Box(Modifier.layoutId("textField")) {
                textFieldContent()
            }
            Box(Modifier.layoutId("plus")) {
                plusButtonContent()
            }
            if (isMultiLine) {
                Box(Modifier.layoutId("multiActions")) {
                    multiLineActionsContent()
                }
            } else {
                Box(Modifier.layoutId("singleActions")) {
                    singleLineActionsContent()
                }
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val attachmentMeasurable = measurables.firstOrNull { it.layoutId == "attachment" }
        val webBadgeMeasurable = measurables.firstOrNull { it.layoutId == "webBadge" }
        val textFieldMeasurable = measurables.first { it.layoutId == "textField" }
        val plusMeasurable = measurables.first { it.layoutId == "plus" }

        val horizontalPadding = 4.dp.roundToPx()
        val verticalPadding = 2.dp.roundToPx()
        val innerSpacing = 4.dp.roundToPx()
        val availableWidth = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0)

        if (isMultiLine) {
            val multiActionsMeasurable = measurables.first { it.layoutId == "multiActions" }

            val attachmentPlaceable = attachmentMeasurable?.measure(
                constraints.copy(minWidth = 0, maxWidth = availableWidth, minHeight = 0)
            )
            val webBadgePlaceable = webBadgeMeasurable?.measure(
                constraints.copy(minWidth = 0, maxWidth = availableWidth, minHeight = 0)
            )
            val plusPlaceable = plusMeasurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )
            val actionsPlaceable = multiActionsMeasurable.measure(
                constraints.copy(minWidth = 0, maxWidth = availableWidth, minHeight = 0)
            )
            val textFieldPlaceable = textFieldMeasurable.measure(
                constraints.copy(minWidth = availableWidth, maxWidth = availableWidth, minHeight = 0)
            )

            val toolbarHeight = maxOf(plusPlaceable.height, actionsPlaceable.height)
            val totalHeight = verticalPadding * 2 +
                    (attachmentPlaceable?.height?.plus(innerSpacing) ?: 0) +
                    (webBadgePlaceable?.height?.plus(innerSpacing) ?: 0) +
                    textFieldPlaceable.height +
                    innerSpacing +
                    toolbarHeight

            layout(constraints.maxWidth, totalHeight) {
                var currentY = verticalPadding

                attachmentPlaceable?.let {
                    it.placeRelative(horizontalPadding, currentY)
                    currentY += it.height + innerSpacing
                }

                webBadgePlaceable?.let {
                    it.placeRelative(horizontalPadding, currentY)
                    currentY += it.height + innerSpacing
                }

                textFieldPlaceable.placeRelative(horizontalPadding, currentY)
                currentY += textFieldPlaceable.height + innerSpacing

                val plusY = currentY + (toolbarHeight - plusPlaceable.height) / 2
                plusPlaceable.placeRelative(horizontalPadding, plusY)

                val actionsX = constraints.maxWidth - horizontalPadding - actionsPlaceable.width
                val actionsY = currentY + (toolbarHeight - actionsPlaceable.height) / 2
                actionsPlaceable.placeRelative(actionsX, actionsY)
            }
        } else {
            val singleActionsMeasurable = measurables.first { it.layoutId == "singleActions" }

            val plusPlaceable = plusMeasurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )
            val actionsPlaceable = singleActionsMeasurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )
            val webBadgePlaceable = webBadgeMeasurable?.measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )

            val badgeWidthWithSpacing = if (webBadgePlaceable != null) webBadgePlaceable.width + innerSpacing else 0
            val remainingWidthForText = (availableWidth - plusPlaceable.width - actionsPlaceable.width - innerSpacing * 2 - badgeWidthWithSpacing).coerceAtLeast(0)

            val textFieldPlaceable = textFieldMeasurable.measure(
                constraints.copy(minWidth = remainingWidthForText, maxWidth = remainingWidthForText, minHeight = 0)
            )

            val contentHeight = maxOf(
                plusPlaceable.height,
                actionsPlaceable.height,
                textFieldPlaceable.height,
                webBadgePlaceable?.height ?: 0
            )
            val totalHeight = contentHeight + verticalPadding * 2

            layout(constraints.maxWidth, totalHeight) {
                var currentX = horizontalPadding

                val plusY = (totalHeight - plusPlaceable.height) / 2
                plusPlaceable.placeRelative(currentX, plusY)
                currentX += plusPlaceable.width + innerSpacing

                webBadgePlaceable?.let {
                    val badgeY = (totalHeight - it.height) / 2
                    it.placeRelative(currentX, badgeY)
                    currentX += it.width + innerSpacing
                }

                val textY = (totalHeight - textFieldPlaceable.height) / 2
                textFieldPlaceable.placeRelative(currentX, textY)

                val actionsX = constraints.maxWidth - horizontalPadding - actionsPlaceable.width
                val actionsY = (totalHeight - actionsPlaceable.height) / 2
                actionsPlaceable.placeRelative(actionsX, actionsY)
            }
        }
    }
}

@Composable
fun FloatingMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .liquidBounceClick(scaleDown = 0.97f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun FloatingMenuItemWithToggle(
    icon: ImageVector,
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .liquidBounceClick(scaleDown = 0.98f, onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) Color(0xFF10A37F) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isChecked) Color(0xFF10A37F) else Color.White
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10A37F),
                    uncheckedThumbColor = Color(0xFF8E8E93),
                    uncheckedTrackColor = Color(0xFF383838)
                ),
                modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
            )
        }
    }
}

@Composable
fun AttachmentDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
    isWebSearchEnabled: Boolean = false,
    onToggleWebSearch: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF212121),
        title = { Text("Add to chat", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AttachOption(emoji = "📷", label = "Camera", desc = "Take a photo for Gemma to examine") {
                    onTakePhoto(); onDismiss()
                }
                AttachOption(emoji = "🖼️", label = "Photos", desc = "Choose an image from gallery") {
                    onPickImage(); onDismiss()
                }
                AttachOption(emoji = "📄", label = "Document", desc = "Attach PDF or text document") {
                    onPickDocument(); onDismiss()
                }
                AttachOption(
                    emoji = "🌐",
                    label = if (isWebSearchEnabled) "Web Search (Active)" else "Web Search",
                    desc = if (isWebSearchEnabled) "Web search is currently ON — tap to disable" else "Search live web for real-time information"
                ) {
                    onToggleWebSearch(); onDismiss()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

/**
 * Combines a small selection of text/PDF files into one token-bounded chat
 * attachment. Vision models accept one image at a time, so a mixed selection
 * retains the first image and combines document text when there is no image.
 */
private fun combineDocumentsForChat(documents: List<ExtractedDocument>): ExtractedDocument {
    require(documents.isNotEmpty())
    if (documents.size == 1) return documents.single()

    documents.firstOrNull { it.isImage }?.let { return it }

    val combinedText = documents.joinToString(separator = "\n\n") { document ->
        "--- ${document.fileName} ---\n${document.text}"
    }
    val names = documents.joinToString(", ") { it.fileName }.take(120)
    return ExtractedDocument(
        fileName = "$names (${documents.size} files)",
        text = combinedText,
        wordCount = documents.sumOf { it.wordCount },
        pageCount = documents.sumOf { it.pageCount }
    )
}

@Composable
private fun AttachOption(
    emoji: String,
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2E2E2E),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E93))
            }
        }
    }
}


@Composable
fun ModelManagementSection(
    installState: ModelInstallState,
    onImportClick: () -> Unit,
    onRetryCheck: () -> Unit,
    viewModel: ChatViewModel
) {
    val hfToken by viewModel.hfToken.collectAsState()
    var tokenInput by remember(hfToken) { mutableStateOf(hfToken) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gemma 4 E2B IT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "100% Local On-Device AI • 2.58 GB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val availableBytes = viewModel.modelManager.getAvailableStorageBytes()
                if (availableBytes > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Available Storage: ${viewModel.modelManager.formatSize(availableBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (installState) {
                    is ModelInstallState.Installing -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { installState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val pct = (installState.progress * 100).toInt()
                            Text(
                                text = "Downloading to App Storage... $pct%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${viewModel.modelManager.formatSize(installState.transferredBytes)} / ${viewModel.modelManager.formatSize(installState.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.cancelDownload() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel Download")
                            }
                        }
                    }

                    is ModelInstallState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = installState.message, style = MaterialTheme.typography.bodySmall)
                    }

                    else -> {
                        // Direct In-App Download UI
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = {
                                tokenInput = it
                                viewModel.onHfTokenChanged(it)
                            },
                            label = { Text("Hugging Face Access Token") },
                            placeholder = { Text("hf_...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Token saved locally in app for authorized direct download",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.onHfTokenChanged(tokenInput)
                                viewModel.downloadModel()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ Download Model Directly in App",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = onImportClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Import Model from File Picker (SAF)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRetryCheck,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Check Model Status")
                }
            }
        }
    }
}

@Composable
fun ModelReadySection(
    installState: ModelInstallState.Installed,
    onStartChat: () -> Unit,
    onDeleteModel: () -> Unit,
    onOpenConfig: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gemma 4 E2B",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✓ Gemma 4 E2B Installed in App",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Location: Internal Storage (${installState.modelFile.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Start Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenConfig,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⚙️ Adjust Parameters (temp, tokens, top-p, etc.)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDeleteModel
                ) {
                    Text(
                        text = "Delete & Re-download",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Intelligent ChatGPT-style follow-up suggestion extractor.
 * Detects cut-off responses, parses Gemma's proactive questions/options,
 * and adds context-aware prompt chips.
 */
fun extractFollowUpSuggestions(text: String): List<String> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return emptyList()

    val suggestions = mutableListOf<String>()

    val seemsCutOff = !trimmed.endsWith(".") && !trimmed.endsWith("?") &&
            !trimmed.endsWith("!") && !trimmed.endsWith("```") &&
            !trimmed.endsWith("\"") && !trimmed.endsWith(")") &&
            !trimmed.endsWith(":") && trimmed.length > 50

    if (seemsCutOff) {
        suggestions.add("Continue generating")
    }

    // Try extracting from explicit closing questions:
    // e.g. "Would you like to explore Supervised Learning next, or see a hands-on Python example?"
    val lastQuestionMatch = Regex(
        """(?:Would you like to|Do you want to|Should we)\s+([^?\n]+)\?""",
        RegexOption.IGNORE_CASE
    ).findAll(trimmed).lastOrNull()

    if (lastQuestionMatch != null) {
        val questionBody = lastQuestionMatch.groupValues[1].trim()
        if (questionBody.contains(" or ", ignoreCase = true)) {
            val parts = questionBody.split(Regex("""\s+or\s+""", RegexOption.IGNORE_CASE))
            for (p in parts) {
                val cleaned = p.replace(Regex("""^(explore|learn about|see|check out)\s+""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s+next$""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""[*_`"]"""), "")
                    .trim()
                if (cleaned.isNotBlank() && cleaned.length in 3..40) {
                    suggestions.add(cleaned.replaceFirstChar { it.uppercase() })
                }
            }
        } else {
            val cleaned = questionBody.replace(Regex("""[*_`"]"""), "").trim()
            if (cleaned.isNotBlank() && cleaned.length in 4..45) {
                suggestions.add(cleaned.replaceFirstChar { it.uppercase() })
            }
        }
    }

    // Context-aware defaults based on topic if fewer than 3 suggestions
    val lower = trimmed.lowercase()
    if (lower.contains("machine learning") || lower.contains("deep learning") || lower.contains("supervised learning")) {
        if (!suggestions.any { it.contains("supervised", ignoreCase = true) }) {
            suggestions.add("Explain Supervised Learning")
        }
        if (!suggestions.any { it.contains("example", ignoreCase = true) }) {
            suggestions.add("Practical Python example")
        }
    } else if (lower.contains("python") || lower.contains("kotlin") || lower.contains("java") || lower.contains("```")) {
        if (!suggestions.any { it.contains("code", ignoreCase = true) }) {
            suggestions.add("Explain how this code works")
        }
        if (!suggestions.any { it.contains("error", ignoreCase = true) }) {
            suggestions.add("Add error handling & tests")
        }
    }

    // Standard high-value ChatGPT-style follow-up actions
    val fallbacks = listOf(
        "Explain in simpler terms",
        "Give real-world examples",
        "Summarize key takeaways"
    )
    for (f in fallbacks) {
        if (suggestions.size >= 4) break
        if (!suggestions.any { it.equals(f, ignoreCase = true) }) {
            suggestions.add(f)
        }
    }

    return suggestions.distinct().take(4)
}

@Composable
private fun ModelInfoSheetContent(
    engineState: com.teja.gemmmobile.ai.EngineState,
    isWebSearchEnabled: Boolean,
    onToggleWebSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ChatGPT Local AI",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = Color.White,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Running on-device with zero server tracking",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8E8E93),
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF242428),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Engine Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (engineState) {
                                        is com.teja.gemmmobile.ai.EngineState.Ready -> Color(0xFF10A37F)
                                        is com.teja.gemmmobile.ai.EngineState.Generating -> Color(0xFFE5A83B)
                                        is com.teja.gemmmobile.ai.EngineState.Loading -> Color(0xFF388BFD)
                                        else -> Color(0xFF8E8E93)
                                    }
                                )
                        )
                        Text(
                            text = "Model Engine",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFE0E0E6),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        )
                    }

                    Text(
                        text = when (engineState) {
                            is com.teja.gemmmobile.ai.EngineState.Ready -> "Ready (${engineState.backend.name})"
                            is com.teja.gemmmobile.ai.EngineState.Generating -> "Active (${engineState.backend.name})"
                            is com.teja.gemmmobile.ai.EngineState.Loading -> "Loading..."
                            else -> "Idle"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9E9EAA),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF323238), thickness = 0.5.dp)

                // Web Search Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Search the Web",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Fetch live citations & real-time answers",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8E8E93),
                                fontSize = 12.sp
                            )
                        )
                    }
                    Switch(
                        checked = isWebSearchEnabled,
                        onCheckedChange = { onToggleWebSearch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10A37F),
                            uncheckedThumbColor = Color(0xFFAAAAAA),
                            uncheckedTrackColor = Color(0xFF3A3A3E)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF242428),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Model & App Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    )
                }
                Text(
                    text = "›",
                    color = Color(0xFF6E6E73),
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                )
            }
        }
    }
}

