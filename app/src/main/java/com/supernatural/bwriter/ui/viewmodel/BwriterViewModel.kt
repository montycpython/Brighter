package com.supernatural.bwriter.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supernatural.bwriter.cmos.CmosLeafEngine
import com.supernatural.bwriter.data.BwriterDatabase
import com.supernatural.bwriter.data.BwriterRepository
import com.supernatural.bwriter.data.UserPreferences
import com.supernatural.bwriter.data.FirestoreNetworkService
import com.supernatural.bwriter.data.GoogleDriveClient
import com.supernatural.bwriter.export.CmosPdfExporter
import com.supernatural.bwriter.model.DriveSyncStatus
import com.supernatural.bwriter.model.GlobalBookIndexEntry
import com.supernatural.bwriter.model.BookVersionSnapshot
import com.supernatural.bwriter.model.ServerlessMailMessage
import com.supernatural.bwriter.model.SuspendedUserEntry
import com.supernatural.bwriter.model.CalculatedLeaf
import com.supernatural.bwriter.model.EditorialCommentEntity
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.MatterType
import com.supernatural.bwriter.model.SectionEntity
import com.supernatural.bwriter.model.SectionStatus
import com.supernatural.bwriter.model.SectionType
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.model.WorkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class BwriterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BwriterRepository
    private val userPreferences = UserPreferences(application)

    // ==========================================
    // User & Google Identity (Persisted)
    // ==========================================
    private val _currentUser = MutableStateFlow(userPreferences.getUserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(userPreferences.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _savedGoogleAccounts = MutableStateFlow(userPreferences.getSavedGoogleAccounts())
    val savedGoogleAccounts: StateFlow<List<UserProfile>> = _savedGoogleAccounts.asStateFlow()

    private val _hasAcceptedTerms = MutableStateFlow(userPreferences.hasAcceptedTerms())
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms.asStateFlow()

    fun acceptTermsOfService(version: String = com.supernatural.bwriter.ui.screens.CURRENT_TERMS_VERSION) {
        userPreferences.setAcceptedTerms(version)
        _hasAcceptedTerms.value = true
    }

    init {
        val db = BwriterDatabase.getDatabase(application)
        repository = BwriterRepository(db)
        viewModelScope.launch {
            refreshNetwork()
        }
    }

    fun updateProfile(
        name: String,
        penName: String,
        email: String,
        role: WorkRole,
        organization: String = _currentUser.value.organization,
        preferredCmosEdition: String = _currentUser.value.preferredCmosEdition
    ) {
        val updated = _currentUser.value.copy(
            name = if (name.isNotBlank()) name.trim() else "Author",
            penName = penName.trim(),
            email = if (email.isNotBlank()) email.trim() else _currentUser.value.email,
            role = standingRoleFor(),
            organization = if (organization.isNotBlank()) organization.trim() else "Author Studio",
            preferredCmosEdition = preferredCmosEdition
        )
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
        userPreferences.addOrUpdateSavedAccount(updated)
        _savedGoogleAccounts.value = userPreferences.getSavedGoogleAccounts()
        refreshNetwork()
    }

    fun signInWithGoogleAccount(
        email: String,
        name: String = "",
        penName: String = "",
        role: WorkRole = WorkRole.AUTHOR,
        organization: String = "Author Studio",
        preferredCmosEdition: String = "17th Edition"
    ) {
        val effectiveEmail = if (email.isNotBlank()) email.trim() else _currentUser.value.email
        val effectiveName = if (name.isNotBlank()) name.trim() else (if (effectiveEmail.contains("@")) effectiveEmail.substringBefore("@").replace(".", " ").capitalizeWords() else "Author")
        val effectivePenName = penName.trim()

        val isEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief
        val effectiveRole = standingRoleFor()

        val updated = UserProfile(
            id = "google_user_${effectiveEmail.replace("[^a-zA-Z0-9]".toRegex(), "_")}",
            email = effectiveEmail,
            name = effectiveName,
            penName = effectivePenName,
            role = effectiveRole,
            organization = if (isEditorInChief) "Bwriter Editorial Board" else organization.trim().ifBlank { "Author Studio" },
            preferredCmosEdition = preferredCmosEdition
        )
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
        userPreferences.addOrUpdateSavedAccount(updated)
        userPreferences.setLoggedIn(true)
        _isLoggedIn.value = true
        _savedGoogleAccounts.value = userPreferences.getSavedGoogleAccounts()
        _userAiSubscription.value = userPreferences.getUserSubscription(updated.email)
        restoreManuscriptsFromGoogleDrive()
        refreshNetwork()
    }

    /** Standing is proven against the back room: the Editor in Chief is
     * an editor; everyone else is an author until invited to edit a book. */
    private fun standingRoleFor(): WorkRole {
        return if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief) WorkRole.EDITOR else WorkRole.AUTHOR
    }

    fun switchGoogleAccount(account: UserProfile) {
        com.supernatural.bwriter.data.StudioStanding.isEditorInChief = false
        val corrected = account.copy(role = standingRoleFor())
        _currentUser.value = corrected
        userPreferences.saveUserProfile(corrected)
        userPreferences.addOrUpdateSavedAccount(corrected)
        userPreferences.setLoggedIn(true)
        _isLoggedIn.value = true
        _savedGoogleAccounts.value = userPreferences.getSavedGoogleAccounts()
        _userAiSubscription.value = userPreferences.getUserSubscription(corrected.email)
        restoreManuscriptsFromGoogleDrive()
        refreshNetwork()
    }

    fun signOutGoogleAccount() {
        com.supernatural.bwriter.auth.FirebaseAuthBridge.signOut()
        com.supernatural.bwriter.data.StudioStanding.isEditorInChief = false
        userPreferences.signOut()
        _isLoggedIn.value = false
    }

    fun removeSavedGoogleAccount(email: String) {
        userPreferences.removeSavedGoogleAccount(email)
        _savedGoogleAccounts.value = userPreferences.getSavedGoogleAccounts()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
    }

    // ==========================================
    // Manuscript Library (Empty for new users, Master works for Superuser)
    // ==========================================
    private val _selectedWorkTypeFilter = MutableStateFlow<WorkType?>(null)
    val selectedWorkTypeFilter: StateFlow<WorkType?> = _selectedWorkTypeFilter.asStateFlow()

    fun setWorkTypeFilter(type: WorkType?) {
        _selectedWorkTypeFilter.value = type
    }

    val manuscripts: StateFlow<List<ManuscriptEntity>> = combine(
        repository.allManuscripts,
        _selectedWorkTypeFilter,
        _currentUser
    ) { list, filter, user ->
        val isEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief
        val userBooks = if (isEditorInChief) {
            list
        } else {
            list.filter { m ->
                m.authorEmail.equals(user.email, ignoreCase = true) ||
                (user.penName.isNotBlank() && m.authorPenName.equals(user.penName, ignoreCase = true)) ||
                (user.name.isNotBlank() && user.name != "Author" && m.authorName.equals(user.name, ignoreCase = true))
            }
        }
        if (filter == null) userBooks else userBooks.filter { it.workType == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // Active Manuscript
    // ==========================================
    private val _selectedManuscriptId = MutableStateFlow<Long?>(null)
    val selectedManuscriptId: StateFlow<Long?> = _selectedManuscriptId.asStateFlow()

    fun selectManuscript(id: Long) {
        _selectedManuscriptId.value = id
    }

    val activeManuscript: StateFlow<ManuscriptEntity?> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getManuscriptById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val activeSections: StateFlow<List<SectionEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getSectionsForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculated Recto-Verso Leaves for active manuscript
    val calculatedLeaves: StateFlow<List<CalculatedLeaf>> = combine(
        activeManuscript,
        activeSections
    ) { manuscript, sections ->
        if (manuscript == null) emptyList()
        else CmosLeafEngine.calculateLeaves(manuscript, sections)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // Active Section Editing
    // ==========================================
    private val _selectedSectionId = MutableStateFlow<Long?>(null)
    val selectedSectionId: StateFlow<Long?> = _selectedSectionId.asStateFlow()

    fun selectSection(id: Long) {
        _selectedSectionId.value = id
    }

    val activeSection: StateFlow<SectionEntity?> = _selectedSectionId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getSectionById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val activeSectionComments: StateFlow<List<EditorialCommentEntity>> = _selectedSectionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCommentsForSection(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // Character Profiling for Active Manuscript
    // ==========================================
    val charactersForActiveManuscript: StateFlow<List<com.supernatural.bwriter.model.CharacterEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCharactersForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveCharacter(character: com.supernatural.bwriter.model.CharacterEntity) {
        viewModelScope.launch {
            if (character.id == 0L) {
                repository.insertCharacter(character)
            } else {
                repository.updateCharacter(character)
            }
        }
    }

    fun deleteCharacter(characterId: Long) {
        viewModelScope.launch {
            repository.deleteCharacterById(characterId)
        }
    }

    // ==========================================
    // Story Settings & Intertextuality
    // ==========================================
    val settingsForActiveManuscript: StateFlow<List<com.supernatural.bwriter.model.StorySettingEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getSettingsForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // Book Version Snapshots & History Ledger
    // ==========================================
    private val _versionHistory = MutableStateFlow<List<BookVersionSnapshot>>(emptyList())
    val versionHistory: StateFlow<List<BookVersionSnapshot>> = _versionHistory.asStateFlow()

    fun loadVersionHistory(manuscriptId: Long) {
        viewModelScope.launch {
            _versionHistory.value = networkService.getVersionHistory(manuscriptId, _currentUser.value.email)
        }
    }

    fun saveSetting(setting: com.supernatural.bwriter.model.StorySettingEntity) {
        viewModelScope.launch {
            if (setting.id == 0L) {
                repository.insertSetting(setting)
            } else {
                repository.updateSetting(setting)
            }
        }
    }

    fun deleteSetting(settingId: Long) {
        viewModelScope.launch {
            repository.deleteSettingById(settingId)
        }
    }

    // ==========================================
    // AI Prose Generation & Subscription State
    // ==========================================
    private val _isGeneratingAiProse = MutableStateFlow(false)
    val isGeneratingAiProse: StateFlow<Boolean> = _isGeneratingAiProse.asStateFlow()

    private val _userAiSubscription = MutableStateFlow(userPreferences.getUserSubscription(_currentUser.value.email))
    val userAiSubscription: StateFlow<com.supernatural.bwriter.model.UserAiSubscription> = _userAiSubscription.asStateFlow()

    private val _tokenTransactions = MutableStateFlow(userPreferences.getTokenTransactions())
    val tokenTransactions: StateFlow<List<com.supernatural.bwriter.model.AiTokenTransaction>> = _tokenTransactions.asStateFlow()


    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    fun openPaywall() {
        _showPaywall.value = true
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    fun generateAiDraftFromPrompt(
        section: SectionEntity,
        prompt: String,
        onGenerated: (String) -> Unit,
        onRequiresUpgrade: () -> Unit = { openPaywall() }
    ) {
        val currentSub = _userAiSubscription.value
        val isEditorInChief = com.supernatural.bwriter.data.StudioStanding.isEditorInChief

        if (!isEditorInChief && currentSub.creditsRemaining <= 0) {
            onRequiresUpgrade()
            return
        }

        _isGeneratingAiProse.value = true
        viewModelScope.launch {
            try {
                val result = com.supernatural.bwriter.ai.GeminiProseGenerator.generateChapterProse(prompt)
                val aiResult = result.getOrNull()
                if (aiResult != null && aiResult.text.isNotBlank()) {
                    val generatedText = aiResult.text
                    val updatedContent = if (section.content.isBlank()) {
                        generatedText
                    } else {
                        "${section.content.trimEnd()}\n\n$generatedText"
                    }
                    repository.updateSection(section.copy(content = updatedContent, aiDraftPrompt = prompt))

                    // Record Token burn and credit deduction in Ledger
                    val promptTokens = aiResult.promptTokens
                    val compTokens = aiResult.completionTokens
                    val totalTokens = aiResult.totalTokens
                    val modelUsed = aiResult.modelUsed

                    val updatedSub = userPreferences.recordTokenUsage(
                        email = _currentUser.value.email,
                        sectionTitle = section.title,
                        promptTokens = promptTokens,
                        completionTokens = compTokens,
                        totalTokens = totalTokens,
                        modelUsed = modelUsed
                    )
                    _userAiSubscription.value = updatedSub
                    _tokenTransactions.value = userPreferences.getTokenTransactions()

                    onGenerated(generatedText)
                }
            } finally {
                _isGeneratingAiProse.value = false
            }
        }
    }

    fun createManuscript(
        title: String,
        subtitle: String,
        workType: WorkType,
        authorName: String,
        authorPenName: String,
        publisher: String,
        year: String,
        dedication: String,
        epigraph: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val author = authorName.ifBlank { _currentUser.value.name }
            val pen = authorPenName.trim()
            val pub = publisher.ifBlank { "Bwriter Editions" }
            val yr = year.ifBlank { "2026" }
            val byline = if (pen.isNotBlank()) pen else author.ifBlank { "Author" }
            val dynamicCopyright = "Copyright © $yr by $byline.\nAll rights reserved under International and Pan-American Copyright Conventions.\nPublished by $pub in accordance with The Chicago Manual of Style.\nPrinted in the United States of America."

            val manuscript = ManuscriptEntity(
                title = title.trim(),
                subtitle = subtitle.trim(),
                workType = workType,
                authorName = author,
                authorPenName = pen,
                authorEmail = _currentUser.value.email,
                publisher = pub,
                year = yr,
                copyrightText = dynamicCopyright,
                dedication = dedication.trim(),
                epigraphText = epigraph.trim(),
                manuscriptStatus = "DRAFT"
            )
            val newId = repository.createManuscript(manuscript, populateTemplate = true)
            _selectedManuscriptId.value = newId
            onCreated(newId)
        }
    }

    fun updateManuscriptDetails(updated: ManuscriptEntity) {
        viewModelScope.launch {
            repository.updateManuscript(updated)
        }
    }

    fun deleteManuscript(id: Long) {
        viewModelScope.launch {
            repository.deleteManuscript(id)
            if (_selectedManuscriptId.value == id) {
                _selectedManuscriptId.value = null
            }
        }
    }

    fun addSection(
        manuscriptId: Long,
        matterType: MatterType,
        sectionType: SectionType,
        title: String,
        subtitle: String = "",
        content: String = ""
    ) {
        viewModelScope.launch {
            val maxOrder = (activeSections.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            val section = SectionEntity(
                manuscriptId = manuscriptId,
                matterType = matterType,
                sectionType = sectionType,
                title = title,
                subtitle = subtitle,
                orderIndex = maxOrder,
                content = content,
                assignedAuthor = _currentUser.value.displayName,
                assignedRole = _currentUser.value.role,
                status = SectionStatus.DRAFT,
                startOnRecto = sectionType.requiresRectoStart
            )
            repository.insertSection(section)
        }
    }

    fun updateSectionContent(section: SectionEntity, newContent: String) {
        viewModelScope.launch {
            repository.updateSection(section.copy(content = newContent))
        }
    }

    fun updateSectionAiPrompt(section: SectionEntity, prompt: String) {
        viewModelScope.launch {
            repository.updateSection(section.copy(aiDraftPrompt = prompt))
        }
    }

    fun updateSectionTitle(section: SectionEntity, newTitle: String, newSubtitle: String) {
        viewModelScope.launch {
            repository.updateSection(section.copy(title = newTitle, subtitle = newSubtitle))
        }
    }

    fun updateSectionStatus(section: SectionEntity, newStatus: SectionStatus) {
        viewModelScope.launch {
            repository.updateSection(section.copy(status = newStatus))
        }
    }

    fun updateSectionAssignment(
        section: SectionEntity,
        assignedAuthor: String,
        assignedRole: WorkRole,
        contributorNotes: String
    ) {
        viewModelScope.launch {
            repository.updateSection(
                section.copy(
                    assignedAuthor = assignedAuthor,
                    assignedRole = assignedRole,
                    contributorNotes = contributorNotes
                )
            )
        }
    }

    fun updateSectionIllustrations(
        section: SectionEntity,
        headerUri: String,
        headerCaption: String,
        tailUri: String,
        tailCaption: String
    ) {
        viewModelScope.launch {
            repository.updateSection(
                section.copy(
                    headerIllustrationUri = headerUri,
                    headerIllustrationCaption = headerCaption,
                    tailIllustrationUri = tailUri,
                    tailIllustrationCaption = tailCaption
                )
            )
        }
    }

    fun deleteSection(sectionId: Long, manuscriptId: Long) {
        viewModelScope.launch {
            repository.deleteSection(sectionId, manuscriptId)
            if (_selectedSectionId.value == sectionId) {
                _selectedSectionId.value = null
            }
        }
    }

    // ==========================================
    // Collaboration: Revisions & Tracked Changes
    // ==========================================
    fun proposeSectionRevision(
        section: SectionEntity,
        newContent: String
    ) {
        viewModelScope.launch {
            repository.proposeRevision(
                section = section,
                newContent = newContent,
                editorUser = _currentUser.value
            )
            // Auto-send a notification to the author's mailbox or Editor in Chief
            val manuscript = repository.getManuscriptByIdOnce(section.manuscriptId)
            if (manuscript != null && !manuscript.authorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
                val editorPenName = if (_currentUser.value.penName.isNotBlank()) _currentUser.value.penName else _currentUser.value.name
                networkService.sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = manuscript.authorEmail.ifBlank { FirestoreNetworkService.DEFAULT_STUDIO_AUTHOR_EMAIL },
                        senderEmail = _currentUser.value.email,
                        senderName = editorPenName,
                        subject = "Revision Proposed for '${section.title}'",
                        body = "Editor/Contributor $editorPenName (${_currentUser.value.role.displayName}) submitted a tracked revision for '${section.title}' in '${manuscript.title}'.\n\nThe chapter is now Under Review. You can review the redline differences and Accept or Reject the changes.",
                        manuscriptId = manuscript.id,
                        manuscriptTitle = manuscript.title,
                        messageType = "EDITORIAL_REVISION"
                    )
                )
            }
            refreshNetwork()
        }
    }

    fun acceptSectionRevision(section: SectionEntity) {
        viewModelScope.launch {
            repository.acceptRevision(section)
            if (section.revisionAuthorEmail.isNotBlank() && !section.revisionAuthorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
                networkService.sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = section.revisionAuthorEmail,
                        senderEmail = _currentUser.value.email,
                        senderName = _currentUser.value.displayName,
                        subject = "Revision Accepted: ${section.title}",
                        body = "Your editorial revisions for '${section.title}' have been accepted and merged by the author. You have been credited in the official Acknowledgments!",
                        manuscriptId = section.manuscriptId,
                        messageType = "EDITORIAL_REVISION"
                    )
                )
            }
            refreshNetwork()
        }
    }

    fun rejectSectionRevision(section: SectionEntity) {
        viewModelScope.launch {
            repository.rejectRevision(section)
            if (section.revisionAuthorEmail.isNotBlank() && !section.revisionAuthorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
                networkService.sendServerlessMail(
                    ServerlessMailMessage(
                        recipientEmail = section.revisionAuthorEmail,
                        senderEmail = _currentUser.value.email,
                        senderName = _currentUser.value.displayName,
                        subject = "Revision Update: ${section.title}",
                        body = "The proposed revision for '${section.title}' was reviewed by the author and discarded.",
                        manuscriptId = section.manuscriptId,
                        messageType = "EDITORIAL_REVISION"
                    )
                )
            }
            refreshNetwork()
        }
    }

    fun addEditorialComment(sectionId: Long, manuscriptId: Long, text: String, cmosRef: String = "") {
        viewModelScope.launch {
            val comment = EditorialCommentEntity(
                sectionId = sectionId,
                manuscriptId = manuscriptId,
                authorName = _currentUser.value.displayName,
                authorRole = _currentUser.value.role,
                commentText = text,
                cmosRuleReference = cmosRef
            )
            repository.addComment(comment)
        }
    }

    fun resolveComment(commentId: Long, resolved: Boolean) {
        viewModelScope.launch {
            repository.setCommentResolved(commentId, resolved)
        }
    }

    // ==========================================
    // PDF Export
    // ==========================================
    private val _exportResult = MutableStateFlow<CmosPdfExporter.ExportResult?>(null)
    val exportResult: StateFlow<CmosPdfExporter.ExportResult?> = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportActiveManuscriptToPdf(context: Context, onComplete: (com.supernatural.bwriter.export.CmosPdfExporter.ExportResult) -> Unit) {
        val m = activeManuscript.value ?: return
        val s = activeSections.value
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val result = com.supernatural.bwriter.export.CmosPdfExporter.exportToPdf(context, m, s)
                _exportResult.value = result
                onComplete(result)
            } finally {
                _isExporting.value = false
            }
        }
    }

    // ==========================================
    // Google Drive & Serverless Governance Ecosystem
    // ==========================================
    val networkService = FirestoreNetworkService(application)

    private val googleDriveClient = GoogleDriveClient(application)

    private val _driveSyncStatus = MutableStateFlow(DriveSyncStatus())
    val driveSyncStatus: StateFlow<DriveSyncStatus> = _driveSyncStatus.asStateFlow()

    private val _globalBookIndex = MutableStateFlow<List<GlobalBookIndexEntry>>(emptyList())
    val globalBookIndex: StateFlow<List<GlobalBookIndexEntry>> = _globalBookIndex.asStateFlow()

    private val _pendingBooks = MutableStateFlow<List<GlobalBookIndexEntry>>(emptyList())
    val pendingBooks: StateFlow<List<GlobalBookIndexEntry>> = _pendingBooks.asStateFlow()

    private val _suspendedUsers = MutableStateFlow<List<SuspendedUserEntry>>(emptyList())
    val suspendedUsers: StateFlow<List<SuspendedUserEntry>> = _suspendedUsers.asStateFlow()

    private val _mailboxMessages = MutableStateFlow<List<ServerlessMailMessage>>(emptyList())
    val mailboxMessages: StateFlow<List<ServerlessMailMessage>> = _mailboxMessages.asStateFlow()

    private val _activeSuspension = MutableStateFlow<SuspendedUserEntry?>(null)
    val activeSuspension: StateFlow<SuspendedUserEntry?> = _activeSuspension.asStateFlow()

    fun checkAccountSuspension() {
        viewModelScope.launch {
            val suspension = networkService.checkUserSuspension(_currentUser.value.email)
            _activeSuspension.value = suspension
            _driveSyncStatus.value = _driveSyncStatus.value.copy(
                isSuspended = suspension != null,
                suspensionReason = suspension?.reason
            )
        }
    }

    private fun bookKey(title: String, authorEmail: String): String =
        title.trim().lowercase() + "|" + authorEmail.trim().lowercase()

    fun restoreManuscriptsFromGoogleDrive(interactive: Boolean = false, onRestored: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val restored = networkService.restoreUserManuscriptsFromBackRoom(_currentUser.value.email)
            val knownKeys = manuscripts.value.map { bookKey(it.title, it.authorEmail) }.toMutableSet()
            var total = 0
            for (pair in restored) {
                val manuscript = pair.first
                val sections = pair.second
                val mId = repository.createManuscript(manuscript, populateTemplate = false)
                for (sec in sections) {
                    repository.insertSection(sec.copy(manuscriptId = mId))
                }
                knownKeys.add(bookKey(manuscript.title, manuscript.authorEmail))
                total++
            }

            // The writer's own Google Drive: bring back every book saved
            // there that this device does not already hold.
            val tokenResult = googleDriveClient.accessToken(interactive = interactive)
            if (tokenResult.isSuccess) {
                val payloads = googleDriveClient.downloadAllPayloads(tokenResult.getOrThrow())
                for (payload in payloads) {
                    val parsed = networkService.parsePayloadJson(payload, _currentUser.value.email) ?: continue
                    if (!knownKeys.add(bookKey(parsed.first.title, parsed.first.authorEmail))) continue
                    val mId = repository.createManuscript(parsed.first, populateTemplate = false)
                    for (sec in parsed.second) {
                        repository.insertSection(sec.copy(manuscriptId = mId))
                    }
                    total++
                }
            }

            refreshNetwork()
            onRestored(total)
        }
    }

    // Registry fileId -> local manuscript id, for books imported from the
    // registry as reading copies on this device.
    private val registryCopyPrefs by lazy {
        getApplication<Application>().getSharedPreferences("bwriter_registry_copies", Context.MODE_PRIVATE)
    }

    /**
     * Resolve a registry entry to a local manuscript this device can open.
     * The author's own book resolves to their own local copy. Another
     * writer's book is fetched whole from the registry on first open and
     * shelved as a reading copy; later opens reuse that copy.
     * onReady receives the local id, or -1 if the text could not be fetched.
     */
    fun openCommunityBook(entry: GlobalBookIndexEntry, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            if (entry.authorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
                onReady(entry.manuscriptId)
                return@launch
            }
            val savedId = registryCopyPrefs.getLong(entry.fileId, -1L)
            if (savedId > 0 && repository.getManuscriptByIdOnce(savedId) != null) {
                onReady(savedId)
                return@launch
            }
            val fetched = networkService.fetchRegistryBook(entry.fileId)
            if (fetched == null) {
                onReady(-1L)
                return@launch
            }
            val mId = repository.createManuscript(fetched.first, populateTemplate = false)
            for (sec in fetched.second) {
                repository.insertSection(sec.copy(manuscriptId = mId))
            }
            registryCopyPrefs.edit().putLong(entry.fileId, mId).apply()
            onReady(mId)
        }
    }

    fun refreshNetwork() {
        viewModelScope.launch {
            com.supernatural.bwriter.data.StudioStanding.isEditorInChief = networkService.verifyEditorInChief()
            val provenStanding = com.supernatural.bwriter.data.StudioStanding.isEditorInChief
            val userNow = _currentUser.value
            if (provenStanding && userNow.role != WorkRole.EDITOR) {
                val corrected = userNow.copy(role = WorkRole.EDITOR, organization = "Bwriter Editorial Board")
                _currentUser.value = corrected
                userPreferences.saveUserProfile(corrected)
                _userAiSubscription.value = userPreferences.getUserSubscription(corrected.email)
            } else if (!provenStanding && userNow.role == WorkRole.EDITOR && userNow.email.isNotBlank()) {
                val corrected = userNow.copy(role = WorkRole.AUTHOR)
                _currentUser.value = corrected
                userPreferences.saveUserProfile(corrected)
                _userAiSubscription.value = userPreferences.getUserSubscription(corrected.email)
            }

            checkAccountSuspension()

            // Fetch index
            val indexResult = networkService.fetchGlobalBookIndex(_currentUser.value)
            if (indexResult.isSuccess) {
                _globalBookIndex.value = indexResult.getOrNull() ?: emptyList()
            }

            // Fetch mail
            val mailResult = networkService.fetchMailboxMessages(_currentUser.value.email)
            if (mailResult.isSuccess) {
                val msgs = mailResult.getOrNull() ?: emptyList()
                _mailboxMessages.value = msgs
                _driveSyncStatus.value = _driveSyncStatus.value.copy(unreadMailCount = msgs.count { !it.isRead })
            }

            // If Editor in Chief, refresh suspended users list and the
            // registry approval queue
            if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief) {
                _suspendedUsers.value = networkService.getSuspendedUsers()
                _pendingBooks.value = networkService.fetchPendingBooks().getOrNull() ?: emptyList()
            }

                    }
    }

    fun syncManuscript(
        manuscript: ManuscriptEntity,
        isPublicInCommunity: Boolean,
        onDone: (Result<GlobalBookIndexEntry>) -> Unit
    ) {
        val authorEmail = manuscript.authorEmail
        if (authorEmail.isNotBlank() && !authorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
            onDone(Result.failure(IllegalStateException("Only the author can sync this book.")))
            return
        }
        _driveSyncStatus.value = _driveSyncStatus.value.copy(isSyncing = true, syncError = null)
        viewModelScope.launch {
            try {
                val sections = repository.getSectionsForManuscriptOnce(manuscript.id)
                val result = networkService.syncManuscriptToBackRoom(
                    manuscript = manuscript,
                    sections = sections,
                    currentUser = _currentUser.value,
                    isPublicInCommunity = isPublicInCommunity
                )
                if (result.isSuccess) {
                    val entry = result.getOrNull()
                    // The same whole book, saved to the writer's own Google Drive.
                    val payload = networkService.buildPayloadJson(
                        manuscript, sections, _currentUser.value, isPublicInCommunity
                    )
                    val driveResult = googleDriveClient.accessToken(interactive = true).fold(
                        onSuccess = { token -> googleDriveClient.saveBook(token, manuscript.id, manuscript.title, payload) },
                        onFailure = { e -> Result.failure(e) }
                    )
                    if (driveResult.isFailure) {
                        val driveError = driveResult.exceptionOrNull()?.message ?: "Google Drive save failed"
                        _driveSyncStatus.value = _driveSyncStatus.value.copy(
                            isSyncing = false,
                            syncError = driveError
                        )
                        onDone(Result.failure(Exception(driveError)))
                        return@launch
                    }
                    _driveSyncStatus.value = _driveSyncStatus.value.copy(
                        isSyncing = false,
                        lastSyncTime = entry?.lastSyncedTimestamp,
                        lastSyncFileId = entry?.fileId
                    )
                    refreshNetwork()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown sync error"
                    _driveSyncStatus.value = _driveSyncStatus.value.copy(
                        isSyncing = false,
                        syncError = errorMsg
                    )
                }
                onDone(result)
            } catch (e: Exception) {
                _driveSyncStatus.value = _driveSyncStatus.value.copy(
                    isSyncing = false,
                    syncError = e.message
                )
                onDone(Result.failure(e))
            }
        }
    }

    fun markMailMessageAsRead(messageId: String) {
        viewModelScope.launch {
            networkService.markMailAsRead(_currentUser.value.email, messageId)
            refreshNetwork()
        }
    }

    // ==========================================
    // Editor in Chief / Superuser Actions
    // ==========================================
    fun suspendUserAccount(targetEmail: String, reason: String) {
        viewModelScope.launch {
            networkService.suspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail,
                reason = reason
            )
            refreshNetwork()
        }
    }

    fun unsuspendUserAccount(targetEmail: String) {
        viewModelScope.launch {
            networkService.unsuspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail
            )
            refreshNetwork()
        }
    }

    fun revokeRegistryAccess(fileId: String) {
        viewModelScope.launch {
            networkService.revokeManuscriptAccess(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            refreshNetwork()
        }
    }

    fun approveRegistryBook(fileId: String) {
        viewModelScope.launch {
            networkService.approvePendingBook(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            refreshNetwork()
        }
    }

    fun rejectRegistryBook(fileId: String) {
        viewModelScope.launch {
            networkService.rejectPendingBook(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            refreshNetwork()
        }
    }

    fun sendServerlessMailDirective(message: ServerlessMailMessage) {
        viewModelScope.launch {
            networkService.sendServerlessMail(message)
            refreshNetwork()
        }
    }
}
