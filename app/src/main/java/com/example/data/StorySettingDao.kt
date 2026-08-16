package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.StorySettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorySettingDao {
    @Query("SELECT * FROM story_settings WHERE manuscriptId = :manuscriptId ORDER BY locationName ASC")
    fun getSettingsForManuscript(manuscriptId: Long): Flow<List<StorySettingEntity>>

    @Query("SELECT * FROM story_settings WHERE manuscriptId = :manuscriptId ORDER BY locationName ASC")
    suspend fun getSettingsForManuscriptOnce(manuscriptId: Long): List<StorySettingEntity>

    @Query("SELECT * FROM story_settings WHERE id = :id")
    fun getSettingById(id: Long): Flow<StorySettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: StorySettingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<StorySettingEntity>): List<Long>

    @Update
    suspend fun updateSetting(setting: StorySettingEntity)

    @Delete
    suspend fun deleteSetting(setting: StorySettingEntity)

    @Query("DELETE FROM story_settings WHERE id = :id")
    suspend fun deleteSettingById(id: Long)
}
