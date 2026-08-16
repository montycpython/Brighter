package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE manuscriptId = :manuscriptId ORDER BY name ASC")
    fun getCharactersForManuscript(manuscriptId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE manuscriptId = :manuscriptId ORDER BY name ASC")
    suspend fun getCharactersForManuscriptOnce(manuscriptId: Long): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterById(id: Long): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterByIdOnce(id: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterEntity>): List<Long>

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacterById(id: Long)

    @Query("DELETE FROM characters WHERE manuscriptId = :manuscriptId")
    suspend fun deleteAllCharactersForManuscript(manuscriptId: Long)
}
