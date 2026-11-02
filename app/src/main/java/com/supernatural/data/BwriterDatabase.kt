package com.supernatural.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.supernatural.model.CharacterEntity
import com.supernatural.model.EditorialCommentEntity
import com.supernatural.model.ManuscriptEntity
import com.supernatural.model.SectionEntity
import com.supernatural.model.StorySettingEntity

@Database(
    entities = [
        ManuscriptEntity::class,
        SectionEntity::class,
        EditorialCommentEntity::class,
        CharacterEntity::class,
        StorySettingEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class BwriterDatabase : RoomDatabase() {
    abstract fun manuscriptDao(): ManuscriptDao
    abstract fun sectionDao(): SectionDao
    abstract fun commentDao(): CommentDao
    abstract fun characterDao(): CharacterDao
    abstract fun settingDao(): StorySettingDao

    companion object {
        @Volatile
        private var INSTANCE: BwriterDatabase? = null

        fun getDatabase(context: Context): BwriterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BwriterDatabase::class.java,
                    "bwriter_manuscripts.db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
