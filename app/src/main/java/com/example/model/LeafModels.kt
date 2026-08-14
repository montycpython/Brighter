package com.example.model

enum class MatterType(val displayName: String, val order: Int) {
    FRONT_MATTER("Front Matter", 1),
    TEXT_BODY("Text / Body", 2),
    BACK_MATTER("Back Matter", 3)
}

enum class LeafSide {
    RECTO, // Right-hand page, odd-numbered (1, 3, 5 / i, iii, v)
    VERSO  // Left-hand page, even-numbered (2, 4, 6 / ii, iv, vi)
}

enum class LeafDisplayType {
    CONTENT,
    BLANK_INTENTIONAL, // Blank leaf inserted to ensure next division starts on Recto per CMOS
    HALF_TITLE,
    TITLE_PAGE,
    COPYRIGHT,
    DEDICATION,
    EPIGRAPH,
    TABLE_OF_CONTENTS,
    CHAPTER_OPENER,
    PART_OPENER,
    COLOPHON
}

enum class SectionType(
    val defaultTitle: String,
    val matterType: MatterType,
    val requiresRectoStart: Boolean,
    val defaultLeafSide: LeafSide,
    val defaultDescription: String
) {
    // Front Matter
    HALF_TITLE("Half-Title", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Contains solely the main title of the work, preceding the full title page."),
    TITLE_PAGE("Title Page", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Full title, subtitle, author byline, and publisher imprint."),
    COPYRIGHT_COLOPHON("Copyright & Colophon", MatterType.FRONT_MATTER, false, LeafSide.VERSO, "Copyright notice, edition notice, cataloging data, and printing information on the verso of the title page."),
    DEDICATION("Dedication", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Author's personal dedication, placed on a recto leaf."),
    EPIGRAPH("Epigraph", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Quotation pertinent to the work."),
    TABLE_OF_CONTENTS("Table of Contents", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Listing of front matter, parts, chapters, and back matter with folios."),
    FOREWORD("Foreword", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Introductory remarks by someone other than the author."),
    PREFACE("Preface", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Author's statement on the origin, purpose, and scope of the work."),
    ACKNOWLEDGMENTS("Acknowledgments", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Author's formal thanks to contributors, editors, and institutions."),
    INTRODUCTION("Introduction", MatterType.FRONT_MATTER, true, LeafSide.RECTO, "Thematic or conceptual introduction preceding the main text body."),

    // Body Text
    PART_DIVIDER("Part Division", MatterType.TEXT_BODY, true, LeafSide.RECTO, "Part title leaf opening a major structural division of the book."),
    CHAPTER("Chapter", MatterType.TEXT_BODY, true, LeafSide.RECTO, "Main narrative chapter beginning on a recto leaf per CMOS."),
    SECTION("Section / Episode", MatterType.TEXT_BODY, false, LeafSide.RECTO, "Subordinate division within a chapter or manual module."),

    // Back Matter
    APPENDIX("Appendix", MatterType.BACK_MATTER, true, LeafSide.RECTO, "Supplementary documentary data, tables, or specialized texts."),
    NOTES_ENDNOTES("Notes & Endnotes", MatterType.BACK_MATTER, true, LeafSide.RECTO, "Chapter-by-chapter endnotes following Chicago style citation rules."),
    GLOSSARY("Glossary", MatterType.BACK_MATTER, true, LeafSide.RECTO, "Alphabetical list of specialized terms with definitions."),
    BIBLIOGRAPHY("Bibliography & References", MatterType.BACK_MATTER, true, LeafSide.RECTO, "Comprehensive list of sources cited, arranged with hanging indent."),
    ABOUT_AUTHOR("About the Author", MatterType.BACK_MATTER, true, LeafSide.RECTO, "Biographical note about the author and contributors."),
    COLOPHON("Colophon", MatterType.BACK_MATTER, false, LeafSide.VERSO, "Typography statement, typeface details, paper stock, and press imprint.")
}

enum class SectionStatus(val displayName: String) {
    DRAFT("Draft"),
    UNDER_REVIEW("Under Review"),
    POLISHED("Polished"),
    FINAL("Final Approved")
}

data class CalculatedLeaf(
    val leafIndex: Int, // Overall leaf number
    val pageNumberDisplay: String, // "i", "ii", "iii" or "1", "2", "3" or ""
    val pageNumberRaw: Int,
    val side: LeafSide,
    val matterType: MatterType,
    val sectionId: Long?,
    val sectionTitle: String,
    val sectionSubtitle: String = "",
    val sectionType: SectionType?,
    val displayType: LeafDisplayType,
    val contentSnippet: String,
    val isOpener: Boolean = false,
    val isCloser: Boolean = false,
    val headerIllustrationUri: String = "",
    val headerIllustrationCaption: String = "",
    val tailIllustrationUri: String = "",
    val tailIllustrationCaption: String = "",
    val runningHeadVerso: String = "",
    val runningHeadRecto: String = "",
    val hasBlindFolio: Boolean = false
)
