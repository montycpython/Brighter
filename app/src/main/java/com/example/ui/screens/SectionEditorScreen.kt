package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cmos.CmosFormatter
import com.example.model.EditorialCommentEntity
import com.example.model.LeafSide
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.ui.components.CmosToolbar
import com.example.ui.components.LeafBadge
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.CrimsonSeal
import com.example.ui.theme.ForestCloth
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
    onUpdateStatus: (SectionEntity, SectionStatus) -> Unit,
    onAddComment: (sectionId: Long, manuscriptId: Long, text: String, cmosRef: String) -> Unit,
    onResolveComment: (commentId: Long, resolved: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var title by remember(section.id) { mutableStateOf(section.title) }
    var subtitle by remember(section.id) { mutableStateOf(section.subtitle) }
    var content by remember(section.id) { mutableStateOf(section.content) }
    var aiDraftPrompt by remember(section.id) {
        mutableStateOf(
            if (section.aiDraftPrompt.isNotBlank()) section.aiDraftPrompt
            else "Write or refine a chapter section titled '${section.title}' adhering strictly to The Chicago Manual of Style (17th Edition). Include narrative pacing, rich sensory detail, dialogue with proper smart quotes, em-dashes for parenthetical breaks, and no missing serial commas."
        )
    }
    var currentStatus by remember(section.id) { mutableStateOf(section.status) }

    // Editor Tab: 0 = Manuscript Prose, 1 = AI Prompt / Assistant Workshop
    var selectedEditorTab by remember { mutableIntStateOf(0) }

    var showCommentsSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var newCommentCmosRef by remember { mutableStateOf("CMOS 17th Ed.") }
    var showStatusMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Realtime metrics
    val wordCount = remember(content) {
        content.split(Regex("""\s+""")).count { it.isNotBlank() }
    }
    val estimatedPages = remember(wordCount) { maxOf(1, (wordCount + 259) / 260) }

    // Check for Oxford comma issues
    val oxfordIssues = remember(content) {
        CmosFormatter.checkOxfordComma(content)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${section.matterType.displayName} • $wordCount words • ~$estimatedPages leaves",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onSaveContent(section, content)
                            onSaveAiPrompt(section, aiDraftPrompt)
                            onSaveTitle(section, title, subtitle)
                            onBack()
                        },
                        modifier = Modifier.testTag("btn_back_editor")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Status selector
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { showStatusMenu = true }
                            .padding(end = 4.dp)
                            .testTag("btn_change_status")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            StatusBadge(status = currentStatus)
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
                                }
                            )
                        }
                    }

                    // Comments button
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.testTag("btn_open_comments")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Comment,
                                contentDescription = "Editorial Comments",
                                tint = if (comments.any { !it.isResolved }) CrimsonSeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (comments.isNotEmpty()) {
                                Surface(
                                    color = if (comments.any { !it.isResolved }) CrimsonSeal else ForestCloth,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = comments.size.toString(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Save Button
                    IconButton(
                        onClick = {
                            onSaveContent(section, content)
                            onSaveAiPrompt(section, aiDraftPrompt)
                            onSaveTitle(section, title, subtitle)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Manuscript section & AI prompt saved.")
                            }
                        },
                        modifier = Modifier.testTag("btn_save_section")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = BookGoldDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Header: Prose Editor vs AI Prompt Studio
            TabRow(
                selectedTabIndex = selectedEditorTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BookGoldDark,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedEditorTab]),
                        color = BookGoldDark
                    )
                }
            ) {
                Tab(
                    selected = selectedEditorTab == 0,
                    onClick = { selectedEditorTab = 0 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Manuscript Prose", fontWeight = if (selectedEditorTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedEditorTab == 1,
                    onClick = { selectedEditorTab = 1 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = BookGoldDark) },
                    text = { Text("AI Prompt Workshop", fontWeight = if (selectedEditorTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (selectedEditorTab == 0) {
                // ==========================================
                // 1. MANUSCRIPT PROSE CANVAS
                // ==========================================
                // CMOS Interactive Toolbar
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
                        coroutineScope.launch { snackbarHostState.showSnackbar("Full Chicago Manual of Style polish applied!") }
                    }
                )

                // Quick Prompt Paste helper bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Working on: ${section.title}",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                        if (pasted.isNotBlank()) {
                                            content = if (content.isBlank()) pasted else "$content\n\n$pasted"
                                            onSaveContent(section, content)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Pasted AI response into manuscript.")
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste from Clipboard", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = { selectedEditorTab = 1 }
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = BookGoldDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Toggle Prompt", fontSize = 12.sp, color = BookGoldDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Oxford Comma Warning Bar if issues found
                if (oxfordIssues.isNotEmpty()) {
                    Surface(
                        color = BookGoldDark.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${oxfordIssues.size} serial comma suggestion(s) detected",
                                fontSize = 11.5.sp,
                                color = BookGoldDark,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    content = CmosFormatter.fixOxfordCommas(content)
                                    onSaveContent(section, content)
                                }
                            ) {
                                Text("Fix All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Main Editor Canvas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Section Title field
                    BasicTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            onSaveTitle(section, title, subtitle)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(BookGoldDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_section_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle field
                    BasicTextField(
                        value = subtitle,
                        onValueChange = {
                            subtitle = it
                            onSaveTitle(section, title, subtitle)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        cursorBrush = SolidColor(BookGoldDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_section_subtitle"),
                        decorationBox = { innerTextField ->
                            if (subtitle.isEmpty()) {
                                Text(
                                    text = "Optional chapter subtitle or epigraph...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Manuscript Prose Editor
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            onSaveContent(section, content)
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(BookGoldDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(550.dp)
                            .testTag("input_section_content"),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = "Begin composing your manuscript text here...\n\nUse quotes for dialogue, -- for em-dashes, and [^1] for footnotes. Switch to 'AI Prompt Workshop' at the top to draft prompts for external LLMs.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    lineHeight = 28.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            } else {
                // ==========================================
                // 2. AI PROMPT WORKSHOP / ASSISTANT STUDIO
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BookGoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Section AI Prompt Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Craft, tweak, and copy specialized prompts for '${section.title}'. Copy your prompt to your favorite LLM (Gemini, Claude, ChatGPT), then copy the generated text and paste it directly into your manuscript leaf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AI Generation & Refinement Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = aiDraftPrompt,
                        onValueChange = {
                            aiDraftPrompt = it
                            onSaveAiPrompt(section, aiDraftPrompt)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("input_ai_prompt"),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, lineHeight = 20.sp),
                        placeholder = { Text("Enter the instructions and scene details for this chapter...") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset prompt chips
                    Text(
                        text = "Quick CMOS Prompt Templates:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                aiDraftPrompt = "Draft the complete opening scenes for '${section.title}' using rich prose, authentic period dialogue, standard serial commas, and Chicago Manual of Style typographic rules. Pacing should be deliberate and evocative."
                                onSaveAiPrompt(section, aiDraftPrompt)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Draft Scene", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                aiDraftPrompt = "Review and edit the following manuscript section for '${section.title}'. Ensure strict adherence to Chicago Manual of Style (17th Edition): fix hyphenation, enforce serial commas, replace hyphens with em-dashes, and enhance sentence flow:\n\n$content"
                                onSaveAiPrompt(section, aiDraftPrompt)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CMOS Polish", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                aiDraftPrompt = "Generate detailed historical and sensory background research, character dialogue ideas, and atmospheric descriptions for '${section.title}'."
                                onSaveAiPrompt(section, aiDraftPrompt)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Worldbuilding", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("AI Prompt", aiDraftPrompt)
                                clipboardManager.setPrimaryClip(clip)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Prompt copied to clipboard! Paste into your LLM.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_prompt")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Prompt")
                        }

                        Button(
                            onClick = {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                    if (pasted.isNotBlank()) {
                                        content = pasted
                                        onSaveContent(section, content)
                                        selectedEditorTab = 0
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("AI response pasted into manuscript leaf!")
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Clipboard is empty.")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestCloth),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_paste_to_manuscript")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste & Return")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Helpful Workflow Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "How the AI Assistant Workflow Works:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Refine your prompt above for this specific chapter or section.\n2. Tap 'Copy Prompt' to copy to clipboard.\n3. Paste into Gemini, Claude, or ChatGPT.\n4. Copy the LLM's response.\n5. Tap 'Paste & Return' to automatically insert the text into this leaf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Editorial Marginalia & CMOS Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showCommentsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of existing comments
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No editorial notes yet on this section.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comm ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (comm.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (comm.isResolved) MaterialTheme.colorScheme.outlineVariant else CrimsonSeal.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RoleBadge(role = comm.authorRole)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = comm.authorName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (comm.cmosRuleReference.isNotBlank()) {
                                            Surface(
                                                color = BookGoldDark.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = comm.cmosRuleReference,
                                                    fontSize = 10.sp,
                                                    color = BookGoldDark,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = comm.commentText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = if (comm.isResolved) "Resolved" else "Mark as Resolved",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Checkbox(
                                            checked = comm.isResolved,
                                            onCheckedChange = { resolved ->
                                                onResolveComment(comm.id, resolved)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // New Comment Input
                Text(
                    text = "Add Editorial Annotation:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    label = { Text("Comment or Rule Citation") },
                    placeholder = { Text("e.g. CMOS 6.19: Serial comma required before 'and'...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newCommentCmosRef,
                    onValueChange = { newCommentCmosRef = it },
                    label = { Text("Rule / Citation Tag") },
                    placeholder = { Text("CMOS 6.19, CMOS 13.1, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(section.id, section.manuscriptId, newCommentText.trim(), newCommentCmosRef.trim())
                            newCommentText = ""
                        }
                    },
                    enabled = newCommentText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post Marginalia")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
