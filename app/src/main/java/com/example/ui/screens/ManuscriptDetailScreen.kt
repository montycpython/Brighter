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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.example.model.BookTrimSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.SectionType
import com.example.model.UserProfile
import com.example.ui.components.LeafBadge
import com.example.ui.components.MatterBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BlankLeafColor
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.CrimsonSeal
import com.example.ui.theme.ForestCloth
import com.example.ui.theme.InkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManuscriptDetailScreen(
    manuscript: ManuscriptEntity,
    sections: List<SectionEntity>,
    calculatedLeaves: List<CalculatedLeaf>,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onOpenSection: (Long) -> Unit,
    onOpenReader: () -> Unit,
    onOpenExport: () -> Unit,
    onAddSection: (MatterType, SectionType, String, String) -> Unit,
    onDeleteSection: (Long) -> Unit,
    onUpdateManuscript: (ManuscriptEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Front Matter, 1: Text Body, 2: Back Matter, 3: Full Leaf Flow
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val frontSections = sections.filter { it.matterType == MatterType.FRONT_MATTER }
    val bodySections = sections.filter { it.matterType == MatterType.TEXT_BODY }
    val backSections = sections.filter { it.matterType == MatterType.BACK_MATTER }

    val totalWords = sections.sumOf { it.wordCount }
    val totalLeaves = calculatedLeaves.size
    val rectoCount = calculatedLeaves.count { it.side == LeafSide.RECTO }
    val versoCount = calculatedLeaves.count { it.side == LeafSide.VERSO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = manuscript.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${manuscript.workType.displayName} • $totalLeaves Leaves • $totalWords Words",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_detail")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenReader, modifier = Modifier.testTag("btn_action_read_spreads")) {
                        Icon(imageVector = Icons.Default.AutoStories, contentDescription = "Read Spreads", tint = BookGoldDark)
                    }
                    IconButton(onClick = onOpenExport, modifier = Modifier.testTag("btn_action_export_pdf")) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = BookGoldDark)
                    }
                    IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.testTag("btn_action_settings")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Manuscript Metadata")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab != 3) {
                FloatingActionButton(
                    onClick = { showAddSectionDialog = true },
                    containerColor = BookGoldDark,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_section")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Division")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> "Add Front Matter"
                                1 -> "Add Chapter / Part"
                                else -> "Add Back Matter"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // CMOS Recto/Verso Leaf Status Bar
            Surface(
                color = InkNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CHICAGO LEAF STRUCTURE",
                            color = BookGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Recto-Verso Aware Pagination",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = Color(0xFF2E5B88),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Recto: $rectoCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFF884A2E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Verso: $versoCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Division Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Front Matter (${frontSections.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_front_matter")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Text / Body (${bodySections.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_text_body")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Back Matter (${backSections.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_back_matter")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Leaf Flow ($totalLeaves)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_leaf_flow")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> FrontMatterTab(
                    manuscript = manuscript,
                    sections = frontSections,
                    onOpenSection = onOpenSection,
                    onDeleteSection = onDeleteSection
                )
                1 -> BodyMatterTab(
                    sections = bodySections,
                    onOpenSection = onOpenSection,
                    onDeleteSection = onDeleteSection
                )
                2 -> BackMatterTab(
                    manuscript = manuscript,
                    sections = backSections,
                    onOpenSection = onOpenSection,
                    onDeleteSection = onDeleteSection
                )
                3 -> LeafFlowTab(
                    leaves = calculatedLeaves,
                    onOpenSection = { secId ->
                        if (secId != null) onOpenSection(secId)
                    }
                )
            }
        }
    }

    // Add Section Dialog
    if (showAddSectionDialog) {
        AddSectionDialog(
            defaultMatter = when (selectedTab) {
                0 -> MatterType.FRONT_MATTER
                2 -> MatterType.BACK_MATTER
                else -> MatterType.TEXT_BODY
            },
            onDismiss = { showAddSectionDialog = false },
            onAdd = { matter, type, title, subtitle ->
                onAddSection(matter, type, title, subtitle)
                showAddSectionDialog = false
            }
        )
    }

    // Metadata Settings Dialog
    if (showSettingsDialog) {
        ManuscriptSettingsDialog(
            manuscript = manuscript,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                onUpdateManuscript(updated)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun FrontMatterTab(
    manuscript: ManuscriptEntity,
    sections: List<SectionEntity>,
    onOpenSection: (Long) -> Unit,
    onDeleteSection: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Preliminary Leaves (Roman Foliation: i, ii, iii...)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Standard CMOS Display Leaves Overview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Standard Chicago Front Matter Leaves",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LeafBadge(side = LeafSide.RECTO, pageNumber = "i")
                        Text("Half-Title", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LeafBadge(side = LeafSide.RECTO, pageNumber = "iii")
                        Text("Title Page", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LeafBadge(side = LeafSide.VERSO, pageNumber = "iv")
                        Text("Copyright & Colophon", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    if (manuscript.dedication.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LeafBadge(side = LeafSide.RECTO, pageNumber = "v")
                            Text("Dedication", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }
        }

        if (sections.isEmpty()) {
            item {
                Text(
                    text = "No custom front matter sections added (e.g. Foreword, Preface, Acknowledgments). Use the + button to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(sections, key = { it.id }) { section ->
                SectionCard(
                    section = section,
                    onOpen = { onOpenSection(section.id) },
                    onDelete = { onDeleteSection(section.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun BodyMatterTab(
    sections: List<SectionEntity>,
    onOpenSection: (Long) -> Unit,
    onDeleteSection: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Main Body Text (Arabic Foliation: 1, 2, 3... - Recto Openers)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No body chapters drafted yet",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap + to add your first chapter or part divider.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sections, key = { it.id }) { section ->
                SectionCard(
                    section = section,
                    onOpen = { onOpenSection(section.id) },
                    onDelete = { onDeleteSection(section.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun BackMatterTab(
    manuscript: ManuscriptEntity,
    sections: List<SectionEntity>,
    onOpenSection: (Long) -> Unit,
    onDeleteSection: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "End Matter (Appendices, Notes, Bibliography, About Author)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sections.isEmpty()) {
            item {
                Text(
                    text = "No back matter divisions added yet (e.g. Bibliography, Glossary, Appendix). Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(sections, key = { it.id }) { section ->
                SectionCard(
                    section = section,
                    onOpen = { onOpenSection(section.id) },
                    onDelete = { onDeleteSection(section.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun LeafFlowTab(
    leaves: List<CalculatedLeaf>,
    onOpenSection: (Long?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Complete Physical Leaf Map (Verso / Recto Pairs)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chicago Manual of Style requires major divisions to open on Recto (odd page). Blank verso leaves are placed automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(leaves, key = { it.leafIndex }) { leaf ->
            val isBlank = leaf.displayType == LeafDisplayType.BLANK_INTENTIONAL
            val isOpener = leaf.isOpener

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSection(leaf.sectionId) }
                    .testTag("leaf_item_${leaf.leafIndex}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBlank) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    } else if (leaf.side == LeafSide.RECTO) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                border = BorderStroke(
                    width = if (isOpener) 1.5.dp else 1.dp,
                    color = if (isOpener) BookGoldDark else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leaf number & side badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp)
                    ) {
                        Text(
                            text = "LEAF ${leaf.leafIndex}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LeafBadge(
                            side = leaf.side,
                            pageNumber = leaf.pageNumberDisplay,
                            isBlank = isBlank
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Matter
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = leaf.sectionTitle,
                                fontWeight = if (isOpener) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp
                            )
                            if (isOpener) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = BookGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text(
                                        text = "OPENER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BookGoldDark,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (leaf.contentSnippet.isNotBlank()) {
                            Text(
                                text = leaf.contentSnippet.take(90) + if (leaf.contentSnippet.length > 90) "..." else "",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isBlank) {
                            Text(
                                text = "Intentionally blank leaf to start next division on Recto.",
                                fontSize = 11.sp,
                                color = BlankLeafColor,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        if (leaf.runningHeadVerso.isNotBlank() || leaf.runningHeadRecto.isNotBlank()) {
                            Text(
                                text = if (leaf.side == LeafSide.VERSO) "Running Head: ${leaf.runningHeadVerso}" else "Running Head: ${leaf.runningHeadRecto}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun SectionCard(
    section: SectionEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("section_card_${section.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(status = section.status)
                    LeafBadge(
                        side = if (section.startOnRecto) LeafSide.RECTO else LeafSide.VERSO
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                if (section.subtitle.isNotBlank()) {
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${section.wordCount} words • Assigned to ${section.assignedAuthor}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionDialog(
    defaultMatter: MatterType,
    onDismiss: () -> Unit,
    onAdd: (MatterType, SectionType, String, String) -> Unit
) {
    var matterType by remember { mutableStateOf(defaultMatter) }
    var selectedType by remember {
        mutableStateOf(
            when (defaultMatter) {
                MatterType.FRONT_MATTER -> SectionType.PREFACE
                MatterType.BACK_MATTER -> SectionType.BIBLIOGRAPHY
                else -> SectionType.CHAPTER
            }
        )
    }
    var title by remember { mutableStateOf(selectedType.defaultTitle) }
    var subtitle by remember { mutableStateOf("") }
    var expandedTypeMenu by remember { mutableStateOf(false) }

    val availableTypes = SectionType.values().filter { it.matterType == matterType }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("dialog_add_section")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Manuscript Division",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Matter Type selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MatterType.values().forEach { m ->
                        val isSel = matterType == m
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    matterType = m
                                    selectedType = SectionType.values().first { it.matterType == m }
                                    title = selectedType.defaultTitle
                                }
                        ) {
                            Text(
                                text = m.displayName.split(" ").first(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section Type dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTypeMenu,
                    onExpandedChange = { expandedTypeMenu = !expandedTypeMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.defaultTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Division Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeMenu,
                        onDismissRequest = { expandedTypeMenu = false }
                    ) {
                        availableTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.defaultTitle) },
                                onClick = {
                                    selectedType = t
                                    title = t.defaultTitle
                                    expandedTypeMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Division Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle / Epigraph (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAdd(matterType, selectedType, title.trim(), subtitle.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManuscriptSettingsDialog(
    manuscript: ManuscriptEntity,
    onDismiss: () -> Unit,
    onSave: (ManuscriptEntity) -> Unit
) {
    var title by remember { mutableStateOf(manuscript.title) }
    var subtitle by remember { mutableStateOf(manuscript.subtitle) }
    var authorName by remember { mutableStateOf(manuscript.authorName) }
    var authorPenName by remember { mutableStateOf(manuscript.authorPenName) }
    var publisher by remember { mutableStateOf(manuscript.publisher) }
    var edition by remember { mutableStateOf(manuscript.edition) }
    var year by remember { mutableStateOf(manuscript.year) }
    var isbn by remember { mutableStateOf(manuscript.isbn) }
    var targetPageSize by remember { mutableStateOf(manuscript.targetPageSize) }
    var copyrightText by remember { mutableStateOf(manuscript.effectiveCopyrightText) }
    var dedication by remember { mutableStateOf(manuscript.dedication) }
    var epigraphText by remember { mutableStateOf(manuscript.epigraphText) }
    var epigraphAuthor by remember { mutableStateOf(manuscript.epigraphAuthor) }
    var trimSizeExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text(
                        text = "Manuscript Colophon & Metadata",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Author Legal Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = authorPenName,
                        onValueChange = { authorPenName = it },
                        label = { Text("Pen Name / Author Byline (Optional)") },
                        placeholder = { Text("e.g. J. D. Cross") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = publisher,
                        onValueChange = { publisher = it },
                        label = { Text("Publisher Imprint") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = edition,
                            onValueChange = { edition = it },
                            label = { Text("Edition") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = isbn,
                        onValueChange = { isbn = it },
                        label = { Text("ISBN / Identifier") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Book Trim Size Selector
                item {
                    ExposedDropdownMenuBox(
                        expanded = trimSizeExpanded,
                        onExpandedChange = { trimSizeExpanded = !trimSizeExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetPageSize,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Book Trim Size & Dimensions") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trimSizeExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = trimSizeExpanded,
                            onDismissRequest = { trimSizeExpanded = false }
                        ) {
                            BookTrimSize.values().forEach { trim ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(trim.displayName, fontWeight = FontWeight.Bold)
                                            Text(trim.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        targetPageSize = trim.displayName
                                        trimSizeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Dynamic Copyright Editor with Reset Button
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Copyright Notice (Verso p. iv)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val byline = if (authorPenName.isNotBlank()) authorPenName.trim() else authorName.ifBlank { "Author" }
                                    val yr = year.ifBlank { "2026" }
                                    val pub = publisher.ifBlank { "Bwriter Editions" }
                                    copyrightText = "Copyright © $yr by $byline.\nAll rights reserved under International and Pan-American Copyright Conventions.\nPublished by $pub in accordance with The Chicago Manual of Style.\nPrinted in the United States of America."
                                }
                            ) {
                                Text("Regenerate", fontSize = 12.sp)
                            }
                        }
                        OutlinedTextField(
                            value = copyrightText,
                            onValueChange = { copyrightText = it },
                            label = { Text("Copyright Notice") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = dedication,
                        onValueChange = { dedication = it },
                        label = { Text("Dedication") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = epigraphText,
                        onValueChange = { epigraphText = it },
                        label = { Text("Epigraph Quotation") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = epigraphAuthor,
                        onValueChange = { epigraphAuthor = it },
                        label = { Text("Epigraph Citation / Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    manuscript.copy(
                                        title = title.trim(),
                                        subtitle = subtitle.trim(),
                                        authorName = authorName.trim(),
                                        authorPenName = authorPenName.trim(),
                                        publisher = publisher.trim(),
                                        edition = edition.trim(),
                                        year = year.trim(),
                                        isbn = isbn.trim(),
                                        targetPageSize = targetPageSize,
                                        copyrightText = copyrightText.trim(),
                                        dedication = dedication.trim(),
                                        epigraphText = epigraphText.trim(),
                                        epigraphAuthor = epigraphAuthor.trim()
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
