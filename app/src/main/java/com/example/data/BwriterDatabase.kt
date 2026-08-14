package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.EditorialCommentEntity
import com.example.model.ManuscriptEntity
import com.example.model.SectionEntity

@Database(
    entities = [
        ManuscriptEntity::class,
        SectionEntity::class,
        EditorialCommentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BwriterDatabase : RoomDatabase() {
    abstract fun manuscriptDao(): ManuscriptDao
    abstract fun sectionDao(): SectionDao
    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile
        private var INSTANCE: BwriterDatabase? = null

        fun getDatabase(context: Context): BwriterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BwriterDatabase::class.java,
                    "bwriter_manuscripts.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
