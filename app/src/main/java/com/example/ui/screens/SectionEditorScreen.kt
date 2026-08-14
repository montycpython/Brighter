package com.example.ui.screens

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
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    onSaveTitle: (SectionEntity, String, String) -> Unit,
    onUpdateStatus: (SectionEntity, SectionStatus) -> Unit,
    onAddComment: (sectionId: Long, manuscriptId: Long, text: String, cmosRef: String) -> Unit,
    onResolveComment: (commentId: Long, resolved: Boolean) -> Unit
) {
    var title by remember(section.id) { mutableStateOf(section.title) }
    var subtitle by remember(section.id) { mutableStateOf(section.subtitle) }
    var content by remember(section.id) { mutableStateOf(section.content) }
    var currentStatus by remember(section.id) { mutableStateOf(section.status) }

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
    val charCount = remember(content) { content.length }
    val estimatedPages = remember(wordCount) { maxOf(1, (wordCount + 259) / 260) }
    val readingTimeMin = remember(wordCount) { maxOf(1, (wordCount + 199) / 200) }

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
                            // Auto save before leaving
                            onSaveContent(section, content)
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
                            onSaveTitle(section, title, subtitle)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Manuscript section saved.")
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
                                text = "Begin composing your manuscript text here...\n\nUse quotes for dialogue, -- for em-dashes, and [^1] for footnotes. Bwriter's Chicago Manual of Style tools will help polish and format your prose to professional publication grade.",
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
                        text = "Editorial Review & Annotations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showCommentsSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Collaborate with authors, managing editors, and proofreaders honoring Chicago Manual of Style guidelines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Add comment input
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoleBadge(role = currentUser.role)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentUser.name} (${currentUser.role.title})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write editorial comment, typo correction, or CMOS remark...") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_comment")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCommentCmosRef,
                                onValueChange = { newCommentCmosRef = it },
                                label = { Text("CMOS Rule (Optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        onAddComment(section.id, section.manuscriptId, newCommentText.trim(), newCommentCmosRef.trim())
                                        newCommentText = ""
                                    }
                                },
                                enabled = newCommentText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                modifier = Modifier.testTag("btn_submit_comment")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of existing comments
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No editorial comments on this section yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (comment.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RoleBadge(role = comment.authorRole)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = comment.authorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = comment.isResolved,
                                            onCheckedChange = { resolved ->
                                                onResolveComment(comment.id, resolved)
                                            },
                                            modifier = Modifier.testTag("chk_resolve_comment_${comment.id}")
                                        )
                                        Text(
                                            text = if (comment.isResolved) "Resolved" else "Open",
                                            fontSize = 11.sp,
                                            color = if (comment.isResolved) ForestCloth else CrimsonSeal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment.commentText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (comment.cmosRuleReference.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Rule Reference: ${comment.cmosRuleReference}",
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
        }
    }
}
