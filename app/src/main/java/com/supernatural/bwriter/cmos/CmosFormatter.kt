package com.supernatural.bwriter.cmos

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.style.ImageSpan
import android.text.style.TypefaceSpan
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em
import ru.noties.jlatexmath.JLatexMathDrawable

object CmosFormatter {

    fun applySmartQuotes(text: String): String {
        var result = text
        result = result.replace(Regex("""(^|[\s(\[\{—–\-])""""), "$1“")
        result = result.replace("\"", "”")

        result = result.replace(Regex("""(\w)'(\w)"""), "$1’$2")
        result = result.replace(Regex("""(^|[\s(\[\{—–\-])'"""), "$1‘")
        result = result.replace("'", "’")

        return result
    }

    fun applyEmDashes(text: String): String {
        return text
            .replace(" -- ", "—")
            .replace("--", "—")
            .replace(" — ", "—")
            .replace(" —", "—")
            .replace("— ", "—")
    }

    fun applyEnDashes(text: String): String {
        return text.replace(Regex("""(\b\d+)\s*-\s*(\d+\b)"""), "$1–$2")
    }

    fun toChicagoHeadlineCase(title: String): String {
        if (title.isBlank()) return ""
        val lowercaseWords = setOf(
            "a", "an", "the",
            "and", "but", "for", "or", "nor",
            "as", "at", "by", "for", "from", "in", "into", "of", "off", "on", "onto", "out", "over", "to", "up", "with", "via"
        )

        val segments = title.split(Regex("(?<=[:—–])|(?=[:—–])"))
        return segments.joinToString("") { segment ->
            val leading = segment.takeWhile { it.isWhitespace() }
            val trailing = segment.takeLastWhile { it.isWhitespace() }
            val core = segment.trim()
            if (core.isEmpty()) return@joinToString segment
            val words = core.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.isEmpty()) return@joinToString segment

            val cased = words.mapIndexed { index, rawWord ->
                val cleanWord = rawWord.lowercase(Locale.ROOT)
                val isFirst = index == 0
                val isLast = index == words.size - 1

                if (isFirst || isLast || cleanWord !in lowercaseWords) {
                    capitalizeWord(rawWord)
                } else {
                    cleanWord
                }
            }.joinToString(" ")
            leading + cased + trailing
        }
    }

    private fun capitalizeWord(word: String): String {
        if (word.isEmpty()) return word
        if (word.contains("-")) {
            return word.split("-").joinToString("-") { capitalizeWord(it) }
        }
        return word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    fun checkOxfordComma(text: String): List<String> {
        val issues = mutableListOf<String>()
        val regex = Regex("""\b([A-Za-z]+),\s+([A-Za-z]+)\s+(and|or)\s+([A-Za-z]+)\b""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(text)
        for (match in matches) {
            val full = match.value
            val fixed = "${match.groupValues[1]}, ${match.groupValues[2]}, ${match.groupValues[3]} ${match.groupValues[4]}"
            issues.add("Missing serial (Oxford) comma: \"$full\" → suggested CMOS: \"$fixed\"")
        }
        return issues
    }

    fun fixOxfordCommas(text: String): String {
        val regex = Regex("""\b([A-Za-z]+),\s+([A-Za-z]+)\s+(and|or)\s+([A-Za-z]+)\b""", RegexOption.IGNORE_CASE)
        return regex.replace(text) { match ->
            "${match.groupValues[1]}, ${match.groupValues[2]}, ${match.groupValues[3]} ${match.groupValues[4]}"
        }
    }

    fun isCandidateForBlockQuote(paragraph: String): Boolean {
        val wordCount = paragraph.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        return wordCount >= 85 || paragraph.length >= 450
    }

    fun polishText(text: String): String {
        var polished = applySmartQuotes(text)
        polished = applyEmDashes(polished)
        polished = applyEnDashes(polished)
        polished = fixOxfordCommas(polished)
        return polished
    }

    fun toRoman(number: Int): String {
        if (number <= 0) return ""
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = arrayOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")
        val sb = StringBuilder()
        var n = number
        for (i in values.indices) {
            while (n >= values[i]) {
                n -= values[i]
                sb.append(symbols[i])
            }
        }
        return sb.toString()
    }


    private val codeBackground = Color(0x14000000)

    private fun mathPlaceholderId(index: Int): String = "bwriter-math-$index"

    fun toAnnotatedString(text: String, baseColor: Color = Color.Unspecified): AnnotatedString {
        return toAnnotatedString(BookMarkup.parse(text))
    }

    fun toAnnotatedString(segments: List<MarkupSegment>): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var mathIndex = 0
        for (seg in segments) {
            when (seg) {
                is MarkupSegment.Text -> builder.append(seg.content)
                is MarkupSegment.Bold -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.Italic -> {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.BoldItalic -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.InlineCode -> {
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.CodeBlock -> {
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.Footnote -> {
                    builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C6D23)))
                    builder.append(seg.content)
                    builder.pop()
                }
                is MarkupSegment.InlineMath -> {
                    builder.appendInlineContent(mathPlaceholderId(mathIndex++), seg.content)
                }
                is MarkupSegment.DisplayMath -> {
                    builder.appendInlineContent(mathPlaceholderId(mathIndex++), seg.content)
                }
            }
        }
        return builder.toAnnotatedString()
    }

    /**
     * Renders one LaTeX formula to a bitmap at the given text size.
     * Returns null when the formula does not parse, so callers fall back
     * to showing the source — a bad formula degrades, it never crashes.
     */
    fun renderMathBitmap(latex: String, textSizePx: Float, colorArgb: Int): Bitmap? {
        return try {
            val drawable = JLatexMathDrawable.builder(latex)
                .textSize(textSizePx)
                .color(colorArgb)
                .build()
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * The inline-content map that paints the math placeholders produced
     * by [toAnnotatedString] for the same segment list. Placeholder ids
     * are assigned by counting math segments in order, so the two calls
     * always agree. A formula that fails to render gets no entry, and
     * Compose then shows its LaTeX source in place.
     */
    fun mathInlineContent(
        segments: List<MarkupSegment>,
        textSizePx: Float,
        color: Color
    ): Map<String, InlineTextContent> {
        val result = LinkedHashMap<String, InlineTextContent>()
        var mathIndex = 0
        for (seg in segments) {
            if (seg is MarkupSegment.InlineMath || seg is MarkupSegment.DisplayMath) {
                val id = mathPlaceholderId(mathIndex++)
                val bitmap = renderMathBitmap(seg.content, textSizePx, color.toArgb())
                if (bitmap != null) {
                    result[id] = InlineTextContent(
                        Placeholder(
                            width = (bitmap.width / textSizePx).em,
                            height = (bitmap.height / textSizePx).em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                        )
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        return result
    }

    fun toSpanned(text: String): CharSequence {
        val ssb = SpannableStringBuilder()
        for (seg in BookMarkup.parse(text)) {
            when (seg) {
                is MarkupSegment.Text -> ssb.append(seg.content)
                is MarkupSegment.Bold -> {
                    val start = ssb.length
                    ssb.append(seg.content)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                is MarkupSegment.Italic -> {
                    val start = ssb.length
                    ssb.append(seg.content)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                is MarkupSegment.BoldItalic -> {
                    val start = ssb.length
                    ssb.append(seg.content)
                    ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                is MarkupSegment.InlineCode, is MarkupSegment.CodeBlock -> {
                    val start = ssb.length
                    ssb.append(seg.content)
                    ssb.setSpan(TypefaceSpan("monospace"), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                is MarkupSegment.Footnote -> {
                    val start = ssb.length
                    ssb.append(seg.content)
                    ssb.setSpan(SuperscriptSpan(), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.7f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                is MarkupSegment.InlineMath, is MarkupSegment.DisplayMath -> {
                    val drawable = try {
                        JLatexMathDrawable.builder(seg.content)
                            .textSize(42f)
                            .color(android.graphics.Color.BLACK)
                            .build()
                    } catch (e: Exception) {
                        null
                    }
                    if (drawable == null) {
                        ssb.append(seg.content)
                    } else {
                        drawable.setBounds(
                            0, 0,
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1)
                        )
                        val start = ssb.length
                        ssb.append("\uFFFC")
                        ssb.setSpan(ImageSpan(drawable), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }
        return ssb
    }

    fun rawOffsetForStrippedOffset(text: String, strippedOffset: Int): Int {
        if (strippedOffset <= 0) return 0
        val removed = BooleanArray(text.length)
        for (seg in BookMarkup.parse(text)) {
            for (i in seg.start until seg.contentStart) removed[i] = true
            for (i in seg.contentEnd until seg.end) removed[i] = true
        }
        var kept = 0
        var raw = 0
        while (raw < text.length && kept < strippedOffset) {
            if (!removed[raw]) kept++
            raw++
        }
        return raw
    }

    // Preserves a 1:1 character index mapping so cursor position and typing never glitch.
    fun createMarkdownVisualTransformation(): VisualTransformation {
        return VisualTransformation { text ->
            val raw = text.text
            val builder = AnnotatedString.Builder()
            for (seg in BookMarkup.parse(raw)) {
                appendEditorSegment(builder, raw, seg)
            }
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    // Appends exactly the segment's source slice — markers in gold, content
    // styled — so the transformed text is the same length as the raw text.
    private fun appendEditorSegment(
        builder: AnnotatedString.Builder,
        raw: String,
        seg: MarkupSegment
    ) {
        when (seg) {
            is MarkupSegment.Text -> builder.append(raw.substring(seg.start, seg.end))
            is MarkupSegment.Bold -> appendMarked(builder, raw, seg, SpanStyle(fontWeight = FontWeight.Bold))
            is MarkupSegment.Italic -> appendMarked(builder, raw, seg, SpanStyle(fontStyle = FontStyle.Italic))
            is MarkupSegment.BoldItalic -> appendMarked(
                builder, raw, seg,
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
            )
            is MarkupSegment.InlineCode -> appendMarked(builder, raw, seg, SpanStyle(fontFamily = FontFamily.Monospace))
            is MarkupSegment.CodeBlock -> appendMarked(builder, raw, seg, SpanStyle(fontFamily = FontFamily.Monospace))
            is MarkupSegment.InlineMath -> appendMarked(builder, raw, seg, SpanStyle(fontFamily = FontFamily.Monospace))
            is MarkupSegment.DisplayMath -> appendMarked(builder, raw, seg, SpanStyle(fontFamily = FontFamily.Monospace))
            is MarkupSegment.Footnote -> {
                builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C6D23)))
                builder.append(raw.substring(seg.start, seg.end))
                builder.pop()
            }
        }
    }

    private fun appendMarked(
        builder: AnnotatedString.Builder,
        raw: String,
        seg: MarkupSegment,
        contentStyle: SpanStyle
    ) {
        val markerStyle = SpanStyle(color = Color(0xFFB09868))
        if (seg.contentStart > seg.start) {
            builder.pushStyle(markerStyle)
            builder.append(raw.substring(seg.start, seg.contentStart))
            builder.pop()
        }
        builder.pushStyle(contentStyle)
        builder.append(raw.substring(seg.contentStart, seg.contentEnd))
        builder.pop()
        if (seg.end > seg.contentEnd) {
            builder.pushStyle(markerStyle)
            builder.append(raw.substring(seg.contentEnd, seg.end))
            builder.pop()
        }
    }

    fun applyItalics(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val min = selection.min
        val max = selection.max

        return if (min != max) {
            val selected = text.substring(min, max)
            if (selected.startsWith("*") && selected.endsWith("*") && selected.length >= 2) {
                val unwrapped = selected.substring(1, selected.length - 1)
                val newText = text.substring(0, min) + unwrapped + text.substring(max)
                TextFieldValue(newText, selection = TextRange(min, min + unwrapped.length))
            } else {
                val wrapped = "*$selected*"
                val newText = text.substring(0, min) + wrapped + text.substring(max)
                TextFieldValue(newText, selection = TextRange(min, min + wrapped.length))
            }
        } else {
            val newText = text.substring(0, min) + "**" + text.substring(min)
            TextFieldValue(newText, selection = TextRange(min + 1))
        }
    }

    fun applyBold(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val min = selection.min
        val max = selection.max

        return if (min != max) {
            val selected = text.substring(min, max)
            if (selected.startsWith("**") && selected.endsWith("**") && selected.length >= 4) {
                val unwrapped = selected.substring(2, selected.length - 2)
                val newText = text.substring(0, min) + unwrapped + text.substring(max)
                TextFieldValue(newText, selection = TextRange(min, min + unwrapped.length))
            } else {
                val wrapped = "**$selected**"
                val newText = text.substring(0, min) + wrapped + text.substring(max)
                TextFieldValue(newText, selection = TextRange(min, min + wrapped.length))
            }
        } else {
            val newText = text.substring(0, min) + "****" + text.substring(min)
            TextFieldValue(newText, selection = TextRange(min + 2))
        }
    }

    fun applySmartQuotesToSelection(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val min = selection.min
        val max = selection.max

        return if (min != max) {
            val selected = text.substring(min, max)
            val wrapped = "“$selected”"
            val newText = text.substring(0, min) + wrapped + text.substring(max)
            TextFieldValue(newText, selection = TextRange(min + 1, min + 1 + selected.length))
        } else {
            val newText = text.substring(0, min) + "“ ”" + text.substring(min)
            TextFieldValue(newText, selection = TextRange(min + 1))
        }
    }

    fun applyEmDashAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + "—" + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + 1))
    }

    fun applyEnDashAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + "–" + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + 1))
    }

    fun applyIndentAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val indent = "    "
        val newText = text.substring(0, min) + indent + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + indent.length))
    }

    fun applyFootnoteAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val count = Regex("""\[\^(\d+)\]""").findAll(text).count() + 1
        val tag = "[^$count]"
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + tag + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + tag.length))
    }

    fun applyBlockQuote(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val block = if (min != max) {
            val selected = text.substring(min, max)
            "\n\n    “$selected”\n\n"
        } else {
            "\n\n    “Insert extended block quotation here...”\n\n"
        }
        val newText = text.substring(0, min) + block + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + block.length))
    }

    fun buildRedlineDiffAnnotatedString(
        originalText: String,
        newText: String,
        normalColor: Color = Color(0xFF222222),
        highlightColor: Color = Color(0xFFB71C1C),
        highlightBgColor: Color = Color(0xFFFFCDD2).copy(alpha = 0.55f)
    ): AnnotatedString {
        if (originalText.isBlank()) {
            if (newText.isBlank()) return AnnotatedString("")
            val builder = AnnotatedString.Builder()
            builder.pushStyle(SpanStyle(color = highlightColor, background = highlightBgColor, fontWeight = FontWeight.Bold))
            builder.append(newText)
            builder.pop()
            return builder.toAnnotatedString()
        }

        if (newText.isEmpty()) {
            return AnnotatedString("")
        }

        if (originalText == newText) {
            val builder = AnnotatedString.Builder()
            builder.pushStyle(SpanStyle(color = normalColor))
            builder.append(newText)
            builder.pop()
            return builder.toAnnotatedString()
        }

        val origTokens = tokenizeForDiff(originalText)
        val newTokens = tokenizeForDiff(newText)

        val matchedInNew = computeLcsMatchedIndices(origTokens, newTokens)
        val builder = AnnotatedString.Builder()

        for (j in newTokens.indices) {
            val token = newTokens[j]
            val isWhitespace = token.isBlank()

            if (isWhitespace) {
                builder.append(token)
            } else if (j in matchedInNew) {
                builder.pushStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Normal))
                builder.append(token)
                builder.pop()
            } else {
                builder.pushStyle(SpanStyle(color = highlightColor, background = highlightBgColor, fontWeight = FontWeight.Bold))
                builder.append(token)
                builder.pop()
            }
        }

        return builder.toAnnotatedString()
    }

    private fun tokenizeForDiff(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val matcher = java.util.regex.Pattern.compile("""\S+|\s+""").matcher(text)
        while (matcher.find()) {
            tokens.add(matcher.group())
        }
        return tokens
    }

    private fun computeLcsMatchedIndices(origTokens: List<String>, newTokens: List<String>): Set<Int> {
        val n = origTokens.size
        val m = newTokens.size
        if (n == 0 || m == 0) return emptySet()

        if (n * m > 600000) {
            val origSet = origTokens.filter { it.isNotBlank() }.toSet()
            val matched = mutableSetOf<Int>()
            for (j in newTokens.indices) {
                if (origSet.contains(newTokens[j])) {
                    matched.add(j)
                }
            }
            return matched
        }

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0 until n) {
            for (j in 0 until m) {
                dp[i + 1][j + 1] = if (origTokens[i] == newTokens[j]) {
                    dp[i][j] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val matchedInNew = mutableSetOf<Int>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            if (origTokens[i - 1] == newTokens[j - 1]) {
                matchedInNew.add(j - 1)
                i--
                j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }
        return matchedInNew
    }

    fun createRedlineVisualTransformation(originalText: String): VisualTransformation {
        return VisualTransformation { text ->
            val annotated = buildRedlineDiffAnnotatedString(
                originalText = originalText,
                newText = text.text,
                normalColor = Color(0xFF1F1B18),
                highlightColor = Color(0xFFC62828),
                highlightBgColor = Color(0xFFFFCDD2).copy(alpha = 0.45f)
            )
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }
}

