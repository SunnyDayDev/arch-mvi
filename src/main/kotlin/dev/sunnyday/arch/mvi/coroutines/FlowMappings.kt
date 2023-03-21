package dev.sunnyday.arch.mvi.coroutines

import dev.sunnyday.arch.mvi.internal.primitive.CoroutineObservable
import dev.sunnyday.arch.mvi.internal.primitive.CoroutineObservableValue
import dev.sunnyday.arch.mvi.primitive.Observable
import dev.sunnyday.arch.mvi.primitive.ObservableValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*

fun <T> Observable<T>.toFlow(): Flow<T> {
    return if (this is CoroutineObservable) {
        flow
    } else {
        callbackFlow {
            val cancellable = observe { value ->
                var result = trySendBlocking(value)

                // TODO: https://github.com/SunnyDayDev/arch-mvi/issues/13
                //  implement better suspending in non coroutine context
                while (result.isFailure && !result.isClosed) {
                    Thread.sleep(1)
                    result = trySendBlocking(value)
                }
            }

            awaitClose { cancellable.cancel() }
        }
    }
}

fun <T> ObservableValue<T>.toStateFlow(coroutineScope: CoroutineScope): StateFlow<T> {
    return if (this is CoroutineObservableValue) {
        stateFlow
    } else {
        toFlow().stateIn(coroutineScope, SharingStarted.Eagerly, value)
    }
}

fun <T> Flow<T>.toObservable(coroutineScope: CoroutineScope? = null): Observable<T> {
    return CoroutineObservable(this, coroutineScope)
}

fun <T> StateFlow<T>.toObservable(coroutineScope: CoroutineScope? = null): ObservableValue<T> {
    return CoroutineObservableValue(this, coroutineScope)
}