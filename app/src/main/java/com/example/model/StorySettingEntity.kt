package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "story_settings",
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
data class StorySettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val locationName: String,
    val timePeriodOrEra: String = "",
    val atmosphereAndSensory: String = "", // Smells, lighting, acoustics, weather
    val architecturalOrSpatialDetails: String = "",
    val historicalOrCulturalContext: String = "",
    val targetIntertextualTouchstones: String = "", // Books/authors this setting converses with
    val otherNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
