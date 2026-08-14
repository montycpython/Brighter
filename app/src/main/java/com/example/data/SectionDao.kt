package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.MatterType
import com.example.model.SectionEntity
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

    @Delete
    suspend fun deleteSection(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteSectionById(id: Long)

    @Query("SELECT COALESCE(MAX(orderIndex), 0) FROM sections WHERE manuscriptId = :manuscriptId")
    suspend fun getMaxOrderIndex(manuscriptId: Long): Int
}
