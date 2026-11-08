package com.supernatural.bwriter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.supernatural.bwriter.model.MatterType
import com.supernatural.bwriter.model.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE manuscriptId = :manuscriptId ORDER BY orderIndex ASC")
    fun getSectionsForManuscript(manuscriptId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE manuscriptId = :manuscriptId ORDER BY orderIndex ASC")
    suspend fun getSectionsForManuscriptOnce(manuscriptId: Long): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE manuscriptId = :manuscriptId AND matterType = :matterType ORDER BY orderIndex ASC")
    fun getSectionsByMatter(manuscriptId: Long, matterType: MatterType): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    fun getSectionById(id: Long): Flow<SectionEntity?>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getSectionByIdOnce(id: Long): SectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>): List<Long>

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Query("UPDATE sections SET content = :content, wordCount = :wordCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSectionContent(id: Long, content: String, wordCount: Int, updatedAt: Long)

    @Query("UPDATE sections SET title = :title, subtitle = :subtitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSectionTitle(id: Long, title: String, subtitle: String, updatedAt: Long)

    @Query("UPDATE sections SET headerIllustrationUri = :headerUri, headerIllustrationCaption = :headerCaption, tailIllustrationUri = :tailUri, tailIllustrationCaption = :tailCaption, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSectionIllustrations(id: Long, headerUri: String, headerCaption: String, tailUri: String, tailCaption: String, updatedAt: Long)

    @Delete
    suspend fun deleteSection(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSectionById(id: Long)

    @Query("SELECT COALESCE(MAX(orderIndex), 0) FROM sections WHERE manuscriptId = :manuscriptId")
    suspend fun getMaxOrderIndex(manuscriptId: Long): Int
}
