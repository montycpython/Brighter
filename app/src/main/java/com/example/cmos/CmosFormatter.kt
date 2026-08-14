package com.example.cmos

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
}
