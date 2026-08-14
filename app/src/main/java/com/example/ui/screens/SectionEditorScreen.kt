package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmos.CmosFormatter
import com.example.model.EditorialCommentEntity
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ParchmentCream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionEditorScreen(
    section: SectionEntity,
    comments: List<EditorialCommentEntity>,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onSaveContent: (SectionEntity, String) -> Unit,
    onSaveAiPrompt: (SectionEntity, String) -> Unit,
    onSaveTitle: (SectionEntity, String, String) -> Unit,
    onSaveIllustrations: (SectionEntity, String, String, String, String) -> Unit,
    onUpdateStatus: (SectionEntity, SectionStatus) -> Unit,
    onAddComment: (sectionId: Long, manuscriptId: Long, text: String, cmosRef: String) -> Unit,
    onResolveComment: (commentId: Long, resolved: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var title by remember(section.id) { mutableStateOf(section.title) }
    var subtitle by remember(section.id) { mutableStateOf(section.subtitle) }
    var contentValue by remember(section.id) { mutableStateOf(TextFieldValue(section.content)) }

    var headerIllustrationUri by remember(section.id) { mutableStateOf(section.headerIllustrationUri) }
    var headerIllustrationCaption by remember(section.id) { mutableStateOf(section.headerIllustrationCaption) }
    var tailIllustrationUri by remember(section.id) { mutableStateOf(section.tailIllustrationUri) }
    var tailIllustrationCaption by remember(section.id) { mutableStateOf(section.tailIllustrationCaption) }

    var aiDraftPrompt by remember(section.id) {
        mutableStateOf(
            if (section.aiDraftPrompt.isNotBlank()) section.aiDraftPrompt
            else "Write or refine a chapter section titled '${section.title}' adhering strictly to The Chicago Manual of Style (17th Edition). Include narrative pacing, rich sensory detail, dialogue with proper smart quotes, em-dashes for parenthetical breaks, and no missing serial commas."
        )
    }
    var currentStatus by remember(section.id) { mutableStateOf(section.status) }

    // 0 = Prose, 1 = Illustrations & Art, 2 = AI Workshop
    var selectedEditorTab by remember { mutableIntStateOf(0) }
    var isTitleExpanded by remember { mutableStateOf(false) }

    var showCommentsSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val newCommentCmosRef by remember { mutableStateOf("CMOS 17th Ed.") }
    var showStatusMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Visual transformation for markdown *italic* and **bold**
    val markdownVisualTransformation = remember { CmosFormatter.createMarkdownVisualTransformation() }

    // Image Pickers
    val headImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            headerIllustrationUri = uri.toString()
            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
            coroutineScope.launch { snackbarHostState.showSnackbar("Chapter Head Illustration attached.") }
        }
    }

    val tailImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            tailIllustrationUri = uri.toString()
            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
            coroutineScope.launch { snackbarHostState.showSnackbar("Chapter Tailpiece Ornament attached.") }
        }
    }

    val wordCount = remember(contentValue.text) {
        contentValue.text.split(Regex("""\s+""")).count { it.isNotBlank() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = section.sectionType.defaultTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = BookGoldDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = title.ifBlank { "Untitled Section" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onSaveContent(section, contentValue.text)
                            onSaveTitle(section, title, subtitle)
                            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
                            onBack()
                        },
                        modifier = Modifier.testTag("btn_back_editor")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Status Badge with Dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (currentStatus) {
                                SectionStatus.DRAFT -> Color(0xFFFFF3E0)
                                SectionStatus.UNDER_REVIEW -> Color(0xFFE3F2FD)
                                SectionStatus.POLISHED -> Color(0xFFE8F5E9)
                                SectionStatus.FINAL -> Color(0xFFEDE7F6)
                            },
                            modifier = Modifier
                                .clickable { showStatusMenu = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("btn_status_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentStatus.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentStatus) {
                                        SectionStatus.DRAFT -> Color(0xFFE65100)
                                        SectionStatus.UNDER_REVIEW -> Color(0xFF1565C0)
                                        SectionStatus.POLISHED -> Color(0xFF2E7D32)
                                        SectionStatus.FINAL -> Color(0xFF6A1B9A)
                                    }
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            SectionStatus.values().forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.displayName) },
                                    onClick = {
                                        currentStatus = st
                                        onUpdateStatus(section, st)
                                        showStatusMenu = false
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Status changed to ${st.displayName}") }
                                    },
                                    trailingIcon = {
                                        if (currentStatus == st) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BookGoldDark)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Comments Button
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.testTag("btn_editor_comments")
                    ) {
                        Box {
                            Icon(imageVector = Icons.Default.Comment, contentDescription = "Comments", tint = BookGoldDark)
                            if (comments.any { !it.isResolved }) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Quick Save Button
                    IconButton(
                        onClick = {
                            onSaveContent(section, contentValue.text)
                            onSaveTitle(section, title, subtitle)
                            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
                            coroutineScope.launch { snackbarHostState.showSnackbar("All changes saved.") }
                        },
                        modifier = Modifier.testTag("btn_save_section")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Compact, Expandable Section Title & Details Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title.ifBlank { "Untitled" },
                            style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isTitleExpanded = !isTitleExpanded },
                            maxLines = 1
                        )
                        IconButton(
                            onClick = { isTitleExpanded = !isTitleExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isTitleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                                contentDescription = "Edit Title",
                                tint = BookGoldDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isTitleExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                onSaveTitle(section, title, subtitle)
                            },
                            label = { Text("Title (Headline Style)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_section_title"),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldDark,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = {
                                subtitle = it
                                onSaveTitle(section, title, subtitle)
                            },
                            label = { Text("Subtitle / Epigraph (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_section_subtitle"),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldDark,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Compact Navigation Tabs (32dp height)
            TabRow(
                selectedTabIndex = selectedEditorTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BookGoldDark,
                modifier = Modifier.height(38.dp)
            ) {
                Tab(
                    selected = selectedEditorTab == 0,
                    onClick = { selectedEditorTab = 0 },
                    text = { Text("Prose Canvas", fontSize = 11.5.sp, fontWeight = if (selectedEditorTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_manuscript_prose")
                )
                Tab(
                    selected = selectedEditorTab == 1,
                    onClick = { selectedEditorTab = 1 },
                    text = { Text("Illustrations", fontSize = 11.5.sp, fontWeight = if (selectedEditorTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_chapter_art")
                )
                Tab(
                    selected = selectedEditorTab == 2,
                    onClick = { selectedEditorTab = 2 },
                    text = { Text("AI Workshop", fontSize = 11.5.sp, fontWeight = if (selectedEditorTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_ai_prompt_workshop")
                )
            }

            when (selectedEditorTab) {
                0 -> {
                    // ==========================================
                    // 1. MANUSCRIPT PROSE CANVAS
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFEFECE6))
                    ) {
                        // Resizing, scrollable paper canvas that stays fully visible
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(2.dp, RoundedCornerShape(4.dp)),
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(containerColor = ParchmentCream),
                                border = BorderStroke(1.dp, Color(0xFFDCD4C7))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    // Status row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$wordCount words • CMOS 17th Preserved",
                                            fontSize = 10.5.sp,
                                            color = Color.Gray,
                                            fontFamily = FontFamily.Serif
                                        )
                                        Text(
                                            text = "*italic*  **bold**",
                                            fontSize = 10.5.sp,
                                            color = BookGoldDark,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEAE2D8))

                                    BasicTextField(
                                        value = contentValue,
                                        onValueChange = {
                                            contentValue = it
                                            onSaveContent(section, it.text)
                                        },
                                        visualTransformation = markdownVisualTransformation,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .testTag("input_section_content"),
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 14.5.sp,
                                            lineHeight = 22.sp,
                                            color = InkBlack
                                        ),
                                        cursorBrush = SolidColor(BookGoldDark)
                                    )
                                }
                            }
                        }

                        // Ultra-Compact Floating Formatting Toolbar with imePadding
                        // Sits directly above the soft keyboard on mobile devices
                        CompactCmosToolbar(
                            onApplyItalics = {
                                contentValue = CmosFormatter.applyItalics(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplyBold = {
                                contentValue = CmosFormatter.applyBold(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplySmartQuotes = {
                                contentValue = CmosFormatter.applySmartQuotesToSelection(contentValue)
                                onSaveContent(section, contentValue.text)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Chicago smart quotes applied.") }
                            },
                            onApplyEmDash = {
                                contentValue = CmosFormatter.applyEmDashAtCursor(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplyEnDash = {
                                contentValue = CmosFormatter.applyEnDashAtCursor(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplyIndent = {
                                contentValue = CmosFormatter.applyIndentAtCursor(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplyFootnote = {
                                contentValue = CmosFormatter.applyFootnoteAtCursor(contentValue)
                                onSaveContent(section, contentValue.text)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Footnote reference inserted.") }
                            },
                            onApplyBlockQuote = {
                                contentValue = CmosFormatter.applyBlockQuote(contentValue)
                                onSaveContent(section, contentValue.text)
                            },
                            onApplyHeadlineCase = {
                                title = CmosFormatter.toChicagoHeadlineCase(title)
                                onSaveTitle(section, title, subtitle)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Applied Chicago Title Case.") }
                            },
                            onFullPolish = {
                                val polished = CmosFormatter.polishText(contentValue.text)
                                contentValue = TextFieldValue(polished)
                                title = CmosFormatter.toChicagoHeadlineCase(title)
                                onSaveContent(section, polished)
                                onSaveTitle(section, title, subtitle)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Full CMOS Polish applied!") }
                            },
                            onCopy = {
                                val clip = ClipData.newPlainText("Section Text", contentValue.text)
                                clipboardManager.setPrimaryClip(clip)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Copied section text.") }
                            },
                            onPaste = {
                                val item = clipboardManager.primaryClip?.getItemAt(0)
                                if (item != null) {
                                    val pasteText = item.text?.toString() ?: ""
                                    val min = contentValue.selection.min
                                    val max = contentValue.selection.max
                                    val newText = contentValue.text.substring(0, min) + pasteText + contentValue.text.substring(max)
                                    contentValue = TextFieldValue(newText, androidx.compose.ui.text.TextRange(min + pasteText.length))
                                    onSaveContent(section, contentValue.text)
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // ==========================================
                    // 2. ILLUSTRATIONS & CHAPTER ART WORKSHOP
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFF9F7F4))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Chapter Illustrations & Ornaments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Embed woodcuts, engravings, or custom photos at chapter head or tail.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // CHAPTER HEAD ILLUSTRATION CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chapter Headpiece (Beginning)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (headerIllustrationUri.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                headerIllustrationUri = ""
                                                headerIllustrationCaption = ""
                                                onSaveIllustrations(section, "", "", tailIllustrationUri, tailIllustrationCaption)
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Headpiece", tint = Color.Red)
                                        }
                                    }
                                }

                                if (headerIllustrationUri.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IllustrationPreview(
                                        uriString = headerIllustrationUri,
                                        caption = headerIllustrationCaption,
                                        maxHeight = 120
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = headerIllustrationCaption,
                                        onValueChange = {
                                            headerIllustrationCaption = it
                                            onSaveIllustrations(section, headerIllustrationUri, it, tailIllustrationUri, tailIllustrationCaption)
                                        },
                                        label = { Text("Headpiece Caption / Citation (Italic)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                } else {
                                    Text(
                                        text = "No headpiece illustration selected.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { headImagePicker.launch("image/*") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick Photo", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            headerIllustrationUri = "drawable:head_engraving"
                                            headerIllustrationCaption = "The Dearborn Foundry at Dusk"
                                            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preset Woodcut", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CHAPTER TAILPIECE / END ORNAMENT CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chapter Tailpiece (End of Chapter)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (tailIllustrationUri.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                tailIllustrationUri = ""
                                                tailIllustrationCaption = ""
                                                onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, "", "")
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Tailpiece", tint = Color.Red)
                                        }
                                    }
                                }

                                if (tailIllustrationUri.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IllustrationPreview(
                                        uriString = tailIllustrationUri,
                                        caption = tailIllustrationCaption,
                                        maxHeight = 80
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tailIllustrationCaption,
                                        onValueChange = {
                                            tailIllustrationCaption = it
                                            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, it)
                                        },
                                        label = { Text("Tailpiece Caption (Optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                } else {
                                    Text(
                                        text = "No tailpiece ornament selected.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { tailImagePicker.launch("image/*") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick Photo", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            tailIllustrationUri = "drawable:tailpiece"
                                            tailIllustrationCaption = "— End of Chapter —"
                                            onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preset Tailpiece", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // ==========================================
                    // 3. AI DRAFT PROMPT WORKSHOP
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFF9F7F4))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Chicago Style AI Prompt Workshop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "Direct Gemini with precise CMOS editorial constraints for this section.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = aiDraftPrompt,
                            onValueChange = {
                                aiDraftPrompt = it
                                onSaveAiPrompt(section, it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("input_ai_prompt"),
                            label = { Text("Custom Editorial Prompt for Gemini") },
                            textStyle = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldDark,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Preset Chicago Style Constraints:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val presets = listOf(
                            "Dialogue Pacing: Use smart quotation marks, commas before dialogue tags inside quotes, and em-dashes for interrupted speech.",
                            "Historical & Narrative Rigor: Ensure evocative descriptive depth, sensory clarity, and period-accurate vocabulary without clichés.",
                            "Chicago §13 Block Quotes: Format quotations of over 100 words with 0.5-inch paragraph indentation and no opening/closing quotes."
                        )

                        presets.forEach { preset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        aiDraftPrompt += "\n\n$preset"
                                        onSaveAiPrompt(section, aiDraftPrompt)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Preset constraint added to prompt.") }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "+ $preset",
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // EDITORIAL COMMENTS BOTTOM SHEET
        if (showCommentsSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showCommentsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Editorial Comments & Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "Track editorial suggestions, Chicago style notes, and proofreading flags.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No editorial comments yet for this section.", color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.5.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comments) { comment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (comment.isResolved) Color(0xFFF5F5F5) else Color(0xFFFFF8E1)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = comment.isResolved,
                                            onCheckedChange = { onResolveComment(comment.id, it) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = comment.commentText, fontSize = 12.5.sp)
                                            if (comment.cmosRuleReference.isNotBlank()) {
                                                Text(
                                                    text = comment.cmosRuleReference,
                                                    fontSize = 10.5.sp,
                                                    color = BookGoldDark,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            label = { Text("Add feedback or CMOS note...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    onAddComment(section.id, section.manuscriptId, newCommentText, newCommentCmosRef)
                                    newCommentText = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = BookGoldDark)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * Sleek, ultra-compact formatting toolbar docked above the soft keyboard.
 * Uses horizontal scrolling and compact buttons to give maximum vertical space to prose canvas.
 */
@Composable
fun CompactCmosToolbar(
    onApplyItalics: () -> Unit,
    onApplyBold: () -> Unit,
    onApplySmartQuotes: () -> Unit,
    onApplyEmDash: () -> Unit,
    onApplyEnDash: () -> Unit,
    onApplyIndent: () -> Unit,
    onApplyFootnote: () -> Unit,
    onApplyBlockQuote: () -> Unit,
    onApplyHeadlineCase: () -> Unit,
    onFullPolish: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Italics Button (I)
            ToolbarChip(
                onClick = onApplyItalics,
                testTag = "btn_toolbar_italics"
            ) {
                Text(
                    text = "I",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Bold Button (B)
            ToolbarChip(
                onClick = onApplyBold,
                testTag = "btn_toolbar_bold"
            ) {
                Text(
                    text = "B",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Smart Quotes
            ToolbarChip(
                onClick = onApplySmartQuotes,
                testTag = "btn_toolbar_quotes"
            ) {
                Text(text = "“ ”", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Em-Dash
            ToolbarChip(
                onClick = onApplyEmDash,
                testTag = "btn_toolbar_emdash"
            ) {
                Text(text = "—", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // En-Dash
            ToolbarChip(
                onClick = onApplyEnDash,
                testTag = "btn_toolbar_endash"
            ) {
                Text(text = "–", fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }

            // Paragraph Indent (4 spaces)
            ToolbarChip(
                onClick = onApplyIndent,
                testTag = "btn_toolbar_indent"
            ) {
                Text(text = "⇥ Indent", fontSize = 10.5.sp)
            }

            // Footnote
            ToolbarChip(
                onClick = onApplyFootnote,
                testTag = "btn_toolbar_footnote"
            ) {
                Text(text = "[^N]", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BookGoldDark)
            }

            // Block Quote
            ToolbarChip(
                onClick = onApplyBlockQuote,
                testTag = "btn_toolbar_blockquote"
            ) {
                Text(text = "“...”", fontSize = 10.5.sp)
            }

            // Title Case
            ToolbarChip(
                onClick = onApplyHeadlineCase,
                testTag = "btn_toolbar_headline"
            ) {
                Text(text = "Aa Case", fontSize = 10.5.sp)
            }

            // CMOS Full Polish
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BookGoldDark,
                modifier = Modifier
                    .height(30.dp)
                    .clickable { onFullPolish() }
                    .testTag("btn_toolbar_polish")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Polish", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Copy & Paste
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp), tint = Color.DarkGray)
            }

            IconButton(
                onClick = onPaste,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(15.dp), tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ToolbarChip(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .height(30.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
