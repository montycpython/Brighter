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

        // ==========================================
        // DYNAMIC TABLE OF CONTENTS (Recto Leaf)
        // ==========================================
        val tocLeafIndex = if (sections.isNotEmpty()) {
            // Ensure TOC starts on Recto
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
                frontMatterPage++
            }

            val tIndex = leaves.size
            leaves.add(
                CalculatedLeaf(
                    leafIndex = leaves.size + 1,
                    pageNumberDisplay = CmosFormatter.toRoman(frontMatterPage),
                    pageNumberRaw = frontMatterPage,
                    side = LeafSide.RECTO,
                    matterType = MatterType.FRONT_MATTER,
                    sectionId = null,
                    sectionTitle = "Contents",
                    sectionType = SectionType.TABLE_OF_CONTENTS,
                    displayType = LeafDisplayType.TABLE_OF_CONTENTS,
                    contentSnippet = "Contents\n\nGenerating dynamically...",
                    isOpener = true,
                    hasBlindFolio = true
                )
            )
            frontMatterPage++

            // Blank Verso after TOC to ensure next section starts on Recto
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
            tIndex
        } else null

        // Custom Front Matter sections (Foreword, Preface, Acknowledgments, Introduction)
        val sectionStartPageMap = mutableMapOf<Long, String>()
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
                val roman = CmosFormatter.toRoman(frontMatterPage)
                if (isOpener) {
                    sectionStartPageMap[sec.id] = roman
                }

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = roman,
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

                val displayPage = bodyPage.toString()
                if (isOpener) {
                    sectionStartPageMap[sec.id] = displayPage
                }

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = displayPage,
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

                val displayPage = bodyPage.toString()
                if (isOpener) {
                    sectionStartPageMap[sec.id] = displayPage
                }

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = displayPage,
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

        // ==========================================
        // DYNAMIC TABLE OF CONTENTS POPULATION
        // ==========================================
        if (tocLeafIndex != null && tocLeafIndex < leaves.size) {
            val tocLines = mutableListOf<String>()
            tocLines.add("CONTENTS\n")

            if (frontSections.isNotEmpty()) {
                for (sec in frontSections) {
                    val page = sectionStartPageMap[sec.id] ?: "v"
                    tocLines.add(formatTocLine(sec.title, page))
                }
                tocLines.add("")
            }

            for (sec in bodySections) {
                val page = sectionStartPageMap[sec.id] ?: "1"
                val title = if (sec.sectionType == SectionType.PART_DIVIDER) sec.title.uppercase() else sec.title
                tocLines.add(formatTocLine(title, page))
            }

            if (backSections.isNotEmpty()) {
                tocLines.add("")
                for (sec in backSections) {
                    val page = sectionStartPageMap[sec.id] ?: ""
                    tocLines.add(formatTocLine(sec.title, page))
                }
            }

            leaves[tocLeafIndex] = leaves[tocLeafIndex].copy(
                contentSnippet = tocLines.joinToString("\n").trimEnd()
            )
        }

        return leaves
    }

    /**
     * Formats a single Table of Contents entry line with a clean tab delimiter.
     */
    fun formatTocLine(title: String, page: String): String {
        val cleanTitle = title.trim()
        val cleanPage = page.trim()
        return "$cleanTitle\t$cleanPage"
    }

    /**
     * Robustly extracts title and page number from a TOC line.
     */
    fun extractTocTitleAndPage(line: String): Pair<String, String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.equals("CONTENTS", ignoreCase = true)) {
            return Pair(trimmed, "")
        }
        if (trimmed.contains("\t")) {
            val parts = trimmed.split("\t")
            return Pair(parts[0].trim(), parts.getOrElse(1) { "" }.trim())
        }
        // Match legacy or custom lines formatted with dot leaders or trailing numbers
        val regex = Regex("""^(.*?)(?:\s*[\.\s]{2,}\s*)([0-9ivxlcIVXLC]+)$""")
        val match = regex.find(trimmed)
        if (match != null) {
            return Pair(match.groupValues[1].trim(), match.groupValues[2].trim())
        }
        return Pair(trimmed, "")
    }

    /**
     * Generates structured TocItem objects for rich UI and interactive navigation.
     */
    fun generateTocItems(
        sections: List<SectionEntity>,
        leaves: List<CalculatedLeaf>
    ): List<com.example.model.TocItem> {
        val pageMap = mutableMapOf<Long, String>()
        for (leaf in leaves) {
            if (leaf.isOpener && leaf.sectionId != null && !pageMap.containsKey(leaf.sectionId)) {
                pageMap[leaf.sectionId] = leaf.pageNumberDisplay
            }
        }

        return sections.map { sec ->
            val page = pageMap[sec.id] ?: if (sec.matterType == MatterType.FRONT_MATTER) "i" else "1"
            com.example.model.TocItem(
                title = sec.title,
                subtitle = sec.subtitle,
                matterType = sec.matterType,
                sectionType = sec.sectionType,
                sectionId = sec.id,
                pageNumberDisplay = page,
                isPartDivider = sec.sectionType == SectionType.PART_DIVIDER
            )
        }
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
