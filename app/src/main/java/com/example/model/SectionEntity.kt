package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = ManuscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["manuscriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("manuscriptId"), Index("orderIndex")]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val matterType: MatterType,
    val sectionType: SectionType,
    val title: String,
    val subtitle: String = "",
    val orderIndex: Int,
    val content: String = "",
    val aiDraftPrompt: String = "",
    val contributorNotes: String = "",
    val assignedAuthor: String = "Author",
    val assignedRole: WorkRole = WorkRole.AUTHOR,
    val status: SectionStatus = SectionStatus.DRAFT,
    val startOnRecto: Boolean = true,
    val wordCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
