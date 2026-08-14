package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "editorial_comments",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId")]
)
data class EditorialCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sectionId: Long,
    val manuscriptId: Long,
    val authorName: String,
    val authorRole: WorkRole,
    val commentText: String,
    val cmosRuleReference: String = "",
    val isResolved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
