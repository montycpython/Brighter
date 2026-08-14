package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ManuscriptEntity
import com.example.model.WorkType
import kotlinx.coroutines.flow.Flow

@Dao
interface ManuscriptDao {
    @Query("SELECT * FROM manuscripts ORDER BY updatedAt DESC")
    fun getAllManuscripts(): Flow<List<ManuscriptEntity>>

    @Query("SELECT * FROM manuscripts WHERE id = :id")
    fun getManuscriptById(id: Long): Flow<ManuscriptEntity?>

    @Query("SELECT * FROM manuscripts WHERE id = :id")
    suspend fun getManuscriptByIdOnce(id: Long): ManuscriptEntity?

    @Query("SELECT * FROM manuscripts WHERE workType = :workType ORDER BY updatedAt DESC")
    fun getManuscriptsByType(workType: WorkType): Flow<List<ManuscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManuscript(manuscript: ManuscriptEntity): Long

    @Update
    suspend fun updateManuscript(manuscript: ManuscriptEntity)

    @Delete
    suspend fun deleteManuscript(manuscript: ManuscriptEntity)

    @Query("DELETE FROM manuscripts WHERE id = :id")
    suspend fun deleteManuscriptById(id: Long)
}
