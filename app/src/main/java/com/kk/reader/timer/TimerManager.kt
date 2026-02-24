package com.kk.reader.timer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class TimerState {
    data object Idle : TimerState()
    data class Running(val remainingSeconds: Int, val totalSeconds: Int) : TimerState()
}

@Singleton
class TimerManager @Inject constructor() {

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timerExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerExpired: SharedFlow<Unit> = _timerExpired.asSharedFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startSleepTimer(minutes: Int) {
        cancel()
        val totalSeconds = minutes * 60
        timerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                _timerState.value = TimerState.Running(remaining, totalSeconds)
                delay(1000L)
                remaining--
            }
            _timerState.value = TimerState.Idle
            _timerExpired.tryEmit(Unit)
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerState.Idle
    }
}
