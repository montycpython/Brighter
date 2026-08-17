package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.ManuscriptEntity
import com.example.model.ServerlessMailMessage
import com.example.model.WorkRole
import com.example.model.WorkType
import com.example.ui.screens.AdminGodModeDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CmosRulebookScreen
import com.example.ui.screens.CommunityDirectoryScreen
import com.example.ui.screens.KillSwitchSuspensionScreen
import com.example.ui.screens.LeafReaderScreen
import com.example.ui.screens.MailboxInboxScreen
import com.example.ui.screens.ManuscriptDetailScreen
import com.example.ui.screens.ManuscriptListScreen
import com.example.ui.screens.NewWorkDialog
import com.example.ui.screens.PdfExportScreen
import com.example.ui.screens.SectionEditorScreen
import com.example.ui.screens.SubscriptionPaywallDialog
import com.example.ui.screens.SyncToGoogleDriveDialog
import com.example.ui.screens.UserAgreementScreen
import com.example.ui.theme.BwriterTheme
import com.example.ui.viewmodel.BwriterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BwriterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    // Google Drive & Serverless Governance State
    val driveSyncStatus by viewModel.driveSyncStatus.collectAsState()
    val globalBookIndex by viewModel.globalBookIndex.collectAsState()
    val suspendedUsers by viewModel.suspendedUsers.collectAsState()
    val mailboxMessages by viewModel.mailboxMessages.collectAsState()
    val activeSuspension by viewModel.activeSuspension.collectAsState()
    val hasAcceptedTerms by viewModel.hasAcceptedTerms.collectAsState()
    val userSubscription by viewModel.userAiSubscription.collectAsState()
    val tokenTransactions by viewModel.tokenTransactions.collectAsState()
    val paidMembersTelemetry by viewModel.paidMembersTelemetry.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()

    var showNewWorkDialog by remember { mutableStateOf(false) }
    var manuscriptToSync by remember { mutableStateOf<ManuscriptEntity?>(null) }

    // Mandatory First-Launch / First Sign-in User Agreement Gate
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

    // Hard-lock Kill-Switch: if this account is suspended in suspended_users.json
    if (activeSuspension != null) {
        KillSwitchSuspensionScreen(
            userProfile = currentUser,
            suspension = activeSuspension!!,
            onRetrySync = {
                viewModel.refreshDriveNetwork()
                Toast.makeText(context, "Verifying Shared Drive status...", Toast.LENGTH_SHORT).show()
            },
            onSwitchUser = {
                navController.navigate("auth")
            }
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = "manuscripts"
    ) {
        // Auth / Google Identity & Role Screen
        composable("auth") {
            AuthScreen(
                currentUser = currentUser,
                onRoleSelected = { role -> viewModel.updateRole(role) },
                onGoogleSignIn = { email, name, penName, role ->
                    viewModel.signInWithGoogleAccount(email, name, penName, role)
                },
                onContinue = {
                    navController.popBackStack()
                }
            )
        }

        // Manuscript Library Dashboard
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
                onSwitchRole = { newRole ->
                    viewModel.updateRole(newRole)
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
                    viewModel.refreshDriveNetwork()
                    navController.navigate("community_directory")
                },
                onOpenMailbox = {
                    viewModel.refreshDriveNetwork()
                    navController.navigate("mailbox_inbox")
                },
                onOpenAdminDashboard = {
                    viewModel.refreshDriveNetwork()
                    navController.navigate("admin_god_mode")
                },
                onSyncManuscript = { manuscript ->
                    manuscriptToSync = manuscript
                },
                onOpenUserAgreement = {
                    navController.navigate("user_agreement")
                },
                onOpenSubscription = {
                    viewModel.openPaywall()
                },
                unreadMailCount = driveSyncStatus.unreadMailCount
            )
        }

        // Manuscript Detail (Divisions, Structure, Leaf Flow)
        composable(
            route = "manuscript_detail/{manuscriptId}",
            arguments = listOf(navArgument("manuscriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val manuscriptId = backStackEntry.arguments?.getLong("manuscriptId") ?: return@composable
            viewModel.selectManuscript(manuscriptId)

            val currentM = activeManuscript
            if (currentM != null) {
                ManuscriptDetailScreen(
                    manuscript = currentM,
                    sections = activeSections,
                    calculatedLeaves = calculatedLeaves,
                    currentUser = currentUser,
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

        // Section Drafting Studio
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
                    subscription = userSubscription,
                    onOpenSubscription = { viewModel.openPaywall() },
                    onSaveAssignment = { sec, author, role, notes ->
                        viewModel.updateSectionAssignment(sec, author, role, notes)
                    },
                    onSendServerlessMail = { mail ->
                        viewModel.sendMailMessage(mail)
                    }
                )
            }
        }

        // Two-Page Spread Leaf Reader
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
                    onBack = { navController.popBackStack() },
                    onOpenExport = {
                        navController.navigate("pdf_export/$manuscriptId")
                    }
                )
            }
        }

        // CMOS PDF Exporter
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
                    onBack = { navController.popBackStack() },
                    onExportPdf = {
                        viewModel.exportActiveManuscriptToPdf(context) { /* Completed */ }
                    }
                )
            }
        }

        // CMOS Rulebook
        composable("cmos_rulebook") {
            CmosRulebookScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // User Agreement / EULA / Terms of Service (Viewable anytime)
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

        // Community Public/Private Book Directory
        composable("community_directory") {
            CommunityDirectoryScreen(
                books = globalBookIndex,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshDriveNetwork() }
            )
        }

        // Serverless Mailbox Inbox
        composable("mailbox_inbox") {
            MailboxInboxScreen(
                currentUser = currentUser,
                messages = mailboxMessages,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshDriveNetwork() },
                onMarkAsRead = { msgId -> viewModel.markMailMessageAsRead(msgId) }
            )
        }

        // Master Admin God-Mode Control Panel (Editor in Chief)
        composable("admin_god_mode") {
            AdminGodModeDashboardScreen(
                currentUser = currentUser,
                globalIndex = globalBookIndex,
                suspendedUsers = suspendedUsers,
                paidSubscribers = paidMembersTelemetry,
                tokenTransactions = tokenTransactions,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshDriveNetwork() },
                onSuspendUser = { email, reason ->
                    viewModel.suspendUserAccount(email, reason)
                    Toast.makeText(context, "User $email suspended (Kill-Switch deployed)", Toast.LENGTH_SHORT).show()
                },
                onUnsuspendUser = { email ->
                    viewModel.unsuspendUserAccount(email)
                    Toast.makeText(context, "User $email unsuspended", Toast.LENGTH_SHORT).show()
                },
                onRevokeManuscript = { fileId ->
                    viewModel.revokeManuscriptDriveAccess(fileId)
                    Toast.makeText(context, "Manuscript access revoked", Toast.LENGTH_SHORT).show()
                },
                onSendServerlessMail = { mail ->
                    viewModel.sendServerlessMailDirective(mail)
                    Toast.makeText(context, "Directive dispatched to ${mail.recipientEmail}", Toast.LENGTH_SHORT).show()
                },
                onGrantBonusCredits = { targetEmail, bonus ->
                    viewModel.adminGrantBonusCredits(targetEmail, bonus)
                    Toast.makeText(context, "Granted $bonus bonus credits to $targetEmail", Toast.LENGTH_SHORT).show()
                },
                onUpdateProfile = { name, penName, email, role, org, cmos ->
                    viewModel.updateProfile(name, penName, email, role, org, cmos)
                    val byline = if (penName.isNotBlank()) penName else name
                    Toast.makeText(context, "Profile updated: $byline", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // AI Studio Subscription & Token Ledger Dialog
    if (showPaywall) {
        SubscriptionPaywallDialog(
            subscription = userSubscription,
            recentTransactions = tokenTransactions,
            onDismiss = { viewModel.dismissPaywall() },
            onSelectPlan = { plan ->
                viewModel.updateSubscriptionPlan(plan)
                Toast.makeText(context, "Upgraded to ${plan.title}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Google Drive Sync Modal Dialog
    if (manuscriptToSync != null) {
        SyncToGoogleDriveDialog(
            manuscript = manuscriptToSync!!,
            currentUser = currentUser,
            isSyncing = driveSyncStatus.isSyncing,
            onDismiss = { manuscriptToSync = null },
            onConfirmSync = { isPublic ->
                val target = manuscriptToSync!!
                viewModel.syncManuscriptToGoogleDrive(target, isPublic) { res ->
                    if (res.isSuccess) {
                        Toast.makeText(context, "Synced “${target.title}” to Google Drive!", Toast.LENGTH_LONG).show()
                        manuscriptToSync = null
                    } else {
                        Toast.makeText(context, "Sync Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // New Work Scaffolding Dialog
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
