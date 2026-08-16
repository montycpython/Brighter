package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmos.CmosLeafEngine
import com.example.data.BwriterDatabase
import com.example.data.BwriterRepository
import com.example.data.UserPreferences
import com.example.data.GoogleDriveSyncService
import com.example.export.CmosPdfExporter
import com.example.model.DriveSyncStatus
import com.example.model.GlobalBookIndexEntry
import com.example.model.ServerlessMailMessage
import com.example.model.SuspendedUserEntry
import com.example.model.CalculatedLeaf
import com.example.model.EditorialCommentEntity
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.SectionType
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.model.WorkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BwriterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BwriterRepository
    private val userPreferences = UserPreferences(application)

    // ==========================================
    // User & Google Identity (Persisted)
    // ==========================================
    private val _currentUser = MutableStateFlow(userPreferences.getUserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _hasAcceptedTerms = MutableStateFlow(userPreferences.hasAcceptedTerms())
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms.asStateFlow()

    fun acceptTermsOfService(version: String = "1.0.0-PROD") {
        userPreferences.setAcceptedTerms(version)
        _hasAcceptedTerms.value = true
    }

    init {
        val db = BwriterDatabase.getDatabase(application)
        repository = BwriterRepository(db)
        viewModelScope.launch {
            repository.seedInitialWorksIfEmpty()
            refreshDriveNetwork()
        }
    }

    fun updateRole(newRole: WorkRole) {
        val updated = _currentUser.value.copy(role = newRole)
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
        refreshDriveNetwork()
    }

    fun updateProfile(name: String, penName: String, email: String, role: WorkRole) {
        val updated = _currentUser.value.copy(
            name = name.trim(),
            penName = penName.trim(),
            email = email.trim(),
            role = role
        )
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
        refreshDriveNetwork()
    }

    fun signInWithGoogleAccount(email: String, name: String, penName: String, role: WorkRole) {
        val finalPenName = if (penName.isNotBlank()) penName.trim() else _currentUser.value.penName
        val updated = _currentUser.value.copy(
            email = email.trim(),
            name = name.trim(),
            penName = finalPenName,
            role = role
        )
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
        refreshDriveNetwork()
    }

    // ==========================================
    // Manuscript Library
    // ==========================================
    private val _selectedWorkTypeFilter = MutableStateFlow<WorkType?>(null)
    val selectedWorkTypeFilter: StateFlow<WorkType?> = _selectedWorkTypeFilter.asStateFlow()

    fun setWorkTypeFilter(type: WorkType?) {
        _selectedWorkTypeFilter.value = type
    }

    val manuscripts: StateFlow<List<ManuscriptEntity>> = combine(
        repository.allManuscripts,
        _selectedWorkTypeFilter
    ) { list, filter ->
        if (filter == null) list else list.filter { it.workType == filter }
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
    val charactersForActiveManuscript: StateFlow<List<com.example.model.CharacterEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCharactersForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveCharacter(character: com.example.model.CharacterEntity) {
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
    val settingsForActiveManuscript: StateFlow<List<com.example.model.StorySettingEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getSettingsForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveSetting(setting: com.example.model.StorySettingEntity) {
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
    val userAiSubscription: StateFlow<com.example.model.UserAiSubscription> = _userAiSubscription.asStateFlow()

    private val _tokenTransactions = MutableStateFlow(userPreferences.getTokenTransactions())
    val tokenTransactions: StateFlow<List<com.example.model.AiTokenTransaction>> = _tokenTransactions.asStateFlow()

    private val _paidMembersTelemetry = MutableStateFlow(userPreferences.getAllSubscribersTelemetry())
    val paidMembersTelemetry: StateFlow<List<com.example.model.PaidMemberTelemetry>> = _paidMembersTelemetry.asStateFlow()

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    fun openPaywall() {
        _showPaywall.value = true
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    fun updateSubscriptionPlan(plan: com.example.model.SubscriptionPlan) {
        val current = _userAiSubscription.value
        val updated = current.copy(
            plan = plan,
            creditsRemaining = plan.monthlyCredits,
            monthlyRenewalTimestamp = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
            isActive = true
        )
        _userAiSubscription.value = updated
        userPreferences.saveUserSubscription(updated)
        _paidMembersTelemetry.value = userPreferences.getAllSubscribersTelemetry()
        _showPaywall.value = false
    }

    fun adminGrantBonusCredits(targetEmail: String, bonusCredits: Int) {
        userPreferences.adminGrantBonusCredits(targetEmail, bonusCredits)
        _userAiSubscription.value = userPreferences.getUserSubscription(_currentUser.value.email)
        _paidMembersTelemetry.value = userPreferences.getAllSubscribersTelemetry()
    }

    fun generateAiDraftFromPrompt(
        section: SectionEntity,
        prompt: String,
        onGenerated: (String) -> Unit,
        onRequiresUpgrade: () -> Unit = { openPaywall() }
    ) {
        val currentSub = _userAiSubscription.value
        val isEditorInChief = _currentUser.value.email.equals("real.artistry@gmail.com", ignoreCase = true)

        if (!isEditorInChief && currentSub.creditsRemaining <= 0) {
            onRequiresUpgrade()
            return
        }

        _isGeneratingAiProse.value = true
        viewModelScope.launch {
            try {
                val result = com.example.ai.GeminiProseGenerator.generateChapterProse(prompt)
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
                    _paidMembersTelemetry.value = userPreferences.getAllSubscribersTelemetry()

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
                epigraphText = epigraph.trim()
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

    fun exportActiveManuscriptToPdf(context: Context, onComplete: (com.example.export.CmosPdfExporter.ExportResult) -> Unit) {
        val m = activeManuscript.value ?: return
        val s = activeSections.value
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val result = com.example.export.CmosPdfExporter.exportToPdf(context, m, s)
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
    val googleDriveSyncService = GoogleDriveSyncService(application)

    private val _driveSyncStatus = MutableStateFlow(DriveSyncStatus())
    val driveSyncStatus: StateFlow<DriveSyncStatus> = _driveSyncStatus.asStateFlow()

    private val _globalBookIndex = MutableStateFlow<List<GlobalBookIndexEntry>>(emptyList())
    val globalBookIndex: StateFlow<List<GlobalBookIndexEntry>> = _globalBookIndex.asStateFlow()

    private val _suspendedUsers = MutableStateFlow<List<SuspendedUserEntry>>(emptyList())
    val suspendedUsers: StateFlow<List<SuspendedUserEntry>> = _suspendedUsers.asStateFlow()

    private val _mailboxMessages = MutableStateFlow<List<ServerlessMailMessage>>(emptyList())
    val mailboxMessages: StateFlow<List<ServerlessMailMessage>> = _mailboxMessages.asStateFlow()

    private val _activeSuspension = MutableStateFlow<SuspendedUserEntry?>(null)
    val activeSuspension: StateFlow<SuspendedUserEntry?> = _activeSuspension.asStateFlow()

    fun checkAccountSuspension() {
        viewModelScope.launch {
            val suspension = googleDriveSyncService.checkUserSuspension(_currentUser.value.email)
            _activeSuspension.value = suspension
            _driveSyncStatus.value = _driveSyncStatus.value.copy(
                isSuspended = suspension != null,
                suspensionReason = suspension?.reason
            )
        }
    }

    fun refreshDriveNetwork() {
        viewModelScope.launch {
            checkAccountSuspension()

            // Fetch index
            val indexResult = googleDriveSyncService.fetchGlobalBookIndex(_currentUser.value)
            if (indexResult.isSuccess) {
                _globalBookIndex.value = indexResult.getOrNull() ?: emptyList()
            }

            // Fetch mail
            val mailResult = googleDriveSyncService.fetchMailboxMessages(_currentUser.value.email)
            if (mailResult.isSuccess) {
                val msgs = mailResult.getOrNull() ?: emptyList()
                _mailboxMessages.value = msgs
                _driveSyncStatus.value = _driveSyncStatus.value.copy(unreadMailCount = msgs.count { !it.isRead })
            }

            // If Editor in Chief, refresh suspended users list
            if (_currentUser.value.email.equals(GoogleDriveSyncService.EDITOR_IN_CHIEF_EMAIL, ignoreCase = true)) {
                _suspendedUsers.value = googleDriveSyncService.getSuspendedUsersFromStorage()
            }
        }
    }

    fun syncManuscriptToGoogleDrive(
        manuscript: ManuscriptEntity,
        isPublicInCommunity: Boolean,
        onDone: (Result<GlobalBookIndexEntry>) -> Unit
    ) {
        _driveSyncStatus.value = _driveSyncStatus.value.copy(isSyncing = true, syncError = null)
        viewModelScope.launch {
            try {
                val sections = repository.getSectionsForManuscriptOnce(manuscript.id)
                val result = googleDriveSyncService.syncManuscriptToDrive(
                    manuscript = manuscript,
                    sections = sections,
                    currentUser = _currentUser.value,
                    isPublicInCommunity = isPublicInCommunity
                )
                if (result.isSuccess) {
                    val entry = result.getOrNull()
                    _driveSyncStatus.value = _driveSyncStatus.value.copy(
                        isSyncing = false,
                        lastSyncTime = entry?.lastSyncedTimestamp,
                        lastSyncFileId = entry?.fileId
                    )
                    refreshDriveNetwork()
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
            googleDriveSyncService.markMailAsRead(_currentUser.value.email, messageId)
            refreshDriveNetwork()
        }
    }

    // ==========================================
    // Editor in Chief / Superuser Actions
    // ==========================================
    fun suspendUserAccount(targetEmail: String, reason: String) {
        viewModelScope.launch {
            googleDriveSyncService.suspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail,
                reason = reason
            )
            refreshDriveNetwork()
        }
    }

    fun unsuspendUserAccount(targetEmail: String) {
        viewModelScope.launch {
            googleDriveSyncService.unsuspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail
            )
            refreshDriveNetwork()
        }
    }

    fun revokeManuscriptDriveAccess(fileId: String) {
        viewModelScope.launch {
            googleDriveSyncService.revokeManuscriptAccess(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            refreshDriveNetwork()
        }
    }

    fun sendMailMessage(message: ServerlessMailMessage) {
        sendServerlessMailDirective(message)
    }

    fun sendServerlessMailDirective(message: ServerlessMailMessage) {
        viewModelScope.launch {
            googleDriveSyncService.sendServerlessMail(message)
            refreshDriveNetwork()
        }
    }
}
