package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.cmos.CmosFormatter
import com.example.model.EditorialCommentEntity
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkNavy
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.ParchmentPaper
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
    var content by remember(section.id) { mutableStateOf(section.content) }
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

    var showCommentsSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var newCommentCmosRef by remember { mutableStateOf("CMOS 17th Ed.") }
    var showStatusMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    val wordCount = remember(content) {
        content.split(Regex("""\s+""")).count { it.isNotBlank() }
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
                            onSaveContent(section, content)
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
                            onSaveContent(section, content)
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
            // Section Title & Subtitle Edit Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                onSaveTitle(section, title, subtitle)
                            },
                            label = { Text("Title (CMOS Headline Style)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_section_title"),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldDark,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                }
            }

            // Tabs Bar: Manuscript Prose vs Illustrations vs AI Prompt Workshop
            TabRow(
                selectedTabIndex = selectedEditorTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BookGoldDark
            ) {
                Tab(
                    selected = selectedEditorTab == 0,
                    onClick = { selectedEditorTab = 0 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Manuscript Prose", fontSize = 12.sp, fontWeight = if (selectedEditorTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_manuscript_prose")
                )
                Tab(
                    selected = selectedEditorTab == 1,
                    onClick = { selectedEditorTab = 1 },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = BookGoldDark) },
                    text = { Text("Illustrations & Art", fontSize = 12.sp, fontWeight = if (selectedEditorTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_chapter_art")
                )
                Tab(
                    selected = selectedEditorTab == 2,
                    onClick = { selectedEditorTab = 2 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = BookGoldDark) },
                    text = { Text("AI Workshop", fontSize = 12.sp, fontWeight = if (selectedEditorTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_ai_prompt_workshop")
                )
            }

            when (selectedEditorTab) {
                0 -> {
                    // ==========================================
                    // 1. MANUSCRIPT PROSE CANVAS
                    // ==========================================
                    CmosToolbar(
                        onApplySmartQuotes = {
                            content = CmosFormatter.applySmartQuotes(content)
                            onSaveContent(section, content)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Applied Chicago smart quotes “ ”.") }
                        },
                        onApplyEmDash = {
                            content = CmosFormatter.applyEmDashes(content)
                            onSaveContent(section, content)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Applied Chicago em-dashes (—).") }
                        },
                        onApplyEnDash = {
                            content = CmosFormatter.applyEnDashes(content)
                            onSaveContent(section, content)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Applied Chicago en-dashes (–) for ranges.") }
                        },
                        onApplyHeadlineCase = {
                            title = CmosFormatter.toChicagoHeadlineCase(title)
                            onSaveTitle(section, title, subtitle)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Applied Chicago Headline Capitalization to title.") }
                        },
                        onApplyOxfordComma = {
                            content = CmosFormatter.fixOxfordCommas(content)
                            onSaveContent(section, content)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Enforced serial (Oxford) commas.") }
                        },
                        onInsertBlockQuote = {
                            content += "\n\n   “Insert block quotation here (over 100 words), indented by standard 0.5 inch...”\n\n"
                            onSaveContent(section, content)
                        },
                        onInsertFootnote = {
                            val nextNum = (Regex("""\[\^(\d+)\]""").findAll(content).count()) + 1
                            content += "[^$nextNum]"
                            onSaveContent(section, content)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Inserted footnote reference [^$nextNum].") }
                        },
                        onFullPolish = {
                            content = CmosFormatter.polishText(content)
                            title = CmosFormatter.toChicagoHeadlineCase(title)
                            onSaveContent(section, content)
                            onSaveTitle(section, title, subtitle)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Full CMOS 17th Edition Polish applied!") }
                        }
                    )

                    // Prose Leaf Sheet Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFE8E5DF))
                            .padding(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(4.dp, RoundedCornerShape(4.dp)),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = ParchmentCream),
                            border = BorderStroke(1.dp, Color(0xFFD4CBBF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                // Live word count and status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$wordCount words • Paragraphs preserved",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Serif
                                    )
                                    Text(
                                        text = "CMOS 17th Edition Typographic Leaf",
                                        fontSize = 11.sp,
                                        color = BookGoldDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0D8CE))

                                BasicTextField(
                                    value = content,
                                    onValueChange = {
                                        content = it
                                        onSaveContent(section, content)
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .testTag("input_section_content"),
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = InkBlack
                                    ),
                                    cursorBrush = SolidColor(BookGoldDark)
                                )
                            }
                        }
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
                                        Text("Preset Fleuron", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // ==========================================
                    // 3. AI PROMPT & ASSISTANT WORKSHOP
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFFBF9F5))
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
                            text = "Draft your prompt, generate narrative sections, and copy/paste directly to the leaf.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI Drafting Prompt (Saved with Section)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BookGoldDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = aiDraftPrompt,
                                    onValueChange = {
                                        aiDraftPrompt = it
                                        onSaveAiPrompt(section, it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .testTag("input_ai_prompt"),
                                    textStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                                    placeholder = { Text("Describe what scene, dialogue, or bibliographic analysis to generate...") }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clip = ClipData.newPlainText("Bwriter AI Prompt", aiDraftPrompt)
                                            clipboardManager.setPrimaryClip(clip)
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Prompt copied to clipboard!") }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Prompt", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val clip = clipboardManager.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val pasted = clip.getItemAt(0).text.toString()
                                                if (pasted.isNotBlank()) {
                                                    content = if (content.isBlank()) pasted else "$content\n\n$pasted"
                                                    onSaveContent(section, content)
                                                    selectedEditorTab = 0
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Pasted AI response into Manuscript Leaf!") }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste to Leaf", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Editorial Comments Bottom Sheet
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Editorial Feedback & Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (comments.isEmpty()) {
                    Text(
                        text = "No comments for this section yet.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = comment.isResolved,
                                        onCheckedChange = { onResolveComment(comment.id, it) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = comment.commentText, fontSize = 13.sp)
                                        if (comment.cmosRuleReference.isNotBlank()) {
                                            Text(
                                                text = comment.cmosRuleReference,
                                                fontSize = 11.sp,
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

                Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CmosToolbar(
    onApplySmartQuotes: () -> Unit,
    onApplyEmDash: () -> Unit,
    onApplyEnDash: () -> Unit,
    onApplyHeadlineCase: () -> Unit,
    onApplyOxfordComma: () -> Unit,
    onInsertBlockQuote: () -> Unit,
    onInsertFootnote: () -> Unit,
    onFullPolish: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onApplySmartQuotes, modifier = Modifier.testTag("btn_toolbar_quotes")) {
                Text("“ ”", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onApplyEmDash, modifier = Modifier.testTag("btn_toolbar_emdash")) {
                Text("—", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onApplyOxfordComma, modifier = Modifier.testTag("btn_toolbar_oxford")) {
                Text(", and", fontSize = 11.sp)
            }
            OutlinedButton(onClick = onApplyHeadlineCase, modifier = Modifier.testTag("btn_toolbar_headline")) {
                Text("Title Case", fontSize = 11.sp)
            }
            Button(
                onClick = onFullPolish,
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                modifier = Modifier.testTag("btn_toolbar_polish")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CMOS Polish", fontSize = 11.sp)
            }
        }
    }
}
