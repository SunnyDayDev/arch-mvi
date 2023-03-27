package dev.sunnyday.arch.mvi.coroutine.ktx

import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableEvent
import dev.sunnyday.arch.mvi.coroutine.primitive.CoroutineObservableState
import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.primitive.ObservableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*

fun <T> ObservableEvent<T>.toFlow(): Flow<T> {
    return if (this is CoroutineObservableEvent) {
        flow
    } else {
        callbackFlow {
            val cancellable = observe { value ->
                retrySendUntilSuccess(value)
            }

            awaitClose { cancellable.cancel() }
        }
    }
}

internal fun <T> SendChannel<T>.retrySendUntilSuccess(value: T) {
    var result = trySendBlocking(value)

    // TODO: https://github.com/SunnyDayDev/arch-mvi/issues/13
    //  implement better suspending in non coroutine context
    while (result.isFailure && !result.isClosed) {
        Thread.sleep(1)
        if (Thread.currentThread().isInterrupted) return
        result = trySendBlocking(value)
    }
}

fun <T> ObservableState<T>.toStateFlow(
    coroutineScope: CoroutineScope,
): StateFlow<T> {
    return if (this is CoroutineObservableState) {
        stateFlow
    } else {
        toFlow().stateIn(coroutineScope, SharingStarted.Eagerly, value)
    }
}

fun <T> Flow<T>.toObservable(coroutineScope: CoroutineScope? = null): ObservableEvent<T> {
    return CoroutineObservableEvent(this, coroutineScope)
}

fun <T> StateFlow<T>.toObservable(coroutineScope: CoroutineScope? = null): ObservableState<T> {
    return CoroutineObservableState(this, coroutineScope)
}