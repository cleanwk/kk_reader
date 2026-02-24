package com.kk.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kk.reader.timer.TimerState
import com.kk.reader.tts.TtsPlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onTtsModelsClick: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val currentChapter by viewModel.currentChapter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val lineSpacing by viewModel.lineSpacing.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val ttsSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val ttsSpeakerId by viewModel.ttsSpeakerId.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showTtsPanel by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = currentChapter,
        pageCount = { maxOf(chapters.size, 1) }
    )

    // Sync pager with viewmodel
    LaunchedEffect(pagerState.currentPage) {
        if (chapters.isNotEmpty()) {
            viewModel.setChapter(pagerState.currentPage)
            viewModel.setPage(pagerState.currentPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chapters.getOrNull(pagerState.currentPage)?.title
                            ?: book?.title ?: "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Timer
                    IconButton(onClick = { showTimerDialog = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Timer")
                    }
                    // Settings
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // Page indicator
                Text(
                    text = if (chapters.isNotEmpty()) {
                        "${pagerState.currentPage + 1} / ${chapters.size}"
                    } else "",
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))

                // Timer indicator
                if (timerState is TimerState.Running) {
                    val remaining = (timerState as TimerState.Running).remainingSeconds
                    Text(
                        text = "${remaining / 60}:%02d".format(remaining % 60),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.cancelTimer() }) {
                        Icon(Icons.Default.TimerOff, contentDescription = "Cancel timer", modifier = Modifier.size(20.dp))
                    }
                }

                // TTS button
                IconButton(onClick = { showTtsPanel = true }) {
                    Icon(
                        when (ttsState) {
                            TtsPlaybackState.PLAYING -> Icons.Default.VolumeUp
                            TtsPlaybackState.PAUSED -> Icons.Default.VolumeMute
                            else -> Icons.Default.VolumeOff
                        },
                        contentDescription = "TTS"
                    )
                }
                // Bookmark
                IconButton(onClick = { viewModel.addBookmark() }) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.toBackground())
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (chapters.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val chapter = chapters.getOrNull(page)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (chapter != null) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = theme.toTextColor()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = chapter.content,
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * lineSpacing).sp,
                                    color = theme.toTextColor()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            ReaderSettingsSheet(
                fontSize = fontSize,
                lineSpacing = lineSpacing,
                theme = theme,
                onFontSizeChange = viewModel::setFontSize,
                onLineSpacingChange = viewModel::setLineSpacing,
                onThemeChange = viewModel::setTheme
            )
        }
    }

    // TTS control panel
    if (showTtsPanel) {
        ModalBottomSheet(onDismissRequest = { showTtsPanel = false }) {
            TtsControlPanel(
                playbackState = ttsState,
                speed = ttsSpeed,
                speakerId = ttsSpeakerId,
                numSpeakers = viewModel.ttsManager.numSpeakers(),
                modelName = viewModel.ttsManager.currentModelName(),
                onTogglePlayback = viewModel::toggleTts,
                onStop = viewModel::stopTts,
                onSpeedChange = viewModel::setTtsSpeed,
                onSpeakerChange = viewModel::setTtsSpeaker,
                onChangeModel = {
                    showTtsPanel = false
                    onTtsModelsClick()
                }
            )
        }
    }

    // Timer dialog
    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onSetTimer = { minutes -> viewModel.startSleepTimer(minutes) }
        )
    }
}
