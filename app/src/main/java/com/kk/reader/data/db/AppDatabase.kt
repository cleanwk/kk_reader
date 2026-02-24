package com.kk.reader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kk.reader.data.db.dao.BookDao
import com.kk.reader.data.db.dao.BookmarkDao
import com.kk.reader.data.db.entity.BookEntity
import com.kk.reader.data.db.entity.BookmarkEntity

@Database(
    entities = [BookEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
}
