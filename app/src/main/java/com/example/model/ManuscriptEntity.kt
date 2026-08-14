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
    val authorName: String = "Dr. Arthur Vance",
    val authorPenName: String = "A. V. Hawthorne",
    val authorEmail: String = "real.artistry@gmail.com",
    val editorName: String = "Eleanor Rigby, Senior Editor",
    val publisher: String = "University Press & Chicago Editorial Arts",
    val edition: String = "First Edition",
    val year: String = "2026",
    val isbn: String = "978-0-226-10403-4",
    val copyrightText: String = "Copyright © 2026 by Arthur Vance. All rights reserved.\nPublished in accordance with The Chicago Manual of Style.\nPrinted in the United States of America.",
    val dedication: String = "For the meticulous editors and discerning readers.",
    val epigraphText: String = "“Style is the dress of thought; a modest dress, neat, but not gaudy, will true respect command.”",
    val epigraphAuthor: String = "Samuel Johnson",
    val targetPageSize: String = "Trade 6\" x 9\"",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
