package com.example.model

enum class WorkType(
    val displayName: String,
    val pluralName: String,
    val description: String,
    val defaultGenre: String,
    val iconName: String
) {
    NOVEL(
        displayName = "Novel",
        pluralName = "Novels",
        description = "Literary fiction, narrative arcs, character dialog, and scenic chapters.",
        defaultGenre = "Literary Fiction",
        iconName = "Book"
    ),
    BIOGRAPHY(
        displayName = "Biography",
        pluralName = "Biographies",
        description = "Historical accounts, memoirs, life chronicles with primary sources and endnotes.",
        defaultGenre = "Historical Biography",
        iconName = "Person"
    ),
    DOCUMENTARY(
        displayName = "Documentary",
        pluralName = "Documentaries",
        description = "Non-fiction investigation, field archives, interviews, and photographic references.",
        defaultGenre = "Investigative Non-Fiction",
        iconName = "Article"
    ),
    MANUAL(
        displayName = "Manual",
        pluralName = "Manuals",
        description = "Technical guides, reference documentation, structured procedures, and glossaries.",
        defaultGenre = "Technical Reference",
        iconName = "MenuBook"
    )
}
