package com.example.cmos

import com.example.model.BookTrimSize
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionType

object CmosLeafEngine {

    /**
     * Computes the complete sequence of physical book leaves honoring Chicago Manual of Style
     * recto/verso conventions, roman front-matter pagination, paragraph/dialogue line preservation,
     * and chapter opener leaf positioning with illustration clearances, adjusted dynamically for the
     * specified Book Trim Size.
     */
    fun calculateLeaves(
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>
    ): List<CalculatedLeaf> {
        val trimSize = BookTrimSize.fromTargetString(manuscript.targetPageSize)
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

        // Leaf 2: Blank Verso (Verso, p. ii)
        leaves.add(
            CalculatedLeaf(
                leafIndex = leaves.size + 1,
                pageNumberDisplay = "ii",
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

        // Leaf 3: Title Page (Recto, p. iii)
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
                contentSnippet = "${manuscript.title}\n${manuscript.subtitle}\n\nBy ${manuscript.effectiveAuthorByline}\n${manuscript.publisher}",
                isOpener = true,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Leaf 4: Copyright Page & Colophon (Verso, p. iv)
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
                contentSnippet = manuscript.effectiveCopyrightText + (if (manuscript.isbn.isNotBlank()) "\nISBN: " + manuscript.isbn else ""),
                isOpener = false,
                hasBlindFolio = true
            )
        )
        frontMatterPage++

        // Optional Dedication (Recto, p. v)
        if (manuscript.dedication.isNotBlank()) {
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = "v",
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

            // Blank verso after dedication (Verso, p. vi)
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = "vi",
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

        // Optional Epigraph (Recto)
        if (manuscript.epigraphText.isNotBlank()) {
            val isRecto = (leaves.size + 1) % 2 != 0
            if (!isRecto) {
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

            // Blank Verso after epigraph
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

        // Custom Front Matter sections (Foreword, Preface, Acknowledgments, Introduction)
        for (sec in frontSections) {
            if (leaves.size % 2 != 0 && sec.startOnRecto) {
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

            val pageSnippets = paginateSectionProse(
                content = sec.content,
                trimSize = trimSize,
                hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                hasSubtitle = sec.subtitle.isNotBlank()
            )

            for ((p, snippet) in pageSnippets.withIndex()) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val isOpener = p == 0
                val isCloser = p == pageSnippets.size - 1

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                        pageNumberRaw = frontMatterPage,
                        side = side,
                        matterType = MatterType.FRONT_MATTER,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionSubtitle = sec.subtitle,
                        sectionType = sec.sectionType,
                        displayType = if (isOpener) LeafDisplayType.CHAPTER_OPENER else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = isOpener,
                        isCloser = isCloser,
                        headerIllustrationUri = if (isOpener) sec.headerIllustrationUri else "",
                        headerIllustrationCaption = if (isOpener) sec.headerIllustrationCaption else "",
                        tailIllustrationUri = if (isCloser) sec.tailIllustrationUri else "",
                        tailIllustrationCaption = if (isCloser) sec.tailIllustrationCaption else "",
                        runningHeadVerso = manuscript.title,
                        runningHeadRecto = sec.title,
                        hasBlindFolio = isOpener
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

        // Process Body Sections with accurate height-based pagination
        for (sec in bodySections) {
            // CMOS mandate: Chapters & Part Openers start on Recto leaf
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

            val pageSnippets = paginateSectionProse(
                content = sec.content,
                trimSize = trimSize,
                hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                hasSubtitle = sec.subtitle.isNotBlank()
            )

            for ((p, snippet) in pageSnippets.withIndex()) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val isOpener = p == 0
                val isCloser = p == pageSnippets.size - 1

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = bodyPage.toString(),
                        pageNumberRaw = bodyPage,
                        side = side,
                        matterType = MatterType.TEXT_BODY,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionSubtitle = sec.subtitle,
                        sectionType = sec.sectionType,
                        displayType = if (isOpener) {
                            if (sec.sectionType == SectionType.PART_DIVIDER) LeafDisplayType.PART_OPENER
                            else LeafDisplayType.CHAPTER_OPENER
                        } else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = isOpener,
                        isCloser = isCloser,
                        headerIllustrationUri = if (isOpener) sec.headerIllustrationUri else "",
                        headerIllustrationCaption = if (isOpener) sec.headerIllustrationCaption else "",
                        tailIllustrationUri = if (isCloser) sec.tailIllustrationUri else "",
                        tailIllustrationCaption = if (isCloser) sec.tailIllustrationCaption else "",
                        runningHeadVerso = manuscript.effectiveAuthorByline.ifBlank { manuscript.title },
                        runningHeadRecto = sec.title,
                        hasBlindFolio = isOpener
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

            val pageSnippets = paginateSectionProse(
                content = sec.content,
                trimSize = trimSize,
                hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                hasSubtitle = sec.subtitle.isNotBlank()
            )

            for ((p, snippet) in pageSnippets.withIndex()) {
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val isOpener = p == 0
                val isCloser = p == pageSnippets.size - 1

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = bodyPage.toString(),
                        pageNumberRaw = bodyPage,
                        side = side,
                        matterType = MatterType.BACK_MATTER,
                        sectionId = sec.id,
                        sectionTitle = sec.title,
                        sectionSubtitle = sec.subtitle,
                        sectionType = sec.sectionType,
                        displayType = if (isOpener) LeafDisplayType.CHAPTER_OPENER else LeafDisplayType.CONTENT,
                        contentSnippet = snippet,
                        isOpener = isOpener,
                        isCloser = isCloser,
                        headerIllustrationUri = if (isOpener) sec.headerIllustrationUri else "",
                        headerIllustrationCaption = if (isOpener) sec.headerIllustrationCaption else "",
                        tailIllustrationUri = if (isCloser) sec.tailIllustrationUri else "",
                        tailIllustrationCaption = if (isCloser) sec.tailIllustrationCaption else "",
                        runningHeadVerso = manuscript.title,
                        runningHeadRecto = sec.title,
                        hasBlindFolio = isOpener
                    )
                )
                bodyPage++
            }
        }

        return leaves
    }

    /**
     * Accurately slices section content into physical page snippets, preserving all paragraph
     * newlines, dialogue speaker turns, and ensuring text fills the printable height down to the
     * 1-inch bottom margin without dropping text.
     */
    fun paginateSectionProse(
        content: String,
        trimSize: BookTrimSize,
        hasHeaderIllustration: Boolean = false,
        hasSubtitle: Boolean = false
    ): List<String> {
        if (content.isBlank()) return listOf("")

        val printableWidth = trimSize.widthPt - trimSize.gutterMarginPt - trimSize.outerMarginPt
        val fontSize = trimSize.defaultBodyFontSizePt
        val lineHeight = fontSize * trimSize.defaultLineHeightMultiplier + 3.2f
        val charsPerLine = (printableWidth / (fontSize * 0.48f)).toInt().coerceIn(45, 100)

        // Opener page vertical occupancy
        val openerOccupied = trimSize.topMarginPt + 20f + 22f + 10f +
                (if (hasSubtitle) 18f else 0f) +
                (if (hasHeaderIllustration) 110f else 0f) + 54f // 54pt bottom margin

        val availableHeightOpener = (trimSize.heightPt - openerOccupied).coerceAtLeast(200f)
        val maxLinesOpener = (availableHeightOpener / lineHeight).toInt().coerceAtLeast(20)

        // Continuation pages vertical occupancy
        val continuationOccupied = trimSize.topMarginPt + 10f + 54f // 54pt bottom margin
        val availableHeightContinuation = (trimSize.heightPt - continuationOccupied).coerceAtLeast(240f)
        val maxLinesContinuation = (availableHeightContinuation / lineHeight).toInt().coerceAtLeast(24)

        val pages = mutableListOf<String>()
        val paragraphs = content.replace("\r\n", "\n").replace('\r', '\n').split("\n")

        var currentPageLines = 0
        var currentPageMaxLines = maxLinesOpener
        val currentPageParagraphs = mutableListOf<String>()

        for (para in paragraphs) {
            if (para.isEmpty()) {
                // Scene break / blank line
                if (currentPageLines + 1 >= currentPageMaxLines && currentPageParagraphs.isNotEmpty()) {
                    pages.add(currentPageParagraphs.joinToString("\n"))
                    currentPageParagraphs.clear()
                    currentPageLines = 0
                    currentPageMaxLines = maxLinesContinuation
                } else {
                    currentPageParagraphs.add("")
                    currentPageLines += 1
                }
                continue
            }

            var remainingPara = para
            while (remainingPara.isNotEmpty()) {
                val neededLines = maxOf(1, (remainingPara.length + charsPerLine - 1) / charsPerLine)
                val availableLinesOnPage = currentPageMaxLines - currentPageLines

                if (neededLines <= availableLinesOnPage || availableLinesOnPage <= 0) {
                    if (availableLinesOnPage <= 0 && currentPageParagraphs.isNotEmpty()) {
                        pages.add(currentPageParagraphs.joinToString("\n"))
                        currentPageParagraphs.clear()
                        currentPageLines = 0
                        currentPageMaxLines = maxLinesContinuation
                        continue
                    }
                    currentPageParagraphs.add(remainingPara)
                    currentPageLines += neededLines
                    remainingPara = ""
                } else {
                    // Paragraph splits across pages: take the portion that fits cleanly on current page
                    val charsToTake = (availableLinesOnPage * charsPerLine).coerceAtMost(remainingPara.length)
                    var splitIdx = remainingPara.lastIndexOf(' ', charsToTake)
                    if (splitIdx < charsToTake / 2 || splitIdx <= 0) {
                        splitIdx = charsToTake
                    }

                    val fitsOnCurrent = remainingPara.substring(0, splitIdx).trimEnd()
                    val flowsToNext = remainingPara.substring(splitIdx).trimStart()

                    if (fitsOnCurrent.isNotEmpty()) {
                        currentPageParagraphs.add(fitsOnCurrent)
                    }

                    pages.add(currentPageParagraphs.joinToString("\n"))
                    currentPageParagraphs.clear()
                    currentPageLines = 0
                    currentPageMaxLines = maxLinesContinuation
                    remainingPara = flowsToNext
                }
            }
        }

        if (currentPageParagraphs.isNotEmpty()) {
            pages.add(currentPageParagraphs.joinToString("\n"))
        }

        return if (pages.isEmpty()) listOf("") else pages
    }
}
