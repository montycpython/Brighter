package com.example.data

import com.example.cmos.CmosFormatter
import com.example.model.ContributorCredit
import com.example.model.EditorialCommentEntity
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionStatus
import com.example.model.SectionType
import com.example.model.UserProfile
import com.example.model.WorkRole
import com.example.model.WorkType
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
    fun getCharactersForManuscript(manuscriptId: Long): Flow<List<com.example.model.CharacterEntity>> =
        characterDao.getCharactersForManuscript(manuscriptId)

    suspend fun getCharactersForManuscriptOnce(manuscriptId: Long): List<com.example.model.CharacterEntity> =
        withContext(Dispatchers.IO) { characterDao.getCharactersForManuscriptOnce(manuscriptId) }

    suspend fun insertCharacter(character: com.example.model.CharacterEntity): Long =
        withContext(Dispatchers.IO) { characterDao.insertCharacter(character) }

    suspend fun updateCharacter(character: com.example.model.CharacterEntity) =
        withContext(Dispatchers.IO) { characterDao.updateCharacter(character) }

    suspend fun deleteCharacterById(id: Long) =
        withContext(Dispatchers.IO) { characterDao.deleteCharacterById(id) }

    // Settings & Worldbuilding
    fun getSettingsForManuscript(manuscriptId: Long): Flow<List<com.example.model.StorySettingEntity>> =
        settingDao.getSettingsForManuscript(manuscriptId)

    suspend fun getSettingsForManuscriptOnce(manuscriptId: Long): List<com.example.model.StorySettingEntity> =
        withContext(Dispatchers.IO) { settingDao.getSettingsForManuscriptOnce(manuscriptId) }

    suspend fun insertSetting(setting: com.example.model.StorySettingEntity): Long =
        withContext(Dispatchers.IO) { settingDao.insertSetting(setting) }

    suspend fun updateSetting(setting: com.example.model.StorySettingEntity) =
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

    suspend fun seedInitialWorksIfEmpty() = withContext(Dispatchers.IO) {
        val existing = manuscriptDao.getManuscriptByIdOnce(1)
        if (existing != null) {
            // Clean up any obsolete hardcoded TABLE_OF_CONTENTS sections from legacy seeds
            for (mId in 1L..4L) {
                val secList = sectionDao.getSectionsForManuscriptOnce(mId)
                for (sec in secList) {
                    if (sec.sectionType == SectionType.TABLE_OF_CONTENTS || sec.title.equals("Table of Contents", ignoreCase = true)) {
                        sectionDao.deleteSectionById(sec.id)
                    }
                }
            }
            // Ensure sample characters exist for novel 1
            val existingChars = characterDao.getCharactersForManuscriptOnce(1)
            if (existingChars.isEmpty()) {
                seedNovelCharactersAndSettings(1)
            }
            return@withContext
        }

        // 1. Novel Seed — Credited to Superuser "Author"
        val novel = ManuscriptEntity(
            title = "The Obsidian Quill",
            subtitle = "A Chronicle of the Chicago Printmasters",
            workType = WorkType.NOVEL,
            authorName = "Author",
            authorPenName = "A. V. Hawthorne",
            authorEmail = "real.artistry@gmail.com",
            editorName = "Eleanor Rigby, Senior Editor",
            publisher = "Great Lakes University Press",
            edition = "First Edition",
            year = "2026",
            isbn = "978-0-226-81932-1",
            copyrightText = "Copyright © 2026 by Author.\nAll rights reserved under International and Pan-American Copyright Conventions.\nPublished in Chicago by Great Lakes University Press.",
            dedication = "To the linotype operators, hot-metal compositors, and proofreaders who gave body to the written thought.",
            epigraphText = "“There is a sanctity in the printed leaf which the spoken word can never claim.”",
            epigraphAuthor = "William Morris, 1891",
            manuscriptStatus = "POLISHED"
        )
        val novelId = manuscriptDao.insertManuscript(novel)
        seedNovelSections(novelId)
        seedNovelCharactersAndSettings(novelId)

        // 2. Biography Seed — Credited to Superuser "Author"
        val bio = ManuscriptEntity(
            title = "The Chronicler of Chicago",
            subtitle = "The Life and Letters of Silas Dearborn (1842–1918)",
            workType = WorkType.BIOGRAPHY,
            authorName = "Author",
            authorPenName = "E. R. Vance",
            authorEmail = "real.artistry@gmail.com",
            editorName = "Marcus Sterling",
            publisher = "Midwestern Historical Society Press",
            edition = "Definitive Edition",
            year = "2026",
            isbn = "978-0-300-24109-9",
            copyrightText = "Copyright © 2026 by Author.\nManufactured in the United States of America.\nCataloging-in-Publication Data available from the Library of Congress.",
            dedication = "In memory of the archival keepers of the Newberry Library.",
            epigraphText = "“Biography is the only true history.”",
            epigraphAuthor = "Thomas Carlyle",
            manuscriptStatus = "POLISHED"
        )
        val bioId = manuscriptDao.insertManuscript(bio)
        seedBiographySections(bioId)

        // 3. Documentary Seed — Credited to Superuser "Author"
        val doc = ManuscriptEntity(
            title = "Echoes of the Great Lake",
            subtitle = "An Oral and Archival Documentary of the Maritime Trades",
            workType = WorkType.DOCUMENTARY,
            authorName = "Author",
            authorPenName = "M. T. Sterling",
            authorEmail = "real.artistry@gmail.com",
            editorName = "Dr. Arthur Vance",
            publisher = "Maritime Documentary Editions",
            edition = "First Edition",
            year = "2026",
            isbn = "978-1-59853-702-4",
            copyrightText = "Copyright © 2026 by Author.\nAll rights reserved. No part of this publication may be reproduced without prior written permission.",
            dedication = "For the crews of the schooners and freighters of Lake Michigan.",
            epigraphText = "“The lake does not give up her secrets willingly; she writes them in deep water.”",
            epigraphAuthor = "Captain John MacAlister, 1888",
            manuscriptStatus = "POLISHED"
        )
        val docId = manuscriptDao.insertManuscript(doc)
        seedDocumentarySections(docId)

        // 4. Manual Seed — Credited to Superuser "Author"
        val manual = ManuscriptEntity(
            title = "The Craft of Book Typography",
            subtitle = "A Practical Manual Honoring the Chicago Manual of Style",
            workType = WorkType.MANUAL,
            authorName = "Author",
            authorPenName = "Author",
            authorEmail = "real.artistry@gmail.com",
            editorName = "Chief Typography Officer",
            publisher = "Bwriter Press & Chicago Typographic Guild",
            edition = "Second Revised Edition",
            year = "2026",
            isbn = "978-0-8047-9104-5",
            copyrightText = "Copyright © 2026 by Author.\nTypeset in Monotype Baskerville and Garamond.\nPrinted on 60# acid-free archival book paper.",
            dedication = "To every typographer who measures margins in points and preserves the dignity of the leaf.",
            epigraphText = "“Typography is the craft of endowing human language with a durable visual form.”",
            epigraphAuthor = "Robert Bringhurst",
            manuscriptStatus = "POLISHED"
        )
        val manualId = manuscriptDao.insertManuscript(manual)
        seedManualSections(manualId)
    }

    private suspend fun seedNovelSections(manuscriptId: Long) {
        val sections = listOf(
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.FRONT_MATTER,
                sectionType = SectionType.PREFACE,
                title = "Preface to the Reader",
                orderIndex = 1,
                content = "The narrative contained in these pages was drawn from the preserved logbooks of the Dearborn Foundry and the personal journals of Master Typesetter Alistair Thorne. In transcribing these records, we have adhered strictly to the punctuation and orthography of the late nineteenth century, while ensuring the typographic presentation conforms to modern editorial standards.\n\nEvery leaf has been composed with the understanding that a book is not merely a vehicle for text, but an intimate architectural space where author and reader converse.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.FINAL,
                wordCount = 85
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter I: The Foundry at Dusk",
                subtitle = "Wherein the Molten Lead Casts Its Spell",
                orderIndex = 2,
                content = "The fog rolled off Lake Michigan with the heavy, damp chill of late November, wrapping the brick warehouses of South Clark Street in a woolen shroud. Inside the Dearborn Foundry, the furnace glowed like a captive ember, throwing long, trembling shadows across the cases of pica and brevier type.\n\nAlistair Thorne stood before his imposing stone, a composing stick resting lightly in his left hand. His fingers moved with the instinctive rhythm of thirty years at the trade—picking sorts from the upper and lower cases, sliding brass thins into place, and verifying the alignment with the delicate touch of a jeweler.\n\n“Mind the gutter margin on the fifth leaf, Thorne,” warned Old Mercer, squinting over his wire-rimmed spectacles from the proofing press. “The University will reject the entire run if the binding swallows the inner folio.”\n\n“The Chicago rules are burned into my retinas, Mercer,” Thorne replied without breaking his stride. “Inner gutter set to forty-five points, running heads on the verso bearing the work title, and every chapter opener starting proudly on the recto.”\n\nOutside, the steam whistle of the evening freight sounded across the river, signaling the arrival of fresh rag paper from the Wisconsin mills.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 205
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter II: The Lost Galleys",
                subtitle = "The Discovery of the Uncorrected Proofs",
                orderIndex = 3,
                content = "Morning arrived with the shrill cry of newsboys along Michigan Avenue. Alistair found the galley proofs strewn across the mahogany table of the reader’s alcove, marked with vermilion ink in the decisive hand of Miss Eleanor Rigby.\n\n“Notice the missing serial commas in the second paragraph,” she said, stepping from behind the cedar screen. “The Chicago Manual of Style is unequivocal on this matter: in a series of three or more elements, the conjunction must be preceded by a comma. We do not permit ambiguity in our prose.”\n\nThorne examined the marks. Her annotations were razor-sharp, noting every straight quote that demanded replacement by typographical curly quotes and flagging parenthetical dashes that lacked the true em-width.\n\n“It shall be corrected before the cylinder press begins its run,” Thorne conceded, admiring the relentless precision of her editorial eye.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 155
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.BACK_MATTER,
                sectionType = SectionType.ABOUT_AUTHOR,
                title = "About the Author",
                orderIndex = 4,
                content = "Author is a dedicated bibliographic scholar and Fellow of the Chicago Typographic Guild. Residing in Chicago, they preserve the classical traditions of recto-verso leaf design.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.FINAL,
                wordCount = 28
            )
        )
        sectionDao.insertSections(sections)

        val comment = EditorialCommentEntity(
            sectionId = 2,
            manuscriptId = manuscriptId,
            authorName = "Eleanor Rigby",
            authorRole = WorkRole.EDITOR,
            commentText = "Chapter I opening is exceptionally atmospheric. Ensure the running heads on verso leaves alternate with author byline per CMOS §1.12.",
            cmosRuleReference = "CMOS 17th Ed. §1.12 (Running Heads & Folios)",
            isResolved = true
        )
        commentDao.insertComment(comment)
    }

    private suspend fun seedBiographySections(manuscriptId: Long) {
        val sections = listOf(
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.FRONT_MATTER,
                sectionType = SectionType.FOREWORD,
                title = "Foreword",
                orderIndex = 1,
                content = "Silas Dearborn stood at the fulcrum of Chicago’s transformation from a muddy mercantile outpost to the printing capital of the American interior. In this definitive biography, archival rigor stands as the benchmark for decades to come.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.CONTRIBUTOR,
                status = SectionStatus.FINAL,
                wordCount = 38
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter I: Youth on the Prairie",
                subtitle = "The Early Years in Sangamon County",
                orderIndex = 2,
                content = "Born in a timber cabin four miles south of Springfield in 1842, Silas Dearborn learned his letters from a battered copy of Franklin’s Autobiography. By the age of fourteen, he had secured an apprenticeship at the Sangamon Daily Gazette, where he washed ink rollers and set display advertisements by the flickering light of tallow candles.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 60
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.BACK_MATTER,
                sectionType = SectionType.BIBLIOGRAPHY,
                title = "Selected Bibliography",
                orderIndex = 3,
                content = "Dearborn, Silas. Letters to a Young Typographer. Chicago: Prairie Press, 1898.\n\nFranklin, Benjamin. The Autobiography of Benjamin Franklin. Edited by Leonard W. Labaree. New Haven: Yale University Press, 1964.\n\nUniversity of Chicago Press. The Chicago Manual of Style. 17th ed. Chicago: University of Chicago Press, 2017.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.FINAL,
                wordCount = 48
            )
        )
        sectionDao.insertSections(sections)
    }

    private suspend fun seedDocumentarySections(manuscriptId: Long) {
        val sections = listOf(
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.FRONT_MATTER,
                sectionType = SectionType.INTRODUCTION,
                title = "Introduction: The Frozen Waterway",
                orderIndex = 1,
                content = "Between 1870 and 1920, more than two thousand vessels foundered upon the treacherous waters of Lake Michigan. This documentary assembles for the first time the surviving telegrams, harbor master logs, and oral histories of the men and women who braved the gales.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 45
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter I: The Gale of November 1888",
                subtitle = "Surviving the Thirty-Six Hour Tempest",
                orderIndex = 2,
                content = "At 4:00 p.m. on November 16, the barometric pressure in Milwaukee plunged to 28.92 inches. Captain MacAlister ordered all canvas reefed aboard the three-masted schooner *Silver Wave*. By nightfall, sixty-knot winds had torn the rudder adrift.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 40
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.BACK_MATTER,
                sectionType = SectionType.APPENDIX,
                title = "Appendix A: Register of Shipwrecks",
                orderIndex = 3,
                content = "Table of Maritime Casualties on Southern Lake Michigan (1880–1900), compiled from United States Life-Saving Service annual reports and Chicago Board of Underwriters records.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.DRAFT,
                wordCount = 26
            )
        )
        sectionDao.insertSections(sections)
    }

    private suspend fun seedManualSections(manuscriptId: Long) {
        val sections = listOf(
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.FRONT_MATTER,
                sectionType = SectionType.PREFACE,
                title = "Typographic Standards & Principles",
                orderIndex = 1,
                content = "This manual establishes the typographic benchmarks for Chicago Manual of Style composition, leaf pagination, recto-verso balance, and dynamic table of contents generation. Adherence to these classical standards ensures that digital manuscripts achieve the structural elegance of hot-metal presscraft.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.FINAL,
                wordCount = 45
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.TEXT_BODY,
                sectionType = SectionType.CHAPTER,
                title = "Chapter 1: The Anatomy of the Leaf",
                subtitle = "Principles of Recto and Verso Placement",
                orderIndex = 2,
                content = "In classical book design, the fundamental unit of composition is not the individual page, but the physical leaf with its two facing sides: recto (right) and verso (left).\n\nUnder the Chicago Manual of Style:\n1. All major divisions (Half-title, Title page, TOC, Foreword, Preface, Chapter 1, and Back Matter) must commence upon a Recto leaf.\n2. When a preceding division terminates on an odd page, an intentionally blank verso leaf must be introduced.\n3. Running heads must distinguish between facing pages: Verso carries the general book title or author, while Recto carries the specific chapter or section.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.POLISHED,
                wordCount = 105
            ),
            SectionEntity(
                manuscriptId = manuscriptId,
                matterType = MatterType.BACK_MATTER,
                sectionType = SectionType.GLOSSARY,
                title = "Glossary of Typographical Terms",
                orderIndex = 3,
                content = "Blind Folio: A page number counted in pagination but suppressed in print on display leaves and chapter openers.\n\nDrop Folio: A page number positioned at the foot of the page, centered or flush with the margin.\n\nGutter: The inner margin of facing pages adjacent to the binding spine.\n\nRecto: The front side of a leaf, always positioned on the right in a two-page spread and assigned an odd page number.\n\nVerso: The back side of a leaf, always positioned on the left in a two-page spread and assigned an even page number.",
                assignedAuthor = "Author",
                assignedRole = WorkRole.AUTHOR,
                status = SectionStatus.FINAL,
                wordCount = 98
            )
        )
        sectionDao.insertSections(sections)
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

    private suspend fun seedNovelCharactersAndSettings(manuscriptId: Long) {
        val characters = listOf(
            com.example.model.CharacterEntity(
                manuscriptId = manuscriptId,
                name = "Silas Vance",
                role = "Protagonist",
                physicalDescription = "Tall and lean with silver-streaked hair, ink-stained fingertips, dark woolen vest, silver pocket watch chain, keen gray eyes behind round wire-rimmed spectacles.",
                psychologicalDescription = "Obsessively meticulous master compositor. Haunted by the industrialization of the press trade; fiercely devoted to hot-metal craftsmanship, yet battling nocturnal doubts about his legacy.",
                backstory = "Apprenticed in the renowned Edinburgh printshops in 1868 before emigrating to Chicago; founded the St. Jude Private Press shortly after the Great Chicago Fire.",
                voiceAndMannerisms = "Speaks in measured, cadence-rich sentences with an undercurrent of dry Scots irony. Taps brass composing sticks rhythmically when contemplating layout problems.",
                intertextualArchetype = "Faustian seeker of aesthetic perfection / Ahab-like devotion to typographic purity",
                otherDetails = "Carries an 1874 brass pica rule passed down from his mentor; keeps a locked leather-bound diary of discarded font geometries."
            ),
            com.example.model.CharacterEntity(
                manuscriptId = manuscriptId,
                name = "Eleanor Rigby Vance",
                role = "Deuteragonist",
                physicalDescription = "Quick and energetic with raven curls pinned by boxwood bodkins, sharp hazel eyes, perpetually rolled linen sleeves, smudge of Prussian blue pigment on collarbone.",
                psychologicalDescription = "Brilliant archival scholar and typographic historian. Radical in her aesthetic visions; impatient with timid editorial conventions and eager to merge classical leaves with modern storytelling.",
                backstory = "Educated at the University of Chicago and the British Museum Library; returned to preserve her family's historic press amid rising commercial monopolies.",
                voiceAndMannerisms = "Rapid, incisive speech punctuated with Latin typographic citations and swift hand gestures. Tilts head with skeptical amusement when evaluating proofs.",
                intertextualArchetype = "Athena archetype / Dorothea Brooke's moral earnestness with a modern modernist edge",
                otherDetails = "Discovered an uncataloged 16th-century Aldine italic specimen leaf that forms the central mystery of the chronicle."
            ),
            com.example.model.CharacterEntity(
                manuscriptId = manuscriptId,
                name = "Marcus Sterling",
                role = "Antagonist / Foil",
                physicalDescription = "Broad-shouldered, tailored charcoal worsted suit, gold signet ring on right index finger, slicked amber hair, heavy resonant voice.",
                psychologicalDescription = "Ambitious industrial syndicate manager. Views printing solely through efficiency, automated high-speed matrices, and volume margins rather than typographic soul.",
                backstory = "Financed by East Coast publishing syndicates to buy out independent midwestern foundries and convert them into automated newsprint factories.",
                voiceAndMannerisms = "Projecting baritone, uses market metaphors, checks his gold repeater watch frequently during editorial disputes.",
                intertextualArchetype = "Industrial Gilded Age titan / The unrelenting spirit of mechanization",
                otherDetails = "Secretly respects Vance's artistry, making their clashes deeply philosophical rather than merely transactional."
            )
        )
        characterDao.insertCharacters(characters)

        val settings = listOf(
            com.example.model.StorySettingEntity(
                manuscriptId = manuscriptId,
                locationName = "The St. Jude Pressroom at Dusk (Chicago, 1888)",
                timePeriodOrEra = "Late 19th-Century Chicago (Autumn 1888)",
                atmosphereAndSensory = "Smell of boiled linseed oil, turpentine, and molten antimony; warm amber lantern glow dancing on cast-iron Miehle cylinder presses; the cold autumn wind rattling Victorian mullioned glass.",
                architecturalOrSpatialDetails = "Two-story brick warehouse on South Dearborn Street; heavy oak composing banks lined with California job cases; a central cast-iron stove glowing cherry-red.",
                historicalOrCulturalContext = "The era immediately preceding the Linotype machine revolution; Chicago's Printers Row bustling with trade unions, immigrant craftsmen, and steam-driven competition.",
                targetIntertextualTouchstones = "Theodore Dreiser's 'Sister Carrie' (industrial grit), Robert Bringhurst's 'Elements of Typographic Style' (sacred craftsmanship).",
                otherNotes = "The heart of the story where all crucial recto-verso confrontations take place."
            ),
            com.example.model.StorySettingEntity(
                manuscriptId = manuscriptId,
                locationName = "The Newberry Rare Manuscripts Vault",
                timePeriodOrEra = "Chicago Winter 1889",
                atmosphereAndSensory = "Whispering quietude, dry scent of ancient rag vellum and calfskin leather, dust motes caught in pale northern sunlight.",
                architecturalOrSpatialDetails = "Underground stone vaulted chamber with iron grille alcoves, mahogany reading lecterns, green-shaded banker lamps.",
                historicalOrCulturalContext = "Post-fire intellectual renaissance of the Midwest; private collectors competing for Renaissance incunabula.",
                targetIntertextualTouchstones = "Umberto Eco's 'The Name of the Rose', Henry James' 'The Turn of the Screw'.",
                otherNotes = "Where Eleanor deciphers the watermark clues."
            )
        )
        settingDao.insertSettings(settings)
    }
}
