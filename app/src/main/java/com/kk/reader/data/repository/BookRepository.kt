package com.kk.reader.data.repository

import com.kk.reader.data.db.entity.BookEntity
import com.kk.reader.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(sortBy: SortOrder = SortOrder.LAST_READ): Flow<List<BookEntity>>
    fun getBookById(id: Long): Flow<BookEntity?>
    suspend fun getBookByIdOnce(id: Long): BookEntity?
    suspend fun insertBook(book: BookEntity): Long
    suspend fun updateBook(book: BookEntity)
    suspend fun deleteBook(book: BookEntity)
    suspend fun updateProgress(bookId: Long, page: Int, chapter: Int, progress: Float)
    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>>
    suspend fun addBookmark(bookmark: BookmarkEntity): Long
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
}

enum class SortOrder {
    TITLE, AUTHOR, LAST_READ, DATE_ADDED
}
