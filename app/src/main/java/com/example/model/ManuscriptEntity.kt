package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manuscripts")
data class ManuscriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val workType: WorkType = WorkType.NOVEL,
    val authorName: String = "",
    val authorPenName: String = "",
    val authorEmail: String = "",
    val editorName: String = "",
    val publisher: String = "Bwriter Editions",
    val edition: String = "First Edition",
    val year: String = "2026",
    val isbn: String = "978-0-226-10403-4",
    val copyrightText: String = "",
    val dedication: String = "",
    val epigraphText: String = "",
    val epigraphAuthor: String = "",
    val targetPageSize: String = "Trade 6\" x 9\"",
    val manuscriptStatus: String = "DRAFT", // DRAFT, UNDER_REVIEW, POLISHED, FINAL
    val acknowledgmentsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns the Pen Name if provided, otherwise the Author Name, or fallback to 'Author'.
     */
    val effectiveAuthorByline: String
        get() {
            return when {
                authorPenName.isNotBlank() -> authorPenName.trim()
                authorName.isNotBlank() -> authorName.trim()
                else -> "Author"
            }
        }

    /**
     * Returns the list of parsed contributor and editor credits.
     */
    val acknowledgmentsList: List<ContributorCredit>
        get() = ContributorCredit.parseListFromJson(acknowledgmentsJson)

    /**
     * Dynamically generates the Chicago Manual of Style Copyright notice attributed to the Author or Pen Name.
     */
    val effectiveCopyrightText: String
        get() {
            if (copyrightText.isNotBlank() && !copyrightText.contains("Arthur Vance")) {
                return copyrightText
            }
            val byline = effectiveAuthorByline
            val yr = year.ifBlank { "2026" }
            val pub = publisher.ifBlank { "Bwriter Editions" }
            return "Copyright © $yr by $byline.\nAll rights reserved under International and Pan-American Copyright Conventions.\nPublished by $pub in accordance with The Chicago Manual of Style.\nPrinted in the United States of America."
        }
}
