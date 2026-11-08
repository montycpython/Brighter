package com.supernatural.bwriter.ui.viewmodel

import android.app.Application
import android.util.Log
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supernatural.bwriter.cmos.CmosLeafEngine
import com.supernatural.bwriter.data.AuthorKeys
import com.supernatural.bwriter.data.BookStamp
import com.supernatural.bwriter.data.CreditChain
import com.supernatural.bwriter.data.CreditSchedule
import com.supernatural.bwriter.data.EditorAssignment
import com.supernatural.bwriter.data.SiteUser
import com.supernatural.bwriter.data.BwriterDatabase
import com.supernatural.bwriter.data.BwriterRepository
import com.supernatural.bwriter.data.UserPreferences
import com.supernatural.bwriter.data.FirestoreNetworkService
import com.supernatural.bwriter.data.RestoredBook
import com.supernatural.bwriter.data.GoogleDriveClient
import com.supernatural.bwriter.data.IllustrationUris
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class BwriterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BwriterRepository
    private val userPreferences = UserPreferences(application)

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

    val networkService = FirestoreNetworkService(application)

    private val googleDriveClient = GoogleDriveClient(application)

    private val guardExceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e("BwriterViewModel", "Guarded launch failed", e)
    }

    private fun launchGuarded(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(guardExceptionHandler, block = block)

    init {
        val db = BwriterDatabase.getDatabase(application)
        repository = BwriterRepository(db)
        launchGuarded {
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
            email = _currentUser.value.email,
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
        com.supernatural.bwriter.data.StudioStanding.isEditorInChief = false
        _isRoot.value = false
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

    private fun standingRoleFor(): WorkRole {
        return if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief) WorkRole.EDITOR else WorkRole.AUTHOR
    }

    fun switchGoogleAccount(account: UserProfile) {
        com.supernatural.bwriter.data.StudioStanding.isEditorInChief = false
        _isRoot.value = false
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
        _isRoot.value = false
        userPreferences.signOut()
        _isLoggedIn.value = false
        val blankUser = UserProfile(
            id = "",
            name = "",
            penName = "",
            email = "",
            role = WorkRole.AUTHOR,
            organization = ""
        )
        _currentUser.value = blankUser
        _userAiSubscription.value = userPreferences.getUserSubscription("")
        _tokenTransactions.value = emptyList()
        _creditBalance.value = 0
        _unclaimedCredit.value = 0
        _mailboxMessages.value = emptyList()
        _globalBookIndex.value = emptyList()
        _suspendedUsers.value = emptyList()
        _pendingBooks.value = emptyList()
        _pendingEditorAssignments.value = emptyList()
        _siteUsers.value = emptyList()
        _registryAuthors.value = emptyList()
        _regentEmail.value = ""
        _selectedManuscriptId.value = null
        lastRefreshEmail = ""
        _driveSyncStatus.value = DriveSyncStatus()
    }

    fun removeSavedGoogleAccount(email: String) {
        userPreferences.removeSavedGoogleAccount(email)
        _savedGoogleAccounts.value = userPreferences.getSavedGoogleAccounts()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
    }

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
                m.authorEmail.equals(user.email, ignoreCase = true)
            }
        }
        if (filter == null) userBooks else userBooks.filter { it.workType == filter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    val charactersForActiveManuscript: StateFlow<List<com.supernatural.bwriter.model.CharacterEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getCharactersForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveCharacter(character: com.supernatural.bwriter.model.CharacterEntity) {
        launchGuarded {
            if (character.id == 0L) {
                repository.insertCharacter(character)
            } else {
                repository.updateCharacter(character)
            }
        }
    }

    fun deleteCharacter(characterId: Long) {
        launchGuarded {
            repository.deleteCharacterById(characterId)
        }
    }

    val settingsForActiveManuscript: StateFlow<List<com.supernatural.bwriter.model.StorySettingEntity>> = _selectedManuscriptId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getSettingsForManuscript(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _versionHistory = MutableStateFlow<List<BookVersionSnapshot>>(emptyList())
    val versionHistory: StateFlow<List<BookVersionSnapshot>> = _versionHistory.asStateFlow()

    fun loadVersionHistory(manuscriptId: Long) {
        launchGuarded {
            _versionHistory.value = networkService.getVersionHistory(manuscriptId, _currentUser.value.email)
        }
    }

    fun saveSetting(setting: com.supernatural.bwriter.model.StorySettingEntity) {
        launchGuarded {
            if (setting.id == 0L) {
                repository.insertSetting(setting)
            } else {
                repository.updateSetting(setting)
            }
        }
    }

    fun deleteSetting(settingId: Long) {
        launchGuarded {
            repository.deleteSettingById(settingId)
        }
    }

    private val _isGeneratingAiProse = MutableStateFlow(false)
    val isGeneratingAiProse: StateFlow<Boolean> = _isGeneratingAiProse.asStateFlow()

    private val _userAiSubscription = MutableStateFlow(userPreferences.getUserSubscription(_currentUser.value.email))
    val userAiSubscription: StateFlow<com.supernatural.bwriter.model.UserAiSubscription> = _userAiSubscription.asStateFlow()

    private val _tokenTransactions = MutableStateFlow(userPreferences.getTokenTransactions(_currentUser.value.email))
    val tokenTransactions: StateFlow<List<com.supernatural.bwriter.model.AiTokenTransaction>> = _tokenTransactions.asStateFlow()


    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _creditBalance = MutableStateFlow(0)
    val creditBalance: StateFlow<Int> = _creditBalance.asStateFlow()

    private val _unclaimedCredit = MutableStateFlow(0)
    val unclaimedCredit: StateFlow<Int> = _unclaimedCredit.asStateFlow()

    private val _isMiningCredit = MutableStateFlow(false)
    private val _claimFailed = MutableStateFlow(false)
    val claimFailed: StateFlow<Boolean> = _claimFailed.asStateFlow()
    val isMiningCredit: StateFlow<Boolean> = _isMiningCredit.asStateFlow()

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
        launchGuarded {
            try {
                val result = com.supernatural.bwriter.ai.GeminiProseGenerator.generateChapterProse(prompt)
                val aiResult = result.getOrNull()
                if (aiResult != null && aiResult.text.isNotBlank()) {
                    val generatedText = aiResult.text
                    val freshSection = repository.getSectionByIdOnce(section.id) ?: section
                    val updatedContent = if (freshSection.content.isBlank()) {
                        generatedText
                    } else {
                        "${freshSection.content.trimEnd()}\n\n$generatedText"
                    }
                    repository.updateSection(freshSection.copy(content = updatedContent, aiDraftPrompt = prompt))

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
                    _tokenTransactions.value = userPreferences.getTokenTransactions(_currentUser.value.email)

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
        launchGuarded {
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
        launchGuarded {
            repository.updateManuscript(updated)
        }
    }

    fun deleteManuscript(id: Long) {
        launchGuarded {
            val manuscript = repository.getManuscriptByIdOnce(id)
            repository.deleteManuscript(id)
            if (_selectedManuscriptId.value == id) {
                _selectedManuscriptId.value = null
            }
            val birthId = manuscript?.birthId.orEmpty()
            if (birthId.isNotBlank()) {
                val deleted = deletedBookPrefs.getStringSet("birthIds", emptySet())?.toMutableSet() ?: mutableSetOf()
                deleted.add(birthId)
                deletedBookPrefs.edit().putStringSet("birthIds", deleted).apply()
                networkService.deleteCloudBackup(_currentUser.value.email, birthId)
                val tokenResult = googleDriveClient.accessToken(interactive = false)
                if (tokenResult.isSuccess) {
                    googleDriveClient.deleteBook(tokenResult.getOrThrow(), birthId)
                }
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
        launchGuarded {
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
        launchGuarded {
            repository.updateSectionContent(section, newContent)
        }
    }

    fun updateSectionAiPrompt(section: SectionEntity, prompt: String) {
        launchGuarded {
            repository.updateSection(section.copy(aiDraftPrompt = prompt))
        }
    }

    fun updateSectionTitle(section: SectionEntity, newTitle: String, newSubtitle: String) {
        launchGuarded {
            repository.updateSectionTitle(section, newTitle, newSubtitle)
        }
    }

    fun updateSectionStatus(section: SectionEntity, newStatus: SectionStatus) {
        launchGuarded {
            repository.updateSection(section.copy(status = newStatus))
        }
    }

    fun updateSectionAssignment(
        section: SectionEntity,
        assignedAuthor: String,
        assignedRole: WorkRole,
        contributorNotes: String
    ) {
        launchGuarded {
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
        launchGuarded {
            repository.updateSectionIllustrations(section, headerUri, headerCaption, tailUri, tailCaption)
        }
    }

    fun deleteSection(sectionId: Long, manuscriptId: Long) {
        launchGuarded {
            repository.deleteSection(sectionId, manuscriptId)
            if (_selectedSectionId.value == sectionId) {
                _selectedSectionId.value = null
            }
        }
    }

    fun proposeSectionRevision(
        section: SectionEntity,
        newContent: String
    ) {
        launchGuarded {
            repository.proposeRevision(
                section = section,
                newContent = newContent,
                editorUser = _currentUser.value
            )
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
        launchGuarded {
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
        launchGuarded {
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
        launchGuarded {
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
        launchGuarded {
            repository.setCommentResolved(commentId, resolved)
        }
    }

    private val _exportResult = MutableStateFlow<CmosPdfExporter.ExportResult?>(null)
    val exportResult: StateFlow<CmosPdfExporter.ExportResult?> = _exportResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportActiveManuscriptToPdf(context: Context, onComplete: (com.supernatural.bwriter.export.CmosPdfExporter.ExportResult) -> Unit) {
        val m = activeManuscript.value ?: return
        val s = activeSections.value
        _isExporting.value = true
        launchGuarded {
            try {
                val result = com.supernatural.bwriter.export.CmosPdfExporter.exportToPdf(
                    context, m, s,
                    ownBook = IllustrationUris.isOwnBook(m.authorEmail, _currentUser.value.email)
                )
                _exportResult.value = result
                onComplete(result)
            } finally {
                _isExporting.value = false
            }
        }
    }

    private val _driveSyncStatus = MutableStateFlow(DriveSyncStatus())
    val driveSyncStatus: StateFlow<DriveSyncStatus> = _driveSyncStatus.asStateFlow()

    private val _globalBookIndex = MutableStateFlow<List<GlobalBookIndexEntry>>(emptyList())
    val globalBookIndex: StateFlow<List<GlobalBookIndexEntry>> = _globalBookIndex.asStateFlow()

    private val _pendingBooks = MutableStateFlow<List<GlobalBookIndexEntry>>(emptyList())
    val pendingBooks: StateFlow<List<GlobalBookIndexEntry>> = _pendingBooks.asStateFlow()

    private val _suspendedUsers = MutableStateFlow<List<SuspendedUserEntry>>(emptyList())
    val suspendedUsers: StateFlow<List<SuspendedUserEntry>> = _suspendedUsers.asStateFlow()

    private val _pendingEditorAssignments = MutableStateFlow<List<EditorAssignment>>(emptyList())
    val pendingEditorAssignments: StateFlow<List<EditorAssignment>> = _pendingEditorAssignments.asStateFlow()

    private val _registryAuthors = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val registryAuthors: StateFlow<List<Pair<String, String>>> = _registryAuthors.asStateFlow()

    private val _siteUsers = MutableStateFlow<List<SiteUser>>(emptyList())
    val siteUsers: StateFlow<List<SiteUser>> = _siteUsers.asStateFlow()

    private val _regentEmail = MutableStateFlow("")
    val regentEmail: StateFlow<String> = _regentEmail.asStateFlow()
    private val _isRoot = MutableStateFlow(false)
    val isRoot: StateFlow<Boolean> = _isRoot.asStateFlow()

    private val _mailboxMessages = MutableStateFlow<List<ServerlessMailMessage>>(emptyList())
    val mailboxMessages: StateFlow<List<ServerlessMailMessage>> = _mailboxMessages.asStateFlow()

    private val _activeSuspension = MutableStateFlow<SuspendedUserEntry?>(null)
    val activeSuspension: StateFlow<SuspendedUserEntry?> = _activeSuspension.asStateFlow()

    fun checkAccountSuspension() {
        launchGuarded {
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
        launchGuarded {
            val restored = networkService.restoreUserManuscriptsFromBackRoom(_currentUser.value.email)
            var total = 0
            for (book in restored) {
                if (applyRestoredBook(book)) total++
            }

            val tokenResult = googleDriveClient.accessToken(interactive = interactive)
            if (tokenResult.isSuccess) {
                val payloads = googleDriveClient.downloadAllPayloads(tokenResult.getOrThrow())
                for (payload in payloads) {
                    val parsed = networkService.parsePayloadJson(payload, _currentUser.value.email) ?: continue
                    if (applyRestoredBook(parsed)) total++
                }
            }

            refreshNetwork()
            onRestored(total)
        }
    }

    private suspend fun insertRestoredBook(book: RestoredBook): Long {
        val mId = repository.createManuscript(book.manuscript, populateTemplate = false)
        val sectionIdMap = mutableMapOf<Long, Long>()
        book.sections.forEachIndexed { index, sec ->
            val newSectionId = repository.insertSection(sec.copy(manuscriptId = mId))
            val oldSectionId = book.sectionIds.getOrNull(index) ?: 0L
            if (oldSectionId > 0) sectionIdMap[oldSectionId] = newSectionId
        }
        for (character in book.characters) {
            repository.insertCharacter(character.copy(id = 0, manuscriptId = mId))
        }
        for (setting in book.settings) {
            repository.insertSetting(setting.copy(id = 0, manuscriptId = mId))
        }
        for (comment in book.comments) {
            val newSectionId = sectionIdMap[comment.sectionId] ?: continue
            repository.addComment(comment.copy(id = 0, sectionId = newSectionId, manuscriptId = mId))
        }
        return mId
    }

    private suspend fun applyRestoredBook(book: RestoredBook): Boolean {
        val manuscript = book.manuscript
        if (manuscript.birthId.isNotBlank() &&
            (deletedBookPrefs.getStringSet("birthIds", emptySet())?.contains(manuscript.birthId) == true)
        ) return false
        val local = manuscripts.value.firstOrNull { existing ->
            (manuscript.birthId.isNotBlank() && existing.birthId == manuscript.birthId) ||
                bookKey(existing.title, existing.authorEmail) == bookKey(manuscript.title, manuscript.authorEmail)
        }
        if (local != null && local.updatedAt >= manuscript.updatedAt) return false
        if (local != null) {
            repository.deleteManuscript(local.id)
        }
        insertRestoredBook(book)
        return true
    }

    private val deletedBookPrefs by lazy {
        getApplication<Application>().getSharedPreferences("bwriter_deleted_books", Context.MODE_PRIVATE)
    }

    private val registryCopyPrefs by lazy {
        getApplication<Application>().getSharedPreferences("bwriter_registry_copies", Context.MODE_PRIVATE)
    }

    fun openCommunityBook(entry: GlobalBookIndexEntry, onReady: (Long) -> Unit) {
        launchGuarded {
            if (entry.authorEmail.equals(_currentUser.value.email, ignoreCase = true)) {
                val local = repository.getManuscriptByIdOnce(entry.manuscriptId)
                if (local != null) {
                    onReady(local.id)
                    return@launchGuarded
                }
            }
            val savedId = registryCopyPrefs.getLong(entry.fileId, -1L)
            val savedTs = registryCopyPrefs.getLong(entry.fileId + "_ts", 0L)
            if (savedId > 0 && repository.getManuscriptByIdOnce(savedId) != null) {
                if (entry.lastSyncedTimestamp > savedTs) {
                    val refreshed = networkService.fetchRegistryBook(entry.fileId)
                    if (refreshed != null) {
                        applyRestoredBook(refreshed)
                        registryCopyPrefs.edit()
                            .putLong(entry.fileId + "_ts", entry.lastSyncedTimestamp).apply()
                    }
                }
                onReady(savedId)
                return@launchGuarded
            }
            val fetched = networkService.fetchRegistryBook(entry.fileId)
            if (fetched == null) {
                onReady(-1L)
                return@launchGuarded
            }
            val mId = insertRestoredBook(fetched)
            registryCopyPrefs.edit()
                .putLong(entry.fileId, mId)
                .putLong(entry.fileId + "_ts", entry.lastSyncedTimestamp)
                .apply()
            onReady(mId)
        }
    }

    private var refreshJob: kotlinx.coroutines.Job? = null
    private var lastRefreshEmail = ""

    fun refreshNetwork() {
        refreshJob?.cancel()
        refreshJob = launchGuarded {
            try {
            com.supernatural.bwriter.data.StudioStanding.isEditorInChief = networkService.verifyEditorInChief()
            val provenStanding = com.supernatural.bwriter.data.StudioStanding.isEditorInChief
            val userNow = _currentUser.value
            if (!userNow.email.equals(lastRefreshEmail, ignoreCase = true)) {
                lastRefreshEmail = userNow.email
                _mailboxMessages.value = emptyList()
                _globalBookIndex.value = emptyList()
            }
            if (!provenStanding) {
                _suspendedUsers.value = emptyList()
                _pendingBooks.value = emptyList()
                _pendingEditorAssignments.value = emptyList()
                _siteUsers.value = emptyList()
                _registryAuthors.value = emptyList()
                _regentEmail.value = ""
                _isRoot.value = false
            }
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

            if (_currentUser.value.email.isNotBlank()) {
                networkService.recordUserPresence(_currentUser.value.displayName, _currentUser.value.email)
            }

            checkAccountSuspension()

            refreshCredit()

            val indexResult = networkService.fetchGlobalBookIndex(_currentUser.value)
            if (indexResult.isSuccess) {
                _globalBookIndex.value = indexResult.getOrNull() ?: emptyList()
            }

            val mailResult = networkService.fetchMailboxMessages(_currentUser.value.email)
            if (mailResult.isSuccess) {
                val msgs = mailResult.getOrNull() ?: emptyList()
                _mailboxMessages.value = msgs
                _driveSyncStatus.value = _driveSyncStatus.value.copy(unreadMailCount = msgs.count { !it.isRead })
            }

            if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief) {
                _suspendedUsers.value = networkService.getSuspendedUsers()
                _pendingBooks.value = networkService.fetchPendingBooks().getOrNull() ?: emptyList()
                _pendingEditorAssignments.value = networkService.fetchPendingEditorAssignments()
                _siteUsers.value = networkService.fetchSiteUsers()
                _regentEmail.value = networkService.fetchRegentEmail()
                _isRoot.value = _currentUser.value.email.isNotBlank() &&
                    (_regentEmail.value.isBlank() ||
                        !_currentUser.value.email.equals(_regentEmail.value, ignoreCase = true))
                val indexAuthors = _globalBookIndex.value
                    .map { it.authorName to it.authorEmail.trim().lowercase() }
                    .filter { it.second.isNotBlank() }
                val directoryAuthors = _siteUsers.value
                    .filter { it.email.isNotBlank() && it.displayName.isNotBlank() }
                    .map { it.displayName to it.email }
                _registryAuthors.value = (directoryAuthors + indexAuthors).distinctBy { it.second }
            }
            } catch (t: Throwable) {
            }

                    }
    }

    private fun sha256Hex(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
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
        if (_driveSyncStatus.value.isSyncing) {
            onDone(Result.failure(IllegalStateException("A sync is already in progress for this book.")))
            return
        }
        _driveSyncStatus.value = _driveSyncStatus.value.copy(isSyncing = true, syncError = null)
        launchGuarded {
            try {
                val manuscript = repository.getManuscriptByIdOnce(manuscript.id) ?: manuscript
                val sections = repository.getSectionsForManuscriptOnce(manuscript.id)
                val characters = repository.getCharactersForManuscriptOnce(manuscript.id)
                val settings = repository.getSettingsForManuscriptOnce(manuscript.id)
                val comments = repository.getCommentsForManuscriptOnce(manuscript.id)
                val appContext = getApplication<Application>().applicationContext
                AuthorKeys.ensureKeyPair(appContext)
                if (!AuthorKeys.isUsable(appContext)) {
                    throw IllegalStateException("Author key unavailable on this device.")
                }
                val authorAddress = AuthorKeys.address(appContext)
                val authorPubKey = AuthorKeys.publicKeyHex(appContext)
                val birthId = manuscript.birthId.ifBlank { java.util.UUID.randomUUID().toString() }
                val stamped = manuscript.copy(birthId = birthId, authorAddress = authorAddress)
                if (manuscript.birthId.isBlank()) {
                    repository.updateManuscript(stamped)
                }
                val payloadJson = networkService.buildPayloadJson(
                    stamped, sections, characters, settings, comments, _currentUser.value, isPublicInCommunity
                )
                val prevStampHash = manuscript.stampHash
                val newStampHash = sha256Hex("$authorAddress|$birthId|$prevStampHash|$payloadJson")
                val signature = AuthorKeys.sign(appContext, newStampHash)
                val stamp = BookStamp(
                    authorAddress = authorAddress,
                    authorPubKey = authorPubKey,
                    birthId = birthId,
                    stampHash = newStampHash,
                    prevStampHash = prevStampHash,
                    stampSignature = signature
                )
                val result = networkService.syncManuscriptToBackRoom(
                    manuscript = stamped,
                    sections = sections,
                    characters = characters,
                    settings = settings,
                    comments = comments,
                    currentUser = _currentUser.value,
                    isPublicInCommunity = isPublicInCommunity,
                    stamp = stamp
                )
                if (result.isSuccess) {
                    val fullyStamped = stamped.copy(
                        stampHash = newStampHash,
                        prevStampHash = prevStampHash,
                        stampSignature = signature
                    )
                    repository.updateManuscript(fullyStamped)
                    val entry = result.getOrNull()
                    if (com.supernatural.bwriter.data.StudioStanding.isEditorInChief && entry != null &&
                        entry.authorEmail.equals(_currentUser.value.email, ignoreCase = true)
                    ) {
                        networkService.issueCredit(entry)
                    }
                    val payload = networkService.buildPayloadJson(
                        fullyStamped, sections, characters, settings, comments, _currentUser.value, isPublicInCommunity
                    )
                    val driveResult = googleDriveClient.accessToken(interactive = true).fold(
                        onSuccess = { token -> googleDriveClient.saveBook(token, birthId, manuscript.title, payload) },
                        onFailure = { e -> Result.failure(e) }
                    )
                    if (driveResult.isFailure) {
                        val driveError = driveResult.exceptionOrNull()?.message ?: "Google Drive save failed"
                        _driveSyncStatus.value = _driveSyncStatus.value.copy(
                            isSyncing = false,
                            syncError = driveError
                        )
                        onDone(Result.failure(Exception(driveError)))
                        return@launchGuarded
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

    fun refreshCredit() {
        launchGuarded {
            val email = _currentUser.value.email
            if (email.isBlank()) {
                _creditBalance.value = 0
                _unclaimedCredit.value = 0
                return@launchGuarded
            }
            val issuancesResult = networkService.fetchIssuancesForResult(email)
            val claimedResult = networkService.fetchClaimedIssuanceIdsResult(email)
            if (issuancesResult.isSuccess && claimedResult.isSuccess) {
                val issuances = issuancesResult.getOrNull() ?: emptyList()
                val claimed = claimedResult.getOrNull() ?: emptySet()
                _creditBalance.value = issuances.filter { claimed.contains(it.issuanceHash) }.sumOf { it.amount }
                _unclaimedCredit.value = issuances.filterNot { claimed.contains(it.issuanceHash) }.sumOf { it.amount }
            }
        }
    }

    fun claimCredit() {
        if (_isMiningCredit.value) return
        _isMiningCredit.value = true
        launchGuarded {
            try {
                val email = _currentUser.value.email
                val issuances = networkService.fetchIssuancesFor(email).sortedBy { it.issuedAtMillis }
                val claimed = networkService.fetchClaimedIssuanceIds(email)
                val unclaimed = issuances.filter { !claimed.contains(it.issuanceHash) }
                if (unclaimed.isNotEmpty()) {
                    _claimFailed.value = false
                    var claimedAny = false
                    for (target in unclaimed) {
                        val difficulty = CreditSchedule.difficultyFor(issuances, target)
                        val nonce = withContext(Dispatchers.Default) {
                            CreditChain.mineClaim(target.issuanceHash, difficulty)
                        }
                        if (nonce != null) {
                            networkService.recordClaim(email, target, nonce)
                            claimedAny = true
                            break
                        }
                    }
                    if (!claimedAny) _claimFailed.value = true
                }
            } finally {
                _isMiningCredit.value = false
                refreshCredit()
            }
        }
    }

    fun backupAuthorKey(passphrase: String, onDone: (Result<Unit>) -> Unit) {
        launchGuarded {
            try {
                val appContext = getApplication<Application>().applicationContext
                AuthorKeys.ensureKeyPair(appContext)
                val sealed = withContext(Dispatchers.Default) {
                    AuthorKeys.exportEncrypted(appContext, passphrase)
                }
                if (sealed.isBlank()) {
                    onDone(Result.failure(IllegalStateException("Could not seal the author key")))
                    return@launchGuarded
                }
                googleDriveClient.accessToken(interactive = true).fold(
                    onSuccess = { token -> onDone(googleDriveClient.uploadKeyBackup(token, sealed)) },
                    onFailure = { onDone(Result.failure(it)) }
                )
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }

    fun restoreAuthorKey(passphrase: String, onDone: (Result<Boolean>) -> Unit) {
        launchGuarded {
            try {
                val appContext = getApplication<Application>().applicationContext
                val tokenResult = googleDriveClient.accessToken(interactive = true)
                if (tokenResult.isFailure) {
                    onDone(Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("Google Drive access failed")))
                    return@launchGuarded
                }
                val sealed = googleDriveClient.downloadKeyBackup(tokenResult.getOrThrow())
                if (sealed == null) {
                    onDone(Result.failure(IllegalStateException("No key backup found")))
                    return@launchGuarded
                }
                val restored = withContext(Dispatchers.Default) {
                    AuthorKeys.importEncrypted(appContext, sealed, passphrase)
                }
                if (restored) refreshCredit()
                onDone(Result.success(restored))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }

    fun markMailMessageAsRead(messageId: String) {
        launchGuarded {
            networkService.markMailAsRead(_currentUser.value.email, messageId)
            refreshNetwork()
        }
    }

    fun suspendUserAccount(targetEmail: String, reason: String, onDone: (Boolean) -> Unit = {}) {
        if (targetEmail.equals(_currentUser.value.email, ignoreCase = true)) {
            onDone(false)
            return
        }
        launchGuarded {
            val result = networkService.suspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail,
                reason = reason
            )
            if (result.isSuccess) refreshNetwork()
            onDone(result.isSuccess)
        }
    }

    fun unsuspendUserAccount(targetEmail: String, onDone: (Boolean) -> Unit = {}) {
        launchGuarded {
            val result = networkService.unsuspendUser(
                adminEmail = _currentUser.value.email,
                targetEmail = targetEmail
            )
            if (result.isSuccess) refreshNetwork()
            onDone(result.isSuccess)
        }
    }

    fun revokeRegistryAccess(fileId: String, onDone: (Boolean) -> Unit = {}) {
        launchGuarded {
            val result = networkService.revokeManuscriptAccess(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            if (result.isSuccess) refreshNetwork()
            onDone(result.isSuccess)
        }
    }

    fun approveRegistryBook(fileId: String, onDone: (Boolean) -> Unit = {}) {
        launchGuarded {
            val result = networkService.approvePendingBook(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            if (result.isSuccess) refreshNetwork()
            onDone(result.isSuccess)
        }
    }

    fun rejectRegistryBook(fileId: String, onDone: (Boolean) -> Unit = {}) {
        launchGuarded {
            val result = networkService.rejectPendingBook(
                adminEmail = _currentUser.value.email,
                fileId = fileId
            )
            if (result.isSuccess) refreshNetwork()
            onDone(result.isSuccess)
        }
    }

    fun setRegent(email: String, onDone: (Boolean) -> Unit = {}) {
        val trimmed = email.trim()
        if (trimmed.equals(_currentUser.value.email, ignoreCase = true)) {
            onDone(false)
            return
        }
        val at = trimmed.indexOf('@')
        if (at < 1 || !trimmed.substring(at).contains('.') || trimmed.contains(' ')) {
            onDone(false)
            return
        }
        launchGuarded {
            val ok = networkService.setRegent(trimmed).isSuccess
            refreshNetwork()
            onDone(ok)
        }
    }

    fun clearRegent(onDone: (Boolean) -> Unit = {}) {
        launchGuarded {
            val ok = networkService.setRegent("").isSuccess
            refreshNetwork()
            onDone(ok)
        }
    }

    fun appointEditor(fileId: String, bookAuthorEmail: String, editorEmail: String, editorAddress: String) {
        launchGuarded {
            val book = _globalBookIndex.value.firstOrNull { it.fileId == fileId }
            networkService.createEditorAssignment(
                EditorAssignment(
                    fileId = fileId,
                    bookAuthorEmail = bookAuthorEmail,
                    editorEmail = editorEmail,
                    editorAddress = editorAddress,
                    requestedBy = _currentUser.value.email,
                    status = "APPROVED"
                )
            )
            networkService.sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = editorEmail,
                    senderEmail = _currentUser.value.email,
                    senderName = _currentUser.value.displayName,
                    subject = "Editor Appointment: ${book?.title ?: fileId}",
                    body = "You have been appointed editor of '${book?.title ?: fileId}' by the Editor in Chief."
                )
            )
            refreshNetwork()
        }
    }

    fun decideEditorAssignment(assignment: EditorAssignment, approve: Boolean) {
        launchGuarded {
            networkService.setEditorAssignmentStatus(assignment.id, if (approve) "APPROVED" else "DECLINED")
            val book = _globalBookIndex.value.firstOrNull { it.fileId == assignment.fileId }
            networkService.sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = assignment.editorEmail,
                    senderEmail = _currentUser.value.email,
                    senderName = _currentUser.value.displayName,
                    subject = "Editor Request ${if (approve) "Approved" else "Declined"}: ${book?.title ?: assignment.fileId}",
                    body = if (approve) {
                        "Your request to edit '${book?.title ?: assignment.fileId}' has been approved by the Editor in Chief."
                    } else {
                        "Your request to edit '${book?.title ?: assignment.fileId}' has been declined by the Editor in Chief."
                    }
                )
            )
            refreshNetwork()
        }
    }

    fun requestAccountDeletion(onResult: (Boolean) -> Unit) {
        val user = _currentUser.value
        if (user.email.isBlank()) {
            onResult(false)
            return
        }
        launchGuarded {
            val result = networkService.sendServerlessMail(
                ServerlessMailMessage(
                    recipientEmail = FirestoreNetworkService.DEFAULT_STUDIO_AUTHOR_EMAIL,
                    senderEmail = user.email,
                    senderName = user.displayName,
                    subject = "Account Deletion Request",
                    body = "I request deletion of my Bwriter account and its associated cloud data: back-room book copies, registry and pending entries, in-app mail, version snapshots, and credit records.",
                    messageType = "ACCOUNT_DELETION"
                )
            )
            onResult(result.isSuccess)
        }
    }

    fun sendServerlessMailDirective(message: ServerlessMailMessage) {
        launchGuarded {
            networkService.sendServerlessMail(message)
            refreshNetwork()
        }
    }
}

