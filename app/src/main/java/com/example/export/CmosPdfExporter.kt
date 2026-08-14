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

    // Standard 6" x 9" Trade Paperback at 72 dpi points (432 x 648 pt)
    const val PAGE_WIDTH_PT = 432
    const val PAGE_HEIGHT_PT = 648

    // Margins (Chicago Manual of Style recommends generous inner gutter for physical binding)
    private const val GUTTER_MARGIN = 54f // 0.75 in inner margin for spine binding
    private const val OUTER_MARGIN = 42f  // ~0.58 in outer margin
    private const val TOP_MARGIN = 48f    // ~0.66 in top margin
    private const val BOTTOM_MARGIN = 48f // ~0.66 in bottom margin

    data class ExportResult(
        val file: File,
        val pageCount: Int,
        val leafCount: Int,
        val formattedTitle: String
    )

    fun exportToPdf(
        context: Context,
        manuscript: ManuscriptEntity,
        sections: List<SectionEntity>
    ): ExportResult {
        val calculatedLeaves = CmosLeafEngine.calculateLeaves(manuscript, sections)

        val pdfDocument = PdfDocument()

        // Text paints
        val textPaint = TextPaint().apply {
            color = Color.rgb(20, 21, 27) // Deep book ink
            textSize = 10.5f
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
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            letterSpacing = 0.05f
        }

        val folioPaint = TextPaint().apply {
            color = Color.rgb(40, 40, 45)
            textSize = 9f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rulePaint = Paint().apply {
            color = Color.rgb(190, 185, 175)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        for ((index, leaf) in calculatedLeaves.withIndex()) {
            val pageNumber = index + 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background paper tone (subtle antique white/cream)
            canvas.drawColor(Color.rgb(253, 252, 249))

            // Compute Recto vs Verso margins
            // Recto (Right page): inner gutter on LEFT, outer margin on RIGHT
            // Verso (Left page): inner gutter on RIGHT, outer margin on LEFT
            val isRecto = leaf.side == LeafSide.RECTO
            val leftMargin = if (isRecto) GUTTER_MARGIN else OUTER_MARGIN
            val rightMargin = if (isRecto) OUTER_MARGIN else GUTTER_MARGIN
            val printableWidth = (PAGE_WIDTH_PT - leftMargin - rightMargin).toInt()

            // Draw Running Heads and Folios (CMOS Rules)
            drawRunningHeadersAndFolios(
                canvas = canvas,
                leaf = leaf,
                isRecto = isRecto,
                leftMargin = leftMargin,
                rightMargin = rightMargin,
                headerPaint = headerPaint,
                folioPaint = folioPaint,
                rulePaint = rulePaint
            )

            // Draw Body Content according to display type
            val contentTop = if (leaf.isOpener) TOP_MARGIN + 54f else TOP_MARGIN + 20f
            val maxContentHeight = (PAGE_HEIGHT_PT - BOTTOM_MARGIN - contentTop).toInt()

            when (leaf.displayType) {
                LeafDisplayType.HALF_TITLE -> {
                    drawHalfTitle(canvas, manuscript.title, leftMargin, printableWidth, boldPaint)
                }
                LeafDisplayType.TITLE_PAGE -> {
                    drawTitlePage(canvas, manuscript, leftMargin, printableWidth, boldPaint, italicPaint, textPaint)
                }
                LeafDisplayType.COPYRIGHT -> {
                    drawCopyrightPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, textPaint, italicPaint)
                }
                LeafDisplayType.DEDICATION -> {
                    drawDedicationPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, italicPaint)
                }
                LeafDisplayType.EPIGRAPH -> {
                    drawEpigraphPage(canvas, leaf.contentSnippet, leftMargin, printableWidth, italicPaint, textPaint)
                }
                LeafDisplayType.BLANK_INTENTIONAL -> {
                    // Intentionally left blank leaf (blind folio)
                    val notePaint = TextPaint(italicPaint).apply {
                        color = Color.rgb(180, 175, 165)
                        textSize = 8f
                    }
                    val text = "— This leaf intentionally left blank per Chicago Manual of Style —"
                    val textWidth = notePaint.measureText(text)
                    canvas.drawText(text, (PAGE_WIDTH_PT - textWidth) / 2f, PAGE_HEIGHT_PT / 2f, notePaint)
                }
                LeafDisplayType.PART_OPENER -> {
                    drawPartOpener(canvas, leaf, leftMargin, printableWidth, boldPaint, textPaint)
                }
                LeafDisplayType.CHAPTER_OPENER -> {
                    drawChapterOpener(
                        canvas = canvas,
                        leaf = leaf,
                        leftMargin = leftMargin,
                        width = printableWidth,
                        contentTop = contentTop,
                        maxHeight = maxContentHeight,
                        boldPaint = boldPaint,
                        italicPaint = italicPaint,
                        textPaint = textPaint
                    )
                }
                LeafDisplayType.CONTENT, LeafDisplayType.TABLE_OF_CONTENTS, LeafDisplayType.COLOPHON -> {
                    drawStandardContent(
                        canvas = canvas,
                        leaf = leaf,
                        leftMargin = leftMargin,
                        width = printableWidth,
                        contentTop = contentTop,
                        maxHeight = maxContentHeight,
                        textPaint = textPaint,
                        italicPaint = italicPaint
                    )
                }
            }

            pdfDocument.finishPage(page)
        }

        // Save PDF file to storage cache
        val sanitizedTitle = manuscript.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val exportDir = File(context.cacheDir, "manuscripts")
        if (!exportDir.exists()) exportDir.mkdirs()
        val pdfFile = File(exportDir, "${sanitizedTitle}_CMOS_Manuscript.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return ExportResult(
            file = pdfFile,
            pageCount = calculatedLeaves.size,
            leafCount = (calculatedLeaves.size + 1) / 2,
            formattedTitle = manuscript.title
        )
    }

    private fun drawRunningHeadersAndFolios(
        canvas: Canvas,
        leaf: CalculatedLeaf,
        isRecto: Boolean,
        leftMargin: Float,
        rightMargin: Float,
        headerPaint: TextPaint,
        folioPaint: TextPaint,
        rulePaint: Paint
    ) {
        val yHeader = TOP_MARGIN - 14f

        if (!leaf.hasBlindFolio) {
            if (isRecto) {
                // Recto: Right page number, Chapter/Section title
                val headText = leaf.runningHeadRecto.ifBlank { leaf.sectionTitle }
                val headWidth = headerPaint.measureText(headText)
                val folioText = leaf.pageNumberDisplay
                val folioWidth = folioPaint.measureText(folioText)

                val headX = (PAGE_WIDTH_PT - rightMargin - headWidth - folioWidth - 16f).coerceAtLeast(leftMargin)
                val folioX = PAGE_WIDTH_PT - rightMargin - folioWidth

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
            canvas.drawLine(leftMargin, TOP_MARGIN - 6f, PAGE_WIDTH_PT - rightMargin, TOP_MARGIN - 6f, rulePaint)
        } else if (leaf.isOpener && leaf.matterType == MatterType.TEXT_BODY && leaf.pageNumberDisplay.isNotBlank()) {
            // Drop folio: Centered at bottom for chapter opening leaves
            val folioText = leaf.pageNumberDisplay
            val folioWidth = folioPaint.measureText(folioText)
            val centerX = (PAGE_WIDTH_PT - folioWidth) / 2f
            canvas.drawText(folioText, centerX, PAGE_HEIGHT_PT - BOTTOM_MARGIN + 18f, folioPaint)
        }
    }

    private fun drawHalfTitle(
        canvas: Canvas,
        title: String,
        leftMargin: Float,
        width: Int,
        boldPaint: TextPaint
    ) {
        val paint = TextPaint(boldPaint).apply {
            textSize = 14f
            letterSpacing = 0.12f
        }
        val uppercaseTitle = title.uppercase()
        val textWidth = paint.measureText(uppercaseTitle)
        val x = (PAGE_WIDTH_PT - textWidth) / 2f
        canvas.drawText(uppercaseTitle, x, PAGE_HEIGHT_PT * 0.38f, paint)
    }

    private fun drawTitlePage(
        canvas: Canvas,
        manuscript: ManuscriptEntity,
        leftMargin: Float,
        width: Int,
        boldPaint: TextPaint,
        italicPaint: TextPaint,
        textPaint: TextPaint
    ) {
        var currentY = PAGE_HEIGHT_PT * 0.28f

        // Main Title
        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 19f
            letterSpacing = 0.04f
        }
        val titleLayout = StaticLayout.Builder.obtain(manuscript.title, 0, manuscript.title.length, titlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()

        currentY += titleLayout.height + 12f

        // Subtitle
        if (manuscript.subtitle.isNotBlank()) {
            val subPaint = TextPaint(italicPaint).apply {
                textSize = 12f
            }
            val subLayout = StaticLayout.Builder.obtain(manuscript.subtitle, 0, manuscript.subtitle.length, subPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
            canvas.save()
            canvas.translate(leftMargin, currentY)
            subLayout.draw(canvas)
            canvas.restore()
            currentY += subLayout.height + 16f
        }

        // Decorative Rule
        val ornamentPaint = Paint().apply {
            color = Color.rgb(180, 150, 90)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val centerX = PAGE_WIDTH_PT / 2f
        canvas.drawLine(centerX - 35f, currentY + 10f, centerX + 35f, currentY + 10f, ornamentPaint)

        // Author
        currentY = PAGE_HEIGHT_PT * 0.58f
        val bylinePaint = TextPaint(italicPaint).apply { textSize = 11f }
        val byText = "by"
        val byWidth = bylinePaint.measureText(byText)
        canvas.drawText(byText, (PAGE_WIDTH_PT - byWidth) / 2f, currentY, bylinePaint)

        currentY += 20f
        val authorPaint = TextPaint(boldPaint).apply {
            textSize = 13.5f
            letterSpacing = 0.05f
        }
        val authorText = manuscript.authorName.uppercase()
        val authorWidth = authorPaint.measureText(authorText)
        canvas.drawText(authorText, (PAGE_WIDTH_PT - authorWidth) / 2f, currentY, authorPaint)

        // Publisher / Imprint at foot
        val footY = PAGE_HEIGHT_PT - BOTTOM_MARGIN - 24f
        val pubPaint = TextPaint(textPaint).apply {
            textSize = 9.5f
        }
        val pubText = "${manuscript.publisher} • ${manuscript.year}"
        val pubWidth = pubPaint.measureText(pubText)
        canvas.drawText(pubText, (PAGE_WIDTH_PT - pubWidth) / 2f, footY, pubPaint)
    }

    private fun drawCopyrightPage(
        canvas: Canvas,
        text: String,
        leftMargin: Float,
        width: Int,
        textPaint: TextPaint,
        italicPaint: TextPaint
    ) {
        val currentY = PAGE_HEIGHT_PT * 0.52f
        val cpPaint = TextPaint(textPaint).apply {
            textSize = 8.5f
            color = Color.rgb(55, 55, 60)
        }

        val layout = StaticLayout.Builder.obtain(text, 0, text.length, cpPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3f, 1f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, currentY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawDedicationPage(
        canvas: Canvas,
        dedication: String,
        leftMargin: Float,
        width: Int,
        italicPaint: TextPaint
    ) {
        val dedPaint = TextPaint(italicPaint).apply {
            textSize = 11.5f
            letterSpacing = 0.02f
        }

        val layout = StaticLayout.Builder.obtain(dedication, 0, dedication.length, dedPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, PAGE_HEIGHT_PT * 0.38f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawEpigraphPage(
        canvas: Canvas,
        text: String,
        leftMargin: Float,
        width: Int,
        italicPaint: TextPaint,
        textPaint: TextPaint
    ) {
        val epPaint = TextPaint(italicPaint).apply {
            textSize = 10.5f
            letterSpacing = 0.02f
        }

        val layout = StaticLayout.Builder.obtain(text, 0, text.length, epPaint, (width * 0.85f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .build()

        val x = leftMargin + (width * 0.15f)
        canvas.save()
        canvas.translate(x, PAGE_HEIGHT_PT * 0.35f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawPartOpener(
        canvas: Canvas,
        leaf: CalculatedLeaf,
        leftMargin: Float,
        width: Int,
        boldPaint: TextPaint,
        textPaint: TextPaint
    ) {
        val currentY = PAGE_HEIGHT_PT * 0.36f

        val partLabelPaint = TextPaint(textPaint).apply {
            textSize = 11f
            letterSpacing = 0.15f
        }
        val partLabel = "PART"
        val labelWidth = partLabelPaint.measureText(partLabel)
        canvas.drawText(partLabel, (PAGE_WIDTH_PT - labelWidth) / 2f, currentY, partLabelPaint)

        val titlePaint = TextPaint(boldPaint).apply {
            textSize = 16f
            letterSpacing = 0.05f
        }
        val layout = StaticLayout.Builder.obtain(leaf.sectionTitle, 0, leaf.sectionTitle.length, titlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(leftMargin, currentY + 16f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawChapterOpener(
        canvas: Canvas,
        leaf: CalculatedLeaf,
        leftMargin: Float,
        width: Int,
        contentTop: Float,
        maxHeight: Int,
        boldPaint: TextPaint,
        italicPaint: TextPaint,
        textPaint: TextPaint
    ) {
        var currentY = contentTop

        // Chapter Header / Title
        val chapTitlePaint = TextPaint(boldPaint).apply {
            textSize = 14.5f
            letterSpacing = 0.04f
        }

        val titleLayout = StaticLayout.Builder.obtain(leaf.sectionTitle, 0, leaf.sectionTitle.length, chapTitlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(leftMargin, currentY)
        titleLayout.draw(canvas)
        canvas.restore()

        currentY += titleLayout.height + 14f

        // Chapter ornament rule
        val rulePaint = Paint().apply {
            color = Color.rgb(180, 150, 90)
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        val centerX = PAGE_WIDTH_PT / 2f
        canvas.drawLine(centerX - 24f, currentY, centerX + 24f, currentY, rulePaint)
        currentY += 22f

        // First paragraph with CMOS First-Line Indent (or drop cap style)
        val formattedContent = formatCmosParagraphs(leaf.contentSnippet)
        val bodyPaint = TextPaint(textPaint).apply {
            textSize = 10f
            isAntiAlias = true
        }

        val bodyLayout = StaticLayout.Builder.obtain(formattedContent, 0, formattedContent.length, bodyPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3.5f, 1f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, currentY)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawStandardContent(
        canvas: Canvas,
        leaf: CalculatedLeaf,
        leftMargin: Float,
        width: Int,
        contentTop: Float,
        maxHeight: Int,
        textPaint: TextPaint,
        italicPaint: TextPaint
    ) {
        val formattedContent = formatCmosParagraphs(leaf.contentSnippet)
        val bodyPaint = TextPaint(textPaint).apply {
            textSize = 10f
            isAntiAlias = true
        }

        val bodyLayout = StaticLayout.Builder.obtain(formattedContent, 0, formattedContent.length, bodyPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3.5f, 1f)
            .build()

        canvas.save()
        canvas.translate(leftMargin, contentTop)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    /**
     * Formats paragraphs according to Chicago Manual of Style §2.12:
     * Standard paragraph indentation of 0.5 inch (3-4 em space), with no blank line between paragraphs in continuous prose.
     */
    private fun formatCmosParagraphs(text: String): String {
        val paragraphs = text.split("\n").filter { it.isNotBlank() }
        val sb = StringBuilder()
        for (p in paragraphs) {
            val trimmed = p.trim()
            if (trimmed.startsWith("“") || trimmed.startsWith("\"") || trimmed.startsWith("—")) {
                sb.append("   ").append(trimmed).append("\n\n")
            } else if (trimmed.startsWith("#")) {
                sb.append("\n").append(trimmed.replace("#", "").trim()).append("\n\n")
            } else {
                sb.append("      ").append(trimmed).append("\n\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
