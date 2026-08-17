package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.model.ServerlessMailMessage
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.InkBlack
import com.example.ui.theme.ParchmentCream
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.model.CharacterEntity
import com.example.model.ManuscriptEntity
import com.example.model.StorySettingEntity
import com.example.ui.theme.BookGoldLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionEditorScreen(
    section: SectionEntity,
    comments: List<EditorialCommentEntity>,
    currentUser: UserProfile,
    manuscript: ManuscriptEntity? = null,
    characters: List<CharacterEntity> = emptyList(),
    settings: List<StorySettingEntity> = emptyList(),
    onBack: () -> Unit,
    onSaveContent: (SectionEntity, String) -> Unit,
    onSaveAiPrompt: (SectionEntity, String) -> Unit,
    onSaveTitle: (SectionEntity, String, String) -> Unit,
    onSaveIllustrations: (SectionEntity, String, String, String, String) -> Unit,
    onUpdateStatus: (SectionEntity, SectionStatus) -> Unit,
    onAddComment: (sectionId: Long, manuscriptId: Long, text: String, cmosRef: String) -> Unit,
    onResolveComment: (commentId: Long, resolved: Boolean) -> Unit,
    onSaveCharacter: (CharacterEntity) -> Unit = {},
    onDeleteCharacter: (Long) -> Unit = {},
    onSaveSetting: (StorySettingEntity) -> Unit = {},
    onDeleteSetting: (Long) -> Unit = {},
    onGenerateAiDraft: (SectionEntity, String, (String) -> Unit) -> Unit = { _, _, _ -> },
    isGeneratingAi: Boolean = false,
    subscription: com.example.model.UserAiSubscription? = null,
    onOpenSubscription: () -> Unit = {},
    onSaveAssignment: (SectionEntity, String, WorkRole, String) -> Unit = { _, _, _, _ -> },
    onSendServerlessMail: (ServerlessMailMessage) -> Unit = {},
    onProposeRevision: (SectionEntity, String) -> Unit = { _, _ -> },
    onAcceptRevision: (SectionEntity) -> Unit = {},
    onRejectRevision: (SectionEntity) -> Unit = {},
    onSwitchRole: (WorkRole) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val isAuthorOfBook = manuscript?.authorEmail.equals(currentUser.email, ignoreCase = true) ||
                         (currentUser.penName.isNotBlank() && manuscript?.authorPenName.equals(currentUser.penName, ignoreCase = true)) ||
                         (currentUser.name.isNotBlank() && currentUser.name != "Author" && manuscript?.authorName.equals(currentUser.name, ignoreCase = true)) ||
                         currentUser.email.equals(com.example.data.GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)

    val isNonAuthorMode = !isAuthorOfBook
    val isAuthorRoleBlocked = isNonAuthorMode && currentUser.role == WorkRole.AUTHOR
    val isCollaboratorMode = isNonAuthorMode && (currentUser.role == WorkRole.EDITOR || currentUser.role == WorkRole.CONTRIBUTOR)

    var title by remember(section.id) { mutableStateOf(section.title) }
    var subtitle by remember(section.id) { mutableStateOf(section.subtitle) }
    var contentValue by remember(section.id) { mutableStateOf(TextFieldValue(section.content)) }

    var headerIllustrationUri by remember(section.id) { mutableStateOf(section.headerIllustrationUri) }
    var headerIllustrationCaption by remember(section.id) { mutableStateOf(section.headerIllustrationCaption) }
    var tailIllustrationUri by remember(section.id) { mutableStateOf(section.tailIllustrationUri) }
    var tailIllustrationCaption by remember(section.id) { mutableStateOf(section.tailIllustrationCaption) }

    // Section Delegation & Contributor States
    var assignedAuthor by remember(section.id) { mutableStateOf(section.assignedAuthor) }
    var assignedRole by remember(section.id) { mutableStateOf(section.assignedRole) }
    var contributorNotes by remember(section.id) { mutableStateOf(section.contributorNotes) }
    var contributorEmailInput by remember(section.id) { mutableStateOf("") }
    var showRoleDropdown by remember { mutableStateOf(false) }

    val handleBackAndSave: () -> Unit = {
        focusManager.clearFocus()
        onSaveContent(section, contentValue.text)
        onSaveTitle(section, title, subtitle)
        onSaveIllustrations(section, headerIllustrationUri, headerIllustrationCaption, tailIllustrationUri, tailIllustrationCaption)
        onBack()
    }

    BackHandler {
        handleBackAndSave()
    }

    var aiDraftPrompt by remember(section.id) {
        mutableStateOf(
            if (section.aiDraftPrompt.isNotBlank()) section.aiDraftPrompt
            else "Write or refine a chapter section titled '${section.title}' adhering strictly to The Chicago Manual of Style (17th Edition). Include narrative pacing, rich sensory detail, dialogue with proper smart quotes, em-dashes for parenthetical breaks, and no missing serial commas."
        )
    }
    var currentStatus by remember(section.id) { mutableStateOf(section.status) }

    // Dialog States
    var showCharacterProfiler by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Questionnaire States
    var questionnaireGoal by remember(section.id) { mutableStateOf("") }
    var questionnaireConflict by remember(section.id) { mutableStateOf("") }
    var questionnairePacing by remember(section.id) { mutableStateOf("Contemplative & Atmospheric") }
    var questionnaireSensory by remember(section.id) { mutableStateOf("") }
    var intertextualTouchstones by remember(section.id) { mutableStateOf("") }
    var customDirectives by remember(section.id) { mutableStateOf("") }
    var selectedLiteraryDevices by remember { mutableStateOf(setOf<String>()) }
    var selectedCharactersForPrompt by remember { mutableStateOf(setOf<CharacterEntity>()) }
    var selectedSettingsForPrompt by remember { mutableStateOf(setOf<StorySettingEntity>()) }

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
                        onClick = { handleBackAndSave() },
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
                            Icon(imageVector = Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments", tint = BookGoldDark)
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

            // Compact Navigation Tabs (38dp height)
            TabRow(
                selectedTabIndex = selectedEditorTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BookGoldDark,
                modifier = Modifier.height(38.dp)
            ) {
                Tab(
                    selected = selectedEditorTab == 0,
                    onClick = { selectedEditorTab = 0 },
                    text = { Text("Prose Canvas", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_manuscript_prose")
                )
                Tab(
                    selected = selectedEditorTab == 1,
                    onClick = { selectedEditorTab = 1 },
                    text = { Text("Illustrations", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_chapter_art")
                )
                Tab(
                    selected = selectedEditorTab == 2,
                    onClick = { selectedEditorTab = 2 },
                    text = { Text("Contributor & Roles", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_contributor_delegation")
                )
                Tab(
                    selected = selectedEditorTab == 3,
                    onClick = { selectedEditorTab = 3 },
                    text = { Text("AI Workshop", fontSize = 11.sp, fontWeight = if (selectedEditorTab == 3) FontWeight.Bold else FontWeight.Normal) },
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
                        // Role & Collaboration Banners
                        if (isAuthorRoleBlocked) {
                            Surface(
                                color = Color(0xFFFFF8E1),
                                border = BorderStroke(1.dp, Color(0xFFFFB300)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Author Read-Only Mode",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You are viewing a manuscript authored by ${manuscript?.effectiveAuthorByline ?: "Author"}. Under publishing rules, you must be in Editor or Contributor mode to edit books you did not author.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF5D4037),
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onSwitchRole(WorkRole.EDITOR) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                            border = BorderStroke(1.dp, Color(0xFFE65100)),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Switch to Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { onSwitchRole(WorkRole.CONTRIBUTOR) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                            border = BorderStroke(1.dp, Color(0xFFE65100)),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Switch to Contributor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else if (isCollaboratorMode) {
                            Surface(
                                color = Color(0xFFE3F2FD),
                                border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0D47A1), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Collaborating as ${currentUser.role.displayName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF0D47A1)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onProposeRevision(section, contentValue.text)
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Tracked revision submitted to Author!") }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Submit Revision", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Edits you save will be marked Under Review and highlighted in Red. Your pen name (${currentUser.effectivePenName}) and words contributed will be logged in Acknowledgments upon sync.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF1565C0),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        // Prominent Redline Tracked Revisions Card (Under Review)
                        if (section.hasPendingRevision) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
                                border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFD32F2F)
                                            ) {
                                                Text(
                                                    text = "UNDER REVIEW",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Tracked Revisions Proposed",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFFB71C1C)
                                            )
                                        }
                                        Text(
                                            text = "+${section.revisionDeltaWords} words",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "By: ${section.revisionAuthorPenName.ifBlank { "Contributor" }} (${section.revisionAuthorRole}) • ${section.revisionAuthorEmail}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF5A1A1A),
                                        fontFamily = FontFamily.Serif
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Red Highlight Diff Preview Box
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "PROPOSED REDLINE DIFF:",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC62828)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = section.pendingEditedContent.ifBlank { "[Empty revision content]" },
                                                fontSize = 12.sp,
                                                color = Color(0xFFB71C1C),
                                                fontFamily = FontFamily.Serif,
                                                lineHeight = 16.sp,
                                                maxLines = 6
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isAuthorOfBook) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onAcceptRevision(section)
                                                    contentValue = TextFieldValue(section.pendingEditedContent)
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Revision accepted! Contributor credited in Acknowledgments.") }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_accept_revision")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept & Credit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    onRejectRevision(section)
                                                    contentValue = TextFieldValue(section.originalAuthorContent)
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Revision rejected. Original prose restored.") }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                                border = BorderStroke(1.dp, Color(0xFFC62828)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_reject_revision")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject Changes", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Awaiting decision by author ${manuscript?.effectiveAuthorByline ?: "Author"}.",
                                            fontSize = 10.5.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Color(0xFF7F0000)
                                        )
                                    }
                                }
                            }
                        }

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
                                            if (!isAuthorRoleBlocked) {
                                                contentValue = it
                                                onSaveContent(section, it.text)
                                            }
                                        },
                                        readOnly = isAuthorRoleBlocked,
                                        visualTransformation = markdownVisualTransformation,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .testTag("input_section_content"),
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 14.5.sp,
                                            lineHeight = 22.sp,
                                            color = if (section.hasPendingRevision) Color(0xFFB71C1C) else InkBlack
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
                    // 2. CONTRIBUTOR & ROLES DELEGATION WORKSHOP
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF101018))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = BookGoldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Section Delegation & Roles",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                }
                                Text(
                                    text = "Assign chapters or front/back matter to co-authors, editors, or guest contributors.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0A89C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ACTIVE ASSIGNMENT STATUS CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181824)),
                            border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CURRENTLY ASSIGNED TO",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BookGoldLight,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = assignedAuthor.ifBlank { "Unassigned" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = Color(0xFFF3EFE6)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BookGoldDark.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, BookGoldLight)
                                    ) {
                                        Text(
                                            text = assignedRole.badgeLabel,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BookGoldLight,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFF262638))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Section: ${section.title} (${section.matterType.name})",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE0DBD1)
                                    )
                                    Text(
                                        text = "Status: ${currentStatus.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BookGoldLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ASSIGN RESPONSIBILITY SECTION
                        Text(
                            text = "Assign Responsibility:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Presets
                        Text(
                            text = "Quick Select Collaborator:",
                            fontSize = 10.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "👑 Myself (${currentUser.displayName})" to (currentUser.displayName to currentUser.email),
                                "Eleanor Vance" to ("Eleanor Vance" to "eleanor.types@bwriter.press"),
                                "Silas Thorne" to ("Silas Thorne" to "silas.thorne@artisan.press"),
                                "Arthur Vance (Editor)" to ("Arthur Vance" to "arthur.editor@bwriter.press")
                            )

                            presets.forEach { (label, data) ->
                                val (name, email) = data
                                val isSelected = assignedAuthor.equals(name, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) BookGoldDark else Color(0xFF1E1E2A),
                                    border = BorderStroke(1.dp, if (isSelected) BookGoldLight else Color(0xFF323246)),
                                    modifier = Modifier.clickable {
                                        assignedAuthor = name
                                        contributorEmailInput = email
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF0E0E14) else Color(0xFFE2DDD5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Name & Email Fields
                        OutlinedTextField(
                            value = assignedAuthor,
                            onValueChange = { assignedAuthor = it },
                            label = { Text("Contributor Display / Pen Name", color = Color.Gray, fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("input_assigned_author"),
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldLight,
                                unfocusedBorderColor = Color(0xFF2C2C3C),
                                focusedContainerColor = Color(0xFF14141C),
                                unfocusedContainerColor = Color(0xFF121218)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = contributorEmailInput,
                            onValueChange = { contributorEmailInput = it },
                            label = { Text("Contributor Gmail / Email Address (for Direct Invite)", color = Color.Gray, fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("input_assigned_email"),
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.5.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldLight,
                                unfocusedBorderColor = Color(0xFF2C2C3C),
                                focusedContainerColor = Color(0xFF14141C),
                                unfocusedContainerColor = Color(0xFF121218)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Selector
                        Text(
                            text = "Assigned Editorial Responsibility:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3EFE6)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkRole.values().forEach { role ->
                                val isSelected = assignedRole == role
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { assignedRole = role },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) BookGoldDark.copy(alpha = 0.3f) else Color(0xFF161622)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) BookGoldLight else Color(0xFF28283A))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = role.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BookGoldLight else Color(0xFFE2DDD5)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = when (role) {
                                                WorkRole.AUTHOR -> "Draft prose"
                                                WorkRole.EDITOR -> "CMOS review"
                                                WorkRole.CONTRIBUTOR -> "Foreword/Essay"
                                            },
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF9E988E)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Editorial Briefing & Directives Field
                        Text(
                            text = "Editorial Briefing & Scope Guidelines (CMOS 17th):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3EFE6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = contributorNotes,
                            onValueChange = { contributorNotes = it },
                            placeholder = {
                                Text(
                                    "E.g., Focus on Chicago typecasting industry in 1893. Maintain first-person retrospective tone. Target 3,500 words. Due Sept 15.",
                                    color = Color.Gray,
                                    fontSize = 11.5.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .testTag("input_contributor_notes"),
                            textStyle = TextStyle(color = Color(0xFFF3EFE6), fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGoldLight,
                                unfocusedBorderColor = Color(0xFF2C2C3C),
                                focusedContainerColor = Color(0xFF14141C),
                                unfocusedContainerColor = Color(0xFF121218)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save Assignment Button
                        Button(
                            onClick = {
                                onSaveAssignment(section, assignedAuthor, assignedRole, contributorNotes)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Section assigned to $assignedAuthor as ${assignedRole.title}") }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier.fillMaxWidth().testTag("btn_save_assignment")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0E0E14))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Assignment & Directives", fontWeight = FontWeight.Bold, color = Color(0xFF0E0E14), fontSize = 12.5.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ==========================================
                        // INVITATION DISPATCH ACTIONS
                        // ==========================================
                        Text(
                            text = "Dispatch Contributor Invitation:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val inviteSubject = "[Bwriter Assignment] Invitation to Contribute to \"${manuscript?.title ?: section.title}\""
                        val inviteBody = """
Dear ${assignedAuthor.ifBlank { "Colleague" }},

You are invited to contribute to the publication of "${manuscript?.title ?: section.title}" on Bwriter as ${assignedRole.title}.

--- SECTION DETAILS ---
• Section: ${section.title} (${section.matterType.name})
• Assigned Role: ${assignedRole.badgeLabel}
• Format / Page Size: ${manuscript?.targetPageSize ?: "Trade 6\" x 9\""}
• Current Status: ${currentStatus.displayName}

--- EDITORIAL BRIEF & DIRECTIVES ---
${contributorNotes.ifBlank { "Please draft or review this section adhering strictly to The Chicago Manual of Style (17th Edition)." }}

--- CHICAGO MANUAL OF STYLE GUIDELINES ---
1. Headline-style capitalization for chapter subheads.
2. Em-dashes (—) without surrounding spaces for parenthetical pauses.
3. Curly typographic smart quotes (“ ” / ‘ ’).
4. Serial Oxford commas in all lists.

Cordially,
${currentUser.displayName}
${if (currentUser.email.equals("real.artistry@gmail.com", true)) "Editor-in-Chief • Bwriter Editions" else "Lead Author • Bwriter Editions"}
                        """.trimIndent()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gmail Intent Button
                            Button(
                                onClick = {
                                    val targetEmail = contributorEmailInput.trim()
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${if (targetEmail.isNotBlank()) targetEmail else ""}")
                                        putExtra(Intent.EXTRA_SUBJECT, inviteSubject)
                                        putExtra(Intent.EXTRA_TEXT, inviteBody)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "Send Invitation via Gmail / Email"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email client available. Text copied to clipboard.", Toast.LENGTH_SHORT).show()
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("Bwriter Invite", inviteBody))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5221F)),
                                modifier = Modifier.weight(1f).testTag("btn_invite_gmail")
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gmail Invite", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // In-App Directive Button
                            Button(
                                onClick = {
                                    val targetEmail = contributorEmailInput.ifBlank { "eleanor.types@bwriter.press" }.trim()
                                    val mail = ServerlessMailMessage(
                                        recipientEmail = targetEmail,
                                        senderEmail = currentUser.email,
                                        senderName = currentUser.displayName,
                                        subject = inviteSubject,
                                        body = inviteBody,
                                        manuscriptId = section.manuscriptId,
                                        manuscriptTitle = manuscript?.title ?: section.title,
                                        messageType = "EDITORIAL_REVISION"
                                    )
                                    onSendServerlessMail(mail)
                                    Toast.makeText(context, "In-App Directive dispatched to $targetEmail", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier.weight(1f).testTag("btn_dispatch_directive")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("App Directive", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Copy Brief Button
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Bwriter Assignment Brief", inviteBody))
                                Toast.makeText(context, "Assignment brief copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_copy_brief"),
                            border = BorderStroke(1.dp, Color(0xFF38384C))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = BookGoldLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Complete Brief to Clipboard", fontSize = 11.5.sp, color = Color(0xFFE2DDD5))
                        }
                    }
                }
                3 -> {
                    // ==========================================
                    // 3. AI DRAFT PROMPT WORKSHOP (High-Contrast Black Theme)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0E0E12))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = BookGoldLight,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chicago Style AI Workshop",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 18.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                }
                                Text(
                                    text = "CMOS-guided chapter questionnaire, character profiling & intertextuality",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFB0A89C)
                                )
                            }

                            if (isGeneratingAi) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(BookGoldDark.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = BookGoldLight,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Drafting...", fontSize = 11.sp, color = BookGoldLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // A. CHARACTER PROFILES STRIP
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C38))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Character Profiles in Work (${characters.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFFF3EFE6)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showCharacterProfiler = true },
                                        border = BorderStroke(1.dp, BookGoldDark),
                                        modifier = Modifier.height(28.dp).testTag("btn_open_character_profiler")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp), tint = BookGoldLight)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Manage Dossiers", fontSize = 10.5.sp, color = BookGoldLight)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (characters.isEmpty()) {
                                    Text(
                                        text = "No characters profiled yet. Tap 'Manage Dossiers' to profile physical, psychological, and backstory traits.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        characters.forEach { char ->
                                            val isSelected = selectedCharactersForPrompt.contains(char)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) BookGoldDark.copy(alpha = 0.35f) else Color(0xFF1F1F28),
                                                border = BorderStroke(1.dp, if (isSelected) BookGold else Color(0xFF383848)),
                                                modifier = Modifier.clickable {
                                                    selectedCharactersForPrompt = if (isSelected) {
                                                        selectedCharactersForPrompt - char
                                                    } else {
                                                        selectedCharactersForPrompt + char
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = char.name,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF3EFE6)
                                                        )
                                                        Text(
                                                            text = char.role,
                                                            fontSize = 9.5.sp,
                                                            color = BookGoldLight
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = BookGoldDark,
                                                        modifier = Modifier
                                                            .clickable {
                                                                val charSummary = "\n• Character [${char.name} - ${char.role}]: Physical: ${char.physicalDescription}; Psychology: ${char.psychologicalDescription}; Voice: ${char.voiceAndMannerisms}; Backstory: ${char.backstory}"
                                                                aiDraftPrompt += charSummary
                                                                onSaveAiPrompt(section, aiDraftPrompt)
                                                                selectedCharactersForPrompt = selectedCharactersForPrompt + char
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Inserted ${char.name} profile into prompt.") }
                                                            }
                                                            .testTag("btn_quick_insert_char_${char.id}")
                                                    ) {
                                                        Text(
                                                            text = "+ Insert",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ==========================================
                        // B. STORY SETTING & WORLDBUILDING STRIP
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C38))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Story Settings & World (${settings.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFFF3EFE6)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showSettingsDialog = true },
                                        border = BorderStroke(1.dp, BookGoldDark),
                                        modifier = Modifier.height(28.dp).testTag("btn_open_settings_dialog")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp), tint = BookGoldLight)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Manage Settings", fontSize = 10.5.sp, color = BookGoldLight)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (settings.isEmpty()) {
                                    Text(
                                        text = "No custom settings configured. Tap 'Manage Settings' to define locations, eras, and sensory atmospheres.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        settings.forEach { set ->
                                            val isSelected = selectedSettingsForPrompt.contains(set)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) BookGoldDark.copy(alpha = 0.35f) else Color(0xFF1F1F28),
                                                border = BorderStroke(1.dp, if (isSelected) BookGold else Color(0xFF383848)),
                                                modifier = Modifier.clickable {
                                                    selectedSettingsForPrompt = if (isSelected) {
                                                        selectedSettingsForPrompt - set
                                                    } else {
                                                        selectedSettingsForPrompt + set
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = set.locationName,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF3EFE6)
                                                        )
                                                        if (set.timePeriodOrEra.isNotBlank()) {
                                                            Text(
                                                                text = set.timePeriodOrEra,
                                                                fontSize = 9.5.sp,
                                                                color = BookGoldLight
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = BookGoldDark,
                                                        modifier = Modifier
                                                            .clickable {
                                                                val setSummary = "\n• Setting [${set.locationName} (${set.timePeriodOrEra})]: Atmosphere: ${set.atmosphereAndSensory}; Architecture: ${set.architecturalOrSpatialDetails}; Historical Context: ${set.historicalOrCulturalContext}"
                                                                aiDraftPrompt += setSummary
                                                                onSaveAiPrompt(section, aiDraftPrompt)
                                                                selectedSettingsForPrompt = selectedSettingsForPrompt + set
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Inserted setting into prompt.") }
                                                            }
                                                            .testTag("btn_quick_insert_setting_${set.id}")
                                                    ) {
                                                        Text(
                                                            text = "+ Insert",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // C. GUIDED CHAPTER QUESTIONNAIRE (Base Template)
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151D)),
                            border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Quiz, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Guided Chapter Questionnaire (Author Blueprint)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                }
                                Text(
                                    text = "Answer key narrative questions to shape the chapter's purpose, stakes, and sensory atmosphere.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0A89C)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Question 1: Goal & Turning Point
                                Text("1. Chapter Goal & Turning Point", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BookGoldLight)
                                OutlinedTextField(
                                    value = questionnaireGoal,
                                    onValueChange = { questionnaireGoal = it },
                                    placeholder = { Text("e.g. Silas discovers the altered Aldine proof and confronts Eleanor before dawn.", color = Color.Gray, fontSize = 11.5.sp) },
                                    modifier = Modifier.fillMaxWidth().height(68.dp).testTag("input_quest_goal"),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BookGold,
                                        unfocusedBorderColor = Color(0xFF3B3B4C),
                                        focusedContainerColor = Color(0xFF1B1B24),
                                        unfocusedContainerColor = Color(0xFF14141A)
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Question 2: Conflict & Stakes
                                Text("2. Central Conflict & Emotional Friction", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BookGoldLight)
                                OutlinedTextField(
                                    value = questionnaireConflict,
                                    onValueChange = { questionnaireConflict = it },
                                    placeholder = { Text("e.g. The pressure of impending commercial buyout vs. artisanal fidelity.", color = Color.Gray, fontSize = 11.5.sp) },
                                    modifier = Modifier.fillMaxWidth().height(68.dp).testTag("input_quest_conflict"),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BookGold,
                                        unfocusedBorderColor = Color(0xFF3B3B4C),
                                        focusedContainerColor = Color(0xFF1B1B24),
                                        unfocusedContainerColor = Color(0xFF14141A)
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Question 3: Pacing & Mood
                                Text("3. Pacing & Tone", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BookGoldLight)
                                val pacingOptions = listOf("Contemplative & Lyrical", "Tense & Suspenseful", "Urgent & Dramatic", "Melancholic", "Analytical & Precise")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pacingOptions.forEach { pace ->
                                        FilterChip(
                                            selected = questionnairePacing == pace,
                                            onClick = { questionnairePacing = pace },
                                            label = { Text(pace, fontSize = 10.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BookGoldDark,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1E1E28),
                                                labelColor = Color(0xFFD0CAC0)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Question 4: Sensory & Physical Details
                                Text("4. Sensory Atmosphere & Physical Artifacts", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BookGoldLight)
                                OutlinedTextField(
                                    value = questionnaireSensory,
                                    onValueChange = { questionnaireSensory = it },
                                    placeholder = { Text("e.g. Smell of turpentine, rattle of Miehle cylinder press, cold Chicago rain.", color = Color.Gray, fontSize = 11.5.sp) },
                                    modifier = Modifier.fillMaxWidth().height(68.dp).testTag("input_quest_sensory"),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BookGold,
                                        unfocusedBorderColor = Color(0xFF3B3B4C),
                                        focusedContainerColor = Color(0xFF1B1B24),
                                        unfocusedContainerColor = Color(0xFF14141A)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // D. SCROLLABLE LITERARY DEVICES CATALOG
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151D)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C38))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Literary Devices & Narrative Craft (Scrollable)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                }
                                Text(
                                    text = "Tap any literary device to incorporate its technique into the chapter prompt.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0A89C)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val literaryDevices = listOf(
                                    "Free Indirect Discourse (Blending 3rd-person narrator with character internal voice)",
                                    "Foreshadowing & Omen (Subtle atmospheric warning of the coming crisis)",
                                    "Dramatic Irony (The reader discovers the secret before the protagonist)",
                                    "Chiaroscuro Imagery (High-contrast play of candlelight, gas jets, and shadows)",
                                    "Stream of Consciousness (Associative, sensory-driven psychological monologue)",
                                    "Motif & Leitmotif Echo (Recurring typographic symbol of the leaf and ink)",
                                    "Epistolary Inset (A formal Chicago §13.8 indented historical letter fragment)",
                                    "Sensory Synesthesia (Intertwining the smell of hot antimony with tactile rhythm)",
                                    "Unspoken Subtext & Pauses (Chicago em-dash cadence reflecting emotional hesitation)",
                                    "Objective Correlative (The brass composing stick embodying lost artisanal pride)",
                                    "Pathetic Fallacy (Lake Michigan's freezing fog mirroring ideological isolation)",
                                    "In Media Res Opening (Beginning immediately in the turning point)",
                                    "Periodic Sentence Structure (CMOS §5.233 suspenseful delayed main clause)",
                                    "Allegorical Resonance (The printing press as a metaphor for human mortality)",
                                    "Lyrical Anaphora (Repetition of introductory cadence phrases)"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    literaryDevices.forEach { device ->
                                        val shortName = device.substringBefore(" (")
                                        val isSelected = selectedLiteraryDevices.contains(device)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedLiteraryDevices = if (isSelected) {
                                                    selectedLiteraryDevices - device
                                                } else {
                                                    aiDraftPrompt += "\n• Apply Literary Device: $device"
                                                    onSaveAiPrompt(section, aiDraftPrompt)
                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Added '$shortName' to prompt.") }
                                                    selectedLiteraryDevices + device
                                                }
                                            },
                                            label = { Text("+ $shortName", fontSize = 10.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BookGoldDark,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1E1E28),
                                                labelColor = Color(0xFFD0CAC0)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // E. INTERTEXTUALITY & COMPANION TEXTS
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151D)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C38))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Intertextuality Studio (Literary Touchstones)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                }
                                Text(
                                    text = "Suggest companion texts and masterworks for your final draft to converse with.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0A89C)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val intertextualPresets = listOf(
                                    "Theodore Dreiser (Sister Carrie - Industrial grit & urban realism)",
                                    "Henry James (Turn of the Screw - Psychological ambiguity & complex syntax)",
                                    "Robert Bringhurst (The Elements of Typographic Style - Sacred leaf craft)",
                                    "Herman Melville (Moby-Dick - Metaphysical inquiry & grandiose prose)",
                                    "Virginia Woolf (To the Lighthouse - Fluid temporal consciousness)",
                                    "Cormac McCarthy (Blood Meridian - Archaic cadence & stark atmosphere)",
                                    "Mary Shelley (Frankenstein - Obsessive pursuit of artisanal creation)",
                                    "Umberto Eco (The Name of the Rose - Archival intrigue & textual mystery)"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    intertextualPresets.forEach { preset ->
                                        val authorName = preset.substringBefore(" (")
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1E1E28),
                                            border = BorderStroke(0.5.dp, Color(0xFF3A3A4C)),
                                            modifier = Modifier.clickable {
                                                intertextualTouchstones = if (intertextualTouchstones.isBlank()) preset else "$intertextualTouchstones; $preset"
                                                aiDraftPrompt += "\n• Intertextual Dialogue: $preset"
                                                onSaveAiPrompt(section, aiDraftPrompt)
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Added $authorName to intertextuality.") }
                                            }
                                        ) {
                                            Text(
                                                text = "+ $authorName",
                                                fontSize = 10.5.sp,
                                                color = BookGoldLight,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = intertextualTouchstones,
                                    onValueChange = { intertextualTouchstones = it },
                                    placeholder = { Text("Enter custom books, authors, or literary movements to communicate with...", color = Color.Gray, fontSize = 11.5.sp) },
                                    modifier = Modifier.fillMaxWidth().height(60.dp).testTag("input_intertextuality"),
                                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BookGold,
                                        unfocusedBorderColor = Color(0xFF3B3B4C),
                                        focusedContainerColor = Color(0xFF1B1B24),
                                        unfocusedContainerColor = Color(0xFF14141A)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ==========================================
                        // F. SUBSCRIPTION WALLET & CREDITS
                        // ==========================================
                        if (subscription != null) {
                            val isSuper = currentUser.email.equals("real.artistry@gmail.com", ignoreCase = true) || subscription.plan == com.example.model.SubscriptionPlan.SUPERUSER_UNLIMITED
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSubscription() },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B26)),
                                border = BorderStroke(1.dp, if (subscription.creditsRemaining > 0 || isSuper) BookGoldDark else Color(0xFFC62828))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = BookGoldLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isSuper) "👑 Editor-in-Chief Unlimited Superuser" else "${subscription.plan.title} (${subscription.plan.priceMonthly})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFF3EFE6)
                                            )
                                            Text(
                                                text = if (isSuper) "Unlimited AI generations • Active Pass" else "${subscription.creditsRemaining} credits remaining (${subscription.totalTokensUsed} tokens burned)",
                                                fontSize = 10.5.sp,
                                                color = if (subscription.creditsRemaining > 0 || isSuper) Color(0xFFB0A89C) else Color(0xFFFF8A80)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (subscription.creditsRemaining > 0 || isSuper) BookGoldDark.copy(alpha = 0.25f) else Color(0xFFC62828).copy(alpha = 0.3f),
                                        border = BorderStroke(0.5.dp, if (subscription.creditsRemaining > 0 || isSuper) BookGoldLight else Color(0xFFFF5252))
                                    ) {
                                        Text(
                                            text = if (subscription.creditsRemaining > 0 || isSuper) "Manage Pass" else "Get Credits",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (subscription.creditsRemaining > 0 || isSuper) BookGoldLight else Color(0xFFFF8A80),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // ==========================================
                        // G. COMPILED CMOS PROMPT WORKSHOP
                        // ==========================================
                        Text(
                            text = "Active CMOS Prompt & Directives:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = BookGoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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
                            label = { Text("Gemini CMOS Prompt", color = BookGoldLight) },
                            textStyle = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp, color = Color(0xFFF3EFE6)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BookGold,
                                unfocusedBorderColor = Color(0xFF3B3B4C),
                                focusedContainerColor = Color(0xFF15151D),
                                unfocusedContainerColor = Color(0xFF121218)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // COMPILE BUTTON & GENERATE BUTTON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val compiled = com.example.ai.GeminiProseGenerator.buildComprehensivePrompt(
                                        manuscript = manuscript,
                                        section = section,
                                        questionnaireGoal = questionnaireGoal,
                                        questionnaireConflict = questionnaireConflict,
                                        questionnairePacing = questionnairePacing,
                                        questionnaireSensory = questionnaireSensory,
                                        selectedCharacters = selectedCharactersForPrompt.toList().ifEmpty { characters.take(2) },
                                        selectedSettings = selectedSettingsForPrompt.toList().ifEmpty { settings.take(1) },
                                        selectedLiteraryDevices = selectedLiteraryDevices.toList(),
                                        intertextualTouchstones = intertextualTouchstones,
                                        customDirectives = customDirectives
                                    )
                                    aiDraftPrompt = compiled
                                    onSaveAiPrompt(section, compiled)
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Compiled comprehensive CMOS prompt.") }
                                },
                                border = BorderStroke(1.dp, BookGoldDark),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_compile_prompt")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp), tint = BookGoldLight)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compile Prompt", fontSize = 11.5.sp, color = BookGoldLight, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val promptToRun = if (aiDraftPrompt.isNotBlank()) aiDraftPrompt else {
                                        com.example.ai.GeminiProseGenerator.buildComprehensivePrompt(
                                            manuscript = manuscript,
                                            section = section,
                                            questionnaireGoal = questionnaireGoal,
                                            questionnaireConflict = questionnaireConflict,
                                            questionnairePacing = questionnairePacing,
                                            questionnaireSensory = questionnaireSensory,
                                            selectedCharacters = selectedCharactersForPrompt.toList().ifEmpty { characters },
                                            selectedSettings = selectedSettingsForPrompt.toList().ifEmpty { settings },
                                            selectedLiteraryDevices = selectedLiteraryDevices.toList(),
                                            intertextualTouchstones = intertextualTouchstones,
                                            customDirectives = customDirectives
                                        )
                                    }
                                    onGenerateAiDraft(section, promptToRun) { generated ->
                                        contentValue = TextFieldValue(
                                            if (contentValue.text.isBlank()) generated
                                            else "${contentValue.text.trimEnd()}\n\n$generated"
                                        )
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Chapter prose generated & added to editor!")
                                        }
                                    }
                                },
                                enabled = !isGeneratingAi,
                                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                modifier = Modifier.weight(1.2f).height(44.dp).testTag("btn_generate_chapter_prose")
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generating...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate Prose", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // DIALOGS: CHARACTER PROFILER & SETTINGS STUDIO
        // ==========================================
        if (showCharacterProfiler) {
            CharacterProfilerDialog(
                manuscriptId = section.manuscriptId,
                characters = characters,
                onDismiss = { showCharacterProfiler = false },
                onSaveCharacter = { char ->
                    onSaveCharacter(char)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Character '${char.name}' saved.") }
                },
                onDeleteCharacter = { charId ->
                    onDeleteCharacter(charId)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Character profile deleted.") }
                },
                onInsertIntoPrompt = { char ->
                    val charSummary = "\n• Profile [${char.name} - ${char.role}]: Physical: ${char.physicalDescription}; Psychology: ${char.psychologicalDescription}; Voice: ${char.voiceAndMannerisms}; Backstory: ${char.backstory}"
                    aiDraftPrompt += charSummary
                    onSaveAiPrompt(section, aiDraftPrompt)
                    selectedCharactersForPrompt = selectedCharactersForPrompt + char
                    coroutineScope.launch { snackbarHostState.showSnackbar("Inserted ${char.name} into AI prompt.") }
                }
            )
        }

        if (showSettingsDialog) {
            SettingIntertextualityDialog(
                manuscriptId = section.manuscriptId,
                settings = settings,
                onDismiss = { showSettingsDialog = false },
                onSaveSetting = { set ->
                    onSaveSetting(set)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Setting '${set.locationName}' saved.") }
                },
                onDeleteSetting = { setId ->
                    onDeleteSetting(setId)
                    coroutineScope.launch { snackbarHostState.showSnackbar("Setting deleted.") }
                },
                onInsertSetting = { set ->
                    val setSummary = "\n• Setting [${set.locationName} (${set.timePeriodOrEra})]: Atmosphere: ${set.atmosphereAndSensory}; Architecture: ${set.architecturalOrSpatialDetails}; Historical Context: ${set.historicalOrCulturalContext}"
                    aiDraftPrompt += setSummary
                    onSaveAiPrompt(section, aiDraftPrompt)
                    selectedSettingsForPrompt = selectedSettingsForPrompt + set
                    coroutineScope.launch { snackbarHostState.showSnackbar("Inserted ${set.locationName} into AI prompt.") }
                }
            )
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BookGoldDark)
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
