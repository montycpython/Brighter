package com.supernatural.bwriter.export

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
import com.supernatural.bwriter.R
import com.supernatural.bwriter.cmos.CmosFormatter
import com.supernatural.bwriter.cmos.CmosLeafEngine
import com.supernatural.bwriter.data.IllustrationUris
import com.supernatural.bwriter.model.BookTrimSize
import com.supernatural.bwriter.model.CalculatedLeaf
import com.supernatural.bwriter.model.LeafDisplayType
import com.supernatural.bwriter.model.LeafSide
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.MatterType
import com.supernatural.bwriter.model.SectionEntity
import com.supernatural.bwriter.model.SectionType
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

    fun exportToPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        targetTrimSize: BookTrimSize? = null,
        ownBook: Boolean = true
    ): ExportResult = exportManuscriptToPdf(context, manuscript, sections, targetTrimSize, ownBook)

    fun exportManuscriptToPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        targetTrimSize: BookTrimSize? = null,
        ownBook: Boolean = true
    ): ExportResult {
        val capturedStarts = mutableMapOf<Long, String>()
        renderManuscriptPdf(context, manuscript, sections, targetTrimSize, ownBook, emptyMap(), capturedStarts)
        return renderManuscriptPdf(context, manuscript, sections, targetTrimSize, ownBook, capturedStarts, null)
    }

    private fun renderManuscriptPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>,
        targetTrimSize: BookTrimSize? = null,
        ownBook: Boolean = true,
        tocStartPages: Map<Long, String> = emptyMap(),
        captureStarts: MutableMap<Long, String>? = null
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

        fun createNewPage(isRecto: Boolean): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, globalPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.rgb(253, 252, 248))
            return Pair(page, canvas)
        }

        run {
            val (page, canvas) = createNewPage(isRecto = true)
            drawHalfTitle(canvas, manuscript.title, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, boldPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        run {
            val (page, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        run {
            val (page, canvas) = createNewPage(isRecto = true)
            drawTitlePage(canvas, manuscript, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageWidthPt, pageHeightPt, boldPaint, italicPaint, textPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        run {
            val (page, canvas) = createNewPage(isRecto = false)
            val copyrightText = manuscript.effectiveCopyrightText + (if (manuscript.isbn.isNotBlank()) "\nISBN: " + manuscript.isbn else "")
            drawCopyrightPage(canvas, copyrightText, outerMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, textPaint, italicPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++
        }

        if (manuscript.dedication.isNotBlank()) {
            val (page, canvas) = createNewPage(isRecto = true)
            drawDedicationPage(canvas, manuscript.dedication, gutterMargin, (pageWidthPt - gutterMargin - outerMargin).toInt(), pageHeightPt, italicPaint)
            pdfDocument.finishPage(page)
            globalPageNumber++
            frontMatterRomanPage++

            val (blankPage, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage)
            globalPageNumber++
            frontMatterRomanPage++
        }

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

            val (blankPage2, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage2)
            globalPageNumber++
            frontMatterRomanPage++
        }

        val calculatedLeaves = CmosLeafEngine.calculateLeaves(manuscript, sections, tocStartPages)
        val tocLeaves = calculatedLeaves.filter { it.displayType == LeafDisplayType.TABLE_OF_CONTENTS }

        if (tocLeaves.isNotEmpty()) {
            if (globalPageNumber % 2 == 0) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                frontMatterRomanPage++
            }

            for ((tocPageIdx, leaf) in tocLeaves.withIndex()) {
                val isRecto = (globalPageNumber % 2 != 0)
                val (page, canvas) = createNewPage(isRecto = isRecto)

                val leftMargin = if (isRecto) gutterMargin.toInt() else outerMargin.toInt()
                val rightMargin = if (isRecto) (pageWidthPt - outerMargin).toInt() else (pageWidthPt - gutterMargin).toInt()

                drawTableOfContentsPage(
                    canvas = canvas,
                    tocContent = leaf.contentSnippet,
                    isOpener = (tocPageIdx == 0),
                    isRecto = isRecto,
                    currentPageDisplay = CmosFormatter.toRoman(frontMatterRomanPage),
                    bookTitle = manuscript.title,
                    leftMargin = leftMargin,
                    rightMargin = rightMargin,
                    topMargin = topMargin.toInt(),
                    pageWidthPt = pageWidthPt,
                    pageHeightPt = pageHeightPt,
                    boldPaint = boldPaint,
                    textPaint = textPaint,
                    headerPaint = headerPaint,
                    folioPaint = folioPaint,
                    rulePaint = rulePaint
                )
                pdfDocument.finishPage(page)
                globalPageNumber++
                frontMatterRomanPage++
            }

            if (globalPageNumber % 2 == 0) {
                val (blankAfterToc, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankAfterToc)
                globalPageNumber++
                frontMatterRomanPage++
            }
        }

        for (sec in frontSections) {
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                frontMatterRomanPage++
            }

            captureStarts?.put(sec.id, CmosFormatter.toRoman(frontMatterRomanPage))
            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                ownBook = ownBook,
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

        if (globalPageNumber % 2 == 0) {
            val (blankPage, _) = createNewPage(isRecto = false)
            pdfDocument.finishPage(blankPage)
            globalPageNumber++
            frontMatterRomanPage++
        }

        for (sec in bodySections) {
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                bodyArabicPage++
            }

            captureStarts?.put(sec.id, bodyArabicPage.toString())
            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                ownBook = ownBook,
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

        for (sec in backSections) {
            if (sec.startOnRecto && (globalPageNumber % 2 == 0)) {
                val (blankPage, _) = createNewPage(isRecto = false)
                pdfDocument.finishPage(blankPage)
                globalPageNumber++
                bodyArabicPage++
            }

            captureStarts?.put(sec.id, bodyArabicPage.toString())
            renderFullSectionPages(
                context = context,
                pdfDocument = pdfDocument,
                sec = sec,
                manuscript = manuscript,
                ownBook = ownBook,
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

    private fun renderFullSectionPages(
        context: Context,
        pdfDocument: PdfDocument,
        sec: SectionEntity,
        manuscript: ManuscriptEntity,
        ownBook: Boolean,
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

        var paraIndex = 0
        var partialParaText: String? = null

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
                if (matterType == MatterType.TEXT_BODY && currentPageDisplay.isNotBlank()) {
                    val folioWidth = folioPaint.measureText(currentPageDisplay)
                    val centerX = (pageWidthPt - folioWidth) / 2f
                    canvas.drawText(currentPageDisplay, centerX, pageHeightPt - (bottomMargin * 0.45f), folioPaint)
                }

                currentY = topMargin + 20f

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

                val dividerPaint = Paint().apply {
                    color = Color.rgb(180, 160, 120)
                    strokeWidth = 0.75f
                }
                val halfW = 35f
                val centerX = leftMargin + printableWidth / 2f
                canvas.drawLine(centerX - halfW, currentY + 2f, centerX + halfW, currentY + 2f, dividerPaint)
                currentY += 10f

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

                if (sec.headerIllustrationUri.isNotBlank()) {
                    val drawnHeight = drawIllustration(
                        context = context,
                        canvas = canvas,
                        uriString = sec.headerIllustrationUri,
                        ownBook = ownBook,
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
                        if (currentY + (textPaint.textSize * 0.8f) <= maxTextBottom) {
                            currentY += textPaint.textSize * 0.8f
                        } else {
                            pageHasRoom = false
                        }
                        continue
                    }

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
                    canvas.save()
                    canvas.translate(leftMargin, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += layout.height + 2f
                } else {
                    var linesThatFit = 0
                    for (i in 0 until layout.lineCount) {
                        if (layout.getLineBottom(i) <= availableHeight) {
                            linesThatFit = i + 1
                        } else {
                            break
                        }
                    }

                    if (linesThatFit > 0) {
                        val endCharOffset = layout.getLineEnd(linesThatFit - 1)
                        val textForCurrentPage = spanned.subSequence(0, endCharOffset)
                        val rawEndOffset = CmosFormatter.rawOffsetForStrippedOffset(currentTextToDraw, endCharOffset)
                        val textForNextPage = currentTextToDraw.substring(rawEndOffset).trimStart()

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
                        partialParaText = currentTextToDraw
                    }

                    pageHasRoom = false
                }
            }

            if (paraIndex >= paragraphs.size && partialParaText == null && sec.tailIllustrationUri.isNotBlank()) {
                val tailTop = currentY + 12f
                val tailMaxHeight = (maxTextBottom - tailTop).coerceAtLeast(0f)
                if (tailMaxHeight >= 30f) {
                    drawIllustration(
                        context = context,
                        canvas = canvas,
                        uriString = sec.tailIllustrationUri,
                        ownBook = ownBook,
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
        isOpener: Boolean,
        isRecto: Boolean,
        currentPageDisplay: String,
        bookTitle: String,
        leftMargin: Int,
        rightMargin: Int,
        topMargin: Int,
        pageWidthPt: Int,
        pageHeightPt: Int,
        boldPaint: TextPaint,
        textPaint: TextPaint,
        headerPaint: TextPaint,
        folioPaint: TextPaint,
        rulePaint: Paint
    ) {
        var currentY: Float

        if (isOpener) {
            currentY = pageHeightPt * 0.14f
            val titleText = "CONTENTS"
            val titlePaint = TextPaint(boldPaint).apply {
                textSize = 14f
                letterSpacing = 0.12f
            }
            val titleWidth = titlePaint.measureText(titleText)
            val centerX = leftMargin + (rightMargin - leftMargin) / 2f
            canvas.drawText(titleText, centerX - titleWidth / 2f, currentY, titlePaint)

            currentY += 12f
            val goldRulePaint = Paint().apply {
                color = Color.rgb(180, 150, 80)
                strokeWidth = 1f
            }
            canvas.drawLine(centerX - 20f, currentY, centerX + 20f, currentY, goldRulePaint)
            currentY += 28f
        } else {
            val yHeader = topMargin.toFloat() - 12f

            if (isRecto) {
                val headText = "TABLE OF CONTENTS"
                val headWidth = headerPaint.measureText(headText)
                val folioWidth = folioPaint.measureText(currentPageDisplay)

                val headX = (rightMargin.toFloat() - headWidth - folioWidth - 16f).coerceAtLeast(leftMargin.toFloat())
                val folioX = rightMargin.toFloat() - folioWidth

                canvas.drawText(headText, headX, yHeader, headerPaint)
                canvas.drawText(currentPageDisplay, folioX, yHeader, folioPaint)
            } else {
                val headText = bookTitle.ifBlank { "TABLE OF CONTENTS" }
                val folioWidth = folioPaint.measureText(currentPageDisplay)

                val folioX = leftMargin.toFloat()
                val headX = leftMargin.toFloat() + folioWidth + 16f

                canvas.drawText(currentPageDisplay, folioX, yHeader, folioPaint)
                canvas.drawText(headText, headX, yHeader, headerPaint)
            }
            canvas.drawLine(leftMargin.toFloat(), topMargin.toFloat() - 4f, rightMargin.toFloat(), topMargin.toFloat() - 4f, rulePaint)

            currentY = topMargin.toFloat() + 14f
        }

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

                    canvas.drawText(entryTitle, leftMargin.toFloat(), baselineY, entryPaint)

                    val pageX = rightMargin.toFloat() - pageNumWidth
                    canvas.drawText(pageNum, pageX, baselineY, pagePaint)

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
        ownBook: Boolean,
        caption: String,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxAllowedHeight: Float,
        captionPaint: TextPaint
    ): Float {
        val printScale = 300f / 72f
        val reqWidthPx = (printableWidth * 0.85f * printScale).toInt().coerceAtLeast(64)
        val reqHeightPx = (maxAllowedHeight * printScale).toInt().coerceAtLeast(64)
        val bitmap = loadBitmap(context, uriString, ownBook, reqWidthPx, reqHeightPx)
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
            bitmap.recycle()
        }
        return totalDrawnHeight
    }

    private fun sampleSizeFor(outWidth: Int, outHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var sample = 1
        while (outWidth / (sample * 2) >= reqWidth && outHeight / (sample * 2) >= reqHeight) {
            sample *= 2
        }
        return sample
    }

    private fun loadBitmap(
        context: Context,
        uriString: String,
        ownBook: Boolean,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        if (!IllustrationUris.isLoadable(uriString, ownBook)) return null
        return try {
            when {
                uriString.startsWith("drawable:") -> {
                    val resName = uriString.removePrefix("drawable:")
                    val resId = when (resName) {
                        "head_engraving" -> R.drawable.img_chapter_head_engraving_1786743429212
                        "tailpiece" -> R.drawable.img_chapter_tailpiece_1786743439495
                        else -> 0
                    }
                    if (resId != 0) {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeResource(context.resources, resId, bounds)
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
                        }
                        BitmapFactory.decodeResource(context.resources, resId, opts)
                    } else null
                }
                uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                    val uri = Uri.parse(uriString)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, bounds)
                    }
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
                    }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                }
                else -> {
                    val file = File(uriString)
                    if (file.exists()) {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, bounds)
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
                        }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
