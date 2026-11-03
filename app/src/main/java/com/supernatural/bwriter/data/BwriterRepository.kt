package com.supernatural.bwriter.data

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

    // Character Profiling
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

    // Settings & Worldbuilding
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
        val manuscriptId = manuscriptDao.insertManuscript(manuscript)

        if (populateTemplate) {
            createTemplateSectionsForWork(manuscriptId, manuscript.workType, manuscript.authorName)
        }
        manuscriptId
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

    /**
     * Propose an edit / revision by an Editor or Contributor on a section they did not author.
     * Marks the section as UNDER_REVIEW with red-highlight tracked changes and logs acknowledgment telemetry.
     */
    suspend fun proposeRevision(
        section: SectionEntity,
        newContent: String,
        editorUser: UserProfile
    ) = withContext(Dispatchers.IO) {
        val originalText = if (section.hasPendingRevision && section.originalAuthorContent.isNotBlank()) {
            section.originalAuthorContent
        } else {
            section.content
        }

        val originalWords = countWords(originalText)
        val newWords = countWords(newContent)
        val deltaWords = abs(newWords - originalWords).coerceAtLeast(newWords.coerceAtLeast(1))

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

        // Update manuscript acknowledgment telemetry and status
        val manuscript = manuscriptDao.getManuscriptByIdOnce(section.manuscriptId)
        if (manuscript != null) {
            val existingCredits = manuscript.acknowledgmentsList.toMutableList()
            val contributorPenName = if (editorUser.penName.isNotBlank()) editorUser.penName else editorUser.name

            // Find existing credit for this contributor or add new
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

    /**
     * Author accepts the proposed revision: merges the edited content into the official text.
     */
    suspend fun acceptRevision(section: SectionEntity) = withContext(Dispatchers.IO) {
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

        // Check if there are other pending revisions in the manuscript
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

    /**
     * Author rejects the proposed revision: discards pending edits and restores original content.
     */
    suspend fun rejectRevision(section: SectionEntity) = withContext(Dispatchers.IO) {
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