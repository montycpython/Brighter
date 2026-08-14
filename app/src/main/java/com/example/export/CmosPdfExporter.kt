package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.cmos.CmosLeafEngine
import com.example.model.BookTrimSize
import com.example.model.CalculatedLeaf
import com.example.model.LeafDisplayType
import com.example.model.LeafSide
import com.example.model.ManuscriptEntity
import com.example.model.MatterType
import com.example.model.SectionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
        sections: List<SectionEntity>
    ): ExportResult {
        val trimSize = BookTrimSize.fromTargetString(manuscript.targetPageSize)
        val calculatedLeaves = CmosLeafEngine.calculateLeaves(manuscript, sections)
        val pdfDocument = PdfDocument()

        val pageWidthPt = trimSize.widthPt
        val pageHeightPt = trimSize.heightPt
        val gutterMargin = trimSize.gutterMarginPt
        val outerMargin = trimSize.outerMarginPt
        val topMargin = trimSize.topMarginPt
        val bottomMargin = trimSize.bottomMarginPt
        val baseFontSize = trimSize.defaultBodyFontSizePt

        val textPaint = TextPaint().apply {
            color = Color.rgb(20, 21, 27)
            textSize = baseFontSize
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val italicPaint = TextPaint(textPaint).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val boldPaint = TextPaint(textPaint).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val headerPaint = TextPaint().apply {
            color = Color.rgb(70, 70, 80)
            textSize = (baseFontSize - 2f).coerceAtLeast(7.5f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            letterSpacing = 0.05f
        }

        val folioPaint = TextPaint().apply {
            color = Color.rgb(40, 40, 50)
            textSize = (baseFontSize - 1.5f).coerceAtLeast(8.5f)
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rulePaint = Paint().apply {
            color = Color.rgb(200, 195, 185)
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        for (leaf in calculatedLeaves) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, leaf.leafIndex).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Light cream paper tone
            canvas.drawColor(Color.rgb(253, 252, 248))

            val isRecto = leaf.side == LeafSide.RECTO
            val leftMargin = if (isRecto) gutterMargin else outerMargin
            val rightMargin = if (isRecto) outerMargin else gutterMargin
            val printableWidth = (pageWidthPt - leftMargin - rightMargin).toInt()

            drawRunningHeadersAndFolios(
                canvas = canvas,
                leaf = leaf,
                isRecto = isRecto,
                leftMargin = leftMargin,
                rightMargin = rightMargin,
                pageWidthPt = pageWidthPt,
                pageHeightPt = pageHeightPt,
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                headerPaint = headerPaint,
                folioPaint = folioPaint,
                rulePaint = rulePaint
            )

            val contentTop = if (leaf.isOpener) topMargin + 36f else topMargin + 14f
            val maxContentHeight = (pageHeightPt - bottomMargin - contentTop - (if (leaf.hasBlindFolio && leaf.isOpener) 36f else 8f)).toInt()

            when (leaf.displayType) {
                LeafDisplayType.HALF_TITLE -> {
                    drawHalfTitle(canvas, manuscript.title, leftMargin, printableWidth, pageHeightPt, boldPaint)
                }
                LeafDisplayType.TITLE_PAGE -> {
                    drawTitlePage(canvas, manuscript, leftMargin, printableWidth, pageWidthPt, pageHeightPt, boldPaint, italicPaint, textPaint)
                }
                LeafDisplayType.COPYRIGHT -> {
                    drawCopyrightPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, pageHeightPt, textPaint, italicPaint)
                }
                LeafDisplayType.DEDICATION -> {
                    drawDedicationPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, pageHeightPt, italicPaint)
                }
                LeafDisplayType.EPIGRAPH -> {
                    drawEpigraphPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, pageHeightPt, italicPaint, textPaint)
                }
                LeafDisplayType.BLANK_INTENTIONAL -> {
                    // Blank leaf per CMOS
                }
                LeafDisplayType.CHAPTER_OPENER, LeafDisplayType.PART_OPENER -> {
                    drawChapterOpener(
                        context = context,
                        canvas = canvas,
                        leaf = leaf,
                        leftMargin = leftMargin,
                        top = contentTop,
                        printableWidth = printableWidth,
                        maxHeight = maxContentHeight,
                        boldPaint = boldPaint,
                        italicPaint = italicPaint,
                        textPaint = textPaint,
                        lineHeightMult = trimSize.defaultLineHeightMultiplier
                    )
                }
                LeafDisplayType.CONTENT, LeafDisplayType.TABLE_OF_CONTENTS, LeafDisplayType.COLOPHON -> {
                    drawProseContent(
                        context = context,
                        canvas = canvas,
                        leaf = leaf,
                        leftMargin = leftMargin,
                        top = contentTop,
                        printableWidth = printableWidth,
                        maxHeight = maxContentHeight,
                        textPaint = textPaint,
                        italicPaint = italicPaint,
                        lineHeightMult = trimSize.defaultLineHeightMultiplier
                    )
                }
            }

            pdfDocument.finishPage(page)
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeTitle = manuscript.title.replace(Regex("""[^a-zA-Z0-9_-]"""), "_").take(30)
        val file = File(exportDir, "${safeTitle}_CMOS_${trimSize.id}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return ExportResult(
            file = file,
            pageCount = calculatedLeaves.size,
            leafCount = (calculatedLeaves.size + 1) / 2,
            formattedTitle = manuscript.title,
            trimSize = trimSize
        )
    }

    private fun drawRunningHeadersAndFolios(
        canvas: Canvas,
        leaf: CalculatedLeaf,
        isRecto: Boolean,
        leftMargin: Float,
        rightMargin: Float,
        pageWidthPt: Int,
        pageHeightPt: Int,
        topMargin: Float,
        bottomMargin: Float,
        headerPaint: TextPaint,
        folioPaint: TextPaint,
        rulePaint: Paint
    ) {
        val yHeader = topMargin - 12f

        if (!leaf.hasBlindFolio) {
            if (isRecto) {
                val headText = leaf.runningHeadRecto.ifBlank { leaf.sectionTitle }
                val headWidth = headerPaint.measureText(headText)
                val folioText = leaf.pageNumberDisplay
                val folioWidth = folioPaint.measureText(folioText)

                val headX = (pageWidthPt - rightMargin - headWidth - folioWidth - 16f).coerceAtLeast(leftMargin)
                val folioX = pageWidthPt - rightMargin - folioWidth

                canvas.drawText(headText, headX, yHeader, headerPaint)
                canvas.drawText(folioText, folioX, yHeader, folioPaint)
            } else {
                val headText = leaf.runningHeadVerso.ifBlank { leaf.sectionTitle }
                val folioText = leaf.pageNumberDisplay
                val folioWidth = folioPaint.measureText(folioText)

                val folioX = leftMargin
                val headX = leftMargin + folioWidth + 16f

                canvas.drawText(folioText, folioX, yHeader, folioPaint)
                canvas.drawText(headText, headX, yHeader, headerPaint)
            }
            canvas.drawLine(leftMargin, topMargin - 4f, pageWidthPt - rightMargin, topMargin - 4f, rulePaint)
        } else if (leaf.isOpener && leaf.matterType == MatterType.TEXT_BODY && leaf.pageNumberDisplay.isNotBlank()) {
            // Drop folio: Cleanly centered at bottom margin foot
            val folioText = leaf.pageNumberDisplay
            val folioWidth = folioPaint.measureText(folioText)
            val centerX = (pageWidthPt - folioWidth) / 2f
            canvas.drawText(folioText, centerX, pageHeightPt - (bottomMargin * 0.55f), folioPaint)
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
        val paint = TextPaint(italicPaint).apply { textSize = 11f }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, pageHeightPt * 0.35f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawEpigraphPage(canvas: Canvas, text: String, leftMargin: Float, width: Int, pageHeightPt: Int, italicPaint: TextPaint, textPaint: TextPaint) {
        val paint = TextPaint(italicPaint).apply { textSize = 10f }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.3f)
            .build()

        canvas.save()
        canvas.translate(leftMargin + width * 0.15f, pageHeightPt * 0.32f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawChapterOpener(
        context: Context,
        canvas: Canvas,
        leaf: CalculatedLeaf,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxHeight: Int,
        boldPaint: TextPaint,
        italicPaint: TextPaint,
        textPaint: TextPaint,
        lineHeightMult: Float
    ) {
        var currentY = top

        // Chapter Title
        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 15.5f
            letterSpacing = 0.03f
        }
        val titleLayout = StaticLayout.Builder.obtain(leaf.sectionTitle, 0, leaf.sectionTitle.length, titlePaint, printableWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 6f

        // Chapter Divider Line
        val dividerPaint = Paint().apply {
            color = Color.rgb(180, 160, 120)
            strokeWidth = 0.75f
        }
        val halfW = 35f
        val centerX = leftMargin + printableWidth / 2f
        canvas.drawLine(centerX - halfW, currentY + 3f, centerX + halfW, currentY + 3f, dividerPaint)
        currentY += 12f

        // Subtitle (if present)
        if (leaf.sectionSubtitle.isNotBlank()) {
            val subPaint = TextPaint(italicPaint).apply { textSize = 10.5f }
            val subLayout = StaticLayout.Builder.obtain(leaf.sectionSubtitle, 0, leaf.sectionSubtitle.length, subPaint, printableWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
            canvas.save()
            canvas.translate(leftMargin, currentY)
            subLayout.draw(canvas)
            canvas.restore()
            currentY += subLayout.height + 10f
        }

        // Chapter Head Illustration (if present)
        if (leaf.headerIllustrationUri.isNotBlank()) {
            val drawnHeight = drawIllustration(
                context = context,
                canvas = canvas,
                uriString = leaf.headerIllustrationUri,
                caption = leaf.headerIllustrationCaption,
                leftMargin = leftMargin,
                top = currentY,
                printableWidth = printableWidth,
                maxAllowedHeight = 120f,
                captionPaint = italicPaint
            )
            currentY += drawnHeight + 10f
        }

        // Prose Content
        if (leaf.contentSnippet.isNotBlank()) {
            val remainingHeight = (maxHeight - (currentY - top)).toInt().coerceAtLeast(40)
            drawProseParagraphs(
                canvas = canvas,
                text = leaf.contentSnippet,
                leftMargin = leftMargin,
                top = currentY,
                printableWidth = printableWidth,
                maxHeight = remainingHeight,
                textPaint = textPaint,
                lineHeightMult = lineHeightMult,
                isOpener = true
            )
        }
    }

    private fun drawProseContent(
        context: Context,
        canvas: Canvas,
        leaf: CalculatedLeaf,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxHeight: Int,
        textPaint: TextPaint,
        italicPaint: TextPaint,
        lineHeightMult: Float
    ) {
        val proseHeight = drawProseParagraphs(
            canvas = canvas,
            text = leaf.contentSnippet,
            leftMargin = leftMargin,
            top = top,
            printableWidth = printableWidth,
            maxHeight = maxHeight,
            textPaint = textPaint,
            lineHeightMult = lineHeightMult,
            isOpener = false
        )

        // Chapter Tailpiece (if present at end of chapter)
        if (leaf.isCloser && leaf.tailIllustrationUri.isNotBlank()) {
            val tailTop = top + proseHeight + 16f
            val tailMaxHeight = (maxHeight - proseHeight - 20f).coerceAtLeast(30f)
            if (tailMaxHeight >= 30f) {
                drawIllustration(
                    context = context,
                    canvas = canvas,
                    uriString = leaf.tailIllustrationUri,
                    caption = leaf.tailIllustrationCaption,
                    leftMargin = leftMargin,
                    top = tailTop,
                    printableWidth = printableWidth,
                    maxAllowedHeight = tailMaxHeight.coerceAtMost(70f),
                    captionPaint = italicPaint
                )
            }
        }
    }

    private fun drawProseParagraphs(
        canvas: Canvas,
        text: String,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxHeight: Int,
        textPaint: TextPaint,
        lineHeightMult: Float,
        isOpener: Boolean
    ): Float {
        val paragraphs = text.split("\n")
        var currentY = top
        val firstLineIndent = "    " // Standard CMOS first-line paragraph indent (~18pt)

        for ((idx, para) in paragraphs.withIndex()) {
            if (para.isBlank()) {
                currentY += textPaint.textSize * 0.8f
                continue
            }

            // In CMOS, first paragraph directly under chapter head is flush left or indented; subsequent paragraphs always indented
            val formattedPara = if (isOpener && idx == 0) para else "$firstLineIndent$para"

            val layout = StaticLayout.Builder.obtain(formattedPara, 0, formattedPara.length, textPaint, printableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3.5f, lineHeightMult)
                .build()

            if (currentY + layout.height > top + maxHeight + 10f) {
                break
            }

            canvas.save()
            canvas.translate(leftMargin, currentY)
            layout.draw(canvas)
            canvas.restore()

            currentY += layout.height
        }

        return currentY - top
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
