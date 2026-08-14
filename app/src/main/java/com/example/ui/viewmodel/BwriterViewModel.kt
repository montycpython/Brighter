package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmos.CmosLeafEngine
import com.example.data.BwriterDatabase
import com.example.data.BwriterRepository
import com.example.data.UserPreferences
import com.example.export.CmosPdfExporter
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

    init {
        val db = BwriterDatabase.getDatabase(application)
        repository = BwriterRepository(db)
        viewModelScope.launch {
            repository.seedInitialWorksIfEmpty()
        }
    }

    fun updateRole(newRole: WorkRole) {
        val updated = _currentUser.value.copy(role = newRole)
        _currentUser.value = updated
        userPreferences.saveUserProfile(updated)
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

    fun exportActiveManuscriptToPdf(context: Context, onComplete: (CmosPdfExporter.ExportResult) -> Unit) {
        val m = activeManuscript.value ?: return
        val s = activeSections.value
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val result = CmosPdfExporter.exportToPdf(context, m, s)
                _exportResult.value = result
                onComplete(result)
            } finally {
                _isExporting.value = false
            }
        }
    }
}
