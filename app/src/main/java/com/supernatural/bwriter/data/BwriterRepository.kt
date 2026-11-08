package com.supernatural.bwriter.data

import androidx.room.withTransaction
import com.supernatural.bwriter.cmos.CmosFormatter
import com.supernatural.bwriter.model.ContributorCredit
import com.supernatural.bwriter.model.EditorialCommentEntity
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.MatterType
import com.supernatural.bwriter.model.SectionEntity
import com.supernatural.bwriter.model.SectionStatus
import com.supernatural.bwriter.model.SectionType
import com.supernatural.bwriter.model.UserProfile
import com.supernatural.bwriter.model.WorkRole
import com.supernatural.bwriter.model.WorkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs

class BwriterRepository(private val database: BwriterDatabase) {

    private val manuscriptDao = database.manuscriptDao()
    private val sectionDao = database.sectionDao()
    private val commentDao = database.commentDao()
    private val characterDao = database.characterDao()
    private val settingDao = database.settingDao()

    val allManuscripts: Flow<List<ManuscriptEntity>> = manuscriptDao.getAllManuscripts()

    fun getManuscriptById(id: Long): Flow<ManuscriptEntity?> = manuscriptDao.getManuscriptById(id)

    suspend fun getManuscriptByIdOnce(id: Long): ManuscriptEntity? = withContext(Dispatchers.IO) {
        manuscriptDao.getManuscriptByIdOnce(id)
    }

    fun getSectionsForManuscript(manuscriptId: Long): Flow<List<SectionEntity>> =
        sectionDao.getSectionsForManuscript(manuscriptId)

    suspend fun getSectionsForManuscriptOnce(manuscriptId: Long): List<SectionEntity> =
        withContext(Dispatchers.IO) {
            sectionDao.getSectionsForManuscriptOnce(manuscriptId)
        }

    fun getSectionById(id: Long): Flow<SectionEntity?> = sectionDao.getSectionById(id)

    suspend fun getSectionByIdOnce(id: Long): SectionEntity? = withContext(Dispatchers.IO) {
        sectionDao.getSectionByIdOnce(id)
    }

    fun getCommentsForSection(sectionId: Long): Flow<List<EditorialCommentEntity>> =
        commentDao.getCommentsForSection(sectionId)

    fun getCommentsForManuscript(manuscriptId: Long): Flow<List<EditorialCommentEntity>> =
        commentDao.getCommentsForManuscript(manuscriptId)

    suspend fun getCommentsForManuscriptOnce(manuscriptId: Long): List<EditorialCommentEntity> =
        withContext(Dispatchers.IO) { commentDao.getCommentsForManuscriptOnce(manuscriptId) }

    fun getCharactersForManuscript(manuscriptId: Long): Flow<List<com.supernatural.bwriter.model.CharacterEntity>> =
        characterDao.getCharactersForManuscript(manuscriptId)

    suspend fun getCharactersForManuscriptOnce(manuscriptId: Long): List<com.supernatural.bwriter.model.CharacterEntity> =
        withContext(Dispatchers.IO) { characterDao.getCharactersForManuscriptOnce(manuscriptId) }

    suspend fun insertCharacter(character: com.supernatural.bwriter.model.CharacterEntity): Long =
        withContext(Dispatchers.IO) { characterDao.insertCharacter(character) }

    suspend fun updateCharacter(character: com.supernatural.bwriter.model.CharacterEntity) =
        withContext(Dispatchers.IO) { characterDao.updateCharacter(character) }

    suspend fun deleteCharacterById(id: Long) =
        withContext(Dispatchers.IO) { characterDao.deleteCharacterById(id) }

    fun getSettingsForManuscript(manuscriptId: Long): Flow<List<com.supernatural.bwriter.model.StorySettingEntity>> =
        settingDao.getSettingsForManuscript(manuscriptId)

    suspend fun getSettingsForManuscriptOnce(manuscriptId: Long): List<com.supernatural.bwriter.model.StorySettingEntity> =
        withContext(Dispatchers.IO) { settingDao.getSettingsForManuscriptOnce(manuscriptId) }

    suspend fun insertSetting(setting: com.supernatural.bwriter.model.StorySettingEntity): Long =
        withContext(Dispatchers.IO) { settingDao.insertSetting(setting) }

    suspend fun updateSetting(setting: com.supernatural.bwriter.model.StorySettingEntity) =
        withContext(Dispatchers.IO) { settingDao.updateSetting(setting) }

    suspend fun deleteSettingById(id: Long) =
        withContext(Dispatchers.IO) { settingDao.deleteSettingById(id) }

    suspend fun createManuscript(
        manuscript: ManuscriptEntity,
        populateTemplate: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
        val manuscriptId = manuscriptDao.insertManuscript(manuscript)

        if (populateTemplate) {
            createTemplateSectionsForWork(manuscriptId, manuscript.workType, manuscript.authorName)
        }
        manuscriptId
        }
    }

    private suspend fun createTemplateSectionsForWork(manuscriptId: Long, workType: WorkType, authorName: String) {
        val sections = when (workType) {
            WorkType.NOVEL -> listOf(
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.FRONT_MATTER,
                    sectionType = SectionType.PREFACE,
                    title = "Preface",
                    orderIndex = 1,
                    content = "A brief statement on the origin and themes of the novel.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.TEXT_BODY,
                    sectionType = SectionType.CHAPTER,
                    title = "Chapter I",
                    subtitle = "The Journey Begins",
                    orderIndex = 2,
                    content = "The morning light broke through the leaded glass windows, illuminating the quiet study. Write your opening prose here with CMOS dialogue and narrative flow...",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.BACK_MATTER,
                    sectionType = SectionType.ABOUT_AUTHOR,
                    title = "About the Author",
                    orderIndex = 3,
                    content = "$authorName is a contemporary novelist and essayist.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                )
            )
            WorkType.BIOGRAPHY -> listOf(
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.FRONT_MATTER,
                    sectionType = SectionType.PREFACE,
                    title = "Author's Preface",
                    orderIndex = 1,
                    content = "Scope and methodology of the historical investigation.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.TEXT_BODY,
                    sectionType = SectionType.CHAPTER,
                    title = "Chapter I: Early Years",
                    subtitle = "Heritage and Formative Influences",
                    orderIndex = 2,
                    content = "The historical subject was born into a pivotal era...",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.BACK_MATTER,
                    sectionType = SectionType.BIBLIOGRAPHY,
                    title = "Selected Bibliography",
                    orderIndex = 3,
                    content = "Primary sources, archival documents, and contemporary periodicals.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                )
            )
            WorkType.DOCUMENTARY -> listOf(
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.FRONT_MATTER,
                    sectionType = SectionType.INTRODUCTION,
                    title = "Documentary Introduction",
                    orderIndex = 1,
                    content = "Statement of investigative thesis and archival access.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.TEXT_BODY,
                    sectionType = SectionType.CHAPTER,
                    title = "Chapter I: The Field Evidence",
                    subtitle = "First-Hand Observations and Recordings",
                    orderIndex = 2,
                    content = "The documentary team arrived on site during the autumn expedition...",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.BACK_MATTER,
                    sectionType = SectionType.APPENDIX,
                    title = "Appendix: Transcripts and Documents",
                    orderIndex = 3,
                    content = "Full unedited transcripts of key witness interviews.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                )
            )
            WorkType.MANUAL -> listOf(
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.FRONT_MATTER,
                    sectionType = SectionType.PREFACE,
                    title = "Manual Scope & Overview",
                    orderIndex = 1,
                    content = "System architecture, procedural conventions, and operations guide.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.TEXT_BODY,
                    sectionType = SectionType.CHAPTER,
                    title = "Chapter 1: Getting Started",
                    subtitle = "Core Instructions & Workflows",
                    orderIndex = 2,
                    content = "Follow these structured procedural steps for initial deployment...",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                ),
                SectionEntity(
                    manuscriptId = manuscriptId,
                    matterType = MatterType.BACK_MATTER,
                    sectionType = SectionType.GLOSSARY,
                    title = "Glossary & References",
                    orderIndex = 3,
                    content = "Definition of technical terms and cited specifications.",
                    assignedAuthor = authorName,
                    assignedRole = WorkRole.AUTHOR,
                    status = SectionStatus.DRAFT
                )
            )
        }
        sectionDao.insertSections(sections)
    }

suspend fun updateManuscript(manuscript: ManuscriptEntity) = withContext(Dispatchers.IO) {
        manuscriptDao.updateManuscript(manuscript.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteManuscript(id: Long) = withContext(Dispatchers.IO) {
        manuscriptDao.deleteManuscriptById(id)
    }

    suspend fun insertSection(section: SectionEntity): Long = withContext(Dispatchers.IO) {
        val wordCount = countWords(section.content)
        val id = sectionDao.insertSection(section.copy(wordCount = wordCount, updatedAt = System.currentTimeMillis()))
        touchManuscript(section.manuscriptId)
        id
    }

    suspend fun updateSection(section: SectionEntity) = withContext(Dispatchers.IO) {
        val wordCount = countWords(section.content)
        sectionDao.updateSection(section.copy(wordCount = wordCount, updatedAt = System.currentTimeMillis()))
        touchManuscript(section.manuscriptId)
    }

    suspend fun updateSectionContent(section: SectionEntity, newContent: String) = withContext(Dispatchers.IO) {
        sectionDao.updateSectionContent(section.id, newContent, countWords(newContent), System.currentTimeMillis())
        touchManuscript(section.manuscriptId)
    }

    suspend fun updateSectionTitle(section: SectionEntity, newTitle: String, newSubtitle: String) = withContext(Dispatchers.IO) {
        sectionDao.updateSectionTitle(section.id, newTitle, newSubtitle, System.currentTimeMillis())
        touchManuscript(section.manuscriptId)
    }

    suspend fun updateSectionIllustrations(section: SectionEntity, headerUri: String, headerCaption: String, tailUri: String, tailCaption: String) = withContext(Dispatchers.IO) {
        sectionDao.updateSectionIllustrations(section.id, headerUri, headerCaption, tailUri, tailCaption, System.currentTimeMillis())
        touchManuscript(section.manuscriptId)
    }

    suspend fun deleteSection(id: Long, manuscriptId: Long) = withContext(Dispatchers.IO) {
        sectionDao.deleteSectionById(id)
        touchManuscript(manuscriptId)
    }

    suspend fun addComment(comment: EditorialCommentEntity): Long = withContext(Dispatchers.IO) {
        commentDao.insertComment(comment)
    }

    suspend fun setCommentResolved(commentId: Long, resolved: Boolean) = withContext(Dispatchers.IO) {
        commentDao.setResolved(commentId, resolved)
    }

    suspend fun proposeRevision(
        section: SectionEntity,
        newContent: String,
        editorUser: UserProfile
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
        val originalText = if (section.hasPendingRevision && section.originalAuthorContent.isNotBlank()) {
            section.originalAuthorContent
        } else {
            section.content
        }

        val originalWords = countWords(originalText)
        val newWords = countWords(newContent)
        val deltaWords = if (newContent == originalText) 0 else abs(newWords - originalWords).coerceAtLeast(1)

        val updatedSection = section.copy(
            hasPendingRevision = true,
            pendingEditedContent = newContent,
            originalAuthorContent = originalText,
            revisionAuthorPenName = if (editorUser.penName.isNotBlank()) editorUser.penName else editorUser.name,
            revisionAuthorEmail = editorUser.email,
            revisionAuthorRole = editorUser.role.displayName,
            revisionTimestamp = System.currentTimeMillis(),
            revisionDeltaWords = deltaWords,
            status = SectionStatus.UNDER_REVIEW,
            updatedAt = System.currentTimeMillis()
        )
        sectionDao.updateSection(updatedSection)

        val manuscript = manuscriptDao.getManuscriptByIdOnce(section.manuscriptId)
        if (manuscript != null) {
            val existingCredits = manuscript.acknowledgmentsList.toMutableList()
            val contributorPenName = if (editorUser.penName.isNotBlank()) editorUser.penName else editorUser.name

            val existingIndex = existingCredits.indexOfFirst {
                it.email.equals(editorUser.email, ignoreCase = true) || it.penName.equals(contributorPenName, ignoreCase = true)
            }
            if (existingIndex >= 0) {
                val existing = existingCredits[existingIndex]
                existingCredits[existingIndex] = existing.copy(
                    penName = contributorPenName,
                    role = editorUser.role.displayName,
                    wordsContributed = existing.wordsContributed + deltaWords,
                    commitTimestamp = System.currentTimeMillis(),
                    chapterTitle = section.title
                )
            } else {
                existingCredits.add(
                    ContributorCredit(
                        penName = contributorPenName,
                        email = editorUser.email,
                        role = editorUser.role.displayName,
                        wordsContributed = deltaWords,
                        commitTimestamp = System.currentTimeMillis(),
                        chapterTitle = section.title
                    )
                )
            }

            manuscriptDao.updateManuscript(
                manuscript.copy(
                    manuscriptStatus = "UNDER_REVIEW",
                    acknowledgmentsJson = ContributorCredit.listToJsonString(existingCredits),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        }
    }

    suspend fun acceptRevision(section: SectionEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
        val mergedContent = if (section.pendingEditedContent.isNotBlank()) section.pendingEditedContent else section.content
        val wordCount = countWords(mergedContent)

        val updatedSection = section.copy(
            content = mergedContent,
            hasPendingRevision = false,
            pendingEditedContent = "",
            status = SectionStatus.POLISHED,
            wordCount = wordCount,
            updatedAt = System.currentTimeMillis()
        )
        sectionDao.updateSection(updatedSection)

        val allSections = sectionDao.getSectionsForManuscriptOnce(section.manuscriptId)
        val hasOtherPending = allSections.any { it.id != section.id && it.hasPendingRevision }

        val manuscript = manuscriptDao.getManuscriptByIdOnce(section.manuscriptId)
        if (manuscript != null) {
            manuscriptDao.updateManuscript(
                manuscript.copy(
                    manuscriptStatus = if (hasOtherPending) "UNDER_REVIEW" else "POLISHED",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        }
    }

    suspend fun rejectRevision(section: SectionEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
        val restoredContent = if (section.originalAuthorContent.isNotBlank()) section.originalAuthorContent else section.content
        val wordCount = countWords(restoredContent)

        val updatedSection = section.copy(
            content = restoredContent,
            hasPendingRevision = false,
            pendingEditedContent = "",
            status = SectionStatus.DRAFT,
            wordCount = wordCount,
            updatedAt = System.currentTimeMillis()
        )
        sectionDao.updateSection(updatedSection)

        val allSections = sectionDao.getSectionsForManuscriptOnce(section.manuscriptId)
        val hasOtherPending = allSections.any { it.id != section.id && it.hasPendingRevision }

        val manuscript = manuscriptDao.getManuscriptByIdOnce(section.manuscriptId)
        if (manuscript != null) {
            manuscriptDao.updateManuscript(
                manuscript.copy(
                    manuscriptStatus = if (hasOtherPending) "UNDER_REVIEW" else "DRAFT",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        }
    }

    private suspend fun touchManuscript(manuscriptId: Long) {
        val m = manuscriptDao.getManuscriptByIdOnce(manuscriptId)
        if (m != null) {
            manuscriptDao.updateManuscript(m.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    private fun countWords(text: String): Int {
        return text.split(Regex("""\s+""")).count { it.isNotBlank() }
    }

}