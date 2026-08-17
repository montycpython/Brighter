package com.example.cmos

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

    /**
     * Converts straight quotes (" ") and apostrophes (' ') to typographical smart/curly quotes
     * honoring Chicago Manual of Style §6.112 - §6.115.
     */
    fun applySmartQuotes(text: String): String {
        var result = text
        // Replace double quotes
        // Starts of strings or after whitespace/opening punctuation -> opening double quote
        result = result.replace(Regex("""(^|[\s(\[\{—–\-])""""), "$1“")
        // Remaining double quotes -> closing double quote
        result = result.replace("\"", "”")

        // Replace single quotes / apostrophes
        // Contractions and possessives (letter + ' + letter)
        result = result.replace(Regex("""(\w)'(\w)"""), "$1’$2")
        // Starts of strings or after whitespace -> opening single quote
        result = result.replace(Regex("""(^|[\s(\[\{—–\-])'"""), "$1‘")
        // Remaining single quotes -> closing single quote / apostrophe
        result = result.replace("'", "’")

        return result
    }

    /**
     * Applies Chicago Manual of Style em-dash rules (§6.85):
     * Em-dashes should be set without spaces on either side.
     * Replaces "--" or " -- " or " - " (when used as parenthetical break) with "—".
     */
    fun applyEmDashes(text: String): String {
        return text
            .replace(" -- ", "—")
            .replace("--", "—")
            .replace(" — ", "—")
            .replace(" —", "—")
            .replace("— ", "—")
    }

    /**
     * Applies Chicago Manual of Style en-dash rules (§6.78):
     * En-dashes connect continuing or inclusive numbers, dates, times, or reference pages (e.g. 1914–1918, pp. 24–28).
     */
    fun applyEnDashes(text: String): String {
        // Replace hyphens between digits with en-dash: 1990-2000 -> 1990–2000, 10-15 -> 10–15
        return text.replace(Regex("""(\b\d+)\s*-\s*(\d+\b)"""), "$1–$2")
    }

    /**
     * Applies Chicago Headline-Style Capitalization (§8.159):
     * - Capitalize first and last words in title and subtitle.
     * - Capitalize all major words (nouns, pronouns, verbs, adjectives, adverbs).
     * - Lowercase articles: a, an, the.
     * - Lowercase coordinating conjunctions: and, but, for, or, nor.
     * - Lowercase short prepositions: of, to, in, for, on, with, as, by, at, from, off, into, onto.
     * - Capitalize prepositions when they are part of a phrasal verb or first/last word.
     */
    fun toChicagoHeadlineCase(title: String): String {
        if (title.isBlank()) return ""
        val lowercaseWords = setOf(
            "a", "an", "the",
            "and", "but", "for", "or", "nor",
            "as", "at", "by", "for", "from", "in", "into", "of", "off", "on", "onto", "out", "over", "to", "up", "with", "via"
        )

        // Split by colons or em-dashes for subtitle handling
        val segments = title.split(Regex("(?<=[:—–])|(?=[:—–])"))
        return segments.joinToString("") { segment ->
            val words = segment.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.isEmpty()) return@joinToString segment

            words.mapIndexed { index, rawWord ->
                val cleanWord = rawWord.lowercase(Locale.ROOT)
                val isFirst = index == 0
                val isLast = index == words.size - 1

                if (isFirst || isLast || cleanWord !in lowercaseWords) {
                    capitalizeWord(rawWord)
                } else {
                    cleanWord
                }
            }.joinToString(" ")
        }
    }

    private fun capitalizeWord(word: String): String {
        if (word.isEmpty()) return word
        // Handle hyphenated words e.g. "Self-Reliance" -> "Self-Reliance"
        if (word.contains("-")) {
            return word.split("-").joinToString("-") { capitalizeWord(it) }
        }
        return word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    /**
     * Checks text for missing serial/Oxford commas (§6.19) before 'and' or 'or' in lists of 3+ items.
     */
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

    /**
     * Fixes serial/Oxford commas in list patterns.
     */
    fun fixOxfordCommas(text: String): String {
        val regex = Regex("""\b([A-Za-z]+),\s+([A-Za-z]+)\s+(and|or)\s+([A-Za-z]+)\b""", RegexOption.IGNORE_CASE)
        return regex.replace(text) { match ->
            "${match.groupValues[1]}, ${match.groupValues[2]}, ${match.groupValues[3]} ${match.groupValues[4]}"
        }
    }

    /**
     * Checks if a paragraph should be formatted as a block quotation per CMOS §13.10:
     * Prose quotations that are 100 words or more (or five lines or more) should be set off as a block.
     */
    fun isCandidateForBlockQuote(paragraph: String): Boolean {
        val wordCount = paragraph.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        return wordCount >= 85 || paragraph.length >= 450
    }

    /**
     * Comprehensive CMOS polishing pipeline:
     * Applies smart quotes, em-dashes, en-dashes, and Oxford commas.
     */
    fun polishText(text: String): String {
        var polished = applySmartQuotes(text)
        polished = applyEmDashes(polished)
        polished = applyEnDashes(polished)
        polished = fixOxfordCommas(polished)
        return polished
    }

    /**
     * Converts an integer to lowercase Roman numeral for front matter pagination (e.g. 1 -> i, 4 -> iv, 9 -> ix, 14 -> xiv).
     */
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

    /**
     * Converts markdown styled text (*italic*, **bold**, ***bold italic***, _italic_)
     * into a Jetpack Compose AnnotatedString with proper styling for Reader and UI components.
     */
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
                    builder.append(full)
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

    /**
     * Converts markdown styled text (*italic*, **bold**, ***bold italic***) into Android Spanned
     * with StyleSpan(Typeface.ITALIC/BOLD) for high quality PDF StaticLayout rendering.
     */
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

    /**
     * VisualTransformation for in-editor markdown rendering that styles *italics* and **bold**
     * while preserving 1:1 character index mapping so the cursor position and typing never glitch.
     */
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

    /**
     * Toggles or applies italics (*...*) to the current selection or inserts *...* at cursor.
     */
    fun applyItalics(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val min = selection.min
        val max = selection.max

        return if (min != max) {
            val selected = text.substring(min, max)
            // Check if already italicized
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

    /**
     * Toggles or applies bold (**...**) to current selection or inserts **** at cursor.
     */
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

    /**
     * Wraps selection in Chicago smart quotes or inserts “ ” at cursor.
     */
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

    /**
     * Inserts standard CMOS em-dash (—) at cursor.
     */
    fun applyEmDashAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + "—" + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + 1))
    }

    /**
     * Inserts standard CMOS en-dash (–) at cursor.
     */
    fun applyEnDashAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + "–" + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + 1))
    }

    /**
     * Inserts Chicago 4-space paragraph indent at cursor.
     */
    fun applyIndentAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val min = current.selection.min
        val max = current.selection.max
        val indent = "    "
        val newText = text.substring(0, min) + indent + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + indent.length))
    }

    /**
     * Inserts next sequential footnote reference [^N] at cursor.
     */
    fun applyFootnoteAtCursor(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val count = Regex("""\[\^(\d+)\]""").findAll(text).count() + 1
        val tag = "[^$count]"
        val min = current.selection.min
        val max = current.selection.max
        val newText = text.substring(0, min) + tag + text.substring(max)
        return TextFieldValue(newText, selection = TextRange(min + tag.length))
    }

    /**
     * Formats block quote at cursor.
     */
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

    /**
     * Builds an AnnotatedString for Redline Tracked Changes diff:
     * - All pre-existing / original text is rendered in normal dark black.
     * - Only newly added / edited text is highlighted in red.
     */
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
                // Pre-existing original text -> Normal black
                builder.pushStyle(SpanStyle(color = normalColor, fontWeight = FontWeight.Normal))
                builder.append(token)
                builder.pop()
            } else {
                // Newly added / edited text -> Highlighted in red
                builder.pushStyle(SpanStyle(color = highlightColor, background = highlightBgColor, fontWeight = FontWeight.Bold))
                builder.append(token)
                builder.pop()
            }
        }

        return builder.toAnnotatedString()
    }

    /**
     * Splits text into words and delimiter tokens preserving exact whitespace and punctuation.
     */
    private fun tokenizeForDiff(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val matcher = java.util.regex.Pattern.compile("""\S+|\s+""").matcher(text)
        while (matcher.find()) {
            tokens.add(matcher.group())
        }
        return tokens
    }

    /**
     * Computes Longest Common Subsequence (LCS) to find indices in newTokens that match originalTokens.
     */
    private fun computeLcsMatchedIndices(origTokens: List<String>, newTokens: List<String>): Set<Int> {
        val n = origTokens.size
        val m = newTokens.size
        if (n == 0 || m == 0) return emptySet()

        // Guard against memory explosion on extraordinarily huge texts
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

    /**
     * VisualTransformation for in-editor redline diff preview:
     * Styles pre-existing text in normal black and highlights newly added text in bold red with a soft tint.
     */
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

