package com.supernatural.bwriter.cmos

/**
 * The book markup language — one parser for every platform.
 *
 * Book text stays plain text. This parser cuts it into segments; each
 * platform only paints the segments. The rules, in the order they are
 * claimed at each position:
 *
 *  1. Escape: `\$`, `\*`, \` \` and `\\` print the escaped character
 *     itself and start nothing.
 *  2. Code block: a ``` fence up to the next ``` fence. The content is
 *     opaque — no marker inside it means anything.
 *  3. Inline code: `...` — likewise opaque.
 *  4. Display math: $$...$$ — LaTeX source, opaque.
 *  5. Inline math: $...$ on one line. The opening $ may not be followed
 *     by a space, the closing $ may not be preceded by a space and may
 *     not be followed by a digit, so prices in prose stay prose; a
 *     literal dollar is written `\$`.
 *  6. Emphasis, asterisks only: ***bold italic***, **bold**, *italic*.
 *     Underscores never format — they are load-bearing in real text
 *     (identifiers, snake_case, __init__), so they print as written.
 *  7. Footnote reference: [^123].
 *
 * Constructs do not nest: the first construct claimed at a position owns
 * its whole span. A marker with no closing marker prints literally.
 * Every character of the source belongs to exactly one segment, and for
 * every segment [start, end) is its full source span including markers
 * while [contentStart, contentEnd) is the part that survives when the
 * markers are stripped.
 */
sealed interface MarkupSegment {
    val content: String
    val start: Int
    val end: Int
    val contentStart: Int
    val contentEnd: Int

    data class Text(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class Bold(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class Italic(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class BoldItalic(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class InlineCode(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class CodeBlock(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class InlineMath(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class DisplayMath(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment

    data class Footnote(
        override val content: String,
        override val start: Int,
        override val end: Int,
        override val contentStart: Int,
        override val contentEnd: Int
    ) : MarkupSegment
}

object BookMarkup {

    fun parse(source: String): List<MarkupSegment> {
        val segments = ArrayList<MarkupSegment>()
        val n = source.length
        var i = 0
        var textStart = 0

        fun flushText(until: Int) {
            if (until > textStart) {
                segments.add(
                    MarkupSegment.Text(
                        source.substring(textStart, until), textStart, until, textStart, until
                    )
                )
            }
        }

        while (i < n) {
            val c = source[i]
            when {
                // 1. Escape.
                c == '\\' && i + 1 < n &&
                    (source[i + 1] == '$' || source[i + 1] == '*' ||
                        source[i + 1] == '`' || source[i + 1] == '\\') -> {
                    flushText(i)
                    segments.add(
                        MarkupSegment.Text(
                            source[i + 1].toString(), i, i + 2, i + 1, i + 2
                        )
                    )
                    i += 2
                    textStart = i
                }
                // 2. Code block.
                c == '`' && source.startsWith("```", i) -> {
                    val close = source.indexOf("```", i + 3)
                    if (close < 0) {
                        i += 3 // Unclosed fence prints literally.
                    } else {
                        var cs = i + 3
                        if (cs < close && source[cs] == '\n') cs++
                        var ce = close
                        if (ce > cs && source[ce - 1] == '\n') ce--
                        flushText(i)
                        segments.add(
                            MarkupSegment.CodeBlock(
                                source.substring(cs, ce), i, close + 3, cs, ce
                            )
                        )
                        i = close + 3
                        textStart = i
                    }
                }
                // 3. Inline code.
                c == '`' -> {
                    val close = source.indexOf('`', i + 1)
                    if (close < 0 || close == i + 1) {
                        i++
                    } else {
                        flushText(i)
                        segments.add(
                            MarkupSegment.InlineCode(
                                source.substring(i + 1, close), i, close + 1, i + 1, close
                            )
                        )
                        i = close + 1
                        textStart = i
                    }
                }
                // 4. Display math.
                c == '$' && source.startsWith("$$", i) -> {
                    val close = source.indexOf("$$", i + 2)
                    if (close < 0 || close == i + 2) {
                        i += 2 // Unclosed or empty prints literally.
                    } else {
                        flushText(i)
                        segments.add(
                            MarkupSegment.DisplayMath(
                                source.substring(i + 2, close), i, close + 2, i + 2, close
                            )
                        )
                        i = close + 2
                        textStart = i
                    }
                }
                // 5. Inline math, with the currency guards.
                c == '$' -> {
                    val close = source.indexOf('$', i + 1)
                    val ok = close > i + 1 &&
                        !source[i + 1].isWhitespace() &&
                        !source[close - 1].isWhitespace() &&
                        !source.substring(i + 1, close).contains('\n') &&
                        (close + 1 >= n || !source[close + 1].isDigit())
                    if (!ok) {
                        i++
                    } else {
                        flushText(i)
                        segments.add(
                            MarkupSegment.InlineMath(
                                source.substring(i + 1, close), i, close + 1, i + 1, close
                            )
                        )
                        i = close + 1
                        textStart = i
                    }
                }
                // 6. Emphasis.
                c == '*' -> {
                    val seg = matchEmphasis(source, i)
                    if (seg == null) {
                        i++
                    } else {
                        flushText(i)
                        segments.add(seg)
                        i = seg.end
                        textStart = i
                    }
                }
                // 7. Footnote reference.
                c == '[' && source.startsWith("[^", i) -> {
                    var j = i + 2
                    while (j < n && source[j].isDigit()) j++
                    if (j > i + 2 && j < n && source[j] == ']') {
                        flushText(i)
                        segments.add(
                            MarkupSegment.Footnote(
                                source.substring(i + 2, j), i, j + 1, i + 2, j
                            )
                        )
                        i = j + 1
                        textStart = i
                    } else {
                        i++
                    }
                }
                else -> i++
            }
        }
        flushText(n)
        return segments
    }

    private fun matchEmphasis(source: String, i: Int): MarkupSegment? {
        if (source.startsWith("***", i)) {
            val close = source.indexOf("***", i + 3)
            if (close > i + 3 && !source.substring(i + 3, close).contains('*')) {
                return MarkupSegment.BoldItalic(
                    source.substring(i + 3, close), i, close + 3, i + 3, close
                )
            }
        }
        if (source.startsWith("**", i)) {
            val close = source.indexOf("**", i + 2)
            if (close > i + 2 && !source.substring(i + 2, close).contains('*')) {
                return MarkupSegment.Bold(
                    source.substring(i + 2, close), i, close + 2, i + 2, close
                )
            }
        }
        val close = source.indexOf('*', i + 1)
        if (close > i + 1) {
            return MarkupSegment.Italic(
                source.substring(i + 1, close), i, close + 1, i + 1, close
            )
        }
        return null
    }
}
