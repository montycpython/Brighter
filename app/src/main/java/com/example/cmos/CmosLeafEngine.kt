package com.example.cmos

import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionType

object CmosLeafEngine {

    private const val WORDS_PER_PAGE = 260 // Average words per 6x9 trade paperback leaf page

    /**
     * Computes the complete sequence of physical book leaves honoring Chicago Manual of Style
     * recto/verso conventions, roman front-matter pagination, and chapter opener leaf positioning.
     */
    fun calculateLeaves(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>
    ): List<CalculatedLeaf> {
        val leaves = mutableListOf<CalculatedLeaf>()

        var frontMatterPage = 1
        var bodyPage = 1

        val frontSections = sections.filter { it.matterType == MatterType.FRONT_MATTER }
        val bodySections = sections.filter { it.matterType == MatterType.TEXT_BODY }
        val backSections = sections.filter { it.matterType == MatterType.BACK_MATTER }

        // ==========================================
        // 1. FRONT MATTER (Leaves in Roman Numerals)
        // ==========================================
        // Leaf 1: Half-Title (Recto, p. i)
        leaves.add(
            CalculatedLeaf(
                leafIndex = leaves.size + 1,
                pageNumberDisplay = "i",
                pageNumberRaw = frontMatterPage,
                side = LeafSide.RECTO,
                matterType = MatterType.FRONT_MATTER,
                sectionId = null,
                sectionTitle = "Half-Title",
                sectionType = SectionType.HALF_TITLE,
                displayType = LeafDisplayType.HALF_TITLE,
                contentSnippet = manuscript.title.uppercase(),
                isOpener = true,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Leaf 2: Blank or Series Verso (Verso, p. ii)
        leaves.add(
            CalculatedLeaf(
                leafIndex = leaves.size + 1,
                pageNumberDisplay = "ii",
                pageNumberRaw = frontMatterPage,
                side = LeafSide.VERSO,
                matterType = MatterType.FRONT_MATTER,
                sectionId = null,
                sectionTitle = "Blank / Series",
                sectionType = null,
                displayType = LeafDisplayType.BLANK_INTENTIONAL,
                contentSnippet = "",
                isOpener = false,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Leaf 3: Full Title Page (Recto, p. iii)
        leaves.add(
            CalculatedLeaf(
                leafIndex = leaves.size + 1,
                pageNumberDisplay = "iii",
                pageNumberRaw = frontMatterPage,
                side = LeafSide.RECTO,
                matterType = MatterType.FRONT_MATTER,
                sectionId = null,
                sectionTitle = "Title Page",
                sectionType = SectionType.TITLE_PAGE,
                displayType = LeafDisplayType.TITLE_PAGE,
                contentSnippet = "${manuscript.title}\n${manuscript.subtitle}\n\nBy ${manuscript.authorName}\n${manuscript.publisher}",
                isOpener = true,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Leaf 4: Copyright & Colophon (Verso, p. iv)
        leaves.add(
            CalculatedLeaf(
                leafIndex = leaves.size + 1,
                pageNumberDisplay = "iv",
                pageNumberRaw = frontMatterPage,
                side = LeafSide.VERSO,
                matterType = MatterType.FRONT_MATTER,
                sectionId = null,
                sectionTitle = "Copyright & Colophon",
                sectionType = SectionType.COPYRIGHT_COLOPHON,
                displayType = LeafDisplayType.COPYRIGHT,
                contentSnippet = manuscript.copyrightText + "\nISBN: " + manuscript.isbn,
                isOpener = false,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Dedication & Epigraph if present
        if (manuscript.dedication.isNotBlank()) {
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.RECTO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Dedication",
                    sectionType = SectionType.DEDICATION,
                    displayType = LeafDisplayType.DEDICATION,
                    contentSnippet = manuscript.dedication,
                    isOpener = true,
                    hasBlindFolio = true
                )
            )
            frontMatterPage++

            // Blank verso after dedication
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.VERSO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Blank Verso",
                    sectionType = null,
                    displayType = LeafDisplayType.BLANK_INTENTIONAL,
                    contentSnippet = "",
                    isOpener = false,
                    hasBlindFolio = true
                )
            )
            frontMatterPage++
        }

        if (manuscript.epigraphText.isNotBlank()) {
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.RECTO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Epigraph",
                    sectionType = SectionType.EPIGRAPH,
                    displayType = LeafDisplayType.EPIGRAPH,
                    contentSnippet = "${manuscript.epigraphText}\n— ${manuscript.epigraphAuthor}",
                    isOpener = true,
                    hasBlindFolio = true
                )
            )
            frontMatterPage++

            // Blank verso after epigraph
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.VERSO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Blank Verso",
                    sectionType = null,
                    displayType = LeafDisplayType.BLANK_INTENTIONAL,
                    contentSnippet = "",
                    isOpener = false,
                    hasBlindFolio = true
                )
            )
            frontMatterPage++
        }

        // Additional custom Front Matter sections (Table of Contents, Foreword, Preface, Acknowledgments, Introduction)
        for (sec in frontSections) {
            // Front matter divisions must start on Recto
            if (leaves.size % 2 != 0 && sec.startOnRecto) {
                // Currently on Recto (odd size means next index is even/verso), which is fine.
                // If next leaf would be Verso, we insert a blank Verso so the division starts on Recto!
                val nextSide = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                if (nextSide == LeafSide.VERSO) {
                    leaves.add(
                        CalculatedLeaf(
                            leafIndex = leaves.size + 1,
                            pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                            pageNumberRaw = frontMatterPage,
                            side = LeafSide.VERSO,
                            matterType = MatterType.FRONT_MATTER,
                            sectionId = null,
                            sectionTitle = "Blank Verso",
                            sectionType = null,
                            displayType = LeafDisplayType.BLANK_INTENTIONAL,
                            contentSnippet = "",
                            isOpener = false,
                            hasBlindFolio = true
                        )
                    )
                    frontMatterPage++
                }
            }

            val words = sec.content.split(Regex("""\s+""")).filter { it.isNotBlank() }
            val pageCount = maxOf(1, (words.size + WORDS_PER_PAGE - 1) / WORDS_PER_PAGE)

            for (p in 0 until pageCount) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val startWord = p * WORDS_PER_PAGE
                val endWord = minOf(words.size, (p + 1) * WORDS_PER_PAGE)
                val snippet = if (words.isNotEmpty()) words.subList(startWord, endWord).joinToString(" ") else sec.content

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                        pageNumberRaw = frontMatterPage,
                        side = side,
                        matterType = MatterType.FRONT_MATTER,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionType = sec.sectionType,
                        displayType = if (p == 0) LeafDisplayType.CHAPTER_OPENER else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = p == 0,
                        runningHeadVerso = manuscript.title,
                        runningHeadRecto = sec.title,
                        hasBlindFolio = p == 0
                    )
                )
                frontMatterPage++
            }
        }

        // ==========================================
        // 2. TEXT / MAIN BODY (Arabic Numerals 1, 2, 3...)
        // ==========================================
        // Ensure Main Text starts on a RECTO (odd) leaf
        if ((leaves.size + 1) % 2 == 0) {
            // Next is Verso, insert blank leaf to start on Recto
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.VERSO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Blank Verso",
                    sectionType = null,
                    displayType = LeafDisplayType.BLANK_INTENTIONAL,
                    contentSnippet = "",
                    isOpener = false,
                    hasBlindFolio = true
                )
            )
        }

        // Now process Body Sections
        for (sec in bodySections) {
            // CMOS mandate: Chapters & Part Openers start on Recto leaf
            if (sec.startOnRecto) {
                val nextLeafNumber = leaves.size + 1
                val isNextRecto = nextLeafNumber % 2 != 0
                if (!isNextRecto) {
                    // It would land on Verso (even). Insert blank Verso leaf!
                    leaves.add(
                        CalculatedLeaf(
                            leafIndex = leaves.size + 1,
                            pageNumberDisplay = bodyPage.toString(),
                            pageNumberRaw = bodyPage,
                            side = LeafSide.VERSO,
                            matterType = MatterType.TEXT_BODY,
                            sectionId = null,
                            sectionTitle = "Blank Verso (CMOS Recto Opener)",
                            sectionType = null,
                            displayType = LeafDisplayType.BLANK_INTENTIONAL,
                            contentSnippet = "",
                            isOpener = false,
                            hasBlindFolio = true
                        )
                    )
                    bodyPage++
                }
            }

            val words = sec.content.split(Regex("""\s+""")).filter { it.isNotBlank() }
            val pageCount = maxOf(1, (words.size + WORDS_PER_PAGE - 1) / WORDS_PER_PAGE)

            for (p in 0 until pageCount) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val startWord = p * WORDS_PER_PAGE
                val endWord = minOf(words.size, (p + 1) * WORDS_PER_PAGE)
                val snippet = if (words.isNotEmpty()) words.subList(startWord, endWord).joinToString(" ") else sec.content

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = bodyPage.toString(),
                        pageNumberRaw = bodyPage,
                        side = side,
                        matterType = MatterType.TEXT_BODY,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionType = sec.sectionType,
                        displayType = if (p == 0) {
                            if (sec.sectionType == SectionType.PART_DIVIDER) LeafDisplayType.PART_OPENER
                            else LeafDisplayType.CHAPTER_OPENER
                        } else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = p == 0,
                        runningHeadVerso = manuscript.authorName.ifBlank { manuscript.title },
                        runningHeadRecto = sec.title,
                        hasBlindFolio = p == 0 // Chapter opener has drop folio or blind folio
                    )
                )
                bodyPage++
            }
        }

        // ==========================================
        // 3. BACK MATTER (Arabic Numerals Continuous)
        // ==========================================
        for (sec in backSections) {
            if (sec.startOnRecto) {
                val nextLeafNumber = leaves.size + 1
                val isNextRecto = nextLeafNumber % 2 != 0
                if (!isNextRecto) {
                    leaves.add(
                        CalculatedLeaf(
                            leafIndex = leaves.size + 1,
                            pageNumberDisplay = bodyPage.toString(),
                            pageNumberRaw = bodyPage,
                            side = LeafSide.VERSO,
                            matterType = MatterType.BACK_MATTER,
                            sectionId = null,
                            sectionTitle = "Blank Verso",
                            sectionType = null,
                            displayType = LeafDisplayType.BLANK_INTENTIONAL,
                            contentSnippet = "",
                            isOpener = false,
                            hasBlindFolio = true
                        )
                    )
                    bodyPage++
                }
            }

            val words = sec.content.split(Regex("""\s+""")).filter { it.isNotBlank() }
            val pageCount = maxOf(1, (words.size + WORDS_PER_PAGE - 1) / WORDS_PER_PAGE)

            for (p in 0 until pageCount) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val startWord = p * WORDS_PER_PAGE
                val endWord = minOf(words.size, (p + 1) * WORDS_PER_PAGE)
                val snippet = if (words.isNotEmpty()) words.subList(startWord, endWord).joinToString(" ") else sec.content

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = bodyPage.toString(),
                        pageNumberRaw = bodyPage,
                        side = side,
                        matterType = MatterType.BACK_MATTER,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionType = sec.sectionType,
                        displayType = if (p == 0) LeafDisplayType.CHAPTER_OPENER else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = p == 0,
                        runningHeadVerso = manuscript.title,
                        runningHeadRecto = sec.title,
                        hasBlindFolio = p == 0
                    )
                )
                bodyPage++
            }
        }

        return leaves
    }
}
