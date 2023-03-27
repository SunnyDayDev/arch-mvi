package dev.sunnyday.arch.mvi.coroutine.ktx

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.takeUntil(signalFlow: Flow<Any?>): Flow<T> {
    val original = this

    return signalFlow
        .take(1)
        .map { emptyFlow<T>() }
        .onStart { emit(original) }
        .flatMapLatest { flow -> flow }
}