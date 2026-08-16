package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = ManuscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["manuscriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("manuscriptId")]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val name: String,
    val role: String = "Protagonist", // Protagonist, Antagonist, Foil, Deuteragonist, Mentor, Supporting, Narrator
    val physicalDescription: String = "",
    val psychologicalDescription: String = "",
    val backstory: String = "",
    val voiceAndMannerisms: String = "",
    val intertextualArchetype: String = "",
    val otherDetails: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
