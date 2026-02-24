package com.kk.reader.data.repository

import com.kk.reader.data.db.dao.BookDao
import com.kk.reader.data.db.dao.BookmarkDao
import com.kk.reader.data.db.entity.BookEntity
import com.kk.reader.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao
) : BookRepository {

    override fun getAllBooks(sortBy: SortOrder): Flow<List<BookEntity>> = when (sortBy) {
        SortOrder.TITLE -> bookDao.getBooksSortedByTitle()
        SortOrder.AUTHOR -> bookDao.getBooksSortedByAuthor()
        SortOrder.LAST_READ -> bookDao.getAllBooks()
        SortOrder.DATE_ADDED -> bookDao.getBooksSortedByDateAdded()
    }

    override fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookByIdFlow(id)

    override suspend fun getBookByIdOnce(id: Long): BookEntity? = bookDao.getBookById(id)

    override suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)

    override suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    override suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    override suspend fun updateProgress(bookId: Long, page: Int, chapter: Int, progress: Float) =
        bookDao.updateProgress(bookId, page, chapter, progress)

    override fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    override suspend fun addBookmark(bookmark: BookmarkEntity): Long =
        bookmarkDao.insertBookmark(bookmark)

    override suspend fun deleteBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.deleteBookmark(bookmark)
}
