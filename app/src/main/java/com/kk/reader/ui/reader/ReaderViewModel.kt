package com.kk.reader.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kk.reader.data.db.entity.BookEntity
import com.kk.reader.data.db.entity.BookmarkEntity
import com.kk.reader.data.preferences.ReaderPreferences
import com.kk.reader.data.repository.BookRepository
import com.kk.reader.domain.model.Chapter
import com.kk.reader.domain.parser.BookParserFactory
import com.kk.reader.timer.TimerManager
import com.kk.reader.tts.TtsManager
import com.kk.reader.tts.TtsPlaybackState
import com.kk.reader.ui.theme.ReaderTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
    private val parserFactory: BookParserFactory,
    private val preferences: ReaderPreferences,
    val ttsManager: TtsManager,
    val timerManager: TimerManager
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0L

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _currentChapter = MutableStateFlow(0)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val fontSize = preferences.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 16f)
    val lineSpacing = preferences.lineSpacing.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.5f)
    val theme = preferences.theme
        .map { ReaderTheme.fromString(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ReaderTheme.LIGHT)
    val ttsSpeed = preferences.ttsSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val ttsSpeakerId = preferences.ttsSpeakerId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val ttsState = ttsManager.playbackState
    val timerState = timerManager.timerState

    val bookmarks = repository.getBookmarks(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBook()
        observeTimer()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = repository.getBookByIdOnce(bookId) ?: return@launch
            _book.value = bookEntity
            _currentPage.value = bookEntity.currentPage
            _currentChapter.value = bookEntity.currentChapter

            val parser = parserFactory.getParser(bookEntity.filePath)
            val parsed = parser.parse(bookEntity.filePath)
            _chapters.value = parsed.chapters
            _isLoading.value = false
        }
    }

    private fun observeTimer() {
        viewModelScope.launch {
            timerManager.timerExpired.collect {
                ttsManager.stop()
            }
        }
    }

    fun setPage(page: Int) {
        _currentPage.value = page
        val book = _book.value ?: return
        val totalPages = maxOf(book.totalPages, 1)
        val progress = (page + 1).toFloat() / totalPages
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProgress(bookId, page, _currentChapter.value, progress.coerceIn(0f, 1f))
        }
    }

    fun setChapter(chapter: Int) {
        _currentChapter.value = chapter
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { preferences.setFontSize(size) }
    }

    fun setLineSpacing(spacing: Float) {
        viewModelScope.launch { preferences.setLineSpacing(spacing) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { preferences.setTheme(theme) }
    }

    fun toggleTts() {
        when (ttsManager.playbackState.value) {
            TtsPlaybackState.PLAYING -> ttsManager.pause()
            TtsPlaybackState.PAUSED -> ttsManager.resume()
            else -> {
                val chapter = _chapters.value.getOrNull(_currentChapter.value) ?: return
                ttsManager.startReading(chapter.content)
            }
        }
    }

    fun stopTts() {
        ttsManager.stop()
    }

    fun setTtsSpeed(speed: Float) {
        viewModelScope.launch { preferences.setTtsSpeed(speed) }
        ttsManager.setSpeed(speed)
    }

    fun setTtsSpeaker(speakerId: Int) {
        viewModelScope.launch { preferences.setTtsSpeakerId(speakerId) }
        ttsManager.setSpeaker(speakerId)
    }

    fun addBookmark() {
        viewModelScope.launch {
            repository.addBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    page = _currentPage.value,
                    chapter = _currentChapter.value
                )
            )
        }
    }

    fun startSleepTimer(minutes: Int) {
        timerManager.startSleepTimer(minutes)
    }

    fun cancelTimer() {
        timerManager.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}
