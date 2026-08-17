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
        // DYNAMIC TABLE OF CONTENTS (Multi-Leaf Pagination)
        // ==========================================
        if (sections.isNotEmpty()) {
            // Ensure TOC starts on Recto (odd leaf)
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

            // Estimate Body & Back section start pages (Arabic numbering starts at 1)
            val sectionStartPageMap = mutableMapOf<Long, String>()
            var tempBodyPage = 1

            for (sec in bodySections) {
                if (sec.startOnRecto && tempBodyPage % 2 == 0) {
                    tempBodyPage++
                }
                sectionStartPageMap[sec.id] = tempBodyPage.toString()
                val pageSnippets = paginateSectionProse(
                    content = sec.content,
                    trimSize = trimSize,
                    hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                    hasSubtitle = sec.subtitle.isNotBlank()
                )
                tempBodyPage += maxOf(1, pageSnippets.size)
            }

            for (sec in backSections) {
                if (sec.startOnRecto && tempBodyPage % 2 == 0) {
                    tempBodyPage++
                }
                sectionStartPageMap[sec.id] = tempBodyPage.toString()
                val pageSnippets = paginateSectionProse(
                    content = sec.content,
                    trimSize = trimSize,
                    hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                    hasSubtitle = sec.subtitle.isNotBlank()
                )
                tempBodyPage += maxOf(1, pageSnippets.size)
            }

            // Build raw TOC entry lines
            fun buildRawTocLines(frontStartPage: Int): List<String> {
                val lines = mutableListOf<String>()
                var curFront = frontStartPage

                if (frontSections.isNotEmpty()) {
                    for (sec in frontSections) {
                        if (sec.startOnRecto && curFront % 2 == 0) {
                            curFront++
                        }
                        lines.add(formatTocLine(sec.title, CmosFormatter.toRoman(curFront)))
                        val pageSnippets = paginateSectionProse(
                            content = sec.content,
                            trimSize = trimSize,
                            hasHeaderIllustration = sec.headerIllustrationUri.isNotBlank(),
                            hasSubtitle = sec.subtitle.isNotBlank()
                        )
                        curFront += maxOf(1, pageSnippets.size)
                    }
                    lines.add("")
                }

                for (sec in bodySections) {
                    val page = sectionStartPageMap[sec.id] ?: "1"
                    val title = if (sec.sectionType == SectionType.PART_DIVIDER) sec.title.uppercase() else sec.title
                    lines.add(formatTocLine(title, page))
                }

                if (backSections.isNotEmpty()) {
                    lines.add("")
                    for (sec in backSections) {
                        val page = sectionStartPageMap[sec.id] ?: ""
                        lines.add(formatTocLine(sec.title, page))
                    }
                }
                return lines
            }

            // First pass to estimate TOC leaf count
            val initialRawLines = buildRawTocLines(frontMatterPage + 1)
            var paginatedTocPages = paginateTocLines(initialRawLines, trimSize)
            val tocPagesCount = paginatedTocPages.size

            // Starting roman page for custom front-matter sections after TOC:
            // If TOC has odd number of pages, an intentional blank verso will follow to keep next section on Recto
            val blankAfterToc = (tocPagesCount % 2 != 0)
            val customFrontStartPage = frontMatterPage + tocPagesCount + (if (blankAfterToc) 1 else 0)

            // Exact raw lines with precise roman numerals
            val exactRawLines = buildRawTocLines(customFrontStartPage)
            paginatedTocPages = paginateTocLines(exactRawLines, trimSize)

            // Emit all TOC leaves across multiple pages/leaves
            for ((pIdx, pageLines) in paginatedTocPages.withIndex()) {
                val isOpener = (pIdx == 0)
                val side = if ((leaves.size + 1) % 2 != 0) LeafSide.RECTO else LeafSide.VERSO
                val romanPage = CmosFormatter.toRoman(frontMatterPage)

                leaves.add(
                    CalculatedLeaf(
                        leafIndex = leaves.size + 1,
                        pageNumberDisplay = romanPage,
                        pageNumberRaw = frontMatterPage,
                        side = side,
                        matterType = MatterType.FRONT_MATTER,
                        sectionId = null,
                        sectionTitle = if (isOpener) "Contents" else "Contents (continued)",
                        sectionType = SectionType.TABLE_OF_CONTENTS,
                        displayType = LeafDisplayType.TABLE_OF_CONTENTS,
                        contentSnippet = (if (isOpener) "CONTENTS\n\n" else "") + pageLines.joinToString("\n"),
                        isOpener = isOpener,
                        isCloser = (pIdx == paginatedTocPages.size - 1),
                        hasBlindFolio = isOpener,
                        runningHeadVerso = manuscript.title,
                        runningHeadRecto = "Contents"
                    )
                )
                frontMatterPage++
            }

            // If TOC finished on Recto (odd page), insert Blank Verso so next front section starts on Recto
            if (leaves.size % 2 != 0) {
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

            val contentToPaginate = if (sec.sectionType == SectionType.ACKNOWLEDGMENTS && manuscript.acknowledgmentsJson.isNotBlank()) {
                val credits = com.example.model.ContributorCredit.parseListFromJson(manuscript.acknowledgmentsJson)
                if (credits.isNotEmpty()) {
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    val creditLines = credits.joinToString("\n") { c ->
                        "• ${c.penName} (${c.role.replace("_", " ")}) — ${c.wordsContributed} words contributed [${dateFormat.format(java.util.Date(c.commitTimestamp))}]"
                    }
                    if (sec.content.isBlank()) {
                        "The author gratefully acknowledges the editorial craftsmanship and contributions of:\n\n$creditLines"
                    } else {
                        "${sec.content}\n\nEditorial & Contributor Registry:\n$creditLines"
                    }
                } else {
                    sec.content
                }
            } else {
                sec.content
            }

            val pageSnippets = paginateSectionProse(
                content = contentToPaginate,
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

        return leaves
    }

    /**
     * Slices raw TOC entry lines into physical pages/leaves so that long tables of contents
     * (e.g. 25+ chapters, prefaces, forewords) automatically flow onto subsequent leaves without truncation.
     */
    fun paginateTocLines(
        lines: List<String>,
        trimSize: BookTrimSize
    ): List<List<String>> {
        val cleanLines = lines.filter { it.trim() != "CONTENTS" }
        if (cleanLines.isEmpty()) return listOf(emptyList())

        val entryHeight = 17.5f
        val openerHeaderHeight = trimSize.heightPt * 0.14f + 40f
        val continuationHeaderHeight = trimSize.topMarginPt + 24f
        val bottomMargin = trimSize.bottomMarginPt

        val availableHeightOpener = (trimSize.heightPt - openerHeaderHeight - bottomMargin).coerceAtLeast(200f)
        val maxLinesOpener = (availableHeightOpener / entryHeight).toInt().coerceIn(16, 26)

        val availableHeightContinuation = (trimSize.heightPt - continuationHeaderHeight - bottomMargin).coerceAtLeast(250f)
        val maxLinesContinuation = (availableHeightContinuation / entryHeight).toInt().coerceIn(20, 32)

        val pages = mutableListOf<List<String>>()
        var currentPageLines = mutableListOf<String>()
        var currentLimit = maxLinesOpener

        for (line in cleanLines) {
            if (currentPageLines.isNotEmpty() && currentPageLines.size >= currentLimit) {
                pages.add(currentPageLines)
                currentPageLines = mutableListOf()
                currentLimit = maxLinesContinuation
            }
            currentPageLines.add(line)
        }

        if (currentPageLines.isNotEmpty()) {
            pages.add(currentPageLines)
        }

        return if (pages.isEmpty()) listOf(emptyList()) else pages
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
