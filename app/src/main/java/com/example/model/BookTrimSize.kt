package com.example.model

/**
 * Standard Book Trim Sizes per Chicago Manual of Style / US & International Trade Standards.
 * Point dimensions at 72 points per inch (1 in = 72 pt).
 */
enum class BookTrimSize(
    val id: String,
    val displayName: String,
    val description: String,
    val widthInches: Float,
    val heightInches: Float,
    val widthPt: Int,
    val heightPt: Int,
    val defaultBodyFontSizePt: Float,
    val defaultLineHeightMultiplier: Float,
    val gutterMarginPt: Float,
    val outerMarginPt: Float,
    val topMarginPt: Float,
    val bottomMarginPt: Float,
    val wordsPerPageEstimate: Int
) {
    TRADE_6X9(
        id = "trade_6x9",
        displayName = "Trade 6\" x 9\"",
        description = "Standard US trade paperback & hardcover novel format.",
        widthInches = 6.0f,
        heightInches = 9.0f,
        widthPt = 432, // 6 * 72
        heightPt = 648, // 9 * 72
        defaultBodyFontSizePt = 10.5f,
        defaultLineHeightMultiplier = 1.35f,
        gutterMarginPt = 54f, // 0.75 in
        outerMarginPt = 42f, // 0.58 in
        topMarginPt = 48f,
        bottomMarginPt = 48f,
        wordsPerPageEstimate = 260
    ),
    DIGEST_5_5X8_5(
        id = "digest_5_5x8_5",
        displayName = "Digest 5.5\" x 8.5\"",
        description = "Compact literary fiction, memoirs, and essays.",
        widthInches = 5.5f,
        heightInches = 8.5f,
        widthPt = 396, // 5.5 * 72
        heightPt = 612, // 8.5 * 72
        defaultBodyFontSizePt = 10.0f,
        defaultLineHeightMultiplier = 1.32f,
        gutterMarginPt = 48f,
        outerMarginPt = 36f,
        topMarginPt = 42f,
        bottomMarginPt = 42f,
        wordsPerPageEstimate = 230
    ),
    CROWN_QUARTO_7X10(
        id = "crown_quarto_7x10",
        displayName = "Crown Quarto 7\" x 10\"",
        description = "Scholarly monographs, illustrated texts, and course manuals.",
        widthInches = 7.0f,
        heightInches = 10.0f,
        widthPt = 504, // 7 * 72
        heightPt = 720, // 10 * 72
        defaultBodyFontSizePt = 11.0f,
        defaultLineHeightMultiplier = 1.4f,
        gutterMarginPt = 60f,
        outerMarginPt = 48f,
        topMarginPt = 54f,
        bottomMarginPt = 54f,
        wordsPerPageEstimate = 340
    ),
    ROYAL_8X10(
        id = "royal_8x10",
        displayName = "Executive / Royal 8\" x 10\"",
        description = "Art collections, photo documentaries, and technical guides.",
        widthInches = 8.0f,
        heightInches = 10.0f,
        widthPt = 576, // 8 * 72
        heightPt = 720, // 10 * 72
        defaultBodyFontSizePt = 11.5f,
        defaultLineHeightMultiplier = 1.45f,
        gutterMarginPt = 64f,
        outerMarginPt = 52f,
        topMarginPt = 58f,
        bottomMarginPt = 58f,
        wordsPerPageEstimate = 420
    ),
    POCKET_4_25X6_87(
        id = "pocket_4_25x6_87",
        displayName = "Mass Market Pocket 4.25\" x 6.87\"",
        description = "Classic pocket paperback mass market size.",
        widthInches = 4.25f,
        heightInches = 6.87f,
        widthPt = 306,
        heightPt = 495,
        defaultBodyFontSizePt = 9.0f,
        defaultLineHeightMultiplier = 1.25f,
        gutterMarginPt = 38f,
        outerMarginPt = 28f,
        topMarginPt = 34f,
        bottomMarginPt = 34f,
        wordsPerPageEstimate = 190
    );

    companion object {
        fun fromTargetString(target: String?): BookTrimSize {
            if (target.isNullOrBlank()) return TRADE_6X9
            return values().firstOrNull { 
                it.displayName.equals(target, ignoreCase = true) ||
                it.id.equals(target, ignoreCase = true) ||
                target.contains(it.displayName, ignoreCase = true)
            } ?: TRADE_6X9
        }
    }
}
