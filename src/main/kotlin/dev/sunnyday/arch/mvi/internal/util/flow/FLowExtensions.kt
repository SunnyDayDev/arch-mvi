@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.sunnyday.arch.mvi.internal.util.flow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal fun <T> Flow<T>.takeUntil(signalFlow: Flow<*>): Flow<T> {
    val original = this

    return signalFlow
        .take(1)
        .map { emptyFlow<T>() }
        .onStart { emit(original) }
        .flatMapLatest { flow -> flow }
}