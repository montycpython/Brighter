package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ManuscriptEntity
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.model.WorkType
import com.example.ui.components.RoleBadge
import com.example.ui.components.WorkTypeIcon
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import com.example.ui.theme.CrimsonSeal
import com.example.ui.theme.ForestCloth
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManuscriptListScreen(
    manuscripts: List<ManuscriptEntity>,
    currentUser: UserProfile,
    selectedFilter: WorkType?,
    onSelectFilter: (WorkType?) -> Unit,
    onOpenManuscript: (Long) -> Unit,
    onOpenReader: (Long) -> Unit,
    onOpenExport: (Long) -> Unit,
    onOpenRulebook: () -> Unit,
    onSwitchRole: (WorkRole) -> Unit,
    onOpenAuthScreen: () -> Unit,
    onDeleteManuscript: (Long) -> Unit,
    onCreateNewWorkClick: () -> Unit,
    onUpdateProfile: (name: String, penName: String, email: String, role: WorkRole, organization: String, cmosEdition: String) -> Unit = { _, _, _, _, _, _ -> },
    onOpenCommunity: () -> Unit = {},
    onOpenMailbox: () -> Unit = {},
    onOpenAdminDashboard: () -> Unit = {},
    onSyncManuscript: (ManuscriptEntity) -> Unit = {},
    onOpenUserAgreement: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onRestoreFromDrive: () -> Unit = {},
    unreadMailCount: Int = 0
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var manuscriptToDelete by remember { mutableStateOf<ManuscriptEntity?>(null) }
    val isEditorInChief = currentUser.email.equals(com.example.data.GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, BookGoldDark),
                            color = Color(0xFF1E1A17),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_bwriter_emblem_1786987156557),
                                    contentDescription = "Bwriter Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bwriter",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chicago Manual of Style Studio",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Restore from Drive Action
                    IconButton(
                        onClick = onRestoreFromDrive,
                        modifier = Modifier.testTag("btn_top_restore_drive")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore from Google Drive",
                            tint = BookGoldDark
                        )
                    }

                    // AI Studio Subscription / Credits Wallet
                    IconButton(
                        onClick = onOpenSubscription,
                        modifier = Modifier.testTag("btn_top_subscription_wallet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Studio Credits Pass",
                            tint = BookGoldLight
                        )
                    }

                    // Community Book Directory
                    IconButton(
                        onClick = onOpenCommunity,
                        modifier = Modifier.testTag("btn_top_community")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Community Directory",
                            tint = BookGoldDark
                        )
                    }

                    // Mailbox with unread badge
                    IconButton(
                        onClick = onOpenMailbox,
                        modifier = Modifier.testTag("btn_top_mailbox")
                    ) {
                        if (unreadMailCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFC62828),
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadMailCount", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = "Editorial Mailbox",
                                    tint = BookGoldDark
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = "Editorial Mailbox",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Master Admin God-Mode Button (Editor in Chief)
                    if (isEditorInChief) {
                        IconButton(
                            onClick = onOpenAdminDashboard,
                            modifier = Modifier.testTag("btn_top_admin_god_mode")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Master Shared Drive Admin",
                                tint = Color(0xFFE5A93C)
                            )
                        }
                    }

                    // CMOS Rulebook button
                    IconButton(
                        onClick = onOpenRulebook,
                        modifier = Modifier.testTag("btn_top_rulebook")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Rule,
                            contentDescription = "Chicago Manual of Style Guide",
                            tint = BookGoldDark
                        )
                    }

                    // User role / Google Profile badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { showProfileDialog = true }
                            .padding(end = 8.dp)
                            .testTag("btn_user_profile_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            RoleBadge(role = currentUser.role)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUser.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile & Pen Name",
                                modifier = Modifier.size(14.dp),
                                tint = BookGoldDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewWorkClick,
                containerColor = BookGoldDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_create_work")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Work")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Work", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Hero Banner / Desk Illustration
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = InkNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_banner_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chicago Manual of Style",
                                color = BookGoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recto & Verso Leaf Craftsmanship",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Every work scaffolds front matter, main body text, and back matter in publication-grade leaf spreads with Roman and Arabic foliation.",
                                color = Color(0xFFD3D0C9),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Filter Chips (All, Novel, Biography, Documentary, Manual)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text("All Works (${manuscripts.size})") },
                        modifier = Modifier.testTag("filter_all")
                    )

                    WorkType.values().forEach { type ->
                        val count = manuscripts.count { it.workType == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { onSelectFilter(if (selectedFilter == type) null else type) },
                            label = { Text("${type.pluralName} ($count)") },
                            leadingIcon = {
                                WorkTypeIcon(workType = type, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("filter_${type.name}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Manuscripts List
            if (manuscripts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, BookGoldDark.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BookGoldDark.copy(alpha = 0.15f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = BookGoldDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Welcome to Bwriter, ${currentUser.effectivePenName}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your local workspace is clean. Author your first work in Chicago format, read published titles from the Global Registry, or restore your previous cloud library from Google Drive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Button(
                                    onClick = onCreateNewWorkClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_empty_create_work")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create New Work", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onOpenCommunity,
                                    border = BorderStroke(1.dp, BookGoldDark),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BookGoldDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_empty_browse_registry")
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse Global Registry", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRestoreFromDrive,
                                    border = BorderStroke(1.dp, Color(0xFF1976D2)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_empty_restore_drive")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore from Google Drive", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(manuscripts, key = { it.id }) { manuscript ->
                    ManuscriptCard(
                        manuscript = manuscript,
                        onOpen = { onOpenManuscript(manuscript.id) },
                        onRead = { onOpenReader(manuscript.id) },
                        onExport = { onOpenExport(manuscript.id) },
                        onSync = { onSyncManuscript(manuscript) },
                        onDelete = { manuscriptToDelete = manuscript }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Author Profile & Pen Name Dialog
    if (showProfileDialog) {
        AuthorProfileDialog(
            currentUser = currentUser,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, penName, email, role, org, cmos ->
                onUpdateProfile(name, penName, email, role, org, cmos)
            },
            onOpenSubscription = onOpenSubscription,
            onOpenUserAgreement = onOpenUserAgreement,
            onOpenAuthScreen = onOpenAuthScreen,
            onSignOut = onSignOut
        )
    }

    // Delete confirmation
    if (manuscriptToDelete != null) {
        AlertDialog(
            onDismissRequest = { manuscriptToDelete = null },
            title = { Text("Delete Manuscript?") },
            text = {
                Text("Are you sure you want to delete “${manuscriptToDelete?.title}”? All front matter, chapters, back matter, and editorial comments will be permanently removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        manuscriptToDelete?.id?.let { onDeleteManuscript(it) }
                        manuscriptToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { manuscriptToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ManuscriptCard(
    manuscript: ManuscriptEntity,
    onOpen: () -> Unit,
    onRead: () -> Unit,
    onExport: () -> Unit,
    onSync: () -> Unit = {},
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("manuscript_card_${manuscript.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: WorkType and Edition/Year
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = when (manuscript.workType) {
                        WorkType.NOVEL -> Color(0xFF28547C).copy(alpha = 0.12f)
                        WorkType.BIOGRAPHY -> Color(0xFFC5A059).copy(alpha = 0.18f)
                        WorkType.DOCUMENTARY -> Color(0xFF2D4A3E).copy(alpha = 0.14f)
                        WorkType.MANUAL -> Color(0xFF9E2A2B).copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        WorkTypeIcon(
                            workType = manuscript.workType,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = manuscript.workType.displayName.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Manuscript Status Badge (Under Review, Polished, Draft)
                Surface(
                    color = when (manuscript.manuscriptStatus.uppercase()) {
                        "UNDER_REVIEW" -> Color(0xFFFFEBEE)
                        "POLISHED" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFF8E1)
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        0.8.dp,
                        when (manuscript.manuscriptStatus.uppercase()) {
                            "UNDER_REVIEW" -> Color(0xFFD32F2F)
                            "POLISHED" -> Color(0xFF2E7D32)
                            else -> Color(0xFFFFB300)
                        }
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = when (manuscript.manuscriptStatus.uppercase()) {
                            "UNDER_REVIEW" -> "UNDER REVIEW"
                            "POLISHED" -> "POLISHED"
                            else -> "DRAFT"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (manuscript.manuscriptStatus.uppercase()) {
                            "UNDER_REVIEW" -> Color(0xFFD32F2F)
                            "POLISHED" -> Color(0xFF2E7D32)
                            else -> Color(0xFFE65100)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${manuscript.edition} • ${manuscript.year}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // Sync button
                IconButton(
                    onClick = onSync,
                    modifier = Modifier.size(28.dp).testTag("btn_sync_card_${manuscript.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Sync to Google Drive",
                        tint = BookGoldDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Subtitle in Serif
            Text(
                text = manuscript.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (manuscript.subtitle.isNotBlank()) {
                Text(
                    text = manuscript.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Author and Publisher
            Text(
                text = "By ${manuscript.effectiveAuthorByline} • ${manuscript.publisher}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer with quick action buttons: Draft / Leaves / PDF
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Draft / Edit
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Draft",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Draft & Polish",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Two-page Leaf Reader
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onRead)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Leaf Reader",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Leaf Spreads",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // PDF Export
                Surface(
                    color = BookGoldDark.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExport)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = BookGoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CMOS PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BookGoldDark
                        )
                    }
                }
            }
        }
    }
}
