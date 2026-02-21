package com.example.memokeyword.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Memo::class, Keyword::class, MemoKeywordCrossRef::class, MemoPhoto::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memoDao(): MemoDao
    abstract fun keywordDao(): KeywordDao
    abstract fun memoPhotoDao(): MemoPhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS memo_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        memoId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(memoId) REFERENCES memos(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_memo_photos_memoId ON memo_photos(memoId)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memo_keyword_database"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
