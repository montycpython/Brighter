package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.EditorialCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM editorial_comments WHERE sectionId = :sectionId ORDER BY timestamp ASC")
    fun getCommentsForSection(sectionId: Long): Flow<List<EditorialCommentEntity>>

    @Query("SELECT * FROM editorial_comments WHERE manuscriptId = :manuscriptId ORDER BY timestamp DESC")
    fun getCommentsForManuscript(manuscriptId: Long): Flow<List<EditorialCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: EditorialCommentEntity): Long

    @Update
    suspend fun updateComment(comment: EditorialCommentEntity)

    @Delete
    suspend fun deleteComment(comment: EditorialCommentEntity)

    @Query("UPDATE editorial_comments SET isResolved = :resolved WHERE id = :commentId")
    suspend fun setResolved(commentId: Long, resolved: Boolean)
}
