package com.example

import android.os.Bundle
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.WorkRole
import com.example.model.WorkType
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CmosRulebookScreen
import com.example.ui.screens.LeafReaderScreen
import com.example.ui.screens.ManuscriptDetailScreen
import com.example.ui.screens.ManuscriptListScreen
import com.example.ui.screens.NewWorkDialog
import com.example.ui.screens.PdfExportScreen
import com.example.ui.screens.SectionEditorScreen
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
    val navController = rememberNavController()

    val currentUser by viewModel.currentUser.collectAsState()
    val manuscripts by viewModel.manuscripts.collectAsState()
    val selectedFilter by viewModel.selectedWorkTypeFilter.collectAsState()
    val activeManuscript by viewModel.activeManuscript.collectAsState()
    val activeSections by viewModel.activeSections.collectAsState()
    val calculatedLeaves by viewModel.calculatedLeaves.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()
    val activeComments by viewModel.activeSectionComments.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var showNewWorkDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = "manuscripts"
    ) {
        // Auth / Google Identity & Role Screen
        composable("auth") {
            AuthScreen(
                currentUser = currentUser,
                onRoleSelected = { role -> viewModel.updateRole(role) },
                onGoogleSignIn = { email, name, role ->
                    viewModel.signInWithGoogleAccount(email, name, role)
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
                onCreateNewWorkClick = {
                    showNewWorkDialog = true
                }
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
                    onBack = { navController.popBackStack() },
                    onSaveContent = { sec, content ->
                        viewModel.updateSectionContent(sec, content)
                    },
                    onSaveTitle = { sec, title, subtitle ->
                        viewModel.updateSectionTitle(sec, title, subtitle)
                    },
                    onUpdateStatus = { sec, status ->
                        viewModel.updateSectionStatus(sec, status)
                    },
                    onAddComment = { sId, mId, text, cmosRef ->
                        viewModel.addEditorialComment(sId, mId, text, cmosRef)
                    },
                    onResolveComment = { commentId, resolved ->
                        viewModel.resolveComment(commentId, resolved)
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
                val context = androidx.compose.ui.platform.LocalContext.current
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
    }

    // New Work Scaffolding Dialog
    if (showNewWorkDialog) {
        NewWorkDialog(
            initialAuthorName = currentUser.name,
            onDismiss = { showNewWorkDialog = false },
            onCreate = { title, subtitle, workType, author, publisher, year, dedication, epigraph ->
                viewModel.createManuscript(
                    title = title,
                    subtitle = subtitle,
                    workType = workType,
                    authorName = author,
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
