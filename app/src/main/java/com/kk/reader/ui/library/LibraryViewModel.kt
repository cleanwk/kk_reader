package com.kk.reader.ui.library

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kk.reader.data.db.entity.BookEntity
import com.kk.reader.data.repository.BookRepository
import com.kk.reader.data.repository.SortOrder
import com.kk.reader.domain.parser.BookParserFactory
import com.kk.reader.domain.parser.totalPages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: BookRepository,
    private val parserFactory: BookParserFactory,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.LAST_READ)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val books: StateFlow<List<BookEntity>> = _sortOrder
        .flatMapLatest { order -> repository.getAllBooks(order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
                val ext = fileName.substringAfterLast('.', "txt").lowercase()
                val booksDir = File(context.filesDir, "books").apply { mkdirs() }
                val destFile = File(booksDir, "${System.currentTimeMillis()}_$fileName")

                // Copy file from URI to internal storage
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val parser = parserFactory.getParser(destFile.path)
                val parsed = parser.parse(destFile.path)

                // Save cover image if available
                val coverPath = parsed.coverImage?.let { bitmap ->
                    saveCover(bitmap, destFile.nameWithoutExtension)
                }

                val book = BookEntity(
                    title = parsed.title,
                    author = parsed.author,
                    filePath = destFile.absolutePath,
                    fileType = ext,
                    coverPath = coverPath,
                    totalPages = totalPages(parsed.totalCharacters)
                )
                repository.insertBook(book)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete book file
            File(book.filePath).delete()
            book.coverPath?.let { File(it).delete() }
            repository.deleteBook(book)
        }
    }

    private fun saveCover(bitmap: Bitmap, name: String): String {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val coverFile = File(coversDir, "$name.jpg")
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return coverFile.absolutePath
    }
}
