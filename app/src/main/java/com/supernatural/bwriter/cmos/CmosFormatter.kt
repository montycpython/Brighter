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

    fun toAnnotatedString(text: String, baseColor: Color = Color.Unspecified): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val regex = Regex("""(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|___([^_]+)___|__([^_]+)__|_\b([^_]+)\b_|\[\^(\d+)\])""")
        var lastIndex = 0
        val matches = regex.findAll(text)

        for (match in matches) {
            if (match.range.first > lastIndex) {
                builder.append(text.substring(lastIndex, match.range.first))
            }
            val full = match.value
            when {
                full.startsWith("***") && full.endsWith("***") -> {
                    val inner = full.substring(3, full.length - 3)
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("___") && full.endsWith("___") -> {
                    val inner = full.substring(3, full.length - 3)
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("**") && full.endsWith("**") -> {
                    val inner = full.substring(2, full.length - 2)
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("__") && full.endsWith("__") -> {
                    val inner = full.substring(2, full.length - 2)
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("*") && full.endsWith("*") -> {
                    val inner = full.substring(1, full.length - 1)
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("_") && full.endsWith("_") -> {
                    val inner = full.substring(1, full.length - 1)
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(inner)
                    builder.pop()
                }
                full.startsWith("[^") && full.endsWith("]") -> {
                    builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C6D23)))
                    builder.append(full.substring(2, full.length - 1))
                    builder.pop()
                }
                else -> {
                    builder.append(full)
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }

        return builder.toAnnotatedString()
    }

    fun toSpanned(text: String): CharSequence {
        val ssb = SpannableStringBuilder()
        val regex = Regex("""(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|___([^_]+)___|__([^_]+)__|_\b([^_]+)\b_|\[\^(\d+)\])""")
        var lastIndex = 0
        val matches = regex.findAll(text)

        for (match in matches) {
            if (match.range.first > lastIndex) {
                ssb.append(text.substring(lastIndex, match.range.first))
            }
            val full = match.value
            when {
                full.startsWith("***") && full.endsWith("***") -> {
                    val inner = full.substring(3, full.length - 3)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("___") && full.endsWith("___") -> {
                    val inner = full.substring(3, full.length - 3)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("**") && full.endsWith("**") -> {
                    val inner = full.substring(2, full.length - 2)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("__") && full.endsWith("__") -> {
                    val inner = full.substring(2, full.length - 2)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("*") && full.endsWith("*") -> {
                    val inner = full.substring(1, full.length - 1)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("_") && full.endsWith("_") -> {
                    val inner = full.substring(1, full.length - 1)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                full.startsWith("[^") && full.endsWith("]") -> {
                    val inner = full.substring(2, full.length - 1)
                    val start = ssb.length
                    ssb.append(inner)
                    ssb.setSpan(SuperscriptSpan(), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.7f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    ssb.append(full)
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            ssb.append(text.substring(lastIndex))
        }

        return ssb
    }

    fun rawOffsetForStrippedOffset(text: String, strippedOffset: Int): Int {
        if (strippedOffset <= 0) return 0
        val regex = Regex("""(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|___([^_]+)___|__([^_]+)__|_\b([^_]+)\b_|\[\^(\d+)\])""")
        val removed = BooleanArray(text.length)
        for (match in regex.findAll(text)) {
            val inner = (2..8).mapNotNull { match.groups[it] }.firstOrNull()
            for (i in match.range) {
                if (inner == null || i !in inner.range) removed[i] = true
            }
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
            val builder = AnnotatedString.Builder()
            val raw = text.text
            val regex = Regex("""(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|___([^_]+)___|__([^_]+)__|_\b([^_]+)\b_|\[\^(\d+)\])""")
            var lastIndex = 0
            val matches = regex.findAll(raw)

            for (match in matches) {
                if (match.range.first > lastIndex) {
                    builder.append(raw.substring(lastIndex, match.range.first))
                }
                val full = match.value
                when {
                    full.startsWith("***") && full.endsWith("***") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("***")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        builder.append(full.substring(3, full.length - 3))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("***")
                        builder.pop()
                    }
                    full.startsWith("**") && full.endsWith("**") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("**")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        builder.append(full.substring(2, full.length - 2))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("**")
                        builder.pop()
                    }
                    full.startsWith("*") && full.endsWith("*") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("*")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        builder.append(full.substring(1, full.length - 1))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("*")
                        builder.pop()
                    }
                    full.startsWith("___") && full.endsWith("___") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("___")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        builder.append(full.substring(3, full.length - 3))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("___")
                        builder.pop()
                    }
                    full.startsWith("__") && full.endsWith("__") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("__")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        builder.append(full.substring(2, full.length - 2))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("__")
                        builder.pop()
                    }
                    full.startsWith("_") && full.endsWith("_") -> {
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("_")
                        builder.pop()
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        builder.append(full.substring(1, full.length - 1))
                        builder.pop()
                        builder.pushStyle(SpanStyle(color = Color(0xFFB09868)))
                        builder.append("_")
                        builder.pop()
                    }
                    full.startsWith("[^") && full.endsWith("]") -> {
                        builder.pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C6D23)))
                        builder.append(full)
                        builder.pop()
                    }
                    else -> {
                        builder.append(full)
                    }
                }
                lastIndex = match.range.last + 1
            }

            if (lastIndex < raw.length) {
                builder.append(raw.substring(lastIndex))
            }

            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
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

