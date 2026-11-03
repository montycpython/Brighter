package com.supernatural.bwriter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.supernatural.bwriter.model.CharacterEntity
import com.supernatural.bwriter.model.EditorialCommentEntity
import com.supernatural.bwriter.model.ManuscriptEntity
import com.supernatural.bwriter.model.SectionEntity
import com.supernatural.bwriter.model.StorySettingEntity

@Database(
    entities = [
        ManuscriptEntity::class,
        SectionEntity::class,
        EditorialCommentEntity::class,
        CharacterEntity::class,
        StorySettingEntity::class
    ],
    version = 6,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE manuscripts ADD COLUMN birthId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE manuscripts ADD COLUMN stampHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE manuscripts ADD COLUMN prevStampHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE manuscripts ADD COLUMN stampSignature TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE manuscripts ADD COLUMN authorAddress TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): BwriterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BwriterDatabase::class.java,
                    "bwriter_manuscripts.db"
                )
                .addMigrations(MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
