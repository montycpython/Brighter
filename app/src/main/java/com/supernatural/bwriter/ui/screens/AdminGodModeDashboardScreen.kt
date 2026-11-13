package com.supernatural.bwriter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import com.supernatural.bwriter.data.CreditIssuance
import com.supernatural.bwriter.data.EditorAssignment
import com.supernatural.bwriter.data.PoolEntry
import com.supernatural.bwriter.data.PoolKinds
import com.supernatural.bwriter.data.PoolTotals
import com.supernatural.bwriter.data.PayoutRequest
import com.supernatural.bwriter.data.PayoutStatuses
import com.supernatural.bwriter.data.StudioSystemConfig
import com.supernatural.bwriter.data.UnitChain
import com.supernatural.bwriter.data.UnitGrant
import com.supernatural.bwriter.data.UnitRoles
import com.supernatural.bwriter.data.SiteUser
import com.supernatural.bwriter.model.WorkRole
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
import com.supernatural.bwriter.data.FirestoreNetworkService
import com.supernatural.bwriter.model.GlobalBookIndexEntry
import com.supernatural.bwriter.model.ServerlessMailMessage
import com.supernatural.bwriter.model.SuspendedUserEntry
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.ui.viewmodel.AdminUiState
import com.supernatural.bwriter.ui.theme.BookGold
import com.supernatural.bwriter.ui.theme.BookGoldDark
import com.supernatural.bwriter.ui.theme.BookGoldLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGodModeDashboardScreen(
    state: AdminUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSuspendUser: (targetEmail: String, reason: String) -> Unit,
    onUnsuspendUser: (targetEmail: String) -> Unit,
    onRevokeManuscript: (fileId: String) -> Unit,
    onApproveBook: (fileId: String) -> Unit = {},
    onRejectBook: (fileId: String) -> Unit = {},
    onDecideEditorAssignment: (EditorAssignment, Boolean) -> Unit = { _, _ -> },
    onAppointEditor: (fileId: String, bookAuthorEmail: String, editorEmail: String, editorAddress: String) -> Unit = { _, _, _, _ -> },
    onSetRegent: (String) -> Unit = {},
    onClearRegent: () -> Unit = {},
    onGiveCredit: (String) -> Unit = {},
    onSetPayoutRequestStatus: (PayoutRequest, String) -> Unit = { _, _ -> },
    onVestCurrentSeason: () -> Unit = {},
    onRecordPoolEntry: (String, Long, String) -> Unit = { _, _, _ -> },
    onUpdateSystemConfig: (String, String) -> Unit = { _, _ -> },
    onSendServerlessMail: (ServerlessMailMessage) -> Unit,
    onUpdateProfile: (name: String, penName: String, email: String, role: WorkRole, organization: String, cmosEdition: String) -> Unit = { _, _, _, _, _, _ -> },
    onSignOut: () -> Unit = {}
) {
    val currentUser = state.currentUser
    val globalIndex = state.globalBookIndex
    val suspendedUsers = state.suspendedUsers
    val pendingBooks = state.pendingBooks
    val mailboxMessages = state.mailboxMessages
    val pendingEditorAssignments = state.pendingEditorAssignments
    val registryAuthors = state.registryAuthors
    val siteUsers = state.siteUsers
    val regentEmail = state.regentEmail
    val isRoot = state.isRoot
    val allIssuances = state.allIssuances
    val claimedIssuanceIds = state.claimedIssuanceIds
    val allEditorAssignments = state.allEditorAssignments
    val poolEntries = state.poolEntries
    val poolTotals = state.poolTotals
    val allUnits = state.allUnits
    val approvalUnitsMessage = state.approvalUnitsMessage
    val allPayoutRequests = state.allPayoutRequests
    val systemConfig = state.systemConfig
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Master Book Index", "Author Roster & Governance", "Mailbox Dispatcher", "Drive Storage Health")

    var showProfileDialog by remember { mutableStateOf(false) }
    var inspectingEntry by remember { mutableStateOf<GlobalBookIndexEntry?>(null) }
    var revokingEntry by remember { mutableStateOf<GlobalBookIndexEntry?>(null) }
    var suspendingTargetUser by remember { mutableStateOf<String?>(null) }
    var suspensionReasonInput by remember { mutableStateOf("Violation of editorial guidelines or unapproved distribution.") }

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
                                text = "Registry Admin",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF3EFE6)
                            )
                            Text(
                                text = "God-Mode Governance • Editor in Chief",
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

            when (selectedTab) {
                0 -> MasterBookIndexTab(
                    entries = globalIndex,
                    currentUser = currentUser,
                    onInspect = { inspectingEntry = it },
                    onRevoke = { fileId -> revokingEntry = globalIndex.firstOrNull { it.fileId == fileId } },
                    pendingBooks = pendingBooks,
                    onApproveBook = onApproveBook,
                    onRejectBook = onRejectBook,
                    pendingEditorAssignments = pendingEditorAssignments,
                    registryAuthors = registryAuthors,
                    siteUsers = siteUsers,
                    onDecideEditorAssignment = onDecideEditorAssignment,
                    onAppointEditor = onAppointEditor,
                    regentEmail = regentEmail,
                    isRoot = isRoot,
                    onSetRegent = onSetRegent,
                    onClearRegent = onClearRegent,
                    suspendedCount = suspendedUsers.size,
                    allIssuances = allIssuances,
                    claimedIssuanceIds = claimedIssuanceIds,
                    allEditorAssignments = allEditorAssignments,
                    onGiveCredit = onGiveCredit,
                    poolEntries = poolEntries,
                    poolTotals = poolTotals,
                    allUnits = allUnits,
                    approvalUnitsMessage = approvalUnitsMessage,
                    allPayoutRequests = allPayoutRequests,
                    onSetPayoutRequestStatus = onSetPayoutRequestStatus,
                    onVestCurrentSeason = onVestCurrentSeason,
                    onRecordPoolEntry = onRecordPoolEntry,
                    systemConfig = systemConfig,
                    onUpdateSystemConfig = onUpdateSystemConfig
                )
                1 -> AuthorRosterTab(
                    globalIndex = globalIndex,
                    suspendedUsers = suspendedUsers,
                    currentUser = currentUser,
                    onOpenSuspendModal = { email -> suspendingTargetUser = email },
                    onUnsuspend = onUnsuspendUser,
                    isRoot = isRoot,
                    regentEmail = regentEmail
                )
                2 -> MailboxDispatcherTab(
                    globalIndex = globalIndex,
                    currentUser = currentUser,
                    onSendMessage = onSendServerlessMail
                )
                3 -> DriveHealthTab(
                    globalIndex = globalIndex,
                    pendingBooks = pendingBooks,
                    suspendedUsers = suspendedUsers,
                    mailboxMessages = mailboxMessages
                )
            }
        }
    }

    revokingEntry?.let { entry ->
        Dialog(onDismissRequest = { revokingEntry = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Revoke registry access?",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = BookGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\u2018${entry.title}\u2019 will lose its registry access.",
                        fontSize = 12.5.sp,
                        color = Color(0xFFC8C2B7)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { revokingEntry = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                onRevokeManuscript(entry.fileId)
                                revokingEntry = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Revoke Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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

                    val rawJson = org.json.JSONObject()
                        .put("fileId", entry.fileId)
                        .put("manuscriptId", entry.manuscriptId)
                        .put("title", entry.title)
                        .put("subtitle", entry.subtitle)
                        .put("authorName", entry.authorName)
                        .put("authorEmail", entry.authorEmail)
                        .put("workType", entry.workType)
                        .put("wordCount", entry.wordCount)
                        .put("totalLeaves", entry.totalLeaves)
                        .put("lastSynced", entry.lastSyncedTimestamp)
                        .put("isPublicInCommunity", entry.isPublicInCommunity)
                        .toString(2)

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
                                revokingEntry = entry
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
                        text = "This adds the account to the suspension list in the back room. The account is locked out on its next check.",
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
    onRevoke: (String) -> Unit,
    pendingBooks: List<GlobalBookIndexEntry> = emptyList(),
    onApproveBook: (String) -> Unit = {},
    onRejectBook: (String) -> Unit = {},
    pendingEditorAssignments: List<EditorAssignment> = emptyList(),
    registryAuthors: List<Pair<String, String>> = emptyList(),
    siteUsers: List<SiteUser> = emptyList(),
    onDecideEditorAssignment: (EditorAssignment, Boolean) -> Unit = { _, _ -> },
    onAppointEditor: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    regentEmail: String = "",
    isRoot: Boolean = false,
    onSetRegent: (String) -> Unit = {},
    onClearRegent: () -> Unit = {},
    suspendedCount: Int = 0,
    allIssuances: List<CreditIssuance> = emptyList(),
    claimedIssuanceIds: Set<String> = emptySet(),
    allEditorAssignments: List<EditorAssignment> = emptyList(),
    onGiveCredit: (String) -> Unit = {},
    poolEntries: List<PoolEntry> = emptyList(),
    poolTotals: PoolTotals = PoolTotals(),
    allUnits: List<UnitGrant> = emptyList(),
    approvalUnitsMessage: String? = null,
    allPayoutRequests: List<PayoutRequest> = emptyList(),
    onSetPayoutRequestStatus: (PayoutRequest, String) -> Unit = { _, _ -> },
    onVestCurrentSeason: () -> Unit = {},
    onRecordPoolEntry: (String, Long, String) -> Unit = { _, _, _ -> },
    systemConfig: StudioSystemConfig = StudioSystemConfig(),
    onUpdateSystemConfig: (String, String) -> Unit = { _, _ -> }
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    var selectedEditorEmail by remember { mutableStateOf<String?>(null) }
    var selectedBookFileId by remember { mutableStateOf<String?>(null) }
    var regentInput by remember { mutableStateOf("") }
    var decidingBook by remember { mutableStateOf<Pair<GlobalBookIndexEntry, Boolean>?>(null) }
    var showVestConfirm by remember { mutableStateOf(false) }
    var poolEntryKind by remember { mutableStateOf(PoolKinds.EXPENSE) }
    var poolEntryAmount by remember { mutableStateOf("") }
    var poolEntryNote by remember { mutableStateOf("") }
    var geminiKeyInput by remember { mutableStateOf("") }
    var geminiModelInput by remember(systemConfig.geminiModel) {
        mutableStateOf(systemConfig.geminiModel)
    }
    val currentSeasonYear = UnitChain.seasonYearFor(System.currentTimeMillis())
    val seasonCapReached = allUnits.count {
        it.role == UnitRoles.AUTHOR && it.seasonYear == currentSeasonYear
    } >= UnitChain.SEASON_AUTHOR_CAP

    decidingBook?.let { (entry, approve) ->
        Dialog(onDismissRequest = { decidingBook = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (approve) "Approve this book?" else "Reject this book?",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = BookGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (approve) {
                            "\u2018${entry.title}\u2019 by ${entry.authorName} will be published to the registry."
                        } else {
                            "\u2018${entry.title}\u2019 by ${entry.authorName} will be removed from the queue. This cannot be undone."
                        },
                        fontSize = 12.5.sp,
                        color = Color(0xFFC8C2B7)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (approve) {
                        val unitsRecorded = allUnits.any {
                            it.fileId == entry.fileId && it.role == UnitRoles.AUTHOR
                        }
                        val editorUnitCount = allEditorAssignments.count {
                            it.fileId == entry.fileId && it.status == "APPROVED"
                        }
                        Text(
                            text = when {
                                unitsRecorded -> "Units: already recorded for this book"
                                seasonCapReached -> "Units: season cap reached, none issued"
                                else -> "Units: Author +1, Editor +1 \u00d7$editorUnitCount, Chair +1"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFC8C2B7)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { decidingBook = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                if (approve) onApproveBook(entry.fileId) else onRejectBook(entry.fileId)
                                decidingBook = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (approve) BookGoldDark else Color(0xFFC62828)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (approve) "Approve" else "Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showVestConfirm) {
        Dialog(onDismissRequest = { showVestConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C26),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Vest $currentSeasonYear units?",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        color = BookGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vesting makes this season's units payable. This cannot be undone.",
                        fontSize = 12.5.sp,
                        color = Color(0xFFC8C2B7)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showVestConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                        Button(
                            onClick = {
                                onVestCurrentSeason()
                                showVestConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BookGoldDark
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Vest", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Totals",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TotalChip("Users", siteUsers.size, Modifier.weight(1f))
            TotalChip("Books", entries.size, Modifier.weight(1f))
            TotalChip("Pending", pendingBooks.size, Modifier.weight(1f))
            TotalChip("Suspended", suspendedCount, Modifier.weight(1f))
            TotalChip("Assignments", allEditorAssignments.count { it.status == "APPROVED" }, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(14.dp))

        approvalUnitsMessage?.let { message ->
            Text(
                text = message,
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

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

        if (pendingBooks.isNotEmpty()) {
            Text(
                text = "Awaiting Approval (${pendingBooks.size})",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = BookGoldLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            pendingBooks.forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("admin_pending_card_${entry.fileId}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, BookGoldDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = entry.title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "${entry.authorName} · ${entry.authorEmail}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "${entry.wordCount} words · ${entry.workType}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { decidingBook = entry to true },
                                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_approve_${entry.fileId}")
                            ) {
                                Text("Approve", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { decidingBook = entry to false },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_reject_${entry.fileId}")
                            ) {
                                Text("Reject", fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = "Credit (${allIssuances.size})",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        var giftEmail by remember { mutableStateOf("") }
        if (isRoot) {
            Text(
                text = "Give credit",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3EFE6)
            )
            Text(
                text = "A gift from the chair, at the schedule amount for that author's mined claims. The author mines it like any other issuance.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = giftEmail,
                    onValueChange = { giftEmail = it },
                    placeholder = { Text("Author email", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BookGoldDark,
                        unfocusedBorderColor = Color(0xFF444455),
                        focusedContainerColor = Color(0xFF1B1B24),
                        unfocusedContainerColor = Color(0xFF16161E)
                    )
                )
                Button(
                    onClick = {
                        if (giftEmail.isNotBlank()) {
                            onGiveCredit(giftEmail.trim())
                            giftEmail = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Give", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (allIssuances.isEmpty()) {
            Text(
                text = "No credit issued.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
        } else {
            allIssuances.forEach { issuance ->
                val bookTitle = (entries + pendingBooks).firstOrNull { it.fileId == issuance.fileId }?.title
                    ?: issuance.fileId
                val claimed = claimedIssuanceIds.contains(issuance.issuanceHash)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("admin_issuance_card_${issuance.issuanceHash}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, BookGoldDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = issuance.authorEmail,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "$bookTitle \u00b7 ${issuance.kind}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "${issuance.amount} \u00b7 ${if (claimed) "Claimed" else "Unclaimed"}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Pool",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Receipts ${formatCents(poolTotals.receiptsCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Expenses ${formatCents(poolTotals.expensesCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Refunds ${formatCents(poolTotals.refundsCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Fees ${formatCents(poolTotals.feesCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Paid out ${formatCents(poolTotals.payoutsCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Balance ${formatCents(poolTotals.balanceCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Settled ${formatCents(poolTotals.balanceCents - poolTotals.unsettledCents)}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        val vestedUnits = allUnits.filter { it.vested }
        val settledCents = poolTotals.balanceCents - poolTotals.unsettledCents
        Text(
            text = "Total units (vested) ${vestedUnits.size}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Unit value ${formatCents(UnitChain.unitValueCents(settledCents, vestedUnits.size))}",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Text(
            text = "Season $currentSeasonYear",
            fontSize = 11.5.sp,
            color = Color(0xFFB0A89C)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = { showVestConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark)
        ) {
            Text("Vest $currentSeasonYear Units", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        UnitChain.totalsByHolder(vestedUnits).entries
            .sortedByDescending { it.value }
            .take(10)
            .forEach { (holderEmail, count) ->
                Text(
                    text = "$holderEmail \u2014 $count units",
                    fontSize = 11.5.sp,
                    color = Color(0xFFB0A89C)
                )
            }
        Spacer(modifier = Modifier.height(8.dp))

        if (poolEntries.isEmpty()) {
            Text(
                text = "No pool entries.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
        } else {
            poolEntries.takeLast(5).reversed().forEach { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("admin_pool_entry_card_${entry.entryHash}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, BookGoldDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = entry.kind,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "${formatCents(entry.amountCents)} \u00b7 ${entry.note}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Record Entry",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                PoolKinds.EXPENSE to "Expense",
                PoolKinds.REFUND to "Refund",
                PoolKinds.FEE to "Fee",
                PoolKinds.AD_RETURN to "Ad Return"
            ).forEach { (kind, label) ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (poolEntryKind == kind) FontWeight.Bold else FontWeight.Normal,
                    color = if (poolEntryKind == kind) BookGoldLight else Color(0xFFB0A89C),
                    modifier = Modifier.clickable { poolEntryKind = kind }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val recordAmountCents = poolEntryAmount.trim().toDoubleOrNull()
            ?.let { kotlin.math.roundToLong(it * 100) }
        OutlinedTextField(
            value = poolEntryAmount,
            onValueChange = { input ->
                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                    poolEntryAmount = input
                }
            },
            label = { Text("Amount (USD)", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGoldDark,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1B1B24),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = poolEntryNote,
            onValueChange = { poolEntryNote = it },
            label = { Text("Note", color = BookGoldLight) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFF3EFE6)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BookGoldDark,
                unfocusedBorderColor = Color(0xFF444455),
                focusedContainerColor = Color(0xFF1B1B24),
                unfocusedContainerColor = Color(0xFF16161E)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onRecordPoolEntry(poolEntryKind, recordAmountCents ?: 0L, poolEntryNote.trim())
                poolEntryAmount = ""
                poolEntryNote = ""
            },
            enabled = recordAmountCents != null && recordAmountCents > 0L,
            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark)
        ) {
            Text("Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Payout Requests",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (allPayoutRequests.isEmpty()) {
            Text(
                text = "No payout requests.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
        } else {
            allPayoutRequests.forEach { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("admin_payout_request_card_${request.requestHash}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, BookGoldDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = request.requesterEmail,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "${request.units} units \u00b7 ${formatCents(request.amountCents)}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "${request.method} \u00b7 ${request.destination}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = request.status,
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        if (request.status == PayoutStatuses.PENDING) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onSetPayoutRequestStatus(request, PayoutStatuses.SENT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_payout_sent_${request.requestHash}")
                                ) {
                                    Text("Mark Sent", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onSetPayoutRequestStatus(request, PayoutStatuses.REJECTED) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_payout_reject_${request.requestHash}")
                                ) {
                                    Text("Reject", fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Editor Assignments",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingEditorAssignments.isEmpty()) {
            Text(
                text = "No editor requests awaiting a decision.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
        } else {
            pendingEditorAssignments.forEach { assignment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("admin_assignment_card_${assignment.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    border = BorderStroke(1.dp, BookGoldDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = entries.firstOrNull { it.fileId == assignment.fileId }?.title ?: assignment.fileId,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF3EFE6)
                        )
                        Text(
                            text = "Author: ${assignment.bookAuthorEmail}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Text(
                            text = "Editor: ${assignment.editorEmail}",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB0A89C)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onDecideEditorAssignment(assignment, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_assignment_approve_${assignment.id}")
                            ) {
                                Text("Approve", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onDecideEditorAssignment(assignment, false) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_assignment_decline_${assignment.id}")
                            ) {
                                Text("Decline", fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Appoint Editor",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        registryAuthors.filterNot { it.second.equals(currentUser.email, ignoreCase = true) }.forEach { (authorName, authorEmail) ->
            val authorSelected = selectedEditorEmail == authorEmail
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { selectedEditorEmail = authorEmail }
                    .testTag("admin_author_row_$authorEmail"),
                colors = CardDefaults.cardColors(containerColor = if (authorSelected) Color(0xFF2A2118) else Color(0xFF14141C)),
                border = BorderStroke(1.dp, if (authorSelected) BookGold else BookGoldDark)
            ) {
                Text(
                    text = "$authorName \u00b7 $authorEmail",
                    fontSize = 12.sp,
                    color = Color(0xFFF3EFE6),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        entries.forEach { entry ->
            val bookSelected = selectedBookFileId == entry.fileId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { selectedBookFileId = entry.fileId }
                    .testTag("admin_book_row_${entry.fileId}"),
                colors = CardDefaults.cardColors(containerColor = if (bookSelected) Color(0xFF2A2118) else Color(0xFF14141C)),
                border = BorderStroke(1.dp, if (bookSelected) BookGold else BookGoldDark)
            ) {
                Text(
                    text = "${entry.title} \u00b7 ${entry.authorName}",
                    fontSize = 12.sp,
                    color = Color(0xFFF3EFE6),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val book = entries.firstOrNull { it.fileId == selectedBookFileId }
                val editorEmail = selectedEditorEmail
                if (book != null && editorEmail != null) {
                    val editorAddress = entries.firstOrNull {
                        it.authorEmail.trim().lowercase() == editorEmail.trim().lowercase()
                    }?.authorAddress ?: ""
                    onAppointEditor(book.fileId, book.authorEmail, editorEmail, editorAddress)
                }
            },
            enabled = selectedBookFileId != null && selectedEditorEmail != null,
            colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_appoint_editor")
        ) {
            Text("Appoint", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRoot) {
        Text(
            text = "Regent Editor in Chief",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (regentEmail.isBlank()) "No regent appointed." else "Regent: $regentEmail",
            fontSize = 12.sp,
            color = Color(0xFFC8C2B7)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = regentInput,
            onValueChange = { regentInput = it },
            label = { Text("Regent email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (regentInput.isNotBlank()) onSetRegent(regentInput) },
                enabled = regentInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BookGoldDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Appoint Regent", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            if (regentEmail.isNotBlank()) {
                OutlinedButton(
                    onClick = onClearRegent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear Regent", fontSize = 12.5.sp)
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRoot) {
        // Ship's panel: steel-navy plate, brass trim, lamp and readouts.
        val panelSteel = Color(0xFF0D1626)
        val panelInset = Color(0xFF05090F)
        val panelLine = Color(0xFF2A3850)
        val panelBrass = Color(0xFFC9A227)
        val panelBrassDim = Color(0xFF8A6D1F)
        val panelReadout = Color(0xFFD7DEE8)
        val lampColor = if (systemConfig.geminiApiKey.isBlank()) {
            Color(0xFFFFB020)
        } else {
            Color(0xFF3DDC74)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(panelSteel, RoundedCornerShape(10.dp))
                .border(1.5.dp, panelBrass, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .background(lampColor, CircleShape)
                            .border(1.dp, panelBrassDim, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = "CONTROL ROOM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 3.sp,
                        color = panelBrass
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(panelBrassDim)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "GEMINI KEY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = panelBrass
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelInset, RoundedCornerShape(4.dp))
                        .border(1.dp, panelLine, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (systemConfig.geminiApiKey.isBlank()) {
                            "Gemini API Key: Not set"
                        } else {
                            "Gemini API Key: Set \u2022\u2022\u2022\u2022" +
                                systemConfig.geminiApiKey.takeLast(4)
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = panelReadout
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MODEL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = panelBrass
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelInset, RoundedCornerShape(4.dp))
                        .border(1.dp, panelLine, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = systemConfig.geminiModel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = panelReadout
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "NEW API KEY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = panelBrass
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = { geminiKeyInput = it },
                    placeholder = { Text("New API key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = panelReadout
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = panelBrass,
                        unfocusedBorderColor = panelLine,
                        focusedContainerColor = panelInset,
                        unfocusedContainerColor = panelInset
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "GEMINI MODEL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = panelBrass
                )
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = geminiModelInput,
                    onValueChange = { geminiModelInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = panelReadout
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = panelBrass,
                        unfocusedBorderColor = panelLine,
                        focusedContainerColor = panelInset,
                        unfocusedContainerColor = panelInset
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onUpdateSystemConfig(geminiKeyInput, geminiModelInput)
                        geminiKeyInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = panelBrass,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Save Controls",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Use a key restricted to the Gemini API, with a quota cap.",
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp,
                    color = panelBrassDim
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(6.dp)
                    .background(panelBrassDim, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(6.dp)
                    .background(panelBrassDim, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .size(6.dp)
                    .background(panelBrassDim, CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(6.dp)
                    .background(panelBrassDim, CircleShape)
            )
        }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Users",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = BookGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (siteUsers.isEmpty()) {
            Text(
                text = "No users recorded yet.",
                fontSize = 11.5.sp,
                color = Color(0xFFB0A89C)
            )
        } else {
            siteUsers.forEach { user ->
                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = user.displayName.ifBlank { user.email },
                        fontSize = 12.sp,
                        color = Color(0xFFF3EFE6)
                    )
                    Text(
                        text = if (user.lastSeenAtMillis > 0L) {
                            "${user.email} · Last seen: ${dateFormat.format(Date(user.lastSeenAtMillis))}"
                        } else {
                            user.email
                        },
                        fontSize = 11.5.sp,
                        color = Color(0xFFB0A89C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No manuscripts currently registered in the registry.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
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

                                if (!entry.authorEmail.equals(currentUser.email, ignoreCase = true)) {
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
}

@Composable
fun AuthorRosterTab(
    globalIndex: List<GlobalBookIndexEntry>,
    suspendedUsers: List<SuspendedUserEntry>,
    currentUser: UserProfile,
    onOpenSuspendModal: (String) -> Unit,
    onUnsuspend: (String) -> Unit,
    isRoot: Boolean = true,
    regentEmail: String = ""
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
                val isProtectedRoot = !isRoot &&
                    !email.equals(regentEmail, ignoreCase = true) &&
                    authorBooks.any { it.editorInChief }
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
                            } else if (!isCurrentUser && !isProtectedRoot) {
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
    var sentToRecipient by remember { mutableStateOf<String?>(null) }

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
            text = "Sends direct messages or broadcasts through the back room mailbox.",
            fontSize = 11.sp,
            color = Color(0xFFB0A89C)
        )

        Spacer(modifier = Modifier.height(14.dp))

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
                    onClick = { selectedRecipient = email; sentToRecipient = null },
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

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it; sentToRecipient = null },
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

        OutlinedTextField(
            value = body,
            onValueChange = { body = it; sentToRecipient = null },
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
                            senderEmail = FirestoreNetworkService.DEFAULT_STUDIO_AUTHOR_EMAIL,
                            senderName = "Editor in Chief",
                            subject = subject.trim(),
                            body = body.trim(),
                            messageType = selectedType
                        )
                    )
                    subject = ""
                    body = ""
                    sentToRecipient = selectedRecipient
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

        if (sentToRecipient != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ Dispatched message successfully to $sentToRecipient.",
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
    pendingBooks: List<GlobalBookIndexEntry>,
    suspendedUsers: List<SuspendedUserEntry>,
    mailboxMessages: List<ServerlessMailMessage>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Back Room Structure",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 14.sp,
            color = Color(0xFFF3EFE6)
        )
        Text(
            text = "Firestore collections, as last refreshed",
            fontSize = 11.sp,
            color = Color(0xFFB0A89C)
        )

        Spacer(modifier = Modifier.height(14.dp))

        val driveNodes = listOf(
            Triple("Registry", "${globalIndex.size} manuscripts registered", true),
            Triple("Awaiting Approval", "${pendingBooks.size} books awaiting approval", true),
            Triple("Suspensions", "${suspendedUsers.size} accounts blocked", true),
            Triple("Mail", "${mailboxMessages.size} messages", true)
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
                            Icons.Default.Storage,
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

                }
            }
        }
    }
}


private fun formatCents(cents: Long): String {
    val sign = if (cents < 0L) "-" else ""
    val abs = kotlin.math.abs(cents)
    return "${sign}\$${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}

@Composable
private fun TotalChip(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        border = BorderStroke(1.dp, BookGoldDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFFF3EFE6)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFFB0A89C)
            )
        }
    }
}
