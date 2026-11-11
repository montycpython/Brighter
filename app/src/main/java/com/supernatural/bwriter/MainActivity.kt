package com.supernatural.bwriter

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.supernatural.bwriter.data.IllustrationUris
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.ServerlessMailMessage
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.model.WorkType
import com.supernatural.bwriter.ui.screens.AdminGodModeDashboardScreen
import com.supernatural.bwriter.ui.screens.AuthScreen
import com.supernatural.bwriter.ui.screens.CmosRulebookScreen
import com.supernatural.bwriter.ui.screens.VersionScreen
import com.supernatural.bwriter.ui.screens.CommunityDirectoryScreen
import com.supernatural.bwriter.ui.screens.KillSwitchSuspensionScreen
import com.supernatural.bwriter.ui.screens.LeafReaderScreen
import com.supernatural.bwriter.ui.screens.MailboxInboxScreen
import com.supernatural.bwriter.ui.screens.ManuscriptDetailScreen
import com.supernatural.bwriter.ui.screens.ManuscriptListScreen
import com.supernatural.bwriter.ui.screens.NewWorkDialog
import com.supernatural.bwriter.ui.screens.OriginOfWritingScreen
import com.supernatural.bwriter.ui.screens.PdfExportScreen
import com.supernatural.bwriter.ui.screens.SectionEditorScreen
import com.supernatural.bwriter.ui.screens.SubscriptionPaywallDialog
import com.supernatural.bwriter.ui.screens.SyncToGoogleDriveDialog
import com.supernatural.bwriter.ui.screens.UserAgreementScreen
import com.supernatural.bwriter.ui.theme.BwriterTheme
import com.google.android.gms.ads.MobileAds
import com.supernatural.bwriter.ui.ads.AdBannerBar
import com.supernatural.bwriter.data.BillingManager
import com.supernatural.bwriter.data.BillingProducts
import com.supernatural.bwriter.data.PayoutStatuses
import com.supernatural.bwriter.ui.ads.AdsFreeStore
import com.supernatural.bwriter.ui.ads.InterstitialAds
import com.supernatural.bwriter.ui.viewmodel.BwriterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BwriterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
        }
        try {
            MobileAds.initialize(this) {}
        } catch (t: Throwable) {
        }
        enableEdgeToEdge()
        setContent {
            BwriterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BwriterAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BwriterAppNavigation(viewModel: BwriterViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        com.supernatural.bwriter.data.DriveConsentBridge.onConsentResult(result.resultCode)
    }
    LaunchedEffect(driveConsentLauncher) {
        com.supernatural.bwriter.data.DriveConsentBridge.launcher = { request ->
            driveConsentLauncher.launch(request)
        }
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val manuscripts by viewModel.manuscripts.collectAsState()
    val selectedFilter by viewModel.selectedWorkTypeFilter.collectAsState()
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val activeSections by viewModel.activeSections.collectAsState()
    val calculatedLeaves by viewModel.calculatedLeaves.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()
    val activeComments by viewModel.activeSectionComments.collectAsState()
    val characters by viewModel.charactersForActiveManuscript.collectAsState()
    val settings by viewModel.settingsForActiveManuscript.collectAsState()
    val isGeneratingAiProse by viewModel.isGeneratingAiProse.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val driveSyncStatus by viewModel.driveSyncStatus.collectAsState()
    val globalBookIndex by viewModel.globalBookIndex.collectAsState()
    val pendingBooks by viewModel.pendingBooks.collectAsState()
    val suspendedUsers by viewModel.suspendedUsers.collectAsState()
    val mailboxMessages by viewModel.mailboxMessages.collectAsState()
    val pendingEditorAssignments by viewModel.pendingEditorAssignments.collectAsState()
    val registryAuthors by viewModel.registryAuthors.collectAsState()
    val siteUsers by viewModel.siteUsers.collectAsState()
    val allIssuances by viewModel.allIssuances.collectAsState()
    val claimedIssuanceIds by viewModel.claimedIssuanceIds.collectAsState()
    val allEditorAssignments by viewModel.allEditorAssignments.collectAsState()
    val approvalPreview by viewModel.approvalPreview.collectAsState()
    val poolEntries by viewModel.poolEntries.collectAsState()
    val poolTotals by viewModel.poolTotals.collectAsState()
    val allUnits by viewModel.allUnits.collectAsState()
    val approvalUnitsMessage by viewModel.approvalUnitsMessage.collectAsState()
    val myPayoutRequests by viewModel.myPayoutRequests.collectAsState()
    val allPayoutRequests by viewModel.allPayoutRequests.collectAsState()
    val myPayoutValueCents by viewModel.myPayoutValueCents.collectAsState()
    val regentEmail by viewModel.regentEmail.collectAsState()
    val isRoot by viewModel.isRoot.collectAsState()
    val activeSuspension by viewModel.activeSuspension.collectAsState()
    val hasAcceptedTerms by viewModel.hasAcceptedTerms.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val savedGoogleAccounts by viewModel.savedGoogleAccounts.collectAsState()
    val userSubscription by viewModel.userAiSubscription.collectAsState()
    val tokenTransactions by viewModel.tokenTransactions.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val creditBalance by viewModel.creditBalance.collectAsState()
    val unclaimedCredit by viewModel.unclaimedCredit.collectAsState()
    val claimFailed by viewModel.claimFailed.collectAsState()
    val adFreeEntitled by viewModel.adFreeEntitled.collectAsState()
    val billingPrices by viewModel.billingPrices.collectAsState()
    val billingMessage by viewModel.billingMessage.collectAsState()
    val aiGenerationFailed by viewModel.aiGenerationFailed.collectAsState()
    val assignedBooks by viewModel.assignedBooks.collectAsState()
    val exportFailed by viewModel.exportFailed.collectAsState()
    val versionHistory by viewModel.versionHistory.collectAsState()

    var showNewWorkDialog by remember { mutableStateOf(false) }
    var manuscriptToSync by remember { mutableStateOf<ManuscriptEntity?>(null) }

    if (!hasAcceptedTerms) {
        UserAgreementScreen(
            isMandatoryOnboarding = true,
            onAccept = {
                viewModel.acceptTermsOfService()
                Toast.makeText(context, "Welcome to Bwriter Studio!", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    if (!isLoggedIn) {
        AuthScreen(
            currentUser = currentUser,
            savedAccounts = savedGoogleAccounts,
            onGoogleSignIn = { email, name, penName ->
                viewModel.signInWithGoogleAccount(email, name, penName)
                Toast.makeText(context, "Signed in as $email", Toast.LENGTH_SHORT).show()
            },
            onRemoveSavedAccount = { email ->
                viewModel.removeSavedGoogleAccount(email)
            },
            onContinue = {}
        )
        return
    }

    if (activeSuspension != null) {
        KillSwitchSuspensionScreen(
            userProfile = currentUser,
            suspension = activeSuspension!!,
            onRetrySync = {
                viewModel.refreshNetwork()
                Toast.makeText(context, "Checking account status...", Toast.LENGTH_SHORT).show()
            },
            onSwitchUser = {
                viewModel.signOutGoogleAccount()
            }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val adsFreeStore = remember { AdsFreeStore(context) }
    var adsFreeUntil by remember { mutableStateOf(adsFreeStore.adsFreeUntil()) }
    val showBanner = !adsFreeStore.isAdsFree() &&
        currentRoute != "section_editor/{sectionId}"

    val billingManager = remember {
        BillingManager(
            context.applicationContext,
            onPurchase = { purchase -> viewModel.onBillingPurchase(purchase) },
            onPrices = { prices -> viewModel.onBillingPrices(prices) }
        )
    }
    LaunchedEffect(billingManager) {
        viewModel.billingManager = billingManager
        billingManager.connect()
    }
    LaunchedEffect(adFreeEntitled) {
        adsFreeStore.setPurchasedAdFree(adFreeEntitled)
    }
    LaunchedEffect(billingMessage) {
        val message = billingMessage
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearBillingMessage()
        }
    }

    val hostActivity = context as? Activity
    var previousRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        InterstitialAds.preload(context)
    }
    LaunchedEffect(currentRoute) {
        val route = currentRoute
        val previous = previousRoute
        if (hostActivity != null && route != null && previous != null) {
            val openedBook = previous == "manuscripts" &&
                (route == "manuscript_detail/{manuscriptId}" || route == "leaf_reader/{manuscriptId}")
            val closedBook = (previous == "manuscript_detail/{manuscriptId}" ||
                previous == "leaf_reader/{manuscriptId}") && route == "manuscripts"
            val openedRegistryBook = previous == "community_directory" &&
                (route == "manuscript_detail/{manuscriptId}" || route == "leaf_reader/{manuscriptId}")
            val closedRegistryBook = (previous == "manuscript_detail/{manuscriptId}" ||
                previous == "leaf_reader/{manuscriptId}") && route == "community_directory"
            if (openedBook || closedBook || openedRegistryBook || closedRegistryBook) {
                InterstitialAds.showIfEligible(hostActivity)
            }
        }
        if (route != null) previousRoute = route
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
    NavHost(
        navController = navController,
        startDestination = "manuscripts"
    ) {
        composable("auth") {
            AuthScreen(
                currentUser = currentUser,
                savedAccounts = savedGoogleAccounts,
                    onGoogleSignIn = { email, name, penName ->
                    viewModel.signInWithGoogleAccount(email, name, penName)
                    Toast.makeText(context, "Signed in as $email", Toast.LENGTH_SHORT).show()
                },
                onRemoveSavedAccount = { email ->
                    viewModel.removeSavedGoogleAccount(email)
                },
                onContinue = {
                    navController.popBackStack()
                }
            )
        }

        composable("manuscripts") {
            ManuscriptListScreen(
                manuscripts = manuscripts,
                currentUser = currentUser,
                selectedFilter = selectedFilter,
                onSelectFilter = { type -> viewModel.setWorkTypeFilter(type) },
                onOpenManuscript = { id ->
                    viewModel.selectManuscript(id)
                    navController.navigate("manuscript_detail/$id")
                },
                onOpenReader = { id ->
                    viewModel.selectManuscript(id)
                    navController.navigate("leaf_reader/$id")
                },
                onOpenExport = { id ->
                    viewModel.selectManuscript(id)
                    navController.navigate("pdf_export/$id")
                },
                onOpenRulebook = {
                    navController.navigate("cmos_rulebook")
                },
                onOpenOrigin = {
                    navController.navigate("origin_of_writing")
                },
                onOpenAuthScreen = {
                    navController.navigate("auth")
                },
                onDeleteManuscript = { id ->
                    viewModel.deleteManuscript(id)
                },
                onUpdateProfile = { name, penName, email, role, org, cmos ->
                    viewModel.updateProfile(name, penName, email, role, org, cmos)
                    val byline = if (penName.isNotBlank()) penName else name
                    Toast.makeText(context, "Author Profile updated: $byline", Toast.LENGTH_SHORT).show()
                },
                onCreateNewWorkClick = {
                    showNewWorkDialog = true
                },
                onOpenCommunity = {
                    viewModel.refreshNetwork()
                    navController.navigate("community_directory")
                },
                onOpenMailbox = {
                    viewModel.refreshNetwork()
                    navController.navigate("mailbox_inbox")
                },
                onOpenAdminDashboard = {
                    viewModel.refreshNetwork()
                    navController.navigate("admin_god_mode")
                },
                onSyncManuscript = { manuscript ->
                    manuscriptToSync = manuscript
                },
                onOpenUserAgreement = {
                    navController.navigate("user_agreement")
                },
                onOpenVersion = {
                    navController.navigate("version")
                },
                onOpenSubscription = {
                    viewModel.openPaywall()
                },
                onSignOut = {
                    viewModel.signOutGoogleAccount()
                    Toast.makeText(context, "Logged out of Google account", Toast.LENGTH_SHORT).show()
                },
                onRequestAccountDeletion = { onResult ->
                    viewModel.requestAccountDeletion(onResult)
                },
                onRestoreFromDrive = {
                    viewModel.restoreManuscriptsFromGoogleDrive(interactive = true) { count ->
                        Toast.makeText(context, "Restored $count manuscripts from Google Drive!", Toast.LENGTH_LONG).show()
                    }
                },
                unreadMailCount = driveSyncStatus.unreadMailCount
            )
        }

        composable(
            route = "manuscript_detail/{manuscriptId}",
            arguments = listOf(navArgument("manuscriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val manuscriptId = backStackEntry.arguments?.getLong("manuscriptId") ?: return@composable
            viewModel.selectManuscript(manuscriptId)
            viewModel.loadVersionHistory(manuscriptId)

            val currentM = activeManuscript
            if (currentM != null) {
                ManuscriptDetailScreen(
                    manuscript = currentM,
                    sections = activeSections,
                    calculatedLeaves = calculatedLeaves,
                    currentUser = currentUser,
                    versionSnapshots = versionHistory,
                    onBack = { navController.popBackStack() },
                    onOpenSection = { secId ->
                        viewModel.selectSection(secId)
                        navController.navigate("section_editor/$secId")
                    },
                    onOpenReader = {
                        navController.navigate("leaf_reader/$manuscriptId")
                    },
                    onOpenExport = {
                        navController.navigate("pdf_export/$manuscriptId")
                    },
                    onAddSection = { matter, type, title, subtitle ->
                        viewModel.addSection(manuscriptId, matter, type, title, subtitle)
                    },
                    onDeleteSection = { secId ->
                        viewModel.deleteSection(secId, manuscriptId)
                    },
                    onUpdateManuscript = { updated ->
                        viewModel.updateManuscriptDetails(updated)
                    },
                    onSyncToDrive = {
                        manuscriptToSync = currentM
                    }
                )
            }
        }

        composable(
            route = "section_editor/{sectionId}",
            arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
            viewModel.selectSection(sectionId)

            val currentSec = activeSection
            if (currentSec != null) {
                SectionEditorScreen(
                    section = currentSec,
                    comments = activeComments,
                    currentUser = currentUser,
                    manuscript = activeManuscript,
                    characters = characters,
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onSaveContent = { sec, content ->
                        viewModel.updateSectionContent(sec, content)
                    },
                    onSaveAiPrompt = { sec, prompt ->
                        viewModel.updateSectionAiPrompt(sec, prompt)
                    },
                    onSaveTitle = { sec, title, subtitle ->
                        viewModel.updateSectionTitle(sec, title, subtitle)
                    },
                    onSaveIllustrations = { sec, hUri, hCap, tUri, tCap ->
                        viewModel.updateSectionIllustrations(sec, hUri, hCap, tUri, tCap)
                    },
                    onUpdateStatus = { sec, status ->
                        viewModel.updateSectionStatus(sec, status)
                    },
                    onAddComment = { sId, mId, text, cmosRef ->
                        viewModel.addEditorialComment(sId, mId, text, cmosRef)
                    },
                    onResolveComment = { commentId, resolved ->
                        viewModel.resolveComment(commentId, resolved)
                    },
                    onSaveCharacter = { char ->
                        viewModel.saveCharacter(char)
                    },
                    onDeleteCharacter = { charId ->
                        viewModel.deleteCharacter(charId)
                    },
                    onSaveSetting = { setting ->
                        viewModel.saveSetting(setting)
                    },
                    onDeleteSetting = { settingId ->
                        viewModel.deleteSetting(settingId)
                    },
                    onGenerateAiDraft = { sec, prompt, onDone ->
                        viewModel.generateAiDraftFromPrompt(sec, prompt, onDone)
                    },
                    isGeneratingAi = isGeneratingAiProse,
                    aiGenerationFailed = aiGenerationFailed,
                    isAssignedEditor = activeManuscript?.let { viewModel.isAssignedEditor(it) } ?: false,
                    subscription = userSubscription,
                    onOpenSubscription = { viewModel.openPaywall() },
                    onSaveAssignment = { sec, author, role, notes ->
                        viewModel.updateSectionAssignment(sec, author, role, notes)
                    },
                    onSendServerlessMail = { mail ->
                        viewModel.sendServerlessMailDirective(mail)
                    },
                    onProposeRevision = { sec, revContent ->
                        viewModel.proposeSectionRevision(sec, revContent)
                    },
                    onAcceptRevision = { sec ->
                        viewModel.acceptSectionRevision(sec)
                    },
                    onRejectRevision = { sec ->
                        viewModel.rejectSectionRevision(sec)
                    }
                )
            }
        }

        composable(
            route = "leaf_reader/{manuscriptId}",
            arguments = listOf(navArgument("manuscriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val manuscriptId = backStackEntry.arguments?.getLong("manuscriptId") ?: return@composable
            viewModel.selectManuscript(manuscriptId)

            val currentM = activeManuscript
            if (currentM != null) {
                LeafReaderScreen(
                    manuscript = currentM,
                    leaves = calculatedLeaves,
                    ownBook = IllustrationUris.isOwnBook(currentM.authorEmail, currentUser.email),
                    onBack = { navController.popBackStack() },
                    onOpenExport = {
                        navController.navigate("pdf_export/$manuscriptId")
                    }
                )
            }
        }

        composable(
            route = "pdf_export/{manuscriptId}",
            arguments = listOf(navArgument("manuscriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val manuscriptId = backStackEntry.arguments?.getLong("manuscriptId") ?: return@composable
            viewModel.selectManuscript(manuscriptId)
            val currentM = activeManuscript

            if (currentM != null) {
                val context = LocalContext.current
                PdfExportScreen(
                    manuscript = currentM,
                    sections = activeSections,
                    calculatedLeaves = calculatedLeaves,
                    exportResult = exportResult,
                    isExporting = isExporting,
                    exportFailed = exportFailed,
                    onBack = { navController.popBackStack() },
                    onExportPdf = {
                        viewModel.exportActiveManuscriptToPdf(context) { }
                    }
                )
            }
        }

        composable("cmos_rulebook") {
            CmosRulebookScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("origin_of_writing") {
            OriginOfWritingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("user_agreement") {
            UserAgreementScreen(
                isMandatoryOnboarding = false,
                onAccept = {
                    viewModel.acceptTermsOfService()
                    navController.popBackStack()
                },
                onDecline = {
                    navController.popBackStack()
                }
            )
        }

        composable("version") {
            VersionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("community_directory") {
            CommunityDirectoryScreen(
                books = globalBookIndex,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshNetwork() },
                onOpenReader = { entry ->
                    viewModel.openCommunityBook(entry) { localId ->
                        if (localId > 0) {
                            viewModel.selectManuscript(localId)
                            navController.navigate("leaf_reader/$localId")
                        } else {
                            Toast.makeText(context, "Book unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenManuscript = { entry ->
                    viewModel.openCommunityBook(entry) { localId ->
                        if (localId > 0) {
                            viewModel.selectManuscript(localId)
                            navController.navigate("manuscript_detail/$localId")
                        } else {
                            Toast.makeText(context, "Book unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                assignedBooks = assignedBooks,
                onOpenAssigned = { entry ->
                    viewModel.openCommunityBook(entry) { localId ->
                        if (localId > 0) {
                            viewModel.selectManuscript(localId)
                            navController.navigate("manuscript_detail/$localId")
                        } else {
                            Toast.makeText(context, "Book unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        composable("mailbox_inbox") {
            MailboxInboxScreen(
                currentUser = currentUser,
                messages = mailboxMessages,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshNetwork() },
                onMarkAsRead = { msgId -> viewModel.markMailMessageAsRead(msgId) }
            )
        }

        composable("admin_god_mode") {
            AdminGodModeDashboardScreen(
                currentUser = currentUser,
                globalIndex = globalBookIndex,
                suspendedUsers = suspendedUsers,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshNetwork() },
                onSuspendUser = { email, reason ->
                    viewModel.suspendUserAccount(email, reason) { ok ->
                        Toast.makeText(context, if (ok) "User $email suspended (Kill-Switch deployed)" else "Suspension failed for $email", Toast.LENGTH_SHORT).show()
                    }
                },
                onUnsuspendUser = { email ->
                    viewModel.unsuspendUserAccount(email) { ok ->
                        Toast.makeText(context, if (ok) "User $email unsuspended" else "Unsuspend failed for $email", Toast.LENGTH_SHORT).show()
                    }
                },
                onRevokeManuscript = { fileId ->
                    viewModel.revokeRegistryAccess(fileId) { ok ->
                        Toast.makeText(context, if (ok) "Manuscript access revoked" else "Revoke failed", Toast.LENGTH_SHORT).show()
                    }
                },
                pendingBooks = pendingBooks,
                mailboxMessages = mailboxMessages,
                onApproveBook = { fileId ->
                    viewModel.approveRegistryBook(fileId) { ok ->
                        Toast.makeText(context, if (ok) "Book approved — now in the registry" else "Approval failed — book is still pending", Toast.LENGTH_SHORT).show()
                    }
                },
                onRejectBook = { fileId ->
                    viewModel.rejectRegistryBook(fileId) { ok ->
                        Toast.makeText(context, if (ok) "Book not approved" else "Rejection failed — book is still pending", Toast.LENGTH_SHORT).show()
                    }
                },
                pendingEditorAssignments = pendingEditorAssignments,
                registryAuthors = registryAuthors,
                siteUsers = siteUsers,
                allIssuances = allIssuances,
                claimedIssuanceIds = claimedIssuanceIds,
                allEditorAssignments = allEditorAssignments,
                approvalPreview = approvalPreview,
                onPreviewApproval = { entry -> viewModel.previewApprovalCredit(entry) },
                poolEntries = poolEntries,
                poolTotals = poolTotals,
                allUnits = allUnits,
                approvalUnitsMessage = approvalUnitsMessage,
                allPayoutRequests = allPayoutRequests,
                onSetPayoutRequestStatus = { request, status ->
                    viewModel.setPayoutRequestStatus(request, status)
                    Toast.makeText(context, if (status == PayoutStatuses.SENT) "Payout marked sent" else "Payout request updated", Toast.LENGTH_SHORT).show()
                },
                onVestCurrentSeason = {
                    viewModel.vestCurrentSeason()
                },
                onRecordPoolEntry = { kind, cents, note ->
                    viewModel.recordManualPoolEntry(kind, cents, note)
                    Toast.makeText(context, "Entry recorded", Toast.LENGTH_SHORT).show()
                },
                onDecideEditorAssignment = { assignment, approve ->
                    viewModel.decideEditorAssignment(assignment, approve)
                    Toast.makeText(context, if (approve) "Editor approved" else "Editor declined", Toast.LENGTH_SHORT).show()
                },
                onAppointEditor = { fileId, bookAuthorEmail, editorEmail, editorAddress ->
                    viewModel.appointEditor(fileId, bookAuthorEmail, editorEmail, editorAddress)
                    Toast.makeText(context, "Editor appointed: $editorEmail", Toast.LENGTH_SHORT).show()
                },
                regentEmail = regentEmail,
                isRoot = isRoot,
                onSetRegent = { email ->
                    viewModel.setRegent(email) { ok ->
                        Toast.makeText(context, if (ok) "Regent appointed: $email" else "Regent appointment failed", Toast.LENGTH_SHORT).show()
                    }
                },
                onClearRegent = {
                    viewModel.clearRegent { ok ->
                        Toast.makeText(context, if (ok) "Regent cleared" else "Regent clear failed", Toast.LENGTH_SHORT).show()
                    }
                },
                onSendServerlessMail = { mail ->
                    viewModel.sendServerlessMailDirective(mail)
                    Toast.makeText(context, "Sending mail to ${mail.recipientEmail}", Toast.LENGTH_SHORT).show()
                },
                onUpdateProfile = { name, penName, email, role, org, cmos ->
                    viewModel.updateProfile(name, penName, email, role, org, cmos)
                    val byline = if (penName.isNotBlank()) penName else name
                    Toast.makeText(context, "Profile updated: $byline", Toast.LENGTH_SHORT).show()
                },
                onSignOut = {
                    viewModel.signOutGoogleAccount()
                    Toast.makeText(context, "Logged out of Superuser account", Toast.LENGTH_SHORT).show()
                }
            )
        }
        }
        }
        if (showBanner) {
            AdBannerBar(onRewardEarned = {
                adsFreeStore.grantAdsFree24Hours()
                adsFreeUntil = adsFreeStore.adsFreeUntil()
            })
        }
    }

    if (showPaywall) {
        SubscriptionPaywallDialog(
            subscription = userSubscription,
            recentTransactions = tokenTransactions,
            creditBalance = creditBalance,
            unclaimedCredit = unclaimedCredit,
            claimFailed = claimFailed,
            adFreePrice = billingPrices[BillingProducts.AD_FREE_PRODUCT_ID] ?: "",
            tokenPackPrice = billingPrices[BillingProducts.TOKENS_PRODUCT_ID] ?: "",
            tokenBulkPrice = billingPrices[BillingProducts.TOKENS_100_PRODUCT_ID] ?: "",
            onBuyAdFree = { hostActivity?.let { viewModel.buyAdFree(it) } },
            onBuyTokens = { hostActivity?.let { viewModel.buyTokens(it) } },
            onBuyTokens100 = { hostActivity?.let { viewModel.buyTokens100(it) } },
            payoutValueCents = myPayoutValueCents,
            myPayoutRequests = myPayoutRequests,
            onRequestPayout = { method, destination ->
                viewModel.requestPayout(method, destination)
                Toast.makeText(context, "Payout requested", Toast.LENGTH_SHORT).show()
            },
            onClaimCredit = { viewModel.claimCredit() },
            onSendCredit = { email, amount ->
                viewModel.sendCredit(email, amount) { ok ->
                    Toast.makeText(context, if (ok) "Credit sent" else "Credit could not be sent", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.dismissPaywall() }
        )
    }

    if (manuscriptToSync != null) {
        SyncToGoogleDriveDialog(
            manuscript = manuscriptToSync!!,
            currentUser = currentUser,
            isSyncing = driveSyncStatus.isSyncing,
            onDismiss = { manuscriptToSync = null },
            onConfirmSync = { isPublic ->
                val target = manuscriptToSync!!
                viewModel.syncManuscript(target, isPublic) { res ->
                    if (res.isSuccess) {
                        Toast.makeText(context, "Synced “${target.title}” to Google Drive!", Toast.LENGTH_LONG).show()
                        manuscriptToSync = null
                    } else {
                        Toast.makeText(context, "Sync Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBackupKey = { passphrase, onDone -> viewModel.backupAuthorKey(passphrase, onDone) },
            onRestoreKey = { passphrase, onDone -> viewModel.restoreAuthorKey(passphrase, onDone) }
        )
    }

    if (showNewWorkDialog) {
        NewWorkDialog(
            initialAuthorName = currentUser.name,
            initialPenName = currentUser.penName,
            onDismiss = { showNewWorkDialog = false },
            onCreate = { title, subtitle, workType, author, penName, publisher, year, dedication, epigraph ->
                viewModel.createManuscript(
                    title = title,
                    subtitle = subtitle,
                    workType = workType,
                    authorName = author,
                    authorPenName = penName,
                    publisher = publisher,
                    year = year,
                    dedication = dedication,
                    epigraph = epigraph
                ) { newId ->
                    showNewWorkDialog = false
                    navController.navigate("manuscript_detail/$newId")
                }
            }
        )
    }
}

