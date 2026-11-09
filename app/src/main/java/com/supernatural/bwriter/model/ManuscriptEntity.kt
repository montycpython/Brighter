package com.supernatural.bwriter.model

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
    val year: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString(),
    val isbn: String = "",
    val birthId: String = "",
    val stampHash: String = "",
    val prevStampHash: String = "",
    val stampSignature: String = "",
    val authorAddress: String = "",
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
    val effectiveAuthorByline: String
        get() {
            return when {
                authorPenName.isNotBlank() -> authorPenName.trim()
                authorName.isNotBlank() -> authorName.trim()
                else -> authorEmail
            }
        }

    val acknowledgmentsList: List<ContributorCredit>
        get() = ContributorCredit.parseListFromJson(acknowledgmentsJson)

    val effectiveCopyrightText: String
        get() {
            if (copyrightText.isNotBlank()) {
                return copyrightText
            }
            val byline = effectiveAuthorByline
            val yr = year.ifBlank { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString() }
            val pub = publisher.ifBlank { "Bwriter Editions" }
            return "Copyright © $yr by $byline.\nAll rights reserved under International and Pan-American Copyright Conventions.\nPublished by $pub in accordance with The Chicago Manual of Style.\nPrinted in the United States of America."
        }
}
