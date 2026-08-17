package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import com.example.model.WorkRole
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GoogleDriveSyncService
import com.example.model.AiTokenTransaction
import com.example.model.GlobalBookIndexEntry
import com.example.model.PaidMemberTelemetry
import com.example.model.ServerlessMailMessage
import com.example.model.SuspendedUserEntry
import com.example.model.UserProfile
import com.example.ui.theme.BookGold
import com.example.ui.theme.BookGoldDark
import com.example.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGodModeDashboardScreen(
    currentUser: UserProfile,
    globalIndex: List<GlobalBookIndexEntry>,
    suspendedUsers: List<SuspendedUserEntry>,
    paidSubscribers: List<PaidMemberTelemetry> = emptyList(),
    tokenTransactions: List<AiTokenTransaction> = emptyList(),
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSuspendUser: (targetEmail: String, reason: String) -> Unit,
    onUnsuspendUser: (targetEmail: String) -> Unit,
    onRevokeManuscript: (fileId: String) -> Unit,
    onSendServerlessMail: (ServerlessMailMessage) -> Unit,
    onGrantBonusCredits: (targetEmail: String, bonusCredits: Int) -> Unit = { _, _ -> },
    onUpdateProfile: (name: String, penName: String, email: String, role: WorkRole, organization: String, cmosEdition: String) -> Unit = { _, _, _, _, _, _ -> },
    onSignOut: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Master Book Index", "Paid Subscribers & Tokens", "Author Roster & Governance", "Mailbox Dispatcher", "Drive Storage Health")

    // Modals
    var showProfileDialog by remember { mutableStateOf(false) }
    var inspectingEntry by remember { mutableStateOf<GlobalBookIndexEntry?>(null) }
    var suspendingTargetUser by remember { mutableStateOf<String?>(null) }
    var suspensionReasonInput by remember { mutableStateOf("Violation of editorial guidelines or unapproved distribution.") }
    var grantingCreditsTargetUser by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC5A059).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BookGold),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = BookGoldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Master Shared Drive Admin",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "God-Mode Governance • ${GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL}",
                                fontSize = 10.5.sp,
                                color = BookGoldLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_admin_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF3EFE6))
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFF1E1E28),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, BookGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { showProfileDialog = true }
                            .padding(end = 4.dp)
                            .testTag("btn_admin_profile_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = currentUser.displayName,
                                fontSize = 11.5.sp,
                                color = Color(0xFFF3EFE6),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile & Pen Name",
                                modifier = Modifier.size(12.dp),
                                tint = BookGoldLight
                            )
                        }
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("btn_admin_refresh")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BookGoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E0E14),
                    titleContentColor = Color(0xFFF3EFE6)
                )
            )
        },
        containerColor = Color(0xFF0A0A0E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Strip
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF14141C),
                contentColor = BookGoldLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BookGold
                    )
                },
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFFF3EFE6) else Color(0xFF9E9EAA)
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> MasterBookIndexTab(
                    entries = globalIndex,
                    currentUser = currentUser,
                    onInspect = { inspectingEntry = it },
                    onRevoke = onRevokeManuscript
                )
                1 -> PaidSubscribersAndTokensTab(
                    subscribers = paidSubscribers,
                    transactions = tokenTransactions,
                    currentUser = currentUser,
                    onGrantBonus = { email -> grantingCreditsTargetUser = email }
                )
                2 -> AuthorRosterTab(
                    globalIndex = globalIndex,
                    suspendedUsers = suspendedUsers,
                    currentUser = currentUser,
                    onOpenSuspendModal = { email -> suspendingTargetUser = email },
                    onUnsuspend = onUnsuspendUser
                )
                3 -> MailboxDispatcherTab(
                    globalIndex = globalIndex,
                    currentUser = currentUser,
                    onSendMessage = onSendServerlessMail
                )
                4 -> DriveHealthTab(
                    globalIndex = globalIndex,
                    suspendedUsers = suspendedUsers
                )
            }
        }
    }

    // Grant Bonus Credits Dialog
    grantingCreditsTargetUser?.let { targetUserEmail ->
        var bonusInput by remember { mutableStateOf("100") }
        Dialog(onDismissRequest = { grantingCreditsTargetUser = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, BookGold)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Grant Bonus AI Credits",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        color = Color(0xFFF3EFE6)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Troubleshoot & top up generation balance for $targetUserEmail",
                        fontSize = 11.5.sp,
                        color = BookGoldLight
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = bonusInput,
                        onValueChange = { bonusInput = it },
                        label = { Text("Credits to Add") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BookGold,
                            unfocusedBorderColor = Color(0xFF3A3A4C),
                            focusedContainerColor = Color(0xFF1B1B24),
                            unfocusedContainerColor = Color(0xFF161620)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { grantingCreditsTargetUser = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = bonusInput.toIntOrNull() ?: 100
                                onGrantBonusCredits(targetUserEmail, amount)
                                grantingCreditsTargetUser = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark)
                        ) {
                            Text("Grant Credits", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Inspect JSON Dialog
    inspectingEntry?.let { entry ->
        Dialog(onDismissRequest = { inspectingEntry = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, BookGoldDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = BookGoldLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remote File Inspection",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFFF3EFE6)
                            )
                        }
                        IconButton(onClick = { inspectingEntry = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Metadata & Permissions Schema for '${entry.title}'",
                        fontSize = 11.5.sp,
                        color = BookGoldLight
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val rawJson = """
{
  "fileId": "${entry.fileId}",
  "manuscriptId": ${entry.manuscriptId},
  "title": "${entry.title}",
  "subtitle": "${entry.subtitle}",
  "authorName": "${entry.authorName}",
  "authorEmail": "${entry.authorEmail}",
  "workType": "${entry.workType}",
  "wordCount": ${entry.wordCount},
  "totalLeaves": ${entry.totalLeaves},
  "lastSynced": ${entry.lastSyncedTimestamp},
  "isPublicInCommunity": ${entry.isPublicInCommunity},
  "sharedWithSuperuser": ${entry.sharedWithEditorInChief},
  "driveUrl": "${entry.driveFileUrl}"
}
                    """.trimIndent()

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0A0A0E),
                        border = BorderStroke(1.dp, Color(0xFF2C2C38))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = rawJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inspectingEntry = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                onRevokeManuscript(entry.fileId)
                                inspectingEntry = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revoke Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Suspension Prompt Dialog
    suspendingTargetUser?.let { targetEmail ->
        Dialog(onDismissRequest = { suspendingTargetUser = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF14141C),
                border = BorderStroke(1.dp, Color(0xFFE53935))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enforce Account Suspension",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            color = Color(0xFFF3EFE6)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Target Account: $targetEmail",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFFE57373)
                    )
                    Text(
                        text = "This will write to suspended_users.json in the Master Shared Drive. The client app will trigger a hard kill-switch lock on next ping.",
                        fontSize = 11.sp,
                        color = Color(0xFFB0A89C),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = suspensionReasonInput,
                        onValueChange = { suspensionReasonInput = it },
                        label = { Text("Reason for Administrative Suspension", color = BookGoldLight) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFF444455),
                            focusedContainerColor = Color(0xFF1B1B24),
                            unfocusedContainerColor = Color(0xFF16161E)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { suspendingTargetUser = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                onSuspendUser(targetEmail, suspensionReasonInput)
                                suspendingTargetUser = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f).testTag("btn_confirm_suspension")
                        ) {
                            Text("Enforce Lock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Superuser / Author Profile Dialog
    if (showProfileDialog) {
        AuthorProfileDialog(
            currentUser = currentUser,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, penName, email, role, org, cmos ->
                onUpdateProfile(name, penName, email, role, org, cmos)
            },
            onSignOut = onSignOut
        )
    }
}

@Composable
fun MasterBookIndexTab(
    entries: List<GlobalBookIndexEntry>,
    currentUser: UserProfile,
    onInspect: (GlobalBookIndexEntry) -> Unit,
    onRevoke: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Master Global Book Registry",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = Color(0xFFF3EFE6)
                )
                Text(
                    text = "${entries.size} total synced works (Public & Private)",
                    fontSize = 11.sp,
                    color = Color(0xFFB0A89C)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No manuscripts currently registered in the Master Shared Drive.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_book_card_${entry.fileId}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                        border = BorderStroke(1.dp, if (entry.isPublicInCommunity) Color(0xFF2C2C38) else BookGoldDark.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                    if (entry.subtitle.isNotBlank()) {
                                        Text(
                                            text = entry.subtitle,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFB0A89C)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (entry.isPublicInCommunity) Color(0xFF1B3B2B) else Color(0xFF3E2723),
                                    border = BorderStroke(1.dp, if (entry.isPublicInCommunity) Color(0xFF4CAF50) else BookGold)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (entry.isPublicInCommunity) Icons.Default.Public else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (entry.isPublicInCommunity) Color(0xFF81C784) else BookGoldLight,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (entry.isPublicInCommunity) "Community Public" else "Private to Superuser",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entry.isPublicInCommunity) Color(0xFF81C784) else BookGoldLight
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Metadata Row
                            val displayAuthor = if (entry.authorEmail.equals(currentUser.email, ignoreCase = true)) {
                                currentUser.displayName
                            } else {
                                entry.authorName
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Author: $displayAuthor", fontSize = 11.5.sp, color = Color(0xFFE2DDD5))
                                Text("Email: ${entry.authorEmail}", fontSize = 11.5.sp, color = BookGoldLight)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Words: ${entry.wordCount}", fontSize = 11.sp, color = Color(0xFFB0A89C))
                                Text("Est. Leaves: ${entry.totalLeaves}", fontSize = 11.sp, color = Color(0xFFB0A89C))
                                Text("Synced: ${dateFormat.format(Date(entry.lastSyncedTimestamp))}", fontSize = 11.sp, color = Color(0xFFB0A89C))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onInspect(entry.copy(authorName = displayAuthor)) },
                                    border = BorderStroke(1.dp, BookGoldDark),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp), tint = BookGoldLight)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Inspect JSON", fontSize = 10.5.sp, color = BookGoldLight)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onRevoke(entry.fileId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381414)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFE57373))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Revoke Access", fontSize = 10.5.sp, color = Color(0xFFE57373))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorRosterTab(
    globalIndex: List<GlobalBookIndexEntry>,
    suspendedUsers: List<SuspendedUserEntry>,
    currentUser: UserProfile,
    onOpenSuspendModal: (String) -> Unit,
    onUnsuspend: (String) -> Unit
) {
    val allAuthorEmails = (globalIndex.map { it.authorEmail } + suspendedUsers.map { it.email } + listOf(currentUser.email)).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            text = "Author Ecosystem & Account Governance",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = Color(0xFFF3EFE6)
        )
        Text(
            text = "Enforce kill-switch suspensions or manage access rights.",
            fontSize = 11.sp,
            color = Color(0xFFB0A89C)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allAuthorEmails) { email ->
                val isCurrentUser = email.equals(currentUser.email, ignoreCase = true)
                val isSuspended = suspendedUsers.any { it.email.equals(email, ignoreCase = true) && it.isLockedOut }
                val suspensionRecord = suspendedUsers.firstOrNull { it.email.equals(email, ignoreCase = true) }
                val authorBooks = globalIndex.filter { it.authorEmail.equals(email, ignoreCase = true) }
                val authorName = if (isCurrentUser) {
                    currentUser.displayName
                } else {
                    authorBooks.firstOrNull()?.authorName ?: email.substringBefore("@")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, if (isSuspended) Color(0xFFE53935) else if (isCurrentUser) BookGoldDark else Color(0xFF2C2C38))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFF3EFE6)
                                    )
                                    if (isCurrentUser) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BookGoldDark.copy(alpha = 0.3f),
                                            border = BorderStroke(0.5.dp, BookGold)
                                        ) {
                                            Text(
                                                text = if (currentUser.penName.isNotBlank()) "Pen Name: ${currentUser.penName}" else "Author Profile",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BookGoldLight,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = email,
                                    fontSize = 11.sp,
                                    color = BookGoldLight
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSuspended) Color(0xFF381414) else Color(0xFF1B3B2B),
                                border = BorderStroke(1.dp, if (isSuspended) Color(0xFFE53935) else Color(0xFF4CAF50))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isSuspended) Icons.Default.Block else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSuspended) Color(0xFFE57373) else Color(0xFF81C784),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSuspended) "SUSPENDED" else "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSuspended) Color(0xFFE57373) else Color(0xFF81C784)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Books Synced: ${authorBooks.size} (${authorBooks.sumOf { it.wordCount }} total words)",
                            fontSize = 11.sp,
                            color = Color(0xFFB0A89C)
                        )

                        if (isSuspended && suspensionRecord != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Suspension Reason: ${suspensionRecord.reason}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFE57373),
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isSuspended) {
                                Button(
                                    onClick = { onUnsuspend(email) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onOpenSuspendModal(email) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381414)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFE57373))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Suspend User", fontSize = 11.sp, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxDispatcherTab(
    globalIndex: List<GlobalBookIndexEntry>,
    currentUser: UserProfile,
    onSendMessage: (ServerlessMailMessage) -> Unit
) {
    val authorEmails = listOf("ALL_AUTHORS") + (globalIndex.map { it.authorEmail } + listOf(currentUser.email)).distinct()
    var selectedRecipient by remember { mutableStateOf(authorEmails.firstOrNull() ?: "ALL_AUTHORS") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EDITORIAL_REVISION") }
    var isSentFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Serverless Mailbox Dispatcher",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = Color(0xFFF3EFE6)
        )
        Text(
            text = "Dispatches direct messages or broadcasts to /Mailboxes/<userEmail>/messages.json in the Master Shared Drive.",
            fontSize = 11.sp,
            color = Color(0xFFB0A89C)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Recipient selector
        Text("Recipient Channel:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BookGoldLight)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            authorEmails.forEach { email ->
                val chipLabel = when {
                    email == "ALL_AUTHORS" -> "📢 Broadcast (All Authors)"
                    email.equals(currentUser.email, ignoreCase = true) -> "👤 ${currentUser.displayName} (You)"
                    else -> email
                }
                FilterChip(
                    selected = selectedRecipient == email,
                    onClick = { selectedRecipient = email },
                    label = { Text(chipLabel, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BookGoldDark,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1B1B24),
                        labelColor = Color(0xFFD0CAC0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Category
        Text("Message Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BookGoldLight)
        val categories = listOf(
            "EDITORIAL_REVISION" to "Editorial Revision",
            "GOVERNANCE_ALERT" to "Governance Alert",
            "DIRECT_MESSAGE" to "Direct Message"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { (catKey, catLabel) ->
                FilterChip(
                    selected = selectedType == catKey,
                    onClick = { selectedType = catKey },
                    label = { Text(catLabel, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BookGoldDark,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1B1B24),
                        labelColor = Color(0xFFD0CAC0)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subject Field
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().testTag("input_admin_mail_subject"),
            textStyle = TextStyle(fontSize = 12.5.sp, color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF3B3B4C),
                focusedContainerColor = Color(0xFF161620),
                unfocusedContainerColor = Color(0xFF121218)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body Field
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message Body & Editorial Directives", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth().height(140.dp).testTag("input_admin_mail_body"),
            textStyle = TextStyle(fontSize = 12.5.sp, color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGold,
                unfocusedBorderColor = Color(0xFF3B3B4C),
                focusedContainerColor = Color(0xFF161620),
                unfocusedContainerColor = Color(0xFF121218)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (subject.isNotBlank() && body.isNotBlank()) {
                    onSendMessage(
                        ServerlessMailMessage(
                            recipientEmail = selectedRecipient,
                            senderEmail = GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL,
                            senderName = "Editor in Chief",
                            subject = subject.trim(),
                            body = body.trim(),
                            messageType = selectedType
                        )
                    )
                    subject = ""
                    body = ""
                    isSentFeedback = true
                }
            },
            enabled = subject.isNotBlank() && body.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_dispatch_admin_mail")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Dispatch Message to Shared Drive Mailbox", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (isSentFeedback) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ Dispatched message successfully to $selectedRecipient.",
                color = Color(0xFF81C784),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DriveHealthTab(
    globalIndex: List<GlobalBookIndexEntry>,
    suspendedUsers: List<SuspendedUserEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Master Shared Drive Structure",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = Color(0xFFF3EFE6)
        )
        Text(
            text = "Serverless Google Drive Registry & Architecture Status",
            fontSize = 11.sp,
            color = Color(0xFFB0A89C)
        )

        Spacer(modifier = Modifier.height(14.dp))

        val driveNodes = listOf(
            Triple("Global_Book_Index.json", "${globalIndex.size} manuscripts registered", true),
            Triple("suspended_users.json", "${suspendedUsers.size} accounts blocked", true),
            Triple("Mailboxes/ Directory", "Decentralized inbox queue", true),
            Triple("Auto-Share Hook (${GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL})", "Writer/Reader permissions active", true)
        )

        driveNodes.forEach { (nodeName, detail, ok) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                border = BorderStroke(1.dp, Color(0xFF2C2C38))
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
                            if (nodeName.endsWith(".json")) Icons.Default.Storage else Icons.Default.FolderShared,
                            contentDescription = null,
                            tint = BookGoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = nodeName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFFF3EFE6))
                            Text(text = detail, fontSize = 10.5.sp, color = Color(0xFFB0A89C))
                        }
                    }

                    Icon(Icons.Default.CloudDone, contentDescription = "Online", tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PaidSubscribersAndTokensTab(
    subscribers: List<PaidMemberTelemetry>,
    transactions: List<AiTokenTransaction>,
    currentUser: UserProfile,
    onGrantBonus: (String) -> Unit
) {
    val totalTokensBurned = subscribers.sumOf { it.totalTokensUsed }
    val totalRuns = subscribers.sumOf { it.totalGenerationsCount }
    val activeCount = subscribers.count { it.status == "ACTIVE" || it.status == "UNLIMITED_SUPERUSER" }
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // High-level telemetry cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161622),
                border = BorderStroke(1.dp, BookGoldDark),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("PAID / MEMBERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("$activeCount / ${subscribers.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3EFE6))
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161622),
                border = BorderStroke(1.dp, Color(0xFF323244)),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("TOTAL CMOS TOKENS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("%,d".format(totalTokensBurned), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BookGoldLight)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161622),
                border = BorderStroke(1.dp, Color(0xFF323244)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("RUNS PRESSED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("$totalRuns Runs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inner Tab Strip: Member Roster vs Token Ledger Audit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Subscribers Roster (${subscribers.size})", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BookGoldDark,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Live Token Stream (${transactions.size})", fontSize = 11.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BookGoldDark,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subTab == 0) {
            if (subscribers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subscriber telemetry logged yet.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subscribers) { sub ->
                        val isCurrentUser = sub.userEmail.equals(currentUser.email, ignoreCase = true)
                        val subscriberName = if (isCurrentUser) currentUser.displayName else sub.displayName

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                            border = BorderStroke(1.dp, if (isCurrentUser) BookGoldDark else Color(0xFF2C2C3C))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = subscriberName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Color(0xFFF3EFE6)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (sub.userEmail.equals("real.artistry@gmail.com", ignoreCase = true)) Color(0xFFFFD700).copy(alpha = 0.2f) else BookGoldDark.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = sub.planTitle,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sub.userEmail.equals("real.artistry@gmail.com", ignoreCase = true)) Color(0xFFFFD700) else BookGoldLight,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(text = sub.userEmail, fontSize = 10.5.sp, color = Color.Gray)
                                    }

                                    Button(
                                        onClick = { onGrantBonus(sub.userEmail) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222230)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("+ Grant Credits", fontSize = 10.5.sp, color = BookGoldLight)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF22222E)))
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Credits: ${if (sub.userEmail.equals("real.artistry@gmail.com", ignoreCase = true)) "∞ Unlimited" else "${sub.creditsRemaining} remaining"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BookGoldLight
                                    )
                                    Text(
                                        text = "Tokens: ${"%,d".format(sub.totalTokensUsed)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC8C2B7)
                                    )
                                    Text(
                                        text = "Runs: ${sub.totalGenerationsCount}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF81C784)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Live Token Transactions
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No AI token transactions recorded yet.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                val dateFmt = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(transactions) { tx ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF14141C),
                            border = BorderStroke(0.5.dp, Color(0xFF262634)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.userEmail} • ${tx.sectionTitle}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF3EFE6)
                                    )
                                    Text(
                                        text = "${dateFmt.format(Date(tx.timestamp))} • ${tx.modelUsed}",
                                        fontSize = 9.5.sp,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${tx.totalTokens} tokens",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BookGoldLight
                                    )
                                    Text(
                                        text = "-${tx.creditsDeducted} credit",
                                        fontSize = 9.5.sp,
                                        color = if (tx.creditsDeducted > 0) Color(0xFFFF8A80) else Color.LightGray
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

