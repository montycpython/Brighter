package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
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

        // Text paints calibrated to trim size
        val textPaint = TextPaint().apply {
            color = Color.rgb(20, 21, 27) // Deep book ink
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

            // Compute Recto vs Verso margins
            val isRecto = leaf.side == LeafSide.RECTO
            val leftMargin = if (isRecto) gutterMargin else outerMargin
            val rightMargin = if (isRecto) outerMargin else gutterMargin
            val printableWidth = (pageWidthPt - leftMargin - rightMargin).toInt()

            // Draw Running Heads and Folios (CMOS Rules)
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

            // Draw Body Content according to display type
            val contentTop = if (leaf.isOpener) topMargin + 50f else topMargin + 18f
            val maxContentHeight = (pageHeightPt - bottomMargin - contentTop).toInt()

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
                    // Blank leaf per Chicago rules
                }
                LeafDisplayType.CHAPTER_OPENER, LeafDisplayType.PART_OPENER -> {
                    drawChapterOpener(
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
                        canvas = canvas,
                        text = leaf.contentSnippet,
                        leftMargin = leftMargin,
                        top = contentTop,
                        printableWidth = printableWidth,
                        maxHeight = maxContentHeight,
                        textPaint = textPaint,
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
        val yHeader = topMargin - 14f

        if (!leaf.hasBlindFolio) {
            if (isRecto) {
                // Recto: Right page number, Chapter/Section title
                val headText = leaf.runningHeadRecto.ifBlank { leaf.sectionTitle }
                val headWidth = headerPaint.measureText(headText)
                val folioText = leaf.pageNumberDisplay
                val folioWidth = folioPaint.measureText(folioText)

                val headX = (pageWidthPt - rightMargin - headWidth - folioWidth - 16f).coerceAtLeast(leftMargin)
                val folioX = pageWidthPt - rightMargin - folioWidth

                canvas.drawText(headText, headX, yHeader, headerPaint)
                canvas.drawText(folioText, folioX, yHeader, folioPaint)
            } else {
                // Verso: Left page number, Book Title or Author Name
                val headText = leaf.runningHeadVerso.ifBlank { leaf.sectionTitle }
                val folioText = leaf.pageNumberDisplay
                val folioWidth = folioPaint.measureText(folioText)

                val folioX = leftMargin
                val headX = leftMargin + folioWidth + 16f

                canvas.drawText(folioText, folioX, yHeader, folioPaint)
                canvas.drawText(headText, headX, yHeader, headerPaint)
            }

            // Running head separator hairline rule
            canvas.drawLine(leftMargin, topMargin - 6f, pageWidthPt - rightMargin, topMargin - 6f, rulePaint)
        } else if (leaf.isOpener && leaf.matterType == MatterType.TEXT_BODY && leaf.pageNumberDisplay.isNotBlank()) {
            // Drop folio: Centered at bottom for chapter opening leaves
            val folioText = leaf.pageNumberDisplay
            val folioWidth = folioPaint.measureText(folioText)
            val centerX = (pageWidthPt - folioWidth) / 2f
            canvas.drawText(folioText, centerX, pageHeightPt - bottomMargin + 18f, folioPaint)
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

        // Title
        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 21f
            letterSpacing = 0.04f
        }
        val titleLayout = StaticLayout.Builder.obtain(manuscript.title, 0, manuscript.title.length, titlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 16f

        // Subtitle
        if (manuscript.subtitle.isNotBlank()) {
            val subPaint = TextPaint(italicPaint).apply {
                textSize = 12.5f
            }
            val subLayout = StaticLayout.Builder.obtain(manuscript.subtitle, 0, manuscript.subtitle.length, subPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
            canvas.save()
            canvas.translate(leftMargin, currentY)
            subLayout.draw(canvas)
            canvas.restore()
            currentY += subLayout.height + 28f
        } else {
            currentY += 28f
        }

        // Byline
        val bylinePaint = TextPaint(italicPaint).apply {
            textSize = 10.5f
        }
        val byText = "by"
        val byWidth = bylinePaint.measureText(byText)
        canvas.drawText(byText, (pageWidthPt - byWidth) / 2f, currentY, bylinePaint)
        currentY += 20f

        val authorPaint = TextPaint(boldPaint).apply {
            textSize = 13.5f
            letterSpacing = 0.05f
        }
        val authorText = manuscript.effectiveAuthorByline.uppercase()
        val authorWidth = authorPaint.measureText(authorText)
        canvas.drawText(authorText, (pageWidthPt - authorWidth) / 2f, currentY, authorPaint)

        // Publisher at bottom
        val pubY = pageHeightPt * 0.82f
        val pubPaint = TextPaint(textPaint).apply {
            textSize = 9.5f
            letterSpacing = 0.08f
        }
        val pubText = manuscript.publisher.uppercase()
        val pubWidth = pubPaint.measureText(pubText)
        canvas.drawText(pubText, (pageWidthPt - pubWidth) / 2f, pubY, pubPaint)

        val yearText = manuscript.year
        val yearWidth = pubPaint.measureText(yearText)
        canvas.drawText(yearText, (pageWidthPt - yearWidth) / 2f, pubY + 14f, pubPaint)
    }

    private fun drawCopyrightPage(
        canvas: Canvas,
        text: String,
        leftMargin: Float,
        width: Int,
        pageHeightPt: Int,
        textPaint: TextPaint,
        italicPaint: TextPaint
    ) {
        val paint = TextPaint(textPaint).apply {
            textSize = 8.5f
        }
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
        val paint = TextPaint(italicPaint).apply {
            textSize = 11.5f
        }
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
        val paint = TextPaint(italicPaint).apply {
            textSize = 10.5f
        }
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

        // Chapter Header
        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 16f
            letterSpacing = 0.03f
        }
        val titleLayout = StaticLayout.Builder.obtain(leaf.sectionTitle, 0, leaf.sectionTitle.length, titlePaint, printableWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 8f

        // Chapter Divider line
        val dividerPaint = Paint().apply {
            color = Color.rgb(180, 160, 120)
            strokeWidth = 1f
        }
        val halfW = 40f
        val centerX = leftMargin + printableWidth / 2f
        canvas.drawLine(centerX - halfW, currentY + 4f, centerX + halfW, currentY + 4f, dividerPaint)
        currentY += 24f

        // Prose Content
        if (leaf.contentSnippet.isNotBlank()) {
            drawProseContent(canvas, leaf.contentSnippet, leftMargin, currentY, printableWidth, maxHeight, textPaint, lineHeightMult)
        }
    }

    private fun drawProseContent(
        canvas: Canvas,
        text: String,
        leftMargin: Float,
        top: Float,
        printableWidth: Int,
        maxHeight: Int,
        textPaint: TextPaint,
        lineHeightMult: Float
    ) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, printableWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, lineHeightMult)
            .build()

        canvas.save()
        canvas.translate(leftMargin, top)
        layout.draw(canvas)
        canvas.restore()
    }
}
