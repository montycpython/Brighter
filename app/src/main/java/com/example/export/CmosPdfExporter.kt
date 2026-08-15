package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.cmos.CmosFormatter
import com.example.cmos.CmosLeafEngine
import com.example.model.BookTrimSize
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import com.example.model.SectionType
import java.io.File
import java.io.FileOutputStream

object CmosPdfExporter {

    data class ExportResult(
        val file: File,
        val pageCount: Int,
        val leafCount: Int,
        val formattedTitle: String,
        val trimSize: BookTrimSize
    )

    /**
     * Exports the manuscript to a high-fidelity PDF adhering strictly to The Chicago Manual of Style.
     * Fills each page completely down to the 1-inch bottom margin, ensures every paragraph is indented,
     * preserves dialogue and all newlines, and dynamically paginates prose without losing words.
     */
    fun exportToPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        targetTrimSize: BookTrimSize? = null
    ): ExportResult = exportManuscriptToPdf(context, manuscript, sections, targetTrimSize)

    fun exportManuscriptToPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        targetTrimSize: BookTrimSize? = null
    ): ExportResult {
        val trimSize = targetTrimSize ?: BookTrimSize.fromTargetString(manuscript.targetPageSize)
        val pdfDocument = PdfDocument()

        val pageWidthPt = trimSize.widthPt
        val pageHeightPt = trimSize.heightPt

        val gutterMargin = trimSize.gutterMarginPt // 54 pt (0.75 in)
        val outerMargin = trimSize.outerMarginPt   // 44 pt (0.61 in)
        val topMargin = trimSize.topMarginPt       // 48 pt
        val bottomMargin = 54f                    // 54 pt (~0.75 in from bottom edge; drop folio at 26 pt from bottom)

        val baseFontSize = trimSize.defaultBodyFontSizePt

        // Typography Paints
        val textPaint = TextPaint().apply {
            color = Color.rgb(20, 20, 25)
            textSize = baseFontSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = TextPaint().apply {
            color = Color.rgb(15, 15, 20)
            textSize = baseFontSize + 3f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val italicPaint = TextPaint().apply {
            color = Color.rgb(40, 40, 45)
            textSize = baseFontSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = Color.rgb(90, 85, 80)
            textSize = (baseFontSize - 2f).coerceAtLeast(8f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val folioPaint = TextPaint().apply {
            color = Color.rgb(40, 40, 50)
            textSize = (baseFontSize - 1.5f).coerceAtLeast(8.5f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rulePaint = Paint().apply {
            color = Color.rgb(205, 200, 190)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        var globalPageNumber = 1
        var frontMatterRomanPage = 1
        var bodyArabicPage = 1

        val frontSections = sections.filter { it.matterType == MatterType.FRONT_MATTER }
        val bodySections = sections.filter { it.matterType == MatterType.TEXT_BODY }
        val backSections = sections.filter { it.matterType == MatterType.BACK_MATTER }

        // Helper to start and prepare a new PDF page
        fun createNewPage(isRecto: Boolean): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, globalPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            // High-grade book paper cream tint
            canvas.drawColor(Color.rgb(253, 252, 248))
            return Pair(page, canvas)
        }

        // ==========================================
        // 1. FRONT MATTER
        // ==========================================
        // Leaf 1: Half-Title (Recto, p. i)
        run {
            val (page, canvas) = createNewPage(isRecto = true)
            drawHalfTitle(canvas, manuscript.title, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, boldPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Leaf 2: Blank Verso (Verso, p. ii)
        run {
            val (page, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Leaf 3: Title Page (Recto, p. iii)
        run {
            val (page, canvas) = createNewPage(isRecto = true)
            drawTitlePage(canvas, manuscript, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageWidthPt, pageHeightPt, boldPaint, italicPaint, textPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Leaf 4: Copyright Page & Colophon (Verso, p. iv)
        run {
            val (page, canvas) = createNewPage(isRecto = false)
            val copyrightText = manuscript.effectiveCopyrightText + (if (manuscript.isbn.isNotBlank()) "\nISBN: " + manuscript.isbn else "")
            drawCopyrightPage(canvas, copyrightText, outerMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, textPaint, italicPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Optional Dedication (Recto, p. v)
        if (manuscript.dedication.isNotBlank()) {
            val (page, canvas) = createNewPage(isRecto = true)
            drawDedicationPage(canvas, manuscript.dedication, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, italicPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++

            // Blank verso after dedication (Verso, p. vi)
            val (blankPage, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Optional Epigraph (Recto)
        if (manuscript.epigraphText.isNotBlank()) {
            val isNextRecto = globalPageNumber % 2 != 0
            if (!isNextRecto) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                frontMatterRomanPage++
            }

            val (page, canvas) = createNewPage(isRecto = true)
            val epigraphFull = "${manuscript.epigraphText}\n\n— ${manuscript.epigraphAuthor}"
            drawEpigraphPage(canvas, epigraphFull, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, italicPaint, textPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++

            // Blank verso after epigraph
            val (blankPage2, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage2)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // ==========================================
        // Dynamic Table of Contents in Front Matter
        // ==========================================
        val calculatedLeaves = CmosLeafEngine.calculateLeaves(manuscript, sections)
        val tocLeaf = calculatedLeaves.find { it.displayType == LeafDisplayType.TABLE_OF_CONTENTS }

        if (tocLeaf != null && tocLeaf.contentSnippet.isNotBlank()) {
            // Ensure TOC starts on Recto (odd global page)
            if (globalPageNumber % 2 == 0) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                frontMatterRomanPage++
            }

            val (page, canvas) = createNewPage(isRecto = true)
            drawTableOfContentsPage(
                canvas = canvas,
                tocContent = tocLeaf.contentSnippet,
                leftMargin = gutterMargin.toInt(),
                rightMargin = (pageWidthPt - outerMargin).toInt(),
                pageHeightPt = pageHeightPt,
                boldPaint = boldPaint,
                textPaint = textPaint
            )
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++

            // Blank verso after TOC to ensure next section starts on Recto
            val (blankAfterToc, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankAfterToc)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Custom Front Matter Sections
        for (sec in frontSections) {
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                frontMatterRomanPage++
            }

            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                matterType = MatterType.FRONT_MATTER,
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                gutterMargin = gutterMargin,
                outerMargin = outerMargin,
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                textPaint = textPaint,
                boldPaint = boldPaint,
                italicPaint = italicPaint,
                headerPaint = headerPaint,
                folioPaint = folioPaint,
                rulePaint = rulePaint,
                lineHeightMult = trimSize.defaultLineHeightMultiplier,
                getPageNumber = { CmosFormatter.toRoman(frontMatterRomanPage) },
                advancePageNumber = {
                    globalPageNumber++
                    frontMatterRomanPage++
                },
                getCurrentGlobalPage = { globalPageNumber }
            )
        }

        // ==========================================
        // 2. TEXT / MAIN BODY (Arabic 1, 2, 3...)
        // ==========================================
        // Ensure Main Text starts on a RECTO (odd) leaf
        if (globalPageNumber % 2 == 0) {
            val (blankPage, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage)
            globalPageNumber++
            frontMatterRomanPage++
        }

        // Render each chapter/body section
        for (sec in bodySections) {
            // CMOS mandate: Chapters start on Recto leaf
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                bodyArabicPage++
            }

            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                matterType = MatterType.TEXT_BODY,
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                gutterMargin = gutterMargin,
                outerMargin = outerMargin,
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                textPaint = textPaint,
                boldPaint = boldPaint,
                italicPaint = italicPaint,
                headerPaint = headerPaint,
                folioPaint = folioPaint,
                rulePaint = rulePaint,
                lineHeightMult = trimSize.defaultLineHeightMultiplier,
                getPageNumber = { bodyArabicPage.toString() },
                advancePageNumber = {
                    globalPageNumber++
                    bodyArabicPage++
                },
                getCurrentGlobalPage = { globalPageNumber }
            )
        }

        // ==========================================
        // 3. BACK MATTER
        // ==========================================
        for (sec in backSections) {
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                bodyArabicPage++
            }

            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                matterType = MatterType.BACK_MATTER,
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                gutterMargin = gutterMargin,
                outerMargin = outerMargin,
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                textPaint = textPaint,
                boldPaint = boldPaint,
                italicPaint = italicPaint,
                headerPaint = headerPaint,
                folioPaint = folioPaint,
                rulePaint = rulePaint,
                lineHeightMult = trimSize.defaultLineHeightMultiplier,
                getPageNumber = { bodyArabicPage.toString() },
                advancePageNumber = {
                    globalPageNumber++
                    bodyArabicPage++
                },
                getCurrentGlobalPage = { globalPageNumber }
            )
        }

        val totalPages = globalPageNumber - 1

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeTitle = manuscript.title.replace(Regex("""[^a-zA-Z0-9_-]"""), "_").take(30)
        val file = File(exportDir, "${safeTitle}_CMOS_${trimSize.id}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return ExportResult(
            file = file,
            pageCount = totalPages,
            leafCount = (totalPages + 1) / 2,
            formattedTitle = manuscript.title,
            trimSize = trimSize
        )
    }

    /**
     * Renders a section across as many pages as needed.
     * Measures layout heights accurately so each page fills completely down to the 1-inch bottom margin,
     * indents EVERY paragraph (including the very first line of the chapter), and never drops words.
     */
    private fun renderFullSectionPages(
        context: Context,
        pdfDocument: PdfDocument,
        sec: SectionEntity,
        manuscript: ManuscriptEntity,
        matterType: MatterType,
        pageWidthPt: Int,
        pageHeightPt: Int,
        gutterMargin: Float,
        outerMargin: Float,
        topMargin: Float,
        bottomMargin: Float,
        textPaint: TextPaint,
        boldPaint: TextPaint,
        italicPaint: TextPaint,
        headerPaint: TextPaint,
        folioPaint: TextPaint,
        rulePaint: Paint,
        lineHeightMult: Float,
        getPageNumber: () -> String,
        advancePageNumber: () -> Unit,
        getCurrentGlobalPage: () -> Int
    ) {
        val paragraphs = sec.content.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val isOpenerSection = true
        var isFirstPageOfSection = true

        // Queue of paragraphs to layout
        var paraIndex = 0
        var partialParaText: String? = null

        // Max text baseline limit (stops above the bottom margin, leaving space for drop folio)
        val maxTextBottom = pageHeightPt - bottomMargin

        while (paraIndex < paragraphs.size || partialParaText != null || isFirstPageOfSection) {
            val isRecto = (getCurrentGlobalPage() % 2 != 0)
            val leftMargin = if (isRecto) gutterMargin else outerMargin
            val rightMargin = if (isRecto) outerMargin else gutterMargin
            val printableWidth = (pageWidthPt - leftMargin - rightMargin).toInt()

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, getCurrentGlobalPage()).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.rgb(253, 252, 248))

            val currentPageDisplay = getPageNumber()

            var currentY: Float

            if (isFirstPageOfSection) {
                // ==========================================
                // CHAPTER OPENER PAGE
                // ==========================================
                // Drop folio at bottom center (cleanly separated from text)
                if (matterType == MatterType.TEXT_BODY && currentPageDisplay.isNotBlank()) {
                    val folioWidth = folioPaint.measureText(currentPageDisplay)
                    val centerX = (pageWidthPt - folioWidth) / 2f
                    canvas.drawText(currentPageDisplay, centerX, pageHeightPt - (bottomMargin * 0.45f), folioPaint)
                }

                // Opener Top Header
                currentY = topMargin + 20f

                // Chapter Title
                val titlePaint = TextPaint(boldPaint).apply {
                    textSize = 15.5f
                    letterSpacing = 0.03f
                }
                val titleLayout = StaticLayout.Builder.obtain(sec.title, 0, sec.title.length, titlePaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
                canvas.save()
                canvas.translate(leftMargin, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 6f

                // Divider Line
                val dividerPaint = Paint().apply {
                    color = Color.rgb(180, 160, 120)
                    strokeWidth = 0.75f
                }
                val halfW = 35f
                val centerX = leftMargin + printableWidth / 2f
                canvas.drawLine(centerX - halfW, currentY + 2f, centerX + halfW, currentY + 2f, dividerPaint)
                currentY += 10f

                // Subtitle (if present)
                if (sec.subtitle.isNotBlank()) {
                    val subPaint = TextPaint(italicPaint).apply { textSize = 10.5f }
                    val subLayout = StaticLayout.Builder.obtain(sec.subtitle, 0, sec.subtitle.length, subPaint, printableWidth)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build()
                    canvas.save()
                    canvas.translate(leftMargin, currentY)
                    subLayout.draw(canvas)
                    canvas.restore()
                    currentY += subLayout.height + 10f
                }

                // Chapter Head Illustration (if present)
                if (sec.headerIllustrationUri.isNotBlank()) {
                    val drawnHeight = drawIllustration(
                        context = context,
                        canvas = canvas,
                        uriString = sec.headerIllustrationUri,
                        caption = sec.headerIllustrationCaption,
                        leftMargin = leftMargin,
                        top = currentY,
                        printableWidth = printableWidth,
                        maxAllowedHeight = 110f,
                        captionPaint = italicPaint
                    )
                    currentY += drawnHeight + 10f
                }

                isFirstPageOfSection = false
            } else {
                // ==========================================
                // CONTINUATION PAGE (Page 2, 3, etc.)
                // ==========================================
                val yHeader = topMargin - 12f

                if (isRecto) {
                    val headText = sec.title
                    val headWidth = headerPaint.measureText(headText)
                    val folioWidth = folioPaint.measureText(currentPageDisplay)

                    val headX = (pageWidthPt - rightMargin - headWidth - folioWidth - 16f).coerceAtLeast(leftMargin)
                    val folioX = pageWidthPt - rightMargin - folioWidth

                    canvas.drawText(headText, headX, yHeader, headerPaint)
                    canvas.drawText(currentPageDisplay, folioX, yHeader, folioPaint)
                } else {
                    val headText = manuscript.effectiveAuthorByline.ifBlank { manuscript.title }
                    val folioWidth = folioPaint.measureText(currentPageDisplay)

                    val folioX = leftMargin
                    val headX = leftMargin + folioWidth + 16f

                    canvas.drawText(currentPageDisplay, folioX, yHeader, folioPaint)
                    canvas.drawText(headText, headX, yHeader, headerPaint)
                }
                canvas.drawLine(leftMargin, topMargin - 4f, pageWidthPt - rightMargin, topMargin - 4f, rulePaint)

                currentY = topMargin + 10f
            }

            // Fill the page with prose down to maxTextBottom
            val firstLineIndent = "    " // 4 spaces standard Chicago first-line indent
            val lineSpacingExtra = 3.2f

            var pageHasRoom = true

            while (pageHasRoom && (paraIndex < paragraphs.size || partialParaText != null)) {
                val currentTextToDraw: String
                val isContinuationOfPara: Boolean

                if (partialParaText != null) {
                    currentTextToDraw = partialParaText!!
                    partialParaText = null
                    isContinuationOfPara = true
                } else {
                    val rawPara = paragraphs[paraIndex]
                    paraIndex++

                    if (rawPara.isBlank()) {
                        // Scene break / blank line spacer
                        if (currentY + (textPaint.textSize * 0.8f) <= maxTextBottom) {
                            currentY += textPaint.textSize * 0.8f
                        } else {
                            // Move to next page
                            pageHasRoom = false
                        }
                        continue
                    }

                    // ALWAYS indent first line of paragraph per user requirement and CMOS standard
                    currentTextToDraw = "$firstLineIndent$rawPara"
                    isContinuationOfPara = false
                }

                val spanned = CmosFormatter.toSpanned(currentTextToDraw)
                val layout = StaticLayout.Builder.obtain(spanned, 0, spanned.length, textPaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(lineSpacingExtra, lineHeightMult)
                    .build()

                val availableHeight = maxTextBottom - currentY

                if (layout.height <= availableHeight) {
                    // Paragraph fits completely on current page
                    canvas.save()
                    canvas.translate(leftMargin, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += layout.height + 2f
                } else {
                    // Paragraph exceeds remaining page space: find how many lines fit
                    var linesThatFit = 0
                    for (i in 0 until layout.lineCount) {
                        if (layout.getLineBottom(i) <= availableHeight) {
                            linesThatFit = i + 1
                        } else {
                            break
                        }
                    }

                    if (linesThatFit > 0) {
                        // Draw the lines that fit
                        val endCharOffset = layout.getLineEnd(linesThatFit - 1)
                        val textForCurrentPage = spanned.subSequence(0, endCharOffset)
                        val textForNextPage = currentTextToDraw.substring(endCharOffset).trimStart()

                        val partialLayout = StaticLayout.Builder.obtain(textForCurrentPage, 0, textForCurrentPage.length, textPaint, printableWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(lineSpacingExtra, lineHeightMult)
                            .build()

                        canvas.save()
                        canvas.translate(leftMargin, currentY)
                        partialLayout.draw(canvas)
                        canvas.restore()

                        currentY += partialLayout.height

                        if (textForNextPage.isNotEmpty()) {
                            partialParaText = textForNextPage
                        }
                    } else {
                        // Not even 1 line fits; push entire text to next page
                        partialParaText = currentTextToDraw
                    }

                    pageHasRoom = false
                }
            }

            // If this is the last page and there is a Tailpiece ornament
            if (paraIndex >= paragraphs.size && partialParaText == null && sec.tailIllustrationUri.isNotBlank()) {
                val tailTop = currentY + 12f
                val tailMaxHeight = (maxTextBottom - tailTop).coerceAtLeast(0f)
                if (tailMaxHeight >= 30f) {
                    drawIllustration(
                        context = context,
                        canvas = canvas,
                        uriString = sec.tailIllustrationUri,
                        caption = sec.tailIllustrationCaption,
                        leftMargin = leftMargin,
                        top = tailTop,
                        printableWidth = printableWidth,
                        maxAllowedHeight = tailMaxHeight.coerceAtMost(60f),
                        captionPaint = italicPaint
                    )
                }
            }

            pdfDocument.finishPage(page)
            advancePageNumber()

            // If nothing left to render and section complete, break
            if (paraIndex >= paragraphs.size && partialParaText == null) {
                break
            }
        }
    }

    private fun drawHalfTitle(canvas: Canvas, title: String, leftMargin: Float, width: Int, pageHeightPt: Int, boldPaint: TextPaint) {
        val paint = TextPaint(boldPaint).apply {
            textSize = 15f
            letterSpacing = 0.1f
        }
        val layout = StaticLayout.Builder.obtain(title.uppercase(), 0, title.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(leftMargin, pageHeightPt * 0.32f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTitlePage(
        canvas: Canvas,
        manuscript: ManuscriptEntity,
        leftMargin: Float,
        width: Int,
        pageWidthPt: Int,
        pageHeightPt: Int,
        boldPaint: TextPaint,
        italicPaint: TextPaint,
        textPaint: TextPaint
    ) {
        var currentY = pageHeightPt * 0.22f

        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 20f
            letterSpacing = 0.04f
        }
        val titleLayout = StaticLayout.Builder.obtain(manuscript.title, 0, manuscript.title.length, titlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 14f

        if (manuscript.subtitle.isNotBlank()) {
            val subPaint = TextPaint(italicPaint).apply { textSize = 12f }
            val subLayout = StaticLayout.Builder.obtain(manuscript.subtitle, 0, manuscript.subtitle.length, subPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
            canvas.save()
            canvas.translate(leftMargin, currentY)
            subLayout.draw(canvas)
            canvas.restore()
            currentY += subLayout.height + 24f
        } else {
            currentY += 24f
        }

        val bylinePaint = TextPaint(italicPaint).apply { textSize = 10f }
        val byText = "by"
        val byWidth = bylinePaint.measureText(byText)
        canvas.drawText(byText, (pageWidthPt - byWidth) / 2f, currentY, bylinePaint)
        currentY += 18f

        val authorPaint = TextPaint(boldPaint).apply {
            textSize = 13f
            letterSpacing = 0.05f
        }
        val authorText = manuscript.effectiveAuthorByline.uppercase()
        val authorWidth = authorPaint.measureText(authorText)
        canvas.drawText(authorText, (pageWidthPt - authorWidth) / 2f, currentY, authorPaint)

        val pubY = pageHeightPt * 0.82f
        val pubPaint = TextPaint(textPaint).apply {
            textSize = 9f
            letterSpacing = 0.08f
        }
        val pubText = manuscript.publisher.uppercase()
        val pubWidth = pubPaint.measureText(pubText)
        canvas.drawText(pubText, (pageWidthPt - pubWidth) / 2f, pubY, pubPaint)

        val yearText = manuscript.year
        val yearWidth = pubPaint.measureText(yearText)
        canvas.drawText(yearText, (pageWidthPt - yearWidth) / 2f, pubY + 14f, pubPaint)
    }

    private fun drawCopyrightPage(canvas: Canvas, text: String, leftMargin: Float, width: Int, pageHeightPt: Int, textPaint: TextPaint, italicPaint: TextPaint) {
        val paint = TextPaint(textPaint).apply { textSize = 8.5f }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3f, 1.2f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, pageHeightPt * 0.58f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawDedicationPage(canvas: Canvas, text: String, leftMargin: Float, width: Int, pageHeightPt: Int, italicPaint: TextPaint) {
        val paint = TextPaint(italicPaint).apply { textSize = 10.5f }
        val spanned = CmosFormatter.toSpanned(text)
        val dedicationWidth = (width * 0.80f).toInt()
        val dedicationLeft = leftMargin + (width - dedicationWidth) / 2f
        val layout = StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, dedicationWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(dedicationLeft, pageHeightPt * 0.35f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawEpigraphPage(canvas: Canvas, text: String, leftMargin: Float, width: Int, pageHeightPt: Int, italicPaint: TextPaint, textPaint: TextPaint) {
        val paint = TextPaint(italicPaint).apply { textSize = 10f }
        val spanned = CmosFormatter.toSpanned(text)
        val epigraphWidth = (width * 0.70f).toInt()
        val epigraphLeft = leftMargin + (width - epigraphWidth) / 2f
        val layout = StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, epigraphWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(epigraphLeft, pageHeightPt * 0.32f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTableOfContentsPage(
        canvas: Canvas,
        tocContent: String,
        leftMargin: Int,
        rightMargin: Int,
        pageHeightPt: Int,
        boldPaint: TextPaint,
        textPaint: TextPaint
    ) {
        var currentY = pageHeightPt * 0.14f
        val titleText = "CONTENTS"
        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 14f
            letterSpacing = 0.12f
        }
        val titleWidth = titlePaint.measureText(titleText)
        val centerX = leftMargin + (rightMargin - leftMargin) / 2f
        canvas.drawText(titleText, centerX - titleWidth / 2f, currentY, titlePaint)

        currentY += 12f
        val rulePaint = Paint().apply {
            color = Color.rgb(180, 150, 80)
            strokeWidth = 1f
        }
        canvas.drawLine(centerX - 20f, currentY, centerX + 20f, currentY, rulePaint)
        currentY += 28f

        val printableWidth = (rightMargin - leftMargin).toFloat()
        val entryPaint = TextPaint(textPaint).apply {
            textSize = 9.5f
            letterSpacing = 0.01f
        }
        val leaderPaint = TextPaint(textPaint).apply {
            textSize = 9.5f
            letterSpacing = 0.05f
            color = Color.rgb(120, 120, 120)
        }
        val pagePaint = TextPaint(textPaint).apply {
            textSize = 9.5f
            letterSpacing = 0.02f
            color = Color.rgb(20, 20, 20)
        }

        val lines = tocContent.lines().filter { it.trim() != "CONTENTS" }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                currentY += 8f
                continue
            }

            val (entryTitle, pageNum) = CmosLeafEngine.extractTocTitleAndPage(trimmed)

            if (pageNum.isEmpty()) {
                val partPaint = TextPaint(boldPaint).apply {
                    textSize = 10f
                    letterSpacing = 0.04f
                }
                val layout = StaticLayout.Builder.obtain(entryTitle, 0, entryTitle.length, partPaint, printableWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()

                currentY += 4f
                canvas.save()
                canvas.translate(leftMargin.toFloat(), currentY)
                layout.draw(canvas)
                canvas.restore()
                currentY += layout.height + 6f
            } else {
                val pageNumWidth = pagePaint.measureText(pageNum)
                val maxTitleWidth = printableWidth - pageNumWidth - 24f
                val titleMeasure = entryPaint.measureText(entryTitle)

                if (titleMeasure <= maxTitleWidth) {
                    val baselineY = currentY + entryPaint.textSize

                    // Draw Title flush left
                    canvas.drawText(entryTitle, leftMargin.toFloat(), baselineY, entryPaint)

                    // Draw Page Number flush right
                    val pageX = rightMargin.toFloat() - pageNumWidth
                    canvas.drawText(pageNum, pageX, baselineY, pagePaint)

                    // Draw Dot Leaders spanning the gap from title to page number
                    val startDotX = leftMargin.toFloat() + titleMeasure + 6f
                    val endDotX = pageX - 6f
                    if (endDotX > startDotX) {
                        val singleDot = " . "
                        val singleDotWidth = leaderPaint.measureText(singleDot)
                        val numDots = ((endDotX - startDotX) / singleDotWidth).toInt()
                        if (numDots > 0) {
                            val dotString = singleDot.repeat(numDots)
                            canvas.drawText(dotString, startDotX, baselineY, leaderPaint)
                        }
                    }

                    currentY += entryPaint.textSize + 8f
                } else {
                    val spannedTitle = CmosFormatter.toSpanned(entryTitle)
                    val titleLayout = StaticLayout.Builder.obtain(spannedTitle, 0, spannedTitle.length, entryPaint, maxTitleWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.2f)
                        .build()

                    canvas.save()
                    canvas.translate(leftMargin.toFloat(), currentY)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    val lastLineIdx = titleLayout.lineCount - 1
                    val lastLineWidth = titleLayout.getLineWidth(lastLineIdx)
                    val lastLineBaseline = currentY + titleLayout.getLineBaseline(lastLineIdx)

                    val pageX = rightMargin.toFloat() - pageNumWidth
                    canvas.drawText(pageNum, pageX, lastLineBaseline, pagePaint)

                    val startDotX = leftMargin.toFloat() + lastLineWidth + 6f
                    val endDotX = pageX - 6f
                    if (endDotX > startDotX) {
                        val singleDot = " . "
                        val singleDotWidth = leaderPaint.measureText(singleDot)
                        val numDots = ((endDotX - startDotX) / singleDotWidth).toInt()
                        if (numDots > 0) {
                            val dotString = singleDot.repeat(numDots)
                            canvas.drawText(dotString, startDotX, lastLineBaseline, leaderPaint)
                        }
                    }

                    currentY += titleLayout.height + 6f
                }
            }
        }
    }

    private fun drawIllustration(
        context: Context,
        canvas: Canvas,
        uriString: String,
        caption: String,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxAllowedHeight: Float,
        captionPaint: TextPaint
    ): Float {
        val bitmap = loadBitmap(context, uriString)
        var totalDrawnHeight = 0f

        if (bitmap != null) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            var destW = printableWidth.toFloat() * 0.85f
            var destH = destW / aspect

            if (destH > maxAllowedHeight) {
                destH = maxAllowedHeight
                destW = destH * aspect
            }

            val destLeft = leftMargin + (printableWidth - destW) / 2f
            val destTop = top
            val destRect = RectF(destLeft, destTop, destLeft + destW, destTop + destH)

            canvas.drawBitmap(bitmap, null, destRect, null)
            totalDrawnHeight += destH

            // Draw Caption
            if (caption.isNotBlank()) {
                val capPaint = TextPaint(captionPaint).apply { textSize = 8.5f }
                val capLayout = StaticLayout.Builder.obtain(caption, 0, caption.length, capPaint, printableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
                canvas.save()
                canvas.translate(leftMargin, top + destH + 4f)
                capLayout.draw(canvas)
                canvas.restore()
                totalDrawnHeight += capLayout.height + 6f
            }
        }
        return totalDrawnHeight
    }

    private fun loadBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            when {
                uriString.startsWith("drawable:") -> {
                    val resName = uriString.removePrefix("drawable:")
                    val resId = when (resName) {
                        "head_engraving" -> R.drawable.img_chapter_head_engraving_1786743429212
                        "tailpiece" -> R.drawable.img_chapter_tailpiece_1786743439495
                        else -> 0
                    }
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                }
                uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                else -> {
                    val file = File(uriString)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
